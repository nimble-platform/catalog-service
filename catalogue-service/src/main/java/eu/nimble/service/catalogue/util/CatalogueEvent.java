package eu.nimble.service.catalogue.util;


public enum CatalogueEvent {
    CATALOGUE_CREATE("catalogueCreate"), CATALOGUE_UPDATE("catalogueUpdate"), CATALOGUE_DELETE("catalogueDelete"),
    PRODUCT_PUBLISH("productPublish"), PRODUCT_UPDATE("productUpdate"), PRODUCT_DELETE("productDelete");

    private String activity;

    CatalogueEvent(String activity){
        this.activity = activity;
    }

    public String getActivity(){
        return activity;
    }
}
