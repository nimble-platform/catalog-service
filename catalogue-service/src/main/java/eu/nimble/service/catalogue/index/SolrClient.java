//package eu.nimble.service.catalogue.sync;
//
//import eu.nimble.service.catalogue.util.SpringBridge;
//import eu.nimble.service.model.ubl.catalogue.CatalogueType;
//import eu.nimble.service.model.ubl.commonaggregatecomponents.CatalogueLineType;
//import eu.nimble.service.model.ubl.commonaggregatecomponents.ItemPropertyType;
//import org.apache.solr.client.solrj.SolrQuery;
//import org.apache.solr.client.solrj.impl.HttpSolrServer;
//import org.apache.solr.client.solrj.response.QueryResponse;
//import org.apache.solr.common.SolrInputDocument;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//
//import java.util.ArrayList;
//import java.util.List;
//import java.util.UUID;
//
//public class SolrClient {
//
//    private static final Logger logger = LoggerFactory.getLogger(SolrClient.class);
//
//    private static final String url = SpringBridge.getInstance().getCatalogueServiceConfig().getSolrURL() + SpringBridge.getInstance().getCatalogueServiceConfig().getSolrPropertyIndex();
//
//    private static final String stringDataType = "http://www.w3.org/2001/XMLSchema#string";
//    private static final String booleanDataType = "http://www.w3.org/2001/XMLSchema#boolean";
//    private static final String doubleDataType = "http://www.w3.org/2001/XMLSchema#double";
//    private static final String intDataType = "http://www.w3.org/2001/XMLSchema#int";
//    private static final String binaryDataType = "http://www.w3.org/2001/XMLSchema#base64Binary";
//    private static final String quantityDataType = "urn:oasis:names:specification:ubl:schema:xsd:CommonBasicComponents-2#QuantityType";
//
//    public static void indexProperties(CatalogueType catalogue){
//        logger.info("Indexing properties for catalogue: {}", catalogue.getUUID());
//
//        HttpSolrServer client = null;
//
//        try {
//            client = new HttpSolrServer(url);
//            for (CatalogueLineType catalogueLine : catalogue.getCatalogueLine()) {
//                List<SolrInputDocument> docs = new ArrayList<>();
//                for (ItemPropertyType property : catalogueLine.getGoodsItem().getItem().getAdditionalItemProperty()) {
//                    // check whether the property is indexed or not
//                    SolrQuery query = new SolrQuery();
//                    query.setQuery("name:" + property.getName() + " AND range:\"" + getType(property.getValueQualifier()) + "\"");
//                    QueryResponse response = client.query(query);
//
//                    if (response.getResults().size() == 0) {
//                        SolrInputDocument doc = new SolrInputDocument();
//                        String id = UUID.randomUUID().toString();
//                        doc.addField("id", id);
//                        doc.addField("name", property.getName());
//                        doc.addField("label", property.getName());
//                        String type = getType(property.getValueQualifier());
//                        doc.addField("range", type);
//
//                        docs.add(doc);
//                    }
//                }
//
//                if (docs.size() != 0) {
//                    client.add(docs);
//                    client.commit();
//                }
//            }
//
//            logger.info("Indexed properties successfully for catalogue: {}", catalogue.getUUID());
//        }
//        catch (Exception e){
//            logger.error("Failed to index properties",e);
//        }
//        finally {
//            if(client != null) {
//                client.shutdown();
//            }
//        }
//    }
//
//    private static String getType(String valueQualifier){
//        if(valueQualifier.equals("STRING")){
//            return stringDataType;
//        }
//        else if(valueQualifier.equals("NUMBER") || valueQualifier.equals("REAL_MEASURE") || valueQualifier.equals("DOUBLE")){
//            return doubleDataType;
//        }
//        else if(valueQualifier.equals("INT")){
//            return intDataType;
//        }
//        else if(valueQualifier.equals("BOOLEAN")){
//            return booleanDataType;
//        }
//        else if(valueQualifier.equals("QUANTITY")){
//            return quantityDataType;
//        }
//        else if(valueQualifier.equals("BINARY")){
//            return binaryDataType;
//        }
//        throw new RuntimeException("Unknown data type for value qualifier: " + valueQualifier);
//    }
//
//}
