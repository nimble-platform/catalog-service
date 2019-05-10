package eu.nimble.service.catalogue.category;

import java.util.Map;

/**
 * Created by suat on 08-Feb-19.
 */
public interface TaxonomyQueryInterface {

    TaxonomyEnum getTaxonomy();

    Map<String,String> getLogisticsServices();

    String getQuery(boolean forLogistics);
}
