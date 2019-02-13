package eu.nimble.service.catalogue.index;

import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import eu.nimble.service.catalogue.util.ExecutionContext;
import eu.nimble.service.model.solr.item.ItemType;
import eu.nimble.service.model.ubl.catalogue.CatalogueType;
import eu.nimble.service.model.ubl.commonaggregatecomponents.CatalogueLineType;
import eu.nimble.utility.JsonSerializationUtility;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by suat on 23-Jan-19.
 */
@Component
public class ItemIndexClient {

    private static final Logger logger = LoggerFactory.getLogger(ItemIndexClient.class);

    @Value("${nimble.indexing.url}")
    private String indexingUrl;
    @Value("${nimble.indexing.solr.url}")
    private String solrUrl;
    @Value("${nimble.indexing.solr.username}")
    private String solrUsername;
    @Value("${nimble.indexing.solr.password}")
    private String solrPassword;

    @Autowired
    private ExecutionContext executionContext;

    public void indexCatalogue(CatalogueType catalogue) {
        HttpResponse<String> response;
        String indexItemsJson;
        try {
            List<ItemType> indexItems = new ArrayList<>();
            for (CatalogueLineType catalogueLine : catalogue.getCatalogueLine()) {
                indexItems.add(IndexingWrapper.toIndexItem(catalogueLine));
            }
            indexItemsJson = JsonSerializationUtility.getObjectMapper().writeValueAsString(indexItems);

        } catch (Exception e) {
            String serializedCatalogue = JsonSerializationUtility.serializeEntitySilently(catalogue);
            logger.error("Failed to transform Catalogue to index ItemType list. uuid: {}, party id: {}\n catalogue: {}",
                    catalogue.getUUID(), catalogue.getProviderParty().getPartyIdentification().get(0).getID(), serializedCatalogue, e);
            return;
        }

        try {
            response = Unirest.post(indexingUrl + "/catalogue")
                    .header(HttpHeaders.AUTHORIZATION, executionContext.getBearerToken())
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .queryString("catalogueId", catalogue.getUUID())
                    .body(indexItemsJson)
                    .asString();

            if (response.getStatus() == HttpStatus.OK.value()) {
                logger.info("Indexed Catalogue successfully. uuid: {}, party id: {}", catalogue.getUUID(), catalogue.getProviderParty().getPartyIdentification().get(0).getID());

            } else {
                String serializedCatalogue = JsonSerializationUtility.serializeEntitySilently(catalogue);
                logger.error("Failed to index Catalogue. uuid: {}, party id: {}, indexing call status: {}, message: {}\nCatalogue: {}",
                        catalogue.getUUID(), catalogue.getProviderParty().getPartyIdentification().get(0).getID(), response.getStatus(), response.getBody(), serializedCatalogue);
                return;
            }

        } catch (UnirestException e) {
            String serializedCatalogue = JsonSerializationUtility.serializeEntitySilently(catalogue);
            logger.error("Failed to index Catalogue to index ItemType list. uuid: {}, party id: {}\nCatalogue: {}",
                    catalogue.getUUID(), catalogue.getProviderParty().getPartyIdentification().get(0).getID(), serializedCatalogue, e);
            return;
        }
    }

    public void indexCatalogueLine(CatalogueLineType catalogueLine) {
        HttpResponse<String> response;
        String indexItemJson;
        try {
            ItemType indexItem = IndexingWrapper.toIndexItem(catalogueLine);
            indexItemJson = JsonSerializationUtility.getObjectMapper().writeValueAsString(indexItem);

        } catch (Exception e) {
            String serializedCatalogueLine = JsonSerializationUtility.serializeEntitySilently(catalogueLine);
            logger.error("Failed to transform CatalogueLine to index ItemType. id: {}, name: {}, party id: {}\nLine: {}",
                    catalogueLine.getID(), catalogueLine.getGoodsItem().getItem().getName(), serializedCatalogueLine, e);
            return;
        }

        try {
            response = Unirest.post(indexingUrl + "/item")
                    .header(HttpHeaders.AUTHORIZATION, executionContext.getBearerToken())
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .body(indexItemJson)
                    .asString();

            if (response.getStatus() == HttpStatus.OK.value()) {
                logger.info("Indexed CatalogueLine successfully. hjid: {}, name: {}, party id: {}", catalogueLine.getHjid(), catalogueLine.getGoodsItem().getItem().getName(), catalogueLine.getGoodsItem().getItem().getManufacturerParty().getPartyIdentification().get(0).getID());

            } else {
                String serializedCatalogueLine = JsonSerializationUtility.serializeEntitySilently(catalogueLine);
                logger.error("Failed to index CatalogueLine. id: {}, name: {}, party id: {}, indexing call status: {}, message: {}\nLine:{}",
                        catalogueLine.getID(), catalogueLine.getGoodsItem().getItem().getName(), response.getStatus(), response.getBody(), serializedCatalogueLine);
            }

        } catch (UnirestException e) {
            String serializedCatalogueLine = JsonSerializationUtility.serializeEntitySilently(catalogueLine);
            logger.error("Failed to index CatalogueLine. id: {}, name: {}, party: {}\nLine: {}", catalogueLine.getID(), catalogueLine.getGoodsItem().getItem().getName(), catalogueLine.getGoodsItem().getItem().getManufacturerParty().getPartyIdentification().get(0).getID(), serializedCatalogueLine, e);
        }
    }

    public void deleteCatalogue(String catalogueUuid) {
        try {
            HttpResponse<String> response;
            response = Unirest.delete(indexingUrl + "/catalogue")
                    .header(HttpHeaders.AUTHORIZATION, executionContext.getBearerToken())
                    .queryString("catalogueId", catalogueUuid)
                    .asString();

            if (response.getStatus() == HttpStatus.OK.value()) {
                logger.info("Deleted indexed Catalogue. uuid: {}", catalogueUuid);

            } else {
                logger.error("Failed to delete indexed Catalogue. uuid: {}, indexing call status: {}, message: {}",
                        catalogueUuid, response.getStatus(), response.getBody());
            }

        } catch (UnirestException e) {
            logger.error("Failed to delete indexed Catalogue. uuid: {}", catalogueUuid, e);
        }
    }

    public void deleteCatalogueLine(long catalogueLineHjid) {
        try {
            HttpResponse<String> response;
            response = Unirest.delete(indexingUrl + "/item")
                    .queryString("uri", catalogueLineHjid)
                    .header(HttpHeaders.AUTHORIZATION, executionContext.getBearerToken())
                    .asString();

            if (response.getStatus() == HttpStatus.OK.value()) {
                logger.info("Deleted indexed CatalogueLine. hjid: {}", catalogueLineHjid);

            } else {
                logger.error("Failed to delete indexed CatalogueLine. hjid: {}, indexing call status: {}, message: {}",
                        catalogueLineHjid, response.getStatus(), response.getBody());
            }

        } catch (UnirestException e) {
            logger.error("Failed to delete indexed CatalogueLine. hjid: {}", catalogueLineHjid, e);
        }
    }
}
