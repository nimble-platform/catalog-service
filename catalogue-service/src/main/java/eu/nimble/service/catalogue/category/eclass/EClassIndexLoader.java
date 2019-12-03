package eu.nimble.service.catalogue.category.eclass;

import eu.nimble.service.catalogue.category.eclass.database.EClassCategoryDatabaseAdapter;
import eu.nimble.service.catalogue.index.ClassIndexClient;
import eu.nimble.service.catalogue.index.PropertyIndexClient;
import eu.nimble.service.catalogue.model.category.Category;
import eu.nimble.service.catalogue.model.category.Property;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * Created by suat on 31-Jan-19.
 */
@Component
public class EClassIndexLoader {
    private static final Logger logger = LoggerFactory.getLogger(EClassIndexLoader.class);

    private static EClassCategoryDatabaseAdapter dbAdapter = new EClassCategoryDatabaseAdapter();

    @Autowired
    private ClassIndexClient classIndexClient;
    @Autowired
    private PropertyIndexClient propertyIndexClient;

    // indexes the given eClass categories
    // if no eClass categories are specified, indexes all eClass categories
    public void indexEClassCategories(List<String> categoryIds) throws Exception {
        // categories failed to be indexed
        List<String> categoriesFailedToIndex = new ArrayList<>();
        // categories to be indexed
        List<Category> allCategories;
        // properties of the categories to be indexed
        Map<String, List<Property>> allProperties;
        // get categories and their properties
        if(categoryIds == null || categoryIds.size() == 0){
            allCategories = dbAdapter.getAllCategories();
            logger.info("Retrieved categories");

            allProperties = dbAdapter.getAllProperties();
            logger.info("Retrieved properties");

        } else{
            // get the given categories
            allCategories = dbAdapter.getCategories(categoryIds);
            // we need to retrieve the parent categories of the given ones
            List<Category> parentCategories = new ArrayList<>();
            for (Category category : allCategories) {
                parentCategories.addAll(dbAdapter.getParentCategories(category.getId()));
            }
            allCategories.addAll(parentCategories);
            logger.info("Retrieved categories");
            // retrieve properties
            for (Category parentCategory : parentCategories) {
                categoryIds.add(parentCategory.getId());
            }
            allProperties = dbAdapter.getPropertiesForCategories(categoryIds);
            logger.info("Retrieved properties");
        }

        // create a category map for easy access
        // map keys are category codes
        Map<String, Category> allCategoriesMapWithCode = new HashMap<>();
        // map keys are category uris
        Map<String, Category> allCategoriesMapWithUri = new HashMap<>();
        for(Category category : allCategories) {
            allCategoriesMapWithCode.put(category.getCode(), category);
            allCategoriesMapWithUri.put(category.getCategoryUri(), category);
        }
        logger.info("Constructed maps");

        // set properties of categories
        for(Category category : allCategories) {
            List<Property> properties = allProperties.get(category.getId());
            category.setProperties(properties);
        }
        logger.info("Populated category properties");

        // construct category parent mapping
        ChildrenParentMaps categoryParentMapping = createCategoryParentMap(allCategories, allCategoriesMapWithCode);
        logger.info("Constructed parent category maps");

        // construct category children mapping
        ChildrenParentMaps categoryChildrenMapping = createCategoryChildrenMap(allCategories, categoryParentMapping.direct, categoryParentMapping.all);
        logger.info("Constructed children category maps");

        // index categories
        for(Category category : allCategories) {
            boolean isIndexed = classIndexClient.indexCategory(category,
                    categoryParentMapping.direct.get(category.getCategoryUri()),
                    categoryParentMapping.all.get(category.getCategoryUri()),
                    categoryChildrenMapping.direct.get(category.getCategoryUri()),
                    categoryChildrenMapping.all.get(category.getCategoryUri()));
            if(!isIndexed){
                categoriesFailedToIndex.add(category.getCategoryUri());
            }
        }
        logger.info("Completed category indexing");
        // log the ones failed to be indexed
        if(categoriesFailedToIndex.size() > 0){
            logger.error("Failed to index following categories: {}",categoriesFailedToIndex);
        }
    }

    // indexes the given eClass properties
    // if no eClass properties are specified, indexes all eClass properties
    public void indexEClassProperties(List<String> propertyIds) throws Exception {
        // the ones failed to be indexed
        List<String> propertiesFailedToIndex = new ArrayList<>();
        // properties to be indexed
        Map<String, List<Property>> allProperties;
        // retrieve the properties
        if(propertyIds == null || propertyIds.size() == 0){
            allProperties = dbAdapter.getAllProperties();
        } else{
            allProperties = dbAdapter.getProperties(propertyIds);
        }
        logger.info("Retrieved properties");

        // property -> category list
        Map<String, Set<String>> propertyCategoryMap = new HashMap<>();
        for(Map.Entry<String, List<Property>> entry : allProperties.entrySet()) {
            String categoryId = entry.getKey();
            List<Property> propertyList = entry.getValue();
            for(Property property : propertyList) {
                //
                Set<String> categoryUris = propertyCategoryMap.get(property.getUri());
                if(categoryUris == null) {
                    categoryUris = new HashSet<>();
                    propertyCategoryMap.put(property.getUri(), categoryUris);
                }
                categoryUris.add(EClassTaxonomyQueryImpl.namespace + categoryId);
            }
        }
        logger.info("Constructed property category map");

        // index properties
        List<String> indexedProperties = new ArrayList<>();
        for(List<Property> properties : allProperties.values()) {
            for(Property property : properties) {
                if(!indexedProperties.contains(property.getUri())) {
                    boolean isIndexed = propertyIndexClient.indexProperty(property, propertyCategoryMap.get(property.getUri()));
                    indexedProperties.add(property.getUri());

                    if(!isIndexed){
                        propertiesFailedToIndex.add(property.getUri());
                    }
                }
            }
        }
        logger.info("Completed property indexing");
        // log the ones failed to be indexed
        if(propertiesFailedToIndex.size() > 0){
            logger.error("Failed to index following properties: {}",propertiesFailedToIndex);
        }
    }

