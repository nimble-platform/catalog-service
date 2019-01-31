package eu.nimble.service.catalogue.category;

/**
 * Created by suat on 24-Jan-19.
 */
public enum TaxonomyEnum {
    eClass("eClass", "http://www.nimble-project.org/resource/eclass/"),
    FurnitureOntology("FurnitureOntology", "http://www.aidimme.es/FurnitureSectorOntology.owl#");

    private String id;
    private String namespace;

    TaxonomyEnum(String id, String namespace) {
        this.id = id;
        this.namespace = namespace;
    }

    public String getId() {
        return id;
    }

    public String getNamespace() {
        return namespace;
    }
}
