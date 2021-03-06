package eu.nimble.service.catalogue.persistence.util;

import eu.nimble.service.catalogue.model.catalogue.CatalogueLineSortOptions;
import eu.nimble.service.catalogue.model.lcpa.ItemLCPAInput;
import eu.nimble.service.model.ubl.commonaggregatecomponents.CatalogueLineType;
import eu.nimble.service.model.ubl.commonaggregatecomponents.DocumentReferenceType;
import eu.nimble.service.model.ubl.commonaggregatecomponents.ItemType;
import eu.nimble.service.model.ubl.commonbasiccomponents.BinaryObjectType;
import eu.nimble.utility.persistence.JPARepositoryFactory;
import eu.nimble.utility.persistence.binary.BinaryContentService;
import org.hibernate.Hibernate;

import java.util.ArrayList;
import java.util.Base64;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Created by suat on 31-Dec-18.
 */
public class CatalogueLinePersistenceUtil {

    private static final String QUERY_CHECK_EXISTENCE_BY_HJID = "SELECT COUNT(cl) FROM CatalogueLineType as cl "
            + " WHERE cl.hjid = :hjid ";

    private static final String QUERY_CHECK_EXISTENCE_BY_ID = "SELECT COUNT(clj) FROM CatalogueType as c "
            + " JOIN c.catalogueLine as clj"
            + " WHERE c.UUID = :catalogueUuid "
            + " AND clj.ID = :lineId ";
    private static final String QUERY_CHECK_EXISTENCE_BY_HJID_AND_ID = "SELECT COUNT(clj) FROM CatalogueType as c "
            + " JOIN c.catalogueLine as clj"
            + " WHERE c.UUID = :catalogueUuid"
            + " AND clj.hjid <> :hjid "
            + " AND clj.ID = :lineId ";
    private static final String QUERY_GET_BY_CAT_UUID_AND_ID = "SELECT clj FROM CatalogueType as c "
            + " JOIN c.catalogueLine as clj"
            + " WHERE c.UUID = :catalogueUuid "
            + " AND clj.ID = :lineId";
    private static final String QUERY_GET_BY_CAT_UUID_AND_IDS = "SELECT clj FROM CatalogueType as c "
            + " JOIN c.catalogueLine as clj"
            + " WHERE c.UUID = :catalogueUuid "
            + " AND clj.ID in :lineIds";
    private static final String QUERY_GET_CAT_UUID_AND_IDS = "SELECT c.UUID, clj.ID FROM CatalogueType as c "
            + " JOIN c.catalogueLine as clj"
            + " WHERE c.UUID in :catalogueUuid";
    private static final String QUERY_GET_HJID_AND_PARTY_ID_BY_CAT_UUID_AND_ID = "SELECT clj.hjid,partyIdentification.ID FROM CatalogueType as c"
            + " JOIN c.catalogueLine as clj join clj.goodsItem.item.manufacturerParty.partyIdentification partyIdentification"
            + " WHERE c.UUID = :catalogueUuid "
            + " AND clj.ID = :lineId";
    private static final String QUERY_GET_BY_HJID = "SELECT cl FROM CatalogueLineType as cl WHERE cl.hjid = :hjid";
    private static final String QUERY_GET_BY_HJIDS = "SELECT cl FROM CatalogueLineType as cl WHERE cl.hjid in :hjids";
    private static final String QUERY_GET_LINE_ITEMS_WITH_LCPA_INPUT_WITHOUT_LCPA_OUTPUT = "SELECT cl.hjid, i FROM CatalogueLineType cl" +
            " JOIN cl.goodsItem gi" +
            " JOIN gi.item i" +
            " JOIN i.lifeCyclePerformanceAssessmentDetails lcpa WHERE" +
            " lcpa.LCPAInput is not null AND" +
            " lcpa.LCPAOutput is null";
    private static final String QUERY_GET_ALL_CATALOGUELINES_CLURI = "SELECT cl.goodsItem.item FROM CatalogueLineType as cl";
    private static final String QUERY_GET_IMAGE_URIS_IN_LINES =
            "SELECT DISTINCT pi.uri FROM CatalogueLineType cl" +
                    " JOIN cl.goodsItem.item.productImage pi" +
                    " WHERE cl IN :lines";
    private static final String QUERY_GET_ITEM_SPECIFICATION_DOCUMENT_URIS_IN_LINES =
            "SELECT DISTINCT dr.attachment.embeddedDocumentBinaryObject.uri FROM CatalogueLineType cl" +
            " JOIN cl.goodsItem.item.itemSpecificationDocumentReference dr" +
            " WHERE cl IN :lines";
    private static final String QUERY_GET_ITEM_CERTIFICATE_DOCUMENT_URIS_IN_LINES =
            "SELECT DISTINCT dr.attachment.embeddedDocumentBinaryObject.uri FROM CatalogueLineType cl" +
            " JOIN cl.goodsItem.item.certificate cer" +
            " JOIN cer.documentReference dr" +
            " WHERE cl IN :lines";

