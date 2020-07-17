package eu.nimble.service.catalogue.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.Unirest;
import eu.nimble.service.catalogue.CatalogueService;
import eu.nimble.service.catalogue.config.RoleConfig;
import eu.nimble.service.catalogue.exception.NimbleExceptionMessageCode;
import eu.nimble.service.model.ubl.commonaggregatecomponents.CatalogueLineType;
import eu.nimble.service.model.ubl.commonaggregatecomponents.PriceOptionType;
import eu.nimble.utility.Configuration;
import eu.nimble.utility.ExecutionContext;
import eu.nimble.utility.JsonSerializationUtility;
import eu.nimble.utility.exception.NimbleException;
import eu.nimble.utility.persistence.resource.EntityIdAwareRepositoryWrapper;
import eu.nimble.utility.persistence.resource.ResourceValidationUtility;
import eu.nimble.utility.validation.IValidationUtil;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.Arrays;
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
public class PriceConfigurationController {
    private static final String VAT_RATES_URL = "https://taxapi.io/api/v1/vat/rates";

    private static Logger log = LoggerFactory.getLogger(PriceConfigurationController.class);

    @Autowired
    private ResourceValidationUtility resourceValidationUtil;
    @Autowired
    private CatalogueService service;
    @Autowired
    private IValidationUtil validationUtil;
    @Autowired
    private ExecutionContext executionContext;

    @CrossOrigin(origins = {"*"})
    @ApiOperation(value = "", notes = "Adds the provided price option to the specified catalogue line (i.e. product/service)")
    @ApiResponses(value = {
            @ApiResponse(code = 201, message = "Added the pricing option successfully", response = PriceOptionType.class),
            @ApiResponse(code = 400, message = "Invalid price option serialization"),
            @ApiResponse(code = 401, message = "Invalid token. No user was found for the provided token"),
            @ApiResponse(code = 404, message = "No catalogue or catalogue line found for the specified parameters"),
            @ApiResponse(code = 500, message = "Unexpected error while adding price option")
    })
    @RequestMapping(
            value = "/catalogue/{catalogueUuid}/catalogueline/{lineId}/price-options",
            consumes = {MediaType.APPLICATION_JSON_VALUE},
            produces = {MediaType.APPLICATION_JSON_VALUE},
            method = RequestMethod.POST)
    public ResponseEntity addPricingOption(@ApiParam(value = "uuid of the catalogue containing the line for which the price option to be added. (catalogue.uuid)", required = true) @PathVariable("catalogueUuid") String catalogueUuid,
                                           @ApiParam(value = "Identifier of the catalogue line to which the price option to be added. (lineId.id)", required = true) @PathVariable("lineId") String lineId,
                                           @ApiParam(value = "Serialized form of PriceOptionType instance. An example price serialization can be found in: https://github.com/nimble-platform/catalog-service/tree/staging/catalogue-service-micro/src/main/resources/example_content/price_option.json", required = true) @RequestBody PriceOptionType priceOption,
                                           @ApiParam(value = "The Bearer token provided by the identity service", required = true) @RequestHeader(value = "Authorization") String bearerToken) {
        // set request log of ExecutionContext
        String requestLog = String.format("Incoming request to add pricing option. catalogueId: %s, lineId: %s", catalogueUuid, lineId);
        executionContext.setRequestLog(requestLog);

        log.info(requestLog);
        try {
            // validate role
            if(!validationUtil.validateRole(bearerToken,executionContext.getUserRoles(), RoleConfig.REQUIRED_ROLES_CATALOGUE_WRITE)) {
                throw new NimbleException(NimbleExceptionMessageCode.UNAUTHORIZED_INVALID_ROLE.toString());
            }

            // check catalogue
            if (service.getCatalogue(catalogueUuid) == null) {
                throw new NimbleException(NimbleExceptionMessageCode.NOT_FOUND_NO_CATALOGUE.toString(), Arrays.asList(catalogueUuid));
            }

            // check the entity ids
            boolean hjidsExists = resourceValidationUtil.hjidsExit(priceOption);
            if(hjidsExists) {
                throw new NimbleException(NimbleExceptionMessageCode.BAD_REQUEST_HJIDS_IN_PRICE_OPTION.toString(),Arrays.asList(JsonSerializationUtility.serializeEntitySilently(priceOption)));
            }

            // check catalogue line
            CatalogueLineType catalogueLine = service.getCatalogueLine(catalogueUuid, lineId);
            if (catalogueLine == null) {
                throw new NimbleException(NimbleExceptionMessageCode.NOT_FOUND_NO_CATALOGUE_LINE.toString(),Arrays.asList(catalogueUuid, lineId));
            }

            // first persist the price options
            EntityIdAwareRepositoryWrapper repositoryWrapper = new EntityIdAwareRepositoryWrapper(catalogueLine.getGoodsItem().getItem().getManufacturerParty().getPartyIdentification().get(0).getID());
            repositoryWrapper.persistEntity(priceOption);

            // update the catalogue line
            catalogueLine.getPriceOption().add(priceOption);
            repositoryWrapper = new EntityIdAwareRepositoryWrapper(catalogueLine.getGoodsItem().getItem().getManufacturerParty().getPartyIdentification().get(0).getID());
            repositoryWrapper.updateEntity(catalogueLine);

            ObjectMapper objectMapper = JsonSerializationUtility.getObjectMapper();
            objectMapper.configure(MapperFeature.PROPAGATE_TRANSIENT_MARKER, false);

            log.info("Completed request to add pricing option. catalogueId: {}, lineId: {}", catalogueUuid, lineId);
            return ResponseEntity.created(new URI(priceOption.getHjid().toString())).body(priceOption);

        } catch (Exception e) {
            String serializedOption;
            try {
                serializedOption = JsonSerializationUtility.getObjectMapper().writeValueAsString(priceOption);
                throw new NimbleException(NimbleExceptionMessageCode.INTERNAL_SERVER_ERROR_ADD_PRICE_OPTION.toString(),Arrays.asList(catalogueUuid, lineId, serializedOption));
            } catch (JsonProcessingException e1) {
                log.error("Failed to deserialize pricing option: catalogueId: {}, lineId: {}", catalogueUuid, lineId, e1);
                throw new NimbleException(NimbleExceptionMessageCode.INTERNAL_SERVER_ERROR_ADD_PRICE_OPTION.toString(),Arrays.asList(catalogueUuid, lineId, ""),e1);
            }
        }
    }

