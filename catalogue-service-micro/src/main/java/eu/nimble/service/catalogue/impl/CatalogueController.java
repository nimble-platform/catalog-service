package eu.nimble.service.catalogue.impl;

import com.sun.tools.doclets.formats.html.resources.standard;
import eu.nimble.data.transformer.ontmalizer.XML2OWLMapper;
import eu.nimble.service.catalogue.CatalogueService;
import eu.nimble.service.catalogue.exception.CatalogueServiceException;
import eu.nimble.service.catalogue.model.catalogue.CatalogueLineSortOptions;
import eu.nimble.service.catalogue.model.catalogue.CataloguePaginationResponse;
import eu.nimble.service.catalogue.persistence.util.CatalogueDatabaseAdapter;
import eu.nimble.service.catalogue.persistence.util.CataloguePersistenceUtil;
import eu.nimble.service.catalogue.persistence.util.LockPool;
import eu.nimble.service.catalogue.util.CatalogueEvent;
import eu.nimble.service.catalogue.util.SemanticTransformationUtil;
import eu.nimble.service.catalogue.validation.CatalogueValidator;
import eu.nimble.service.catalogue.validation.ValidationException;
import eu.nimble.service.model.modaml.catalogue.TEXCatalogType;
import eu.nimble.service.model.ubl.catalogue.CatalogueType;
import eu.nimble.service.model.ubl.commonaggregatecomponents.PartyNameType;
import eu.nimble.service.model.ubl.commonaggregatecomponents.PartyType;
import eu.nimble.utility.*;
import eu.nimble.utility.exception.BinaryContentException;
import eu.nimble.utility.persistence.resource.ResourceValidationUtility;
import eu.nimble.utility.serialization.TransactionEnabledSerializationUtility;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import org.apache.poi.ss.usermodel.Workbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.logging.LogLevel;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;
import java.util.zip.ZipInputStream;

/**
 * Catalogue level REST services. A catalogue is a collection of products or services on which various business processes
 * can be executed. A catalogue contains contains catalogue lines each of which corresponds to a product or service.
 */
@Controller
//@Transactional(transactionManager = "ubldbTransactionManager")
public class CatalogueController {

    private String defaultLanguage = "en";

    private static Logger log = LoggerFactory
            .getLogger(CatalogueController.class);

    @Autowired
    private CatalogueService service;
    @Autowired
    private TransactionEnabledSerializationUtility serializationUtility;
    @Autowired
    private ResourceValidationUtility resourceValidationUtil;
    @Autowired
    private LockPool lockPool;

