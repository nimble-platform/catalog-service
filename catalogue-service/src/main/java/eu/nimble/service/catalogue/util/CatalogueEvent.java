package eu.nimble.service.catalogue.util;


public enum CatalogueEvent {
    CATALOGUE_CREATE("catalogueCreate"), CATALOGUE_UPDATE("catalogueUpdate"), CATALOGUE_DELETE("catalogueDelete");

    private String activity;

    CatalogueEvent(String activity){
        this.activity = activity;
    }

    public String getActivity(){
        return activity;
    }
}
