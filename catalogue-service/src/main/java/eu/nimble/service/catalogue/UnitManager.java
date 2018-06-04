package eu.nimble.service.catalogue;

import eu.nimble.service.catalogue.model.unit.UnitList;
import eu.nimble.service.model.ubl.commonbasiccomponents.QuantityType;
import eu.nimble.utility.Configuration;
import eu.nimble.utility.HibernateUtility;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class UnitManager {
    private static UnitManager instance;

    private UnitManager() {
        checkUnits();
    }

    public static UnitManager getInstance() {
        if (instance == null) {
            instance = new UnitManager();
        }
        return instance;
    }

    private void checkUnits(){
        List<String> resultSet;
        String query = "SELECT qt FROM QuantityType qt WHERE qt.unitCodeListID = 'NIMBLE_quantity' AND qt.unitCode = 'NIMBLE_flag'";
        resultSet = (List<String>) HibernateUtility.getInstance(Configuration.UBL_PERSISTENCE_UNIT_NAME)
                .loadAll(query);
        if(resultSet.size() > 0){
            return;
        }
        else{
            insertUnits();
        }
    }

    private void insertUnits(){
        QuantityType quantityType = new QuantityType();

        // insert flag
        quantityType.setUnitCode("NIMBLE_flag");
        quantityType.setUnitCodeListID("NIMBLE_quantity");
        HibernateUtility.getInstance(Configuration.UBL_PERSISTENCE_UNIT_NAME).update(quantityType);

        quantityType.setUnitCode("EUR");
        quantityType.setUnitCodeListID("currency_quantity");
        HibernateUtility.getInstance(Configuration.UBL_PERSISTENCE_UNIT_NAME).update(quantityType);

        quantityType.setUnitCode("USD");
        quantityType.setUnitCodeListID("currency_quantity");
        HibernateUtility.getInstance(Configuration.UBL_PERSISTENCE_UNIT_NAME).update(quantityType);

        quantityType.setUnitCode("working days");
        quantityType.setUnitCodeListID("time_quantity");
        HibernateUtility.getInstance(Configuration.UBL_PERSISTENCE_UNIT_NAME).update(quantityType);

        quantityType.setUnitCode("days");
        quantityType.setUnitCodeListID("time_quantity");
        HibernateUtility.getInstance(Configuration.UBL_PERSISTENCE_UNIT_NAME).update(quantityType);

        quantityType.setUnitCode("weeks");
        quantityType.setUnitCodeListID("time_quantity");
        HibernateUtility.getInstance(Configuration.UBL_PERSISTENCE_UNIT_NAME).update(quantityType);

        quantityType.setUnitCode("L");
        quantityType.setUnitCodeListID("volume_quantity");
        HibernateUtility.getInstance(Configuration.UBL_PERSISTENCE_UNIT_NAME).update(quantityType);

        quantityType.setUnitCode("g");
        quantityType.setUnitCodeListID("weight_quantity");
        HibernateUtility.getInstance(Configuration.UBL_PERSISTENCE_UNIT_NAME).update(quantityType);

        quantityType.setUnitCode("kg");
        quantityType.setUnitCodeListID("weight_quantity");
        HibernateUtility.getInstance(Configuration.UBL_PERSISTENCE_UNIT_NAME).update(quantityType);

        quantityType.setUnitCode("mm");
        quantityType.setUnitCodeListID("length_quantity");
        HibernateUtility.getInstance(Configuration.UBL_PERSISTENCE_UNIT_NAME).update(quantityType);

        quantityType.setUnitCode("cm");
        quantityType.setUnitCodeListID("length_quantity");
        HibernateUtility.getInstance(Configuration.UBL_PERSISTENCE_UNIT_NAME).update(quantityType);

        quantityType.setUnitCode("m");
        quantityType.setUnitCodeListID("length_quantity");
        HibernateUtility.getInstance(Configuration.UBL_PERSISTENCE_UNIT_NAME).update(quantityType);

        quantityType.setUnitCode("box");
        quantityType.setUnitCodeListID("package_quantity");
        HibernateUtility.getInstance(Configuration.UBL_PERSISTENCE_UNIT_NAME).update(quantityType);
    }

    public List<String> getValues(String unitListId){
        String query = "SELECT qt.unitCode FROM QuantityType qt WHERE qt.unitCodeListID = '"+unitListId+"'";
        return (List<String>) HibernateUtility.getInstance(Configuration.UBL_PERSISTENCE_UNIT_NAME)
                .loadAll(query);
    }

    public List<UnitList> getAllUnitList(){
        List<String> listOfIds = getAllUnitListIds();

        List<UnitList> listOfUnitLists = new ArrayList<>();
        for(String id : listOfIds){
            List<String> resultSet = getValues(id);

            UnitList unitList = new UnitList();
            unitList.setUnitListId(id);
            unitList.setUnits(resultSet);

            listOfUnitLists.add(unitList);
        }
        return listOfUnitLists;
    }

    public List<String> addUnitToList(String unit,String unitListId){
        QuantityType quantityType = new QuantityType();

        quantityType.setUnitCode(unit);
        quantityType.setUnitCodeListID(unitListId);
        HibernateUtility.getInstance(Configuration.UBL_PERSISTENCE_UNIT_NAME).update(quantityType);

        return getValues(unitListId);
    }

    public List<String> deleteUnitFromList(String unit,String unitListId){
        String query = "SELECT qt.hjid FROM QuantityType qt WHERE unitCodeListID = '"+unitListId+"' AND unitCode = '"+unit+"'";
        Long hjid = ((List<Long>)HibernateUtility.getInstance(Configuration.UBL_PERSISTENCE_UNIT_NAME).loadAll(query)).get(0);
        HibernateUtility.getInstance(Configuration.UBL_PERSISTENCE_UNIT_NAME).delete(QuantityType.class, hjid);
        return getValues(unitListId);
    }

    public List<String> addUnitList(String unitListId,List<String> units){
        for(String unit : units){
            QuantityType quantityType = new QuantityType();
            quantityType.setUnitCodeListID(unitListId);
            quantityType.setUnitCode(unit);
            HibernateUtility.getInstance(Configuration.UBL_PERSISTENCE_UNIT_NAME).update(quantityType);
        }
        return getValues(unitListId);
    }

    // checks whether unit list with unitListId exists or not
    public Boolean checkUnitListId(String unitListId){
        List<String> resultSet;
        String query = "SELECT qt FROM QuantityType qt WHERE qt.unitCodeListID = '"+unitListId+"'";
        resultSet = (List<String>) HibernateUtility.getInstance(Configuration.UBL_PERSISTENCE_UNIT_NAME)
                .loadAll(query);
        if(resultSet.size() > 0){
            return true;
        }
        return false;
    }

    // check whether unit exists or not for given unitListId
    public Boolean checkUnit(String unit,String unitListId){
        List<String> resultSet;
        String query = "SELECT qt FROM QuantityType qt WHERE qt.unitCodeListID = '"+unitListId+"' AND qt.unitCode = '"+unit+"'";
        resultSet = (List<String>) HibernateUtility.getInstance(Configuration.UBL_PERSISTENCE_UNIT_NAME)
                .loadAll(query);
        if(resultSet.size() > 0){
            return true;
        }
        return false;
    }

    private List<String> getAllUnitListIds(){
        List<String> resultSet;
        String query = "SELECT DISTINCT qt.unitCodeListID FROM QuantityType qt WHERE qt.unitCodeListID != null AND qt.unitCodeListID != 'NIMBLE_quantity'";
        return resultSet = (List<String>) HibernateUtility.getInstance(Configuration.UBL_PERSISTENCE_UNIT_NAME)
                .loadAll(query);
    }


}
