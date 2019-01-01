package eu.nimble.service.catalogue;

import eu.nimble.service.catalogue.model.unit.UnitList;
import eu.nimble.service.catalogue.persistence.util.UnitPersistenceUtil;
import eu.nimble.service.model.ubl.commonaggregatecomponents.UnitType;
import eu.nimble.service.model.ubl.commonaggregatecomponents.UnitTypeUnitCodeItem;
import eu.nimble.utility.persistence.GenericJPARepository;
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
    private GenericJPARepository genericJPARepository;

    @PostConstruct
    private void checkUnits(){
        List<UnitType> resultSet;
        resultSet = UnitPersistenceUtil.getUnitMarker(genericJPARepository.withEmf("ubldbEntityManagerFactory"));
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
        genericJPARepository.withEmf("ubldbEntityManagerFactory").persistEntity(unitType);

        UnitType unitType2 = new UnitType();
        unitType2.setID("currency_quantity");
        unitType2.setUnitCode(Arrays.asList("EUR","USD","SEK"));
        genericJPARepository.withEmf("ubldbEntityManagerFactory").persistEntity(unitType2);

        UnitType unitType3 = new UnitType();
        unitType3.setID("time_quantity");
        unitType3.setUnitCode(Arrays.asList("working days","days","weeks"));
        genericJPARepository.withEmf("ubldbEntityManagerFactory").persistEntity(unitType3);

        UnitType unitType4 = new UnitType();
        unitType4.setID("volume_quantity");
        unitType4.setUnitCode(Arrays.asList("L, m3"));
        genericJPARepository.withEmf("ubldbEntityManagerFactory").persistEntity(unitType4);

        UnitType unitType5 = new UnitType();
        unitType5.setID("weight_quantity");
        unitType5.setUnitCode(Arrays.asList("g","kg", "ton"));
        genericJPARepository.withEmf("ubldbEntityManagerFactory").persistEntity(unitType5);

        UnitType unitType6 = new UnitType();
        unitType6.setID("length_quantity");
        unitType6.setUnitCode(Arrays.asList("mm","cm","m"));
        genericJPARepository.withEmf("ubldbEntityManagerFactory").persistEntity(unitType6);

        UnitType unitType7 = new UnitType();
        unitType7.setID("package_quantity");
        unitType7.setUnitCode(Arrays.asList("box", "unit"));
        genericJPARepository.withEmf("ubldbEntityManagerFactory").persistEntity(unitType7);

        UnitType unitType8 = new UnitType();
        unitType8.setID("dimensions");
        unitType8.setUnitCode(Arrays.asList("length","width","height","depth"));
        genericJPARepository.withEmf("ubldbEntityManagerFactory").persistEntity(unitType8);

        UnitType unitType9 = new UnitType();
        unitType9.setID("warranty_period");
        unitType9.setUnitCode(Arrays.asList("month","year"));
        genericJPARepository.withEmf("ubldbEntityManagerFactory").persistEntity(unitType9);
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
        unitType = genericJPARepository.withEmf("ubldbEntityManagerFactory").updateEntity(unitType);
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
        genericJPARepository.withEmf("ubldbEntityManagerFactory").deleteEntity(unitTypeUnitCodeItemn);
        return getValues(unitListId);
    }

    public List<String> addUnitList(String unitListId,List<String> units){
        UnitType unitType = new UnitType();
        unitType.setID(unitListId);
        unitType.setUnitCode(units);
        genericJPARepository.withEmf("ubldbEntityManagerFactory").persistEntity(unitType);
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