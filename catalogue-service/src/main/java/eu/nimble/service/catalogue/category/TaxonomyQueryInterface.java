package eu.nimble.service.catalogue.category;

import java.util.Map;

/**
 * Created by suat on 08-Feb-19.
 */
public interface TaxonomyQueryInterface {

    TaxonomyEnum getTaxonomy();

    String getQuery(boolean forLogistics);
}
