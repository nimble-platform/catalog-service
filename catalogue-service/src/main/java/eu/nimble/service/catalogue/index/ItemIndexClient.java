package eu.nimble.service.catalogue.index;

import eu.nimble.common.rest.indexing.IIndexingServiceClient;
import eu.nimble.service.catalogue.persistence.util.CataloguePersistenceUtil;
import eu.nimble.service.catalogue.util.CredentialsUtil;
import eu.nimble.service.model.solr.item.ItemType;
import eu.nimble.service.model.ubl.catalogue.CatalogueType;
import eu.nimble.service.model.ubl.commonaggregatecomponents.CatalogueLineType;
import eu.nimble.utility.JsonSerializationUtility;
import feign.Response;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
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
    IndexingClientController indexingClientController;

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

            List<IIndexingServiceClient> clients = indexingClientController.getClients();
            for (IIndexingServiceClient client : clients) {
                response = client.postCatalogue(credentialsUtil.getBearerToken(),catalogue.getUUID(),indexItemsJson);
                if (response.status() == HttpStatus.OK.value()) {
                    logger.info("Indexed Catalogue successfully. uuid: {}, party id: {}", catalogue.getUUID(), catalogue.getProviderParty().getPartyIdentification().get(0).getID());

                } else {
                    String serializedCatalogue = JsonSerializationUtility.serializeEntitySilently(catalogue);
                    logger.error("Failed to index Catalogue. uuid: {}, party id: {}, indexing call status: {}, message: {}\nCatalogue: {}",
                            catalogue.getUUID(), catalogue.getProviderParty().getPartyIdentification().get(0).getID(), response.status(), IOUtils.toString(response.body().asInputStream()), serializedCatalogue);
                }
            }
            //response = SpringBridge.getInstance().getiIndexingServiceClient().postCatalogue(credentialsUtil.getBearerToken(),catalogue.getUUID(),indexItemsJson);

        } catch (Exception e) {
            String serializedCatalogue = JsonSerializationUtility.serializeEntitySilently(catalogue);
            logger.error("Failed to index Catalogue to index ItemType list. uuid: {}, party id: {}\nCatalogue: {}",
                    catalogue.getUUID(), catalogue.getProviderParty().getPartyIdentification().get(0).getID(), serializedCatalogue, e);
            return;
        }
    }

    public void indexAllCatalogues() {
        List<CatalogueType> catalogues = CataloguePersistenceUtil.getAllCataloguesExceptCarts();
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
            List<IIndexingServiceClient> clients = indexingClientController.getClients();
            for (IIndexingServiceClient client : clients) {
                response = client.setItem(credentialsUtil.getBearerToken(),indexItemJson);
                if (response.status() == HttpStatus.OK.value()) {
                    logger.info("Indexed CatalogueLine successfully. hjid: {}, name: {}, party id: {}", catalogueLine.getHjid(), catalogueLine.getGoodsItem().getItem().getName(), catalogueLine.getGoodsItem().getItem().getManufacturerParty().getPartyIdentification().get(0).getID());
                } else {
                    String serializedCatalogueLine = JsonSerializationUtility.serializeEntitySilently(catalogueLine);
                    logger.error("Failed to index CatalogueLine. id: {}, name: {}, party id: {}, indexing call status: {}, message: {}\nLine:{}",
                            catalogueLine.getID(), catalogueLine.getGoodsItem().getItem().getName(), catalogueLine.getGoodsItem().getItem().getManufacturerParty().getPartyIdentification().get(0).getID(), response.status(), IOUtils.toString(response.body().asInputStream()), serializedCatalogueLine);
                }
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
            List<IIndexingServiceClient> clients = indexingClientController.getClients();
            for (IIndexingServiceClient client : clients) {
                Response response = client.deleteCatalogue(credentialsUtil.getBearerToken(),catalogueUuid);
                if (response.status() == HttpStatus.OK.value()) {
                    logger.info("Deleted indexed Catalogue. uuid: {}", catalogueUuid);

                } else {
                    logger.error("Failed to delete indexed Catalogue. uuid: {}, indexing call status: {}, message: {}",
                            catalogueUuid, response.status(), IOUtils.toString(response.body().asInputStream()));
                }
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
            List<IIndexingServiceClient> clients = indexingClientController.getClients();
            for (IIndexingServiceClient client : clients) {
                Response response = client.deleteItem(credentialsUtil.getBearerToken(),Long.toString(catalogueLineHjid));

                if (response.status() == HttpStatus.OK.value()) {
                    logger.info("Deleted indexed CatalogueLine. hjid: {}", catalogueLineHjid);

                } else {
                    logger.error("Failed to delete indexed CatalogueLine. hjid: {}, indexing call status: {}, message: {}",
                            catalogueLineHjid, response.status(), IOUtils.toString(response.body().asInputStream()));
                }
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
            List<IIndexingServiceClient> clients = indexingClientController.getClients();
            for (IIndexingServiceClient client : clients) {
                Response response = client.clearItemIndex(credentialsUtil.getBearerToken());
                if (response.status() == HttpStatus.OK.value()) {
                    logger.info("Cleared item index");

                } else {
                    logger.error("Failed to clear item index, indexing call status: {}, message: {}",
                            response.status(), IOUtils.toString(response.body().asInputStream()));
                }
            }

        } catch (Exception e) {
            logger.error("Failed to clear item index", e);
        }
    }
}
