package eu.nimble.service.catalogue.category;

import eu.nimble.service.catalogue.model.category.Category;
import eu.nimble.service.catalogue.model.category.CategoryTreeResponse;

import java.util.*;

/**
 * Created by suat on 07-Jul-17.
 */
public class CategoryServiceManager {
    private static CategoryServiceManager instance;

    private Map<String, ProductCategoryService> services = new LinkedHashMap<>();

    private CategoryServiceManager() {
        ServiceLoader<ProductCategoryService> loader
                = ServiceLoader.load(ProductCategoryService.class);

        for (ProductCategoryService cp : loader) {
            services.put(cp.getTaxonomyId(), cp);
        }
    }

    public static CategoryServiceManager getInstance() {
        if (instance == null) {
            instance = new CategoryServiceManager();
        }
        return instance;
    }

    public Category getCategory(String taxonomyId, String categoryId) {
        ProductCategoryService pcs = services.get(taxonomyId);
        return pcs.getCategory(categoryId);
    }

    public List<Category> getProductCategories(String categoryName) {
        List<Category> categories = new ArrayList<>();
        for(ProductCategoryService pcs : services.values()) {
            categories.addAll(pcs.getProductCategories(categoryName));
        }
        return categories;
    }

    public List<Category> getSubCategories(String taxonomyId, String categoryId) {
        ProductCategoryService pcs = services.get(taxonomyId);
        return pcs.getSubCategories(categoryId);
    }

    public CategoryTreeResponse getCategoryTree(String taxonomyId, String categoryId) {
        ProductCategoryService pcs = services.get(taxonomyId);
        return pcs.getCategoryTree(categoryId);
    }

    public List<Category> getParentCategories(String taxonomyId,String categoryId){
        ProductCategoryService pcs = services.get(taxonomyId);
        return pcs.getParentCategories(categoryId);
    }

    public List<String> getAvailableTaxonomies() {
        return new ArrayList<>(services.keySet());
    }
}