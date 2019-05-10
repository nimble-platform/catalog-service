package eu.nimble.service.catalogue.model.lcpa;

import eu.nimble.service.model.ubl.commonaggregatecomponents.LCPAInputType;

/**
 * Created by suat on 29-Mar-19.
 */
public class ItemLCPAInput {
    private String catalogueLineHjid;
    private LCPAInputType lcpaInput;

    public String getCatalogueLineHjid() {
        return catalogueLineHjid;
    }

    public void setCatalogueLineHjid(String catalogueLineHjid) {
        this.catalogueLineHjid = catalogueLineHjid;
    }

    public LCPAInputType getLcpaInput() {
        return lcpaInput;
    }

    public void setLcpaInput(LCPAInputType lcpaInput) {
        this.lcpaInput = lcpaInput;
    }
}
