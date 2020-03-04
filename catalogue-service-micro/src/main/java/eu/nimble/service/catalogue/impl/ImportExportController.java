package eu.nimble.service.catalogue.impl;

import eu.nimble.service.catalogue.CatalogueService;
import eu.nimble.service.catalogue.config.RoleConfig;
import eu.nimble.service.catalogue.persistence.util.CataloguePersistenceUtil;
import eu.nimble.service.catalogue.util.SpringBridge;
import eu.nimble.service.model.ubl.catalogue.CatalogueType;
import eu.nimble.service.model.ubl.commonaggregatecomponents.CatalogueLineType;
import eu.nimble.service.model.ubl.commonaggregatecomponents.PartyType;
import eu.nimble.service.model.ubl.commonaggregatecomponents.PersonType;
import eu.nimble.service.model.ubl.commonbasiccomponents.BinaryObjectType;
import eu.nimble.utility.Configuration;
import eu.nimble.utility.ExecutionContext;
import eu.nimble.utility.JsonSerializationUtility;
import eu.nimble.utility.exception.NimbleException;
import eu.nimble.utility.exception.NimbleExceptionMessageCode;
import eu.nimble.utility.validation.IValidationUtil;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import org.apache.poi.ss.usermodel.Workbook;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;
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
    @Autowired
    private ExecutionContext executionContext;

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
        // set request log of ExecutionContext
        String requestLog = "Importing catalogue ...";
        executionContext.setRequestLog(requestLog);
        try {
            log.info(requestLog);
            // validate role
            if(!validationUtil.validateRole(bearerToken, RoleConfig.REQUIRED_ROLES_CATALOGUE)) {
                throw new NimbleException(NimbleExceptionMessageCode.UNAUTHORIZED_INVALID_ROLE.toString());
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
            throw new NimbleException(NimbleExceptionMessageCode.INTERNAL_SERVER_ERROR_IMPORT_CATALOGUE.toString(),Arrays.asList(serializedCatalogue));
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
        // set request log of ExecutionContext
        String requestLog = String.format("Incoming request to export catalogue with uuid %s", catalogueUuid);
        executionContext.setRequestLog(requestLog);

        log.info(requestLog);
        // validate role
        if(!validationUtil.validateRole(bearerToken, RoleConfig.REQUIRED_ROLES_TO_EXPORT_CATALOGUE)) {
            throw new NimbleException(NimbleExceptionMessageCode.UNAUTHORIZED_INVALID_ROLE.toString());
        }

        // check whether the catalogue with the given uuid exists or not
        CatalogueType catalogue;
        try {
            catalogue = service.getCatalogue(catalogueUuid);
        } catch (Exception e) {
            throw new NimbleException(NimbleExceptionMessageCode.INTERNAL_SERVER_ERROR_FAILED_TO_GET_CATALOGUE.toString(),Arrays.asList(catalogueUuid),true);
        }

        // no catalogue for the given uuid
        if (catalogue == null) {
            throw new NimbleException(NimbleExceptionMessageCode.NOT_FOUND_NO_CATALOGUE.toString(),Arrays.asList(catalogueUuid),true);
        }
        // get workbooks
        Map<Workbook,String> workbooks = service.generateTemplateForCatalogue(catalogue,languageId);

        // export catalogue
        ZipOutputStream zos = null;
        try {
            zos = new ZipOutputStream(response.getOutputStream());

            // zip all workbooks
            for (Map.Entry<Workbook,String> workbook : workbooks.entrySet()) {
                addWorkbookImageToZip(workbook.getValue(),zos,workbook.getKey(),null);
            }

            response.flushBuffer();
        } catch (IOException e) {
            throw new NimbleException(NimbleExceptionMessageCode.INTERNAL_SERVER_ERROR_WRITE_CATALOGUE_CONTENT_TO_OUTPUT_STREAM.toString(),true);
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

        // set request log of ExecutionContext
        String requestLog = String.format("Incoming request to export catalogues for party: %s, ids: %s, export all: %s", partyId,ids, exportAll);
        executionContext.setRequestLog(requestLog);

        String idsLog = ids == null ? "" : ids.toString();
        ByteArrayInputStream responseInputStream = null;
        ByteArrayOutputStream responseOutputStream = new ByteArrayOutputStream();
        ZipOutputStream zos = null;

        try {
            log.info(requestLog);
            zos = new ZipOutputStream(response.getOutputStream());

            // validate role
            if(!validationUtil.validateRole(bearerToken, RoleConfig.REQUIRED_ROLES_TO_EXPORT_CATALOGUE)) {
                throw new NimbleException(NimbleExceptionMessageCode.UNAUTHORIZED_INVALID_ROLE.toString());
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
            throw new NimbleException(NimbleExceptionMessageCode.INTERNAL_SERVER_ERROR_EXPORT_CATALOGUE.toString(),Arrays.asList(partyId, idsLog, exportAll.toString()),true);
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
        // get product images
        Map<String,List<BinaryObjectType>> catalogImages = service.getAllImagesFromCatalogue(catalogue);

        // export catalogue
        ZipOutputStream zos = new ZipOutputStream(outputStream);
        try {
            // zip all workbooks
            for (Map.Entry<Workbook,String> workbook : workbooks.entrySet()) {
                addWorkbookImageToZip(workbook.getValue(),zos,workbook.getKey(),null);
            }

            // zip all images
            for (String lineId : catalogImages.keySet()) {
                List<BinaryObjectType> images = catalogImages.get(lineId);
                for (BinaryObjectType catalogImage : images) {
                    String fileName = catalogImage.getFileName();
                    fileName = fileName.startsWith(lineId+".") ? fileName : lineId + "." + fileName;
                    addWorkbookImageToZip(fileName, zos, null,catalogImage.getValue());
                }
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

    private void addWorkbookImageToZip(String fileName, ZipOutputStream zos, Workbook workbook, byte[] value) throws IOException {
        ByteArrayOutputStream bos = null;
        try {
            ZipEntry zipEntry = new ZipEntry(fileName);
            zos.putNextEntry(zipEntry);

            if(workbook == null){
                bos = new ByteArrayOutputStream(value.length);
                bos.write(value);
            }
            else{
                bos = new ByteArrayOutputStream();
                workbook.write(bos);
            }
            bos.writeTo(zos);
        }finally {
            zos.closeEntry();
            if(bos != null){
                bos.close();
            }
        }
    }
}
