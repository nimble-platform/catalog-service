package eu.nimble.service.catalogue.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.Unirest;
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
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import springfox.documentation.annotations.ApiIgnore;

import java.io.InputStream;
import java.util.Arrays;
import java.util.List;

/**
 * Created by suat on 28-Jan-19.
 */
@ApiIgnore
@Controller
public class AdminController {

    private static final Logger logger = LoggerFactory.getLogger(AdminController.class);

    @Value("${nimble.indexing.url}")
    private String indexingUrl;
    
    @Autowired
    private PartyIndexClient partyIndexClient;
    @Autowired
    private CatalogueIndexLoader catalogueIndexLoader;

    @CrossOrigin(origins = {"*"})
    @ApiOperation(value = "", notes = "Indexes UBL properties")
    @ApiResponses(value = {
            @ApiResponse(code = 401, message = "No user exists for the given token")
    })
    @RequestMapping(value = "/admin/index/ubl-properties",
            produces = {"application/json"},
            method = RequestMethod.GET)
    public ResponseEntity indexUBLProperties(@ApiParam(value = "The Bearer token provided by the identity service", required = true) @RequestHeader(value = "Authorization", required = true) String bearerToken) throws Exception{
        // check token
        ResponseEntity tokenCheck = eu.nimble.service.catalogue.util.HttpResponseUtil.checkToken(bearerToken);
        if (tokenCheck != null) {
            return tokenCheck;
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

            HttpResponse<String> response = Unirest.post(indexingUrl + "/property")
                    .header(HttpHeaders.AUTHORIZATION, bearerToken)
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .body(propertyJson)
                    .asString();

            if (response.getStatus() == HttpStatus.OK.value()) {
                logger.info("Indexed property successfully. property uri: " + indexProperty.getUri());

            } else {
                String msg = String.format("Failed to index property. uri: %s, indexing call status: %d, message: %s", indexProperty.getUri(), response.getStatus(), response.getBody());
                logger.error(msg);
            }
        }

        inputStream.close();

        return null;
    }

    @CrossOrigin(origins = {"*"})
    @RequestMapping(value = "/admin/add-class",
            produces = {"application/json"},
            method = RequestMethod.GET)
    public ResponseEntity getDefaultCatalogue() {
//        partyIndexClient.indexParty();
        return null;
    }

    @CrossOrigin(origins = {"*"})
    @ApiOperation(value = "", notes = "Indexes all catalogues in the database")
    @ApiResponses(value = {
            @ApiResponse(code = 401, message = "No user exists for the given token")
    })
    @RequestMapping(value = "/admin/index-catalogues",
            produces = {"application/json"},
            method = RequestMethod.POST)
    public ResponseEntity indexAllCatalogues(@ApiParam(value = "The Bearer token provided by the identity service", required = true) @RequestHeader(value = "Authorization", required = true) String bearerToken) {
        // check token
        ResponseEntity tokenCheck = eu.nimble.service.catalogue.util.HttpResponseUtil.checkToken(bearerToken);
        if (tokenCheck != null) {
            return tokenCheck;
        }
        catalogueIndexLoader.indexCatalogues();
        return null;
    }
}
