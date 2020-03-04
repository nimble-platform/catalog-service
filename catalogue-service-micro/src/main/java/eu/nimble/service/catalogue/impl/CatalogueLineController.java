package eu.nimble.service.catalogue.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import eu.nimble.service.catalogue.CatalogueService;
import eu.nimble.service.catalogue.config.CatalogueServiceConfig;
import eu.nimble.service.catalogue.config.RoleConfig;
import eu.nimble.service.catalogue.model.catalogue.CatalogueLineSortOptions;
import eu.nimble.service.catalogue.model.statistics.ProductAndServiceStatistics;
import eu.nimble.service.catalogue.persistence.util.CatalogueLinePersistenceUtil;
import eu.nimble.service.catalogue.util.CatalogueEvent;
import eu.nimble.service.catalogue.util.ExecutionContext;
import eu.nimble.service.catalogue.util.LoggerUtil;
import eu.nimble.service.catalogue.validation.CatalogueLineValidator;
import eu.nimble.service.catalogue.validation.ValidationMessages;
import eu.nimble.service.model.ubl.catalogue.CatalogueType;
import eu.nimble.service.model.ubl.commonaggregatecomponents.CatalogueLineType;
import eu.nimble.utility.*;
import eu.nimble.utility.exception.NimbleException;
import eu.nimble.utility.exception.NimbleExceptionMessageCode;
import eu.nimble.utility.persistence.resource.ResourceValidationUtility;
import eu.nimble.utility.serialization.TransactionEnabledSerializationUtility;
import eu.nimble.utility.validation.IValidationUtil;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;

/**
 * Created by suat on 22-Aug-17.
 * <p>
 * Catalogue line-level REST services. All services in this class work only on the UBL based catalogues.
 */
@Controller
public class CatalogueLineController {
    private static Logger log = LoggerFactory.getLogger(CatalogueLineController.class);

    @Autowired
    private CatalogueServiceConfig catalogueServiceConfig;
    @Autowired
    private TransactionEnabledSerializationUtility serializationUtility;
    @Autowired
    private ResourceValidationUtility resourceValidationUtil;
    @Autowired
    private CatalogueService service;
    @Autowired
    private IValidationUtil validationUtil;
    @Autowired
    private ExecutionContext executionContext;

