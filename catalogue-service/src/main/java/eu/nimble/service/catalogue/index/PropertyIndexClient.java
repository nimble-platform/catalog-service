package eu.nimble.service.catalogue.index;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import com.mashape.unirest.request.HttpRequest;
import eu.nimble.service.catalogue.model.category.Property;
import eu.nimble.service.catalogue.util.ExecutionContext;
import eu.nimble.service.model.solr.SearchResult;
import eu.nimble.service.model.solr.owl.PropertyType;
import eu.nimble.utility.JsonSerializationUtility;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Created by suat on 01-Feb-19.
 */
@Component
public class PropertyIndexClient {
    private static final Logger logger = LoggerFactory.getLogger(PropertyIndexClient.class);

    @Value("${nimble.indexing.url}")
    private String indexingUrl;
    @Value("${nimble.indexing.solr.url}")
    private String solrUrl;
    @Value("${nimble.indexing.solr.username}")
    private String solrUsername;
    @Value("${nimble.indexing.solr.password}")
    private String solrPassword;

    @Autowired
    private ExecutionContext executionContext;

    public void indexProperty(Property property, Set<String> associatedCategoryUris) {
        try {
            String propertyJson;
            try {
                PropertyType indexProperty = IndexingWrapper.toIndexProperty(property, associatedCategoryUris);
                propertyJson = JsonSerializationUtility.getObjectMapper().writeValueAsString(indexProperty);

            } catch (Exception e) {
                String serializedProperty = JsonSerializationUtility.serializeEntitySilently(property);
                String msg = String.format("Failed to transform Property to PropertyType. \n property: %s", serializedProperty);
                logger.error(msg, e);
                return;
            }

            HttpResponse<String> response = Unirest.post(indexingUrl + "/property")
                    .header(HttpHeaders.AUTHORIZATION, executionContext.getBearerToken())
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .body(propertyJson)
                    .asString();

            if (response.getStatus() == HttpStatus.OK.value()) {
                logger.info("Indexed property successfully. property uri: {}", property.getUri());
                return;

            } else {
                String msg = String.format("Failed to index property. uri: %s, indexing call status: %d, message: %s", property.getUri(), response.getStatus(), response.getBody());
                logger.error(msg);
                return;
            }

        } catch (UnirestException e) {
            String msg = String.format("Failed to index property. uri: %s", property.getUri());
            logger.error(msg, e);
            return;
        }
    }

    public List<PropertyType> getIndexPropertiesForCategory(String categoryUri) {

        try {
            HttpRequest request = Unirest.get(indexingUrl + "/properties")
                    .queryString("class", "\"" + categoryUri + "\"")
                    .header(HttpHeaders.AUTHORIZATION, executionContext.getBearerToken());
            HttpResponse<String> response = request.asString();

            if (response.getStatus() == HttpStatus.OK.value()) {
                List<PropertyType> properties = extractIndexPropertiesFromSearchResults(response, categoryUri);
                logger.info("Retrieved properties for category: {}", categoryUri);
                return properties;

            } else {
                String msg = String.format("Failed to retrieve properties for category: %s, indexing call status: %d, message: %s", categoryUri, response.getStatus(), response.getBody());
                logger.error(msg);
                throw new RuntimeException(msg);
            }

        } catch (UnirestException e) {
            String msg = String.format("Failed to retrieve properties for category: %s", categoryUri);
            logger.error(msg, e);
            throw new RuntimeException(msg, e);
        }
    }

    public List<PropertyType> getProperties(Set<String> uris) {
        HttpResponse<String> response;
        try {
            response = Unirest.get(indexingUrl + "/properties")
                    .queryString("uri", uris)
                    .header(HttpHeaders.AUTHORIZATION, executionContext.getBearerToken())
                    .asString();

            if (response.getStatus() == HttpStatus.OK.value()) {
                List<PropertyType> properties = extractIndexPropertiesFromSearchResults(response, uris.toString());
                logger.info("Retrieved properties for uris: {}", uris);
                return properties;

            } else {
                String msg = String.format("Failed to retrieve properties. uris: %s, indexing call status: %d, message: %s", uris, response.getStatus(), response.getBody());
                logger.error(msg);
                throw new RuntimeException(msg);
            }

        } catch (UnirestException e) {
            String msg = String.format("Failed to retrieve properties for uris. uris: %s", uris);
            logger.error(msg, e);
            throw new RuntimeException(msg, e);
        }
    }

    private List<PropertyType> extractIndexPropertiesFromSearchResults(HttpResponse<String> response, String query) {
        ObjectMapper mapper = JsonSerializationUtility.getObjectMapper();
        SearchResult<PropertyType> searchResult;
        List<PropertyType> indexProperties;
        try {
            searchResult = mapper.readValue(response.getBody(), new TypeReference<SearchResult<PropertyType>>() {});
            indexProperties = searchResult.getResult();

            // filter properties so that only datatype properties are included
            indexProperties = indexProperties.stream()
                    .filter(indexProperty -> indexProperty.getPropertyType().contentEquals("DatatypeProperty"))
                    .collect(Collectors.toList());
            return indexProperties;

        } catch (IOException e) {
            String msg = String.format("Failed to parse SearchResult while getting properties. query: %s, serialized results: %s", query, response.getBody());
            logger.error(msg, e);
            throw new RuntimeException(msg, e);
        }
    }
}
