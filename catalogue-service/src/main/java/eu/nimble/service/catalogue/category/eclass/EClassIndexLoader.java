package eu.nimble.service.catalogue.category.eclass;

import eu.nimble.service.catalogue.category.TaxonomyEnum;
import eu.nimble.service.catalogue.category.eclass.database.EClassCategoryDatabaseAdapter;
import eu.nimble.service.catalogue.index.ClassIndexClient;
import eu.nimble.service.catalogue.index.IndexingWrapper;
import eu.nimble.service.catalogue.index.PropertyIndexClient;
import eu.nimble.service.catalogue.model.category.Category;
import eu.nimble.service.catalogue.model.category.Property;
import eu.nimble.service.catalogue.model.category.Unit;
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

    public void indexEClassResources() throws Exception {
        List<Category> allCategories = dbAdapter.getAllCategories();
        logger.info("Retrieved categories");

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

        Map<String, List<Property>> allProperties = dbAdapter.getAllProperties();
        logger.info("Retrieved properties");

        // set properties of categories
        for(Category category : allCategories) {
            List<Property> properties = allProperties.get(category.getId());
            category.setProperties(properties);
        }
        logger.info("Populated category properties");

        // get unit property mappings
        // in the meantime, contruct also the property->categories associations
        List<String> proertiesWithUnits = dbAdapter.getPropertiesWithUnits();
        logger.info("Get properties with units");

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
                categoryUris.add(TaxonomyEnum.eClass.getNamespace() + categoryId);

                // set unit
                if(proertiesWithUnits.contains(property.getId().substring(TaxonomyEnum.eClass.getNamespace().length()))) {
                    // an empty unit object is pushed since we do not index the unit details
                    // it is just deduce that this property is a "quantity" property
                    property.setUnit(new Unit());
                }
            }
        }
        logger.info("Constructed property category map");

        // construct category parent mapping
        ChildrenParentMaps categoryParentMapping = createCategoryParentMap(allCategories, allCategoriesMapWithCode);
        logger.info("Constructed parent category maps");

        // construct category children mapping
        ChildrenParentMaps categoryChildrenMapping = createCategoryChildrenMap(allCategories, categoryParentMapping.direct, categoryParentMapping.all);
        logger.info("Constructed children category maps");

        // index categories
        int logIndex = 0;
//        for(Category category : allCategories) {
//            classIndexClient.indexCategory(category,
//                    categoryParentMapping.direct.get(category.getCategoryUri()),
//                    categoryParentMapping.all.get(category.getCategoryUri()),
//                    categoryChildrenMapping.direct.get(category.getCategoryUri()),
//                    categoryChildrenMapping.all.get(category.getCategoryUri()));
//            logIndex++;
//            if(logIndex % 1000 == 0) {
//                logger.info("{} category indexed", logIndex);
//            }
//        }
        logger.info("Completed category indexing");

        // index properties
        logIndex = 0;
        List<String> indexedProperties = new ArrayList<>();
        for(List<Property> properties : allProperties.values()) {
            for(Property property : properties) {
                if(!indexedProperties.contains(property.getUri())) {
                    propertyIndexClient.indexProperty(property, propertyCategoryMap.get(property.getUri()));
                    indexedProperties.add(property.getUri());
                    logIndex++;
                    if(logIndex % 1000 == 0) {
                        logger.info("{} property indexed", logIndex);
                    }
                }
            }
        }
        logger.info("Completed property indexing");
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
