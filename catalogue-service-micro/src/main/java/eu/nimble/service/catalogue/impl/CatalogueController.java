package eu.nimble.service.catalogue.impl;

import eu.nimble.service.catalogue.CatalogueServiceImpl;
import eu.nimble.service.catalogue.config.RoleConfig;
import eu.nimble.service.catalogue.exception.CatalogueServiceException;
import eu.nimble.service.catalogue.exception.NimbleExceptionMessageCode;
import eu.nimble.service.catalogue.index.ItemIndexClient;
import eu.nimble.service.catalogue.model.catalogue.CatalogueLineSortOptions;
import eu.nimble.service.catalogue.model.catalogue.CatalogueIDResponse;
import eu.nimble.service.catalogue.model.catalogue.CataloguePaginationResponse;
import eu.nimble.service.catalogue.persistence.util.CatalogueDatabaseAdapter;
import eu.nimble.service.catalogue.persistence.util.CataloguePersistenceUtil;
import eu.nimble.service.catalogue.persistence.util.LockPool;
import eu.nimble.service.catalogue.util.CatalogueEvent;
import eu.nimble.service.catalogue.util.SpringBridge;
import eu.nimble.service.catalogue.util.email.EmailSenderUtil;
import eu.nimble.service.catalogue.validation.CatalogueValidator;
import eu.nimble.service.catalogue.validation.ValidationMessages;
import eu.nimble.service.model.modaml.catalogue.TEXCatalogType;
import eu.nimble.service.model.ubl.catalogue.CatalogueType;
import eu.nimble.service.model.ubl.commonaggregatecomponents.PartyNameType;
import eu.nimble.service.model.ubl.commonaggregatecomponents.PartyType;
import eu.nimble.service.model.ubl.commonaggregatecomponents.PersonType;
import eu.nimble.utility.*;
import eu.nimble.utility.exception.BinaryContentException;
import eu.nimble.utility.exception.NimbleException;
import eu.nimble.utility.persistence.JPARepositoryFactory;
import eu.nimble.utility.persistence.resource.ResourceValidationUtility;
import eu.nimble.utility.serialization.TransactionEnabledSerializationUtility;
import eu.nimble.utility.validation.IValidationUtil;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import org.apache.poi.ss.usermodel.Workbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;
import java.util.zip.ZipInputStream;

/**
 * Catalogue level REST services. A catalogue is a collection of products or services on which various business processes
 * can be executed. A catalogue contains contains catalogue lines each of which corresponds to a product or service.
 */
@Controller
//@Transactional(transactionManager = "ubldbTransactionManager")
public class CatalogueController {

    private String defaultLanguage = "en";

    private static Logger log = LoggerFactory
            .getLogger(CatalogueController.class);

    @Autowired
    private CatalogueServiceImpl service;
    @Autowired
    private TransactionEnabledSerializationUtility serializationUtility;
    @Autowired
    private ResourceValidationUtility resourceValidationUtil;
    @Autowired
    private LockPool lockPool;
    @Autowired
    private IValidationUtil validationUtil;
    @Autowired
    private ItemIndexClient itemIndexClient;
    @Autowired
    private ExecutionContext executionContext;
    @Autowired
    private EmailSenderUtil emailSenderUtil;

