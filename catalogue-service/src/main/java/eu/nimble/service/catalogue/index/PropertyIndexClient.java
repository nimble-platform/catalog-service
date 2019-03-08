package eu.nimble.service.catalogue.index;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import com.mashape.unirest.request.HttpRequest;
import eu.nimble.service.catalogue.model.category.Property;
import eu.nimble.service.catalogue.util.CredentialsUtil;
import eu.nimble.service.model.solr.SearchResult;
import eu.nimble.service.model.solr.owl.ClassType;
import eu.nimble.service.model.solr.owl.PropertyType;
import eu.nimble.utility.JsonSerializationUtility;
import jena.query;
import org.apache.commons.collections.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.*;
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
    private CredentialsUtil credentialsUtil;

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
                    .header(HttpHeaders.AUTHORIZATION, credentialsUtil.getBearerToken())
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

    public List<PropertyType> getProperties(Set<String> uris) {
        HttpResponse<String> response;
        try {
            response = Unirest.get(indexingUrl + "/properties")
                    .queryString("uri", uris)
                    .header(HttpHeaders.AUTHORIZATION, credentialsUtil.getBearerToken())
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

    public List<PropertyType> getIndexPropertiesForCategory(String categoryUri) {
        return getIndexPropertiesForCategories(Arrays.asList(categoryUri));
    }

    public List<PropertyType> getIndexPropertiesForCategories(List<String> categoryUris) {
        try {
            HttpRequest request = Unirest.get(indexingUrl + "/properties")
                    .header(HttpHeaders.AUTHORIZATION, credentialsUtil.getBearerToken());
            for(String categoryUri : categoryUris) {
                request = request.queryString("class", categoryUri);
            }
            HttpResponse<String> response = request.asString();

            if (response.getStatus() == HttpStatus.OK.value()) {
                List<PropertyType> properties = extractIndexPropertiesFromSearchResults(response, categoryUris.toString());
                logger.info("Retrieved properties for categories: {}", categoryUris);
                return properties;

            } else {
                String msg = String.format("Failed to retrieve properties for categories: %s, indexing call status: %d, message: %s", categoryUris, response.getStatus(), response.getBody());
                logger.error(msg);
                throw new RuntimeException(msg);
            }

        } catch (UnirestException e) {
            String msg = String.format("Failed to retrieve properties for categories: %s", categoryUris);
            logger.error(msg, e);
            throw new RuntimeException(msg, e);
        }
    }

    public Map<String, List<Property>> getIndexPropertiesForIndexCategories(List<ClassType> indexCategories) {
        Map<String, Collection<String>> categoryPropertyMap = new HashMap<>();
        // aggregate the properties to be fetched and keep the mapping between categories and properties
        for(ClassType indexCategory : indexCategories) {
            Collection<String> categoryProperties = indexCategory.getProperties();
            if(categoryProperties != null) {
                categoryPropertyMap.put(indexCategory.getUri(), categoryProperties);
            }
        }

        // fetch properties for bottom-most categories
        List<String> bottomMostCategories = getBottomMostCategories(indexCategories);
        List<PropertyType> indexProperties = getIndexPropertiesForCategories(bottomMostCategories);
        List<Property> properties = IndexingWrapper.toProperties(indexProperties);

        // put properties into a map for easy access
        Map<String, Property> propertyMap = new HashMap<>(); // property uri -> property map
        for(Property property : properties) {
            propertyMap.put(property.getUri(), property);
        }

        // populate the category->property map
        Map<String, List<Property>> categoryProperties = new HashMap<>();
        for(ClassType indexCategory : indexCategories) {
            String categoryUri = indexCategory.getUri();
            Collection<String> catPropCol = categoryPropertyMap.get(categoryUri);
            List<Property> catPropList = new ArrayList<>();
            if(CollectionUtils.isNotEmpty(catPropCol)) {
                for (String propUri : catPropCol) {
                    // skip the object properties that are not retrieved from the index
                    if(propertyMap.get(propUri) != null) {
                        catPropList.add(propertyMap.get(propUri));
                    }
                }
            }
            categoryProperties.put(categoryUri, catPropList);
        }
        return categoryProperties;
    }

    /**
     * Identifies only the bottom-most categories among the given list
     * @param indexCategories
     * @return
     */
    private List<String> getBottomMostCategories(List<ClassType> indexCategories) {
        List<String> remainingCategories = new ArrayList<>();
        for(ClassType category : indexCategories) {
            boolean isParent = false;
            for(ClassType category2 : indexCategories) {
                // if category2 is included in the children of the category, then, category is a parent
                if(category.getAllChildren() != null && category.getAllChildren().contains(category2.getUri())) {
                    isParent = true;
                    break;
                }
            }
            if(!isParent) {
                remainingCategories.add(category.getUri());
            }
        }
        return remainingCategories;
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
                    .filter(indexProperty -> indexProperty.getPropertyType().contentEquals("DatatypeProperty") || indexProperty.getPropertyType().contentEquals("FunctionalProperty") )
                    .collect(Collectors.toList());
            return indexProperties;

        } catch (IOException e) {
            String msg = String.format("Failed to parse SearchResult while getting properties. query: %s, serialized results: %s", query, response.getBody());
            logger.error(msg, e);
            throw new RuntimeException(msg, e);
        }
    }
}
