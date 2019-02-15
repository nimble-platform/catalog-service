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


    public static CataloguePaginationResponse getCatalogueLinesForParty(String catalogueId, String partyId,String selectedCategoryName,String searchText,String languageId, int limit, int offset) {
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
                // no category name filtering and search text filtering
                if(selectedCategoryName == null && searchText == null){
                    catalogueLineIds = new JPARepositoryFactory().forCatalogueRepository(false).getEntities(QUERY_GET_CATALOGUE_LINE_IDS_FOR_PARTY,new String[]{"catalogueId","partyId"}, new Object[]{catalogueId,partyId},limit,offset);
                }
                // category name filtering and search text filtering
                else if(selectedCategoryName != null && searchText != null){
                    // TODO: implement it
                }
                // category name filtering
                else if(selectedCategoryName != null){
                    catalogueLineIds = new JPARepositoryFactory().forCatalogueRepository(false).getEntities(QUERY_GET_CATALOGUE_LINE_IDS_WITH_CATEGORY_NAMES_FOR_PARTY,new String[]{"catalogueId","partyId","categoryNames"}, new Object[]{catalogueId,partyId,selectedCategoryName},limit,offset);
                }
                // search text filtering
                else{
                    QueryData queryData = getQuery(catalogueId,partyId,searchText,languageId);
                    catalogueLineIds = new JPARepositoryFactory().forCatalogueRepository(false).getEntities(queryData.query,queryData.parameterNames.toArray(new String[0]), queryData.parameterValues.toArray(),limit,offset,true);
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

    // TODO: update this function to get other queries as well
    private static QueryData getQuery(String catalogueId,String partyId,String searchText,String languageId){
        if(searchText != null){
            QueryData queryData = new QueryData();

            String query = "select catalogue_line.id from catalogue_type catalogue join party_type party on (catalogue.provider_party_catalogue_typ_0 = party.hjid)" +
                    "join party_identification_type party_identification on (party_identification.party_identification_party_t_0 = party.hjid)" +
                    "join catalogue_line_type catalogue_line on (catalogue_line.catalogue_line_catalogue_typ_0 = catalogue.hjid)" +
                    "join goods_item_type goods_item on (catalogue_line.goods_item_catalogue_line_ty_0 = goods_item.hjid)" +
                    "join item_type item_type on (goods_item.item_goods_item_type_hjid = item_type.hjid)" +
                    "join text_type text_type on (text_type.name__item_type_hjid = item_type.hjid or text_type.description_item_type_hjid = item_type.hjid)" +
                    "where catalogue.id = :catalogueId and party_identification.id = :partyId and text_type.language_id = :languageId and text_type.value_ in (" +
                    "SELECT text_type.value_ FROM text_type, plainto_tsquery(:searchText) AS q WHERE (value_ @@ q)" +
                    ")";

            List<String> parameterNames = queryData.parameterNames;
            List<Object> parameterValues = queryData.parameterValues;

            parameterNames.add("catalogueId");
            parameterValues.add(catalogueId);

            parameterNames.add("partyId");
            parameterValues.add(partyId);

            parameterNames.add("languageId");
            parameterValues.add(languageId);

            parameterNames.add("searchText");
            parameterValues.add(searchText);

            queryData.query = query;
            return queryData;
        }
        return null;
    }

    private static class QueryData {
        private String query;
        private List<String> parameterNames = new ArrayList<>();
        private List<Object> parameterValues = new ArrayList<>();
    }
}