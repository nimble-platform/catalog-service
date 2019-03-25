package eu.nimble.service.catalogue.model.category;

import eu.nimble.service.model.ubl.commonbasiccomponents.TextType;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by suat on 03-Mar-17.
 */
// TODO this class should be eliminated when the front-end uses the indexing service directly to retrieve category details
public class Category {
    private String id;
    private List<TextType> preferredName = new ArrayList<>();
    private String code;
    private int level;
    private List<TextType> definition = new ArrayList<>();
    private String note;
    private String remark;
    private List<KeywordSynonym> keywords;
    private List<Property> properties;
    private String taxonomyId;
    private String categoryUri;

    public void addPreferredName(String name, String language) {
        TextType prefName = new TextType();
        prefName.setLanguageID(language);
        prefName.setValue(name);
        preferredName.add(prefName);
    }

    public String getPreferredName(String language) {
        if(language == null) {
            language = "en";
        }

        String englishValue = null;

        for(TextType pNames: preferredName) {
            if(pNames.getLanguageID().equals(language))
                return pNames.getValue();
            else if(pNames.getLanguageID().equals("en"))
                englishValue = pNames.getValue();
        }
        // if we have a english value,return it
        if(englishValue != null){
            return englishValue;
        }
        return null;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public List<TextType> getPreferredName() {
        return preferredName;
    }

    public void setPreferredName(List<TextType> preferredName) {
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

    public List<TextType> getDefinition() {
        return definition;
    }

    public void setDefinition(List<TextType> definition) {
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
