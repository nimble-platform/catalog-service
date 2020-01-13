package eu.nimble.service.catalogue.util.migration.r15;

import eu.nimble.service.catalogue.UnitManager;
import eu.nimble.service.catalogue.config.RoleConfig;
import eu.nimble.service.catalogue.util.SpringBridge;
import eu.nimble.service.model.ubl.commonaggregatecomponents.PartyType;
import eu.nimble.utility.HttpResponseUtil;
import eu.nimble.utility.persistence.GenericJPARepository;
import eu.nimble.utility.persistence.JPARepositoryFactory;
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
import java.util.List;

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

    @ApiOperation(value = "", notes = "Sets the federation ids of parties")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Set federation ids of parties successfully"),
            @ApiResponse(code = 401, message = "Invalid token. No user was found for the provided token"),
            @ApiResponse(code = 500, message = "Unexpected error while setting federation ids of parties")
    })
    @RequestMapping(value = "/r15/migration/federate-parties",
            produces = {"application/json"},
            method = RequestMethod.PATCH)
    public ResponseEntity federateParties(@ApiParam(value = "The Bearer token provided by the identity service", required = true) @RequestHeader(value = "Authorization", required = true) String bearerToken
    ) {
        logger.info("Incoming request to federate parties");

        // validate role
        if(!validationUtil.validateRole(bearerToken, RoleConfig.REQUIRED_ROLES_CATALOGUE)) {
            return HttpResponseUtil.createResponseEntityAndLog("Invalid role", HttpStatus.UNAUTHORIZED);
        }

        // federation id
        String federationId = SpringBridge.getInstance().getFederationId();
        if(federationId == null){
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("This instance does not have a federation id");
        }

        GenericJPARepository catalogueRepo = new JPARepositoryFactory().forCatalogueRepositoryMultiTransaction(true);
        try{
            // set federation ids of Parties
            List<PartyType> parties = catalogueRepo.getEntities(PartyType.class);
            logger.info("There are {} parties",parties.size());
            for (PartyType party : parties) {
                party.setFederationInstanceID(federationId);
                catalogueRepo.updateEntity(party);
            }

            catalogueRepo.commit();
        }
        catch (Exception e){
            catalogueRepo.rollback();
            String msg = "Unexpected error while federating parties";
            logger.error(msg,e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(msg);
        }

        logger.info("Completed request to federate parties");
        return ResponseEntity.ok(null);
    }
}
