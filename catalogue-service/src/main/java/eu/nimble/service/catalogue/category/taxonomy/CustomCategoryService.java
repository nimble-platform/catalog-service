package eu.nimble.service.catalogue.category.taxonomy;

import eu.nimble.service.catalogue.category.ProductCategoryService;
import eu.nimble.service.catalogue.category.datamodel.Category;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by suat on 03-Apr-18.
 */
public class CustomCategoryService implements ProductCategoryService {
    @Override
    public Category getCategory(String categoryId) {
        Category category = new Category();
        category.setTaxonomyId("Custom");
        category.setId(categoryId);
        category.setPreferredName(categoryId);
        category.setProperties(new ArrayList<>());
        return category;
    }

    @Override
    public List<Category> getProductCategories(String categoryName) {
        return new ArrayList<>();
    }

    @Override
    public List<Category> getSubCategories(String categoryId) {
        return new ArrayList<>();
    }

    @Override
    public String getTaxonomyId() {
        return "Custom";
    }
}
