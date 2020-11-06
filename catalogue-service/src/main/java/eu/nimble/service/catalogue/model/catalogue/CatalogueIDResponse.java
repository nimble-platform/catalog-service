package eu.nimble.service.catalogue.model.catalogue;

public class CatalogueIDResponse {
    private String uuid;
    private String id;

    public CatalogueIDResponse(String uuid, String id) {
        this.uuid = uuid;
        this.id = id;
    }

    public String getUuid() {
        return uuid;
    }

    public String getId() {
        return id;
    }
}
