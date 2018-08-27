package eu.nimble.service.catalogue.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import eu.nimble.service.catalogue.CatalogueService;
import eu.nimble.service.catalogue.CatalogueServiceImpl;
import eu.nimble.service.catalogue.sync.MarmottaSynchronizer;
import eu.nimble.service.catalogue.util.HttpResponseUtil;
import eu.nimble.service.catalogue.util.HyperJaxbSerializationUtil;
import eu.nimble.service.model.ubl.commonaggregatecomponents.*;
import eu.nimble.service.model.ubl.commonbasiccomponents.AmountType;
import eu.nimble.service.model.ubl.commonbasiccomponents.CodeType;
import eu.nimble.service.model.ubl.commonbasiccomponents.QuantityType;
import eu.nimble.utility.Configuration;
import eu.nimble.utility.HibernateUtility;
import eu.nimble.utility.JsonSerializationUtility;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import org.jvnet.hyperjaxb3.item.Item;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.logging.LogLevel;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.URI;
import java.util.ArrayList;
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

    private CatalogueService service = CatalogueServiceImpl.getInstance();

    public static void main(String[] args) throws JsonProcessingException {
        PriceOptionType option = new PriceOptionType();

        // item props
        List<ItemPropertyType> props = new ArrayList<>();
        ItemPropertyType prop = new ItemPropertyType();
        prop.setName("prop1");
        List<String> propVals = new ArrayList<>();
        propVals.add("p1V1");
        propVals.add("p1V2");
        prop.setValue(propVals);
        props.add(prop);

        prop = new ItemPropertyType();
        prop.setName("prop2");
        propVals = new ArrayList<>();
        propVals.add("p2V1");
        propVals.add("p2V2");
        prop.setValue(propVals);
        props.add(prop);
        option.setAdditionalItemProperty(props);

        // delivery period
        PeriodType period = new PeriodType();
        QuantityType duration = new QuantityType();
        duration.setValue(BigDecimal.valueOf(2));
        duration.setUnitCode("weeks");
        period.setDurationMeasure(duration);
        option.setEstimatedDeliveryPeriod(period);

        // incoterms
        List<String> incoTerms = new ArrayList<>();
        incoTerms.add("CIF");
        incoTerms.add("CIP");
        option.setIncoterms(incoTerms);

        List<PaymentMeansType> paymentMeans = new ArrayList<>();
        PaymentMeansType mean = new PaymentMeansType();
        CodeType meanCode = new CodeType();
        meanCode.setName("Credit Card");
        mean.setPaymentMeansCode(meanCode);
        paymentMeans.add(mean);

        mean = new PaymentMeansType();
        meanCode = new CodeType();
        meanCode.setName("ACH Transfer");
        mean.setPaymentMeansCode(meanCode);
        paymentMeans.add(mean);

        option.setPaymentMeans(paymentMeans);

        PaymentTermsType paymentTerms = new PaymentTermsType();
        List<TradingTermType> tradingTerms = new ArrayList<>();
        TradingTermType term = new TradingTermType();
        term.setDescription("Payment in advance");
        tradingTerms.add(term);
        term = new TradingTermType();
        term.setDescription("10% discount if the payment is done ...");
        term.setTradingTermFormat("%s, %s, %s");
        List<String> termValues = new ArrayList<>();
        termValues.add("10");
        termValues.add("1");
        termValues.add("1");
        term.setValue(termValues);
        tradingTerms.add(term);

        paymentTerms.setTradingTerms(tradingTerms);
        option.setPaymentTerms(paymentTerms);
        option.setSpecialTerms("Some special terms");

        ItemLocationQuantityType ilqt = new ItemLocationQuantityType();
        List<AddressType> addresses = new ArrayList<>();
        AddressType addr = new AddressType();
        CountryType country = new CountryType();
        country.setName("Turkey");
        addr.setCountry(country);
        addresses.add(addr);

        addr = new AddressType();
        country = new CountryType();
        country.setName("Bulgaria");
        addr.setCountry(country);
        addresses.add(addr);

        ilqt.setApplicableTerritoryAddress(addresses);

        PriceType price = new PriceType();
        QuantityType base = new QuantityType();
        base.setUnitCode("m2");
        base.setValue(BigDecimal.valueOf(1));
        price.setBaseQuantity(base);
        AmountType priceAmount = new AmountType();
        priceAmount.setValue(BigDecimal.valueOf(10));
        priceAmount.setCurrencyID("EUR");
        price.setPriceAmount(priceAmount);
        ilqt.setPrice(price);

        QuantityType minQuantity = new QuantityType();
        minQuantity.setUnitCode("m2");
        minQuantity.setValue(BigDecimal.valueOf(10));
        ilqt.setMinimumQuantity(minQuantity);

        List<AllowanceChargeType> discounts = new ArrayList<>();
        AllowanceChargeType discount = new AllowanceChargeType();
        discount.setPercent(BigDecimal.valueOf(10));
        discounts.add(discount);
        ilqt.setAllowanceCharge(discounts);
        option.setItemLocationQuantity(ilqt);

        ObjectMapper objectMapper = new ObjectMapper();
        System.out.println(objectMapper.writeValueAsString(option));
    }

    @CrossOrigin(origins = {"*"})
    @ApiOperation(value = "", notes = "Adds the given pricing option to the specified catalogue line (i.e. product/service)")
    @ApiResponses(value = {
            @ApiResponse(code = 201, message = "Added the pricing option successfully", response = PriceOptionType.class),
            @ApiResponse(code = 404, message = "No catalogue or catalogue line found for the specified parameters")
    })
    @RequestMapping(consumes = {MediaType.APPLICATION_JSON_VALUE},
            produces = {MediaType.APPLICATION_JSON_VALUE},
            method = RequestMethod.POST)
    public ResponseEntity addPricingOption(@PathVariable("catalogueUuid") String catalogueUuid,
                                           @PathVariable("lineId") String lineId,
                                           @RequestBody PriceOptionType priceOption,
                                           @RequestHeader(value = "Authorization") String bearerToken) {
        log.info("Incoming request to add pricing option. catalogueId: {}, lineId: {}", catalogueUuid, lineId);
        try {
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

            // first persist the price options
            HibernateUtility.getInstance(Configuration.UBL_PERSISTENCE_UNIT_NAME).persist(priceOption);

            // update the catalogue line
            catalogueLine.getPriceOption().add(priceOption);
            HibernateUtility.getInstance(Configuration.UBL_PERSISTENCE_UNIT_NAME).update(catalogueLine);

            // update the index
            MarmottaSynchronizer.getInstance().addRecord(MarmottaSynchronizer.SyncStatus.UPDATE, catalogueUuid);

            ObjectMapper objectMapper = new ObjectMapper();
            objectMapper.configure(MapperFeature.PROPAGATE_TRANSIENT_MARKER, false);

            log.info("Completed request to add pricing option. catalogueId: {}, lineId: {}", catalogueUuid, lineId);
            return ResponseEntity.created(new URI(priceOption.getHjid().toString())).body(priceOption);

        } catch (Exception e) {
            String serializedOption;
            try {
                serializedOption = new ObjectMapper().writeValueAsString(priceOption);
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
            @ApiResponse(code = 201, message = "Added the pricing option successfully", response = PriceOptionType.class),
            @ApiResponse(code = 404, message = "No catalogue or catalogue line found for the specified parameters")
    })
    @RequestMapping(value = "/{optionId}",
            method = RequestMethod.DELETE)
    public ResponseEntity deletePricingOption(@PathVariable("catalogueUuid") String catalogueUuid,
                                              @PathVariable("lineId") String lineId,
                                              @PathVariable("optionId") Long optionId,
                                              @RequestHeader(value = "Authorization") String bearerToken) {
        log.info("Incoming request to delete pricing option. catalogueId: {}, lineId: {}, optionId: {}", catalogueUuid, lineId, optionId);
        try {
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
            HibernateUtility.getInstance(Configuration.UBL_PERSISTENCE_UNIT_NAME).delete(PriceOptionType.class, optionId);

            // update the index
            MarmottaSynchronizer.getInstance().addRecord(MarmottaSynchronizer.SyncStatus.UPDATE, catalogueUuid);

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
            @ApiResponse(code = 404, message = "No catalogue or catalogue line found for the specified parameters")
    })
    @RequestMapping(consumes = {MediaType.APPLICATION_JSON_VALUE},
            produces = {MediaType.APPLICATION_JSON_VALUE},
            method = RequestMethod.PUT)
    public ResponseEntity updatePricingOption(@PathVariable("catalogueUuid") String catalogueUuid,
                                              @PathVariable("lineId") String lineId,
                                              @RequestBody String priceOptionJson,
                                              @RequestHeader(value = "Authorization") String bearerToken) {
        log.info("Incoming request to delete pricing option. catalogueId: {}, lineId: {}, option: {}", catalogueUuid, lineId, priceOptionJson);
        PriceOptionType priceOption = null;
        try {
            priceOption = HyperJaxbSerializationUtil.checkBuiltInLists(priceOptionJson, PriceOptionType.class);
        } catch (IOException e) {
            e.printStackTrace();
        }

        try {
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
            priceOption = (PriceOptionType) HibernateUtility.getInstance(Configuration.UBL_PERSISTENCE_UNIT_NAME).update(priceOption);

            // update the index
            MarmottaSynchronizer.getInstance().addRecord(MarmottaSynchronizer.SyncStatus.UPDATE, catalogueUuid);

            log.info("Completed request to update price option. catalogueId: {}, lineId: {}, optionId: {}", catalogueUuid, lineId, priceOption.getHjid());
            return ResponseEntity.ok().body(JsonSerializationUtility.getMapperForTransientFields().writeValueAsString(priceOption));

        } catch (Exception e) {
            String msg = String.format("Unexpected error while deleting price option catalogueId: %s, lineId: %s, optionId: %d", catalogueUuid, lineId, priceOption.getHjid());
            return HttpResponseUtil.createResponseEntityAndLog(msg, e, HttpStatus.INTERNAL_SERVER_ERROR, LogLevel.ERROR);
        }
    }
}
