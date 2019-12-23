package eu.nimble.service.catalogue.util.migration.r15;

import eu.nimble.service.catalogue.UnitManager;
import eu.nimble.service.catalogue.config.RoleConfig;
import eu.nimble.utility.HttpResponseUtil;
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
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import springfox.documentation.annotations.ApiIgnore;

import java.util.Arrays;

@ApiIgnore
@Controller
public class R15MigrationController {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Autowired
    private UnitManager unitManager;
    @Autowired
    private IValidationUtil validationUtil;

    @ApiOperation(value = "", notes = "Add LCPA input units to Unit Manager")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Added LCPA input units successfully"),
            @ApiResponse(code = 401, message = "Invalid token. No user was found for the provided token")
    })
    @RequestMapping(value = "/r15/migration/lcpa-units",
            produces = {"application/json"},
            method = RequestMethod.PATCH)
    public ResponseEntity addLCPAInputUnits(@ApiParam(value = "The Bearer token provided by the identity service", required = true) @RequestHeader(value = "Authorization", required = true) String bearerToken
    ) {
        logger.info("Incoming request to add LCPA input units");

        // validate role
        if(!validationUtil.validateRole(bearerToken, RoleConfig.REQUIRED_ROLES_CATALOGUE)) {
            return HttpResponseUtil.createResponseEntityAndLog("Invalid role", HttpStatus.UNAUTHORIZED);
        }

        unitManager.addUnitList("lcpa_input_currency_quantity", Arrays.asList("EUR","USD","SEK","EUR/Year","USD/Year","SEK/Year","EUR/Month","USD/Month","SEK/Month"));

        logger.info("Completed request to add LCPA input units");
        return ResponseEntity.ok(null);
    }
}
