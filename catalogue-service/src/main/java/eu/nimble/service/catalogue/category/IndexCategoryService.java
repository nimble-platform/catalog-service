package eu.nimble.service.catalogue.category;

import com.fasterxml.jackson.core.type.TypeReference;
import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import com.mashape.unirest.request.HttpRequest;
import eu.nimble.service.catalogue.model.category.Category;
import eu.nimble.service.catalogue.model.category.CategoryTreeResponse;
import eu.nimble.service.catalogue.sync.ClassIndexClient;
import eu.nimble.service.catalogue.sync.IndexingWrapper;
import eu.nimble.service.catalogue.util.ExecutionContext;
import eu.nimble.service.model.solr.SearchResult;
import eu.nimble.service.model.solr.owl.ClassType;
import eu.nimble.service.model.solr.owl.IClassType;
import eu.nimble.service.model.solr.owl.IConcept;
import eu.nimble.utility.JsonSerializationUtility;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.solr.client.solrj.SolrQuery;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.*;

/**
 * Created by suat on 24-Jan-19.
 */
@Component
public class IndexCategoryService {
    private static final String FURNITURE_ONTOLOGY_LOGISTICS_SERVICE = "http://www.aidimme.es/FurnitureSectorOntology.owl#LogisticsService";

    private static final Logger logger = LoggerFactory.getLogger(IndexCategoryService.class);

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
    @Autowired
    private ClassIndexClient classIndexClient;

    public Category getCategory(String taxonomyId, String categoryId) {
        String categoryUri = constructUri(taxonomyId, categoryId);
        return getCategory(categoryUri);
    }

    public Category getCategory(String categoryUri) {
        Category category = classIndexClient.getCategory(categoryUri);
        return category;
    }

    public List<Category> getProductCategories(String categoryName) {
        List<Category> categories = classIndexClient.getCategories(categoryName);
        return categories;
    }

    public CategoryTreeResponse getCategoryTree(String taxonomyId, String categoryId) {
        String categoryUri = constructUri(taxonomyId, categoryId);
        return getCategoryTree(categoryUri);
    }

    public CategoryTreeResponse getCategoryTree(String categoryUri) {
        // get parent categories including the category specifried by the identifier
        List<ClassType> parentIndexCategories = getParentIndexCategories(categoryUri);

        // get children of parents categories
        List<List<Category>> childenListsList = new ArrayList<>();
        // first the root categories
        List<Category> rootCategories = getRootCategories(IndexingWrapper.extractTaxonomyFromUri(parentIndexCategories.get(0).getUri()).getId());
        childenListsList.add(rootCategories);

        // get all children of each parent
        for(int i = 0; i<parentIndexCategories.size(); i++) {
            Collection<String> childrenUris = parentIndexCategories.get(i).getChildren();
            // parent has children
            if(childrenUris != null) {
                List<ClassType> children = classIndexClient.getIndexCategories(new HashSet<>(parentIndexCategories.get(i).getChildren()));
                if (!CollectionUtils.isEmpty(children)) {
                    List<Category> childrenCategories = IndexingWrapper.toCategories(children);

                    // populate the response
                    childenListsList.add(childrenCategories);
                }
            }
        }

        CategoryTreeResponse categoryTreeResponse = new CategoryTreeResponse();
        List<Category> parentCategories = IndexingWrapper.toCategories(parentIndexCategories);
        categoryTreeResponse.setParents(parentCategories);
        categoryTreeResponse.setCategories(childenListsList);

        return categoryTreeResponse;
    }

    public List<Category> getParentCategories(String taxonomyId, String categoryId) {
        String categoryUri = constructUri(taxonomyId, categoryId);
        List<ClassType> parentIndexCategories = getParentIndexCategories(categoryUri);

        // transform to Catalogue category model
        List<Category> categories = new ArrayList<>();
        for(ClassType parentIndexCategory : parentIndexCategories) {
            categories.add(IndexingWrapper.toCategory(parentIndexCategory));
        }

        return categories;
    }

    private List<ClassType> getParentIndexCategories(String categoryUri) {
        // get the category itself
        ClassType indexCategory = classIndexClient.getIndexCategory(categoryUri);

        // get parent categories
        Collection<String> parentCategoryUris = indexCategory.getAllParents();
        List<ClassType> parentIndexCategories = new ArrayList<>();
        if(!CollectionUtils.isEmpty(parentCategoryUris)) {
             parentIndexCategories.addAll(classIndexClient.getIndexCategories(new HashSet<>(parentCategoryUris)));
        }
        parentIndexCategories.add(indexCategory);

        // sort the categories such that the high-level categories are at the beginning
        parentIndexCategories = sortCategoriesByLevel(parentIndexCategories);
        return parentIndexCategories;
    }

