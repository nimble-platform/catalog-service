package eu.nimble.service.catalogue.impl;

import eu.nimble.service.catalogue.CatalogueService;
import eu.nimble.service.catalogue.ProductCategoryService;
import eu.nimble.service.catalogue.category.datamodel.Category;
import eu.nimble.service.catalogue.exception.CategoryDatabaseException;
import eu.nimble.service.catalogue.exception.ProductCategoryServiceException;
import eu.nimble.service.catalogue.impl.database.EClassCategoryDatabaseAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Created by suat on 03-Mar-17.
 */
public class ProductCategoryServiceImpl implements ProductCategoryService {
    private static final Logger logger = LoggerFactory.getLogger(ProductCategoryServiceImpl.class);
    private static ProductCategoryServiceImpl instance = null;

    private ProductCategoryServiceImpl() {
    }

    public static ProductCategoryServiceImpl getInstance() {
        if (instance == null) {
            return new ProductCategoryServiceImpl();
        } else {
            return instance;
        }
    }

    @Override
    public Category getCategory(String categoryId) {
        EClassCategoryDatabaseAdapter eClassCategoryDatabaseAdapter = new EClassCategoryDatabaseAdapter();
        Category category;
        try {
            category = eClassCategoryDatabaseAdapter.getCategoryById(categoryId);
            category.setProperties(eClassCategoryDatabaseAdapter.getPropertiesForCategory(categoryId));
        } catch (CategoryDatabaseException e) {
            throw new ProductCategoryServiceException("Failed to retrieve product category", e);
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
            throw new ProductCategoryServiceException("Failed to retrieve product categories", e);
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
            throw new ProductCategoryServiceException("Failed to retrieve product sub-categories", e);
        }
        return categories;
    }
}
