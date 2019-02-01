package eu.nimble.service.catalogue.category.eclass;

import eu.nimble.service.catalogue.category.eclass.database.EClassCategoryDatabaseAdapter;
import eu.nimble.service.catalogue.model.category.Category;
import eu.nimble.service.catalogue.model.category.Property;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Created by suat on 31-Jan-19.
 */
public class EClassIndexLoader {
    private static EClassCategoryDatabaseAdapter dbAdapter = new EClassCategoryDatabaseAdapter();

    public static void main(String[] args) throws Exception {
        List<Category> allCategories = dbAdapter.getAllCategories();
        Map<String, List<Property>> allProperties = dbAdapter.getAllProperties();

        for(Category category : allCategories) {
            List<Property> properties = allProperties.get(category.getId());
            category.setProperties(properties);
        }

        // get unit property mappings
        List<String> proertiesWithUnits = dbAdapter.getPropertiesWithUnits();

        // construct category parent mapping
        Map<String, String> categoryParentMapping = createCategoryParentMap(allCategories);

        // construct category children mapping
        Map<String, Set<String>> categoryChildrenMapping = createCategoryChildrenMap(allCategories);

        // index categories
        for(Category category : allCategories) {

        }

        // index properties

    }

    private static Map<String, String> createCategoryParentMap(List<Category> allCategories) {
        Map<String, String> categoryParentMap = new HashMap<>();
        for(Category category : allCategories) {

        }
        return categoryParentMap;
    }

    private static Map<String, Set<String>> createCategoryChildrenMap(List<Category> allCategories) {
        Map<String, Set<String>> categoryChildrenMap = new HashMap<>();
        for(Category category : allCategories) {

        }
        return categoryChildrenMap;
    }
}
