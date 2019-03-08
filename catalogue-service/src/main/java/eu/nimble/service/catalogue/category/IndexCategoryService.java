package eu.nimble.service.catalogue.category;

import com.fasterxml.jackson.core.type.TypeReference;
import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import com.mashape.unirest.request.HttpRequest;
import eu.nimble.service.catalogue.index.ClassIndexClient;
import eu.nimble.service.catalogue.index.IndexingWrapper;
import eu.nimble.service.catalogue.model.category.Category;
import eu.nimble.service.catalogue.model.category.CategoryTreeResponse;
import eu.nimble.service.catalogue.util.ExecutionContext;
import eu.nimble.service.model.solr.SearchResult;
import eu.nimble.service.model.solr.owl.ClassType;
import eu.nimble.service.model.solr.owl.IClassType;
import eu.nimble.service.model.solr.owl.IConcept;
import eu.nimble.utility.JsonSerializationUtility;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.util.*;

/**
 * Created by suat on 24-Jan-19.
 */
@Component
public class IndexCategoryService {
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
    @Autowired
    private List<TaxonomyQueryInterface> taxonomyQueries;
    private Map<String, TaxonomyQueryInterface> taxonomyQueryMap;

    @PostConstruct
    private void initialize() {
        taxonomyQueryMap = new HashMap<>();
        taxonomyQueries.stream()
                .forEach(taxonomyQueryInterface -> taxonomyQueryMap.put(taxonomyQueryInterface.getTaxonomy().getId(), taxonomyQueryInterface));
    }

    public Category getCategory(String taxonomyId, String categoryId) {
        String categoryUri = constructUri(taxonomyId, categoryId);
        return getCategory(categoryUri);
    }

    public List<Category> getCategories(List<String> taxonomyIds, List<String> categoryIds) {
        Set<String> categoryUris = new HashSet<>();
        for(int i = 0; i<taxonomyIds.size(); i++) {
            categoryUris.add(constructUri(taxonomyIds.get(i), categoryIds.get(i)));
        }
        List<Category> categories = classIndexClient.getCategories(categoryUris, true);
        return categories;
    }

