package eu.nimble.service.catalogue.model.demand;

import java.util.List;

public class DemandFacetResponse {
    private String facetName;
    private List<DemandFacetValue> facetValues;

    public DemandFacetResponse(String facetName, List<DemandFacetValue> facetValues) {
        this.facetName = facetName;
        this.facetValues = facetValues;
    }

    public String getFacetName() {
        return facetName;
    }

    public void setFacetName(String facetName) {
        this.facetName = facetName;
    }

    public List<DemandFacetValue> getFacetValues() {
        return facetValues;
    }
}
