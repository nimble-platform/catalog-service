package eu.nimble.service.catalogue.model.catalogue;

import eu.nimble.service.model.ubl.commonaggregatecomponents.CatalogueLineType;

import java.util.List;

public class CataloguePaginationResponse {

    private String catalogueUuid;
    private String catalogueId;
    private long size;
    private List<CatalogueLineType> catalogueLines;
    private List<String> categoryUris;
    private List<String> permittedParties;
    private List<String> restrictedParties;
    private Boolean priceHidden;

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

    public List<String> getCategoryUris() {
        return categoryUris;
    }

    public void setCategoryUris(List<String> categoryUris) {
        this.categoryUris = categoryUris;
    }

    public String getCatalogueId() {
        return catalogueId;
    }

    public void setCatalogueId(String catalogueId) {
        this.catalogueId = catalogueId;
    }

    public List<String> getPermittedParties() {
        return permittedParties;
    }

    public void setPermittedParties(List<String> permittedParties) {
        this.permittedParties = permittedParties;
    }

    public List<String> getRestrictedParties() {
        return restrictedParties;
    }

    public void setRestrictedParties(List<String> restrictedParties) {
        this.restrictedParties = restrictedParties;
    }

    public Boolean getPriceHidden() {
        return priceHidden;
    }

    public void setPriceHidden(Boolean priceHidden) {
        this.priceHidden = priceHidden;
    }
}