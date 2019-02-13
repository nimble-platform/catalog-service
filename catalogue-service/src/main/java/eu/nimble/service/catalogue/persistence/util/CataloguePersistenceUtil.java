package eu.nimble.service.catalogue.persistence.util;

import eu.nimble.service.catalogue.model.catalogue.CataloguePaginationResponse;
import eu.nimble.service.model.ubl.catalogue.CatalogueType;
import eu.nimble.service.model.ubl.commonaggregatecomponents.CatalogueLineType;
import eu.nimble.utility.persistence.JPARepositoryFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by suat on 31-Dec-18.
 */
public class CataloguePersistenceUtil {
    private static final String QUERY_GET_BY_UUID = "SELECT catalogue FROM CatalogueType catalogue WHERE catalogue.UUID = :uuid";
    private static final String QUERY_GET_FOR_PARTY = "SELECT catalogue FROM CatalogueType as catalogue "
            + " JOIN catalogue.providerParty as catalogue_provider_party JOIN catalogue_provider_party.partyIdentification partyIdentification"
            + " WHERE catalogue.ID = :catalogueId"
            + " AND partyIdentification.ID = :partyId";
    private static final String QUERY_CHECK_EXISTENCE_BY_ID = "SELECT COUNT(catalogue) FROM CatalogueType catalogue"
            + " JOIN catalogue.providerParty as catalogue_provider_party JOIN catalogue_provider_party.partyIdentification partyIdentification"
            + " WHERE catalogue.ID = :catalogueId and partyIdentification.ID = :partyId";
    private static final String QUERY_GET_CATALOGUE_IDS_FOR_PARTY = "SELECT catalogue.UUID FROM CatalogueType as catalogue" +
            " JOIN catalogue.providerParty as catalogue_provider_party JOIN catalogue_provider_party.partyIdentification partyIdentification" +
            " WHERE partyIdentification.ID = :partyId";

    private static final String QUERY_GET_CATALOGUE_LINES_BY_IDS = "SELECT catalogueLine FROM CatalogueLineType catalogueLine " +
            " WHERE catalogueLine.ID in :catalogueLineIds";
    private static final String QUERY_GET_COMMODITY_CLASSIFICATION_NAMES_OF_CATALOGUE_LINES = "SELECT DISTINCT itemClassificationCode.name FROM CatalogueType as catalogue " +
            " JOIN catalogue.catalogueLine catalogueLine JOIN catalogueLine.goodsItem.item.commodityClassification commodityClassification JOIN commodityClassification.itemClassificationCode itemClassificationCode " +
            " WHERE catalogue.UUID = :catalogueUuid";
    private static final String QUERY_GET_CATALOGUE_LINE_IDS_FOR_PARTY = "SELECT catalogueLine.ID FROM CatalogueType as catalogue "
            + " JOIN catalogue.providerParty as catalogue_provider_party JOIN catalogue_provider_party.partyIdentification partyIdentification JOIN catalogue.catalogueLine catalogueLine"
            + " WHERE catalogue.ID = :catalogueId"
            + " AND partyIdentification.ID = :partyId";
    private static final String QUERY_GET_CATALOGUE_LINE_IDS_WITH_CATEGORY_NAMES_FOR_PARTY = "SELECT catalogueLine.ID FROM CatalogueType as catalogue "
            + " JOIN catalogue.providerParty as catalogue_provider_party JOIN catalogue_provider_party.partyIdentification partyIdentification JOIN catalogue.catalogueLine catalogueLine "
            + " JOIN catalogueLine.goodsItem.item.commodityClassification commodityClassification JOIN commodityClassification.itemClassificationCode itemClassificationCode "
            + " WHERE catalogue.ID = :catalogueId"
            + " AND partyIdentification.ID = :partyId"
            + " AND itemClassificationCode.name in :categoryNames";
    private static final String QUERY_GET_CATALOGUE_LINE_COUNT_FOR_PARTY = "SELECT COUNT(catalogueLine) FROM CatalogueType as catalogue "
            + " JOIN catalogue.providerParty as catalogue_provider_party JOIN catalogue_provider_party.partyIdentification partyIdentification JOIN catalogue.catalogueLine catalogueLine"
            + " WHERE catalogue.ID = :catalogueId"
            + " AND partyIdentification.ID = :partyId";
    private static final String QUERY_GET_CATALOGUE_UUID_FOR_PARTY = "SELECT catalogue.UUID FROM CatalogueType as catalogue "
            + " JOIN catalogue.providerParty as catalogue_provider_party JOIN catalogue_provider_party.partyIdentification partyIdentification"
            + " WHERE catalogue.ID = :catalogueId"
            + " AND partyIdentification.ID = :partyId";


