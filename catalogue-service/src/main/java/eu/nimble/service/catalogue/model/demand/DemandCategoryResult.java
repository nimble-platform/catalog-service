package eu.nimble.service.catalogue.model.demand;

public class DemandCategoryResult {
    private String categoryUri;
    private int count;

    public DemandCategoryResult(String categoryUri, int count) {
        this.categoryUri = categoryUri;
        this.count = count;
    }

    public String getCategoryUri() {
        return categoryUri;
    }

    public void setCategoryUri(String categoryUri) {
        this.categoryUri = categoryUri;
    }

    public int getCount() {
        return count;
    }

    public void setCount(int count) {
        this.count = count;
    }
}
