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

    private static final String url = SpringBridge.getInstance().getCatalogueServiceConfig().getSolrURL()+SpringBridge.getInstance().getCatalogueServiceConfig().getSolrIndex();
    private static final String customPropertyURI = "http://www.nimble-project.org/ontology/custom/property/";

    public static void indexProperties(List<ItemPropertyType> properties){
        logger.info("Indexing properties...");

        HttpSolrServer client = new HttpSolrServer(url);

        try {
            List<SolrInputDocument> docs = new ArrayList<>();
            for (ItemPropertyType property:properties){
                // check whether the property is indexed or not
                SolrQuery query = new SolrQuery();
                query.setQuery("name:"+property.getName()+" AND lmf.type:"+property.getValueQualifier());
                QueryResponse response = client.query(query);

                if(response.getResults().size() == 0){
                    SolrInputDocument doc = new SolrInputDocument();
                    String id = UUID.randomUUID().toString();
                    doc.addField("id",id);
                    doc.addField("name", property.getName());
                    doc.addField("label", property.getName());
                    doc.addField("lmf.type",property.getValueQualifier());
                    if(property.getItemClassificationCode().getListID().equals("Custom")){
                        doc.addField("lmf.uri",customPropertyURI+id);
                    }
                    else{
                        doc.addField("lmf.uri",property.getURI());
                    }
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
}