    public Category getCategory(String categoryUri) {
        Category category = classIndexClient.getCategory(categoryUri);
        return category;
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
        // category uri -> children category list map
        Map<String, List<String>> childrenMap = new HashMap<>();
        Set<String> categorysToFetch = new HashSet<>();
        for(int i = 0; i<parentIndexCategories.size(); i++) {
            ClassType parentCategory = parentIndexCategories.get(i);
            Collection<String> childrenUris = parentCategory.getChildren();

            // parent has children
            if(!CollectionUtils.isEmpty(childrenUris)) {
                childrenMap.put(parentCategory.getUri(), new ArrayList<>(childrenUris));
                categorysToFetch.addAll(childrenUris);
            }
        }

        if(!CollectionUtils.isEmpty(categorysToFetch)) {
            List<ClassType> children = classIndexClient.getIndexCategories(categorysToFetch);
            List<Category> childrenCategoryList = IndexingWrapper.toCategories(children);
            // collect the categories in a map for easy access
            Map<String, Category> childrenCategoryMap = new HashMap<>();
            for(Category cat : childrenCategoryList) {
                childrenCategoryMap.put(cat.getCategoryUri(), cat);
            }

            // populate the children lists for each parent category
            for(ClassType parentCategory : parentIndexCategories) {
                List<Category> childrenCategories = new ArrayList<>();
                if(childrenMap.get(parentCategory.getUri()) != null) {
                    for (String childrenCategoryUri : childrenMap.get(parentCategory.getUri())) {
                        childrenCategories.add(childrenCategoryMap.get(childrenCategoryUri));
                    }
                    childenListsList.add(childrenCategories);
                }
            }
        }

        // populate the response
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

    public List<Category> getProductCategories(String categoryName) {
        List<Category> categories = classIndexClient.getCategories(categoryName);
        getProductCategories(categoryName, null, false);
        return categories;
    }

    public List<Category> getProductCategories(String categoryName, String taxonomyId, boolean forLogistics) {
        List<Category> results;
        String query = constructQuery(categoryName, taxonomyId, forLogistics);
        results = getProductCategoriesWithConstructedQuery(query);

        return results;
    }

    private String constructQuery(String categoryName, String taxonomyId, boolean forLogistics) {
        String query;
        if(StringUtils.isNotEmpty(categoryName)) {
            query = IConcept.TEXT_FIELD + ":" + categoryName;
        } else {
            query = IConcept.TEXT_FIELD + ":*";
        }

        if(query != null) {
            query += " AND";
        }
        query += "(";
        // get criteria for a specific taxonomy
        if(taxonomyId != null) {
            query += taxonomyQueryMap.get(taxonomyId).getQuery(forLogistics);

            // get criteria for all taxonomies
        } else {
            for (TaxonomyQueryInterface taxonomyQuery : taxonomyQueryMap.values()) {
                query += " " + taxonomyQuery.getQuery(forLogistics) + " OR";
            }

            query = query.substring(0, query.length() - 2);
        }
        query += ")";
        return query;
    }

    private List<Category> getProductCategoriesWithConstructedQuery(String query) {
        try {
            HttpRequest request = Unirest.get(indexingUrl + "/class/select");
            request = request
                    .queryString("q", query)
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

                    logger.info("Retrieved {} categories successfully. query: {}", results.size(), query);
                    return results;

                } catch (IOException e) {
                    logger.error("Failed to parse SearchResults with query: {}, serialized result: {}", query, response.getBody(), e);
                }

            } else {
                logger.error("Failed to retrieve categories. query: {}, indexing call status: {}, message: {}",
                        query, response.getStatus(), response.getBody());
            }

        } catch (UnirestException e) {
            logger.error("Failed to retrieve categories. query: {}", query, e);
        }

        return new ArrayList<>();
    }

    public static String constructUri(String taxonomyId, String id) {
        for(TaxonomyEnum taxonomyEnum : TaxonomyEnum.values()) {
            if(id.startsWith(taxonomyEnum.getNamespace())) {
                return id;
            }
        }
        if (taxonomyId.contentEquals(TaxonomyEnum.eClass.getId())) {
            return TaxonomyEnum.eClass.getNamespace() + id;
        }
        return null;
    }

    //    public List<Category> getLogisticsCategoriesForEClass(String name) {
//        Map<String, String> facetCriteria = new HashMap<>();
//        facetCriteria.put(IConcept.NAME_SPACE_FIELD, TaxonomyEnum.eClass.getNamespace());
//        facetCriteria.put(IConcept.CODE_FIELD, "14*");
//        facetCriteria.put(IClassType.LEVEL_FIELD, "4");
//        String query = StringUtils.isNotEmpty(name) ? name : "*" ;
//
//        List<Category> categories = classIndexClient.getCategories(query, facetCriteria);
//        return categories;
//
////        SolrQuery query = new SolrQuery();
////        query.set("q", queryField);
////
////        List<Category> categories = new ArrayList<>();
////        try {
////            QueryResponse response = solrClient.query(query);
////            List<ClassType> indexCategories = response.getBeans(ClassType.class);
////            for (ClassType indexCategory : indexCategories) {
////                categories.add(IndexingWrapper.toCategory(indexCategory));
////            }
////
////            // populate properties
////            populateCategoryProperties(indexCategories, categories);
////
////            logger.info("Retrieved {} eClass logistics categories. name: {}, lang: {}", categories.size(), name, lang);
////            return categories;
////
////        } catch (SolrServerException | IOException e) {
////            logger.error("Failed to retrieve eClass logistics categories for query: {}", query, e);
////        }
////
////        return new ArrayList<>();
//    }
}