    public static CataloguePaginationResponse getCatalogueLinesForParty(String catalogueId, String partyId,List<String> selectedCategoryNames, int limit, int offset) {
        // get catalogue uuid
        String catalogueUuid = new JPARepositoryFactory().forCatalogueRepository(false).getSingleEntity(QUERY_GET_CATALOGUE_UUID_FOR_PARTY,new String[]{"catalogueId","partyId"}, new Object[]{catalogueId,partyId});
        long size = 0;
        List<String> categoryNames = new ArrayList<>();
        List<CatalogueLineType> catalogueLines = new ArrayList<>();
        if(catalogueUuid != null){
            // get number of catalogue lines which the catalogue contains
            size = new JPARepositoryFactory().forCatalogueRepository(false).getSingleEntity(QUERY_GET_CATALOGUE_LINE_COUNT_FOR_PARTY,new String[]{"catalogueId","partyId"}, new Object[]{catalogueId,partyId});
            // get names of the categories for all catalogue lines which the catalogue contains
            categoryNames = new JPARepositoryFactory().forCatalogueRepository(false).getEntities(QUERY_GET_COMMODITY_CLASSIFICATION_NAMES_OF_CATALOGUE_LINES,new String[]{"catalogueUuid"}, new Object[]{catalogueUuid});
            // if limit is equal to 0,then no catalogue lines are returned
            if(limit != 0){
                // get catalogue line ids according to the given limit and offset
                List<String> catalogueLineIds = new ArrayList<>();
                // no category name filtering
                if(selectedCategoryNames == null){
                    catalogueLineIds = new JPARepositoryFactory().forCatalogueRepository(false).getEntities(QUERY_GET_CATALOGUE_LINE_IDS_FOR_PARTY,new String[]{"catalogueId","partyId"}, new Object[]{catalogueId,partyId},limit,offset);
                }
                // category name filtering
                else{
                    catalogueLineIds = new JPARepositoryFactory().forCatalogueRepository(false).getEntities(QUERY_GET_CATALOGUE_LINE_IDS_WITH_CATEGORY_NAMES_FOR_PARTY,new String[]{"catalogueId","partyId","categoryNames"}, new Object[]{catalogueId,partyId,selectedCategoryNames},limit,offset);
                }

                if(catalogueLineIds.size() != 0)
                    catalogueLines = new JPARepositoryFactory().forCatalogueRepository().getEntities(QUERY_GET_CATALOGUE_LINES_BY_IDS,new String[]{"catalogueLineIds"}, new Object[]{catalogueLineIds});
            }
        }
        // created CataloguePaginationResponse
        CataloguePaginationResponse cataloguePaginationResponse = new CataloguePaginationResponse();
        cataloguePaginationResponse.setSize(size);
        cataloguePaginationResponse.setCatalogueLines(catalogueLines);
        cataloguePaginationResponse.setCatalogueUuid(catalogueUuid);
        cataloguePaginationResponse.setCategoryNames(categoryNames);
        return cataloguePaginationResponse;
    }

    public static CatalogueType getCatalogueByUuid(String catalogueUuid) {
        return new JPARepositoryFactory().forCatalogueRepository().getSingleEntity(QUERY_GET_BY_UUID, new String[]{"uuid"}, new Object[]{catalogueUuid});
    }

    public static CatalogueType getCatalogueForParty(String catalogueId, String partyId) {
        return new JPARepositoryFactory().forCatalogueRepository().getSingleEntity(QUERY_GET_FOR_PARTY, new String[]{"catalogueId", "partyId"}, new Object[]{catalogueId, partyId});
    }

    public static Boolean checkCatalogueExistenceById(String catalogueId, String partyId) {
        long catalogueExists = new JPARepositoryFactory().forCatalogueRepository().getSingleEntity(QUERY_CHECK_EXISTENCE_BY_ID, new String[]{"catalogueId", "partyId"}, new Object[]{catalogueId, partyId});
        return catalogueExists == 1 ? true : false;
    }

    public static List<String> getCatalogueIdsForParty(String partyId) {
        return new JPARepositoryFactory().forCatalogueRepository().getEntities(QUERY_GET_CATALOGUE_IDS_FOR_PARTY, new String[]{"partyId"}, new Object[]{partyId});
    }
}
