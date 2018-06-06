package eu.nimble.service.catalogue;

import eu.nimble.service.catalogue.model.unit.UnitList;
import eu.nimble.service.model.ubl.commonaggregatecomponents.UnitType;
import eu.nimble.service.model.ubl.commonaggregatecomponents.UnitTypeUnitCodeItem;
import eu.nimble.utility.Configuration;
import eu.nimble.utility.HibernateUtility;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

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
        List<UnitType> resultSet;
        String query = "SELECT ut FROM UnitType ut WHERE ut.ID = 'NIMBLE_quantity'";
        resultSet = (List<UnitType>) HibernateUtility.getInstance(Configuration.UBL_PERSISTENCE_UNIT_NAME).loadAll(query);
        if(resultSet.size() > 0){
            return;
        }
        else{
            insertUnits();
        }
    }

    private void insertUnits(){
        UnitType unitType = new UnitType();

        // insert flag
        unitType.setID("NIMBLE_quantity");
        unitType.setUnitCode(Collections.singletonList("NIMBLE_flag"));
        HibernateUtility.getInstance(Configuration.UBL_PERSISTENCE_UNIT_NAME).persist(unitType);

        UnitType unitType2 = new UnitType();
        unitType2.setID("currency_quantity");
        unitType2.setUnitCode(Arrays.asList("EUR","USD"));
        HibernateUtility.getInstance(Configuration.UBL_PERSISTENCE_UNIT_NAME).persist(unitType2);

        UnitType unitType3 = new UnitType();
        unitType3.setID("time_quantity");
        unitType3.setUnitCode(Arrays.asList("working days","days","weeks"));
        HibernateUtility.getInstance(Configuration.UBL_PERSISTENCE_UNIT_NAME).persist(unitType3);

        UnitType unitType4 = new UnitType();
        unitType4.setID("volume_quantity");
        unitType4.setUnitCode(Collections.singletonList("L"));
        HibernateUtility.getInstance(Configuration.UBL_PERSISTENCE_UNIT_NAME).persist(unitType4);

        UnitType unitType5 = new UnitType();
        unitType5.setID("weight_quantity");
        unitType5.setUnitCode(Arrays.asList("g","kg"));
        HibernateUtility.getInstance(Configuration.UBL_PERSISTENCE_UNIT_NAME).persist(unitType5);

        UnitType unitType6 = new UnitType();
        unitType6.setID("length_quantity");
        unitType6.setUnitCode(Arrays.asList("mm","cm","m"));
        HibernateUtility.getInstance(Configuration.UBL_PERSISTENCE_UNIT_NAME).persist(unitType6);

        UnitType unitType7 = new UnitType();
        unitType7.setID("package_quantity");
        unitType7.setUnitCode(Collections.singletonList("box"));
        HibernateUtility.getInstance(Configuration.UBL_PERSISTENCE_UNIT_NAME).persist(unitType7);

        UnitType unitType8 = new UnitType();
        unitType8.setID("dimensions");
        unitType8.setUnitCode(Arrays.asList("length","width","height","depth"));
        HibernateUtility.getInstance(Configuration.UBL_PERSISTENCE_UNIT_NAME).persist(unitType8);
    }


    public List<String> getValues(String unitListId){
        List<UnitType> resultSet;
        String query = "SELECT ut FROM UnitType ut WHERE ut.ID = '"+unitListId+"'";
        resultSet = (List<UnitType>) HibernateUtility.getInstance(Configuration.UBL_PERSISTENCE_UNIT_NAME).loadAll(query);
        return resultSet.get(0).getUnitCode();
    }


    public List<UnitList> getAllUnitList(){
        List<UnitType> resultSet;
        String query = "FROM UnitType WHERE ID != 'NIMBLE_quantity'";
        resultSet = (List<UnitType>) HibernateUtility.getInstance(Configuration.UBL_PERSISTENCE_UNIT_NAME).loadAll(query);

        List<UnitList> list = new ArrayList<>();

        for(UnitType unitType : resultSet){
            UnitList unitList = new UnitList();
            unitList.setUnitListId(unitType.getID());
            unitList.setUnits(unitType.getUnitCode());
            list.add(unitList);
        }
        return list;
    }


    public List<String> addUnitToList(String unit,String unitListId){
        List<UnitTypeUnitCodeItem> resultSet;
        String query = "SELECT ut.unitCodeItems FROM UnitType ut WHERE ut.ID = '"+unitListId+"'";
        resultSet = (List<UnitTypeUnitCodeItem>) HibernateUtility.getInstance(Configuration.UBL_PERSISTENCE_UNIT_NAME).loadAll(query);

        UnitTypeUnitCodeItem unitTypeUnitCodeItem = new UnitTypeUnitCodeItem();
        unitTypeUnitCodeItem.setItem(unit);

        resultSet.add(unitTypeUnitCodeItem);

        UnitType unitType = new UnitType();
        unitType.setID(unitListId);
        unitType.setHjid(getHjid(unitListId));
        unitType.setUnitCodeItems(resultSet);
        HibernateUtility.getInstance(Configuration.UBL_PERSISTENCE_UNIT_NAME).update(unitType);
        return unitType.getUnitCode();
    }

    public List<String> deleteUnitFromList(String unit,String unitListId){
        List<UnitTypeUnitCodeItem> resultSet;
        String query = "SELECT ut.unitCodeItems FROM UnitType ut WHERE ut.ID = '"+unitListId+"'";
        resultSet = (List<UnitTypeUnitCodeItem>) HibernateUtility.getInstance(Configuration.UBL_PERSISTENCE_UNIT_NAME).loadAll(query);

        Long hjid = null;
        for(UnitTypeUnitCodeItem utci : resultSet){
            if(utci.getItem().contentEquals(unit)){
                hjid = utci.getHjid();
                break;
            }
        }

        UnitTypeUnitCodeItem unitTypeUnitCodeItemn = new UnitTypeUnitCodeItem();
        unitTypeUnitCodeItemn.setHjid(hjid);
        HibernateUtility.getInstance(Configuration.UBL_PERSISTENCE_UNIT_NAME).delete(unitTypeUnitCodeItemn);
        return getValues(unitListId);
    }

    public List<String> addUnitList(String unitListId,List<String> units){
        UnitType unitType = new UnitType();
        unitType.setID(unitListId);
        unitType.setUnitCode(units);
        HibernateUtility.getInstance(Configuration.UBL_PERSISTENCE_UNIT_NAME).persist(unitType);
        return units;
    }


    // checks whether unit list with unitListId exists or not
    public Boolean checkUnitListId(String unitListId){
        List<UnitType> resultSet;
        String query = "SELECT ut FROM UnitType ut WHERE ut.ID = '"+unitListId+"'";
        resultSet = (List<UnitType>) HibernateUtility.getInstance(Configuration.UBL_PERSISTENCE_UNIT_NAME).loadAll(query);
        if(resultSet.size() > 0){
            return true;
        }
        return false;
    }


    // check whether unit exists or not for given unitListId
    public Boolean checkUnit(String unit,String unitListId){
        List<UnitType> resultSet;
        String query = "SELECT ut FROM UnitType ut WHERE ut.ID = '"+unitListId+"'";
        resultSet = (List<UnitType>) HibernateUtility.getInstance(Configuration.UBL_PERSISTENCE_UNIT_NAME).loadAll(query);
        if(resultSet.get(0).getUnitCode().contains(unit)){
            return true;
        }
        return false;
    }

    private List<String> getAllUnitListIds(){
        List<String> resultSet;
        String query = "SELECT ut.ID FROM UnitType ut WHERE ut.ID != 'NIMBLE_quantity'";
        resultSet = (List<String>) HibernateUtility.getInstance(Configuration.UBL_PERSISTENCE_UNIT_NAME).loadAll(query);
        return resultSet;
    }

    private Long getHjid(String unitListId){
        List<Long> resultSet;
        String query = "SELECT ut.hjid FROM UnitType ut WHERE ut.ID = '"+unitListId+"'";
        resultSet = (List<Long>) HibernateUtility.getInstance(Configuration.UBL_PERSISTENCE_UNIT_NAME).loadAll(query);
        return resultSet.get(0);
    }

}