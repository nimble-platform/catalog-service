package eu.nimble.service.catalogue.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import eu.nimble.service.catalogue.CatalogueService;
import eu.nimble.service.model.ubl.commonaggregatecomponents.CatalogueLineType;
import eu.nimble.service.model.ubl.commonaggregatecomponents.PriceOptionType;
import eu.nimble.utility.Configuration;
import eu.nimble.utility.HttpResponseUtil;
import eu.nimble.utility.JsonSerializationUtility;
import eu.nimble.utility.persistence.resource.EntityIdAwareRepositoryWrapper;
import eu.nimble.utility.persistence.resource.ResourceValidationUtility;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.logging.LogLevel;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

/**
 * <p>
 * This service is used to manage pricing options {@link eu.nimble.service.model.ubl.commonaggregatecomponents.CatalogueLineType}s.
 * The {@link eu.nimble.service.model.ubl.commonaggregatecomponents.PriceOptionType}s are used to specify distinct prices for
 * various parameters.
 * </p>
 * <p>
 * <p>Created by suat on 01-Aug-18.</p>
 */
@Controller
@RequestMapping(value = "/catalogue/{catalogueUuid}/catalogueline/{lineId}/price-options")
public class PriceConfigurationController {
    private static Logger log = LoggerFactory.getLogger(PriceConfigurationController.class);

    @Autowired
    private ResourceValidationUtility resourceValidationUtil;
    @Autowired
    private CatalogueService service;

    @CrossOrigin(origins = {"*"})
    @ApiOperation(value = "", notes = "Adds the provided price option to the specified catalogue line (i.e. product/service)")
    @ApiResponses(value = {
            @ApiResponse(code = 201, message = "Added the pricing option successfully", response = PriceOptionType.class),
            @ApiResponse(code = 400, message = "Invalid price option serialization"),
            @ApiResponse(code = 404, message = "No catalogue or catalogue line found for the specified parameters"),
            @ApiResponse(code = 500, message = "Unexpected error while adding price option")
    })
    @RequestMapping(consumes = {MediaType.APPLICATION_JSON_VALUE},
            produces = {MediaType.APPLICATION_JSON_VALUE},
            method = RequestMethod.POST)
    public ResponseEntity addPricingOption(@ApiParam(value = "uuid of the catalogue containing the line for which the price option to be added. (catalogue.uuid)", required = true) @PathVariable("catalogueUuid") String catalogueUuid,
                                           @ApiParam(value = "Identifier of the catalogue line to which the price option to be added. (lineId.id)", required = true) @PathVariable("lineId") String lineId,
                                           @ApiParam(value = "Serialized form of PriceOptionType instance. An example price serialization can be found in: https://github.com/nimble-platform/catalog-service/tree/staging/catalogue-service-micro/src/main/resources/example_content/price_option.json", required = true) @RequestBody PriceOptionType priceOption,
                                           @ApiParam(value = "The Bearer token provided by the identity service", required = true) @RequestHeader(value = "Authorization") String bearerToken) {
        log.info("Incoming request to add pricing option. catalogueId: {}, lineId: {}", catalogueUuid, lineId);
        try {
            // check token
            ResponseEntity tokenCheck = eu.nimble.service.catalogue.util.HttpResponseUtil.checkToken(bearerToken);
            if (tokenCheck != null) {
                return tokenCheck;
            }

            // check catalogue
            if (service.getCatalogue(catalogueUuid) == null) {
                String msg = String.format("Catalogue with uuid : {} does not exist", catalogueUuid);
                log.info(msg);
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(String.format("Catalogue with uuid %s does not exist", catalogueUuid));
            }

            // check the entity ids
            boolean hjidsExists = resourceValidationUtil.hjidsExit(priceOption);
            if(hjidsExists) {
                return HttpResponseUtil.createResponseEntityAndLog(String.format("Entity IDs (hjid fields) found in the passed price option: %s. Make sure they are null", JsonSerializationUtility.serializeEntitySilently(priceOption)), null, HttpStatus.BAD_REQUEST, LogLevel.INFO);
            }

            // check catalogue line
            CatalogueLineType catalogueLine = service.getCatalogueLine(catalogueUuid, lineId);
            if (catalogueLine == null) {
                String msg = String.format("Catalogue line does not exist. catalogueId: %s, lineId: %s", catalogueUuid, lineId);
                log.info(msg);
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(msg);
            }

            // first persist the price options
            EntityIdAwareRepositoryWrapper repositoryWrapper = new EntityIdAwareRepositoryWrapper(catalogueLine.getGoodsItem().getItem().getManufacturerParty().getID());
            repositoryWrapper.persistEntity(priceOption);

            // update the catalogue line
            catalogueLine.getPriceOption().add(priceOption);
            repositoryWrapper = new EntityIdAwareRepositoryWrapper(catalogueLine.getGoodsItem().getItem().getManufacturerParty().getID());
            repositoryWrapper.updateEntity(catalogueLine);

            ObjectMapper objectMapper = JsonSerializationUtility.getObjectMapper();
            objectMapper.configure(MapperFeature.PROPAGATE_TRANSIENT_MARKER, false);

            log.info("Completed request to add pricing option. catalogueId: {}, lineId: {}", catalogueUuid, lineId);
            return ResponseEntity.created(new URI(priceOption.getHjid().toString())).body(priceOption);

        } catch (Exception e) {
            String serializedOption;
            try {
                serializedOption = JsonSerializationUtility.getObjectMapper().writeValueAsString(priceOption);
                String msg = String.format("Failed to add pricing option for catalogueId: %s, lineId: %s, pricingOption: %s", catalogueUuid, lineId, serializedOption);
                return HttpResponseUtil.createResponseEntityAndLog(msg, e, HttpStatus.INTERNAL_SERVER_ERROR, LogLevel.ERROR);
            } catch (JsonProcessingException e1) {
                String msg = String.format("Failed to add pricing option for catalogueId: %s, lineId: %s", catalogueUuid, lineId);
                ResponseEntity response = HttpResponseUtil.createResponseEntityAndLog(msg, e, HttpStatus.INTERNAL_SERVER_ERROR, LogLevel.ERROR);
                log.error("Failed to deserialize pricing option: catalogueId: {}, lineId: {}", catalogueUuid, lineId, e1);
                return response;
            }
        }
    }

