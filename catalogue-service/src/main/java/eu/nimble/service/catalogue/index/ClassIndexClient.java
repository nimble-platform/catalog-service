package eu.nimble.service.catalogue.index;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import com.mashape.unirest.request.HttpRequest;
import eu.nimble.service.catalogue.category.IndexCategoryService;
import eu.nimble.service.catalogue.category.TaxonomyEnum;
import eu.nimble.service.catalogue.model.category.Category;
import eu.nimble.service.catalogue.model.category.Property;
import eu.nimble.service.catalogue.util.CredentialsUtil;
import eu.nimble.service.model.solr.Search;
import eu.nimble.service.model.solr.SearchResult;
import eu.nimble.service.model.solr.owl.ClassType;
import eu.nimble.service.model.solr.owl.PropertyType;
import eu.nimble.utility.JsonSerializationUtility;
import io.swagger.models.auth.In;
import jena.query;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
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
    private CredentialsUtil credentialsUtil;
    @Autowired
    private PropertyIndexClient propertyIndexClient;

    public void indexCategory(Category category, Set<String> directParentUris, Set<String> allParentUris, Set<String> directChildrenUris, Set<String> allChildrenUris) {
        try {
            String categoryJson;
            try {
                ClassType indexCategory = IndexingWrapper.toIndexCategory(category, directParentUris, allParentUris, directChildrenUris, allChildrenUris);
                categoryJson = JsonSerializationUtility.getObjectMapper().writeValueAsString(indexCategory);

            } catch (Exception e) {
                String serializedCategory = JsonSerializationUtility.serializeEntitySilently(category);
                String msg = String.format("Failed to transform Category to ClassType. \n category: %s", serializedCategory);
                logger.error(msg, e);
                return;
            }

            HttpResponse<String> response = Unirest.post(indexingUrl + "/class")
                    .header(HttpHeaders.AUTHORIZATION, credentialsUtil.getBearerToken())
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .body(categoryJson)
                    .asString();

            if (response.getStatus() == HttpStatus.OK.value()) {
                logger.info("Indexed category successfully. category uri: {}", category.getCategoryUri());
                return;

            } else {
                String msg = String.format("Failed to index category. uri: %s, indexing call status: %d, message: %s", category.getCategoryUri(), response.getStatus(), response.getBody());
                logger.error(msg);
                return;
            }

        } catch (UnirestException e) {
            String msg = String.format("Failed to index category. uri: %s", category.getCategoryUri());
            logger.error(msg, e);
            return;
        }
    }

    public ClassType getIndexCategory(String taxonomyId, String categoryId) {
        String uri = IndexCategoryService.constructUri(taxonomyId, categoryId);
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
            StringBuilder queryStr = new StringBuilder("");
            for(String uri : uris) {
                queryStr.append("id:\"").append(uri).append("\" OR ");
            }
            Search search = new Search();
            search.setRows(Integer.MAX_VALUE);
            search.setStart(0);
            search.setQuery(queryStr.substring(0, queryStr.length()-3));

            HttpRequest request;
            try {
                request = Unirest.post(indexingUrl + "/class/search")
                        .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .header(HttpHeaders.AUTHORIZATION, credentialsUtil.getBearerToken())
                        .body(JsonSerializationUtility.getObjectMapper().writeValueAsString(search))
                        .getHttpRequest();
            } catch (JsonProcessingException e) {
                String msg = String.format("Failed to get categories for uris: %s. ", uris);
                logger.error(msg);
                throw new RuntimeException(msg, e);
            }
            HttpResponse<String> response = request.asString();

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
            List<PropertyType> indexProperties = propertyIndexClient.getIndexPropertiesForCategory(uri);
//            List<PropertyType> indexProperties = propertyIndexClient.getProperties(new HashSet<>(indexCategory.getProperties()));
            List<Property> properties = IndexingWrapper.toProperties(indexProperties);
            category.setProperties(properties);
        }
        return category;
    }

    public List<Category> getCategories(Set<String> uris) {
        List<Category> categories = getCategories(uris, false);
        return categories;
    }

    public List<Category> getCategories(Set<String> uris, boolean populateProperties) {
        List<ClassType> indexCategories = getIndexCategories(uris);
        List<Category> categories = IndexingWrapper.toCategories(indexCategories);

        if(populateProperties) {
            Map<String, List<Property>> categoryProperties = propertyIndexClient.getIndexPropertiesForIndexCategories(indexCategories);
            for(Category category : categories) {
                category.setProperties(categoryProperties.get(category.getCategoryUri()));
            }
        }
        return categories;
    }

    public List<Category> getCategories(String query) {
        return getCategories(query, null);
    }

    public List<Category> getCategories(String query, Map<String, String> facetCriteria) {
        try {
            HttpRequest request = Unirest.get(indexingUrl + "/class/select")
                    .queryString("rows", Integer.MAX_VALUE)
                    .header(HttpHeaders.AUTHORIZATION, credentialsUtil.getBearerToken());
            if(query != null) {
                request = request.queryString("q", query);
            }

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
                String msg = String.format("Failed to retrieve categories. query: %s, fq: %s, call status: %d, message: %s", query, (MapUtils.isNotEmpty(facetCriteria) ? facetCriteria.toString() : ""), response.getStatus(), response.getBody());
                logger.error(msg);
                throw new RuntimeException(msg);
            }

        } catch (UnirestException e) {
            String msg = String.format("Failed to retrieve categories. query: %s, fq: %s", query, (MapUtils.isNotEmpty(facetCriteria) ? facetCriteria.toString() : ""));
            logger.error(msg, e);
            throw new RuntimeException(msg, e);
        }
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
}
