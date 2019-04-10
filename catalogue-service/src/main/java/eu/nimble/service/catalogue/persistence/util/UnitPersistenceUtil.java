package eu.nimble.service.catalogue.persistence.util;

import eu.nimble.service.catalogue.util.SpringBridge;
import eu.nimble.service.model.ubl.commonaggregatecomponents.UnitType;
import eu.nimble.service.model.ubl.commonaggregatecomponents.UnitTypeUnitCodeItem;
import eu.nimble.utility.persistence.GenericJPARepository;
import eu.nimble.utility.persistence.JPARepositoryFactory;

import java.util.List;

/**
 * Created by suat on 31-Dec-18.
 */
public class UnitPersistenceUtil {
    private static final String QUERY_GET_UNIT_MARKER = "SELECT ut FROM UnitType ut WHERE ut.ID = 'NIMBLE_quantity'";
    private static final String QUERY_GET_UNITS_IN_LIST = "SELECT ut FROM UnitType ut WHERE ut.ID = :listId";
    private static final String QUERY_GET_ALL_UNITS = "SELECT ut FROM UnitType ut WHERE ut.ID <> 'NIMBLE_quantity'";
    private static final String QUERY_GET_UNIT_CODES_IN_LIST = "SELECT ut.unitCodeItems FROM UnitType ut WHERE ut.ID = :listId";
    private static final String QUERY_GET_ALL_UNIT_LIST_IDS = "SELECT ut.ID FROM UnitType ut WHERE ut.ID <> 'NIMBLE_quantity'";
    private static final String QUERY_GET_LIST_UNIQUE_ID = "SELECT ut.hjid FROM UnitType ut WHERE ut.ID = :listId";

    public static List<UnitType> getUnitMarker(GenericJPARepository jpaRepository) {
        return jpaRepository.getEntities(QUERY_GET_UNIT_MARKER);
    }

    public static List<UnitType> getUnitsInList(String listId) {
        return new JPARepositoryFactory().forCatalogueRepository(true).getEntities(QUERY_GET_UNITS_IN_LIST, new String[]{"listId"}, new Object[]{listId});
    }

    public static List<UnitType> getAllUnits() {
        return new JPARepositoryFactory().forCatalogueRepository(true).getEntities(QUERY_GET_ALL_UNITS);
    }

    public static List<UnitTypeUnitCodeItem> getUnitCodesInList(String listId) {
        return new JPARepositoryFactory().forCatalogueRepository(true).getEntities(QUERY_GET_UNIT_CODES_IN_LIST, new String[]{"listId"}, new Object[]{listId});
    }

    public static List<String> getAllUnitListIds() {
        return new JPARepositoryFactory().forCatalogueRepository().getEntities(QUERY_GET_ALL_UNIT_LIST_IDS);
    }

    public static Long getListUniqueId(String listId) {
        return new JPARepositoryFactory().forCatalogueRepository().getSingleEntity(QUERY_GET_LIST_UNIQUE_ID, new String[]{"listId"}, new Object[]{listId});
    }
}
