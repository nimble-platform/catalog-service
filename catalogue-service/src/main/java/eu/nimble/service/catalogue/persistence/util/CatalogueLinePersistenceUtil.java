package eu.nimble.service.catalogue.persistence.util;

import eu.nimble.service.catalogue.model.catalogue.CatalogueLineSortOptions;
import eu.nimble.service.model.ubl.commonaggregatecomponents.CatalogueLineType;
import eu.nimble.utility.persistence.JPARepositoryFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

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
    private static final String QUERY_GET_HJID_AND_PARTY_ID_BY_CAT_UUID_AND_ID = "SELECT clj.hjid,partyIdentification.ID FROM CatalogueType as c"
            + " JOIN c.catalogueLine as clj join clj.goodsItem.item.manufacturerParty.partyIdentification partyIdentification"
            + " WHERE c.UUID = :catalogueUuid "
            + " AND clj.ID = :lineId";
    private static final String QUERY_GET_BY_HJID = "SELECT cl FROM CatalogueLineType as cl WHERE cl.hjid = :hjid";
    private static final String QUERY_GET_BY_HJIDS = "SELECT cl FROM CatalogueLineType as cl WHERE cl.hjid in :hjids";

    public static Boolean checkCatalogueLineExistence(String catalogueUuid, String lineId) {
        long lineExistence = new JPARepositoryFactory().forCatalogueRepository().getSingleEntity(QUERY_CHECK_EXISTENCE_BY_ID, new String[]{"catalogueUuid", "lineId"}, new Object[]{catalogueUuid, lineId});
        return lineExistence == 1 ? true : false;
    }

    public static Boolean checkCatalogueLineExistence(String catalogueUuid, String lineId, Long hjid) {
        long lineExistence = new JPARepositoryFactory().forCatalogueRepository().getSingleEntity(QUERY_CHECK_EXISTENCE_BY_HJID_AND_ID, new String[]{"catalogueUuid", "lineId", "hjid"}, new Object[]{catalogueUuid, lineId, hjid});
        return lineExistence == 1 ? true : false;
    }

    public static CatalogueLineType getCatalogueLine(Long hjid) {
        return new JPARepositoryFactory().forCatalogueRepository().getSingleEntity(QUERY_GET_BY_HJID, new String[]{"hjid"}, new Object[]{hjid});
    }

    public static List<CatalogueLineType> getCatalogueLines(List<Long> hjids,CatalogueLineSortOptions sortOption,int limit, int pageNo) {

        List<CatalogueLineType> catalogueLines = new ArrayList<>();
        if(hjids.size() > 0){
            String getCatalogueLinesQuery = QUERY_GET_BY_HJIDS;
            if(sortOption != null){
                switch (sortOption){
                    case PRICE_HIGH_TO_LOW:
                        getCatalogueLinesQuery += " ORDER BY cl.requiredItemLocationQuantity.price.priceAmount.value DESC NULLS LAST";
                        break;
                    case PRICE_LOW_TO_HIGH:
                        getCatalogueLinesQuery += " ORDER BY cl.requiredItemLocationQuantity.price.priceAmount.value ASC NULLS LAST";
                        break;
                }
            }
            catalogueLines = new JPARepositoryFactory().forCatalogueRepository().getEntities(getCatalogueLinesQuery, new String[]{"hjids"}, new Object[]{hjids});

            if(limit != 0){
                int startIndex = limit*pageNo;
                int endIndex = startIndex+limit;
                if(endIndex > catalogueLines.size())
                    endIndex = catalogueLines.size();
                catalogueLines = catalogueLines.subList(startIndex,endIndex);
            }
        }
        return catalogueLines;
    }

    public static CatalogueLineType getCatalogueLine(String catalogueUuid, String lineId) {
        return new JPARepositoryFactory().forCatalogueRepository().getSingleEntity(QUERY_GET_BY_CAT_UUID_AND_ID, new String[]{"catalogueUuid", "lineId"}, new Object[]{catalogueUuid, lineId});
    }

    // this method returns an array of objects
    // first one is the hjid of the catalogue line
    // second one is the id of the party owning the catalogue line
    public static Object[] getCatalogueLineHjidAndPartyId(String catalogueUuid, String lineId){
        return new JPARepositoryFactory().forCatalogueRepository().getSingleEntity(QUERY_GET_HJID_AND_PARTY_ID_BY_CAT_UUID_AND_ID, new String[]{"catalogueUuid", "lineId"}, new Object[]{catalogueUuid, lineId});
    }
}
