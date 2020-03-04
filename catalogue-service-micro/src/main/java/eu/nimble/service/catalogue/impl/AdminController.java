package eu.nimble.service.catalogue.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import eu.nimble.service.catalogue.config.RoleConfig;
import eu.nimble.service.catalogue.index.ItemIndexClient;
import eu.nimble.service.catalogue.persistence.util.CatalogueLinePersistenceUtil;
import eu.nimble.service.catalogue.util.SpringBridge;
import eu.nimble.service.catalogue.util.migration.r10.VatMigrationUtility;
import eu.nimble.utility.ExecutionContext;
import eu.nimble.utility.exception.NimbleException;
import eu.nimble.utility.exception.NimbleExceptionMessageCode;
import eu.nimble.utility.validation.IValidationUtil;
import feign.Response;
import org.json.JSONArray;
import org.json.JSONObject;
import eu.nimble.service.catalogue.index.PartyIndexClient;
import eu.nimble.service.catalogue.model.category.Property;
import eu.nimble.service.catalogue.util.migration.r8.CatalogueIndexLoader;
import eu.nimble.service.model.solr.item.ItemType;
import eu.nimble.service.model.solr.owl.PropertyType;
import eu.nimble.service.model.solr.owl.ValueQualifier;
import eu.nimble.utility.JsonSerializationUtility;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.io.InputStream;
import java.util.Arrays;
import java.util.List;

/**
 * Created by suat on 28-Jan-19.
 */
@Controller
public class AdminController {

    private static final Logger logger = LoggerFactory.getLogger(AdminController.class);

    @Value("${nimble.indexing.url}")
    private String indexingUrl;
    
    @Autowired
    private PartyIndexClient partyIndexClient;
    @Autowired
    private CatalogueIndexLoader catalogueIndexLoader;
    @Autowired
    private ItemIndexClient itemIndexClient;

    @Autowired
    private IValidationUtil validationUtil;
    @Autowired
    private ExecutionContext executionContext;

    @CrossOrigin(origins = {"*"})
    @ApiOperation(value = "", notes = "Indexes UBL properties")
    @ApiResponses(value = {
            @ApiResponse(code = 401, message = "No user exists for the given token")
    })
    @RequestMapping(value = "/admin/index/ubl-properties",
            produces = {"application/json"},
            method = RequestMethod.GET)
    public ResponseEntity indexUBLProperties(@ApiParam(value = "The Bearer token provided by the identity service", required = true) @RequestHeader(value = "Authorization", required = true) String bearerToken) throws Exception{
        // set request log of ExecutionContext
        String requestLog = "Incoming request to index UBL properties";
        executionContext.setRequestLog(requestLog);
        // validate role
        if(!validationUtil.validateRole(bearerToken, RoleConfig.REQUIRED_ROLES_FOR_ADMIN_OPERATIONS)) {
            throw new NimbleException(NimbleExceptionMessageCode.UNAUTHORIZED_INDEX_UBL_PROPERTIES.toString());
        }

        String namespace = "http://www.nimble-project.org/resource/ubl#";

        logger.info("Reading CatalogueProperties.json ...");
        InputStream inputStream = null;
        inputStream =  ClassLoader.getSystemClassLoader().getResourceAsStream("CatalogueProperties.json");
        String fileContent = IOUtils.toString(inputStream);

        logger.info("Read CatalogueProperties.json");

        ObjectMapper objectMapper = new ObjectMapper();

        // Properties
        List<Property> properties = objectMapper.readValue(fileContent,new TypeReference<List<Property>>(){});

        logger.info("Properties are created");

        for(Property property: properties){
            PropertyType indexProperty = new PropertyType();
            indexProperty.setUri(namespace + property.getId());
            property.getPreferredName().forEach(label -> indexProperty.addLabel(label.getLanguageID(), label.getValue()));

            indexProperty.setLocalName(property.getId());
            indexProperty.setNameSpace(namespace);
            indexProperty.setItemFieldNames(Arrays.asList(ItemType.dynamicFieldPart(indexProperty.getUri())));

            if(property.getDataType().equals("http://www.w3.org/2001/XMLSchema#string")){
                indexProperty.setValueQualifier(ValueQualifier.TEXT);
            }
            else if(property.getDataType().equals("http://www.w3.org/2001/XMLSchema#float")){
                indexProperty.setValueQualifier(ValueQualifier.NUMBER);
            }
            else if(property.getDataType().equals("http://www.w3.org/2001/XMLSchema#boolean")){
                indexProperty.setValueQualifier(ValueQualifier.BOOLEAN);
            }

            indexProperty.setRange(property.getDataType());

            // all properties are assumed to be a datatype property (including the quantity properties)
            indexProperty.setPropertyType("DatatypeProperty");
            indexProperty.setLanguages(indexProperty.getLabel().keySet());


            String propertyJson = JsonSerializationUtility.getObjectMapper().writeValueAsString(indexProperty);

            Response response = SpringBridge.getInstance().getiIndexingServiceClient().setItem(bearerToken,propertyJson);

            if (response.status() == HttpStatus.OK.value()) {
                logger.info("Indexed property successfully. property uri: " + indexProperty.getUri());

            } else {
                String msg = String.format("Failed to index property. uri: %s, indexing call status: %d, message: %s", indexProperty.getUri(), response.status(), IOUtils.toString(response.body().asInputStream()));
                logger.error(msg);
            }
        }

        inputStream.close();

        return null;
    }

