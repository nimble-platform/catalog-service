package eu.nimble.service.catalogue.category.furnitureontology;

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

        logisticServiceCategoryUriMap.put("ROADTRANSPORT", "http://www.aidimme.es/FurnitureSectorOntology.owl#RoadTransportService");
        logisticServiceCategoryUriMap.put("MARITIMETRANSPORT","http://www.aidimme.es/FurnitureSectorOntology.owl#MaritimeTransportService");
        logisticServiceCategoryUriMap.put("AIRTRANSPORT","http://www.aidimme.es/FurnitureSectorOntology.owl#AirTransportService");
        logisticServiceCategoryUriMap.put("RAILTRANSPORT","http://www.aidimme.es/FurnitureSectorOntology.owl#RailTransportService");
        logisticServiceCategoryUriMap.put("WAREHOUSING","http://www.aidimme.es/FurnitureSectorOntology.owl#WarehousingService");
        logisticServiceCategoryUriMap.put("ORDERPICKING","http://www.aidimme.es/FurnitureSectorOntology.owl#OrderPickingService");
        logisticServiceCategoryUriMap.put("REVERSELOGISTICS","http://www.aidimme.es/FurnitureSectorOntology.owl#ReverseLogisticsService");
        logisticServiceCategoryUriMap.put("INHOUSESERVICES","http://www.aidimme.es/FurnitureSectorOntology.owl#InHouseService");
        logisticServiceCategoryUriMap.put("CUSTOMSMANAGEMENT","http://www.aidimme.es/FurnitureSectorOntology.owl#CustomsManagementService");
        logisticServiceCategoryUriMap.put("LOGISTICSCONSULTANCY","http://www.aidimme.es/FurnitureSectorOntology.owl#LogisticsConsultancyService");

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
