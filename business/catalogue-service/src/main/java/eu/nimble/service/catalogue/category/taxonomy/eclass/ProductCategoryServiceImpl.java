package eu.nimble.service.catalogue.category.taxonomy.eclass;

import eu.nimble.service.catalogue.ProductCategoryService;
import eu.nimble.service.catalogue.category.datamodel.Category;
import eu.nimble.service.catalogue.exception.CategoryDatabaseException;
import eu.nimble.service.catalogue.category.taxonomy.eclass.database.EClassCategoryDatabaseAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by suat on 03-Mar-17.
 */
public class ProductCategoryServiceImpl implements ProductCategoryService {
    private static final Logger logger = LoggerFactory.getLogger(ProductCategoryServiceImpl.class);

    @Override
    public Category getCategory(String categoryId) {
        EClassCategoryDatabaseAdapter eClassCategoryDatabaseAdapter = new EClassCategoryDatabaseAdapter();
        Category category;
        try {
            category = eClassCategoryDatabaseAdapter.getCategoryById(categoryId);
            category.setProperties(eClassCategoryDatabaseAdapter.getPropertiesForCategory(categoryId));
        } catch (CategoryDatabaseException e) {
            logger.error("Failed to retrieve product category", e);
            return null;
        }
        return category;
    }

    @Override
    public List<Category> getProductCategories(String categoryName) {
        EClassCategoryDatabaseAdapter eClassCategoryDatabaseAdapter = new EClassCategoryDatabaseAdapter();
        List<Category> categories;
        try {
            categories = eClassCategoryDatabaseAdapter.getClassificationClassesByName(categoryName);
        } catch (CategoryDatabaseException e) {
            logger.error("Failed to retrieve product categories", e);
            return new ArrayList<>();
        }
        return categories;
    }

    @Override
    public List<Category> getSubCategories(String categoryId) {
        EClassCategoryDatabaseAdapter eClassCategoryDatabaseAdapter = new EClassCategoryDatabaseAdapter();
        List<Category> categories;
        try {
            categories = eClassCategoryDatabaseAdapter.getSubCategories(categoryId);
        } catch (CategoryDatabaseException e) {
            logger.error("Failed to retrieve product sub-categories", e);
            return new ArrayList<>();
        }
        return categories;
    }

    @Override
    public String getTaxonomyId() {
        return "eClass";
    }
}
