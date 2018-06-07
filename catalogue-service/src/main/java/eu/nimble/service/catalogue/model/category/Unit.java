package eu.nimble.service.catalogue.model.category;

/**
 * Created by suat on 03-Mar-17.
 */
public class Unit {
    private String id;
    private String structuredName;
    private String shortName;
    private String definition;
    private String source;
    private String comment;
    private String siNotation;
    private String siName;
    private String dinNotation;
    private String eceName;
    private String eceCode;
    private String nistName;
    private String iecClassification;
    private String nameOfDedicatedQuantity;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getStructuredName() {
        return structuredName;
    }

    public void setStructuredName(String structuredName) {
        this.structuredName = structuredName;
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

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public String getSiNotation() {
        return siNotation;
    }

    public void setSiNotation(String siNotation) {
        this.siNotation = siNotation;
    }

    public String getSiName() {
        return siName;
    }

    public void setSiName(String siName) {
        this.siName = siName;
    }

    public String getDinNotation() {
        return dinNotation;
    }

    public void setDinNotation(String dinNotation) {
        this.dinNotation = dinNotation;
    }

    public String getEceName() {
        return eceName;
    }

    public void setEceName(String eceName) {
        this.eceName = eceName;
    }

    public String getEceCode() {
        return eceCode;
    }

    public void setEceCode(String eceCode) {
        this.eceCode = eceCode;
    }

    public String getNistName() {
        return nistName;
    }

    public void setNistName(String nistName) {
        this.nistName = nistName;
    }

    public String getIecClassification() {
        return iecClassification;
    }

    public void setIecClassification(String iecClassification) {
        this.iecClassification = iecClassification;
    }

    public String getNameOfDedicatedQuantity() {
        return nameOfDedicatedQuantity;
    }

    public void setNameOfDedicatedQuantity(String nameOfDedicatedQuantity) {
        this.nameOfDedicatedQuantity = nameOfDedicatedQuantity;
    }
}
