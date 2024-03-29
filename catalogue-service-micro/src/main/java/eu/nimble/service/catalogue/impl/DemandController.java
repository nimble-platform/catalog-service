package eu.nimble.service.catalogue.impl;

import eu.nimble.common.rest.identity.IIdentityClientTyped;
import eu.nimble.common.rest.identity.model.PersonPartyTuple;
import eu.nimble.service.catalogue.DemandIndexService;
import eu.nimble.service.catalogue.DemandService;
import eu.nimble.service.catalogue.config.RoleConfig;
import eu.nimble.service.catalogue.exception.NimbleExceptionMessageCode;
import eu.nimble.service.catalogue.model.demand.DemandFacetResponse;
import eu.nimble.service.catalogue.model.demand.DemandLastSeenResponse;
import eu.nimble.service.catalogue.model.demand.DemandPaginationResponse;
import eu.nimble.service.catalogue.persistence.util.DemandPersistenceUtil;
import eu.nimble.service.catalogue.util.email.EmailSenderUtil;
import eu.nimble.service.model.ubl.commonaggregatecomponents.*;
import eu.nimble.utility.ExecutionContext;
import eu.nimble.utility.JsonSerializationUtility;
import eu.nimble.utility.exception.NimbleException;
import eu.nimble.utility.persistence.JPARepositoryFactory;
import eu.nimble.utility.persistence.repository.MetadataUtility;
import eu.nimble.utility.validation.IValidationUtil;
import eu.nimble.utility.validation.NimbleRole;
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
import org.thymeleaf.context.Context;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
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
    @Autowired
    private DemandIndexService demandIndexService;
    @Autowired
    private EmailSenderUtil emailSenderUtil;

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

