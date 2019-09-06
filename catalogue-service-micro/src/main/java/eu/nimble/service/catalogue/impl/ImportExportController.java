package eu.nimble.service.catalogue.impl;

import com.google.common.io.ByteStreams;
import eu.nimble.service.catalogue.CatalogueService;
import eu.nimble.service.catalogue.persistence.util.CataloguePersistenceUtil;
import eu.nimble.service.catalogue.util.SpringBridge;
import eu.nimble.service.model.ubl.catalogue.CatalogueType;
import eu.nimble.service.model.ubl.commonaggregatecomponents.CatalogueLineType;
import eu.nimble.service.model.ubl.commonaggregatecomponents.PartyType;
import eu.nimble.service.model.ubl.commonaggregatecomponents.PersonType;
import eu.nimble.utility.Configuration;
import eu.nimble.utility.HttpResponseUtil;
import eu.nimble.utility.JsonSerializationUtility;
import eu.nimble.utility.validation.IValidationUtil;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import org.apache.commons.io.IOUtils;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.tools.ant.taskdefs.Zip;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.logging.LogLevel;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import sun.rmi.runtime.Log;

import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayInputStream;
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
    @Autowired
    private IValidationUtil validationUtil;

    @CrossOrigin(origins = {"*"})
    @ApiOperation(value = "", notes = "This service imports the provided UBL catalogue. The service replaces the PartyType" +
            " information in the given catalogue with the PartyType obtained from the currently configured identity service." +
            " Party information is deduced from the authorization token of the user issuing the call.")
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
            // validate role
            if(!validationUtil.validateRole(bearerToken, CatalogueController.REQUIRED_ROLES_CATALOGUE)) {
                return HttpResponseUtil.createResponseEntityAndLog("Invalid role", HttpStatus.UNAUTHORIZED);
            }

            // remove hjid fields of catalogue
            JSONObject object = new JSONObject(serializedCatalogue);
            JsonSerializationUtility.removeHjidFields(object);
            CatalogueType catalogue = JsonSerializationUtility.getObjectMapper().readValue(object.toString(), CatalogueType.class);

            // get person using the given bearer token
            PersonType person = SpringBridge.getInstance().getiIdentityClientTyped().getPerson(bearerToken);
            // get party for the person
            PartyType party = SpringBridge.getInstance().getiIdentityClientTyped().getPartyByPersonID(person.getID()).get(0);

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
            @ApiResponse(code = 200, message = "Exported the catalogue successfully"),
            @ApiResponse(code = 401, message = "Invalid token. No user was found for the provided token"),
            @ApiResponse(code = 500, message = "Unexpected error while exporting catalogue")
    })
    @RequestMapping(value = "/catalogue/export/{uuid}",
            method = RequestMethod.GET,
            produces = {"application/zip"})
    public void exportCatalogue(
            @ApiParam(value = "Identifier of the catalogue to be exported", required = true) @PathVariable("uuid") String catalogueUuid,
            @ApiParam(value = "language id", required = true) @RequestParam("languageId") String languageId,
            @ApiParam(value = "The Bearer token provided by the identity service", required = true) @RequestHeader(value = "Authorization", required = true) String bearerToken,
            HttpServletResponse response) {

        log.info("Incoming request to export catalogue with uuid {}", catalogueUuid);
        // validate role
        if(!validationUtil.validateRole(bearerToken, CatalogueController.REQUIRED_ROLES_CATALOGUE)) {
            HttpResponseUtil.writeMessageServletResponseAndLog(response, "Invalid role", HttpStatus.UNAUTHORIZED);
            return;
        }

        // check whether the catalogue with the given uuid exists or not
        CatalogueType catalogue;
        try {
            catalogue = service.getCatalogue(catalogueUuid);
        } catch (Exception e) {
            String msg = "Failed to get catalogue for uuid: "+catalogueUuid + "\n" + e.getMessage();
            HttpResponseUtil.writeMessageServletResponseAndLog(response, msg, e, HttpStatus.INTERNAL_SERVER_ERROR, LogLevel.ERROR);
            return;
        }

        // no catalogue for the given uuid
        if (catalogue == null) {
            String msg = "No catalogue for uuid: " + catalogueUuid;
            HttpResponseUtil.writeMessageServletResponseAndLog(response, msg, null, HttpStatus.NOT_FOUND, LogLevel.INFO);
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
                addWorkbookToZip(workbook.getValue(),zos,workbook.getKey());
            }

            response.flushBuffer();
        } catch (IOException e) {
            String msg = String.format("Failed to write the catalogue content to the response output stream: %s", e.getMessage());
            HttpResponseUtil.writeMessageServletResponseAndLog(response, msg, e, HttpStatus.INTERNAL_SERVER_ERROR, LogLevel.ERROR);

        } finally {
            try {
                if(zos != null){
                    zos.close();
                }
            } catch (IOException e) {
                log.warn("Failed to close zip output stream", e);
            }
        }

        log.info("Exported catalogue successfully: uuid {}", catalogueUuid);
    }

    @CrossOrigin(origins = {"*"})
    @ApiOperation(value = "", notes = "Exports the specified catalogues. Concerning a single catalogue, the products are exported to separate " +
            "Excel sheets according to the commodity classifications (categories). Each distinct combination of categories " +
            "of products are exported to a separate sheet. The sheets are collected into a ZIP-compressed file." +
            " A dedicated ZIP file is created for each catalogue.")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Exported catalogues successfully"),
            @ApiResponse(code = 401, message = "Invalid token. No user was found for the provided token"),
            @ApiResponse(code = 500, message = "Unexpected error while exporting catalogue")
    })
    @RequestMapping(value = "/catalogue/export",
            method = RequestMethod.GET,
            produces = {"application/zip"})
    public void exportCatalogues(
            @ApiParam(value = "Identifier of the party for which the catalogues to be exported", required = true) @RequestParam(value = "partyId", required = true) String partyId,
            @ApiParam(value = "An indicator for selecting all the catalogues to be exported. ", required = false) @RequestParam(value = "exportAll", required = false, defaultValue = "false") Boolean exportAll,
            @ApiParam(value = "Identifier of the catalogues to be exported. (catalogue.id)", required = false) @RequestParam(value = "ids", required = false) List<String> ids,
            @ApiParam(value = "language id", required = true) @RequestParam("languageId") String languageId,
            @ApiParam(value = "The Bearer token provided by the identity service", required = true) @RequestHeader(value = "Authorization", required = true) String bearerToken,
            HttpServletResponse response) {

        String idsLog = ids == null ? "" : ids.toString();
        ByteArrayInputStream responseInputStream = null;
        ByteArrayOutputStream responseOutputStream = new ByteArrayOutputStream();
        ZipOutputStream zos = null;

        try {
            log.info("Incoming request to export catalogues for party: {}, ids: {}, export all: {}", partyId, idsLog, exportAll);
            zos = new ZipOutputStream(response.getOutputStream());

            // validate role
            if(!validationUtil.validateRole(bearerToken, CatalogueController.REQUIRED_ROLES_CATALOGUE)) {
                HttpResponseUtil.writeMessageServletResponseAndLog(response, "Invalid role", HttpStatus.UNAUTHORIZED);
                return;
            }


            // if all the catalogues is requested to be deleted get the identifiers first
            if(exportAll) {
                ids = CataloguePersistenceUtil.getCatalogueIdListsForParty(partyId);
            }

            for (String id : ids) {
                CatalogueType catalogue = CataloguePersistenceUtil.getCatalogueForParty(id, partyId);
                ByteArrayOutputStream catalogueBaos = new ByteArrayOutputStream();
                getZipForCatalogue(catalogue, languageId, catalogueBaos);

                try {
                    ZipEntry zipEntry = new ZipEntry(catalogue.getID() + ".zip");
                    zos.putNextEntry(zipEntry);
                    catalogueBaos.writeTo(zos);

                } catch (IOException e) {
                    log.warn("Failed to write catalogue output stream to response output stream for catalogue: id{}, uuid: {}", catalogue.getID(), catalogue.getUUID(), e);

                } finally {
                    try {
                        zos.closeEntry();
                    } catch (IOException e) {
                        log.warn("Failed to close zip entry for catalogue: id{}, uuid: {}", catalogue.getID(), catalogue.getUUID(), e);
                    }
                    try {
                        catalogueBaos.close();
                    } catch (IOException e) {
                        log.warn("Failed to close catalogue output stream for catalogue: id{}, uuid: {}", catalogue.getID(), catalogue.getUUID(), e);
                    }
                }
            }

            response.flushBuffer();
            log.info("Completed request to delete catalogues for party: {}, ids: {}, delete all: {}", partyId, idsLog, exportAll);

        } catch(Exception e) {
            String msg = String.format("Unexpected error while deleting catalogues for party: %s ids: %s, delete all: %b", partyId, idsLog, exportAll);
            log.error(msg, e);
            response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
            try {
                response.getOutputStream().write(msg.getBytes());
            } catch (IOException e1) {
                log.error("Failed to write the error message to the output stream", e);
            }

        } finally {
            if(zos != null) {
                try {
                    // zos also closes the underlying output stream i.e. responseOutputStream
                    zos.close();
                } catch (IOException e) {
                    log.warn("Failed to close zip output stream", e);
                }
            }
        }
    }

    private void getZipForCatalogue(CatalogueType catalogue, String languageId, ByteArrayOutputStream outputStream) {
        // get workbooks
        Map<Workbook,String> workbooks = service.generateTemplateForCatalogue(catalogue,languageId);

        // export catalogue
        ZipOutputStream zos = new ZipOutputStream(outputStream);
        try {
            // zip all workbooks
            for (Map.Entry<Workbook,String> workbook : workbooks.entrySet()) {
                addWorkbookToZip(workbook.getValue(),zos,workbook.getKey());
            }

        } catch (IOException e) {
            log.error("Failed to write the catalogue content to the zip output stream for catalogue id: {}, uuid: {}", catalogue.getID(), catalogue.getUUID(), e);

        } finally {
            try {
                zos.close();
            } catch (IOException e) {
                log.warn("Failed to close zip output stream for catalogue id:{}, uuid: {}", catalogue.getID(), catalogue.getUUID(), e);
            }
        }
    }

    private void addWorkbookToZip(String fileName, ZipOutputStream zos, Workbook workbook) throws IOException {
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
