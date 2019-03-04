package eu.nimble.service.catalogue.persistence.util;

import eu.nimble.service.catalogue.util.SpringBridge;
import eu.nimble.service.model.ubl.commonaggregatecomponents.CatalogueLineType;
import eu.nimble.utility.persistence.JPARepositoryFactory;
import org.springframework.data.repository.query.Param;

import javax.swing.*;

/**
 * Created by suat on 31-Dec-18.
 */
public class CatalogueLinePersistenceUtil {

    private static final String QUERY_CHECK_EXISTENCE_BY_ID = "SELECT COUNT(cl) FROM CatalogueLineType as cl, CatalogueType as c "
            + " JOIN c.catalogueLine as clj"
            + " WHERE c.UUID = :catalogueUuid "
            + " AND cl.ID = :lineId "
            + " AND clj.ID = cl.ID ";
    private static final String QUERY_CHECK_EXISTENCE_BY_HJID_AND_ID = "SELECT COUNT(clj) FROM CatalogueType as c "
            + " JOIN c.catalogueLine as clj"
            + " WHERE c.UUID = :catalogueUuid"
            + " AND clj.hjid <> :hjid "
            + " AND clj.ID = :lineId ";
    private static final String QUERY_GET_BY_CAT_UUID_AND_ID = "SELECT clj FROM CatalogueType as c "
            + " JOIN c.catalogueLine as clj"
            + " WHERE c.UUID = :catalogueUuid "
            + " AND clj.ID = :lineId";

    public static Boolean checkCatalogueLineExistence(String catalogueUuid, String lineId) {
        long lineExistence = new JPARepositoryFactory().forCatalogueRepository().getSingleEntity(QUERY_CHECK_EXISTENCE_BY_ID, new String[]{"catalogueUuid", "lineId"}, new Object[]{catalogueUuid, lineId});
        return lineExistence == 1 ? true : false;
    }

    public static Boolean checkCatalogueLineExistence(String catalogueUuid, String lineId, Long hjid) {
        long lineExistence = new JPARepositoryFactory().forCatalogueRepository().getSingleEntity(QUERY_CHECK_EXISTENCE_BY_HJID_AND_ID, new String[]{"catalogueUuid", "lineId", "hjid"}, new Object[]{catalogueUuid, lineId, hjid});
        return lineExistence == 1 ? true : false;
    }

    public static CatalogueLineType getCatalogueLine(String catalogueUuid, String lineId) {
        return new JPARepositoryFactory().forCatalogueRepository().getSingleEntity(QUERY_GET_BY_CAT_UUID_AND_ID, new String[]{"catalogueUuid", "lineId"}, new Object[]{catalogueUuid, lineId});
    }
}