    /**
     * Sorts the given categories by the number of parents such that the ones with fewer parents are located earlier in
     * returned list
     *
     * @param categories
     * @return
     */
    private List<ClassType> sortCategoriesByLevel(List<ClassType> categories) {
        List<ClassType> sortedCategories = new ArrayList<>();
        for (int i = 0; i < categories.size(); i++) {
            int indexToInsert = 0;
            for (; indexToInsert < sortedCategories.size(); indexToInsert++) {
                // if the parent list is null, it means it is a root category
                if (sortedCategories.get(indexToInsert).getAllParents() != null) {
                    // again if the parent list is null, then it is a root category
                    if (categories.get(i).getAllParents() == null ||
                            // if the parents size of a category is relatively fewer, it is closer to root
                            categories.get(i).getAllParents().size() < sortedCategories.get(indexToInsert).getAllParents().size()) {
                        break;
                    }
                }
            }
            sortedCategories.add(indexToInsert, categories.get(i));
        }
        return sortedCategories;
    }

    public List<Category> getChildrenCategories(String uri) {
        ClassType indexCategory = classIndexClient.getIndexCategory(uri);
        if(indexCategory.getChildren() != null) {
            List<Category> categories = classIndexClient.getCategories(new HashSet<>(indexCategory.getChildren()));
            return categories;
        }
        return new ArrayList<>();
    }

    public List<Category> getChildrenCategories(String taxonomyId, String categoryId) {
        String categoryUri = constructUri(taxonomyId, categoryId);
        return getChildrenCategories(categoryUri);
    }

    public List<Category> getRootCategories(String taxonomyId) {
        Map<String, String> facetCriteria = new HashMap<>();
        facetCriteria.put(IConcept.NAME_SPACE_FIELD, "\"" + TaxonomyEnum.valueOf(taxonomyId).getNamespace() + "\"");
        facetCriteria.put("-" + IClassType.ALL_PARENTS_FIELD, "*");
        List<Category> categories = classIndexClient.getCategories("*", facetCriteria);
        return categories;

//        SolrQuery query = new SolrQuery();
//        query.set("q", queryField);
//
//        List<Category> categories = new ArrayList<>();
//        try {
//            SolrRequest<QueryResponse> req = new QueryRequest(query);
//            req.setBasicAuthCredentials(solrUsername, solrPassword);
//            QueryResponse response = req.process(solrClient);
//            List<ClassType> indexCategories = response.getBeans(ClassType.class);
//            for (ClassType indexCategory : indexCategories) {
//                categories.add(IndexingWrapper.toCategory(indexCategory));
//            }
//
//            logger.info("Retrieved {} root categories for taxonomy: {}", taxonomyId);
//            return categories;
//
//        } catch (SolrServerException | IOException e) {
//            logger.error("Failed to retrieve root categories for taxonomy: {}, query: {}", taxonomyId, query, e);
//        }
//        return new ArrayList<>();
    }


    public List<Category> getProductCategories(String categoryName, String taxonomyId, boolean forLogistics) {
        List<Category> results = new ArrayList<>();

        if(taxonomyId != null) {
            if(forLogistics ) {
                if (taxonomyId.contentEquals(TaxonomyEnum.eClass.getId())) {
                    results = getLogisticsCategoriesForEClass(categoryName);
                } else if (taxonomyId.contentEquals(TaxonomyEnum.FurnitureOntology.getId())) {
                    results = getLogisticsCategoriesForFurnitureOntology(categoryName);
                }

            } else {
                if (taxonomyId.contentEquals(TaxonomyEnum.eClass.getId())) {
                    results = getProductCategories(TaxonomyEnum.eClass.getNamespace(), categoryName);
                } else if (taxonomyId.contentEquals(TaxonomyEnum.FurnitureOntology.getId())) {
                    results = getProductCategories(TaxonomyEnum.FurnitureOntology.getNamespace(), categoryName);
                }
            }

        } else {
            if(forLogistics ) {
                results = getLogisticsCategoriesForEClass(categoryName);
                results.addAll(getLogisticsCategoriesForFurnitureOntology(categoryName));
            } else {
                results = getProductCategories(categoryName);
            }
        }
        return results;
    }

