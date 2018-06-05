package eu.nimble.service.catalogue.model.category;

import java.util.List;

public class CategoryTreeResponse {
    private List<Category> parents;
    private List<List<Category>> categories;

    public List<Category> getParents() {
        return parents;
    }

    public void setParents(List<Category> parents) {
        this.parents = parents;
    }

    public List<List<Category>> getCategories() {
        return categories;
    }

    public void setCategories(List<List<Category>> categories) {
        this.categories = categories;
    }
}
