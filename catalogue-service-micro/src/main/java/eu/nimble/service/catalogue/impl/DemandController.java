package eu.nimble.service.catalogue.impl;

import eu.nimble.common.rest.identity.IIdentityClientTyped;
import eu.nimble.common.rest.identity.model.PersonPartyTuple;
import eu.nimble.service.catalogue.DemandService;
import eu.nimble.service.catalogue.config.RoleConfig;
import eu.nimble.service.catalogue.exception.NimbleExceptionMessageCode;
import eu.nimble.service.catalogue.persistence.util.DemandPersistenceUtil;
import eu.nimble.service.model.ubl.commonaggregatecomponents.DemandType;
import eu.nimble.service.model.ubl.commonaggregatecomponents.MetadataType;
import eu.nimble.utility.ExecutionContext;
import eu.nimble.utility.JsonSerializationUtility;
import eu.nimble.utility.exception.NimbleException;
import eu.nimble.utility.persistence.JPARepositoryFactory;
import eu.nimble.utility.persistence.resource.MetadataUtility;
import eu.nimble.utility.validation.IValidationUtil;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.List;

@Controller
public class DemandController {

    private static Logger logger = LoggerFactory.getLogger(DemandController.class);
    @Autowired
    private ExecutionContext executionContext;
    @Autowired
    private IValidationUtil validationUtil;
    @Autowired
    private DemandService demandService;
    @Autowired
    private IIdentityClientTyped identityClient;