    public void indexEClassResources() throws Exception {
        // index categories
        indexEClassCategories(null);

        // index properties
        indexEClassProperties(null);
    }

    private static ChildrenParentMaps createCategoryParentMap(List<Category> allCategories, Map<String, Category> allCategoriesMap) {
        Map<String, Set<String>> directParentsMap = new HashMap<>();
        Map<String, Set<String>> allParentsMap = new HashMap<>();

        for(Category category : allCategories) {
            Set<String> parentCategory = new HashSet<>();
            Set<String> allParentsCategories = allParentsMap.get(category.getCategoryUri());
            if(allParentsCategories == null) {
                allParentsCategories = new HashSet<>();
                allParentsMap.put(category.getCategoryUri(), allParentsCategories);
            }
            logger.info("category: {} processed for parents", category.getCategoryUri());
            if(category.getLevel() == 1) {
                // root category do nothing
            } else if(category.getLevel() == 2) {
                String parentCategoryCode = category.getCode().substring(0, 2) + "000000";
                // direct parent
                parentCategory.add(allCategoriesMap.get(parentCategoryCode).getCategoryUri());
                // all parents for direct parent
                allParentsCategories.add(allCategoriesMap.get(parentCategoryCode).getCategoryUri());

            } else if(category.getLevel() == 3) {
                String parentCategoryCode = category.getCode().substring(0, 4) + "0000";
                // direct parent
                parentCategory.add(allCategoriesMap.get(parentCategoryCode).getCategoryUri());
                // all parents for direct parent
                allParentsCategories.add(allCategoriesMap.get(parentCategoryCode).getCategoryUri());
                // all parents for indirect parent
                parentCategoryCode = category.getCode().substring(0,2) + "000000";
                allParentsCategories.add(allCategoriesMap.get(parentCategoryCode).getCategoryUri());

            } else if(category.getLevel() == 4) {
                String parentCategoryCode = category.getCode().substring(0, 6) + "00";
                // direct parent
                parentCategory.add(allCategoriesMap.get(parentCategoryCode).getCategoryUri());
                // all parents for direct parent
                allParentsCategories.add(allCategoriesMap.get(parentCategoryCode).getCategoryUri());
                // all parents for indirect parent
                parentCategoryCode = category.getCode().substring(0,4) + "0000";
                allParentsCategories.add(allCategoriesMap.get(parentCategoryCode).getCategoryUri());
                // all parents for indirect parent
                parentCategoryCode = category.getCode().substring(0,2) + "000000";
                allParentsCategories.add(allCategoriesMap.get(parentCategoryCode).getCategoryUri());
            }
            directParentsMap.put(category.getCategoryUri(), parentCategory);
            allParentsMap.put(category.getCategoryUri(), allParentsCategories);
        }

        ChildrenParentMaps childrenParentMaps = new ChildrenParentMaps();
        childrenParentMaps.all = allParentsMap;
        childrenParentMaps.direct = directParentsMap;
        return childrenParentMaps;
    }

    private static ChildrenParentMaps createCategoryChildrenMap(List<Category> allCategories, Map<String, Set<String>> directParentsMap, Map<String, Set<String>> allParentsMap) {
        Map<String, Set<String>> directChildrenMap = new HashMap<>();
        Map<String, Set<String>> allChildrenMap = new HashMap<>();

        for(Category category : allCategories) {
            logger.info("category: {} processed for children", category.getCategoryUri());

            // add this category to its direct parent as a children
            Optional<String> parentUri = directParentsMap.get(category.getCategoryUri()).stream().findFirst();
            if(parentUri.isPresent()) {
                Set<String> directParentSet = directChildrenMap.get(parentUri.get());
                if(directParentSet == null) {
                    directParentSet = new HashSet<>();
                    directChildrenMap.put(parentUri.get(), directParentSet);
                }
                directParentSet.add(category.getCategoryUri());
            }

            // add this category to its indirect parents as a children
            Set<String> allParentUris = allParentsMap.get(category.getCategoryUri());
            if(!allParentUris.isEmpty()) {
                for (String parent : allParentUris) {
                    Set<String> children = allChildrenMap.get(parent);
                    if(children == null) {
                        children = new HashSet<>();
                        allChildrenMap.put(parent, children);
                    }
                    children.add(category.getCategoryUri());
                }
            }
        }
        ChildrenParentMaps childrenParentMaps = new ChildrenParentMaps();
        childrenParentMaps.all = allChildrenMap;
        childrenParentMaps.direct = directChildrenMap;
        return childrenParentMaps;
    }



    private static class ChildrenParentMaps {
        private Map<String, Set<String>> direct = new HashMap<>();
        private Map<String, Set<String>> all = new HashMap<>();
    }
}