    @CrossOrigin(origins = {"*"})
    @ApiOperation(value = "", notes = "Retrieves the default CataloguePaginationResponse for the specified party.")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Retrieved CataloguePaginationResponse for the specified party successfully", response = CataloguePaginationResponse.class),
            @ApiResponse(code = 400, message = "Both language id and search text should be provided"),
            @ApiResponse(code = 401, message = "Invalid token. No user was found for the provided token"),
            @ApiResponse(code = 404, message = "No default catalogue for the party"),
            @ApiResponse(code = 500, message = "Failed to get CataloguePaginationResponse for the party")
    })
    @RequestMapping(value = "/catalogue/{partyId}/pagination/{catalogueId}",
            produces = {"application/json"},
            method = RequestMethod.GET)
    public ResponseEntity getDefaultCataloguePagination(@ApiParam(value = "Identifier of the party for which the catalogue to be retrieved", required = true) @PathVariable String partyId,
                                                        @ApiParam(value = "Identifier of the catalogue", required = true) @PathVariable String catalogueId,
                                                        @ApiParam(value = "Number of catalogue lines to be included in CataloguePaginationResponse ") @RequestParam(value = "limit",required = true) Integer limit,
                                                        @ApiParam(value = "Offset of the first catalogue line among all catalogue lines of the default catalogue for the party") @RequestParam(value = "offset",required = true) Integer offset,
                                                        @ApiParam(value = "Text to be used to filter the catalogue lines.Item name and description will be searched for the given text.") @RequestParam(value = "searchText",required = false) String searchText,
                                                        @ApiParam(value = "Identifier for the language of search text such as en and tr") @RequestParam(value = "languageId",required = false) String languageId,
                                                        @ApiParam(value = "Name of the category which is used to filter catalogue lines.Catalogue lines are added to the response if and only if they contain the given category.") @RequestParam(value = "categoryName",required = false) String categoryName,
                                                        @ApiParam(value = "Option used to sort catalogue lines") @RequestParam(value = "sortOption",required = false) CatalogueLineSortOptions sortOption,
                                                        @ApiParam(value = "The Bearer token provided by the identity service", required = true) @RequestHeader(value = "Authorization", required = true) String bearerToken) {
        log.info("Incoming request to get CataloguePaginationResponse for party: {}, catalogue id: {} with limit: {}, offset: {}", partyId, catalogueId, limit, offset);
        ResponseEntity tokenCheck = eu.nimble.service.catalogue.util.HttpResponseUtil.checkToken(bearerToken);
        if (tokenCheck != null) {
            return tokenCheck;
        }

        // check the validity of the request params
        if(searchText != null && languageId == null){
            return createErrorResponseEntity("Both language id and search text should be provided", HttpStatus.BAD_REQUEST, null);
        }

        CataloguePaginationResponse cataloguePaginationResponse;

        try {
            cataloguePaginationResponse = service.getCataloguePaginationResponse(catalogueId, partyId,categoryName,searchText,languageId,sortOption,limit,offset);
        } catch (Exception e) {
            return createErrorResponseEntity(String.format("Failed to get CataloguePaginationResponse for party id: %s catalogue id: %s", partyId, catalogueId), HttpStatus.INTERNAL_SERVER_ERROR, e);
        }
        if (cataloguePaginationResponse.getCatalogueUuid() == null) {
            log.info("No catalogue for party: {}, catalogue: {}", partyId, catalogueId);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(String.format("No catalogue for party: %s, catalogue id: %s", partyId, catalogueId));
        }

        log.info("Completed request to get CataloguePaginationResponse for party: {}, catalogue id: {} with limit: {}, offset: {}", partyId, catalogueId, limit, offset);
        return ResponseEntity.ok(serializationUtility.serializeUBLObject(cataloguePaginationResponse));
    }


    @CrossOrigin(origins = {"*"})
    @ApiOperation(value = "", notes = "Retrieves the catalogue for the given standard and uuid.")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Retrieved the catalogue successfully", response = CatalogueType.class),
            @ApiResponse(code = 400, message = "Invalid standard"),
            @ApiResponse(code = 401, message = "Invalid token. No user was found for the provided token"),
            @ApiResponse(code = 404, message = "No catalogue for the given uuid and standard"),
            @ApiResponse(code = 500, message = "Unexpected error while getting catalogue")
    })
    @RequestMapping(value = "/catalogue/{standard}/{uuid}",
            produces = {"application/json"},
            method = RequestMethod.GET)
    public ResponseEntity getCatalogue(@ApiParam(value = "Data model standard that the provided catalogue is compatible with.", defaultValue = "ubl", required = true) @PathVariable String standard,
                                       @ApiParam(value = "uuid of the catalogue to be retrieved.", required = true) @PathVariable String uuid,
                                       @ApiParam(value = "The Bearer token provided by the identity service", required = true) @RequestHeader(value = "Authorization", required = true) String bearerToken) {
        log.info("Incoming request to get catalogue for standard: {}, uuid: {}", standard, uuid);
        ResponseEntity tokenCheck = eu.nimble.service.catalogue.util.HttpResponseUtil.checkToken(bearerToken);
        if (tokenCheck != null) {
            return tokenCheck;
        }

        Configuration.Standard std;
        try {
            std = getStandardEnum(standard);
        } catch (Exception e) {
            return createErrorResponseEntity("Invalid standard: " + standard, HttpStatus.BAD_REQUEST, e);
        }
        Object catalogue;
        try {
            catalogue = service.getCatalogue(uuid, std);
        } catch (Exception e) {
            return createErrorResponseEntity("Failed to get catalogue for standard: " + standard + " uuid: " + uuid, HttpStatus.INTERNAL_SERVER_ERROR, e);
        }

        if (catalogue == null) {
            log.info("No catalogue for uuid: {}", uuid);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(String.format("No catalogue for uuid: %s", uuid));
        }

        log.info("Completed request to get catalogue for standard: {}, uuid: {}", standard, uuid);
        return ResponseEntity.ok(serializationUtility.serializeUBLObject(catalogue));
    }

    private <T> T parseCatalogue(String contentType, String serializedCatalogue, Configuration.Standard standard) throws IOException {
        T catalogue = null;
        if (contentType.contentEquals(MediaType.APPLICATION_XML_VALUE)) {
            if (standard == Configuration.Standard.UBL) {
                CatalogueType ublCatalogue = (CatalogueType) JAXBUtility.deserialize(serializedCatalogue, Configuration.UBL_CATALOGUE_PACKAGENAME);
                catalogue = (T) ublCatalogue;

            } else if (standard == Configuration.Standard.MODAML) {
                catalogue = (T) JAXBUtility.deserialize(serializedCatalogue, Configuration.MODAML_CATALOGUE_PACKAGENAME);
            }

        } else if (contentType.contentEquals(MediaType.APPLICATION_JSON_VALUE)) {
            catalogue = (T) JsonSerializationUtility.getObjectMapper().readValue(serializedCatalogue, CatalogueType.class);
        }
        return catalogue;
    }

    private ResponseEntity createCreatedCatalogueResponse(Object catalogue, String baseUrl) {
        String uuid;
        String partyName="";
        if (catalogue instanceof CatalogueType) {
            CatalogueType catalogueObject = (CatalogueType) catalogue;
            if (catalogueObject.getProviderParty() != null) {
                List<PartyNameType> partyNameTypes = catalogueObject.getProviderParty().getPartyName();
                partyName = UblUtil.getName(partyNameTypes);
            }
            uuid = catalogueObject.getUUID().toString();
        } else {
            uuid = ((TEXCatalogType) catalogue).getTCheader().getMsgID();
        }
        URI catalogueURI;
        try {

            catalogueURI = new URI(baseUrl + uuid);
        } catch (URISyntaxException e) {
            String msg = "Failed to generate a URI for the newly created item";
            log.warn(msg, e);
            try {
                log.info("Completed request to add catalogue with an empty URI, uuid: {}", uuid);
                return ResponseEntity.created(new URI("")).body(catalogue);
            } catch (URISyntaxException e1) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("WTF");
            }
        }
        Map<String,String> paramMap = new HashMap<String, String>();
        paramMap.put("companyName",partyName);
        paramMap.put("catalogueId",uuid);
        paramMap.put("activity", CatalogueEvent.CATALOGUE_CREATE.getActivity());
        LoggerUtils.logWithMDC(log, paramMap, LoggerUtils.LogLevel.INFO, "Completed request to add catalogue, uuid: {}", uuid);
        return ResponseEntity.created(catalogueURI).body(serializationUtility.serializeUBLObject(catalogue));
    }

    @CrossOrigin(origins = {"*"})
    @ApiOperation(value = "", notes = "Adds the catalogue passed in a serialized form. The serialized catalogue should be compliant with the specified standard.")
    @ApiResponses(value = {
            @ApiResponse(code = 201, message = "Provided catalogue has been persisted."),
            @ApiResponse(code = 401, message = "Invalid token. No user was found for the provided token"),
            @ApiResponse(code = 400, message = "Invalid standard or invalid content"),
            @ApiResponse(code = 409, message = "A catalogue with the same id exists for the publisher party"),
            @ApiResponse(code = 500, message = "Unexpected error while publishing the catalogue")
    })
    @RequestMapping(value = "/catalogue/{standard}",
            consumes = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE},
            produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE},
            method = RequestMethod.POST)
    public <T> ResponseEntity addCatalogue(@ApiParam(value = "Data model standard that the provided catalogue is compatible with.", defaultValue = "ubl", required = true) @PathVariable String standard,
                                           @ApiParam(value = "Serialized form of the catalogue. Valid serializations can be achieved via JsonSerializationUtility.getObjectMapper method located in the utility module. An example catalogue serialization can be found in: https://github.com/nimble-platform/catalog-service/tree/staging/catalogue-service-micro/src/main/resources/example_content/catalogue.json", required = true) @RequestBody String serializedCatalogue,
                                           @ApiParam(value = "The Bearer token provided by the identity service", required = true) @RequestHeader(value = "Authorization", required = true) String bearerToken, HttpServletRequest request) {
        try {
            log.info("Incoming request to post catalogue with standard: {} standard", standard);
            ResponseEntity tokenCheck = eu.nimble.service.catalogue.util.HttpResponseUtil.checkToken(bearerToken);
            if (tokenCheck != null) {
                return tokenCheck;
            }

            // get standard
            Configuration.Standard std;
            try {
                std = getStandardEnum(standard);
            } catch (Exception e) {
                return createErrorResponseEntity("Invalid standard: " + standard, HttpStatus.BAD_REQUEST, e);
            }

            String contentType = request.getContentType();
            // get catalogue
            T catalogue;
            try {
                catalogue = parseCatalogue(contentType, serializedCatalogue, std);
            } catch (Exception e) {
                return createErrorResponseEntity(String.format("Failed to deserialize catalogue: %s", serializedCatalogue), HttpStatus.BAD_REQUEST, e);
            }
            // for ubl catalogues, do the following validations
            if (std.equals(Configuration.Standard.UBL)) {
                CatalogueType ublCatalogue = (CatalogueType) catalogue;
                // validate the content of the catalogue
                CatalogueValidator catalogueValidator = new CatalogueValidator(ublCatalogue);
                try {
                    catalogueValidator.validate();
                } catch (ValidationException e) {
                    return HttpResponseUtil.createResponseEntityAndLog(e.getMessage(), e, HttpStatus.BAD_REQUEST, LogLevel.INFO);
                }

                // check catalogue with the same id exists
                boolean catalogueExists = CataloguePersistenceUtil.checkCatalogueExistenceById(ublCatalogue.getID(), ublCatalogue.getProviderParty().getPartyIdentification().get(0).getID());
                if (catalogueExists) {
                    return HttpResponseUtil.createResponseEntityAndLog(String.format("Catalogue with ID: '%s' already exists for the publishing party", ublCatalogue.getID()), null, HttpStatus.CONFLICT, LogLevel.INFO);
                }

                // check the entity ids
                boolean hjidsExists = resourceValidationUtil.hjidsExit(catalogue);
                if (hjidsExists) {
                    return HttpResponseUtil.createResponseEntityAndLog(String.format("Entity IDs (hjid fields) found in the passed catalogue: %s. Make sure they are null", serializedCatalogue), null, HttpStatus.BAD_REQUEST, LogLevel.INFO);
                }
            }

            catalogue = service.addCatalogue(catalogue, std);
            return createCreatedCatalogueResponse(catalogue, HttpResponseUtil.baseUrl(request));

        } catch (Exception e) {
            return HttpResponseUtil.createResponseEntityAndLog(String.format("Unexpected error while adding the catalogue: %s", serializedCatalogue), e, HttpStatus.INTERNAL_SERVER_ERROR, LogLevel.ERROR);
        }
    }

    /**
     * @param catalogueJson
     * @return <li>200 along with the updated catalogue</li>
     * <li>400 in case of an invalid standard or invalid catalogue serialization</li>
     * <li>501 if a standard than ubl is passed</li>
     */
    @CrossOrigin(origins = {"*"})
    @ApiOperation(value = "", notes = "Updates the catalogue represented in JSON serialization. The serialization should be compliant with the UBL standard.")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Updated the catalogue successfully", response = CatalogueType.class),
            @ApiResponse(code = 400, message = "Invalid standard"),
            @ApiResponse(code = 500, message = "Failed to update the catalogue")
    })
    @RequestMapping(value = "/catalogue/{standard}",
            consumes = {"application/json"},
            produces = {"application/json"},
            method = RequestMethod.PUT)
    public ResponseEntity updateCatalogue(@ApiParam(value = "Data model standard that the provided catalogue is compatible with.", defaultValue = "ubl", required = true) @PathVariable String standard,
                                          @ApiParam(value = "Serialized form of the catalogue. Valid serializations can be achieved via JsonSerializationUtility.getObjectMapper method located in the utility module. An example catalogue serialization can be found in: https://github.com/nimble-platform/catalog-service/tree/staging/catalogue-service-micro/src/main/resources/example_content/catalogue.json", required = true) @RequestBody String catalogueJson,
                                          @ApiParam(value = "The Bearer token provided by the identity service", required = true) @RequestHeader(value = "Authorization", required = true) String bearerToken) {
        try {
            log.info("Incoming request to update catalogue");
            ResponseEntity tokenCheck = eu.nimble.service.catalogue.util.HttpResponseUtil.checkToken(bearerToken);
            if (tokenCheck != null) {
                return tokenCheck;
            }

            // check standard
            Configuration.Standard std;
            try {
                std = getStandardEnum(standard);
                if (std != Configuration.Standard.UBL) {
                    String msg = "Update operation is not support for " + standard;
                    log.info(msg);
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(msg);
                }
            } catch (Exception e) {
                return createErrorResponseEntity("Invalid standard: " + standard, HttpStatus.BAD_REQUEST, e);
            }

            // parse catalogue
            CatalogueType catalogue;
            try {
                catalogue = JsonSerializationUtility.getObjectMapper().readValue(catalogueJson, CatalogueType.class);
            } catch (IOException e) {
                return createErrorResponseEntity(String.format("Failed to deserialize catalogue: %s", catalogueJson), HttpStatus.INTERNAL_SERVER_ERROR, e);
            }

            // validate the catalogue content
            CatalogueValidator catalogueValidator = new CatalogueValidator(catalogue);
            try {
                catalogueValidator.validate();
            } catch (ValidationException e) {
                return HttpResponseUtil.createResponseEntityAndLog(e.getMessage(), e, HttpStatus.BAD_REQUEST, LogLevel.INFO);
            }

            // validate the entity ids
            boolean hjidsBelongToCompany = resourceValidationUtil.hjidsBelongsToParty(catalogue, catalogue.getProviderParty().getPartyIdentification().get(0).getID(), Configuration.Standard.UBL.toString());
            if (!hjidsBelongToCompany) {
                return HttpResponseUtil.createResponseEntityAndLog(String.format("Some of the identifiers (hjid fields) do not belong to the party in the passed catalogue: %s", catalogueJson), null, HttpStatus.BAD_REQUEST, LogLevel.INFO);
            }
            String catalogueId = "";
            String partyName = "";
            try {
                catalogue = service.updateCatalogue(catalogue);
                if (catalogue.getProviderParty() != null) {
                    List<PartyNameType> partyNameTypes = catalogue.getProviderParty().getPartyName();
                    partyName = UblUtil.getName(partyNameTypes);
                }
                if (catalogue.getUUID() != null) {
                    catalogueId = catalogue.getUUID().toString();
                }
                Map<String,String> paramMap = new HashMap<String, String>();
                paramMap.put("activity", CatalogueEvent.CATALOGUE_UPDATE.getActivity());
                paramMap.put("catalogueId", catalogueId);
                paramMap.put("companyName", partyName);
                LoggerUtils.logWithMDC(log, paramMap, LoggerUtils.LogLevel.INFO, "Successfully updated catalogue, id: {}", catalogueId);
            } catch (Exception e) {
                log.warn("Failed to update the following catalogue: {}", catalogueJson);
                return createErrorResponseEntity("Failed to update the catalogue", HttpStatus.INTERNAL_SERVER_ERROR, e);
            }

            log.info("Completed request to update the catalogue. uuid: {}", catalogue.getUUID());
            return ResponseEntity.ok(serializationUtility.serializeUBLObject(catalogue));

        } catch (Exception e) {
            return HttpResponseUtil.createResponseEntityAndLog(String.format("Unexpected error while adding the catalogue: %s", catalogueJson), e, HttpStatus.INTERNAL_SERVER_ERROR, LogLevel.ERROR);
        }
    }

    @CrossOrigin(origins = {"*"})
    @ApiOperation(value = "", notes = "Deletes the specified catalogue")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Specified catalogue has been deleted successfully"),
            @ApiResponse(code = 400, message = "Invalid standard"),
            @ApiResponse(code = 500, message = "Unexpected error while deleting the catalogue")
    })
    @RequestMapping(value = "/catalogue/{standard}/{uuid}",
            method = RequestMethod.DELETE)
    public ResponseEntity deleteCatalogue(@ApiParam(value = "Data model standard that the provided catalogue is compatible with.", defaultValue = "ubl", required = true) @PathVariable String standard,
                                          @ApiParam(value = "uuid of the catalogue to be retrieved.", required = true) @PathVariable(value = "uuid", required = true) String uuid,
                                          @ApiParam(value = "The Bearer token provided by the identity service", required = true) @RequestHeader(value = "Authorization", required = true) String bearerToken) {
        log.info("Incoming request to delete catalogue with uuid: {}", uuid);
        ResponseEntity tokenCheck = eu.nimble.service.catalogue.util.HttpResponseUtil.checkToken(bearerToken);
        if (tokenCheck != null) {
            return tokenCheck;
        }

        Configuration.Standard std;
        try {
            std = getStandardEnum(standard);
        } catch (Exception e) {
            return createErrorResponseEntity("Invalid standard: " + standard, HttpStatus.BAD_REQUEST, e);
        }

        try {
            service.deleteCatalogue(uuid, std);
            Map<String,String> paramMap = new HashMap<String, String>();
            paramMap.put("activity", CatalogueEvent.CATALOGUE_DELETE.getActivity());
            paramMap.put("catalogueId", uuid);
            LoggerUtils.logWithMDC(log, paramMap, LoggerUtils.LogLevel.INFO, "Successfully deleted catalogue, id: {}", uuid);
        } catch (Exception e) {
            return createErrorResponseEntity("Failed to delete catalogue", HttpStatus.INTERNAL_SERVER_ERROR, e);
        }

        log.info("Completed request to delete catalogue with uuid: {}", uuid);
        return ResponseEntity.status(HttpStatus.OK).build();
    }

    @CrossOrigin(origins = {"*"})
    @ApiOperation(value = "", notes = "Deletes the specified catalogues")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Specified catalogue has been deleted successfully"),
            @ApiResponse(code = 401, message = "Invalid token"),
            @ApiResponse(code = 500, message = "Unexpected error while deleting the catalogues")
    })
    @RequestMapping(value = "/catalogue",
            method = RequestMethod.DELETE)
    public ResponseEntity deleteCataloguesForParty(@ApiParam(value = "Identifier of the party for which the catalogues to be deleted", required = true) @RequestParam(value = "partyId", required = true) String partyId,
                                                   @ApiParam(value = "An indicator for selecting all the catalogues to be deleted. ", required = false) @RequestParam(value = "deleteAll", required = false, defaultValue = "false") Boolean deleteAll,
                                                   @ApiParam(value = "Identifier of the catalogues to be deleted. (catalogue.id)", required = false) @RequestParam(value = "ids", required = false) List<String> ids,
                                                   @ApiParam(value = "The Bearer token provided by the identity service", required = true) @RequestHeader(value = "Authorization", required = true) String bearerToken) {
        String idsLog = ids == null ? "" : ids.toString();
        try {
            log.info("Incoming request to delete catalogues for party: {}, ids: {}, delete all: {}", partyId, idsLog, deleteAll);
            ResponseEntity tokenCheck = eu.nimble.service.catalogue.util.HttpResponseUtil.checkToken(bearerToken);
            if (tokenCheck != null) {
                return tokenCheck;
            }

            // if all the catalogues is requested to be deleted get the identifiers first
            if(deleteAll) {
                ids = CataloguePersistenceUtil.getCatalogueIdListsForParty(partyId);
            }

            for (String id : ids) {
                service.deleteCatalogue(id, partyId);
                Map<String,String> paramMap = new HashMap<String, String>();
                paramMap.put("activity", CatalogueEvent.CATALOGUE_DELETE.getActivity());
                paramMap.put("catalogueId", id);
                paramMap.put("companyId", partyId);
                LoggerUtils.logWithMDC(log, paramMap, LoggerUtils.LogLevel.INFO, "Successfully deleted catalogue, id: {} for company: {}", id, partyId);
            }

            log.info("Completed request to delete catalogues for party: {}, ids: {}, delete all: {}", partyId, idsLog, deleteAll);
            return ResponseEntity.status(HttpStatus.OK).build();

        } catch(Exception e) {
            return HttpResponseUtil.createResponseEntityAndLog(String.format("Unexpected error while deleting catalogues for party: %s ids: %s, delete all: %b", partyId, idsLog, deleteAll), e, HttpStatus.INTERNAL_SERVER_ERROR, LogLevel.ERROR);
        }
    }

    @CrossOrigin(origins = {"*"})
    @ApiOperation(value = "", notes = "Generates an excel-based template for the specified categories. Category ids and " +
            "taxonomy ids must be provided in comma separated manner and they must have the same number of elements. " +
            "Taxonomy id must be provided such that they specify the taxonomies including the specified categories. See " +
            "the examples in parameter definitions.")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Generated the template for the given categories and taxonomy ids", response = CatalogueType.class),
            @ApiResponse(code = 401, message = "Invalid token. No user was found for the provided token"),
            @ApiResponse(code = 500, message = "Unexpected error while generating template"),
    })
    @RequestMapping(value = "/catalogue/template",
            method = RequestMethod.GET,
            produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    public void downloadTemplate(
            @ApiParam(value = "Category ids for which the properties to be generated in the template. Examples: http://www.aidimme.es/FurnitureSectorOntology.owl#MDFBoard,0173-1#01-ACH237#011", required = true) @RequestParam("categoryIds") List<String> categoryIds,
            @ApiParam(value = "Taxonomy ids corresponding to the categories specified in the categoryIds parameter", required = true) @RequestParam("taxonomyIds") List<String> taxonomyIds,
            @ApiParam(value = "The Bearer token provided by the identity service", required = true) @RequestHeader(value = "Authorization", required = true) String bearerToken,
            @RequestParam("templateLanguage") String templateLanguage,
            HttpServletResponse response) {
        log.info("Incoming request to generate a template. Category ids: {}, taxonomy ids: {}", categoryIds, taxonomyIds);
        ResponseEntity tokenCheck = eu.nimble.service.catalogue.util.HttpResponseUtil.checkToken(bearerToken);
        if (tokenCheck != null) {
            return;
        }

        Workbook template;
        try {
            template = service.generateTemplateForCategory(categoryIds, taxonomyIds,templateLanguage);
        } catch (Exception e) {
            String msg = "Failed to generate template\n" + e.getMessage();
            log.error(msg, e);
            response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
            try {
                response.getOutputStream().write(msg.getBytes());
            } catch (IOException e1) {
                log.error("Failed to write the error message to the output stream", e);
            }
            return;
        }

        try {
            String fileName = "product_data_template.xlsx";
            response.setHeader("Content-disposition", "attachment; filename=" + fileName);
            response.addHeader("Access-Control-Expose-Headers", "Content-Disposition");
            template.write(response.getOutputStream());
            response.flushBuffer();
            log.info("Completed the request to generate template");
        } catch (IOException e) {
            String msg = "Failed to write the template content to the response output stream\n" + e.getMessage();
            log.error(msg, e);
            response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
            try {
                response.getOutputStream().write(msg.getBytes());
            } catch (IOException e1) {
                log.error("Failed to write the error message to the output stream", e);
            }
        }
    }

    @CrossOrigin(origins = {"*"})
    @ApiOperation(value = "", notes = "Adds the catalogue specified with the provided template. The created catalogue is compliant with the default " +
            "standard, which is UBL. If there is a published catalogue already, the type of update is realized according to " +
            "the update mode. There are two update modes: append and replace. In the former mode, if some of the products were " +
            "already published, they are replaced with the new ones; furthermore, the brand new ones are appended to the " +
            "existing product list. In the latter mode, all previously published products having the same categories specified in the template are deleted," +
            "the new list of products is set as it is.")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Persisted uploaded template successfully and returned the corresponding catalogue", response = CatalogueType.class),
            @ApiResponse(code = 400, message = "Invalid template content"),
            @ApiResponse(code = 401, message = "Invalid token. No user was found for the provided token"),
            @ApiResponse(code = 500, message = "Unexpected error while uploading the template")
    })
    @RequestMapping(value = "/catalogue/template/upload",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE,
            method = RequestMethod.POST)
    public ResponseEntity uploadTemplate(
            @ApiParam(value = "Filled in excel-based template. An example filled in template can be found in: https://github.com/nimble-platform/catalog-service/tree/staging/catalogue-service-micro/src/main/resources/example_content/product_data_template.xlsx . Check the \"Information\" tab for detailed instructions. Furthermore, example instantiations can be found in the \"Product Properties Example\" and \"Trading and Delivery Terms Example\" tabs.", required = true) @RequestParam("file") MultipartFile file,
            @ApiParam(value = "Upload mode for the catalogue. Possible options are: append and replace", defaultValue = "append") @RequestParam(value = "uploadMode", defaultValue = "append") String uploadMode,
            @ApiParam(value = "Identifier of the party for which the catalogue will be published", required = true) @RequestParam("partyId") String partyId,
            @ApiParam(value = "The Bearer token provided by the identity service", required = true) @RequestHeader(value = "Authorization", required = true) String bearerToken,
            HttpServletRequest request) {
        try {
            log.info("Incoming request to upload template upload mode: {}, party id: {}", uploadMode, partyId);
            ResponseEntity tokenCheck = eu.nimble.service.catalogue.util.HttpResponseUtil.checkToken(bearerToken);
            if (tokenCheck != null) {
                return tokenCheck;
            }

            // check the existence of the specified party in the catalogue DB
            PartyType party = CatalogueDatabaseAdapter.syncPartyInUBLDB(partyId, bearerToken);

            CatalogueType catalogue;

            // lock the update for the specified party
            try {
                lockPool.getLockForParty(partyId).writeLock().lock();

                // parse catalogue
                try {
                    catalogue = service.parseCatalogue(file.getInputStream(), uploadMode, party);
                } catch (Exception e) {
                    String msg = e.getMessage() != null ? e.getMessage() : "Failed to retrieve the template";
                    return HttpResponseUtil.createResponseEntityAndLog(msg, e, HttpStatus.BAD_REQUEST, LogLevel.INFO);
                }

                // save catalogue
                // check whether an insert or update operations is needed
                if (catalogue.getHjid() == null) {
                    catalogue = service.addCatalogue(catalogue, Configuration.Standard.UBL);
                } else {
                    catalogue = service.updateCatalogue(catalogue);
                }
            } finally {
                lockPool.getLockForParty(partyId).writeLock().unlock();
            }

            URI catalogueURI;
            try {
                catalogueURI = new URI(HttpResponseUtil.baseUrl(request) + catalogue.getUUID());
                log.info("Completed the request to upload template. Added catalogue uuid: {}", catalogue.getUUID());
                return ResponseEntity.created(catalogueURI).body(serializationUtility.serializeUBLObject(catalogue));

            } catch (URISyntaxException e) {
                return createErrorResponseEntity("Failed to generate a URI for the newly created item", HttpStatus.INTERNAL_SERVER_ERROR, e);
            }


        } catch (Exception e) {
            return HttpResponseUtil.createResponseEntityAndLog("Unexpected error while uploading the template", e, HttpStatus.INTERNAL_SERVER_ERROR, LogLevel.ERROR);
        }
    }

    @CrossOrigin(origins = {"*"})
    @ApiOperation(value = "", notes = "Returns the supported catalogue data model standards. Only the UBL standard is supported for the being considering " +
            "the full-fledged catalogue management services as well as connection of published products with the business processes.")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Retrieve the supported standards successfully", response = String.class, responseContainer = "List"),
            @ApiResponse(code = 401, message = "Invalid token. No user was found for the provided token"),
            @ApiResponse(code = 500, message = "Unexpected error while getting the supported standards")
    })
    @RequestMapping(value = "/catalogue/standards",
            produces = {"application/json"},
            method = RequestMethod.GET)
    public ResponseEntity getSupportedStandards(@ApiParam(value = "The Bearer token provided by the identity service", required = true) @RequestHeader(value = "Authorization", required = true) String authorization) {
        log.info("Incoming request to retrieve the supported standards");
        ResponseEntity tokenCheck = eu.nimble.service.catalogue.util.HttpResponseUtil.checkToken(authorization);
        if (tokenCheck != null) {
            return tokenCheck;
        }

        List<Configuration.Standard> standards = new ArrayList<>();
        try {
//            standards = Arrays.asList(Configuration.Standard.values());
            standards.add(Configuration.Standard.UBL);
        } catch (Exception e) {
            return createErrorResponseEntity("Failed to get supported standards", HttpStatus.INTERNAL_SERVER_ERROR, e);
        }

        log.info("Completed request to retrieve the supported standards");
        return ResponseEntity.ok(standards);
    }

    @CrossOrigin(origins = {"*"})
    @ApiOperation(value = "", notes = "Associate the images provided in a ZIP package to relevant products. The images " +
            "are associated with the target products via the product ids. For this, the image names should start with a " +
            "valid product id. Specifically image name format should be as follows: &lt;productId&gt;.&lt;imageName&gt;.&lt;imageFormat&gt; " +
            "e.g. Product_id1.product1_image.jpg")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Added the images provided in the package object to relevant products successfully."),
            @ApiResponse(code = 400, message = "Failed obtain a Zip package from the provided data"),
            @ApiResponse(code = 404, message = "Catalogue with the given uuid does not exist"),
            @ApiResponse(code = 500, message = "Unexpected error while uploading images")
    })
    @RequestMapping(value = "/catalogue/{uuid}/image/upload",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE,
            method = RequestMethod.POST)
    public ResponseEntity uploadImages(
            @ApiParam(value = "The package compressed as a Zip file, including the images. An example image package can be found in: https://github.com/nimble-platform/catalog-service/tree/staging/catalogue-service-micro/src/main/resources/example_content/images.zip", required = true) @RequestParam("package") MultipartFile pack,
            @ApiParam(value = "uuid of the catalogue to be retrieved.", required = true) @PathVariable("uuid") String uuid,
            @ApiParam(value = "The Bearer token provided by the identity service", required = true) @RequestHeader(value = "Authorization", required = true) String bearerToken) {
        try {
            log.info("Incoming request to upload images for catalogue: {}", uuid);
            ResponseEntity tokenCheck = eu.nimble.service.catalogue.util.HttpResponseUtil.checkToken(bearerToken);
            if (tokenCheck != null) {
                return tokenCheck;
            }
            if (service.getCatalogue(uuid) == null) {
                log.error("Catalogue with uuid : {} does not exist", uuid);
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(String.format("Catalogue with uuid %s does not exist", uuid));
            }

            CatalogueType catalogue;
            ZipInputStream zis = null;
            try {
                zis = new ZipInputStream(pack.getInputStream());
                catalogue = service.addImagesToProducts(zis, uuid);

            } catch (IOException e) {
                return createErrorResponseEntity("Failed obtain a Zip package from the provided data", HttpStatus.BAD_REQUEST, e);
            } catch (CatalogueServiceException e) {
                return createErrorResponseEntity(e.getMessage(), HttpStatus.BAD_REQUEST, e);
            } catch (BinaryContentException e){
                return createErrorResponseEntity(e.getMessage(), HttpStatus.BAD_REQUEST, null);
            } catch (Exception e){
                return createErrorResponseEntity(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR, e);
            }finally {
                try {
                    zis.close();
                } catch (IOException e) {
                    log.warn("Failed to close Zip stream", e);
                }
            }

            log.info("Completed the request to upload images for catalogue: {}", uuid);
            return ResponseEntity.ok().body(serializationUtility.serializeUBLObject(catalogue));

        } catch (Exception e) {
            return HttpResponseUtil.createResponseEntityAndLog(String.format("Unexpected error while uploading images. uuid: %s", uuid), e, HttpStatus.INTERNAL_SERVER_ERROR, LogLevel.ERROR);
        }
    }

    @CrossOrigin(origins = {"*"})
    @ApiOperation(value = "", notes = "Deletes all the images of CatalogueLines of the specified catalogue")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Deleted the images successfully",response = CatalogueType.class),
            @ApiResponse(code = 401, message = "Invalid token. No user was found for the provided token"),
            @ApiResponse(code = 404, message = "No catalogue for the given uuid"),
            @ApiResponse(code = 500, message = "Unexpected error while getting catalogue")
    })
    @RequestMapping(value = "/catalogue/{uuid}/delete-images",
            method = RequestMethod.GET)
    public ResponseEntity deleteImagesInsideCatalogue(@ApiParam(value = "uuid of the catalogue to be retrieved.", required = true) @PathVariable String uuid,
                                                      @ApiParam(value = "The Bearer token provided by the identity service", required = true) @RequestHeader(value = "Authorization", required = true) String bearerToken) {
        try {
            log.info("Incoming request to delete images for catalogue: {}", uuid);
            // token check
            ResponseEntity tokenCheck = eu.nimble.service.catalogue.util.HttpResponseUtil.checkToken(bearerToken);
            if (tokenCheck != null) {
                return tokenCheck;
            }

            // catalogue check
            CatalogueType catalogue = service.getCatalogue(uuid);
            if (catalogue == null) {
                log.error("Catalogue with uuid : {} does not exist", uuid);
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(String.format("Catalogue with uuid %s does not exist", uuid));
            }

            // remove the images
            catalogue = service.removeAllImagesFromCatalogue(catalogue);

            log.info("Deleted images for catalogue: {}",uuid);
            return ResponseEntity.ok().body(serializationUtility.serializeUBLObject(catalogue));

        } catch (Exception e) {
            return HttpResponseUtil.createResponseEntityAndLog(String.format("Unexpected error while uploading images. uuid: %s", uuid), e, HttpStatus.INTERNAL_SERVER_ERROR, LogLevel.ERROR);
        }
    }

    @CrossOrigin(origins = {"*"})
    @ApiOperation(value = "", notes = "Retrieves the specified UBL-compliant catalogue in the specified semantic format. If no format is specified, RDF/XML is used.")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Retrieved the catalogue successfully"),
            @ApiResponse(code = 401, message = "Invalid token. No user was found for the provided token"),
            @ApiResponse(code = 404, message = "No catalogue for the given uuid"),
            @ApiResponse(code = 500, message = "Unexpected error while getting catalogue")
    })
    @RequestMapping(value = "/catalogue/semantic/{uuid}",
            method = RequestMethod.GET)
    public ResponseEntity getCatalogueInSemanticFormat(@ApiParam(value = "uuid of the catalogue to be retrieved.", required = true) @PathVariable String uuid,
                                                       @ApiParam(value = "Target content type for the serialization.", defaultValue = "RDF/XML") @RequestParam(value = "semanticContentType", required = false) String semanticContentType,
                                                       @ApiParam(value = "The Bearer token provided by the identity service", required = true) @RequestHeader(value = "Authorization", required = true) String authorization,
                                                       HttpServletResponse response) {
        try {
            log.info("Incoming request to get catalogue in semantic format, uuid: {}, content type: {}", uuid, semanticContentType);
            ResponseEntity tokenCheck = eu.nimble.service.catalogue.util.HttpResponseUtil.checkToken(authorization);
            if (tokenCheck != null) {
                return tokenCheck;
            }

            Object catalogue;
            try {
                catalogue = service.getCatalogue(uuid, Configuration.Standard.UBL);
            } catch (Exception e) {
                return createErrorResponseEntity("Failed to get catalogue for uuid: " + uuid, HttpStatus.INTERNAL_SERVER_ERROR, e);
            }

            if (catalogue == null) {
                log.info("No catalogue for uuid: {}", uuid);
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(String.format("No default catalogue for uuid: %s", uuid));
            }

            // transform content to other semantic formats
            if (semanticContentType == null) {
                semanticContentType = "RDF/XML";
            }

            SemanticTransformationUtil semanticTransformationUtil = new SemanticTransformationUtil();
            try {
                XML2OWLMapper rdfGenerator = semanticTransformationUtil.transformCatalogueToRDF((CatalogueType) catalogue);
                rdfGenerator.writeModel(response.getOutputStream(), semanticContentType);
                response.flushBuffer();
            } catch (IOException e) {
                return HttpResponseUtil.createResponseEntityAndLog(String.format("Failed to get catalogue with uuid: %s", uuid), e, HttpStatus.INTERNAL_SERVER_ERROR, LogLevel.ERROR);
            }

            log.info("Completed request to get catalogue, uuid: {}", uuid);
            return ResponseEntity.ok().build();

        } catch (Exception e) {
            return HttpResponseUtil.createResponseEntityAndLog(String.format("Unexpected error while getting the catalogue in semantic format. uuid: %s, content-type: %s", uuid, semanticContentType), e, HttpStatus.INTERNAL_SERVER_ERROR, LogLevel.ERROR);
        }
    }

    @CrossOrigin(origins = {"*"})
    @ApiOperation(value = "", notes = "Retrieves the catalogue id's for a party.")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Retrieved the catalogue id's successfully"),
            @ApiResponse(code = 401, message = "Invalid token. No user was found for the provided token"),
            @ApiResponse(code = 404, message = "No catalogue ids found for the given party"),
            @ApiResponse(code = 500, message = "Unexpected error while getting catalogue id's")
    })
    @RequestMapping(value = "/catalogue/ids/{partyId}",
            produces = {"application/json"},
            method = RequestMethod.GET)
    public ResponseEntity getAllCatalogueIdsForParty(@ApiParam(value = "Identifier of the party for which the catalogue to be retrieved", required = true) @PathVariable String partyId,
                                              @ApiParam(value = "The Bearer token provided by the identity service", required = true) @RequestHeader(value = "Authorization", required = true) String bearerToken) {
        log.info("Incoming request to get catalogue id list for party: {}", partyId);
        ResponseEntity tokenCheck = eu.nimble.service.catalogue.util.HttpResponseUtil.checkToken(bearerToken);
        if (tokenCheck != null) {
            return tokenCheck;
        }

        List<String> catalogueIds;
        try {
            catalogueIds = service.getCatalogueIdsForParty(partyId);
        } catch (Exception e) {
            return createErrorResponseEntity("Failed to get catalogues for party id: " + partyId, HttpStatus.INTERNAL_SERVER_ERROR, e);
        }

        if (catalogueIds == null) {
            log.info("No default catalogue for party: {}", partyId);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(String.format("No default catalogue for party: %s", partyId));
        }

        log.info("Completed request to get catalogue id list for party: {}", partyId);
        return ResponseEntity.ok(catalogueIds);
    }

    @CrossOrigin(origins = {"*"})
    @ApiOperation(value = "", notes = "Retrieves catalogue from id specific for a party.")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Retrieved the catalogue successfully"),
            @ApiResponse(code = 401, message = "Invalid token. No user was found for the provided token"),
            @ApiResponse(code = 404, message = "No catalogue found for the given id and party id"),
            @ApiResponse(code = 500, message = "Unexpected error while getting catalogue id's")
    })
    @RequestMapping(value = "/catalogue/{partyId}/{id}/{standard}",
            produces = {"application/json"},
            method = RequestMethod.GET)
    public ResponseEntity getCatalogueFromId(@ApiParam(value = "Identifier of the party for which the catalogue to be retrieved", required = true) @PathVariable String partyId,
                                                        @ApiParam(value = "Identifier of the catalogue", required = true) @PathVariable String id,
                                                        @ApiParam(value = "Data model standard that the provided catalogue is compatible with.", defaultValue = "ubl", required = true) @PathVariable String standard,
                                                     @ApiParam(value = "The Bearer token provided by the identity service", required = true) @RequestHeader(value = "Authorization", required = true) String bearerToken) {

        log.info("Incoming request to get catalogue for id: {} for party: {}",id,partyId);
        ResponseEntity tokenCheck = eu.nimble.service.catalogue.util.HttpResponseUtil.checkToken(bearerToken);

        if (tokenCheck != null) {
            return tokenCheck;
        }

        Configuration.Standard std;
        try {
            std = getStandardEnum(standard);
        } catch (Exception e) {
            return createErrorResponseEntity("Invalid standard: " + standard, HttpStatus.BAD_REQUEST, e);
        }

        Object catalogue;
        try {
            catalogue = service.getCatalogue(id,partyId,std);
        } catch (Exception e) {
            return createErrorResponseEntity("Failed to get catalogue for party id: " + partyId, HttpStatus.INTERNAL_SERVER_ERROR, e);
        }

        if (catalogue == null) {
            log.info("No catalogue for id: {}", id);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(String.format("No catalogue for id: %s", id));
        }

        log.info("Completed request to get catalogue for party: {} and id {}", partyId,id);
        return ResponseEntity.ok(catalogue);
    }

    private ResponseEntity createErrorResponseEntity(String msg, HttpStatus status, Exception e) {
        if (e != null) {
            msg = msg + e.getMessage();
            log.error(msg, e);
        } else {
            log.error(msg);
        }
        return ResponseEntity.status(status).body(msg);
    }

    private Configuration.Standard getStandardEnum(String standard) {
        standard = standard.toUpperCase();
        Configuration.Standard std = Configuration.Standard.valueOf(standard);
        return std;
    }
}