//            JAXBContext jc = JAXBContext.newInstance(DemandType.class);
//            SchemaFactory sf = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
//            Schema schema = sf.newSchema(new File("customer.xsd"));
//
//
//            Marshaller marshaller = jc.createMarshaller();
//            marshaller.setSchema(schema);
//            marshaller.marshal(demand, new DefaultHandler());

            inviteCompaniesToDemandDetails(demand,bearerToken,executionContext.getLanguageId());
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
    @RequestMapping(value = "/demands/{demandHjid}",
            method = RequestMethod.GET,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity getDemand(@ApiParam(value = "Serialized form of DemandType instance.", required = true) @PathVariable Long demandHjid,
                                    @ApiParam(value = "The Bearer token provided by the identity service", required = true) @RequestHeader(value = "Authorization") String bearerToken) {
        try {
            // set request log of ExecutionContext
            String requestLog = String.format("Incoming request to get demand with hjid: %d", demandHjid);
            executionContext.setRequestLog(requestLog);

            logger.info(requestLog);

            DemandType demand = demandService.getDemand(demandHjid);
            if (demand == null) {
                throw new NimbleException(NimbleExceptionMessageCode.NOT_FOUND_NO_DEMAND.toString(), Collections.singletonList(demandHjid.toString()));
            }

            logger.info("Completed request to get demand with hjid: {}", demandHjid);
            return ResponseEntity.status(HttpStatus.OK).body(JsonSerializationUtility.getObjectMapper(1).writeValueAsString(demand));

        } catch (Exception e) {
            throw new NimbleException(NimbleExceptionMessageCode.INTERNAL_SERVER_ERROR_FAILED_CREATE_DEMAND.toString(), e);
        }
    }

    @CrossOrigin(origins = {"*"})
    @ApiOperation(value = "", notes = "Queries demands. It works in two modes. Demands are either retrieved for a specific company or they are queried " +
            "with a query term")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Created the demand instance successfully", response = DemandType.class),
            @ApiResponse(code = 401, message = "Invalid token. No user was found for the provided token"),
            @ApiResponse(code = 500, message = "Unexpected error while creating the demand"),
    })
    @RequestMapping(value = "/demands",
            method = RequestMethod.GET,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity getDemands(@ApiParam(value = "Identifier of the company of which demands to be retrieved.") @RequestParam(required = false) String companyId,
                                     @ApiParam(value = "Search query term.") @RequestParam(required = false) String query,
                                     @ApiParam(value = "Query language") @RequestParam(required = false) String lang,
                                     @ApiParam(value = "Demand category") @RequestParam(required = false) String categoryUri,
                                     @ApiParam(value = "Latest due date. Demands of which due dates are equal or earlier than the provided date are retrieved") @RequestParam(required = false) String dueDate,
                                     @ApiParam(value = "Buyer country") @RequestParam(required = false) String buyerCountry,
                                     @ApiParam(value = "Delivery country") @RequestParam(required = false) String deliveryCountry,
                                     @ApiParam(value = "Circular economy certificates") @RequestParam(required = false) List<String> circularEconomyCertificates,
                                     @ApiParam(value = "Certificates other than circular economy related ones") @RequestParam(required = false) List<String> otherCertificates,
                                     @ApiParam(value = "Page no, which used as the offset to retrieve demands. It's also used to calculate the offset for the results", defaultValue = "0") @RequestParam(defaultValue = "0", required = false) Integer pageNo,
                                     @ApiParam(value = "Number of demands to be retrieved", defaultValue = "10") @RequestParam(defaultValue = "10", required = false) Integer limit) {
        try {
            // set request log of ExecutionContext
            String requestLog = String.format("Incoming request to get demands for party: %s, query term: %s, lang: %s, category: %s, due date: %s, buyer country: %s, delivery country: %s, page no: %d, limit: %d", companyId, query, lang, categoryUri, dueDate, buyerCountry, deliveryCountry, pageNo, limit);
            executionContext.setRequestLog(requestLog);

            logger.info(requestLog);

            List<DemandType> demands;
            int demandCount;
            DemandPaginationResponse response;
            // normalize the query term
            query = normalizeQueryTerm(query);
            demandCount = demandIndexService.getDemandCount(query, lang, companyId, categoryUri, dueDate, buyerCountry, deliveryCountry,circularEconomyCertificates,otherCertificates);
            demands = demandIndexService.searchDemand(query, lang, companyId, categoryUri, dueDate, buyerCountry, deliveryCountry, pageNo, limit,circularEconomyCertificates,otherCertificates);
            response = new DemandPaginationResponse(demandCount, demands);
            // TODO enable this and do not include image content in the actual results
//            ObjectMapper mapper = JsonSerializationUtility.getObjectMapper(1);

            logger.info("Completed request to get demands for party: {}, query term: {}, lang: {}, category: {}, due date: {}, buyer country: {}, delivery country: {}, page no: {}, limit: {}", companyId, query, lang, categoryUri, dueDate, buyerCountry, deliveryCountry, pageNo, limit);

            return ResponseEntity.status(HttpStatus.OK).body(JsonSerializationUtility.getObjectMapper().writeValueAsString(response));
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
            // check the metadata modification date to prevent an outdated object to be used in the update operation
            if (demand.getMetadata() == null || demand.getMetadata().getModificationDate() == null) {
                throw new NimbleException(NimbleExceptionMessageCode.BAD_REQUEST_NO_METADATA_OR_MODIFICATION_DATE.toString(),
                        Arrays.asList(DemandType.class.getName(), demandHjid.toString()));
            }
            if (existingDemand.getMetadata().getModificationDate().toGregorianCalendar().getTimeInMillis() !=
                    demand.getMetadata().getModificationDate().toGregorianCalendar().getTimeInMillis()) {
                throw new NimbleException(NimbleExceptionMessageCode.BAD_REQUEST_INVALID_MODIFICATION_DATE.toString(),
                        Arrays.asList(demand.getMetadata().getModificationDate().getMillisecond() + "", DemandType.class.getName(), demandHjid.toString()));
            }

            PersonPartyTuple personPartyTuple = identityClient.getPersonPartyTuple(bearerToken);
            if (!MetadataUtility.isOwnerCompany(personPartyTuple.getCompanyID(), existingDemand.getMetadata())) {
                throw new NimbleException(NimbleExceptionMessageCode.UNAUTHORIZER_INVALID_AUTHORIZATION.toString());
            }
            demandService.updateDemand(existingDemand, demand);

            logger.info("Completed request to update demand with hjid: {}", demandHjid);
            return ResponseEntity.ok().build();

        } catch (NimbleException e) {
            throw e;
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

    @CrossOrigin(origins = {"*"})
    @ApiOperation(value = "", notes = "Gets demand facets.")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Retrieved demand facets successfully", response = DemandType.class),
            @ApiResponse(code = 500, message = "Unexpected error while getting demand facets"),
    })
    @RequestMapping(value = "/demand-facets",
            method = RequestMethod.GET,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity getFacets(@ApiParam(value = "Identifier of the company of which demands to be retrieved.") @RequestParam(required = false) String companyId,
                                    @ApiParam(value = "Search query term.") @RequestParam(required = false) String query,
                                    @ApiParam(value = "Query language") @RequestParam(required = false) String lang,
                                    @ApiParam(value = "Demand category") @RequestParam(required = false) String categoryUri,
                                    @ApiParam(value = "Latest due date. Demands of which due dates are equal or earlier than the provided date are retrieved") @RequestParam(required = false) String dueDate,
                                    @ApiParam(value = "Buyer country") @RequestParam(required = false) String buyerCountry,
                                    @ApiParam(value = "Delivery country") @RequestParam(required = false) String deliveryCountry,
                                    @ApiParam(value = "Circular economy certificates") @RequestParam(required = false) List<String> circularEconomyCertificates,
                                    @ApiParam(value = "Certificates other than circular economy related ones") @RequestParam(required = false) List<String> otherCertificates) {
        try {
            // set request log of ExecutionContext
            String requestLog = String.format("Incoming request to get demand facets for party: %s, query term: %s, lang: %s, category: %s, due date: %s, buyer country: %s, delivery country: %s", companyId, query, lang, categoryUri, dueDate, buyerCountry, deliveryCountry);
            executionContext.setRequestLog(requestLog);

            logger.info(requestLog);

            // normalize the query term
            query = normalizeQueryTerm(query);
            List<DemandFacetResponse> facets = demandIndexService.getDemandFacets(query, lang, companyId, categoryUri, dueDate, buyerCountry, deliveryCountry,circularEconomyCertificates,otherCertificates);

            logger.info("Completed request to get demand facets for party: {}, query term: {}, lang: {}, category: {}, due date: {}, buyer country: {}, delivery country: {}", companyId, query, lang, categoryUri, dueDate, buyerCountry, deliveryCountry);
            return ResponseEntity.status(HttpStatus.OK).body(facets);

        } catch (Exception e) {
            throw new NimbleException(NimbleExceptionMessageCode.INTERNAL_SERVER_ERROR_FAILED_TO_GET_DEMAND_FACETS.toString(), e);
        }
    }

    @CrossOrigin(origins = {"*"})
    @ApiOperation(value = "", notes = "Submits a demand interest count.")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Saved the interest activity successfully", response = DemandType.class),
            @ApiResponse(code = 500, message = "Unexpected error while saving the demand interest activity"),
    })
    @RequestMapping(value = "/demands/{demandHjid}/visit",
            method = RequestMethod.POST)
    public ResponseEntity addDemandVisitCount(@ApiParam(value = "Demand hjid.", required = true) @PathVariable(required = true) Long demandHjid,
                                              @ApiParam(value = "Identifier of the visitor company.", required = true) @RequestParam(required = true) String visitorCompanyId,
                                              @ApiParam(value = "The Bearer token provided by the identity service", required = true) @RequestHeader(value = "Authorization") String bearerToken) {
        try {
            // set request log of ExecutionContext
            String requestLog = String.format("Incoming request to save interest activity for demand: %d by %s", demandHjid, visitorCompanyId);
            executionContext.setRequestLog(requestLog);

            logger.info(requestLog);

            DemandPersistenceUtil.saveDemandInterestActivity(demandHjid, visitorCompanyId);

            logger.info("to save interest activity for demand: {} by {}", demandHjid, visitorCompanyId);
            return ResponseEntity.status(HttpStatus.OK).build();

        } catch (Exception e) {
            throw new NimbleException(NimbleExceptionMessageCode.INTERNAL_SERVER_ERROR_FAILED_TO_SAVE_DEMAND_INTEREST_ACTIVITY.toString(), Arrays.asList(demandHjid.toString(), visitorCompanyId), e);
        }
    }

    @CrossOrigin(origins = {"*"})
    @ApiOperation(value = "", notes = "Gets .")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Retrieved interest counts successfully", response = DemandType.class),
            @ApiResponse(code = 500, message = "Unexpected error while saving the demand interest activity"),
    })
    @RequestMapping(value = "/demands/interest-counts",
            method = RequestMethod.GET)
    public ResponseEntity getDemandInterestCounts(@ApiParam(value = "The Bearer token provided by the identity service", required = true) @RequestHeader(value = "Authorization") String bearerToken) {
        try {
            // set request log of ExecutionContext
            String requestLog = String.format("Incoming request to get interest counts");
            executionContext.setRequestLog(requestLog);

            logger.info(requestLog);

            List<DemandInterestCount> interestCounts = DemandPersistenceUtil.getInterestCounts();

            logger.info("Incoming request to get interest counts");
            return ResponseEntity.status(HttpStatus.OK).body(interestCounts);

        } catch (Exception e) {
            throw new NimbleException(NimbleExceptionMessageCode.INTERNAL_SERVER_ERROR_FAILED_TO_GET_INTEREST_COUNTS.toString(), e);
        }
    }

    @CrossOrigin(origins = {"*"})
    @ApiOperation(value = "", notes = "Saves the last seen demand id for the user.")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Saved the last seen demand id successfully", response = DemandType.class),
            @ApiResponse(code = 500, message = "Unexpected error while saving the last seen demand id"),
    })
    @RequestMapping(value = "/demands/last-seen",
            method = RequestMethod.POST)
    public ResponseEntity addLastSeenDemand(@ApiParam(value = "Demand id", required = true) @RequestBody Long lastSeenDemandId,
                                            @ApiParam(value = "The Bearer token provided by the identity service", required = true) @RequestHeader(value = "Authorization") String bearerToken) {
        try {
            // set request log of ExecutionContext
            String requestLog = String.format("Incoming request to save last seen demand id for %s",lastSeenDemandId);
            executionContext.setRequestLog(requestLog);

            logger.info(requestLog);

            PersonPartyTuple personPartyTuple = identityClient.getPersonPartyTuple(bearerToken);
            // get last seen demand info
            DemandLastSeenInfo demandLastSeenInfo = DemandPersistenceUtil.getLastSeenDemandId(personPartyTuple.getPersonID());
            // save the last seen demand id
            if (demandLastSeenInfo == null) {
                demandLastSeenInfo = new DemandLastSeenInfo();
                demandLastSeenInfo.setLastSeenDemandID(lastSeenDemandId);
                demandLastSeenInfo.setPersonID(personPartyTuple.getPersonID());
            } else {
                if (demandLastSeenInfo.getLastSeenDemandID() < lastSeenDemandId) {
                    demandLastSeenInfo.setLastSeenDemandID(lastSeenDemandId);
                }
            }

            new JPARepositoryFactory().forCatalogueRepository().updateEntity(demandLastSeenInfo);

            logger.info("Completed request to save last seen demand id for {}",lastSeenDemandId);
            return ResponseEntity.status(HttpStatus.OK).build();

        } catch (Exception e) {
            throw new NimbleException(NimbleExceptionMessageCode.INTERNAL_SERVER_ERROR_FAILED_TO_ADD_LAST_SEEN_DEMAND.toString(),Arrays.asList(String.valueOf(lastSeenDemandId)),e);
        }
    }

    @CrossOrigin(origins = {"*"})
    @ApiOperation(value = "", notes = "Gets demand last seen response for the user.")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Retrieved demand last seen response successfully", response = DemandType.class),
            @ApiResponse(code = 500, message = "Unexpected error while retrieving the demand last seen response for the user"),
    })
    @RequestMapping(value = "/demands/last-seen/response",
            method = RequestMethod.GET)
    public ResponseEntity getDemandLastSeenResponse(@ApiParam(value = "Latest due date. Demands of which due dates are equal or earlier than the provided date are considered") @RequestParam(required = false) String dueDate,
                                                    @ApiParam(value = "The Bearer token provided by the identity service", required = true) @RequestHeader(value = "Authorization") String bearerToken) {
        try {
            // set request log of ExecutionContext
            String requestLog = String.format("Incoming request to retrieve demand last seen response for the user");
            executionContext.setRequestLog(requestLog);

            logger.info(requestLog);

            PersonPartyTuple personPartyTuple = identityClient.getPersonPartyTuple(bearerToken);
            // get demand last seen info
            DemandLastSeenInfo demandLastSeenInfo = DemandPersistenceUtil.getLastSeenDemandId(personPartyTuple.getPersonID());
            Long lastSeenDemandId = demandLastSeenInfo != null ? demandLastSeenInfo.getLastSeenDemandID() : null;
            // get new demand count
            int count = DemandPersistenceUtil.getNewDemandsCount(personPartyTuple.getCompanyID(), lastSeenDemandId,dueDate);
            // create the response
            DemandLastSeenResponse demandLastSeenResponse = new DemandLastSeenResponse(lastSeenDemandId, count);

            logger.info("Completed request to retrieve demand last seen response for the user");
            return ResponseEntity.status(HttpStatus.OK).body(demandLastSeenResponse);

        } catch (Exception e) {
            throw new NimbleException(NimbleExceptionMessageCode.INTERNAL_SERVER_ERROR_FAILED_TO_GET_DEMAND_LAST_SEEN_RESPONSE.toString(), e);
        }
    }

    private void inviteCompaniesToDemandDetails(DemandType demand, String bearerToken, String languageId){
        new Thread(() -> {
            identityClient.inviteCompaniesToDemandDetails(demand, bearerToken,languageId);
        }).start();
    }

    private String normalizeQueryTerm(String query) {
        if (query != null) {
            if (query.trim().contentEquals("")) {
                query = null;
            } else {
                query = query.trim();
            }
        }
        return query;
    }
}