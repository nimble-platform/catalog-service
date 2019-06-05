package eu.nimble.service.catalogue.category;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import eu.nimble.service.catalogue.category.eclass.EClassTaxonomyQueryImpl;
import eu.nimble.service.catalogue.category.furnitureontology.FurnitureOntologyTaxonomyQueryImpl;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class TaxonomyManager {

    private static Logger logger = LoggerFactory.getLogger(TaxonomyManager.class);

    private Map<String,TaxonomyQueryInterface> taxonomiesMap = new HashMap<>();

    public TaxonomyManager() {
        InputStream inputStream = null;
        logger.info("Reading categories from Taxonomies.json file...");
        inputStream =  TaxonomyManager.class.getResourceAsStream("/Taxonomies.json");
        String fileContent = null;
        try {
            fileContent = IOUtils.toString(inputStream);
        } catch (IOException e) {
            logger.error("Failed to read categories from Taxonomies.json file",e);
        } finally {
            if(inputStream != null){
                try {
                    inputStream.close();
                } catch (IOException e) {
                    logger.error("Failed to close input stream",e);
                }
            }
        }

        ObjectMapper objectMapper = new ObjectMapper();

        // Taxonomies
        try {
            List<Taxonomy> taxonomies = objectMapper.readValue(fileContent,new TypeReference<List<Taxonomy>>(){});
            // populate taxonomies map
            for(Taxonomy taxonomy:taxonomies){
                if(taxonomy.getId().contentEquals("eClass")){
                    taxonomiesMap.put(taxonomy.getId(),new EClassTaxonomyQueryImpl(taxonomy));
                }
                else if(taxonomy.getId().contentEquals("FurnitureOntology")){
                    taxonomiesMap.put(taxonomy.getId(),new FurnitureOntologyTaxonomyQueryImpl(taxonomy));
                }
                else{
                    taxonomiesMap.put(taxonomy.getId(),new DefaultTaxonomyQueryImpl(taxonomy));
                }
                logger.info("Parsed {} taxonomy metadata", taxonomy.getId());
            }
            logger.info("Created taxonomies map");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public Map<String, TaxonomyQueryInterface> getTaxonomiesMap() {
        return taxonomiesMap;
    }
}