    @CrossOrigin(origins = {"*"})
    @ApiOperation(value = "", notes = "Retrieves the default CataloguePaginationResponse for the specified party.")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Retrieved CataloguePaginationResponse for the specified party successfully", response = CataloguePaginationResponse.class),
            @ApiResponse(code = 400, message = "Both language id and search text should be provided"),
            @ApiResponse(code = 401, message = "Invalid role."),
            @ApiResponse(code = 404, message = "No default catalogue for the party"),
            @ApiResponse(code = 500, message = "Failed to get CataloguePaginationResponse for the party")
    })
    @RequestMapping(value = "/catalogue/{partyId}/pagination",
            produces = {"application/json"},
            method = RequestMethod.GET)
    public ResponseEntity getDefaultCataloguePagination(@ApiParam(value = "Identifier of the party for which the catalogue to be retrieved", required = true) @PathVariable String partyId,
                                                        @ApiParam(value = "Identifier of the catalogue", required = true) @RequestParam(value = "catalogueId") String catalogueId,
                                                        @ApiParam(value = "Number of catalogue lines to be included in CataloguePaginationResponse ",required = true) @RequestParam(value = "limit",required = true) Integer limit,
                                                        @ApiParam(value = "Offset of the first catalogue line among all catalogue lines of the default catalogue for the party",required = true) @RequestParam(value = "offset",required = true) Integer offset,
                                                        @ApiParam(value = "Text to be used to filter the catalogue lines.Item name and description will be searched for the given text.") @RequestParam(value = "searchText",required = false) String searchText,
                                                        @ApiParam(value = "Identifier for the language of search text such as en and tr") @RequestParam(value = "languageId",required = false) String languageId,
                                                        @ApiParam(value = "Name of the category which is used to filter catalogue lines.Catalogue lines are added to the response if and only if they contain the given category.") @RequestParam(value = "categoryName",required = false) String categoryName,
                                                        @ApiParam(value = "Option used to sort catalogue lines") @RequestParam(value = "sortOption",required = false) CatalogueLineSortOptions sortOption,
                                                        @ApiParam(value = "The Bearer token provided by the identity service", required = true) @RequestHeader(value = "Authorization", required = true) String bearerToken) {
        // set request log of ExecutionContext
        String requestLog = String.format("Incoming request to get CataloguePaginationResponse for party: %s, catalogue id: %s with limit: %s, offset: %s", partyId, catalogueId, limit, offset);
        executionContext.setRequestLog(requestLog);

        log.info(requestLog);
        // validate role
        if(!validationUtil.validateRole(bearerToken, executionContext.getUserRoles(),RoleConfig.REQUIRED_ROLES_CATALOGUE_READ)) {
            throw new NimbleException(NimbleExceptionMessageCode.UNAUTHORIZED_INVALID_ROLE.toString());
        }

        // check the validity of the request params
        if(searchText != null && languageId == null){
            throw new NimbleException(NimbleExceptionMessageCode.BAD_REQUEST_MISSING_PARAMETERS.toString());
        }

        if(catalogueId.contentEquals("all")){
            List<String> ids = CataloguePersistenceUtil.getCatalogueIdListsForParty(partyId);
            for (String id : ids) {
                if(!CataloguePersistenceUtil.checkCatalogueForWhiteBlackList(id,partyId,executionContext.getVatNumber())){
                    throw new NimbleException(NimbleExceptionMessageCode.FORBIDDEN_ACCESS_CATALOGUE_BY_ID.toString(),Arrays.asList(partyId,id));
                }
            }
        } else{
            if(!CataloguePersistenceUtil.checkCatalogueForWhiteBlackList(catalogueId,partyId,executionContext.getVatNumber())){
                throw new NimbleException(NimbleExceptionMessageCode.FORBIDDEN_ACCESS_CATALOGUE_BY_ID.toString(),Arrays.asList(partyId,catalogueId));
            }
        }

        CataloguePaginationResponse cataloguePaginationResponse;

        try {
            cataloguePaginationResponse = service.getCataloguePaginationResponse(catalogueId, partyId,categoryName,searchText,languageId,sortOption,limit,offset);
        } catch (Exception e) {
            throw new NimbleException(NimbleExceptionMessageCode.INTERNAL_SERVER_ERROR_CATALOGUE_PAGINATION_RESPONSE.toString(), Arrays.asList(partyId, catalogueId),e);
        }
        if (cataloguePaginationResponse.getCatalogueUuid() == null) {
            throw new NimbleException(NimbleExceptionMessageCode.NOT_FOUND_NO_CATALOGUE_FOR_PARTY.toString(),Arrays.asList(partyId, catalogueId));
        }

        log.info("Completed request to get CataloguePaginationResponse for party: {}, catalogue id: {} with limit: {}, offset: {}", partyId, catalogueId, limit, offset);
        return ResponseEntity.ok(serializationUtility.serializeUBLObject(cataloguePaginationResponse));
    }


    @CrossOrigin(origins = {"*"})
    @ApiOperation(value = "", notes = "Retrieves the catalogue for the given standard and uuid.")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Retrieved the catalogue successfully", response = CatalogueType.class),
            @ApiResponse(code = 400, message = "Invalid standard"),
            @ApiResponse(code = 401, message = "Invalid token. No user was found for the provided token"),
            @ApiResponse(code = 404, message = "No catalogue for the given uuid and standard"),
            @ApiResponse(code = 500, message = "Unexpected error while getting catalogue")
    })
    @RequestMapping(value = "/catalogue/{standard}/{uuid}",
            produces = {"application/json"},
            method = RequestMethod.GET)
    public ResponseEntity getCatalogue(@ApiParam(value = "Data model standard that the provided catalogue is compatible with.", defaultValue = "ubl", required = true) @PathVariable String standard,
                                       @ApiParam(value = "uuid of the catalogue to be retrieved.", required = true) @PathVariable String uuid,
                                       @ApiParam(value = "The Bearer token provided by the identity service", required = true) @RequestHeader(value = "Authorization", required = true) String bearerToken) {
        // set request log of ExecutionContext
        String requestLog = String.format("Incoming request to get catalogue for standard: %s, uuid: %s", standard, uuid);
        executionContext.setRequestLog(requestLog);

        log.info(requestLog);
        // validate role
        if(!validationUtil.validateRole(bearerToken,executionContext.getUserRoles(), RoleConfig.REQUIRED_ROLES_CATALOGUE_READ)) {
            throw new NimbleException(NimbleExceptionMessageCode.UNAUTHORIZED_INVALID_ROLE.toString());
        }

        Configuration.Standard std;
        try {
            std = getStandardEnum(standard);
        } catch (Exception e) {
            throw new NimbleException(NimbleExceptionMessageCode.BAD_REQUEST_INVALID_STANDARD.toString(),e);
        }
        Object catalogue;
        try {
            catalogue = service.getCatalogue(uuid, std);
        } catch (Exception e) {
            throw new NimbleException(NimbleExceptionMessageCode.INTERNAL_SERVER_ERROR_FAILED_TO_GET_CATALOGUE_FOR_STANDARD.toString(),Arrays.asList(standard,uuid),e);
        }

        if (catalogue == null) {
            throw new NimbleException(NimbleExceptionMessageCode.NOT_FOUND_NO_CATALOGUE.toString(),Arrays.asList(uuid));
        }

        if(!CataloguePersistenceUtil.checkCatalogueForWhiteBlackList(uuid,executionContext.getVatNumber())){
            throw new NimbleException(NimbleExceptionMessageCode.FORBIDDEN_ACCESS_CATALOGUE.toString(),Arrays.asList(uuid));
        }

        log.info("Completed request to get catalogue for standard: {}, uuid: {}", standard, uuid);
        return ResponseEntity.ok(serializationUtility.serializeUBLObject(catalogue));
    }

    @CrossOrigin(origins = {"*"})
    @ApiOperation(value = "", notes = "Sends a request (as a mail) for catalogue exchange to the catalogue provider")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Requested catalogue exchange successfully"),
            @ApiResponse(code = 401, message = "Invalid role."),
            @ApiResponse(code = 500, message = "Unexpected error while requesting catalogue exchange")
    })
    @RequestMapping(value = "/catalogue/exchange",
            produces = {"application/json"},
            method = RequestMethod.POST)
    public ResponseEntity requestCatalogueExchange(@ApiParam(value = "The uuid of catalogue to be requested for the exchange", required = true) @RequestParam(value = "catalogueUuid",required = true) String catalogueUuid,
                                               @ApiParam(value = "The details of the request") @RequestBody String requestDetails,
                                               @ApiParam(value = "The Bearer token provided by the identity service", required = true) @RequestHeader(value = "Authorization") String bearerToken) {
        // set request log of ExecutionContext
        String requestLog = String.format("Incoming request to exchange catalogue: %s, offer details: %s",catalogueUuid,requestDetails);
        executionContext.setRequestLog(requestLog);

        log.info(requestLog);
        // validate role
        if(!validationUtil.validateRole(bearerToken, executionContext.getUserRoles(),RoleConfig.REQUIRED_ROLES_CATALOGUE_READ)) {
            throw new NimbleException(NimbleExceptionMessageCode.UNAUTHORIZED_INVALID_ROLE.toString());
        }

        // get catalogue name
        List<CatalogueIDResponse> catalogueIDResponses = service.getCatalogueNames(Arrays.asList(catalogueUuid));
        if (catalogueIDResponses.size() == 0) {
            throw new NimbleException(NimbleExceptionMessageCode.NOT_FOUND_NO_CATALOGUE.toString(),Arrays.asList(catalogueUuid));
        }
        // get catalogue provider id
        String catalogueProviderPartyId = CataloguePersistenceUtil.getCatalogueProviderId(catalogueUuid);

        try {
            // get person using the given bearer token
            PersonType person = SpringBridge.getInstance().getiIdentityClientTyped().getPerson(bearerToken);
            // get party for the person
            PartyType requesterParty = SpringBridge.getInstance().getiIdentityClientTyped().getPartyByPersonID(person.getID()).get(0);
            // get party for the catalog provider
            PartyType catalogProvider = SpringBridge.getInstance().getiIdentityClientTyped().getParty(bearerToken,catalogueProviderPartyId,true);

            // send an email
            emailSenderUtil.requestCatalogExchange(requestDetails,catalogueIDResponses.get(0).getId(),requesterParty.getPartyName().get(0).getName().getValue(),catalogProvider);
        } catch (Exception e) {
            throw new NimbleException(NimbleExceptionMessageCode.INTERNAL_SERVER_ERROR_REQUEST_CATALOGUE_EXCHANGE.toString(), Arrays.asList(catalogueUuid,requestDetails),e);
        }

        log.info("Completed the request to exchange catalogue: {}, offer details: {}",catalogueUuid,requestDetails);
        return ResponseEntity.ok(null);
    }

    private <T> T parseCatalogue(String contentType, String serializedCatalogue, Configuration.Standard standard) throws IOException {
        T catalogue = null;
        if (contentType.contentEquals(MediaType.APPLICATION_XML_VALUE)) {
            if (standard == Configuration.Standard.UBL) {
                CatalogueType ublCatalogue = (CatalogueType) JAXBUtility.deserialize(serializedCatalogue, Configuration.UBL_CATALOGUE_PACKAGENAME);
                catalogue = (T) ublCatalogue;

            } else if (standard == Configuration.Standard.MODAML) {
                catalogue = (T) JAXBUtility.deserialize(serializedCatalogue, Configuration.MODAML_CATALOGUE_PACKAGENAME);
            }

        } else if (contentType.contentEquals(MediaType.APPLICATION_JSON_VALUE)) {
            catalogue = (T) JsonSerializationUtility.getObjectMapper().readValue(serializedCatalogue, CatalogueType.class);
        }
        return catalogue;
    }

    private ResponseEntity createCreatedCatalogueResponse(Object catalogue, String baseUrl) {
        String uuid;
        String partyName="";
        if (catalogue instanceof CatalogueType) {
            CatalogueType catalogueObject = (CatalogueType) catalogue;
            if (catalogueObject.getProviderParty() != null) {
                List<PartyNameType> partyNameTypes = catalogueObject.getProviderParty().getPartyName();
                partyName = UblUtil.getName(partyNameTypes);
            }
            uuid = catalogueObject.getUUID().toString();
        } else {
            uuid = ((TEXCatalogType) catalogue).getTCheader().getMsgID();
        }
        URI catalogueURI;
        try {

            catalogueURI = new URI(baseUrl + uuid);
        } catch (URISyntaxException e) {
            String msg = "Failed to generate a URI for the newly created item";
            log.warn(msg, e);
            try {
                log.info("Completed request to add catalogue with an empty URI, uuid: {}", uuid);
                return ResponseEntity.created(new URI("")).body(catalogue);
            } catch (URISyntaxException e1) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("WTF");
            }
        }
        Map<String,String> paramMap = new HashMap<String, String>();
        paramMap.put("companyName",partyName);
        paramMap.put("catalogueId",uuid);
        paramMap.put("activity", CatalogueEvent.CATALOGUE_CREATE.getActivity());
        LoggerUtils.logWithMDC(log, paramMap, LoggerUtils.LogLevel.INFO, "Completed request to add catalogue, uuid: {}", uuid);
        return ResponseEntity.created(catalogueURI).body(serializationUtility.serializeUBLObject(catalogue));
    }

    @CrossOrigin(origins = {"*"})
    @ApiOperation(value = "", notes = "Adds the catalogue passed in a serialized form. The serialized catalogue should be compliant with the specified standard.")
    @ApiResponses(value = {
            @ApiResponse(code = 201, message = "Provided catalogue has been persisted."),
            @ApiResponse(code = 401, message = "Invalid token. No user was found for the provided token"),
            @ApiResponse(code = 400, message = "Invalid standard or invalid content"),
            @ApiResponse(code = 409, message = "A catalogue with the same id exists for the publisher party"),
            @ApiResponse(code = 500, message = "Unexpected error while publishing the catalogue")
    })
    @RequestMapping(value = "/catalogue/{standard}",
            consumes = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE},
            produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE},
            method = RequestMethod.POST)
    public <T> ResponseEntity addCatalogue(@ApiParam(value = "Data model standard that the provided catalogue is compatible with.", defaultValue = "ubl", required = true) @PathVariable String standard,
                                           @ApiParam(value = "Serialized form of the catalogue. Valid serializations can be achieved via JsonSerializationUtility.getObjectMapper method located in the utility module. An example catalogue serialization can be found in: https://github.com/nimble-platform/catalog-service/tree/staging/catalogue-service-micro/src/main/resources/example_content/catalogue.json", required = true) @RequestBody String serializedCatalogue,
                                           @ApiParam(value = "The Bearer token provided by the identity service", required = true) @RequestHeader(value = "Authorization", required = true) String bearerToken, HttpServletRequest request) {
        try {
            // set request log of ExecutionContext
            String requestLog = String.format("Incoming request to post catalogue with standard: %s standard", standard);
            executionContext.setRequestLog(requestLog);

            log.info(requestLog);
            // validate role
            if(!validationUtil.validateRole(bearerToken,executionContext.getUserRoles(), RoleConfig.REQUIRED_ROLES_CATALOGUE_WRITE)) {
                throw new NimbleException(NimbleExceptionMessageCode.UNAUTHORIZED_INVALID_ROLE.toString());
            }

            // get standard
            Configuration.Standard std;
            try {
                std = getStandardEnum(standard);
            } catch (Exception e) {
                throw new NimbleException(NimbleExceptionMessageCode.BAD_REQUEST_INVALID_STANDARD.toString(),Arrays.asList(standard),e);
            }

            String contentType = request.getContentType();
            // get catalogue
            T catalogue;
            try {
                catalogue = parseCatalogue(contentType, serializedCatalogue, std);
            } catch (Exception e) {
                throw new NimbleException(NimbleExceptionMessageCode.BAD_REQUEST_DESERIALIZE_CATALOGUE.toString(),Arrays.asList(serializedCatalogue),e);
            }
            // for ubl catalogues, do the following validations
            if (std.equals(Configuration.Standard.UBL)) {
                CatalogueType ublCatalogue = (CatalogueType) catalogue;
                // validate the content of the catalogue
                CatalogueValidator catalogueValidator = new CatalogueValidator(ublCatalogue);
                ValidationMessages validationMessages = catalogueValidator.validate();
                if(validationMessages.getErrorMessages().size() > 0){
                    throw new NimbleException(validationMessages.getErrorMessages(),validationMessages.getErrorParameters());
                }

                // check catalogue with the same id exists
                boolean catalogueExists = CataloguePersistenceUtil.checkCatalogueExistenceById(ublCatalogue.getID(), ublCatalogue.getProviderParty().getPartyIdentification().get(0).getID());
                if (catalogueExists) {
                    throw new NimbleException(NimbleExceptionMessageCode.CONFLICT_CATALOGUE_ID_ALREADY_EXISTS.toString(),Arrays.asList(ublCatalogue.getID()));
                }

                // check the entity ids
                boolean hjidsExists = resourceValidationUtil.hjidsExit(catalogue);
                if (hjidsExists) {
                    throw new NimbleException(NimbleExceptionMessageCode.BAD_REQUEST_HJIDS.toString(),Arrays.asList(serializedCatalogue));
                }
            }

            catalogue = service.addCatalogue(catalogue, std);
            return createCreatedCatalogueResponse(catalogue, HttpResponseUtil.baseUrl(request));

        } catch (Exception e) {
            throw new NimbleException(NimbleExceptionMessageCode.INTERNAL_SERVER_ERROR_ADD_CATALOGUE.toString(),Arrays.asList(serializedCatalogue),e);
        }
    }

    /**
     * @param catalogueJson
     * @return <li>200 along with the updated catalogue</li>
     * <li>400 in case of an invalid standard or invalid catalogue serialization</li>
     * <li>501 if a standard than ubl is passed</li>
     */
    @CrossOrigin(origins = {"*"})
    @ApiOperation(value = "", notes = "Updates the catalogue represented in JSON serialization. The serialization should be compliant with the UBL standard.")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Updated the catalogue successfully", response = CatalogueType.class),
            @ApiResponse(code = 400, message = "Invalid standard"),
            @ApiResponse(code = 500, message = "Failed to update the catalogue")
    })
    @RequestMapping(value = "/catalogue/{standard}",
            consumes = {"application/json"},
            produces = {"application/json"},
            method = RequestMethod.PUT)
    public ResponseEntity updateCatalogue(@ApiParam(value = "Data model standard that the provided catalogue is compatible with.", defaultValue = "ubl", required = true) @PathVariable String standard,
                                          @ApiParam(value = "Serialized form of the catalogue. Valid serializations can be achieved via JsonSerializationUtility.getObjectMapper method located in the utility module. An example catalogue serialization can be found in: https://github.com/nimble-platform/catalog-service/tree/staging/catalogue-service-micro/src/main/resources/example_content/catalogue.json", required = true) @RequestBody String catalogueJson,
                                          @ApiParam(value = "The Bearer token provided by the identity service", required = true) @RequestHeader(value = "Authorization", required = true) String bearerToken) {
        try {
            // set request log of ExecutionContext
            String requestLog = "Incoming request to update catalogue";
            executionContext.setRequestLog(requestLog);

            log.info(requestLog);
            // validate role
            if(!validationUtil.validateRole(bearerToken,executionContext.getUserRoles(), RoleConfig.REQUIRED_ROLES_CATALOGUE_WRITE)) {
                throw new NimbleException(NimbleExceptionMessageCode.UNAUTHORIZED_INVALID_ROLE.toString());
            }

            // check standard
            Configuration.Standard std;
            try {
                std = getStandardEnum(standard);
                if (std != Configuration.Standard.UBL) {
                    throw new NimbleException(NimbleExceptionMessageCode.BAD_REQUEST_NO_UPDATE_OPERATION_FOR_STANDARD.toString(),Arrays.asList(standard));
                }
            } catch (Exception e) {
                throw new NimbleException(NimbleExceptionMessageCode.BAD_REQUEST_INVALID_STANDARD.toString(),Arrays.asList(standard),e);
            }

            // parse catalogue
            CatalogueType catalogue;
            try {
                catalogue = JsonSerializationUtility.getObjectMapper().readValue(catalogueJson, CatalogueType.class);
            } catch (IOException e) {
                throw new NimbleException(NimbleExceptionMessageCode.BAD_REQUEST_DESERIALIZE_CATALOGUE.toString(),Arrays.asList(catalogueJson),e);
            }

            // validate the catalogue content
            CatalogueValidator catalogueValidator = new CatalogueValidator(catalogue);
            ValidationMessages validationMessages = catalogueValidator.validate();
            if(validationMessages.getErrorMessages().size() > 0){
                throw new NimbleException(validationMessages.getErrorMessages(),validationMessages.getErrorParameters());
            }

            if(!CataloguePersistenceUtil.checkCatalogueForWhiteBlackList(catalogue.getUUID(),executionContext.getVatNumber())){
                throw new NimbleException(NimbleExceptionMessageCode.FORBIDDEN_ACCESS_CATALOGUE.toString(), Collections.singletonList(catalogue.getUUID()));
            }

            // validate the entity ids
            boolean hjidsBelongToCompany = resourceValidationUtil.hjidsBelongsToParty(catalogue, catalogue.getProviderParty().getPartyIdentification().get(0).getID(), Configuration.Standard.UBL.toString());
            if (!hjidsBelongToCompany) {
                throw new NimbleException(NimbleExceptionMessageCode.BAD_REQUEST_INVALID_HJIDS.toString(),Arrays.asList(catalogueJson));
            }
            String catalogueId = "";
            String partyName = "";
            try {
                catalogue = service.updateCatalogue(catalogue);
                if (catalogue.getProviderParty() != null) {
                    List<PartyNameType> partyNameTypes = catalogue.getProviderParty().getPartyName();
                    partyName = UblUtil.getName(partyNameTypes);
                }
                if (catalogue.getUUID() != null) {
                    catalogueId = catalogue.getUUID().toString();
                }
                Map<String,String> paramMap = new HashMap<String, String>();
                paramMap.put("activity", CatalogueEvent.CATALOGUE_UPDATE.getActivity());
                paramMap.put("catalogueId", catalogueId);
                paramMap.put("companyName", partyName);
                LoggerUtils.logWithMDC(log, paramMap, LoggerUtils.LogLevel.INFO, "Successfully updated catalogue, id: {}", catalogueId);
            } catch (Exception e) {
                log.warn("Failed to update the following catalogue: {}", catalogueJson);
                throw new NimbleException(NimbleExceptionMessageCode.INTERNAL_SERVER_ERROR_UPDATE_CATALOGUE.toString(),e);
            }

            log.info("Completed request to update the catalogue. uuid: {}", catalogue.getUUID());
            return ResponseEntity.ok(serializationUtility.serializeUBLObject(catalogue));

        } catch (Exception e) {
            throw new NimbleException(NimbleExceptionMessageCode.INTERNAL_SERVER_ERROR_UNEXPECTED_ERROR_WHILE_UPDATING_CATALOGUE.toString(),Arrays.asList(catalogueJson),e);
        }
    }

    @CrossOrigin(origins = {"*"})
    @ApiOperation(value = "", notes = "Deletes the specified catalogue")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Specified catalogue has been deleted successfully"),
            @ApiResponse(code = 400, message = "Invalid standard"),
            @ApiResponse(code = 500, message = "Unexpected error while deleting the catalogue")
    })
    @RequestMapping(value = "/catalogue/{standard}/{uuid}",
            method = RequestMethod.DELETE)
    public ResponseEntity deleteCatalogue(@ApiParam(value = "Data model standard that the provided catalogue is compatible with.", defaultValue = "ubl", required = true) @PathVariable String standard,
                                          @ApiParam(value = "uuid of the catalogue to be retrieved.", required = true) @PathVariable(value = "uuid", required = true) String uuid,
                                          @ApiParam(value = "The Bearer token provided by the identity service", required = true) @RequestHeader(value = "Authorization", required = true) String bearerToken) {
        // set request log of ExecutionContext
        String requestLog = String.format("Incoming request to delete catalogue with uuid: %s", uuid);
        executionContext.setRequestLog(requestLog);

        log.info(requestLog);
        // validate role
        if(!validationUtil.validateRole(bearerToken,executionContext.getUserRoles(), RoleConfig.REQUIRED_ROLES_CATALOGUE_WRITE)) {
            throw new NimbleException(NimbleExceptionMessageCode.UNAUTHORIZED_INVALID_ROLE.toString());
        }

        Configuration.Standard std;
        try {
            std = getStandardEnum(standard);
        } catch (Exception e) {
            throw new NimbleException(NimbleExceptionMessageCode.BAD_REQUEST_INVALID_STANDARD.toString(),Arrays.asList(standard),e);
        }

        if(!CataloguePersistenceUtil.checkCatalogueForWhiteBlackList(uuid,executionContext.getVatNumber())){
            throw new NimbleException(NimbleExceptionMessageCode.FORBIDDEN_ACCESS_CATALOGUE.toString(), Collections.singletonList(uuid));
        }

        try {
            service.deleteCatalogue(uuid, std);
            Map<String,String> paramMap = new HashMap<String, String>();
            paramMap.put("activity", CatalogueEvent.CATALOGUE_DELETE.getActivity());
            paramMap.put("catalogueId", uuid);
            LoggerUtils.logWithMDC(log, paramMap, LoggerUtils.LogLevel.INFO, "Successfully deleted catalogue, id: {}", uuid);
        } catch (Exception e) {
            throw new NimbleException(NimbleExceptionMessageCode.INTERNAL_SERVER_ERROR_DELETE_CATALOGUE.toString(),e);
        }

        log.info("Completed request to delete catalogue with uuid: {}", uuid);
        return ResponseEntity.status(HttpStatus.OK).build();
    }

    @CrossOrigin(origins = {"*"})
    @ApiOperation(value = "", notes = "Deletes the specified catalogues")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Specified catalogue has been deleted successfully"),
            @ApiResponse(code = 401, message = "Invalid token"),
            @ApiResponse(code = 500, message = "Unexpected error while deleting the catalogues")
    })
    @RequestMapping(value = "/catalogue",
            method = RequestMethod.DELETE)
    public ResponseEntity deleteCataloguesForParty(@ApiParam(value = "Identifier of the party for which the catalogues to be deleted", required = true) @RequestParam(value = "partyId", required = true) String partyId,
                                                   @ApiParam(value = "An indicator for selecting all the catalogues to be deleted. ", required = false) @RequestParam(value = "deleteAll", required = false, defaultValue = "false") Boolean deleteAll,
                                                   @ApiParam(value = "Identifier of the catalogues to be deleted. (catalogue.id)", required = false) @RequestParam(value = "ids", required = false) List<String> ids,
                                                   @ApiParam(value = "The Bearer token provided by the identity service", required = true) @RequestHeader(value = "Authorization", required = true) String bearerToken) {
        String idsLog = ids == null ? "" : ids.toString();
        try {
            // set request log of ExecutionContext
            String requestLog = String.format("Incoming request to delete catalogues for party: %s, ids: %s, delete all: %s", partyId, idsLog, deleteAll);
            executionContext.setRequestLog(requestLog);

            log.info(requestLog);
            // validate role
            if(!validationUtil.validateRole(bearerToken, executionContext.getUserRoles(),RoleConfig.REQUIRED_ROLES_CATALOGUE_WRITE)) {
                throw new NimbleException(NimbleExceptionMessageCode.UNAUTHORIZED_INVALID_ROLE.toString());
            }

            // if all the catalogues is requested to be deleted get the identifiers first
            if(deleteAll) {
                ids = CataloguePersistenceUtil.getCatalogueIdListsForParty(partyId);
            }

            if(ids != null){
                for (String id : ids) {
                    if (!CataloguePersistenceUtil.checkCatalogueForWhiteBlackList(id,partyId,executionContext.getVatNumber())) {
                        throw new NimbleException(NimbleExceptionMessageCode.FORBIDDEN_ACCESS_CATALOGUE_BY_ID.toString(), Arrays.asList(partyId,id));
                    }
                    service.deleteCatalogue(id, partyId);
                    Map<String,String> paramMap = new HashMap<String, String>();
                    paramMap.put("activity", CatalogueEvent.CATALOGUE_DELETE.getActivity());
                    paramMap.put("catalogueId", id);
                    paramMap.put("companyId", partyId);
                    LoggerUtils.logWithMDC(log, paramMap, LoggerUtils.LogLevel.INFO, "Successfully deleted catalogue, id: {} for company: {}", id, partyId);
                }
            }

            log.info("Completed request to delete catalogues for party: {}, ids: {}, delete all: {}", partyId, idsLog, deleteAll);
            return ResponseEntity.status(HttpStatus.OK).build();

        } catch(Exception e) {
            throw new NimbleException(NimbleExceptionMessageCode.INTERNAL_SERVER_ERROR_DELETE_CATALOGUES.toString(),Arrays.asList(partyId, idsLog, deleteAll.toString()));
        }
    }

    @CrossOrigin(origins = {"*"})
    @ApiOperation(value = "", notes = "Generates an excel-based template for the specified categories. Category ids and " +
            "taxonomy ids must be provided in comma separated manner and they must have the same number of elements. " +
            "Taxonomy id must be provided such that they specify the taxonomies including the specified categories. See " +
            "the examples in parameter definitions.")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Generated the template for the given categories and taxonomy ids", response = CatalogueType.class),
            @ApiResponse(code = 401, message = "Invalid token. No user was found for the provided token"),
            @ApiResponse(code = 500, message = "Unexpected error while generating template"),
    })
    @RequestMapping(value = "/catalogue/template",
            method = RequestMethod.GET,
            produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    public void downloadTemplate(
            @ApiParam(value = "Category ids for which the properties to be generated in the template. Examples: http://www.aidimme.es/FurnitureSectorOntology.owl#MDFBoard,0173-1#01-ACH237#011", required = true) @RequestParam("categoryIds") List<String> categoryIds,
            @ApiParam(value = "Taxonomy ids corresponding to the categories specified in the categoryIds parameter", required = true) @RequestParam("taxonomyIds") List<String> taxonomyIds,
            @ApiParam(value = "The Bearer token provided by the identity service", required = true) @RequestHeader(value = "Authorization", required = true) String bearerToken,
            @RequestParam("templateLanguage") String templateLanguage,
            HttpServletResponse response) {
        // set request log of ExecutionContext
        String requestLog = String.format("Incoming request to generate a template. Category ids: %s, taxonomy ids: %s", categoryIds, taxonomyIds);
        executionContext.setRequestLog(requestLog);

        log.info(requestLog);
        // validate role
        if(!validationUtil.validateRole(bearerToken,executionContext.getUserRoles(), RoleConfig.REQUIRED_ROLES_CATALOGUE_READ)) {
            throw new NimbleException(NimbleExceptionMessageCode.UNAUTHORIZED_INVALID_ROLE.toString());
        }

        Workbook template;
        try {
            template = service.generateTemplateForCategory(categoryIds, taxonomyIds,templateLanguage);
        } catch (Exception e) {
            throw new NimbleException(NimbleExceptionMessageCode.INTERNAL_SERVER_ERROR_GENERATE_TEMPLATE.toString(),e,true);
        }

        try {
            String fileName = "product_data_template.xlsx";
            response.setHeader("Content-disposition", "attachment; filename=" + fileName);
            response.addHeader("Access-Control-Expose-Headers", "Content-Disposition");
            template.write(response.getOutputStream());
            response.flushBuffer();
            log.info("Completed the request to generate template");
        } catch (IOException e) {
            throw new NimbleException(NimbleExceptionMessageCode.INTERNAL_SERVER_ERROR_WRITE_TEMPLATE_CONTENT_TO_OUTPUT_STREAM.toString(),e,true);
        }
    }

    @CrossOrigin(origins = {"*"})
    @ApiOperation(value = "", notes = "Adds the catalogue specified with the provided template. The created catalogue is compliant with the default " +
            "standard, which is UBL. If there is a published catalogue already, the type of update is realized according to " +
            "the update mode. There are two update modes: append and replace. In the former mode, if some of the products were " +
            "already published, they are replaced with the new ones; furthermore, the brand new ones are appended to the " +
            "existing product list. In the latter mode, all previously published products having the same categories specified in the template are deleted," +
            "the new list of products is set as it is.")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Persisted uploaded template successfully and returned the corresponding catalogue", response = CatalogueType.class),
            @ApiResponse(code = 400, message = "Invalid template content"),
            @ApiResponse(code = 401, message = "Invalid token. No user was found for the provided token"),
            @ApiResponse(code = 500, message = "Unexpected error while uploading the template")
    })
    @RequestMapping(value = "/catalogue/template/upload",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE,
            method = RequestMethod.POST)
    public ResponseEntity uploadTemplate(
            @ApiParam(value = "Filled in excel-based template. An example filled in template can be found in: https://github.com/nimble-platform/catalog-service/tree/staging/catalogue-service-micro/src/main/resources/example_content/product_data_template.xlsx . Check the \"Information\" tab for detailed instructions. Furthermore, example instantiations can be found in the \"Product Properties Example\" and \"Trading and Delivery Terms Example\" tabs.", required = true) @RequestParam("file") MultipartFile file,
            @ApiParam(value = "Upload mode for the catalogue. Possible options are: append and replace", defaultValue = "append") @RequestParam(value = "uploadMode", defaultValue = "append") String uploadMode,
            @ApiParam(value = "Identifier of the party for which the catalogue will be published", required = true) @RequestParam("partyId") String partyId,
            @ApiParam(value = "Whether VAT should be set for the uploaded products or not", required = true) @RequestParam(value = "includeVat", defaultValue = "true") Boolean includeVat ,
            @ApiParam(value = "The Bearer token provided by the identity service", required = true) @RequestHeader(value = "Authorization", required = true) String bearerToken,
            HttpServletRequest request) {
        try {
            // set request log of ExecutionContext
            String requestLog = String.format("Incoming request to upload template upload mode: %s, party id: %s", uploadMode, partyId);
            executionContext.setRequestLog(requestLog);

            log.info(requestLog);
            // validate role
            if(!validationUtil.validateRole(bearerToken,executionContext.getUserRoles(), RoleConfig.REQUIRED_ROLES_CATALOGUE_WRITE)) {
                throw new NimbleException(NimbleExceptionMessageCode.UNAUTHORIZED_INVALID_ROLE.toString());
            }

            // check the existence of the specified party in the catalogue DB
            PartyType party = CatalogueDatabaseAdapter.syncPartyInUBLDB(partyId, bearerToken);

            CatalogueType catalogue;

            // lock the update for the specified party
            try {
                lockPool.getLockForParty(partyId).writeLock().lock();


                // parse catalogue
                catalogue = CataloguePersistenceUtil.getCatalogueForParty("default", partyId);

                if (catalogue != null) {
                    if (!CataloguePersistenceUtil.checkCatalogueForWhiteBlackList(catalogue.getUUID(), executionContext.getVatNumber())) {
                        throw new NimbleException(NimbleExceptionMessageCode.FORBIDDEN_ACCESS_CATALOGUE.toString(), Collections.singletonList(catalogue.getUUID()));
                    }
                }

                catalogue = service.saveTemplate(file.getInputStream(), uploadMode, party, includeVat, "default", catalogue);

            } finally {
                lockPool.getLockForParty(partyId).writeLock().unlock();
            }

            URI catalogueURI;
            try {
                catalogueURI = new URI(HttpResponseUtil.baseUrl(request) + catalogue.getUUID());
                log.info("Completed the request to upload template. Added catalogue uuid: {}", catalogue.getUUID());
                return ResponseEntity.created(catalogueURI).build();

            } catch (URISyntaxException e) {
                throw new NimbleException(NimbleExceptionMessageCode.INTERNAL_SERVER_ERROR_GENERATE_URI_FOR_ITEM.toString(),e);
            }


        } catch (Exception e) {
            throw new NimbleException(NimbleExceptionMessageCode.INTERNAL_SERVER_ERROR_UPLOAD_TEMPLATE.toString(),e);
        }
    }

    @CrossOrigin(origins = {"*"})
    @ApiOperation(value = "", notes = "Returns the supported catalogue data model standards. Only the UBL standard is supported for the being considering " +
            "the full-fledged catalogue management services as well as connection of published products with the business processes.")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Retrieve the supported standards successfully", response = String.class, responseContainer = "List"),
            @ApiResponse(code = 401, message = "Invalid token. No user was found for the provided token"),
            @ApiResponse(code = 500, message = "Unexpected error while getting the supported standards")
    })
    @RequestMapping(value = "/catalogue/standards",
            produces = {"application/json"},
            method = RequestMethod.GET)
    public ResponseEntity getSupportedStandards(@ApiParam(value = "The Bearer token provided by the identity service", required = true) @RequestHeader(value = "Authorization", required = true) String authorization) {
        // set request log of ExecutionContext
        String requestLog = "Incoming request to retrieve the supported standards";
        executionContext.setRequestLog(requestLog);

        log.info(requestLog);

        List<Configuration.Standard> standards = new ArrayList<>();
        try {
//            standards = Arrays.asList(Configuration.Standard.values());
            standards.add(Configuration.Standard.UBL);
        } catch (Exception e) {
            throw new NimbleException(NimbleExceptionMessageCode.INTERNAL_SERVER_ERROR_GET_STANDARDS.toString(),e);
        }

        log.info("Completed request to retrieve the supported standards");
        return ResponseEntity.ok(standards);
    }

    @CrossOrigin(origins = {"*"})
    @ApiOperation(value = "", notes = "Associate the images provided in a ZIP package to relevant products. The images " +
            "are associated with the target products via the product ids. For this, the image names should start with a " +
            "valid product id. Specifically image name format should be as follows: &lt;productId&gt;.&lt;imageName&gt;.&lt;imageFormat&gt; " +
            "e.g. Product_id1.product1_image.jpg")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Added the images provided in the package object to relevant products successfully."),
            @ApiResponse(code = 400, message = "Failed obtain a Zip package from the provided data"),
            @ApiResponse(code = 404, message = "Catalogue with the given uuid does not exist"),
            @ApiResponse(code = 500, message = "Unexpected error while uploading images")
    })
    @RequestMapping(value = "/catalogue/{id}/image/upload",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE,
            method = RequestMethod.POST)
    public ResponseEntity uploadImages(
            @ApiParam(value = "The package compressed as a Zip file, including the images. An example image package can be found in: https://github.com/nimble-platform/catalog-service/tree/staging/catalogue-service-micro/src/main/resources/example_content/images.zip", required = true) @RequestParam("package") MultipartFile pack,
            @ApiParam(value = "id of the catalogue to be retrieved.", required = true) @PathVariable("id") String id,
            @ApiParam(value = "Identifier of the party for which the catalogue will be updated", required = true) @RequestParam("partyId") String partyId,
            @ApiParam(value = "The Bearer token provided by the identity service", required = true) @RequestHeader(value = "Authorization", required = true) String bearerToken) {
        try {
            // set request log of ExecutionContext
            String requestLog = String.format("Incoming request to upload images for catalogue: %s", id);
            executionContext.setRequestLog(requestLog);

            log.info(requestLog);
            // validate role
            if(!validationUtil.validateRole(bearerToken,executionContext.getUserRoles(), RoleConfig.REQUIRED_ROLES_CATALOGUE_WRITE)) {
                throw new NimbleException(NimbleExceptionMessageCode.UNAUTHORIZED_INVALID_ROLE.toString());
            }

            String catalogueUUid = CataloguePersistenceUtil.getCatalogueUUid(id, partyId);

            if(!CataloguePersistenceUtil.checkCatalogueForWhiteBlackList(id,executionContext.getVatNumber())){
                throw new NimbleException(NimbleExceptionMessageCode.FORBIDDEN_ACCESS_CATALOGUE.toString(),Arrays.asList(id));
            }

            if(!pack.getOriginalFilename().endsWith(".zip")){
                log.error("Provided file to upload images is not zip");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("You should provide a Zip package to upload images");
            }

            ZipInputStream zis = null;
            try {
                zis = new ZipInputStream(pack.getInputStream());
                service.addImagesToProducts(zis, catalogueUUid);

            } catch (IOException e) {
                throw new NimbleException(NimbleExceptionMessageCode.BAD_REQUEST_GET_ZIP_PACKAGE.toString(),e);
            } catch (CatalogueServiceException | BinaryContentException e) {
                throw new NimbleException(NimbleExceptionMessageCode.BAD_REQUEST_UPLOAD_IMAGES.toString(),e);
            } catch (Exception e){
                throw new NimbleException(NimbleExceptionMessageCode.INTERNAL_SERVER_ERROR_UPLOAD_IMAGES.toString(),e);
            }finally {
                try {
                    zis.close();
                } catch (IOException e) {
                    log.warn("Failed to close Zip stream", e);
                }
            }

            log.info("Completed the request to upload images for catalogue: {}", id);
            return ResponseEntity.ok().build();

        } catch (Exception e) {
            if (e instanceof NimbleException) {
                throw e;
            } else {
                throw new NimbleException(NimbleExceptionMessageCode.INTERNAL_SERVER_ERROR_UNEXPECTED_ERROR_WHILE_UPLOADING_IMAGES.toString(), Arrays.asList(id), e);
            }
        }
    }

    @CrossOrigin(origins = {"*"})
    @ApiOperation(value = "", notes = "Deletes all the images of CatalogueLines of the specified catalogue")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Deleted the images successfully"),
            @ApiResponse(code = 401, message = "Invalid token. No user was found for the provided token"),
            @ApiResponse(code = 500, message = "Unexpected error while getting catalogue")
    })
    @RequestMapping(value = "/catalogue/delete-images",
            method = RequestMethod.GET)
    public ResponseEntity deleteImagesInsideCatalogue(@ApiParam(value = "Identifier of the catalogues whose images to be deleted. (catalogue.id)", required = false) @RequestParam(value = "ids", required = false) List<String> ids,
                                                      @ApiParam(value = "Identifier of the party for which the product images to be deleted ", required = true) @RequestParam(value = "partyId", required = true) String partyId,
                                                      @ApiParam(value = "The Bearer token provided by the identity service", required = true) @RequestHeader(value = "Authorization", required = true) String bearerToken) {
        try {
            // set request log of ExecutionContext
            String requestLog = String.format("Incoming request to delete images for catalogues: %s", ids);
            executionContext.setRequestLog(requestLog);

            log.info(requestLog);
            // validate role
            if(!validationUtil.validateRole(bearerToken, executionContext.getUserRoles(),RoleConfig.REQUIRED_ROLES_CATALOGUE_WRITE)) {
                throw new NimbleException(NimbleExceptionMessageCode.UNAUTHORIZED_INVALID_ROLE.toString());
            }

            for (String id : ids) {
                // catalogue check
                CatalogueType catalogue = CataloguePersistenceUtil.getCatalogueForParty(id, partyId);
                if (catalogue == null) {
                    log.warn("Catalogue with uuid : {} does not exist", id);
                    continue;
                }
                if(!CataloguePersistenceUtil.checkCatalogueForWhiteBlackList(catalogue.getUUID(),executionContext.getVatNumber())){
                    throw new NimbleException(NimbleExceptionMessageCode.FORBIDDEN_ACCESS_CATALOGUE.toString(),Arrays.asList(catalogue.getUUID()));
                }
                // remove the images
                service.removeAllImagesFromCatalogue(catalogue);
            }

            log.info("Deleted images for catalogues: {}",ids);
            return ResponseEntity.ok().body(serializationUtility.serializeUBLObject(null));

        } catch (Exception e) {
            throw new NimbleException(NimbleExceptionMessageCode.INTERNAL_SERVER_ERROR_DELETE_IMAGES.toString(),Arrays.asList(ids.toString()));
        }
    }

    @CrossOrigin(origins = {"*"})
    @ApiOperation(value = "", notes = "Retrieves the catalogue id's for a party.")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Retrieved the catalogue id's successfully"),
            @ApiResponse(code = 401, message = "Invalid token. No user was found for the provided token"),
            @ApiResponse(code = 404, message = "No catalogue ids found for the given party"),
            @ApiResponse(code = 500, message = "Unexpected error while getting catalogue id's")
    })
    @RequestMapping(value = "/catalogue/ids/{partyId}",
            produces = {"application/json"},
            method = RequestMethod.GET)
    public ResponseEntity getAllCatalogueIdsForParty(@ApiParam(value = "Identifier of the party for which the catalogue to be retrieved", required = true) @PathVariable String partyId,
                                              @ApiParam(value = "The Bearer token provided by the identity service", required = true) @RequestHeader(value = "Authorization", required = true) String bearerToken) {
        // set request log of ExecutionContext
        String requestLog = String.format("Incoming request to get catalogue id list for party: %s", partyId);
        executionContext.setRequestLog(requestLog);

        log.info(requestLog);
        // validate role
        if(!validationUtil.validateRole(bearerToken, executionContext.getUserRoles(),RoleConfig.REQUIRED_ROLES_CATALOGUE_READ)) {
            throw new NimbleException(NimbleExceptionMessageCode.UNAUTHORIZED_INVALID_ROLE.toString());
        }

        List<String> catalogueIds;
        try {
            catalogueIds = service.getCatalogueIdsForParty(partyId);
        } catch (Exception e) {
            throw new NimbleException(NimbleExceptionMessageCode.INTERNAL_SERVER_ERROR_FAILED_TO_GET_CATALOGUES.toString(),e);
        }

        if (catalogueIds == null) {
            throw new NimbleException(NimbleExceptionMessageCode.NOT_FOUND_NO_DEFAULT_CATALOGUE.toString(),Arrays.asList(partyId));
        }

        log.info("Completed request to get catalogue id list for party: {}", partyId);
        return ResponseEntity.ok(catalogueIds);
    }

    @CrossOrigin(origins = {"*"})
    @ApiOperation(value = "", notes = "Retrieves the catalogue id's for a party.")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Retrieved the catalogue uuid's successfully"),
            @ApiResponse(code = 401, message = "Invalid token. No user was found for the provided token"),
            @ApiResponse(code = 404, message = "No catalogue uuids found for the given party"),
            @ApiResponse(code = 500, message = "Unexpected error while getting catalogue id's")
    })
    @RequestMapping(value = "/catalogue/idsuuids/{partyId}",
            produces = {"application/json"},
            method = RequestMethod.GET)
    public ResponseEntity getAllCatalogueIdsUUIDsForParty(@ApiParam(value = "Identifier of the party for which the catalogue to be retrieved", required = true) @PathVariable String partyId,
            @ApiParam(value = "The Bearer token provided by the identity service", required = true) @RequestHeader(value = "Authorization", required = true) String bearerToken) {
        // set request log of ExecutionContext
        String requestLog = String.format("Incoming request to get catalogue uuidid list for party: %s", partyId);
        executionContext.setRequestLog(requestLog);

        log.info(requestLog);
        // validate role
        if(!validationUtil.validateRole(bearerToken,executionContext.getUserRoles() ,RoleConfig.REQUIRED_ROLES_CATALOGUE_READ)) {
            throw new NimbleException(NimbleExceptionMessageCode.UNAUTHORIZED_INVALID_ROLE.toString());
        }

        List<Object[]> catalogueIds;
        try {
            catalogueIds = service.getCatalogueIdAndNameForParty(partyId);
        } catch (Exception e) {
            throw new NimbleException(NimbleExceptionMessageCode.INTERNAL_SERVER_ERROR_FAILED_TO_GET_CATALOGUES.toString(),Arrays.asList(partyId),e);
        }

        if (catalogueIds == null) {
            throw new NimbleException(NimbleExceptionMessageCode.NOT_FOUND_NO_DEFAULT_CATALOGUE.toString(),Arrays.asList(partyId));
        }

        log.info("Completed request to get catalogue uuid list for party: {}", partyId);
        return ResponseEntity.ok(catalogueIds);
    }

    @CrossOrigin(origins = {"*"})
    @ApiOperation(value = "", notes = "Retrieves catalogue IDs for the given UUIDs.")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Retrieved catalogue names successfully"),
            @ApiResponse(code = 401, message = "Invalid token. No user was found for the provided token"),
            @ApiResponse(code = 500, message = "Unexpected error while getting catalogue names")
    })
    @RequestMapping(value = "/catalogue/ids",
            produces = {"application/json"},
            method = RequestMethod.GET)
    public ResponseEntity getCatalogueIds(@ApiParam(value = "UUIDs of catalogues of which names to be retrieved", required = true) @RequestParam List<String> catalogueUuids,
                                          @ApiParam(value = "The Bearer token provided by the identity service", required = true) @RequestHeader(value = "Authorization", required = true) String bearerToken) {
        try {
            // set request log of ExecutionContext
            String requestLog = String.format("Incoming request to get ids for catalogues: %s", catalogueUuids);
            executionContext.setRequestLog(requestLog);

            log.info(requestLog);

            List<CatalogueIDResponse> catalogueIds = service.getCatalogueNames(catalogueUuids);

            log.info("Completed request to get catalogue ids for uuids: {}", catalogueUuids);
            return ResponseEntity.ok(catalogueIds);

        } catch (Exception e) {
            throw new NimbleException(NimbleExceptionMessageCode.INTERNAL_SERVER_ERROR_GET_CATALOGUE_IDS.toString(), catalogueUuids,e);
        }
    }

    @CrossOrigin(origins = {"*"})
    @ApiOperation(value = "", notes = "Adds the given white/black lists to the specified catalogue")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Added white/black lists to the catalogue successfully", response = CatalogueType.class),
            @ApiResponse(code = 401, message = "Invalid role"),
            @ApiResponse(code = 500, message = "Unexpected error while adding white/black list to the catalogue")
    })
    @RequestMapping(value = "/catalogue/{id}/white-black-list",
            consumes = {"application/json"},
            produces = {"application/json"},
            method = RequestMethod.PUT)
    public ResponseEntity addBlackWhiteList(@ApiParam(value = "Uuid of the catalogue to which the white/black lists are added. (catalogue.uuid)", required = true) @PathVariable(value = "id") String id,
                                            @ApiParam(value = "VAT numbers of the companies in the black list", required = false) @RequestParam(value = "blackList",required = false) List<String> blackList,
                                            @ApiParam(value = "VAT numbers of the companies in the white list", required = false) @RequestParam(value = "whiteList",required = false) List<String> whiteList,
                                            @ApiParam(value = "The Bearer token provided by the identity service", required = true) @RequestHeader(value = "Authorization", required = true) String bearerToken) {
        try {
            // set request log of ExecutionContext
            String requestLog = String.format("Incoming request to add white/black list to the catalogue %s",id);
            executionContext.setRequestLog(requestLog);

            log.info(requestLog);
            // validate role
            if(!validationUtil.validateRole(bearerToken,executionContext.getUserRoles(), RoleConfig.REQUIRED_ROLES_CATALOGUE_WRITE)) {
                throw new NimbleException(NimbleExceptionMessageCode.UNAUTHORIZED_INVALID_ROLE.toString());
            }

            CatalogueType catalogue = CataloguePersistenceUtil.getCatalogueByUuid(id);

            catalogue.getPermittedPartyIDItems().clear();
            catalogue.getPermittedPartyID().clear();
            catalogue.getRestrictedPartyIDItems().clear();
            catalogue.getRestrictedPartyID().clear();
            catalogue.setPermittedPartyID(whiteList);
            catalogue.setRestrictedPartyID(blackList);

            catalogue = new JPARepositoryFactory().forCatalogueRepository(true).updateEntity(catalogue);

            // cache catalog
            SpringBridge.getInstance().getCacheHelper().putCatalog(catalogue);
            itemIndexClient.indexCatalogue(catalogue);

            log.info("Completed request to add white/black list to the catalogue {}", id);
            return ResponseEntity.ok(null);

        } catch (Exception e) {
            throw new NimbleException(NimbleExceptionMessageCode.INTERNAL_SERVER_ERROR_UNEXPECTED_ERROR_WHILE_ADDING_WHITE_BLACK_LIST.toString(),Arrays.asList(id),e);
        }
    }

    private ResponseEntity createErrorResponseEntity(String msg, HttpStatus status, Exception e) {
        if (e != null) {
            msg = msg + e.getMessage();
            log.error(msg, e);
        } else {
            log.error(msg);
        }
        return ResponseEntity.status(status).body(msg);
    }

    private Configuration.Standard getStandardEnum(String standard) {
        standard = standard.toUpperCase();
        Configuration.Standard std = Configuration.Standard.valueOf(standard);
        return std;
    }
}
