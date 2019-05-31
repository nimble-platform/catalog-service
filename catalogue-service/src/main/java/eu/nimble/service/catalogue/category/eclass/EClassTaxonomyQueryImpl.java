package eu.nimble.service.catalogue.category.eclass;

import eu.nimble.service.catalogue.category.Taxonomy;
import eu.nimble.service.catalogue.category.TaxonomyQueryInterface;
import eu.nimble.service.model.solr.owl.IClassType;
import eu.nimble.service.model.solr.owl.IConcept;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by suat on 08-Feb-19.
 */
public class EClassTaxonomyQueryImpl implements TaxonomyQueryInterface {

    public static final String id = "eClass";
    public static final String namespace = "http://www.nimble-project.org/resource/eclass#";

    private Taxonomy taxonomy;

    public EClassTaxonomyQueryImpl(Taxonomy taxonomy) {
        this.taxonomy = taxonomy;
    }

    @Override
    public Taxonomy getTaxonomy() {
        return taxonomy;
    }

    @Override
    public Map<String, String> getLogisticsServices() {
        Map<String,String> logisticServiceCategoryUriMap = new HashMap<>();

        logisticServiceCategoryUriMap.put("MARITIMETRANSPORT", "http://www.nimble-project.org/resource/eclass#0173-1#01-AAB379#014");
        logisticServiceCategoryUriMap.put("AIRTRANSPORT", "http://www.nimble-project.org/resource/eclass#0173-1#01-ADU384#007");
        logisticServiceCategoryUriMap.put("RAILTRANSPORT", "http://www.nimble-project.org/resource/eclass#0173-1#01-AAB365#013");
        logisticServiceCategoryUriMap.put("WAREHOUSING", "http://www.nimble-project.org/resource/eclass#0173-1#01-ADU628#007");
        logisticServiceCategoryUriMap.put("ORDERPICKING", "http://www.nimble-project.org/resource/eclass#0173-1#01-AKG236#013");
        logisticServiceCategoryUriMap.put("LOGISTICSCONSULTANCY", "http://www.nimble-project.org/resource/eclass#0173-1#01-BAC130#011");

        return logisticServiceCategoryUriMap;
    }

    @Override
    public String getQuery(boolean forLogistics) {
        StringBuilder sb = new StringBuilder(commonQuery());
        if(forLogistics) {
            sb.append(" AND ").append(IClassType.CODE_FIELD).append(":14*");
        }
        StringBuilder finalQuery = new StringBuilder();
        finalQuery.append("(").append(sb).append(")");
        return finalQuery.toString();
    }

    private StringBuilder commonQuery() {
        StringBuilder sb = new StringBuilder("");
        sb.append(IConcept.NAME_SPACE_FIELD).append(":\"").append(EClassTaxonomyQueryImpl.namespace).append("\" AND ")
                .append(IClassType.LEVEL_FIELD).append(":4");
        return sb;
    }
}
