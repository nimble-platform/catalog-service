package eu.nimble.service.catalogue.category;

import eu.nimble.service.catalogue.category.taxonomy.CustomCategoryService;
import eu.nimble.service.catalogue.category.taxonomy.IndexCategoryService;
import eu.nimble.service.catalogue.category.taxonomy.TaxonomyEnum;
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

    @Autowired
    private IndexCategoryService indexCategoryService;
    @Autowired
    private CustomCategoryService customCategoryService;

    public CategoryServiceManager(){

    }

    public static CategoryServiceManager getInstance() {
        if (instance == null) {
            instance = new CategoryServiceManager();
        }
        return instance;
    }

    public Category getCategory(String taxonomyId, String categoryId) {
        ProductCategoryService pcs = getRequiredService(taxonomyId);
        // construct uri for eClass categories since they will be queried with uris starting with the namespace of the
        // eClass taxonomy
        if(taxonomyId.contentEquals(TaxonomyEnum.eClass.getId())) {
            categoryId = constructUri(taxonomyId, categoryId);
        }
        return pcs.getCategory(categoryId);
    }

    public List<Category> getProductCategories(String categoryName,String taxonomyId ,Boolean forLogistics) {
        List<Category> categories = indexCategoryService.getProductCategories(taxonomyId, categoryName, "en", forLogistics);
        return categories;
    }

    public CategoryTreeResponse getCategoryTree(String taxonomyId, String categoryId) {
        ProductCategoryService pcs = getRequiredService(taxonomyId);
        return pcs.getCategoryTree(categoryId);
    }

    public List<Category> getParentCategories(String taxonomyId,String categoryId){
        ProductCategoryService pcs = getRequiredService(taxonomyId);
        return pcs.getParentCategories(categoryId);
    }

    public List<Category> getChildrenCategories(String taxonomyId,String categoryId){
        ProductCategoryService pcs = getRequiredService(taxonomyId);
        return pcs.getChildrenCategories(categoryId);
    }

    public List<Category> getRootCategories(String taxonomyId){
        ProductCategoryService pcs = getRequiredService(taxonomyId);
        return pcs.getRootCategories();
    }

    public List<String> getAvailableTaxonomies() {
        return Arrays.asList("eClass", "FurnitureOntology");
    }

    private ProductCategoryService getRequiredService(String taxonomyId) {
        if(!(taxonomyId == null || taxonomyId.contentEquals("Custom"))) {
            return indexCategoryService;
        } else {
            return customCategoryService;
        }
    }

    private static String constructUri(String taxonomyId, String id) {
        if(taxonomyId.contentEquals(TaxonomyEnum.eClass.getId())) {
            return TaxonomyEnum.eClass.getNamespace() + id;

        } else if (taxonomyId.contentEquals(TaxonomyEnum.FurnitureOntology.getId())) {
            return id;
        }
        return null;
    }
}