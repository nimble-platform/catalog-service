package eu.nimble.service.catalogue.category;

import java.util.List;

public class Taxonomy {

    private String id;
    private String namespace;
    private List<String> serviceRootCategories;
    private List<String> productRootCategories;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getNamespace() {
        return namespace;
    }

    public void setNamespace(String namespace) {
        this.namespace = namespace;
    }

    public List<String> getServiceRootCategories() {
        return serviceRootCategories;
    }

    public void setServiceRootCategories(List<String> serviceRootCategories) {
        this.serviceRootCategories = serviceRootCategories;
    }

    public List<String> getProductRootCategories() {
        return this.productRootCategories;
    }

    public void setProductRootCategories(final List<String> productRootCategories) {
        this.productRootCategories = productRootCategories;
    }
}
