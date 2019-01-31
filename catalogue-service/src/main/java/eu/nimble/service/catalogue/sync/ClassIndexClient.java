package eu.nimble.service.catalogue.sync;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import com.mashape.unirest.request.HttpRequest;
import eu.nimble.service.catalogue.category.TaxonomyEnum;
import eu.nimble.service.catalogue.model.category.Category;
import eu.nimble.service.catalogue.model.category.Property;
import eu.nimble.service.catalogue.util.ExecutionContext;
import eu.nimble.service.model.solr.SearchResult;
import eu.nimble.service.model.solr.owl.ClassType;
import eu.nimble.service.model.solr.owl.PropertyType;
import eu.nimble.utility.JsonSerializationUtility;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Created by suat on 28-Jan-19.
 */
@Component
public class ClassIndexClient {
    private static final Logger logger = LoggerFactory.getLogger(ClassIndexClient.class);

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

    public ClassType getIndexCategory(String taxonomyId, String categoryId) {
        String uri = constructUri(taxonomyId, categoryId);
        return getIndexCategory(uri);
    }

    public ClassType getIndexCategory(String uri) {
        Set<String> paramWrap = new HashSet<>();
        paramWrap.add(uri);
        List<ClassType> categories = getIndexCategories(paramWrap);
        ClassType indexCategory = categories.size() > 0 ? categories.get(0) : null;
        return indexCategory;
    }

    public List<ClassType> getIndexCategories(Set<String> uris) {
        try {
            HttpResponse<String> response;
            response = Unirest.get(indexingUrl + "/classes")
                    .queryString("uri", uris)
                    .header(HttpHeaders.AUTHORIZATION, executionContext.getBearerToken())
                    .asString();

            if (response.getStatus() == HttpStatus.OK.value()) {
                List<ClassType> indexCategories = extractIndexCategoriesFromSearchResults(response, uris.toString());
                return indexCategories;

            } else {
                String msg = String.format("Failed to get categories for uris: %s, indexing call status: %d, message: %s", uris, response.getStatus(), response.getBody());
                logger.error(msg);
                throw new RuntimeException(msg);
            }

        } catch (UnirestException e) {
            String msg = String.format("Failed to retrieve categories for uris: %s", uris);
            logger.error(msg, e);
            throw new RuntimeException(msg, e);
        }
    }

    public Category getCategory(String uri) {
        ClassType indexCategory = getIndexCategory(uri);
        Category category = IndexingWrapper.toCategory(indexCategory);

        //populate properties
        if(CollectionUtils.isNotEmpty(indexCategory.getProperties())) {
            List<PropertyType> indexProperties = getIndexPropertiesForCategory(uri);
            List<Property> properties = IndexingWrapper.toProperties(indexProperties);
            category.setProperties(properties);
        }
        return category;
    }

    public List<Category> getCategories(Set<String> uris) {
        List<ClassType> indexCategories = getIndexCategories(uris);
        List<Category> categories = IndexingWrapper.toCategories(indexCategories);
        return categories;
    }

    public List<Category> getCategories(String query) {
        return getCategories(query, null);
    }

    public List<Category> getCategories(String query, Map<String, String> facetCriteria) {
        try {
            HttpRequest request = Unirest.get(indexingUrl + "/class/select")
                    .queryString("rows", Integer.MAX_VALUE)
                    .header(HttpHeaders.AUTHORIZATION, executionContext.getBearerToken());

            if(MapUtils.isNotEmpty(facetCriteria)) {
                for(Map.Entry e : facetCriteria.entrySet()) {
                    request = request.queryString("fq", e.getKey() + ":" + e.getValue());
                }
            }

            HttpResponse<String> response = request.asString();

            if (response.getStatus() == HttpStatus.OK.value()) {
                List<ClassType> indexCategories = extractIndexCategoriesFromSearchResults(response, query);
                List<Category> categories = IndexingWrapper.toCategories(indexCategories);
                return categories;

            } else {
                String msg = String.format("Failed to retrieve categories. query: %s, call status: %d, message: %s", query, response.getStatus(), response.getBody());
                logger.error(msg);
                throw new RuntimeException(msg);
            }

        } catch (UnirestException e) {
            String msg = String.format("Failed to retrieve categories. taxonomy: %s");
            logger.error(msg, e);
            throw new RuntimeException(msg, e);
        }
    }

    public Map<String, List<PropertyType>> getIndexPropertiesForCategories(List<ClassType> indexCategories) {
        Map<String, Collection<String>> categoryPropertyMap = new HashMap<>();
        Set<String> propertyUris = new HashSet<>();
        // aggregate the properties to be fetched and keep the mapping between categories and properties
        for(ClassType indexCategory : indexCategories) {
            Collection<String> categoryProperties = indexCategory.getProperties();
            if(categoryProperties != null) {
                categoryPropertyMap.put(indexCategory.getUri(), categoryProperties);
                propertyUris.addAll(categoryProperties);
            }
        }

        // fetch properties at once
        List<PropertyType> properties = getProperties(propertyUris);
        // put properties into a map for easy access
        Map<String, PropertyType> fetchedPrpoerties = new HashMap<>();
        for(PropertyType property : properties) {
            fetchedPrpoerties.put(property.getUri(), property);
        }

        // populate the category-property map
        Map<String, List<PropertyType>> categoryProperties = new HashMap<>();
        for(Map.Entry<String, Collection<String>> mapEntry : categoryPropertyMap.entrySet()) {
            String categoryUri = mapEntry.getKey();
            Collection<String> catPropCol = mapEntry.getValue();
            List<PropertyType> catPropList = new ArrayList<>();
            for(String propUri : catPropCol) {
                catPropList.add(fetchedPrpoerties.get(propUri));
            }
            categoryProperties.put(categoryUri, catPropList);
        }
        return categoryProperties;
    }

    public List<PropertyType> getIndexPropertiesForCategory(String categoryUri) {

        try {
            HttpRequest request = Unirest.get(indexingUrl + "/properties")
                    .queryString("class", categoryUri)
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

    private static String constructUri(String taxonomyId, String id) {
        if (taxonomyId.contentEquals(TaxonomyEnum.eClass.getId())) {
            return TaxonomyEnum.eClass.getNamespace() + id;

        } else if (taxonomyId.contentEquals(TaxonomyEnum.FurnitureOntology.getId())) {
            return id;
        }
        return null;
    }

    private List<ClassType> extractIndexCategoriesFromSearchResults(HttpResponse<String> response, String query) {
        ObjectMapper mapper = JsonSerializationUtility.getObjectMapper();
        SearchResult<ClassType> searchResult;
        List<ClassType> indexCategories;
        try {
            searchResult = mapper.readValue(response.getBody(), new TypeReference<SearchResult<ClassType>>() {});
            indexCategories = searchResult.getResult();
            return indexCategories;

        } catch (IOException e) {
            String msg = String.format("Failed to parse SearchResult while getting categories. query: %s, serialized results: %s", query, response.getBody());
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
