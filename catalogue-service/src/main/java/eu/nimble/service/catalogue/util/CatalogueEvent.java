package eu.nimble.service.catalogue.util;


public enum CatalogueEvent {
    CATALOGUE_CREATION("catalogueCreation");

    private String activity;

    CatalogueEvent(String activity){
        this.activity = activity;
    }

    public String getActivity(){
        return activity;
    }
}
