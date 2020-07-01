package eu.nimble.service.catalogue.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import eu.nimble.service.catalogue.config.RoleConfig;
import eu.nimble.service.catalogue.exception.NimbleExceptionMessageCode;
import eu.nimble.service.catalogue.model.catalogue.CataloguePaginationResponse;
import eu.nimble.service.catalogue.persistence.util.CataloguePersistenceUtil;
import eu.nimble.service.catalogue.util.SpringBridge;
import eu.nimble.service.model.ubl.catalogue.CatalogueType;
import eu.nimble.service.model.ubl.commonaggregatecomponents.ClauseType;
import eu.nimble.utility.ExecutionContext;
import eu.nimble.utility.JsonSerializationUtility;
import eu.nimble.utility.exception.NimbleException;
import eu.nimble.utility.persistence.GenericJPARepository;
import eu.nimble.utility.persistence.JPARepositoryFactory;
import eu.nimble.utility.serialization.TransactionEnabledSerializationUtility;
import eu.nimble.utility.validation.IValidationUtil;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.*;

@Controller
public class ContractController {

    private static Logger log = LoggerFactory
            .getLogger(ContractController.class);

    @Autowired
    private ExecutionContext executionContext;
    @Autowired
    private TransactionEnabledSerializationUtility serializationUtility;
    @Autowired
    private IValidationUtil validationUtil;

    @CrossOrigin(origins = {"*"})
    @ApiOperation(value = "", notes = "Retrieves the contract details for the specified catalogue.")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Retrieved contract details for the specified catalogue successfully"),
            @ApiResponse(code = 401, message = "Invalid role."),
            @ApiResponse(code = 500, message = "Unexpected error while retrieving contract details for the specified catalogue")
    })
    @RequestMapping(value = "/catalogue/contract",
            produces = {"application/json"},
            method = RequestMethod.GET)
    public ResponseEntity getContractForCatalogue(@ApiParam(value = "Comma-separated catalogue uuids to be retrieved e.g. 5e910673-8232-4ec1-adb3-9188377309bf,34rwe231-34ds-5dw2-hgd2-462tdr64wfgs", required = true) @RequestParam(value = "catalogueUuids",required = true) List<String> catalogueUuids,
                                                  @ApiParam(value = "The Bearer token provided by the identity service", required = true) @RequestHeader(value = "Authorization", required = true) String bearerToken) {
        // set request log of ExecutionContext
        String requestLog = String.format("Incoming request to get contract for catalogue uuids: %s", catalogueUuids);
        executionContext.setRequestLog(requestLog);

        log.info(requestLog);
        // validate role
        if(!validationUtil.validateRole(bearerToken, executionContext.getUserRoles(), RoleConfig.REQUIRED_ROLES_CATALOGUE_READ)) {
            throw new NimbleException(NimbleExceptionMessageCode.UNAUTHORIZED_INVALID_ROLE.toString());
        }

        Map<String,List<ClauseType>> catalogueClausesMap = new HashMap<>();

        try {
            for (String catalogueUuid : catalogueUuids) {
                catalogueClausesMap.put(catalogueUuid,CataloguePersistenceUtil.getClausesForCatalogue(catalogueUuid));
            }

        } catch (Exception e) {
            throw new NimbleException(NimbleExceptionMessageCode.INTERNAL_SERVER_ERROR_GET_CONTRACT_FOR_CATALOGUE.toString(), catalogueUuids,e);
        }

        log.info("Completed request to get contract for catalogue uuids: {}", catalogueUuids);
        return ResponseEntity.ok(serializationUtility.serializeUBLObject(catalogueClausesMap));
    }

    @CrossOrigin(origins = {"*"})
    @ApiOperation(value = "", notes = "Sets the contract for the specified party and catalogue.")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Set the contract for the specified party and catalogue successfully", response = CataloguePaginationResponse.class),
            @ApiResponse(code = 401, message = "Invalid role."),
            @ApiResponse(code = 500, message = "Unexpected error while setting the contract for the specified party and catalogue")
    })
    @RequestMapping(value = "/catalogue/{uuid}/contract",
            produces = {"application/json"},
            method = RequestMethod.POST)
    public ResponseEntity setContractForCatalogue(@ApiParam(value = "uuid of the catalogue to be retrieved.", required = true) @PathVariable String uuid,
                                                  @ApiParam(value = "Serialized form of the list of clauses", required = true) @RequestBody String clauses,
                                                  @ApiParam(value = "The Bearer token provided by the identity service", required = true) @RequestHeader(value = "Authorization", required = true) String bearerToken) {

        // set request log of ExecutionContext
        String requestLog = String.format("Incoming request to set contract for catalogue uuid: %s",  uuid);
        executionContext.setRequestLog(requestLog);

        log.info(requestLog);
        // validate role
        if(!validationUtil.validateRole(bearerToken, executionContext.getUserRoles(),RoleConfig.REQUIRED_ROLES_CATALOGUE_WRITE)) {
            throw new NimbleException(NimbleExceptionMessageCode.UNAUTHORIZED_INVALID_ROLE.toString());
        }

        List<ClauseType> clauseTypes;
        try {
            clauseTypes = JsonSerializationUtility.getObjectMapper().readValue(clauses,new TypeReference<List<ClauseType>>(){});
        } catch (IOException e) {
            throw new NimbleException(NimbleExceptionMessageCode.INTERNAL_SERVER_ERROR_SET_CONTRACT_FOR_CATALOGUE.toString(), Collections.singletonList(uuid),e);
        }

        GenericJPARepository repo = new JPARepositoryFactory().forCatalogueRepository(true);
        CatalogueType catalogue = CataloguePersistenceUtil.getCatalogueByUuid(uuid);
        catalogue.setClause(clauseTypes);

        catalogue = repo.updateEntity(catalogue);
        // cache catalog
        SpringBridge.getInstance().getCacheHelper().putCatalog(catalogue);

        log.info("Completed request to set contract for catalogue uuid: {}", uuid);
        return ResponseEntity.ok(null);
    }
}
