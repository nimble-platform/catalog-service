package eu.nimble.service.catalogue.sync;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import eu.nimble.service.catalogue.model.category.Property;
import eu.nimble.service.catalogue.util.SpringBridge;
import eu.nimble.service.model.ubl.commonaggregatecomponents.ItemPropertyType;
import eu.nimble.service.model.ubl.commonbasiccomponents.TextType;
import org.apache.commons.io.IOUtils;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.impl.HttpSolrServer;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrInputDocument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class SolrClient {

    private static final Logger logger = LoggerFactory.getLogger(SolrClient.class);

    private static final String url = SpringBridge.getInstance().getCatalogueServiceConfig().getSolrURL() + SpringBridge.getInstance().getCatalogueServiceConfig().getSolrPropertyIndex();

    private static final String stringDataType = "http://www.w3.org/2001/XMLSchema#string";
    private static final String booleanDataType = "http://www.w3.org/2001/XMLSchema#boolean";
    private static final String doubleDataType = "http://www.w3.org/2001/XMLSchema#double";
    private static final String intDataType = "http://www.w3.org/2001/XMLSchema#int";
    private static final String binaryDataType = "http://www.w3.org/2001/XMLSchema#base64Binary";
    private static final String quantityDataType = "urn:oasis:names:specification:ubl:schema:xsd:CommonBasicComponents-2#QuantityType";

    public static void indexProperties(List<ItemPropertyType> properties){
        logger.info("Indexing properties...");

        HttpSolrServer client = new HttpSolrServer(url);

        try {
            List<SolrInputDocument> docs = new ArrayList<>();
            for (ItemPropertyType property:properties){
                // check whether the property is indexed or not
                SolrQuery query = new SolrQuery();
                query.setQuery("name:"+property.getName()+" AND range:\""+getType(property.getValueQualifier())+"\"");
                QueryResponse response = client.query(query);

                if(response.getResults().size() == 0){
                    SolrInputDocument doc = new SolrInputDocument();
                    String id = UUID.randomUUID().toString();
                    doc.addField("id",id);
                    doc.addField("name", property.getName());
                    doc.addField("label", property.getName());
                    String type = getType(property.getValueQualifier());
                    doc.addField("range",type);

                    docs.add(doc);
                }
            }

            if(docs.size() != 0){
                client.add(docs);
                client.commit();
            }
        }
        catch (Exception e){
            logger.error("Failed to index properties",e);
        }
        finally {
            client.shutdown();
        }
        logger.info("Indexed properties successfully");
    }

    public static void indexCatalogueProperties(){
        logger.info("Indexing catalogue properties...");
        InputStream inputStream = null;
        try {
            logger.debug("Reading CatalogueProperties.json ...");
            inputStream =  ClassLoader.getSystemClassLoader().getResourceAsStream("CatalogueProperties.json");
            String fileContent = IOUtils.toString(inputStream);

            logger.debug("Read CatalogueProperties.json");

            ObjectMapper objectMapper = new ObjectMapper();

            // Properties
            List<Property> properties = objectMapper.readValue(fileContent,new TypeReference<List<Property>>(){});

            logger.debug("Properties are created");

            HttpSolrServer client = new HttpSolrServer(url);
            List<SolrInputDocument> docs = new ArrayList<>();

            for (Property property:properties){
                SolrInputDocument doc = new SolrInputDocument();
                doc.addField("id",property.getId());
                doc.addField("name", property.getShortName());
                // get preferred names
                for(TextType type:property.getPreferredName()){
                    doc.addField("label_"+type.getLanguageID(), type.getValue());
                }
                doc.addField("range",property.getDataType());
                docs.add(doc);
            }

            logger.debug("SorlInputDocuments are created");

            if(docs.size() != 0){
                client.add(docs);
                client.commit();

                logger.debug("SolrInputDocuments are sent");
            }
        }
        catch (Exception e){
            logger.error("Failed to index catalogue properties",e);
        }
        finally {
            try {
                if(inputStream != null){
                    inputStream.close();
                }
            }
            catch (Exception e){
                logger.error("Failed to close input stream",e);
            }
        }
        logger.info("Indexed catalogue properties successfully");
    }

    private static String getType(String valueQualifier){
        if(valueQualifier.equals("STRING")){
            return stringDataType;
        }
        else if(valueQualifier.equals("NUMBER") || valueQualifier.equals("REAL_MEASURE") || valueQualifier.equals("DOUBLE")){
            return doubleDataType;
        }
        else if(valueQualifier.equals("INT")){
            return intDataType;
        }
        else if(valueQualifier.equals("BOOLEAN")){
            return booleanDataType;
        }
        else if(valueQualifier.equals("QUANTITY")){
            return quantityDataType;
        }
        else if(valueQualifier.equals("BINARY")){
            return binaryDataType;
        }
        throw new RuntimeException("Unknown data type for value qualifier: " + valueQualifier);
    }
}