    @CrossOrigin(origins = {"*"})
    @ApiOperation(value = "", notes = "Creates a demand.")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Created the demand instance successfully", response = DemandType.class),
            @ApiResponse(code = 401, message = "Invalid token. No user was found for the provided token"),
            @ApiResponse(code = 500, message = "Unexpected error while creating the demand"),
    })
    @RequestMapping(value = "/demands",
            method = RequestMethod.POST,
            consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity createDemand(@ApiParam(value = "Serialized form of DemandType instance.", required = true) @RequestBody DemandType demand,
                                       @ApiParam(value = "The Bearer token provided by the identity service", required = true) @RequestHeader(value = "Authorization") String bearerToken) {
        try {
            // set request log of ExecutionContext
            String requestLog = String.format("Incoming request to create demand");
            executionContext.setRequestLog(requestLog);

            logger.info(requestLog);

            // validate role
            if (!validationUtil.validateRole(bearerToken, executionContext.getUserRoles(), RoleConfig.REQUIRED_ROLES_CATALOGUE_WRITE)) {
                throw new NimbleException(NimbleExceptionMessageCode.UNAUTHORIZED_INVALID_ROLE.toString());
            }

            // inject party ids into the execution context to be used in the metadata
            PersonPartyTuple personPartyTuple = identityClient.getPersonPartyTuple(bearerToken);
            executionContext.setCompanyId(personPartyTuple.getCompanyID());

            demandService.saveDemand(demand);

            logger.info("Completed request to create demand");
            return ResponseEntity.status(HttpStatus.CREATED).body(demand.getHjid());

        } catch (Exception e) {
            throw new NimbleException(NimbleExceptionMessageCode.INTERNAL_SERVER_ERROR_FAILED_CREATE_DEMAND.toString(), e);
        }
    }

    @CrossOrigin(origins = {"*"})
    @ApiOperation(value = "", notes = "Creates a demand.")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Created the demand instance successfully", response = DemandType.class),
            @ApiResponse(code = 401, message = "Invalid token. No user was found for the provided token"),
            @ApiResponse(code = 500, message = "Unexpected error while creating the demand"),
    })
    @RequestMapping(value = "/demands",
            method = RequestMethod.GET,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity getDemandsForCompany(@ApiParam(value = "Identifier of the company of which demands to be retrieved.", required = true) @RequestParam String companyId,
                                       @ApiParam(value = "The Bearer token provided by the identity service", required = true) @RequestHeader(value = "Authorization") String bearerToken) {
        try {
            // set request log of ExecutionContext
            String requestLog = String.format("Incoming request to get demands for party: %s", companyId);
            executionContext.setRequestLog(requestLog);

            logger.info(requestLog);

            // validate role
            if (!validationUtil.validateRole(bearerToken, executionContext.getUserRoles(), RoleConfig.REQUIRED_ROLES_CATALOGUE_WRITE)) {
                throw new NimbleException(NimbleExceptionMessageCode.UNAUTHORIZED_INVALID_ROLE.toString());
            }

            List<DemandType> demands = DemandPersistenceUtil.getDemandsForParty(companyId);

            logger.info("Completed request to get demands for party: {}", companyId);
            return ResponseEntity.status(HttpStatus.OK).body(JsonSerializationUtility.getObjectMapper(5).writeValueAsString(demands));

        } catch (Exception e) {
            throw new NimbleException(NimbleExceptionMessageCode.INTERNAL_SERVER_ERROR_FAILED_TO_GET_DEMANDS.toString(), Collections.singletonList(companyId), e);
        }
    }

    @CrossOrigin(origins = {"*"})
    @ApiOperation(value = "", notes = "Updates a demand.")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Updated the demand instance successfully", response = DemandType.class),
            @ApiResponse(code = 401, message = "Invalid token. No user was found for the provided token"),
            @ApiResponse(code = 500, message = "Unexpected error while updating the demand"),
    })
    @RequestMapping(value = "/demands/{demandHjid}",
            method = RequestMethod.PUT,
            consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity updateDemand(@ApiParam(value = "hjid of the demand to be replaced with the given demand", required = true) @PathVariable Long demandHjid,
                                       @ApiParam(value = "Serialized form of DemandType instance.", required = true) @RequestBody DemandType demand,
                                       @ApiParam(value = "The Bearer token provided by the identity service", required = true) @RequestHeader(value = "Authorization") String bearerToken) {
        try {
            // set request log of ExecutionContext
            String requestLog = String.format("Incoming request to update the demand with hjid: %d", demandHjid);
            executionContext.setRequestLog(requestLog);

            logger.info(requestLog);

            // validate role
            if (!validationUtil.validateRole(bearerToken, executionContext.getUserRoles(), RoleConfig.REQUIRED_ROLES_CATALOGUE_WRITE)) {
                throw new NimbleException(NimbleExceptionMessageCode.UNAUTHORIZED_INVALID_ROLE.toString());
            }

            // get the existing demand
            // (lazy loading is disabled below as the demand entity does not include any other collections to be fetched as a side effect)
            DemandType existingDemand = new JPARepositoryFactory().forCatalogueRepository(true).getSingleEntityByHjid(DemandType.class, demandHjid);
            if (existingDemand == null) {
                throw new NimbleException(NimbleExceptionMessageCode.NOT_FOUND_NO_DEMAND.toString(), Collections.singletonList(demandHjid.toString()));
            }

            PersonPartyTuple personPartyTuple = identityClient.getPersonPartyTuple(bearerToken);
            if (!MetadataUtility.isOwnerCompany(personPartyTuple.getCompanyID(), existingDemand.getMetadata())) {
                throw new NimbleException(NimbleExceptionMessageCode.UNAUTHORIZER_INVALID_AUTHORIZATION.toString());
            }
            demandService.updateDemand(existingDemand, demand);

            logger.info("Completed request to update demand with hjid: {}", demandHjid);
            return ResponseEntity.ok().build();

        } catch (Exception e) {
            throw new NimbleException(NimbleExceptionMessageCode.INTERNAL_SERVER_ERROR_FAILED_UPDATE_DEMAND.toString(), Collections.singletonList(demandHjid.toString()), e);
        }
    }

    @CrossOrigin(origins = {"*"})
    @ApiOperation(value = "", notes = "Deletes a demand.")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Deleted the demand instance successfully"),
            @ApiResponse(code = 401, message = "Invalid token. No user was found for the provided token"),
            @ApiResponse(code = 500, message = "Unexpected error while deleting the demand"),
    })
    @RequestMapping(value = "/demands/{demandHjid}",
            method = RequestMethod.DELETE)
    public ResponseEntity deleteDemand(@ApiParam(value = "hjid of the demand to be deleted", required = true) @PathVariable Long demandHjid,
                                       @ApiParam(value = "The Bearer token provided by the identity service", required = true) @RequestHeader(value = "Authorization") String bearerToken) {
        try {
            // set request log of ExecutionContext
            String requestLog = String.format("Incoming request to delete the demand with hjid: %d", demandHjid);
            executionContext.setRequestLog(requestLog);

            logger.info(requestLog);

            // validate role
            if (!validationUtil.validateRole(bearerToken, executionContext.getUserRoles(), RoleConfig.REQUIRED_ROLES_CATALOGUE_WRITE)) {
                throw new NimbleException(NimbleExceptionMessageCode.UNAUTHORIZED_INVALID_ROLE.toString());
            }

            // get the existing demand
            DemandType existingDemand = new JPARepositoryFactory().forCatalogueRepository(true).getSingleEntityByHjid(DemandType.class, demandHjid);
            if (existingDemand == null) {
                throw new NimbleException(NimbleExceptionMessageCode.NOT_FOUND_NO_DEMAND.toString(), Collections.singletonList(demandHjid.toString()));
            }

            PersonPartyTuple personPartyTuple = identityClient.getPersonPartyTuple(bearerToken);
            if (!MetadataUtility.isOwnerCompany(personPartyTuple.getCompanyID(), existingDemand.getMetadata())) {
                throw new NimbleException(NimbleExceptionMessageCode.UNAUTHORIZER_INVALID_AUTHORIZATION.toString());
            }

            demandService.deleteDemand(existingDemand);

            logger.info("Completed request to delete demand with hjid: {}", demandHjid);
            return ResponseEntity.ok().build();

        } catch (Exception e) {
            throw new NimbleException(NimbleExceptionMessageCode.INTERNAL_SERVER_ERROR_FAILED_DELETE_DEMAND.toString(), Collections.singletonList(demandHjid.toString()), e);
        }
    }


}