    // lazy loading queries
    public static final String QUERY_GET_CATALOGUE_LINES_WITH_COMMODITY_CLASSIFICATIONS =
            "SELECT DISTINCT cl FROM CatalogueType c" +
                    " JOIN c.catalogueLine cl" +
                    " JOIN FETCH cl.goodsItem gi" +
                    " JOIN FETCH gi.item item" +
                    " JOIN FETCH item.commodityClassification" +
                    " WHERE c.UUID = :uuid";

    public static Boolean checkCatalogueLineExistence(Long hjid) {
        long lineExistence = new JPARepositoryFactory().forCatalogueRepository().getSingleEntity(QUERY_CHECK_EXISTENCE_BY_HJID, new String[]{"hjid"}, new Object[]{hjid});
        return lineExistence == 1;
    }

    public static Boolean checkCatalogueLineExistence(String catalogueUuid, String lineId) {
        long lineExistence = new JPARepositoryFactory().forCatalogueRepository().getSingleEntity(QUERY_CHECK_EXISTENCE_BY_ID, new String[]{"catalogueUuid", "lineId"}, new Object[]{catalogueUuid, lineId});
        return lineExistence == 1;
    }

    public static Boolean checkCatalogueLineExistence(String catalogueUuid, String lineId, Long hjid) {
        long lineExistence = new JPARepositoryFactory().forCatalogueRepository().getSingleEntity(QUERY_CHECK_EXISTENCE_BY_HJID_AND_ID, new String[]{"catalogueUuid", "lineId", "hjid"}, new Object[]{catalogueUuid, lineId, hjid});
        return lineExistence == 1;
    }

    public static CatalogueLineType getCatalogueLine(Long hjid) {
        return new JPARepositoryFactory().forCatalogueRepository(true).getSingleEntity(QUERY_GET_BY_HJID, new String[]{"hjid"}, new Object[]{hjid});
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
            catalogueLines = new JPARepositoryFactory().forCatalogueRepository(true).getEntities(getCatalogueLinesQuery, new String[]{"hjids"}, new Object[]{hjids});

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
        return new JPARepositoryFactory().forCatalogueRepository(true).getSingleEntity(QUERY_GET_BY_CAT_UUID_AND_ID, new String[]{"catalogueUuid", "lineId"}, new Object[]{catalogueUuid, lineId});
    }

    public static List<CatalogueLineType> getCatalogueLines(String catalogueUuid, List<String> lineIds) {
        return new JPARepositoryFactory().forCatalogueRepository(true).getEntities(QUERY_GET_BY_CAT_UUID_AND_IDS, new String[]{"catalogueUuid", "lineIds"}, new Object[]{catalogueUuid, lineIds});
    }

