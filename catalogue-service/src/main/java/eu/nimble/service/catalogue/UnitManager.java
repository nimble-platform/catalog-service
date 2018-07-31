package eu.nimble.service.catalogue;

import eu.nimble.service.catalogue.model.unit.UnitList;
import eu.nimble.service.model.ubl.commonaggregatecomponents.UnitType;
import eu.nimble.service.model.ubl.commonaggregatecomponents.UnitTypeUnitCodeItem;
import eu.nimble.utility.Configuration;
import eu.nimble.utility.HibernateUtility;
import eu.nimble.utility.config.PersistenceConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@Component
public class UnitManager {
    private static UnitManager instance;

    @Autowired
    private PersistenceConfig config;

    private HibernateUtility hibernateUtility; 

    @PostConstruct
    private void checkUnits(){
        hibernateUtility = HibernateUtility.getInstance(Configuration.UBL_PERSISTENCE_UNIT_NAME, config.getUbl());
        List<UnitType> resultSet;
        String query = "SELECT ut FROM UnitType ut WHERE ut.ID = 'NIMBLE_quantity'";
        resultSet = (List<UnitType>) hibernateUtility.loadAll(query);
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
        hibernateUtility.persist(unitType);

        UnitType unitType2 = new UnitType();
        unitType2.setID("currency_quantity");
        unitType2.setUnitCode(Arrays.asList("EUR","USD"));
        hibernateUtility.persist(unitType2);

        UnitType unitType3 = new UnitType();
        unitType3.setID("time_quantity");
        unitType3.setUnitCode(Arrays.asList("working days","days","weeks"));
        hibernateUtility.persist(unitType3);

        UnitType unitType4 = new UnitType();
        unitType4.setID("volume_quantity");
        unitType4.setUnitCode(Arrays.asList("L, m3"));
        hibernateUtility.persist(unitType4);

        UnitType unitType5 = new UnitType();
        unitType5.setID("weight_quantity");
        unitType5.setUnitCode(Arrays.asList("g","kg", "ton"));
        hibernateUtility.persist(unitType5);

        UnitType unitType6 = new UnitType();
        unitType6.setID("length_quantity");
        unitType6.setUnitCode(Arrays.asList("mm","cm","m"));
        hibernateUtility.persist(unitType6);

        UnitType unitType7 = new UnitType();
        unitType7.setID("package_quantity");
        unitType7.setUnitCode(Arrays.asList("box", "unit"));
        hibernateUtility.persist(unitType7);

        UnitType unitType8 = new UnitType();
        unitType8.setID("dimensions");
        unitType8.setUnitCode(Arrays.asList("length","width","height","depth"));
        hibernateUtility.persist(unitType8);

        UnitType unitType9 = new UnitType();
        unitType9.setID("warranty_period");
        unitType9.setUnitCode(Arrays.asList("month","year"));
        hibernateUtility.persist(unitType9);
    }


    public List<String> getValues(String unitListId){
        List<UnitType> resultSet;
        String query = "SELECT ut FROM UnitType ut WHERE ut.ID = '"+unitListId+"'";
        resultSet = (List<UnitType>) hibernateUtility.loadAll(query);
        return resultSet.get(0).getUnitCode();
    }


    public List<UnitList> getAllUnitList(){
        List<UnitType> resultSet;
        String query = "FROM UnitType WHERE ID != 'NIMBLE_quantity'";
        resultSet = (List<UnitType>) hibernateUtility.loadAll(query);

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
        resultSet = (List<UnitTypeUnitCodeItem>) hibernateUtility.loadAll(query);

        UnitTypeUnitCodeItem unitTypeUnitCodeItem = new UnitTypeUnitCodeItem();
        unitTypeUnitCodeItem.setItem(unit);

        resultSet.add(unitTypeUnitCodeItem);

        UnitType unitType = new UnitType();
        unitType.setID(unitListId);
        unitType.setHjid(getHjid(unitListId));
        unitType.setUnitCodeItems(resultSet);
        hibernateUtility.update(unitType);
        return unitType.getUnitCode();
    }

    public List<String> deleteUnitFromList(String unit,String unitListId){
        List<UnitTypeUnitCodeItem> resultSet;
        String query = "SELECT ut.unitCodeItems FROM UnitType ut WHERE ut.ID = '"+unitListId+"'";
        resultSet = (List<UnitTypeUnitCodeItem>) hibernateUtility.loadAll(query);

        Long hjid = null;
        for(UnitTypeUnitCodeItem utci : resultSet){
            if(utci.getItem().contentEquals(unit)){
                hjid = utci.getHjid();
                break;
            }
        }

        UnitTypeUnitCodeItem unitTypeUnitCodeItemn = new UnitTypeUnitCodeItem();
        unitTypeUnitCodeItemn.setHjid(hjid);
        hibernateUtility.delete(unitTypeUnitCodeItemn);
        return getValues(unitListId);
    }

    public List<String> addUnitList(String unitListId,List<String> units){
        UnitType unitType = new UnitType();
        unitType.setID(unitListId);
        unitType.setUnitCode(units);
        hibernateUtility.persist(unitType);
        return units;
    }


    // checks whether unit list with unitListId exists or not
    public Boolean checkUnitListId(String unitListId){
        List<UnitType> resultSet;
        String query = "SELECT ut FROM UnitType ut WHERE ut.ID = '"+unitListId+"'";
        resultSet = (List<UnitType>) hibernateUtility.loadAll(query);
        if(resultSet.size() > 0){
            return true;
        }
        return false;
    }


    // check whether unit exists or not for given unitListId
    public Boolean checkUnit(String unit,String unitListId){
        List<UnitType> resultSet;
        String query = "SELECT ut FROM UnitType ut WHERE ut.ID = '"+unitListId+"'";
        resultSet = (List<UnitType>) hibernateUtility.loadAll(query);
        if(resultSet.get(0).getUnitCode().contains(unit)){
            return true;
        }
        return false;
    }

    private List<String> getAllUnitListIds(){
        List<String> resultSet;
        String query = "SELECT ut.ID FROM UnitType ut WHERE ut.ID != 'NIMBLE_quantity'";
        resultSet = (List<String>) hibernateUtility.loadAll(query);
        return resultSet;
    }

    private Long getHjid(String unitListId){
        List<Long> resultSet;
        String query = "SELECT ut.hjid FROM UnitType ut WHERE ut.ID = '"+unitListId+"'";
        resultSet = (List<Long>) hibernateUtility.loadAll(query);
        return resultSet.get(0);
    }

}