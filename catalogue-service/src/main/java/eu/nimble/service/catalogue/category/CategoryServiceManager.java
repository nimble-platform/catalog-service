package eu.nimble.service.catalogue.category;

import eu.nimble.service.catalogue.model.category.Category;
import eu.nimble.service.catalogue.model.category.CategoryTreeResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * Created by suat on 07-Jul-17.
 */
@Component
public class CategoryServiceManager {
    private static CategoryServiceManager instance;

    private Map<String, ProductCategoryService> services = new LinkedHashMap<>();

    @Autowired
    public CategoryServiceManager(List<ProductCategoryService> productCategoryServices){
        for (ProductCategoryService cp : productCategoryServices) {
            String taxonomyID = cp.getTaxonomyId();
            // taxonomy id is null for custom categories
            if(taxonomyID != null){
                instance.services.put(cp.getTaxonomyId(), cp);
            }
        }
    }

    public CategoryServiceManager(){

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

    public List<Category> getProductCategories(String categoryName,String taxonomyId ,Boolean forLogistics) {
        List<Category> categories = new ArrayList<>();
        // if taxonomy id is null, then get categories for all available taxonomies
        if(taxonomyId == null){
            for(ProductCategoryService pcs : services.values()) {
                if(forLogistics == null) {
                    categories.addAll(pcs.getProductCategories(categoryName));
                } else {
                    categories.addAll(pcs.getProductCategories(categoryName, forLogistics));
                }
            }
        }
        // otherwise, get categories only for the given taxonomy id
        else {
            for(ProductCategoryService pcs : services.values()) {
                if(pcs.getTaxonomyId().equals(taxonomyId)){
                    if(forLogistics == null) {
                        categories.addAll(pcs.getProductCategories(categoryName));
                    } else {
                        categories.addAll(pcs.getProductCategories(categoryName, forLogistics));
                    }
                }
            }
        }

        return categories;
    }

    public CategoryTreeResponse getCategoryTree(String taxonomyId, String categoryId) {
        ProductCategoryService pcs = services.get(taxonomyId);
        return pcs.getCategoryTree(categoryId);
    }

    public List<Category> getParentCategories(String taxonomyId,String categoryId){
        ProductCategoryService pcs = services.get(taxonomyId);
        return pcs.getParentCategories(categoryId);
    }

    public List<Category> getChildrenCategories(String taxonomyId,String categoryId){
        ProductCategoryService pcs = services.get(taxonomyId);
        return pcs.getChildrenCategories(categoryId);
    }

    public List<Category> getRootCategories(String taxonomyId){
        ProductCategoryService pcs = services.get(taxonomyId);
        return pcs.getRootCategories();
    }

    public List<Category> getAllCategories(String taxonomyId){
        ProductCategoryService pcs = services.get(taxonomyId);
        return pcs.getAllCategories();
    }

    public List<String> getAvailableTaxonomies() {
        return new ArrayList<>(services.keySet());
    }
}