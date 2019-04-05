package eu.nimble.service.catalogue.impl;

import eu.nimble.service.catalogue.CatalogueService;
import eu.nimble.service.catalogue.model.lcpa.ItemLCPAInput;
import eu.nimble.service.catalogue.persistence.util.CatalogueLinePersistenceUtil;
import eu.nimble.service.catalogue.persistence.util.CataloguePersistenceUtil;
import eu.nimble.service.model.ubl.commonaggregatecomponents.PriceOptionType;
import eu.nimble.utility.persistence.resource.ResourceValidationUtility;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Created by suat on 27-Mar-19.
 */
@Controller
@RequestMapping(value = "/lcpa")
public class LCPAController {
    private static Logger log = LoggerFactory.getLogger(PriceConfigurationController.class);

    @CrossOrigin(origins = {"*"})
    @ApiOperation(value = "", notes = "Returns the catalogue uuid/catalogue line information along with the corresponding " +
            "LCPA Details")
    @ApiResponses(value = {
            @ApiResponse(code = 201, message = "Returned products with valid LCPA input", response = ItemLCPAInput.class, responseContainer = "List"),
            @ApiResponse(code = 401, message = "Invalid token. "),
            @ApiResponse(code = 500, message = "Unexpected error")
    })
    @RequestMapping(value = "/products-with-lcpa-input",
            produces = {MediaType.APPLICATION_JSON_VALUE},
            method = RequestMethod.GET)
    public ResponseEntity getProductsWithoutLCPAProcessing(@ApiParam(value = "The Bearer token provided by the identity service", required = true) @RequestHeader(value = "Authorization") String bearerToken) {
        log.info("Incoming request to add pricing option. catalogueId: {}, lineId: {}");
        List<ItemLCPAInput> results = CatalogueLinePersistenceUtil.getLinesIdsWithValidLcpaInput();
        return ResponseEntity.ok(results);
    }
}
