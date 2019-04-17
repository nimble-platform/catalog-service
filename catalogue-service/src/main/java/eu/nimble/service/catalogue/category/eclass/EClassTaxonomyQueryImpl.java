package eu.nimble.service.catalogue.category.eclass;

import eu.nimble.service.catalogue.category.TaxonomyEnum;
import eu.nimble.service.catalogue.category.TaxonomyQueryInterface;
import eu.nimble.service.model.solr.owl.IClassType;
import eu.nimble.service.model.solr.owl.IConcept;
import org.springframework.stereotype.Component;

/**
 * Created by suat on 08-Feb-19.
 */
@Component
public class EClassTaxonomyQueryImpl implements TaxonomyQueryInterface {
    @Override
    public TaxonomyEnum getTaxonomy() {
        return TaxonomyEnum.eClass;
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
        sb.append(IConcept.NAME_SPACE_FIELD).append(":\"").append(TaxonomyEnum.eClass.getNamespace()).append("\" AND ")
                .append(IClassType.LEVEL_FIELD).append(":4");
        return sb;
    }
}
