package eu.nimble.service.catalogue.category.datamodel;

import java.util.List;

/**
 * Created by suat on 03-Mar-17.
 */
public class Category {
    private String id;
    private String preferredName;
    private String code;
    private int level;
    private String definition;
    private String note;
    private String remark;
    private List<KeywordSynonym> keywords;
    private List<Property> properties;
    private String taxonomyId;
    private String categoryUri;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getPreferredName() {
        return preferredName;
    }

    public void setPreferredName(String preferredName) {
        this.preferredName = preferredName;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public int getLevel() {
        return level;
    }

    public void setLevel(int level) {
        this.level = level;
    }

    public String getDefinition() {
        return definition;
    }

    public void setDefinition(String definition) {
        this.definition = definition;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }

    public List<Property> getProperties() {
        return properties;
    }

    public void setProperties(List<Property> properties) {
        this.properties = properties;
    }

    public String getTaxonomyId() {
        return taxonomyId;
    }

    public void setTaxonomyId(String taxonomyId) {
        this.taxonomyId = taxonomyId;
    }

    public String getCategoryUri() {
        return categoryUri;
    }

    public void setCategoryUri(String categoryUri) {
        this.categoryUri = categoryUri;
    }
}