    public static List<Object[]> getCatalogueUuidAndLines(List<String> catalogueUuid) {
        return new JPARepositoryFactory().forCatalogueRepository(true).getEntities(QUERY_GET_CAT_UUID_AND_IDS, new String[]{"catalogueUuid"}, new Object[]{catalogueUuid});
    }

    public static List<ItemLCPAInput> getLinesIdsWithValidLcpaInput() {
        List<Object[]> dbResults = new JPARepositoryFactory().forCatalogueRepository(true).getEntities(QUERY_GET_LINE_ITEMS_WITH_LCPA_INPUT_WITHOUT_LCPA_OUTPUT);
        List<ItemLCPAInput> results = new ArrayList<>();
        for (Object[] result : dbResults) {
            ItemLCPAInput itemLcpaInput = new ItemLCPAInput();
            ItemType item = (ItemType) result[1];

            byte[] bomTemplate = null;
            for (DocumentReferenceType documentReferenceType : item.getItemSpecificationDocumentReference()) {
                if(documentReferenceType.getDocumentType().contentEquals("BOM")){
                    BinaryObjectType bomTemplateBinaryObject = new BinaryContentService().retrieveContent(documentReferenceType.getAttachment().getEmbeddedDocumentBinaryObject().getUri());
                    bomTemplate = Base64.getEncoder().encode(bomTemplateBinaryObject.getValue());
                }
            }

            itemLcpaInput.setCatalogueLineHjid((result[0]).toString());
            itemLcpaInput.setLcpaInput(item.getLifeCyclePerformanceAssessmentDetails().getLCPAInput());
            itemLcpaInput.setBomTemplate(bomTemplate);
            results.add(itemLcpaInput);
        }
        return results;
    }

    public static List<ItemType> getItemTypeOfAllLines() {
        return new JPARepositoryFactory().forCatalogueRepository(true).getEntities(QUERY_GET_ALL_CATALOGUELINES_CLURI);
    }

    // this method returns an array of objects
    // first one is the hjid of the catalogue line
    // second one is the id of the party owning the catalogue line
    public static Object[] getCatalogueLineHjidAndPartyId(String catalogueUuid, String lineId){
        return new JPARepositoryFactory().forCatalogueRepository().getSingleEntity(QUERY_GET_HJID_AND_PARTY_ID_BY_CAT_UUID_AND_ID, new String[]{"catalogueUuid", "lineId"}, new Object[]{catalogueUuid, lineId});
    }

    /**
     * Retrieves catalogue lines with commodity classifications initialized inside the item object, for the given initial lines.
     */
    public static List<CatalogueLineType> getCatalogueLinesWithCommodityClassifications(String uuid) {
        return new JPARepositoryFactory().forCatalogueRepository().getEntities(QUERY_GET_CATALOGUE_LINES_WITH_COMMODITY_CLASSIFICATIONS,
                new String[]{"uuid"}, new Object[]{uuid});
    }


    /**
     * Returns binary content uris of the given catalogue lines. Extracts uris from all relevant collections inside the catalogue line or other subentities.
     */
    public static List<String> getBinaryObjectUris(List<CatalogueLineType> lines) {
        List<String> uris = new ArrayList<>();
        // uris for product images
        uris.addAll(new JPARepositoryFactory().forCatalogueRepository().getEntities(QUERY_GET_IMAGE_URIS_IN_LINES, new String[]{"lines"}, new Object[]{lines}));

        // uris for item specification documents
        uris.addAll(new JPARepositoryFactory().forCatalogueRepository().getEntities(QUERY_GET_ITEM_SPECIFICATION_DOCUMENT_URIS_IN_LINES, new String[]{"lines"}, new Object[]{lines}));

        // uris for the certificates
        uris.addAll(new JPARepositoryFactory().forCatalogueRepository().getEntities(QUERY_GET_ITEM_CERTIFICATE_DOCUMENT_URIS_IN_LINES, new String[]{"lines"}, new Object[]{lines}));
        return uris;
    }
}
