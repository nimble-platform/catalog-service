package eu.nimble.service.catalogue.model.catalogue;

import eu.nimble.service.model.ubl.commonaggregatecomponents.CatalogueLineType;

import java.util.List;

public class CataloguePaginationResponse {

    private String catalogueUuid;
    private long size;
    private List<CatalogueLineType> catalogueLines;

    public CataloguePaginationResponse() {
    }

    public long getSize() {
        return size;
    }

    public void setSize(long size) {
        this.size = size;
    }

    public List<CatalogueLineType> getCatalogueLines() {
        return catalogueLines;
    }

    public void setCatalogueLines(List<CatalogueLineType> catalogueLines) {
        this.catalogueLines = catalogueLines;
    }

    public String getCatalogueUuid() {
        return catalogueUuid;
    }

    public void setCatalogueUuid(String catalogueUuid) {
        this.catalogueUuid = catalogueUuid;
    }
}