    @CrossOrigin(origins = {"*"})
    @ApiOperation(value = "", notes = "Retrieves the catalogue line with the DB-scoped identifier")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Retrieved catalogue line successfully", response = CatalogueLineType.class),
            @ApiResponse(code = 400, message = "Failed to get catalogue line"),
            @ApiResponse(code = 404, message = "Specified catalogue or catalogue line does not exist"),
            @ApiResponse(code = 500, message = "Unexpected error while getting catalogue line")
    })
    @RequestMapping(value = "/catalogueline/{hjid}",
            produces = {"application/json"},
            method = RequestMethod.GET)
    public ResponseEntity getCatalogueLineByHjid(@ApiParam(value = "Identifier of the catalogue line to be retrieved. (line.hjid)", required = true) @PathVariable Long hjid,
                                           @ApiParam(value = "The Bearer token provided by the identity service", required = true) @RequestHeader(value = "Authorization") String bearerToken) {
        // set request log of ExecutionContext
        String requestLog = String.format("Incoming request to get catalogue line with hjid: %s", hjid);
        executionContext.setRequestLog(requestLog);

        log.info(requestLog);
        // validate role
        if(!validationUtil.validateRole(bearerToken, RoleConfig.REQUIRED_ROLES_CATALOGUE)) {
            throw new NimbleException(NimbleExceptionMessageCode.UNAUTHORIZED_INVALID_ROLE.toString());
        }

        CatalogueLineType catalogueLine;
        try {
            catalogueLine = service.getCatalogueLine(hjid);
        } catch (Exception e) {
            throw new NimbleException(NimbleExceptionMessageCode.INTERNAL_SERVER_ERROR_GET_CATALOGUE_LINE.toString(),e);
        }

        if (catalogueLine == null) {
            throw new NimbleException(NimbleExceptionMessageCode.NOT_FOUND_NO_CATALOGUE_LINE_FOR_HJID.toString(),Arrays.asList(hjid.toString()));
        }
        log.info("Completed the request to get catalogue line with hjid: {}", hjid);
        return ResponseEntity.ok(serializationUtility.serializeUBLObject(catalogueLine));
    }

    @CrossOrigin(origins = {"*"})
    @ApiOperation(value = "", notes = "Retrieves the catalogue lines with the DB-scoped identifiers")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Retrieved catalogue lines successfully", response = CatalogueLineType.class,responseContainer = "List"),
            @ApiResponse(code = 401, message = "No user exists for the given token"),
            @ApiResponse(code = 500, message = "Unexpected error while getting catalogue lines")
    })
    @RequestMapping(value = "/cataloguelines",
            produces = {"application/json"},
            method = RequestMethod.GET)
    public ResponseEntity getCatalogueLinesByHjids(@ApiParam(value = "Identifiers of the catalogue lines to be retrieved. (line.hjid)", required = true) @RequestParam(value = "ids") List<Long> hjids,
                                                   @ApiParam(value = "Number of catalogue lines to be included in CataloguePaginationResponse",required = true) @RequestParam(value = "limit",required = true) Integer limit,
                                                   @ApiParam(value = "Offset of the first catalogue line among all catalogue lines of the default catalogue for the party",required = true) @RequestParam(value = "offset",required = true) Integer pageNo,
                                                   @ApiParam(value = "Option used to sort catalogue lines", required = false) @RequestParam(value = "sortOption", required = false) CatalogueLineSortOptions sortOption,
                                                   @ApiParam(value = "The Bearer token provided by the identity service", required = true) @RequestHeader(value = "Authorization") String bearerToken) {
        // set request log of ExecutionContext
        String requestLog = String.format("Incoming request to get catalogue lines with hjids: %s", hjids);
        executionContext.setRequestLog(requestLog);

        log.info(requestLog);
        // validate role
        if(!validationUtil.validateRole(bearerToken, RoleConfig.REQUIRED_ROLES_CATALOGUE)) {
            throw new NimbleException(NimbleExceptionMessageCode.UNAUTHORIZED_INVALID_ROLE.toString());
        }

        List<CatalogueLineType> catalogueLines;
        try {
            catalogueLines = service.getCatalogueLines(hjids,sortOption,limit,pageNo);
        } catch (Exception e) {
            throw new NimbleException(NimbleExceptionMessageCode.INTERNAL_SERVER_ERROR_GET_CATALOGUE_LINES.toString(),e);
        }

        log.info("Completed the request to get catalogue lines with hjids: {}", hjids);
        return ResponseEntity.ok(serializationUtility.serializeUBLObject(catalogueLines));
    }

    @CrossOrigin(origins = {"*"})
    @ApiOperation(value = "", notes = "Retrieves a list of catalogue lines. The service takes a list of catalogue uuids and "+
    "another list containing corresponding catalogue line ids.")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Retrieved catalogue lines successfully", response = CatalogueLineType.class, responseContainer = "List"),
            @ApiResponse(code = 400, message = "Number of elements in catalogue uuids list and line ids list does not match"),
            @ApiResponse(code = 401, message = "No user exists for the given token")
    })
    @RequestMapping(value = "/catalogue/cataloguelines",
            produces = {"application/json"},
            method = RequestMethod.GET)
    public ResponseEntity getCatalogueLines(@ApiParam(value = "Comma-separated catalogue uuids to be retrieved e.g. 5e910673-8232-4ec1-adb3-9188377309bf,34rwe231-34ds-5dw2-hgd2-462tdr64wfgs", required = true) @RequestParam(value = "catalogueUuids",required = true) List<String> catalogueUuids,
                                            @ApiParam(value = "Comma-separated line ids to be retrieved e.g. e86e6558-b95c-4c3d-ac17-ac84830d7527,80f50752-e147-4063-8573-be78cde0d3a6",required = true) @RequestParam(value = "lineIds",required = true) List<String> lineIds,
                                            @ApiParam(value = "The Bearer token provided by the identity service", required = true) @RequestHeader(value = "Authorization") String bearerToken) {
        // set request log of ExecutionContext
        String requestLog = String.format("Incoming request to get catalogue lines, catalogue uuids: %s, line ids: %s",catalogueUuids,lineIds);
        executionContext.setRequestLog(requestLog);

        log.info(requestLog);
        // validate role
        if(!validationUtil.validateRole(bearerToken, RoleConfig.REQUIRED_ROLES_CATALOGUE)) {
            throw new NimbleException(NimbleExceptionMessageCode.UNAUTHORIZED_INVALID_ROLE.toString());
        }

        // ensure that catalogue uuids and catalogue line ids lists have the same size
        if (catalogueUuids.size() != lineIds.size()) {
            throw new NimbleException(NimbleExceptionMessageCode.BAD_REQUEST_GET_CATALOGUE_LINES.toString());
        }

        List<CatalogueLineType> catalogueLines = new ArrayList<>();

        int numberOfCatalog = catalogueUuids.size();
        for(int i = 0; i < numberOfCatalog; i++){
            CatalogueLineType catalogueLine = service.getCatalogueLine(catalogueUuids.get(i),lineIds.get(i));
            if(catalogueLine != null){
                catalogueLines.add(catalogueLine);
            }
        }

        log.info("Completed the request to get catalogue lines, catalogue uuids: {}, line ids: {}",catalogueUuids,lineIds);
        return ResponseEntity.ok(serializationUtility.serializeUBLObject(catalogueLines));
    }

    @CrossOrigin(origins = {"*"})
    @ApiOperation(value = "", notes = "Retrieves the catalogue line specified with the catalogueUuid and lineId parameters")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Retrieved catalogue line successfully", response = CatalogueLineType.class),
            @ApiResponse(code = 400, message = "Failed to get catalogue line"),
            @ApiResponse(code = 404, message = "Specified catalogue or catalogue line does not exist"),
            @ApiResponse(code = 500, message = "Unexpected error while getting catalogue line")
    })
    @RequestMapping(value = "/catalogue/{catalogueUuid}/catalogueline/{lineId}",
            produces = {"application/json"},
            method = RequestMethod.GET)
    public ResponseEntity getCatalogueLine(@ApiParam(value = "uuid of the catalogue containing the line to be retrieved. (catalogue.uuid)", required = true) @PathVariable String catalogueUuid,
                                           @ApiParam(value = "Identifier of the catalogue line to be retrieved. (line.id)", required = true) @PathVariable String lineId,
                                           @ApiParam(value = "The Bearer token provided by the identity service", required = true) @RequestHeader(value = "Authorization") String bearerToken) {
        // set request log of ExecutionContext
        String requestLog = String.format("Incoming request to get catalogue line with lineId: %s, catalogue uuid: %s", lineId, catalogueUuid);
        executionContext.setRequestLog(requestLog);

        log.info(requestLog);
        // validate role
        if(!validationUtil.validateRole(bearerToken, RoleConfig.REQUIRED_ROLES_CATALOGUE)) {
            throw new NimbleException(NimbleExceptionMessageCode.UNAUTHORIZED_INVALID_ROLE.toString());
        }

        if (service.getCatalogue(catalogueUuid) == null) {
            throw new NimbleException(NimbleExceptionMessageCode.NOT_FOUND_NO_CATALOGUE.toString(),Arrays.asList(catalogueUuid));
        }

        CatalogueLineType catalogueLine;
        try {
            catalogueLine = service.getCatalogueLine(catalogueUuid, lineId);
        } catch (Exception e) {
            throw new NimbleException(NimbleExceptionMessageCode.INTERNAL_SERVER_ERROR_GET_CATALOGUE_LINE_WITH_LINE_AND_CATALOGUE_ID.toString(),Arrays.asList(lineId, catalogueUuid));
        }

        if (catalogueLine == null) {
            throw new NimbleException(NimbleExceptionMessageCode.NOT_FOUND_NO_CATALOGUE_LINE_WITH_ID.toString(),Arrays.asList(lineId));
        }
        log.info("Completed the request to get catalogue line with lineId: {}", lineId);
        return ResponseEntity.ok(serializationUtility.serializeUBLObject(catalogueLine));
    }

    @CrossOrigin(origins = {"*"})
    @ApiOperation(value = "", notes = "Retrieves the catalogue lines specified with the catalogueUuid and lineIds parameters")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Retrieved catalogue lines successfully", response = CatalogueLineType.class, responseContainer = "List"),
            @ApiResponse(code = 400, message = "Failed to get catalogue lines"),
            @ApiResponse(code = 404, message = "Specified catalogue does not exist"),
            @ApiResponse(code = 500, message = "Unexpected error while getting catalogue lines")
    })
    @RequestMapping(value = "/catalogue/{catalogueUuid}/cataloguelines",
            produces = {"application/json"},
            method = RequestMethod.GET)
    public ResponseEntity getCatalogueLines(@ApiParam(value = "uuid of the catalogue containing the lines to be retrieved. (catalogue.uuid)", required = true) @PathVariable String catalogueUuid,
                                           @ApiParam(value = "Identifier of the catalogue lines to be retrieved. (line.id)", required = true) @RequestParam(value = "lineIds") List<String> lineIds,
                                           @ApiParam(value = "The Bearer token provided by the identity service", required = true) @RequestHeader(value = "Authorization") String bearerToken) {
        // set request log of ExecutionContext
        String requestLog = String.format("Incoming request to get catalogue line with lineIds: %s, catalogue uuid: %s", lineIds, catalogueUuid);
        executionContext.setRequestLog(requestLog);

        log.info(requestLog);
        // validate role
        if(!validationUtil.validateRole(bearerToken, RoleConfig.REQUIRED_ROLES_CATALOGUE)) {
            throw new NimbleException(NimbleExceptionMessageCode.UNAUTHORIZED_INVALID_ROLE.toString());
        }

        if (service.getCatalogue(catalogueUuid) == null) {
            throw new NimbleException(NimbleExceptionMessageCode.NOT_FOUND_NO_CATALOGUE.toString(),Arrays.asList(catalogueUuid));
        }

        List<CatalogueLineType> catalogueLines;
        try {
            catalogueLines = service.getCatalogueLines(catalogueUuid, lineIds);
        } catch (Exception e) {
            throw new NimbleException(NimbleExceptionMessageCode.INTERNAL_SERVER_ERROR_GET_MULTIPLE_CATALOGUE_LINES.toString(),Arrays.asList(lineIds.toString(), catalogueUuid),e);
        }

        log.info("Completed the request to get catalogue lines with lineIds: {}", lineIds);
        return ResponseEntity.ok(serializationUtility.serializeUBLObject(catalogueLines));
    }

    @CrossOrigin(origins = {"*"})
    @ApiOperation(value = "", notes = "Adds the provided catalogue line to the specified catalogue")
    @ApiResponses(value = {
            @ApiResponse(code = 201, message = "Persisted the catalogue line successfully and returned the persisted entity"),
            @ApiResponse(code = 404, message = "Catalogue with the given uuid does not exist"),
            @ApiResponse(code = 406, message = "There already exists a product with the given id"),
            @ApiResponse(code = 400, message = "Invalid catalogue line serialization"),
            @ApiResponse(code = 500, message = "Unexpected error while adding catalogue line")
    })
    @RequestMapping( value = "/catalogue/{catalogueUuid}/catalogueline",
            consumes = {"application/json"},
            produces = {"application/json"},
            method = RequestMethod.POST)
    public ResponseEntity addCatalogueLine(@ApiParam(value = "uuid of the catalogue containing the line to be retrieved. (catalogue.uuid)", required = true) @PathVariable String catalogueUuid,
                                           @ApiParam(value = "Serialized form of the catalogue line. Valid serializations can be achieved via JsonSerializationUtility.getObjectMapper method located in the utility module. An example catalogue line serialization can be found in: https://github.com/nimble-platform/catalog-service/tree/staging/catalogue-service-micro/src/main/resources/example_content/catalogue_line.json", required = true) @RequestBody String catalogueLineJson,
                                           @ApiParam(value = "The Bearer token provided by the identity service", required = true) @RequestHeader(value = "Authorization") String bearerToken) {
        // set request log of ExecutionContext
        String requestLog = String.format("Incoming request to add catalogue line to catalogue: %s", catalogueUuid);
        executionContext.setRequestLog(requestLog);

        log.info(requestLog);
        CatalogueType catalogue;
        CatalogueLineType catalogueLine;

        try {
            // validate role
            if(!validationUtil.validateRole(bearerToken, RoleConfig.REQUIRED_ROLES_CATALOGUE)) {
                throw new NimbleException(NimbleExceptionMessageCode.UNAUTHORIZED_INVALID_ROLE.toString());
            }

            // get owning catalogue
            catalogue = service.getCatalogue(catalogueUuid);
            if (catalogue == null) {
                throw new NimbleException(NimbleExceptionMessageCode.NOT_FOUND_NO_CATALOGUE.toString(),Arrays.asList(catalogueUuid));
            }

            // parse catalogue line
            try {
                catalogueLine = JsonSerializationUtility.getObjectMapper().readValue(catalogueLineJson, CatalogueLineType.class);
            } catch (IOException e) {
                throw new NimbleException(NimbleExceptionMessageCode.BAD_REQUEST_DESERIALIZE_CATALOGUE_LINE.toString(),Arrays.asList(catalogueLineJson),e);
            }

            // validate the incoming content
            CatalogueLineValidator catalogueLineValidator = new CatalogueLineValidator(catalogue, catalogueLine);
            ValidationMessages errors = catalogueLineValidator.validate();
            if (errors.getErrorMessages().size() > 0) {
                throw new NimbleException(errors.getErrorMessages(),errors.getErrorParameters());
            }

            // check the entity ids
            boolean hjidsExists = resourceValidationUtil.hjidsExit(catalogueLine);
            if(hjidsExists) {
                throw new NimbleException(NimbleExceptionMessageCode.BAD_REQUEST_HJIDS.toString(),Arrays.asList(catalogueLineJson));
            }

            // check duplicate line
            boolean lineExists = CatalogueLinePersistenceUtil.checkCatalogueLineExistence(catalogueUuid, catalogueLine.getID());
            if (!lineExists) {
                catalogueLine = service.addLineToCatalogue(catalogue, catalogueLine);
            } else {
                throw new NimbleException(NimbleExceptionMessageCode.NOT_ACCEPTABLE_ALREADY_EXISTS.toString());
            }

        } catch (Exception e) {
            log.warn("The following catalogue line could not be created: {}", catalogueLineJson);
            throw new NimbleException(NimbleExceptionMessageCode.INTERNAL_SERVER_ERROR_ADD_CATALOGUE_LINE.toString(),e);
        }

        return createCreatedCatalogueLineResponse(catalogueUuid, catalogueLine);
    }

    private ResponseEntity createCreatedCatalogueLineResponse(String catalogueUuid, CatalogueLineType line) {
        URI lineURI;
        try {
            String applicationUrl = catalogueServiceConfig.getSpringApplicationUrl();
            lineURI = new URI(applicationUrl + "/catalogue/" + catalogueUuid + "/" + line.getID());

            Map<String,String> paramMap = LoggerUtil.getMDCParamMapForCatalogueLine(line, CatalogueEvent.PRODUCT_PUBLISH);
            LoggerUtils.logWithMDC(log, paramMap, LoggerUtils.LogLevel.INFO, "Item published with  catalogue uuid: {}, lineId: {}",
                    catalogueUuid, line.getID());

        } catch (URISyntaxException e) {
            String msg = "Failed to generate a URI for the newly created item";
            log.warn(msg, e);
            try {
                log.info("Completed request to add catalogue line with an empty URI, catalogue uuid: {}, lineId: {}", catalogueUuid, line.getID());
                return ResponseEntity.created(new URI("")).body(serializationUtility.serializeUBLObject(line));
            } catch (URISyntaxException e1) {
                throw new NimbleException(NimbleExceptionMessageCode.INTERNAL_SERVER_ERROR_ADD_CATALOGUE_LINE.toString(),e);
            }
        }
        log.info("Completed request to add catalogue line, catalogue uuid: {}, lineId: {}", catalogueUuid, line.getID());
        return ResponseEntity.created(lineURI).body(serializationUtility.serializeUBLObject(line));
    }

    @CrossOrigin(origins = {"*"})
    @ApiOperation(value = "", notes = "Updates the specified catalogue line")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Updated the catalogue line successfully and returned the persisted entity"),
            @ApiResponse(code = 404, message = "Catalogue with the given uuid does not exist"),
            @ApiResponse(code = 406, message = "There already exists a product with the given id"),
            @ApiResponse(code = 400, message = "Invalid catalogue line serialization"),
            @ApiResponse(code = 500, message = "Unexpected error while updating catalogue line")
    })
    @RequestMapping( value = "/catalogue/{catalogueUuid}/catalogueline",
            consumes = {"application/json"},
            produces = {"application/json"},
            method = RequestMethod.PUT)
    public ResponseEntity updateCatalogueLine(@ApiParam(value = "uuid of the catalogue containing the line to be retrieved. (catalogue.uuid)", required = true) @PathVariable String catalogueUuid,
                                              @ApiParam(value = "Serialized form of the catalogue line. Valid serializations can be achieved via JsonSerializationUtility.getObjectMapper method located in the utility module. An example catalogue line serialization can be found in: https://github.com/nimble-platform/catalog-service/tree/staging/catalogue-service-micro/src/main/resources/example_content/catalogue_line.json.", required = true) @RequestBody String catalogueLineJson,
                                              @ApiParam(value = "The Bearer token provided by the identity service", required = true) @RequestHeader(value = "Authorization") String bearerToken) {
        // set request log of ExecutionContext
        String requestLog = String.format("Incoming request to update catalogue line. Catalogue uuid: %s}", catalogueUuid);
        executionContext.setRequestLog(requestLog);
        try {
            // validate role
            if(!validationUtil.validateRole(bearerToken, RoleConfig.REQUIRED_ROLES_CATALOGUE)) {
                throw new NimbleException(NimbleExceptionMessageCode.UNAUTHORIZED_INVALID_ROLE.toString());
            }

            CatalogueLineType catalogueLine;

            //parse catalogue line
            try {
                ObjectMapper objectMapper = JsonSerializationUtility.getObjectMapper();
                catalogueLine = objectMapper.readValue(catalogueLineJson, CatalogueLineType.class);

            } catch (IOException e) {
                throw new NimbleException(NimbleExceptionMessageCode.BAD_REQUEST_DESERIALIZE_CATALOGUE_LINE.toString(),Arrays.asList(catalogueLineJson),e);
            }

            log.info("Incoming request to update catalogue line. Catalogue uuid: {}, line hjid: {}", catalogueUuid, catalogueLine.getHjid());
            CatalogueType catalogue = service.getCatalogue(catalogueUuid);
            if (catalogue == null) {
                throw new NimbleException(NimbleExceptionMessageCode.NOT_FOUND_NO_CATALOGUE.toString(),Arrays.asList(catalogueUuid));
            }

            CatalogueLineValidator catalogueLineValidator = new CatalogueLineValidator(catalogue, catalogueLine);

            if(!catalogueUuid.equals(catalogueLine.getGoodsItem().getItem().getCatalogueDocumentReference().getID())){
                String newCatalogueUuid = catalogueUuid;
                String oldCatalogueUuid = catalogueLine.getGoodsItem().getItem().getCatalogueDocumentReference().getID();

                catalogueLine.getGoodsItem().getItem().getCatalogueDocumentReference().setID(newCatalogueUuid);
                ValidationMessages validationMessages = catalogueLineValidator.validate();
                if (validationMessages.getErrorMessages().size() > 0) {
                    throw new NimbleException(validationMessages.getErrorMessages(),validationMessages.getErrorParameters());
                }

                // validate the entity ids
                boolean hjidsBelongToCompany = resourceValidationUtil.hjidsBelongsToParty(catalogueLine, catalogue.getProviderParty().getPartyIdentification().get(0).getID(), Configuration.Standard.UBL.toString());
                if(!hjidsBelongToCompany) {
                    throw new NimbleException(NimbleExceptionMessageCode.BAD_REQUEST_INVALID_HJIDS_IN_LINE.toString(),Arrays.asList(catalogueLineJson));
                }

                // consider the case of an updated line id conflicting with the id of an existing line
                boolean lineExists = CatalogueLinePersistenceUtil.checkCatalogueLineExistence(catalogueUuid, catalogueLine.getID(), catalogueLine.getHjid());

                if (!lineExists) {
                    service.updateLinesCatalogue(newCatalogueUuid,oldCatalogueUuid,catalogueLine);
                } else {
                    throw new NimbleException(NimbleExceptionMessageCode.NOT_ACCEPTABLE_ALREADY_EXISTS.toString());
                }
                //mdc logging
                Map<String,String> paramMap = LoggerUtil.getMDCParamMapForCatalogueLine(catalogueLine, CatalogueEvent.CATALOGUE_UPDATE);
                LoggerUtils.logWithMDC(log, paramMap, LoggerUtils.LogLevel.INFO, "Catalogue line updated for  catalogue uuid: {}, lineId: {}",
                        catalogueUuid, catalogueLine.getID());
                return ResponseEntity.ok(serializationUtility.serializeUBLObject(catalogueLine));
            }else{
                // validate the incoming content
                ValidationMessages errors = catalogueLineValidator.validate();
                if (errors.getErrorMessages().size() > 0) {
                    throw new NimbleException(errors.getErrorMessages(),errors.getErrorParameters());
                }

                // validate the entity ids
                boolean hjidsBelongToCompany = resourceValidationUtil.hjidsBelongsToParty(catalogueLine, catalogue.getProviderParty().getPartyIdentification().get(0).getID(), Configuration.Standard.UBL.toString());
                if(!hjidsBelongToCompany) {
                    throw new NimbleException(NimbleExceptionMessageCode.BAD_REQUEST_INVALID_HJIDS_IN_LINE.toString(),Arrays.asList(catalogueLineJson));
                }

                // consider the case of an updated line id conflicting with the id of an existing line
                boolean lineExists = CatalogueLinePersistenceUtil.checkCatalogueLineExistence(catalogueUuid, catalogueLine.getID(), catalogueLine.getHjid());
                if (!lineExists) {
                    service.updateCatalogueLine(catalogueLine);
                } else {
                    throw new NimbleException(NimbleExceptionMessageCode.NOT_ACCEPTABLE_ALREADY_EXISTS.toString());
                }

                //mdc logging
                Map<String,String> paramMap = LoggerUtil.getMDCParamMapForCatalogueLine(catalogueLine, CatalogueEvent.PRODUCT_UPDATE);
                LoggerUtils.logWithMDC(log, paramMap, LoggerUtils.LogLevel.INFO, "Catalogue line updated for  catalogue uuid: {}, lineId: {}",
                        catalogueUuid, catalogueLine.getID());
                return ResponseEntity.ok(serializationUtility.serializeUBLObject(catalogueLine));
            }

        } catch (Exception e) {
            throw new NimbleException(NimbleExceptionMessageCode.INTERNAL_SERVER_ERROR_UPDATE_CATALOGUE_LINE.toString(),e);
        }
    }

    @CrossOrigin(origins = {"*"})
    @ApiOperation(value = "", notes = "Deletes the specified catalogue line")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Deleted the catalogue line successfully"),
            @ApiResponse(code = 404, message = "Catalogue with the given uuid does not exist"),
            @ApiResponse(code = 500, message = "Failed to delete the catalogue line")
    })
    @RequestMapping(value = "/catalogue/{catalogueUuid}/catalogueline/{lineId}",
            produces = {"application/json"},
            method = RequestMethod.DELETE)
    public ResponseEntity deleteCatalogueLine(@ApiParam(value = "uuid of the catalogue containing the line to be retrieved. (catalogue.uuid)", required = true) @PathVariable String catalogueUuid,
                                              @ApiParam(value = "Identifier of the catalogue line to be retrieved. (line.id)", required = true) @PathVariable String lineId,
                                              @ApiParam(value = "The Bearer token provided by the identity service", required = true) @RequestHeader(value = "Authorization") String bearerToken) {
        // set request log of ExecutionContext
        String requestLog = String.format("Incoming request to delete catalogue line. catalogue uuid: %s: line lineId %s", catalogueUuid, lineId);
        executionContext.setRequestLog(requestLog);

        log.info(requestLog);
        // validate role
        if(!validationUtil.validateRole(bearerToken, RoleConfig.REQUIRED_ROLES_CATALOGUE)) {
            throw new NimbleException(NimbleExceptionMessageCode.UNAUTHORIZED_INVALID_ROLE.toString());
        }

        if (service.getCatalogue(catalogueUuid) == null) {
            throw new NimbleException(NimbleExceptionMessageCode.NOT_FOUND_NO_CATALOGUE.toString(),Arrays.asList(catalogueUuid));
        }

        try {
            service.deleteCatalogueLineById(catalogueUuid, lineId);
        } catch (Exception e) {
            throw new NimbleException(NimbleExceptionMessageCode.INTERNAL_SERVER_ERROR_DELETE_CATALOGUE_LINE.toString(),e);
        }
        //mdc logging
        Map<String,String> paramMap = new HashMap<String, String>();
        paramMap.put("activity", CatalogueEvent.PRODUCT_DELETE.getActivity());
        paramMap.put("productId", lineId);
        LoggerUtils.logWithMDC(log, paramMap, LoggerUtils.LogLevel.INFO, "Completed the request to delete catalogue line: catalogue uuid: {}, lineId: {}",
                catalogueUuid, lineId);
        return ResponseEntity.status(HttpStatus.OK).build();
    }

    @RequestMapping(value = "/cataloguelines/statistics",
            produces = {"application/json"},
            method = RequestMethod.GET)
    public ResponseEntity getCatalogue(
            @ApiParam(value = "The Bearer token provided by the identity service", required = true) @RequestHeader(value = "Authorization", required = true) String bearerToken) {

        // set request log of ExecutionContext
        String requestLog = "Incoming request to get catalogue line statistics no of products and services";
        executionContext.setRequestLog(requestLog);

        log.info(requestLog);
        // validate role
        if(!validationUtil.validateRole(bearerToken, RoleConfig.REQUIRED_ROLES_CATALOGUE)) {
            throw new NimbleException(NimbleExceptionMessageCode.UNAUTHORIZED_INVALID_ROLE.toString());
        }

       ProductAndServiceStatistics stats;
        try {
            stats = service.getProductAndServiceCount();
        } catch (Exception e) {
            throw new NimbleException(NimbleExceptionMessageCode.INTERNAL_SERVER_ERROR_GET_PRODUCT_AND_SERVICE_COUNT.toString(),e);
        }

        log.info("Completed the request to get product and service count");
        return ResponseEntity.ok(serializationUtility.serializeUBLObject(stats));
    }

    private ResponseEntity createErrorResponseEntity(String msg, HttpStatus status, Exception e) {
        msg = msg + "\n" + e.getMessage();
        log.error(msg, e);
        return ResponseEntity.status(status).body(msg);
    }
}
