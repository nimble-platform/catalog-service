package eu.nimble.service.catalogue.category;

import eu.nimble.service.catalogue.category.datamodel.Category;

import java.util.List;

/**
 * This service deals with identification of the product category to be used for associating the products / services
 * to be published onto NIMBLE.
 * Created by suat on 03-Mar-17.
 */
public interface ProductCategoryService {
    public Category getCategory(String categoryId);

    public List<Category> getProductCategories(String categoryName);

    public List<Category> getSubCategories(String categoryId);

    public String getTaxonomyId();
}
