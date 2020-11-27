package eu.nimble.service.catalogue;

import eu.nimble.service.model.ubl.commonaggregatecomponents.DemandType;
import org.hibernate.dialect.Ingres9Dialect;

import java.util.List;

public interface DemandIndexService {
    /**
     * Extracts the text information from the given {@link DemandType} instances and indexes it in the configured persistence mechanism
     * @param demand
     */
    void indexDemandText(DemandType demand);

    /**
     * Retrieves demand information with the query term
     * @param queryTerm
     * @param language
     */
    List<DemandType> searchDemand(String queryTerm, String language, Integer pageNo, Integer limit);
}