    @CrossOrigin(origins = {"*"})
    @ApiOperation(value = "", notes = "Adds the given pricing option to the specified catalogue line (i.e. product/service)")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Deleted the pricing option successfully"),
            @ApiResponse(code = 404, message = "No catalogue or catalogue line found for the specified parameters")
    })
    @RequestMapping(value = "/{optionId}",
            method = RequestMethod.DELETE)
    public ResponseEntity deletePricingOption(@ApiParam(value = "uuid of the catalogue containing the line for which the price option to be deleted. (catalogue.uuid)", required = true) @PathVariable("catalogueUuid") String catalogueUuid,
                                              @ApiParam(value = "Identifier of the catalogue line from which the price option to be deleted. (lineId.id)", required = true) @PathVariable("lineId") String lineId,
                                              @ApiParam(value = "Identifier of the price option to be deleted. (priceOption.hjid)", required = true) @PathVariable("optionId") Long optionId,
                                              @ApiParam(value = "The Bearer token provided by the identity service", required = true) @RequestHeader(value = "Authorization") String bearerToken) {
        log.info("Incoming request to delete pricing option. catalogueId: {}, lineId: {}, optionId: {}", catalogueUuid, lineId, optionId);
        try {
            // check token
            ResponseEntity tokenCheck = eu.nimble.service.catalogue.util.HttpResponseUtil.checkToken(bearerToken);
            if (tokenCheck != null) {
                return tokenCheck;
            }

            // check catalogue
            if (service.getCatalogue(catalogueUuid) == null) {
                String msg = String.format("Catalogue with uuid : {} does not exist", catalogueUuid);
                log.info(msg);
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(String.format("Catalogue with uuid %s does not exist", catalogueUuid));
            }

            // check catalogue line
            CatalogueLineType catalogueLine = service.getCatalogueLine(catalogueUuid, lineId);
            if (catalogueLine == null) {
                String msg = String.format("Catalogue line does not exist. catalogueId: %s, lineId: %s", catalogueUuid, lineId);
                log.info(msg);
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(msg);
            }

            // check option
            Integer optionIndex = null;
            List<PriceOptionType> options = catalogueLine.getPriceOption();
            for (int i = 0; i < options.size(); i++) {
                if (options.get(i).getHjid().equals(optionId)) {
                    optionIndex = i;
                    break;
                }
            }
            if (optionIndex == null) {
                String msg = String.format("No option exists. catalogueId: %s, lineId: %s, optionId: %d", catalogueUuid, lineId, optionId);
                log.info(msg);
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(msg);
            }

            // remove the option and update the line
            EntityIdAwareRepositoryWrapper repositoryWrapper = new EntityIdAwareRepositoryWrapper(catalogueLine.getGoodsItem().getItem().getManufacturerParty().getID());
            repositoryWrapper.deleteEntityByHjid(PriceOptionType.class, optionId);
//            catalogueLine.getPriceOption().remove(optionIndex.intValue());
//            repositoryWrapper.updateEntity(catalogueLine);

            log.info("Completed request to delete pricing option. catalogueId: {}, lineId: {}, optionId: {}", catalogueUuid, lineId, optionId);
            return ResponseEntity.ok().build();

        } catch (Exception e) {
            String msg = String.format("Unexpected error while deleting price option catalogueId: %s, lineId: %s, pricingOptionId: %s", catalogueUuid, lineId, optionId);
            return HttpResponseUtil.createResponseEntityAndLog(msg, e, HttpStatus.INTERNAL_SERVER_ERROR, LogLevel.ERROR);
        }
    }

    @CrossOrigin(origins = {"*"})
    @ApiOperation(value = "", notes = "Adds the given pricing option to the specified catalogue line (i.e. product/service)")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Added the pricing option successfully", response = PriceOptionType.class),
            @ApiResponse(code = 400, message = "Invalid price option serialization"),
            @ApiResponse(code = 404, message = "No catalogue or catalogue line found for the specified parameters"),
            @ApiResponse(code = 500, message = "Unexpected error while updating price option")
    })
    @RequestMapping(consumes = {MediaType.APPLICATION_JSON_VALUE},
            produces = {MediaType.APPLICATION_JSON_VALUE},
            method = RequestMethod.PUT)
    public ResponseEntity updatePricingOption(@ApiParam(value = "uuid of the catalogue containing the line for which the price option to be deleted. (catalogue.uuid)", required = true) @PathVariable("catalogueUuid") String catalogueUuid,
                                              @ApiParam(value = "Identifier of the catalogue line to which the price option to be updated. (lineId.id)", required = true) @PathVariable("lineId") String lineId,
                                              @ApiParam(value = "Serialized form of PriceOptionType instance to be updated. An example price serialization can be found in: https://github.com/nimble-platform/catalog-service/tree/staging/catalogue-service-micro/src/main/resources/example_content/price_option.json", required = true) @RequestBody PriceOptionType priceOption,
                                              @ApiParam(value = "The Bearer token provided by the identity service", required = true) @RequestHeader(value = "Authorization") String bearerToken) {
        log.info("Incoming request to delete pricing option. catalogueId: {}, lineId: {}, optionId: {}", catalogueUuid, lineId, priceOption.getHjid());

        try {
            // check token
            ResponseEntity tokenCheck = eu.nimble.service.catalogue.util.HttpResponseUtil.checkToken(bearerToken);
            if (tokenCheck != null) {
                return tokenCheck;
            }

            // check catalogue
            if (service.getCatalogue(catalogueUuid) == null) {
                String msg = String.format("Catalogue with uuid : {} does not exist", catalogueUuid);
                log.info(msg);
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(String.format("Catalogue with uuid %s does not exist", catalogueUuid));
            }

            // check catalogue line
            CatalogueLineType catalogueLine = service.getCatalogueLine(catalogueUuid, lineId);
            if (catalogueLine == null) {
                String msg = String.format("Catalogue line does not exist. catalogueId: %s, lineId: %s", catalogueUuid, lineId);
                log.info(msg);
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(msg);
            }

            // validate the entity ids
            boolean hjidsBelongToCompany = resourceValidationUtil.hjidsBelongsToParty(priceOption, catalogueLine.getGoodsItem().getItem().getManufacturerParty().getID(), Configuration.Standard.UBL.toString());
            if(!hjidsBelongToCompany) {
                return HttpResponseUtil.createResponseEntityAndLog(String.format("Some of the identifiers (hjid fields) do not belong to the party in the passed catalogue: %s", JsonSerializationUtility.serializeEntitySilently(priceOption)), null, HttpStatus.BAD_REQUEST, LogLevel.INFO);
            }

            // check option
            PriceOptionType oldOption = null;
            List<PriceOptionType> options = catalogueLine.getPriceOption();
            for (int i = 0; i < options.size(); i++) {
                if (options.get(i).getHjid().equals(priceOption.getHjid())) {
                    oldOption = options.get(i);
                    break;
                }
            }
            if (oldOption == null) {
                String msg = String.format("No option exists. catalogueId: %s, lineId: %s, optionId: %d", catalogueUuid, lineId, oldOption.getHjid());
                log.info(msg);
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(msg);
            }

            // remove the option and update the line
            EntityIdAwareRepositoryWrapper repositoryWrapper = new EntityIdAwareRepositoryWrapper(catalogueLine.getGoodsItem().getItem().getManufacturerParty().getID());
            priceOption = repositoryWrapper.updateEntity(priceOption);

            log.info("Completed request to update price option. catalogueId: {}, lineId: {}, optionId: {}", catalogueUuid, lineId, priceOption.getHjid());
            return ResponseEntity.ok().body(JsonSerializationUtility.getMapperForTransientFields().writeValueAsString(priceOption));

        } catch (Exception e) {
            String msg = String.format("Unexpected error while deleting price option catalogueId: %s, lineId: %s, optionId: %d", catalogueUuid, lineId, priceOption.getHjid());
            return HttpResponseUtil.createResponseEntityAndLog(msg, e, HttpStatus.INTERNAL_SERVER_ERROR, LogLevel.ERROR);
        }
    }
}
