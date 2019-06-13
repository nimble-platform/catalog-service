package eu.nimble.service.catalogue.impl;

import eu.nimble.service.catalogue.CatalogueService;
import eu.nimble.service.catalogue.CatalogueServiceImpl;
import eu.nimble.service.catalogue.persistence.util.CatalogueDatabaseAdapter;
import eu.nimble.service.catalogue.util.SpringBridge;
import eu.nimble.service.model.ubl.catalogue.CatalogueType;
import eu.nimble.service.model.ubl.commonaggregatecomponents.CatalogueLineType;
import eu.nimble.service.model.ubl.commonaggregatecomponents.PartyType;
import eu.nimble.service.model.ubl.commonaggregatecomponents.PersonType;
import eu.nimble.utility.Configuration;
import eu.nimble.utility.HttpResponseUtil;
import eu.nimble.utility.JsonSerializationUtility;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import org.apache.poi.ss.usermodel.Workbook;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.logging.LogLevel;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Controller
public class ImportExportController {

    private static Logger log = LoggerFactory
            .getLogger(ImportExportController.class);

    @Autowired
    private CatalogueService service;

    @CrossOrigin(origins = {"*"})
    @ApiOperation(value = "", notes = "This service imports the provided UBL catalogue.If there is no PartyType information"+
            " in the given catalogue,it's retrieved from the currently configured identity service using the authorization"+
            " token of the user issuing the call.")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Imported the catalogue succesfully"),
            @ApiResponse(code = 401, message = "Invalid token. No user was found for the provided token"),
            @ApiResponse(code = 500, message = "Unexpected error while importing catalogue")
    })
    @RequestMapping(value = "/catalogue/import",
            method = RequestMethod.POST,
            consumes = {MediaType.APPLICATION_JSON_VALUE})
    public ResponseEntity importCatalogue(@ApiParam(value = "Serialized form of the catalogue.", required = true) @RequestBody String serializedCatalogue,
                                          @ApiParam(value = "The Bearer token provided by the identity service", required = true) @RequestHeader(value = "Authorization") String bearerToken) {
        try {
            log.info("Importing catalogue ...");
            // check token
            ResponseEntity tokenCheck = eu.nimble.service.catalogue.util.HttpResponseUtil.checkToken(bearerToken);
            if (tokenCheck != null) {
                return tokenCheck;
            }

            // remove hjid fields of catalogue
            JSONObject object = new JSONObject(serializedCatalogue);
            JsonSerializationUtility.removeHjidFields(object);
            CatalogueType catalogue = JsonSerializationUtility.getObjectMapper().readValue(object.toString(), CatalogueType.class);

            PartyType party = CatalogueDatabaseAdapter.syncPartyInUBLDB(catalogue.getProviderParty(),bearerToken);

            // remove hjid fields of party
            JSONObject partyObject = new JSONObject(party);
            JsonSerializationUtility.removeHjidFields(partyObject);
            party = JsonSerializationUtility.getObjectMapper().readValue(partyObject.toString(), PartyType.class);

            // replaced provider party of the catalogue with the party
            catalogue.setProviderParty(party);
            for (CatalogueLineType catalogueLineType : catalogue.getCatalogueLine()) {
                // replaced manufacturer parties of catalogue lines with the party
                catalogueLineType.getGoodsItem().getItem().setManufacturerParty(party);
            }
            // add the catalogue
            catalogue = service.addCatalogueWithUUID(catalogue, Configuration.Standard.UBL, catalogue.getUUID());
            log.info("Imported the catalogue successfully");
            return ResponseEntity.ok().body(JsonSerializationUtility.serializeEntity(catalogue));

        } catch (Exception e) {
            String msg = String.format("Failed to import catalogue: %s", serializedCatalogue);
            return HttpResponseUtil.createResponseEntityAndLog(msg, e, HttpStatus.INTERNAL_SERVER_ERROR, LogLevel.ERROR);
        }
    }


    @CrossOrigin(origins = {"*"})
    @ApiOperation(value = "", notes = "Exports the catalogue specified with uuid. The products are exported to separate " +
            "Excel sheets according to the commodity classifications (categories). Each distinct combination of categories " +
            "of products are exported to a separate sheet. The sheets are collected into a ZIP-compressed file.")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Imported the catalogue successfully"),
            @ApiResponse(code = 401, message = "Invalid token. No user was found for the provided token"),
            @ApiResponse(code = 500, message = "Unexpected error while exporting catalogue")
    })
    @RequestMapping(value = "/catalogue/export",
            method = RequestMethod.GET,
            produces = {"application/zip"})
    public void exportCatalogue(
            @ApiParam(value = "Identifier of the catalogue to be exported", required = true) @RequestParam("uuid") String catalogueUuid,
            @ApiParam(value = "language id", required = true) @RequestParam("languageId") String languageId,
            @ApiParam(value = "The Bearer token provided by the identity service", required = true) @RequestHeader(value = "Authorization", required = true) String bearerToken,
            HttpServletResponse response) {

        log.info("Incoming request to export catalogue with uuid {}", catalogueUuid);
        // token check
        ResponseEntity tokenCheck = eu.nimble.service.catalogue.util.HttpResponseUtil.checkToken(bearerToken);
        if (tokenCheck != null) {
            return;
        }
        // check whether the catalogue with the given uuid exists or not
        CatalogueType catalogue;
        try {
            catalogue = service.getCatalogue(catalogueUuid);
        } catch (Exception e) {
            String msg = "Failed to get catalogue for uuid: "+catalogueUuid + "\n" + e.getMessage();
            log.error(msg);
            response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
            try {
                response.getOutputStream().write(msg.getBytes());
            } catch (IOException e1) {
                log.error("Failed to write the error message to the output stream", e1);
            }
            return;
        }

        // no catalogue for the given uuid
        if (catalogue == null) {
            String msg = "No catalogue for uuid: " + catalogueUuid;
            log.info(msg);
            response.setStatus(HttpStatus.NOT_FOUND.value());
            try {
                response.getOutputStream().write(msg.getBytes());
            } catch (IOException e) {
                log.error("Failed to write the error message to the output stream", e);
            }
            return;
        }
        // get workbooks
        Map<Workbook,String> workbooks = service.generateTemplateForCatalogue(catalogue,languageId);

        // export catalogue
        ZipOutputStream zos = null;
        try {
            zos = new ZipOutputStream(response.getOutputStream());

            // zip all workbooks
            for (Map.Entry<Workbook,String> workbook : workbooks.entrySet()) {
                addToZip(workbook.getValue(),zos,workbook.getKey());
            }

            response.flushBuffer();
        } catch (IOException e) {
            String msg = "Failed to write the catalogue content to the response output stream\n" + e.getMessage();
            log.error(msg, e);
            response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
            try {
                response.getOutputStream().write(msg.getBytes());
            } catch (IOException e1) {
                log.error("Failed to write the error message to the output stream", e);
            }
        } finally {
            try {
                if(zos != null){
                    zos.close();
                }
            } catch (IOException e) {
                log.warn("Failed to close zip output stream");
            }
        }

        log.info("Exported catalogue successfully: uuid {}", catalogueUuid);
    }

    private static void addToZip(String fileName, ZipOutputStream zos, Workbook workbook) throws IOException {
        ByteArrayOutputStream bos = null;
        try {
            ZipEntry zipEntry = new ZipEntry(fileName);
            zos.putNextEntry(zipEntry);

            bos = new ByteArrayOutputStream();
            workbook.write(bos);
            bos.writeTo(zos);
        }finally {
            zos.closeEntry();
            if(bos != null){
                bos.close();
            }
        }
    }
}
