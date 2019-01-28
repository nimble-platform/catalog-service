package eu.nimble.service.catalogue.category.taxonomy;

import eu.nimble.service.catalogue.category.ProductCategoryService;
import eu.nimble.service.catalogue.model.category.Category;
import eu.nimble.service.catalogue.model.category.CategoryTreeResponse;
import eu.nimble.service.catalogue.sync.IndexingClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by suat on 24-Jan-19.
 */
@Component
public class IndexCategoryService implements ProductCategoryService {
    @Autowired
    private IndexingClient indexingClient;

    @Override
    public Category getCategory(String categoryId) {
        return null;
    }

    @Override
    public List<Category> getProductCategories(String categoryName) {
        return null;
    }

    @Override
    public List<Category> getProductCategories(String categoryName, boolean forLogistics) {
        return null;
    }

    @Override
    public CategoryTreeResponse getCategoryTree(String categoryId) {
        return null;
    }

    @Override
    public List<Category> getParentCategories(String categoryId) {
        return null;
    }

    @Override
    public List<Category> getChildrenCategories(String categoryId) {
        return null;
    }

    @Override
    public List<Category> getRootCategories() {
        return null;
    }

    public List<Category> getProductCategories(String taxonomyId, String categoryName, String lang, boolean forLogistics) {
        List<Category> results = new ArrayList<>();
        String taxonomyNamespace = getTaxonomyNamespace(taxonomyId);

        if(taxonomyId != null) {
            if(forLogistics ) {
                if (taxonomyId.contentEquals(TaxonomyEnum.eClass.getId())) {
                    results = indexingClient.getLogisticsCategoriesForEClass(categoryName, lang);
                } else if (taxonomyId.contentEquals(TaxonomyEnum.FurnitureOntology.getId())) {
                    results = indexingClient.getLogisticsCategoriesForFurnitureOntology(categoryName, lang);
                }

            } else {
                if (taxonomyId.contentEquals(TaxonomyEnum.eClass.getId())) {
                    results = indexingClient.getProductCategories(TaxonomyEnum.eClass.getNamespace(), lang, categoryName);
                } else if (taxonomyId.contentEquals(TaxonomyEnum.FurnitureOntology.getId())) {
                    results = indexingClient.getProductCategories(TaxonomyEnum.FurnitureOntology.getNamespace(), lang, categoryName);
                }
            }

        } else {
            if(forLogistics ) {
                results = indexingClient.getLogisticsCategoriesForEClass(categoryName, lang);
                results.addAll(indexingClient.getLogisticsCategoriesForFurnitureOntology(categoryName, lang));
            } else {
                results = getProductCategories(categoryName);
            }
        }
        return results;
    }

    private String getTaxonomyNamespace(String taxonomyId) {
        if(taxonomyId.compareToIgnoreCase(TaxonomyEnum.eClass.getId()) == 0) {
            return TaxonomyEnum.eClass.getNamespace();
        } else if(taxonomyId.compareToIgnoreCase(TaxonomyEnum.FurnitureOntology.getId()) == 0) {
            return TaxonomyEnum.FurnitureOntology.getNamespace();
        }
        return "";
    }
}
