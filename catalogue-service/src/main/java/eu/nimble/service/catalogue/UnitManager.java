package eu.nimble.service.catalogue;

import eu.nimble.service.catalogue.model.unit.UnitList;
import eu.nimble.service.catalogue.persistence.util.UnitPersistenceUtil;
import eu.nimble.service.model.ubl.commonaggregatecomponents.UnitType;
import eu.nimble.service.model.ubl.commonaggregatecomponents.UnitTypeUnitCodeItem;
import eu.nimble.utility.persistence.GenericJPARepository;
import eu.nimble.utility.persistence.JPARepositoryFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@Component
public class UnitManager {
    @Autowired
    private JPARepositoryFactory repoFactory;

    @PostConstruct
    private void checkUnits(){
        List<UnitType> resultSet;
        resultSet = UnitPersistenceUtil.getUnitMarker(repoFactory.forCatalogueRepository(true));
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
        repoFactory.forCatalogueRepository().persistEntity(unitType);

        UnitType unitType2 = new UnitType();
        unitType2.setID("currency_quantity");
        unitType2.setUnitCode(Arrays.asList("EUR","USD","SEK"));
        repoFactory.forCatalogueRepository().persistEntity(unitType2);

        UnitType unitType3 = new UnitType();
        unitType3.setID("time_quantity");
        unitType3.setUnitCode(Arrays.asList("hour(s)", "working day(s)","day(s)","week(s)","month(s)"));
        repoFactory.forCatalogueRepository().persistEntity(unitType3);

        UnitType unitType4 = new UnitType();
        unitType4.setID("volume_quantity");
        unitType4.setUnitCode(Arrays.asList("m3", "L"));
        repoFactory.forCatalogueRepository().persistEntity(unitType4);

        UnitType unitType5 = new UnitType();
        unitType5.setID("weight_quantity");
        unitType5.setUnitCode(Arrays.asList("g","kg", "ton"));
        repoFactory.forCatalogueRepository().persistEntity(unitType5);

        UnitType unitType6 = new UnitType();
        unitType6.setID("length_quantity");
        unitType6.setUnitCode(Arrays.asList("mm","cm","m"));
        repoFactory.forCatalogueRepository().persistEntity(unitType6);

        UnitType unitType7 = new UnitType();
        unitType7.setID("package_quantity");
        unitType7.setUnitCode(Arrays.asList("box", "unit"));
        repoFactory.forCatalogueRepository().persistEntity(unitType7);

        UnitType unitType8 = new UnitType();
        unitType8.setID("dimensions");
        unitType8.setUnitCode(Arrays.asList("length","width","height"));
        repoFactory.forCatalogueRepository().persistEntity(unitType8);

        UnitType unitType9 = new UnitType();
        unitType9.setID("warranty_period");
        unitType9.setUnitCode(Arrays.asList("month(s)","year(s)"));
        repoFactory.forCatalogueRepository().persistEntity(unitType9);

        UnitType unitType10 = new UnitType();
        unitType10.setID("frame_contract_period");
        unitType10.setUnitCode(Arrays.asList("day(s)", "week(s)", "month(s)","year(s)"));
        repoFactory.forCatalogueRepository().persistEntity(unitType10);
    }


    public List<String> getValues(String unitListId){
        List<UnitType> resultSet;
        resultSet = UnitPersistenceUtil.getUnitsInList(unitListId);
        return resultSet.get(0).getUnitCode();
    }


    public List<UnitList> getAllUnitList(){
        List<UnitType> resultSet;
        resultSet = UnitPersistenceUtil.getAllUnits();

        List<UnitList> list = new ArrayList<>();

        for(UnitType unitType : resultSet){
            UnitList unitList = new UnitList();
            unitList.setUnitListId(unitType.getID());
            unitList.setUnits(unitType.getUnitCode());
            list.add(unitList);
        }
        return list;
    }

    public void deleteUnitList(String unitListId) {
        UnitType unit = UnitPersistenceUtil.getUnit(unitListId);
        repoFactory.forCatalogueRepository().deleteEntity(unit);
    }

    public List<String> addUnitToList(String unit,String unitListId){
        List<UnitTypeUnitCodeItem> resultSet;
        resultSet = UnitPersistenceUtil.getUnitCodesInList(unitListId);

        UnitTypeUnitCodeItem unitTypeUnitCodeItem = new UnitTypeUnitCodeItem();
        unitTypeUnitCodeItem.setItem(unit);

        resultSet.add(unitTypeUnitCodeItem);

        UnitType unitType = new UnitType();
        unitType.setID(unitListId);
        unitType.setHjid(getHjid(unitListId));
        unitType.setUnitCodeItems(resultSet);
        unitType = repoFactory.forCatalogueRepository().updateEntity(unitType);
        return unitType.getUnitCode();
    }

    public List<String> deleteUnitFromList(String unit,String unitListId){
        List<UnitTypeUnitCodeItem> resultSet;
        resultSet = UnitPersistenceUtil.getUnitCodesInList(unitListId);

        Long hjid = null;
        for(UnitTypeUnitCodeItem utci : resultSet){
            if(utci.getItem().contentEquals(unit)){
                hjid = utci.getHjid();
                break;
            }
        }

        UnitTypeUnitCodeItem unitTypeUnitCodeItemn = new UnitTypeUnitCodeItem();
        unitTypeUnitCodeItemn.setHjid(hjid);
        repoFactory.forCatalogueRepository().deleteEntity(unitTypeUnitCodeItemn);
        return getValues(unitListId);
    }

    public List<String> addUnitList(String unitListId,List<String> units){
        UnitType unitType = new UnitType();
        unitType.setID(unitListId);
        unitType.setUnitCode(units);
        repoFactory.forCatalogueRepository().persistEntity(unitType);
        return units;
    }


    // checks whether unit list with unitListId exists or not
    public Boolean checkUnitListId(String unitListId){
        List<UnitType> resultSet;
        resultSet = UnitPersistenceUtil.getUnitsInList(unitListId);
        if(resultSet.size() > 0){
            return true;
        }
        return false;
    }


    // check whether unit exists or not for given unitListId
    public Boolean checkUnit(String unit,String unitListId){
        List<UnitType> resultSet;
        resultSet = UnitPersistenceUtil.getUnitsInList(unitListId);
        if(resultSet.get(0).getUnitCode().contains(unit)){
            return true;
        }
        return false;
    }

    private List<String> getAllUnitListIds(){
        List<String> resultSet;
        resultSet = UnitPersistenceUtil.getAllUnitListIds();
        return resultSet;
    }

    private Long getHjid(String unitListId){
        Long hjid = UnitPersistenceUtil.getListUniqueId(unitListId);
        return hjid;
    }
}