    public List<Category> getProductCategories(String namespace, String name) {
        try {
            HttpRequest request = Unirest.get(indexingUrl + "/class/select");
            if (StringUtils.isNotEmpty(namespace)) {
                request = request.queryString("nameSpace", namespace);
            }
            request = request
                    .queryString("q", name)
                    .queryString("rows", Integer.MAX_VALUE)
                    .header(HttpHeaders.AUTHORIZATION, executionContext.getBearerToken());

            HttpResponse<String> response = request.asString();
            if (response.getStatus() == HttpStatus.OK.value()) {
                try {
                    List<Category> results = new ArrayList<>();
                    SearchResult<ClassType> categories = JsonSerializationUtility.getObjectMapper().readValue(response.getBody(), new TypeReference<SearchResult<ClassType>>() {
                    });
                    for (ClassType indexCategory : categories.getResult()) {
                        results.add(IndexingWrapper.toCategory(indexCategory));
                    }

                    logger.info("Retrieved {} categories successfully. namespace: {}, name: {}", results.size(), namespace, name);
                    return results;

                } catch (IOException e) {
                    logger.error("Failed to parse SearchResults with namespace: {}, name: {}, serialized result: {}", namespace, name, response.getBody(), e);
                }

            } else {
                logger.error("Failed to retrieve categories. namespace: {}, name: {}, indexing call status: {}, message: {}",
                        namespace, name, response.getStatus(), response.getBody());
            }

        } catch (UnirestException e) {
            logger.error("Failed to retrieve categories. namespace: {}, name: {}", namespace, name, e);
        }

        return new ArrayList<>();
    }

    public List<Category> getLogisticsCategoriesForEClass(String name) {
        String labelField = null;
        if (StringUtils.isNotEmpty(name)) {
            labelField = IConcept.TEXT_FIELD + ":" + name;
        }

        String namespaceField = IConcept.NAME_SPACE_FIELD + ":" + TaxonomyEnum.eClass.getNamespace();
        String localNameField = IConcept.LOCAL_NAME_FIELD + ":14*";
        String queryField = namespaceField + " AND " + localNameField;
        if(labelField != null) {
            queryField += " AND " + labelField;
        }

        List<Category> categories = classIndexClient.getCategories(queryField);
        return categories;

//        SolrQuery query = new SolrQuery();
//        query.set("q", queryField);
//
//        List<Category> categories = new ArrayList<>();
//        try {
//            QueryResponse response = solrClient.query(query);
//            List<ClassType> indexCategories = response.getBeans(ClassType.class);
//            for (ClassType indexCategory : indexCategories) {
//                categories.add(IndexingWrapper.toCategory(indexCategory));
//            }
//
//            // populate properties
//            populateCategoryProperties(indexCategories, categories);
//
//            logger.info("Retrieved {} eClass logistics categories. name: {}, lang: {}", categories.size(), name, lang);
//            return categories;
//
//        } catch (SolrServerException | IOException e) {
//            logger.error("Failed to retrieve eClass logistics categories for query: {}", query, e);
//        }
//
//        return new ArrayList<>();
    }

    public List<Category> getLogisticsCategoriesForFurnitureOntology(String name) {
        String labelField = null;
        if (StringUtils.isNotEmpty(name)) {
            labelField = IConcept.TEXT_FIELD + ":" + name;
        }

        String namespaceField = IConcept.NAME_SPACE_FIELD + ":" + TaxonomyEnum.FurnitureOntology.getNamespace();
        String parentsField = IClassType.ALL_PARENTS_FIELD + ":" + FURNITURE_ONTOLOGY_LOGISTICS_SERVICE;
        String queryField = namespaceField + " AND " + parentsField;
        if(labelField != null) {
            queryField += " AND " + labelField;
        }

        SolrQuery query = new SolrQuery();
        query.set("q", queryField);

        List<Category> categories = classIndexClient.getCategories(queryField);
        return categories;
    }

    public String constructUri(String taxonomyId, String id) {
        if (taxonomyId.contentEquals(TaxonomyEnum.eClass.getId())) {
            return TaxonomyEnum.eClass.getNamespace() + id;

        } else if (taxonomyId.contentEquals(TaxonomyEnum.FurnitureOntology.getId())) {
            return id;
        }
        return null;
    }
}
