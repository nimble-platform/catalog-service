package eu.nimble.service.catalogue.index;

import eu.nimble.service.catalogue.persistence.util.CataloguePersistenceUtil;
import eu.nimble.service.catalogue.util.CredentialsUtil;
import eu.nimble.service.catalogue.util.SpringBridge;
import eu.nimble.service.model.solr.item.ItemType;
import eu.nimble.service.model.ubl.catalogue.CatalogueType;
import eu.nimble.service.model.ubl.commonaggregatecomponents.CatalogueLineType;
import eu.nimble.utility.JsonSerializationUtility;
import feign.Response;
import org.apache.commons.io.IOUtils;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.HttpSolrClient;
import org.apache.solr.client.solrj.response.UpdateResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by suat on 23-Jan-19.
 */
@Component
public class ItemIndexClient {

    private static final Logger logger = LoggerFactory.getLogger(ItemIndexClient.class);

    @Value("${nimble.indexing.sync}")
    private Boolean indexingSync;

    @Autowired
    private CredentialsUtil credentialsUtil;
    @Autowired
    private HttpSolrClient httpSolrClient;

    public void indexCatalogue(CatalogueType catalogue) {
        if(!indexingSync) {
            logger.info("Synchronization with Solr disabled. Won't index the catalogue");
            return;
        }
        Response response;
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
            response = SpringBridge.getInstance().getiIndexingServiceClient().postCatalogue(credentialsUtil.getBearerToken(),catalogue.getUUID(),indexItemsJson);

            if (response.status() == HttpStatus.OK.value()) {
                logger.info("Indexed Catalogue successfully. uuid: {}, party id: {}", catalogue.getUUID(), catalogue.getProviderParty().getPartyIdentification().get(0).getID());

            } else {
                String serializedCatalogue = JsonSerializationUtility.serializeEntitySilently(catalogue);
                logger.error("Failed to index Catalogue. uuid: {}, party id: {}, indexing call status: {}, message: {}\nCatalogue: {}",
                        catalogue.getUUID(), catalogue.getProviderParty().getPartyIdentification().get(0).getID(), response.status(), IOUtils.toString(response.body().asInputStream()), serializedCatalogue);
                return;
            }

        } catch (Exception e) {
            String serializedCatalogue = JsonSerializationUtility.serializeEntitySilently(catalogue);
            logger.error("Failed to index Catalogue to index ItemType list. uuid: {}, party id: {}\nCatalogue: {}",
                    catalogue.getUUID(), catalogue.getProviderParty().getPartyIdentification().get(0).getID(), serializedCatalogue, e);
            return;
        }
    }

    public void indexAllCatalogues() {
        List<CatalogueType> catalogues = CataloguePersistenceUtil.getAllCatalogues();
        for(CatalogueType catalogue : catalogues) {
            indexCatalogue(catalogue);
        }
        logger.info("All catalogues are indexed");
    }

    public void indexCatalogueLine(CatalogueLineType catalogueLine) {
        if(!indexingSync) {
            logger.info("Synchronization with Solr disabled. Won't index the catalogue line");
            return;
        }

        Response response;
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
            response = SpringBridge.getInstance().getiIndexingServiceClient().setItem(credentialsUtil.getBearerToken(),indexItemJson);

            if (response.status() == HttpStatus.OK.value()) {
                logger.info("Indexed CatalogueLine successfully. hjid: {}, name: {}, party id: {}", catalogueLine.getHjid(), catalogueLine.getGoodsItem().getItem().getName(), catalogueLine.getGoodsItem().getItem().getManufacturerParty().getPartyIdentification().get(0).getID());

            } else {
                String serializedCatalogueLine = JsonSerializationUtility.serializeEntitySilently(catalogueLine);
                logger.error("Failed to index CatalogueLine. id: {}, name: {}, party id: {}, indexing call status: {}, message: {}\nLine:{}",
                        catalogueLine.getID(), catalogueLine.getGoodsItem().getItem().getName(), catalogueLine.getGoodsItem().getItem().getManufacturerParty().getPartyIdentification().get(0).getID(), response.status(), IOUtils.toString(response.body().asInputStream()), serializedCatalogueLine);
            }

        } catch (Exception e) {
            String serializedCatalogueLine = JsonSerializationUtility.serializeEntitySilently(catalogueLine);
            logger.error("Failed to index CatalogueLine. id: {}, name: {}, party id: {}\nLine: {}", catalogueLine.getID(), catalogueLine.getGoodsItem().getItem().getName(), catalogueLine.getGoodsItem().getItem().getManufacturerParty().getPartyIdentification().get(0).getID(), serializedCatalogueLine, e);
        }
    }

    public void deleteCatalogue(String catalogueUuid) {
        if(!indexingSync) {
            logger.info("Synchronization with Solr disabled. Won't delete the catalogue");
            return;
        }

        try {
            Response response = SpringBridge.getInstance().getiIndexingServiceClient().deleteCatalogue(credentialsUtil.getBearerToken(),catalogueUuid);

            if (response.status() == HttpStatus.OK.value()) {
                logger.info("Deleted indexed Catalogue. uuid: {}", catalogueUuid);

            } else {
                logger.error("Failed to delete indexed Catalogue. uuid: {}, indexing call status: {}, message: {}",
                        catalogueUuid, response.status(), IOUtils.toString(response.body().asInputStream()));
            }

        } catch (Exception e) {
            logger.error("Failed to delete indexed Catalogue. uuid: {}", catalogueUuid, e);
        }
    }

    public void deleteCatalogueLine(long catalogueLineHjid) {
        if(!indexingSync) {
            logger.info("Synchronization with Solr disabled. Won't delete the catalogue line");
            return;
        }

        try {
            Response response = SpringBridge.getInstance().getiIndexingServiceClient().deleteItem(credentialsUtil.getBearerToken(),Long.toString(catalogueLineHjid));

            if (response.status() == HttpStatus.OK.value()) {
                logger.info("Deleted indexed CatalogueLine. hjid: {}", catalogueLineHjid);

            } else {
                logger.error("Failed to delete indexed CatalogueLine. hjid: {}, indexing call status: {}, message: {}",
                        catalogueLineHjid, response.status(), IOUtils.toString(response.body().asInputStream()));
            }

        } catch (Exception e) {
            logger.error("Failed to delete indexed CatalogueLine. hjid: {}", catalogueLineHjid, e);
        }
    }

    public void deleteAllContent() {
        if(!indexingSync) {
            logger.info("Synchronization with Solr disabled. Won't delete the content");
            return;
        }

        try {
            logger.info("Clearing the item index content");
            UpdateResponse response = httpSolrClient.deleteByQuery("*:*");
            logger.info("Delete query response: {}", response.getStatus());
            response = httpSolrClient.commit();
            logger.info("Cleared the item index content. Commit response: {}", response.getStatus());

        } catch (SolrServerException | IOException e) {
            logger.error("Failed to clear the index content", e);
        }
    }
}
