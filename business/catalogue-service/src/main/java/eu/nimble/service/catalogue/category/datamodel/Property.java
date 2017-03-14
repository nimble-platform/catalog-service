package eu.nimble.service.catalogue.category.datamodel;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by suat on 03-Mar-17.
 */
public class Property {
    private String id;
    private String preferredName;
    private String shortName;
    private String definition;
    private String note;
    private String remark;
    private String preferredSymbol;
    private Unit unit;
    private String iecCategory;
    private String attributeType;
    private String dataType;
    private List<KeywordSynonym> synonyms;
    private List<Value> values = new ArrayList<>();

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

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
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
}
