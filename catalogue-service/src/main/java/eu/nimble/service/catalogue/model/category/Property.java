package eu.nimble.service.catalogue.model.category;

import eu.nimble.service.model.ubl.commonbasiccomponents.TextType;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by suat on 03-Mar-17.
 */
public class Property {
    private String id;
    private List<TextType> preferredName = new ArrayList<>();
    private String shortName;
    private String definition;
    private String note;
    private List<TextType> remark;
    private String preferredSymbol;
    private Unit unit;
    private String iecCategory;
    private String attributeType;
    private String valueQualifier;
    private String dataType;
    private List<KeywordSynonym> synonyms;
    private List<Value> values = new ArrayList<>();
    private String uri;
    private Boolean required;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public void addPreferredName(String name, String language) {
        TextType prefName = new TextType();
        prefName.setLanguageID(language);
        prefName.setValue(name);
        preferredName.add(prefName);
    }

    public String getPreferredName(String language) {
        return getLabelForLanguage(preferredName, language);
    }

    public List<TextType> getPreferredName() {
        return preferredName;
    }

    public void setPreferredName(List<TextType> preferredName) {
        this.preferredName = preferredName;
    }

    public String getShortName() {
        return shortName;
    }

    public void setShortName(String shortName) {
        this.shortName = shortName;
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

    public Boolean getRequired() {
        return required;
    }

    public void setRequired(Boolean required) {
        this.required = required;
    }

    public List<TextType> getRemark() {
        return remark;
    }

    public String getRemark(String languageId) {
        return getLabelForLanguage(remark, languageId);
    }

    public void setRemark(List<TextType> remark) {
        this.remark = remark;
    }

    public String getPreferredSymbol() {
        return preferredSymbol;
    }

    public void setPreferredSymbol(String preferredSymbol) {
        this.preferredSymbol = preferredSymbol;
    }

    public Unit getUnit() {
        return unit;
    }

    public void setUnit(Unit unit) {
        this.unit = unit;
    }

    public String getIecCategory() {
        return iecCategory;
    }

    public void setIecCategory(String iecCategory) {
        this.iecCategory = iecCategory;
    }

    public String getAttributeType() {
        return attributeType;
    }

    public void setAttributeType(String attributeType) {
        this.attributeType = attributeType;
    }

    public String getValueQualifier() {
        return valueQualifier;
    }

    public void setValueQualifier(String valueQualifier) {
        this.valueQualifier = valueQualifier;
    }

    public String getDataType() {
        return dataType;
    }

    public void setDataType(String dataType) {
        this.dataType = dataType;
    }

    public void addSynonym(KeywordSynonym synonym) {
        synonyms.add(synonym);
    }

    public List<KeywordSynonym> getSynonyms() {
        return synonyms;
    }

    public void addValue(Value value) {
        values.add(value);
    }

    public List<Value> getValues() {
        return values;
    }

    public String getUri() {
        return uri;
    }

    public void setUri(String uri) {
        this.uri = uri;
    }

    private String getLabelForLanguage(List<TextType> labels, String languageId) {
        if(languageId == null) {
            languageId = "en";
        }
        String englishLabel = null;
        for(TextType label: labels) {
            String id = label.getLanguageID();
            if(id.equals("en")){
                englishLabel = label.getValue();
            }
            if(id.equals(languageId)) {
                return label.getValue();
            }
        }
        return englishLabel;
    }
}
