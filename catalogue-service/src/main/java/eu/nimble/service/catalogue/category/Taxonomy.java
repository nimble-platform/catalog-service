package eu.nimble.service.catalogue.category;

public class Taxonomy {

    private String id;
    private String namespace;

    public Taxonomy() {
    }

    public Taxonomy(String id, String namespace) {
        this.id = id;
        this.namespace = namespace;
    }

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

}
