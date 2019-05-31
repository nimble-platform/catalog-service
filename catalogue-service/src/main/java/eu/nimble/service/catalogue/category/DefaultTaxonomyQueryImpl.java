package eu.nimble.service.catalogue.category;

import eu.nimble.service.model.solr.owl.IClassType;
import eu.nimble.service.model.solr.owl.IConcept;

import java.util.HashMap;
import java.util.Map;

public class DefaultTaxonomyQueryImpl implements TaxonomyQueryInterface {

    private Taxonomy taxonomy;

    public DefaultTaxonomyQueryImpl(Taxonomy taxonomy) {
        this.taxonomy = taxonomy;
    }

    @Override
    public Taxonomy getTaxonomy() {
        return taxonomy;
    }

    @Override
    public Map<String, String> getLogisticsServices() {
        Map<String,String> logisticServiceCategoryUriMap = new HashMap<>();

        return logisticServiceCategoryUriMap;
    }

    @Override
    public String getQuery(boolean forLogistics) {
        StringBuilder sb = new StringBuilder(commonQuery());
        if(forLogistics) {
            sb.append(" AND ").append(IClassType.ALL_PARENTS_FIELD).append(":\"").append(taxonomy.getId()).append("\"");
        }
        StringBuilder finalQuery = new StringBuilder();
        finalQuery.append("(").append(sb).append(")");
        return finalQuery.toString();
    }

    private StringBuilder commonQuery() {
        StringBuilder sb = new StringBuilder("");
        sb.append(IConcept.NAME_SPACE_FIELD).append(":\"").append(taxonomy.getNamespace()).append("\"");
        return sb;
    }
}
