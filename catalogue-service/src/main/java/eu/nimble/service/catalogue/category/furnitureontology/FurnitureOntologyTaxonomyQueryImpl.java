package eu.nimble.service.catalogue.category.furnitureontology;

import eu.nimble.service.catalogue.category.LogisticsServicesEnum;
import eu.nimble.service.catalogue.category.TaxonomyEnum;
import eu.nimble.service.catalogue.category.TaxonomyQueryInterface;
import eu.nimble.service.model.solr.owl.IClassType;
import eu.nimble.service.model.solr.owl.IConcept;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by suat on 08-Feb-19.
 */
@Component
public class FurnitureOntologyTaxonomyQueryImpl implements TaxonomyQueryInterface {
    private static final String FURNITURE_ONTOLOGY_LOGISTICS_SERVICE = "http://www.aidimme.es/FurnitureSectorOntology.owl#LogisticsService";

    @Override
    public TaxonomyEnum getTaxonomy() {
        return TaxonomyEnum.FurnitureOntology;
    }

    @Override
    public Map<String, String> getLogisticsServices() {
        Map<String,String> logisticServiceCategoryUriMap = new HashMap<>();
        for(LogisticsServicesEnum logisticsServicesEnum : LogisticsServicesEnum.values()){
            if(logisticsServicesEnum.getFurnitureOntologyCategoryUri() != null){
                logisticServiceCategoryUriMap.put(logisticsServicesEnum.getId(), logisticsServicesEnum.getFurnitureOntologyCategoryUri());
            }
        }
        return logisticServiceCategoryUriMap;
    }

    @Override
    public String getQuery(boolean forLogistics) {
        StringBuilder sb = new StringBuilder(commonQuery());
        if(forLogistics) {
            sb.append(" AND ").append(IClassType.ALL_PARENTS_FIELD).append(":\"").append(FURNITURE_ONTOLOGY_LOGISTICS_SERVICE).append("\"");
        }
        StringBuilder finalQuery = new StringBuilder();
        finalQuery.append("(").append(sb).append(")");
        return finalQuery.toString();
    }

    private StringBuilder commonQuery() {
        StringBuilder sb = new StringBuilder("");
        sb.append(IConcept.NAME_SPACE_FIELD).append(":\"").append(TaxonomyEnum.FurnitureOntology.getNamespace()).append("\"");
        return sb;
    }
}
