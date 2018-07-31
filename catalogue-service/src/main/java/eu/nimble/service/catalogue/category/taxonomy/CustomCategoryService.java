package eu.nimble.service.catalogue.category.taxonomy;

import eu.nimble.service.catalogue.category.ProductCategoryService;
import eu.nimble.service.catalogue.model.category.Category;
import eu.nimble.service.catalogue.model.category.CategoryTreeResponse;
import eu.nimble.service.model.ubl.commonbasiccomponents.TextType;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

import static eu.nimble.service.catalogue.category.taxonomy.eclass.database.EClassCategoryDatabaseConfig.COLUMN_CLASSIFICATION_CLASS_PREFERRED_NAME;

/**
 * Created by suat on 03-Apr-18.
 */
@Component
public class CustomCategoryService implements ProductCategoryService {
    private String defaultLanguage = "en";

    @Override
    public Category getCategory(String categoryId) {
        Category category = new Category();
        category.setTaxonomyId("Custom");
        category.setId(categoryId);

        category.addPreferredName(categoryId, defaultLanguage);
        category.setProperties(new ArrayList<>());
        return category;
    }

    @Override
    public List<Category> getProductCategories(String categoryName) {
        return new ArrayList<>();
    }

    @Override
    public String getTaxonomyId() {
        return "Custom";
    }

    @Override
    public CategoryTreeResponse getCategoryTree(String categoryId) {
        return new CategoryTreeResponse();
    }

    @Override
    public List<Category> getParentCategories(String categoryId) {
        return new ArrayList<>();
    }

    @Override
    public List<Category> getRootCategories() {
        return new ArrayList<>();
    }

    @Override
    public List<Category> getChildrenCategories(String categoryId) {
        return new ArrayList<>();
    }
}