package eu.nimble.service.catalogue.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import eu.nimble.service.catalogue.CatalogueService;
import eu.nimble.service.catalogue.CatalogueServiceImpl;
import eu.nimble.service.catalogue.config.CatalogueServiceConfig;
import eu.nimble.service.catalogue.persistence.util.CatalogueLinePersistenceUtil;
import eu.nimble.service.catalogue.util.SpringBridge;
import eu.nimble.service.catalogue.validation.CatalogueLineValidator;
import eu.nimble.service.model.ubl.catalogue.CatalogueType;
import eu.nimble.service.model.ubl.commonaggregatecomponents.CatalogueLineType;
import eu.nimble.utility.Configuration;
import eu.nimble.utility.HttpResponseUtil;
import eu.nimble.utility.JsonSerializationUtility;
import eu.nimble.utility.persistence.resource.ResourceValidationUtility;
import eu.nimble.utility.serialization.TransactionEnabledSerializationUtility;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.logging.LogLevel;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;

/**
 * Created by suat on 22-Aug-17.
 * <p>
 * Catalogue line-level REST services. All services defined in this class are prefixed with the
 * "/catalogue/{catalogueUuid}" path where the {@catalogueId} is the unique identifier of the specified
 * catalogue. All services in this class work only on the UBL based catalogues.
 */
@Controller
@RequestMapping(value = "/catalogue/{catalogueUuid}")
public class CatalogueLineController {
    private static Logger log = LoggerFactory.getLogger(CatalogueLineController.class);

    @Autowired
    private CatalogueServiceConfig catalogueServiceConfig;
    @Autowired
    private TransactionEnabledSerializationUtility serializationUtility;
    @Autowired
    private ResourceValidationUtility resourceValidationUtil;

    private CatalogueService service = CatalogueServiceImpl.getInstance();

