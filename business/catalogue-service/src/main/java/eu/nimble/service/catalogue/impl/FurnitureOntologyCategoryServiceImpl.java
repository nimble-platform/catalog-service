package eu.nimble.service.catalogue.impl;

import eu.nimble.service.catalogue.ProductCategoryService;
import eu.nimble.service.catalogue.category.datamodel.Category;
import eu.nimble.service.catalogue.category.datamodel.Property;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by suat on 07-Jul-17.
 */
public class FurnitureOntologyCategoryServiceImpl implements ProductCategoryService {
    private List<Category> categories;

    public FurnitureOntologyCategoryServiceImpl() {
        categories = new ArrayList<>();
        Category category = new Category();
        category.setTaxonomyId(getTaxonomyId());
        category.setCode("MDFBoard");
        category.setPreferredName("MDF Board");
        category.setId("MDFBoard");

        Property property = new Property();
        property.setId("Material_Composition");
        property.setPreferredName("Material Composition");
        property.setDataType("STRING");

        List<Property> properties = new ArrayList<>();
        properties.add(property);
        category.setProperties(properties);

        categories.add(category);
    }

    @Override
    public Category getCategory(String categoryId) {
        if (categoryId.equals(categories.get(0).getId())) {
            return categories.get(0);
        } else {
            return null;
        }
    }

    @Override
    public List<Category> getProductCategories(String categoryName) {
        if (categoryName.toLowerCase().contentEquals("mdf")) {
            return categories;
        } else {
            return new ArrayList<>();
        }
    }

    @Override
    public List<Category> getSubCategories(String categoryId) {
        throw new IllegalStateException("Not implemented yet");
    }

    @Override
    public String getTaxonomyId() {
        return "FurnitureOntology";
    }
}
