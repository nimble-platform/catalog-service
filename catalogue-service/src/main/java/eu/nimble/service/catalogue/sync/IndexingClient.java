package eu.nimble.service.catalogue.sync;

import com.fasterxml.jackson.core.type.TypeReference;
import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import com.mashape.unirest.request.HttpRequest;
import eu.nimble.service.catalogue.category.taxonomy.TaxonomyEnum;
import eu.nimble.service.catalogue.model.category.Category;
import eu.nimble.service.catalogue.util.ExecutionContext;
import eu.nimble.service.model.solr.SearchResult;
import eu.nimble.service.model.solr.item.ItemType;
import eu.nimble.service.model.solr.owl.ClassType;
import eu.nimble.service.model.ubl.catalogue.CatalogueType;
import eu.nimble.service.model.ubl.commonaggregatecomponents.CatalogueLineType;
import eu.nimble.utility.JsonSerializationUtility;
import jena.query;
import org.apache.commons.lang3.StringUtils;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocumentList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by suat on 23-Jan-19.
 */
@Component
public class IndexingClient {

    private static final Logger logger = LoggerFactory.getLogger(IndexingClient.class);

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
    @Autowired
    private SolrClient solrClient;


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
                    catalogue.getUUID(), catalogue.getProviderParty().getID(), serializedCatalogue, e);
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
                logger.info("Indexed Catalogue successfully. uuid: {}, party id: {}", catalogue.getUUID(), catalogue.getProviderParty().getID());

            } else {
                String serializedCatalogue = JsonSerializationUtility.serializeEntitySilently(catalogue);
                logger.error("Failed to transform Catalogue to index ItemType list. uuid: {}, party id: {}, indexing call status: {}, message: {}\nCatalogue: {}",
                        catalogue.getUUID(), catalogue.getProviderParty().getID(), response.getStatus(), response.getBody(), serializedCatalogue);
                return;
            }

        } catch (UnirestException e) {
            String serializedCatalogue = JsonSerializationUtility.serializeEntitySilently(catalogue);
            logger.error("Failed to transform Catalogue to index ItemType list. uuid: {}, party id: {}\nCatalogue: {}",
                    catalogue.getUUID(), catalogue.getProviderParty().getID(), serializedCatalogue, e);
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
                logger.info("Indexed CatalogueLine successfully. id: {}, name: {}, party id: {}", catalogueLine.getID(), catalogueLine.getGoodsItem().getItem().getName());

            } else {
                String serializedCatalogueLine = JsonSerializationUtility.serializeEntitySilently(catalogueLine);
                logger.error("Failed to index CatalogueLine. id: {}, name: {}, party id: {}, indexing call status: {}, message: {}\nLine:{}",
                        catalogueLine.getID(), catalogueLine.getGoodsItem().getItem().getName(), response.getStatus(), response.getBody(), serializedCatalogueLine);
            }

        } catch (UnirestException e) {
            String serializedCatalogueLine = JsonSerializationUtility.serializeEntitySilently(catalogueLine);
            logger.error("Failed to index CatalogueLine. id: {}, name: {}, party: {}\nLine: {}", catalogueLine.getID(), catalogueLine.getGoodsItem().getItem().getName(), catalogueLine.getGoodsItem().getItem().getManufacturerParty().getID(), serializedCatalogueLine, e);
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

    public ClassType getCategory(String uri) {
        try {
            HttpResponse<String> response;
            response = Unirest.get(indexingUrl + "/class")
                    .queryString("uri", uri)
                    .header(HttpHeaders.AUTHORIZATION, executionContext.getBearerToken())
                    .asString();

            if (response.getStatus() == HttpStatus.OK.value()) {
                try {
                    ClassType category = JsonSerializationUtility.getObjectMapper().readValue(response.getBody(), ClassType.class);
                    logger.info("Retrieved category with uri: {}", uri);
                    return category;

                } catch (IOException e) {
                    String msg = String.format("Failed to parse category with uri: %s, serialized category: %s", uri, response.getBody());
                    logger.error(msg, e);
                    throw new RuntimeException(msg, e);
                }

            } else {
                String msg = String.format("Failed to get category. uri: %s, indexing call status: %d, message: %s", uri, response.getStatus(), response.getBody());
                logger.error(msg);
                throw new RuntimeException(msg);
            }

        } catch (UnirestException e) {
            String msg = String.format("Failed to retrieve category with uri: %s", uri);
            logger.error(msg, e);
            throw new RuntimeException(msg, e);
        }
    }

    public List<Category> getProductCategories(String namespace, String lang, String name) {
        if (StringUtils.isEmpty(lang)) {
            lang = "en";
        }

        try {
            HttpRequest request = Unirest.get(indexingUrl + "/classes");
            if (StringUtils.isNotEmpty(namespace)) {
                request = request.queryString("nameSpace", namespace);
            }
            request = request
                    .queryString("q", name)
                    .queryString("lang", lang)
                    .queryString("rows", Integer.MAX_VALUE)
                    .header(HttpHeaders.AUTHORIZATION, executionContext.getBearerToken());

            HttpResponse<String> response = request.asString();
            if (response.getStatus() == HttpStatus.OK.value()) {
                try {
                    List<Category> results = new ArrayList<>();
                    SearchResult<ClassType> categories = JsonSerializationUtility.getObjectMapper().readValue(response.getBody(), new TypeReference<SearchResult<ClassType>>() {
                    });
                    for (ClassType indexCategory : categories.getResult()) {
                        results.add(IndexingWrapper.toCategory(indexCategory));
                    }

                    logger.info("Retrieved {} categories successfully. namespace: {}, name: {}, lang: {}", results.size(), namespace, name, lang);
                    return results;

                } catch (IOException e) {
                    logger.error("Failed to parse SearchResults with namespace: {}, name: {}, lang: {}, serialized result: {}", namespace, name, lang, response.getBody(), e);
                }

            } else {
                logger.error("Failed to retrieve categories. namespace: {}, name: {}, lang: {}, indexing call status: {}, message: {}",
                        namespace, name, lang, response.getStatus(), response.getBody());
            }

        } catch (UnirestException e) {
            logger.error("Failed to retrieve categories. namespace: {}, name: {}, lang: {}", namespace, name, lang, e);
        }

        return new ArrayList<>();
    }

    public List<Category> getLogisticsCategoriesForEClass(String name, String lang) {
        String labelField = null;
        if (StringUtils.isNotEmpty(name)) {
            if (StringUtils.isEmpty(lang)) {
                lang = "en";
            }
            labelField = "label_" + lang + ":" + name;
        }

        String namespaceField = "namespace:" + TaxonomyEnum.eClass.getNamespace();
        String localNameField = "localName:14*";
        String queryField = namespaceField + " AND " + localNameField;
        if(labelField != null) {
            queryField += " AND " + labelField;
        }

        SolrQuery query = new SolrQuery();
        query.set("q", queryField);

        List<Category> categories = new ArrayList<>();
        try {
            QueryResponse response = solrClient.query(query);
            List<ClassType> indexCategories = response.getBeans(ClassType.class);
            for (ClassType indexCategory : indexCategories) {
                categories.add(IndexingWrapper.toCategory(indexCategory));
            }

            logger.info("Retrieved {} eClass logistics categories. name: {}, lang: {}", categories.size(), name, lang);
            return categories;

        } catch (SolrServerException | IOException e) {
            logger.error("Failed to retrieve eClass logistics categories for query: {}", query, e);
        }

        return new ArrayList<>();
    }

    public List<Category> getLogisticsCategoriesForFurnitureOntology(String name, String lang) {
        String labelField = null;
        if (StringUtils.isNotEmpty(name)) {
            if (StringUtils.isEmpty(lang)) {
                lang = "en";
            }
            labelField = "label_" + lang + ":" + name;
        }

        String namespaceField = "namespace:" + TaxonomyEnum.FurnitureOntology.getNamespace();
        String parentsField = "allParents:http://www.aidimme.es/FurnitureSectorOntology.owl#LogisticsService";
        String queryField = namespaceField + " AND " + parentsField;
        if(labelField != null) {
            queryField += " AND " + labelField;
        }

        SolrQuery query = new SolrQuery();
        query.set("q", queryField);

        List<Category> categories = new ArrayList<>();
        try {
            QueryResponse response = solrClient.query(query);
            List<ClassType> indexCategories = response.getBeans(ClassType.class);
            for (ClassType indexCategory : indexCategories) {
                categories.add(IndexingWrapper.toCategory(indexCategory));
            }

            logger.info("Retrieved {} FurnitureOntology logistics categories. name: {}, lang: {}", categories.size(), name, lang);
            return categories;

        } catch (SolrServerException | IOException e) {
            logger.error("Failed to retrieve FurnitureOntology logistics categories for query: {}", query, e);
        }

        return new ArrayList<>();
    }
}
