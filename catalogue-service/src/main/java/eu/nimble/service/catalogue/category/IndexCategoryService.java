package eu.nimble.service.catalogue.category;

import com.fasterxml.jackson.core.type.TypeReference;
import eu.nimble.service.catalogue.category.eclass.EClassTaxonomyQueryImpl;
import eu.nimble.service.catalogue.exception.InvalidCategoryException;
import eu.nimble.service.catalogue.index.ClassIndexClient;
import eu.nimble.service.catalogue.index.IndexingWrapper;
import eu.nimble.service.catalogue.model.category.Category;
import eu.nimble.service.catalogue.model.category.CategoryTreeResponse;
import eu.nimble.service.catalogue.util.CredentialsUtil;
import eu.nimble.service.catalogue.util.SpringBridge;
import eu.nimble.service.model.solr.SearchResult;
import eu.nimble.service.model.solr.owl.ClassType;
import eu.nimble.service.model.solr.owl.IClassType;
import eu.nimble.service.model.solr.owl.IConcept;
import eu.nimble.utility.JsonSerializationUtility;
import feign.Response;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.io.IOException;
import java.util.*;

/**
 * Created by suat on 24-Jan-19.
 */
@Component
public class IndexCategoryService {
    private static final Logger logger = LoggerFactory.getLogger(IndexCategoryService.class);

    // self-invocation of this class needed to make sure that calls from this class leads to an cache interception for the methods annotated with @Cacheable
    @Resource
    private IndexCategoryService indexCategoryService;
    @Autowired
    private CredentialsUtil credentialsUtil;
    @Autowired
    private ClassIndexClient classIndexClient;