    @CrossOrigin(origins = {"*"})
    @ApiOperation(value = "", notes = "Retrieves the catalogue line specified with the catalogueUuid and lineId parameters")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Retrieved catalogue line successfully", response = CatalogueLineType.class),
            @ApiResponse(code = 400, message = "Failed to get catalogue line"),
            @ApiResponse(code = 404, message = "Specified catalogue or catalogue line does not exist"),
            @ApiResponse(code = 500, message = "Unexpected error while getting catalogue line")
    })
    @RequestMapping(value = "/catalogueline/{lineId}",
            produces = {"application/json"},
            method = RequestMethod.GET)
    public ResponseEntity getCatalogueLine(@ApiParam(value = "uuid of the catalogue containing the line to be retrieved. (catalogue.uuid)", required = true) @PathVariable String catalogueUuid,
                                           @ApiParam(value = "Identifier of the catalogue line to be retrieved. (line.id)", required = true) @PathVariable String lineId,
                                           @ApiParam(value = "The Bearer token provided by the identity service", required = true) @RequestHeader(value = "Authorization") String bearerToken) {
        log.info("Incoming request to get catalogue line with lineId: {}", lineId);
        // check token
        ResponseEntity tokenCheck = eu.nimble.service.catalogue.util.HttpResponseUtil.checkToken(bearerToken);
        if (tokenCheck != null) {
            return tokenCheck;
        }

        if (service.getCatalogue(catalogueUuid) == null) {
            log.error("Catalogue with uuid : {} does not exist", catalogueUuid);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(String.format("Catalogue with uuid %s does not exist", catalogueUuid));
        }

        CatalogueLineType catalogueLine;
        try {
            catalogueLine = service.getCatalogueLine(catalogueUuid, lineId);
        } catch (Exception e) {
            return createErrorResponseEntity("Failed to get catalogue line", HttpStatus.INTERNAL_SERVER_ERROR, e);
        }

        if (catalogueLine == null) {
            log.error("There does not exist a catalogue line with lineId {}", lineId);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(String.format("There does not exist a catalogue line with lineId %s", lineId));
        }
        log.info("Completed the request to get catalogue line with lineId: {}", lineId);
        return ResponseEntity.ok(serializationUtility.serializeUBLObject(catalogueLine));
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
    @RequestMapping( value = "/catalogueline",
            consumes = {"application/json"},
            produces = {"application/json"},
            method = RequestMethod.POST)
    public ResponseEntity addCatalogueLine(@ApiParam(value = "uuid of the catalogue containing the line to be retrieved. (catalogue.uuid)", required = true) @PathVariable String catalogueUuid,
                                           @ApiParam(value = "Serialized form of the catalogue line. Valid serializations can be achieved via JsonSerializationUtility.getObjectMapper method located in the utility module. An example catalogue line serialization can be found in: https://github.com/nimble-platform/catalog-service/tree/staging/catalogue-service-micro/src/main/resources/example_content/catalogue_line.json", required = true) @RequestBody String catalogueLineJson,
                                           @ApiParam(value = "The Bearer token provided by the identity service", required = true) @RequestHeader(value = "Authorization") String bearerToken) {
        log.info("Incoming request to add catalogue line to catalogue: {}", catalogueUuid);
        CatalogueType catalogue;
        CatalogueLineType catalogueLine;

        try {
            // check token
            ResponseEntity tokenCheck = eu.nimble.service.catalogue.util.HttpResponseUtil.checkToken(bearerToken);
            if (tokenCheck != null) {
                return tokenCheck;
            }

            // get owning catalogue
            catalogue = service.getCatalogue(catalogueUuid);
            if (catalogue == null) {
                log.error("Catalogue with uuid : {} does not exist", catalogueUuid);
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(String.format("Catalogue with uuid %s does not exist", catalogueUuid));
            }

            // parse catalogue line
            try {
                catalogueLine = JsonSerializationUtility.getObjectMapper().readValue(catalogueLineJson, CatalogueLineType.class);
            } catch (IOException e) {
                log.warn("The following catalogue line could not be created: {}", catalogueLineJson);
                return HttpResponseUtil.createResponseEntityAndLog(String.format("Failed to deserialize catalogue line: %s", catalogueLineJson), e, HttpStatus.BAD_REQUEST, LogLevel.ERROR);
            }

            // validate the incoming content
            CatalogueLineValidator catalogueLineValidator = new CatalogueLineValidator(catalogue, catalogueLine);
            List<String> errors = catalogueLineValidator.validate();
            if (errors.size() > 0) {
                StringBuilder sb = new StringBuilder("");
                for (String error : errors) {
                    sb.append(error).append(System.lineSeparator());
                }
                return HttpResponseUtil.createResponseEntityAndLog(sb.toString(), null, HttpStatus.BAD_REQUEST, LogLevel.WARN);
            }

            // check the entity ids
            boolean hjidsExists = resourceValidationUtil.hjidsExit(catalogueLine);
            if(hjidsExists) {
                return HttpResponseUtil.createResponseEntityAndLog(String.format("Entity IDs (hjid fields) found in the passed catalogue line: %s. Make sure they are null", catalogueLineJson), null, HttpStatus.BAD_REQUEST, LogLevel.INFO);
            }

            // check duplicate line
            boolean lineExists = CatalogueLinePersistenceUtil.checkCatalogueLineExistence(catalogueUuid, catalogueLine.getID());
            if (!lineExists) {
                catalogueLine = service.addLineToCatalogue(catalogue, catalogueLine);
            } else {
                return ResponseEntity.status(HttpStatus.NOT_ACCEPTABLE).body("There already exists a product with the given id");
            }

        } catch (Exception e) {
            log.warn("The following catalogue line could not be created: {}", catalogueLineJson);
            return createErrorResponseEntity("Failed to add the provided catalogue line", HttpStatus.INTERNAL_SERVER_ERROR, e);
        }

        log.info("Completed request to add catalogue line: {} to catalogue: {}", catalogueLine.getID(), catalogue.getUUID());
        return createCreatedCatalogueLineResponse(catalogueUuid, catalogueLine);
    }

    private ResponseEntity createCreatedCatalogueLineResponse(String catalogueUuid, CatalogueLineType line) {
        URI lineURI;
        try {
            String applicationUrl = catalogueServiceConfig.getSpringApplicationUrl();
            lineURI = new URI(applicationUrl + "/catalogue/" + catalogueUuid + "/" + line.getID());
        } catch (URISyntaxException e) {
            String msg = "Failed to generate a URI for the newly created item";
            log.warn(msg, e);
            try {
                log.info("Completed request to add catalogue line with an empty URI, catalogue uuid: {}, lineId: {}", catalogueUuid, line.getID());
                return ResponseEntity.created(new URI("")).body(serializationUtility.serializeUBLObject(line));
            } catch (URISyntaxException e1) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("WTF");
            }
        }
        log.info("Completed request to add catalogue, catalogue uuid: {}, lineId: {}", catalogueUuid, line.getID());
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
    @RequestMapping( value = "/catalogueline",
            consumes = {"application/json"},
            produces = {"application/json"},
            method = RequestMethod.PUT)
    public ResponseEntity updateCatalogueLine(@ApiParam(value = "uuid of the catalogue containing the line to be retrieved. (catalogue.uuid)", required = true) @PathVariable String catalogueUuid,
                                              @ApiParam(value = "Serialized form of the catalogue line. Valid serializations can be achieved via JsonSerializationUtility.getObjectMapper method located in the utility module. An example catalogue line serialization can be found in: https://github.com/nimble-platform/catalog-service/tree/staging/catalogue-service-micro/src/main/resources/example_content/catalogue_line.json.", required = true) @RequestBody String catalogueLineJson,
                                              @ApiParam(value = "The Bearer token provided by the identity service", required = true) @RequestHeader(value = "Authorization") String bearerToken) {
        try {
            // check token
            ResponseEntity tokenCheck = eu.nimble.service.catalogue.util.HttpResponseUtil.checkToken(bearerToken);
            if (tokenCheck != null) {
                return tokenCheck;
            }

            CatalogueLineType catalogueLine;

            //parse catalogue line
            try {
                ObjectMapper objectMapper = JsonSerializationUtility.getObjectMapper();
                catalogueLine = objectMapper.readValue(catalogueLineJson, CatalogueLineType.class);

            } catch (IOException e) {
                log.warn("The following catalogue line could not be updated: {}", catalogueLineJson);
                return HttpResponseUtil.createResponseEntityAndLog(String.format("Failed to deserialize catalogue line from json string: %s", catalogueLineJson), e, HttpStatus.BAD_REQUEST, LogLevel.ERROR);
            }

            log.info("Incoming request to update catalogue line. Catalogue uuid: {}", catalogueUuid);
            CatalogueType catalogue = service.getCatalogue(catalogueUuid);
            if (catalogue == null) {
                log.error("Catalogue with uuid : {} does not exist", catalogueUuid);
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(String.format("Catalogue with uuid %s does not exist", catalogueUuid));
            }

            // validate the incoming content
            CatalogueLineValidator catalogueLineValidator = new CatalogueLineValidator(catalogue, catalogueLine);
            List<String> errors = catalogueLineValidator.validate();
            if (errors.size() > 0) {
                StringBuilder sb = new StringBuilder("");
                for (String error : errors) {
                    sb.append(error).append(System.lineSeparator());
                }
                return HttpResponseUtil.createResponseEntityAndLog(sb.toString(), null, HttpStatus.BAD_REQUEST, LogLevel.WARN);
            }

            // validate the entity ids
            boolean hjidsBelongToCompany = resourceValidationUtil.hjidsBelongsToParty(catalogueLine, catalogue.getProviderParty().getID(), Configuration.Standard.UBL.toString());
            if(!hjidsBelongToCompany) {
                return HttpResponseUtil.createResponseEntityAndLog(String.format("Some of the identifiers (hjid fields) do not belong to the party in the passed catalogue line: %s.", catalogueLineJson), null, HttpStatus.BAD_REQUEST, LogLevel.INFO);
            }

            // consider the case of an updated line id conflicting with the id of an existing line
            boolean lineExists = CatalogueLinePersistenceUtil.checkCatalogueLineExistence(catalogueUuid, catalogueLine.getID(), catalogueLine.getHjid());
            if (!lineExists) {
                service.updateCatalogueLine(catalogueLine);
            } else {
                return ResponseEntity.status(HttpStatus.NOT_ACCEPTABLE).body("There already exists a product with the given id");
            }

            log.info("Completed the request to add catalogue line catalogue uuid, line lineId: {}", catalogueUuid, catalogueLine.getID());
            return ResponseEntity.ok(serializationUtility.serializeUBLObject(catalogueLine));

        } catch (Exception e) {
            log.warn("The following catalogue line could not be updated: {}", catalogueLineJson);
            return createErrorResponseEntity("Failed to add the provided catalogue line", HttpStatus.INTERNAL_SERVER_ERROR, e);
        }
    }

    @CrossOrigin(origins = {"*"})
    @ApiOperation(value = "", notes = "Deletes the specified catalogue line")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Deleted the catalogue line successfully"),
            @ApiResponse(code = 404, message = "Catalogue with the given uuid does not exist"),
            @ApiResponse(code = 500, message = "Failed to delete the catalogue line")
    })
    @RequestMapping(value = "/catalogueline/{lineId}",
            produces = {"application/json"},
            method = RequestMethod.DELETE)
    public ResponseEntity deleteCatalogueLine(@ApiParam(value = "uuid of the catalogue containing the line to be retrieved. (catalogue.uuid)", required = true) @PathVariable String catalogueUuid,
                                              @ApiParam(value = "Identifier of the catalogue line to be retrieved. (line.id)", required = true) @PathVariable String lineId,
                                              @ApiParam(value = "The Bearer token provided by the identity service", required = true) @RequestHeader(value = "Authorization") String bearerToken) {
        log.info("Incoming request to delete catalogue line. catalogue uuid: {}: line lineId {}", catalogueUuid, lineId);
        // check token
        ResponseEntity tokenCheck = eu.nimble.service.catalogue.util.HttpResponseUtil.checkToken(bearerToken);
        if (tokenCheck != null) {
            return tokenCheck;
        }

        if (service.getCatalogue(catalogueUuid) == null) {
            log.error("Catalogue with uuid : {} does not exist", catalogueUuid);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(String.format("Catalogue with uuid %s does not exist", catalogueUuid));
        }

        try {
            service.deleteCatalogueLineById(catalogueUuid, lineId);
        } catch (Exception e) {
            return createErrorResponseEntity("Failed to delete the catalogue line. catalogue uuid: " + catalogueUuid + " line id: " + lineId, HttpStatus.INTERNAL_SERVER_ERROR, e);
        }
        log.info("Completed the request to delete catalogue line: catalogue uuid: {}, lineId: {}", catalogueUuid, lineId);
        return ResponseEntity.status(HttpStatus.OK).build();
    }

    private ResponseEntity createErrorResponseEntity(String msg, HttpStatus status, Exception e) {
        msg = msg + e.getMessage();
        log.error(msg, e);
        return ResponseEntity.status(status).body(msg);
    }
}
