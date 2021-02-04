package eu.nimble.service.catalogue.category;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import eu.nimble.service.catalogue.category.eclass.EClassTaxonomyQueryImpl;
import eu.nimble.service.catalogue.category.furnitureontology.FurnitureOntologyTaxonomyQueryImpl;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;

@Component
public class TaxonomyManager {

    private static Logger logger = LoggerFactory.getLogger(TaxonomyManager.class);

    @Value("${nimble.catalog.category.enabled-taxonomies}")
    private String enabledTaxonomies;

    private Map<String,TaxonomyQueryInterface> taxonomiesMap = new HashMap<>();
    private List<String> serviceRootCategories = new ArrayList<>();
    private List<String> productRootCategories = new ArrayList<>();
    private List<String> rootCategories = new ArrayList<>();

    @PostConstruct
    public void initTaxonomyManager() throws Exception {
        // read the taxonomy configurations
        logger.info("Reading categories from Taxonomies.json file...");
        List<Taxonomy> taxonomies = new ArrayList<>();
        InputStream inputStream = TaxonomyManager.class.getResourceAsStream("/Taxonomies.json");
        String fileContent = null;
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            fileContent = IOUtils.toString(inputStream);
            taxonomies = objectMapper.readValue(fileContent,new TypeReference<List<Taxonomy>>(){});
            logger.info("Read categories from Taxonomies.json file");
        } catch (IOException e) {
            logger.error("Failed to read categories from Taxonomies.json file",e);
            throw e;
        } finally {
            if(inputStream != null){
                try {
                    inputStream.close();
                } catch (IOException e) {
                    logger.error("Failed to close input stream",e);
                    throw e;
                }
            }
        }

        // get the enabled taxonomies
        List<String> enabledTaxonomyIds = Arrays.asList(enabledTaxonomies.split(","));
        for (int i=0; i<enabledTaxonomyIds.size(); i++) {
            enabledTaxonomyIds.set(i, enabledTaxonomyIds.get(i).trim());
        }
        logger.info("Enabled taxonomies are: {}", enabledTaxonomyIds);

        // populate taxonomies map
        for(Taxonomy taxonomy:taxonomies){
            String taxonomyId = taxonomy.getId();
            // do not consider non-enabled taxonomies
            if (!enabledTaxonomyIds.contains(taxonomyId)) {
                continue;
            }

            if(taxonomy.getId().contentEquals("eClass")){
                taxonomiesMap.put(taxonomy.getId(),new EClassTaxonomyQueryImpl(taxonomy));
            }
            else if(taxonomy.getId().contentEquals("FurnitureOntology")){
                taxonomiesMap.put(taxonomy.getId(),new FurnitureOntologyTaxonomyQueryImpl(taxonomy));
            }
            else{
                taxonomiesMap.put(taxonomy.getId(),new DefaultTaxonomyQueryImpl(taxonomy));
            }

            // add service root categories for the taxonomy
            serviceRootCategories.addAll(taxonomy.getServiceRootCategories());
            // add product root categories for the taxonomy
            productRootCategories.addAll(taxonomy.getProductRootCategories());
            // add root categories for the taxonomy
            rootCategories.addAll(taxonomy.getServiceRootCategories());
            rootCategories.addAll(taxonomy.getProductRootCategories());

            logger.info("Parsed {} taxonomy metadata", taxonomy.getId());
        }
        logger.info("Created taxonomies map");
    }

    public Map<String, TaxonomyQueryInterface> getTaxonomiesMap() {
        return taxonomiesMap;
    }

    public List<String> getServiceRootCategories() {
        return serviceRootCategories;
    }

    public List<String> getProductRootCategories() {
        return productRootCategories;
    }

    public List<String> getRootCategories() {
        return rootCategories;
    }
}