    public Category getCategory(String taxonomyId, String categoryId) throws InvalidCategoryException {
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

    public Category getCategory(String categoryUri) throws InvalidCategoryException {
        Category category = classIndexClient.getCategory(categoryUri);
        return category;
    }

    public CategoryTreeResponse getCategoryTree(String taxonomyId, String categoryId) throws InvalidCategoryException {
        String categoryUri = constructUri(taxonomyId, categoryId);
        return getCategoryTree(categoryUri);
    }

    public CategoryTreeResponse getCategoryTree(String categoryUri) throws InvalidCategoryException {
        // get parent categories including the category specifried by the identifier
        List<ClassType> parentIndexCategories = getParentIndexCategories(categoryUri);
        // get children of parents categories
        List<List<Category>> childenListsList = new ArrayList<>();
        // first the root categories
        List<Category> rootCategories = indexCategoryService.getRootCategories(Collections.singletonList(IndexingWrapper.extractTaxonomyFromUri(parentIndexCategories.get(0).getUri()).getId()));
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

    public List<Category> getParentCategories(String taxonomyId, String categoryId) throws InvalidCategoryException {
        String categoryUri = constructUri(taxonomyId, categoryId);
        List<ClassType> parentIndexCategories = getParentIndexCategories(categoryUri);

        // transform to Catalogue category model
        List<Category> categories = new ArrayList<>();
        for(ClassType parentIndexCategory : parentIndexCategories) {
            categories.add(IndexingWrapper.toCategory(parentIndexCategory));
        }

        return categories;
    }

    private List<ClassType> getParentIndexCategories(String categoryUri) throws InvalidCategoryException {
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

    public List<Category> getChildrenCategories(String uri) throws InvalidCategoryException {
        ClassType indexCategory = classIndexClient.getIndexCategory(uri);
        if(indexCategory.getChildren() != null) {
            List<Category> categories = classIndexClient.getCategories(new HashSet<>(indexCategory.getChildren()));
            return categories;
        }
        return new ArrayList<>();
    }

    public List<Category> getChildrenCategories(String taxonomyId, String categoryId) throws InvalidCategoryException {
        String categoryUri = constructUri(taxonomyId, categoryId);
        return getChildrenCategories(categoryUri);
    }

    @Cacheable(value = "rootCategories")
    public List<Category> getRootCategories(List<String> taxonomyIds) {
        Map<String, String> facetCriteria = new HashMap<>();

        List<String> namespaces = new ArrayList<>();
        taxonomyIds.forEach(taxonomyId -> namespaces.add(SpringBridge.getInstance().getTaxonomyManager().getTaxonomiesMap().get(taxonomyId).getTaxonomy().getNamespace()));
        List<Category> categories = new ArrayList<>();
        for (String namespace : namespaces) {
            facetCriteria.put(IConcept.NAME_SPACE_FIELD, "\"" + namespace + "\"");
            facetCriteria.put("-" + IClassType.ALL_PARENTS_FIELD, "*");
            categories.addAll(classIndexClient.getCategories("*", facetCriteria)) ;
        }

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
            query += SpringBridge.getInstance().getTaxonomyManager().getTaxonomiesMap().get(taxonomyId).getQuery(forLogistics);

            // get criteria for all taxonomies
        } else {
            for (TaxonomyQueryInterface taxonomyQuery : SpringBridge.getInstance().getTaxonomyManager().getTaxonomiesMap().values()) {
                query += " " + taxonomyQuery.getQuery(forLogistics) + " OR";
            }

            query = query.substring(0, query.length() - 2);
        }
        query += ")";
        return query;
    }

    private List<Category> getProductCategoriesWithConstructedQuery(String query) {
        try {
            Response response = SpringBridge.getInstance().getiIndexingServiceClient().selectClass(credentialsUtil.getBearerToken(),Integer.toString(Integer.MAX_VALUE),query,null);
            if (response.status() == HttpStatus.OK.value()) {
                try {
                    List<Category> results = new ArrayList<>();
                    SearchResult<ClassType> categories = JsonSerializationUtility.getObjectMapper().readValue(response.body().asInputStream(), new TypeReference<SearchResult<ClassType>>() {
                    });
                    for (ClassType indexCategory : categories.getResult()) {
                        results.add(IndexingWrapper.toCategory(indexCategory));
                    }

                    logger.info("Retrieved {} categories successfully. query: {}", results.size(), query);
                    return results;

                } catch (IOException e) {
                    logger.error("Failed to parse SearchResults with query: {}, serialized result: {}", query, IOUtils.toString(response.body().asInputStream()), e);
                }

            } else {
                logger.error("Failed to retrieve categories. query: {}, indexing call status: {}, message: {}",
                        query, response.status(), IOUtils.toString(response.body().asInputStream()));
            }

        } catch (Exception e) {
            logger.error("Failed to retrieve categories. query: {}", query, e);
        }

        return new ArrayList<>();
    }

    public static String constructUri(String taxonomyId, String id) {
        for(TaxonomyQueryInterface taxonomyQueryInterface: SpringBridge.getInstance().getTaxonomyManager().getTaxonomiesMap().values()){
            if(id.startsWith(taxonomyQueryInterface.getTaxonomy().getNamespace())){
                return id;
            }
        }
        if (taxonomyId.contentEquals(EClassTaxonomyQueryImpl.id)) {
            return EClassTaxonomyQueryImpl.namespace + id;
        }
        return null;
    }

    public Map<String,Map<String,String>> getLogisticsRelatedServices(String taxonomyId){
        Map<String,Map<String,String>> logisticServicesCategoryUriMap = new HashMap<>();
        // check whether the given id is 'all' or not
        boolean isTaxonomyIdSpecified = taxonomyId.compareToIgnoreCase("all") != 0;

        for (TaxonomyQueryInterface taxonomyQuery : SpringBridge.getInstance().getTaxonomyManager().getTaxonomiesMap().values()) {
            if(!isTaxonomyIdSpecified){
                logisticServicesCategoryUriMap.put(taxonomyQuery.getTaxonomy().getId(),taxonomyQuery.getLogisticsServices());
            }
            else if(taxonomyQuery.getTaxonomy().getId().contentEquals(taxonomyId)){
                logisticServicesCategoryUriMap.put(taxonomyQuery.getTaxonomy().getId(),taxonomyQuery.getLogisticsServices());
            }
        }
        return logisticServicesCategoryUriMap;
    }

    //    public List<Category> getLogisticsCategoriesForEClass(String name) {
//        Map<String, String> facetCriteria = new HashMap<>();
//        facetCriteria.put(IConcept.NAME_SPACE_FIELD, EClassTaxonomyQueryImpl.namespace);
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
