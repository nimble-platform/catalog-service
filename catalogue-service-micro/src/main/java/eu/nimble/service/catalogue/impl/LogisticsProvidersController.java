package eu.nimble.service.catalogue.impl;

import eu.nimble.service.catalogue.config.RoleConfig;
import eu.nimble.service.catalogue.persistence.util.LogisticsProviderPersistenceUtil;
import eu.nimble.service.model.ubl.commonaggregatecomponents.LogisticsProviderType;
import eu.nimble.utility.ExecutionContext;
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
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;

import java.util.List;

/**
 * HCDP-05-04: Logistics Provider directory — read-only endpoints.
 *
 * <p>Returns the list of carriers the platform supports. Consumed by the
 * Replace Provider modal in the buyer's Fulfilment view. Population happens
 * out-of-band via {@code seed-demo.sh} STEP 16; this controller is read-only.</p>
 */
@Controller
public class LogisticsProvidersController {

    private static final Logger logger = LoggerFactory.getLogger(LogisticsProvidersController.class);

    @Autowired
    private ExecutionContext executionContext;

    @Autowired
    private IValidationUtil validationUtil;

    @CrossOrigin(origins = {"*"})
    @ApiOperation(value = "", notes = "Returns the list of active logistics providers (carriers) the platform supports. " +
            "Used to populate the Replace Provider modal's alternatives dropdown.")
    @ApiResponses({
        @ApiResponse(code = 200, message = "Logistics providers retrieved successfully"),
        @ApiResponse(code = 401, message = "Unauthorized request")
    })
    @GetMapping(value = "/logistics-providers", produces = "application/json")
    public ResponseEntity<List<LogisticsProviderType>> getActiveProviders(
            @ApiParam(value = "Bearer token") @RequestHeader(value = "Authorization") String bearerToken) {

        if (!validationUtil.validateRole(bearerToken, executionContext.getUserRoles(),
                                         RoleConfig.REQUIRED_ROLES_CATALOGUE_READ)) {
            return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
        }
        List<LogisticsProviderType> providers = LogisticsProviderPersistenceUtil.getActiveProviders();
        logger.debug("[LOGISTICS-PROVIDERS] Returning {} active providers", providers.size());
        return ResponseEntity.ok(providers);
    }

    @CrossOrigin(origins = {"*"})
    @ApiOperation(value = "", notes = "Returns a single logistics provider by its stable slug.")
    @ApiResponses({
        @ApiResponse(code = 200, message = "Provider found"),
        @ApiResponse(code = 404, message = "Provider not found"),
        @ApiResponse(code = 401, message = "Unauthorized request")
    })
    @GetMapping(value = "/logistics-providers/{slug}", produces = "application/json")
    public ResponseEntity<LogisticsProviderType> getProviderBySlug(
            @ApiParam(value = "Stable provider slug, e.g. 'dhl-freight'") @PathVariable("slug") String slug,
            @ApiParam(value = "Bearer token") @RequestHeader(value = "Authorization") String bearerToken) {

        if (!validationUtil.validateRole(bearerToken, executionContext.getUserRoles(),
                                         RoleConfig.REQUIRED_ROLES_CATALOGUE_READ)) {
            return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
        }
        LogisticsProviderType provider = LogisticsProviderPersistenceUtil.getBySlug(slug);
        if (provider == null) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
        return ResponseEntity.ok(provider);
    }
}