    @CrossOrigin(origins = {"*"})
    @ApiOperation(value = "", notes = "Adds the given pricing option to the specified catalogue line (i.e. product/service)")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Deleted the pricing option successfully"),
            @ApiResponse(code = 401, message = "Invalid token. No user was found for the provided token"),
            @ApiResponse(code = 404, message = "No catalogue or catalogue line found for the specified parameters")
    })
    @RequestMapping(value = "/catalogue/{catalogueUuid}/catalogueline/price-options/{optionId}",
            method = RequestMethod.DELETE)
    public ResponseEntity deletePricingOption(@ApiParam(value = "uuid of the catalogue containing the line for which the price option to be deleted. (catalogue.uuid)", required = true) @PathVariable("catalogueUuid") String catalogueUuid,
                                              @ApiParam(value = "Identifier of the catalogue line from which the price option to be deleted. (lineId.id)", required = true) @RequestParam(value = "lineId") String lineId,
                                              @ApiParam(value = "Identifier of the price option to be deleted. (priceOption.hjid)", required = true) @PathVariable("optionId") Long optionId,
                                              @ApiParam(value = "The Bearer token provided by the identity service", required = true) @RequestHeader(value = "Authorization") String bearerToken) {
        // set request log of ExecutionContext
        String requestLog = String.format("Incoming request to delete pricing option. catalogueId: %s, lineId: %s, optionId: %s", catalogueUuid, lineId, optionId);
        executionContext.setRequestLog(requestLog);

        log.info(requestLog);
        try {
            // validate role
            if(!validationUtil.validateRole(bearerToken, executionContext.getUserRoles(),RoleConfig.REQUIRED_ROLES_CATALOGUE_WRITE)) {
                throw new NimbleException(NimbleExceptionMessageCode.UNAUTHORIZED_INVALID_ROLE.toString());
            }

            // check catalogue
            if (service.getCatalogue(catalogueUuid) == null) {
                throw new NimbleException(NimbleExceptionMessageCode.NOT_FOUND_NO_CATALOGUE.toString(),Arrays.asList(catalogueUuid));
            }

            // check catalogue line
            CatalogueLineType catalogueLine = service.getCatalogueLine(catalogueUuid, lineId);
            if (catalogueLine == null) {
                throw new NimbleException(NimbleExceptionMessageCode.NOT_FOUND_NO_CATALOGUE_LINE.toString(),Arrays.asList(catalogueUuid, lineId));
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
                throw new NimbleException(NimbleExceptionMessageCode.NOT_FOUND_NO_PRICE_OPTION.toString(),Arrays.asList(catalogueUuid, lineId, optionId.toString()));
            }

            // remove the option and update the line
            EntityIdAwareRepositoryWrapper repositoryWrapper = new EntityIdAwareRepositoryWrapper(catalogueLine.getGoodsItem().getItem().getManufacturerParty().getPartyIdentification().get(0).getID());
            repositoryWrapper.deleteEntityByHjid(PriceOptionType.class, optionId);
//            catalogueLine.getPriceOption().remove(optionIndex.intValue());
//            repositoryWrapper.updateEntity(catalogueLine);

            log.info("Completed request to delete pricing option. catalogueId: {}, lineId: {}, optionId: {}", catalogueUuid, lineId, optionId);
            return ResponseEntity.ok().build();

        } catch (Exception e) {
            throw new NimbleException(NimbleExceptionMessageCode.INTERNAL_SERVER_ERROR_DELETE_PRICE_OPTION.toString(),Arrays.asList(catalogueUuid, lineId, optionId.toString()));
        }
    }

    @CrossOrigin(origins = {"*"})
    @ApiOperation(value = "", notes = "Adds the given pricing option to the specified catalogue line (i.e. product/service)")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Added the pricing option successfully", response = PriceOptionType.class),
            @ApiResponse(code = 400, message = "Invalid price option serialization"),
            @ApiResponse(code = 401, message = "Invalid token. No user was found for the provided token"),
            @ApiResponse(code = 404, message = "No catalogue or catalogue line found for the specified parameters"),
            @ApiResponse(code = 500, message = "Unexpected error while updating price option")
    })
    @RequestMapping(
            value = "/catalogue/{catalogueUuid}/catalogueline/{lineId}/price-options",
            consumes = {MediaType.APPLICATION_JSON_VALUE},
            produces = {MediaType.APPLICATION_JSON_VALUE},
            method = RequestMethod.PUT)
    public ResponseEntity updatePricingOption(@ApiParam(value = "uuid of the catalogue containing the line for which the price option to be deleted. (catalogue.uuid)", required = true) @PathVariable("catalogueUuid") String catalogueUuid,
                                              @ApiParam(value = "Identifier of the catalogue line to which the price option to be updated. (lineId.id)", required = true) @PathVariable("lineId") String lineId,
                                              @ApiParam(value = "Serialized form of PriceOptionType instance to be updated. An example price serialization can be found in: https://github.com/nimble-platform/catalog-service/tree/staging/catalogue-service-micro/src/main/resources/example_content/price_option.json", required = true) @RequestBody PriceOptionType priceOption,
                                              @ApiParam(value = "The Bearer token provided by the identity service", required = true) @RequestHeader(value = "Authorization") String bearerToken) {
        // set request log of ExecutionContext
        String requestLog = String.format("Incoming request to delete pricing option. catalogueId: %s, lineId: %s, optionId: %s", catalogueUuid, lineId, priceOption.getHjid());
        executionContext.setRequestLog(requestLog);

        log.info(requestLog);

        try {
            // validate role
            if(!validationUtil.validateRole(bearerToken, executionContext.getUserRoles(),RoleConfig.REQUIRED_ROLES_CATALOGUE_WRITE)) {
                throw new NimbleException(NimbleExceptionMessageCode.UNAUTHORIZED_INVALID_ROLE.toString());
            }

            // check catalogue
            if (service.getCatalogue(catalogueUuid) == null) {
                throw new NimbleException(NimbleExceptionMessageCode.NOT_FOUND_NO_CATALOGUE.toString(),Arrays.asList(catalogueUuid));
            }

            // check catalogue line
            CatalogueLineType catalogueLine = service.getCatalogueLine(catalogueUuid, lineId);
            if (catalogueLine == null) {
                throw new NimbleException(NimbleExceptionMessageCode.NOT_FOUND_NO_CATALOGUE_LINE.toString(),Arrays.asList(catalogueUuid, lineId));
            }

            // validate the entity ids
            boolean hjidsBelongToCompany = resourceValidationUtil.hjidsBelongsToParty(priceOption, catalogueLine.getGoodsItem().getItem().getManufacturerParty().getPartyIdentification().get(0).getID(), Configuration.Standard.UBL.toString());
            if(!hjidsBelongToCompany) {
                throw new NimbleException(NimbleExceptionMessageCode.BAD_REQUEST_INVALID_HJIDS.toString(),Arrays.asList(JsonSerializationUtility.serializeEntitySilently(priceOption)));
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
                throw new NimbleException(NimbleExceptionMessageCode.NOT_FOUND_NO_PRICE_OPTION.toString(),Arrays.asList(catalogueUuid, lineId, priceOption.getHjid().toString()));
            }

            // remove the option and update the line
            EntityIdAwareRepositoryWrapper repositoryWrapper = new EntityIdAwareRepositoryWrapper(catalogueLine.getGoodsItem().getItem().getManufacturerParty().getPartyIdentification().get(0).getID());
            priceOption = repositoryWrapper.updateEntity(priceOption);

            log.info("Completed request to update price option. catalogueId: {}, lineId: {}, optionId: {}", catalogueUuid, lineId, priceOption.getHjid());
            return ResponseEntity.ok().body(JsonSerializationUtility.getMapperForTransientFields().writeValueAsString(priceOption));

        } catch (Exception e) {
            throw new NimbleException(NimbleExceptionMessageCode.INTERNAL_SERVER_ERROR_UPDATE_PRICE_OPTION.toString(),Arrays.asList(catalogueUuid, lineId, priceOption.getHjid().toString()));
        }
    }

    @CrossOrigin(origins = {"*"})
    @ApiOperation(value = "", notes = "Gets standard VAT rates")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Retrieved VAT rates successfully"),
            @ApiResponse(code = 401, message = "Invalid token. No user was found for the provided token"),
            @ApiResponse(code = 500, message = "Unexpected error while updating price option")
    })
    @RequestMapping(
            value = "/catalogue/vat-rates",
            produces = {MediaType.APPLICATION_JSON_VALUE},
            method = RequestMethod.GET)
    public ResponseEntity getStandardVatRates(@ApiParam(value = "The Bearer token provided by the identity service", required = true) @RequestHeader(value = "Authorization") String bearerToken) {
        // set request log of ExecutionContext
        String requestLog = "Incoming request to get VAT rates";
        executionContext.setRequestLog(requestLog);

        log.info(requestLog);

        try {
            // validate role
            if(!validationUtil.validateRole(bearerToken,executionContext.getUserRoles(), RoleConfig.REQUIRED_ROLES_CATALOGUE_READ)) {
                throw new NimbleException(NimbleExceptionMessageCode.UNAUTHORIZED_INVALID_ROLE.toString());
            }

            HttpResponse<String> response = Unirest.get(VAT_RATES_URL)
                    .asString();

            log.info("Completed request to get VAT rates");
            return ResponseEntity.ok().body(response.getBody());

        } catch (Exception e) {
            throw new NimbleException(NimbleExceptionMessageCode.INTERNAL_SERVER_ERROR_GET_VAT_RATES.toString());
        }
    }
}
