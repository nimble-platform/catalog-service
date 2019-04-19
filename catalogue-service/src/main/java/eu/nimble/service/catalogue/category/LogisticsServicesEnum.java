package eu.nimble.service.catalogue.category;

public enum LogisticsServicesEnum {
    // TODO: replace the furniture ontology category uris with the real ones
    ROADTRANSPORT("ROADTRANSPORT", null, "http://www.aidimme.es/FurnitureSectorOntology.owl#AssemblyComplement"),
    MARITIMETRANSPORT("MARITIMETRANSPORT", "http://www.nimble-project.org/resource/eclass#0173-1#01-AAB379#014", "http://www.aidimme.es/FurnitureSectorOntology.owl#Nail"),
    AIRTRANSPORT("AIRTRANSPORT", "http://www.nimble-project.org/resource/eclass#0173-1#01-ADU384#007", "http://www.aidimme.es/FurnitureSectorOntology.owl#Nut"),
    RAILTRANSPORT("RAILTRANSPORT", "http://www.nimble-project.org/resource/eclass#0173-1#01-AAB365#013", "http://www.aidimme.es/FurnitureSectorOntology.owl#Screw"),
    WAREHOUSING("WAREHOUSING", "http://www.nimble-project.org/resource/eclass#0173-1#01-ADU628#007", "http://www.aidimme.es/FurnitureSectorOntology.owl#Washer"),
    ORDERPICKING("ORDERPICKING", "http://www.nimble-project.org/resource/eclass#0173-1#01-AKG236#013", "http://www.aidimme.es/FurnitureSectorOntology.owl#WoodenDowelPin"),
    REVERSELOGISTICS("REVERSELOGISTICS", null, "http://www.aidimme.es/FurnitureSectorOntology.owl#Handle"),
    INHOUSESERVICES("INHOUSESERVICES", null, "http://www.aidimme.es/FurnitureSectorOntology.owl#Hinge"),
    CUSTOMSMANAGEMENT("CUSTOMSMANAGEMENT", null, "http://www.aidimme.es/FurnitureSectorOntology.owl#Latch"),
    LOGISTICSCONSULTANCY("LOGISTICSCONSULTANCY", "http://www.nimble-project.org/resource/eclass#0173-1#01-BAC130#011", "http://www.aidimme.es/FurnitureSectorOntology.owl#Stay");

    private String id;
    private String eClassCategoryUri;
    private String furnitureOntologyCategoryUri;

    LogisticsServicesEnum(String id, String eClassCategoryUri, String furnitureOntologyCategoryUri) {
        this.id = id;
        this.eClassCategoryUri = eClassCategoryUri;
        this.furnitureOntologyCategoryUri = furnitureOntologyCategoryUri;
    }

    public String getId() {
        return id;
    }

    public String getEClassCategoryUri() {
        return eClassCategoryUri;
    }

    public String getFurnitureOntologyCategoryUri() {
        return furnitureOntologyCategoryUri;
    }
}
