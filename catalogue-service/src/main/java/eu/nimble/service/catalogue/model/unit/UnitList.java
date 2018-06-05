package eu.nimble.service.catalogue.model.unit;

import java.util.List;

public class UnitList {
    private String unitListId;
    private List<String> units;

    public void setUnits(List<String> units) {
        this.units = units;
    }

    public List<String> getUnits() {
        return units;
    }

    public String getUnitListId() {
        return unitListId;
    }

    public void setUnitListId(String unitListId) {
        this.unitListId = unitListId;
    }
}