    @CrossOrigin(origins = {"*"})
    @ApiOperation(value = "", notes = "Indexes all catalogues in the database. If partyId is specified, only catalogues belonging to that party are indexed")
    @ApiResponses(value = {
            @ApiResponse(code = 401, message = "No user exists for the given token")
    })
    @RequestMapping(value = "/admin/index-catalogues",
            produces = {"application/json"},
            method = RequestMethod.POST)
    public ResponseEntity indexAllCatalogues(@ApiParam(value = "Identifier of the party", required = false) @RequestParam(value = "partyId", required = false) String partyId,
                                             @ApiParam(value = "The Bearer token provided by the identity service", required = true) @RequestHeader(value = "Authorization", required = true) String bearerToken) {
        // set request log of ExecutionContext
        String requestLog = String.format("Incoming request to index all catalogues for party %s",partyId);
        executionContext.setRequestLog(requestLog);
        // validate role
        if(!validationUtil.validateRole(bearerToken, RoleConfig.REQUIRED_ROLES_FOR_ADMIN_OPERATIONS)) {
            throw new NimbleException(NimbleExceptionMessageCode.UNAUTHORIZED_INDEX_CATALOGUES.toString());
        }

        catalogueIndexLoader.indexCatalogues(partyId);
        return null;
    }

    @CrossOrigin(origins = {"*"})
    @ApiOperation(value = "", notes = "Deletes invalid catalogue lines from the index. Invalid catalogue lines are the ones which do not exist in the database")
    @ApiResponses(value = {
            @ApiResponse(code = 401, message = "No user exists for the given token")
    })
    @RequestMapping(value = "/admin/index-catalogueline",
            produces = {"application/json"},
            method = RequestMethod.DELETE)
    public ResponseEntity deleteInvalidLinesFromIndex(@ApiParam(value = "The Bearer token provided by the identity service", required = true) @RequestHeader(value = "Authorization", required = true) String bearerToken) {
        // set request log of ExecutionContext
        String requestLog = "Incoming request to delete invalid lines from index";
        executionContext.setRequestLog(requestLog);

        logger.info(requestLog);
        // validate role
        if(!validationUtil.validateRole(bearerToken, RoleConfig.REQUIRED_ROLES_FOR_ADMIN_OPERATIONS)) {
            throw new NimbleException(NimbleExceptionMessageCode.UNAUTHORIZED_DELETE_INVALID_PRODUCTS.toString());
        }

        // query to retrieve hjids of indexed catalogue lines
        String query = "{\"facet\": {\"field\": [ \"uri\"],\"limit\": -1,\"minCount\": 1},\"q\": \"*\",\"rows\": 0,\"sort\": [],\"start\": 0}";

        Response response;
        try {
            response = SpringBridge.getInstance().getiIndexingServiceClient().searchItem(query);

            if(response.status() == 200){
                JSONObject object = new JSONObject(IOUtils.toString(response.body().asInputStream()));
                JSONObject facets = (JSONObject) object.get("facets");
                JSONArray array = (JSONArray) ((org.json.JSONObject) facets.get("id")).get("entry");

                int size = array.length();
                for(int i = 0; i < size ; i++){
                    String label = (String) ((JSONObject)array.get(i)).get("label");
                    Long hjid = Long.parseLong(label);
                    // check the existence of catalogue line
                    if(!CatalogueLinePersistenceUtil.checkCatalogueLineExistence(hjid)){
                        // if there is no catalogue line with this hjid in the database, remove it from the index
                        itemIndexClient.deleteCatalogueLine(hjid);
                    }
                }
                logger.info("Deleted invalid lines from index successfully");
            }
            else{
                logger.error("Failed to delete invalid lines from index. indexing call status: {}, message: {}", response.status(), IOUtils.toString(response.body().asInputStream()));
            }
        } catch (Exception e) {
            logger.error("Failed to delete invalid lines from index",e);
        }

        return null;
    }

    @Autowired
    private VatMigrationUtility vatMigrationUtility;

    @CrossOrigin(origins = {"*"})
    @ApiOperation(value = "", notes = "Creates VAT rates for products that do not have it")
    @ApiResponses(value = {
            @ApiResponse(code = 401, message = "No user exists for the given token")
    })
    @RequestMapping(value = "/admin/create-vats",
            method = RequestMethod.POST)
    public ResponseEntity createVats(@ApiParam(value = "The Bearer token provided by the identity service", required = true) @RequestHeader(value = "Authorization", required = true) String bearerToken) {
        // set request log of ExecutionContext
        String requestLog ="Incoming request for VAT migration";
        executionContext.setRequestLog(requestLog);

        logger.info(requestLog);
        // validate role
        if(!validationUtil.validateRole(bearerToken, RoleConfig.REQUIRED_ROLES_FOR_ADMIN_OPERATIONS)) {
            throw new NimbleException(NimbleExceptionMessageCode.UNAUTHORIZED_CREATE_VAT_FOR_PRODUCTS.toString());
        }

        try {
            vatMigrationUtility.createVatsForExistingPrdocuts();
            logger.info("Completed VAT migration request");

        } catch (Exception e) {
            logger.error("Unexpected error while creating VATs", e);
        }
        return null;
    }
}
