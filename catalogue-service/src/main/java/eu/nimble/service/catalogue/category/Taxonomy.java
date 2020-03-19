package eu.nimble.service.catalogue.category;

import java.util.List;

public class Taxonomy {

    private String id;
    private String namespace;
    private List<String> serviceRootCategories;

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
}
