package eu.nimble.service.catalogue;

import eu.nimble.service.catalogue.model.demand.DemandFacetResponse;
import eu.nimble.service.catalogue.model.demand.DemandFacetValue;
import eu.nimble.service.model.ubl.commonaggregatecomponents.DemandType;

import java.util.List;

public interface DemandIndexService {
    /**
     * Extracts the text information from the given {@link DemandType} instances and indexes it in the configured persistence mechanism
     * @param demand
     */
    void indexDemandText(DemandType demand);

    /**
     * Retrieves {@link DemandType} instances for the given query parameters
     * @param queryTerm
     * @param lang
     * @param companyId
     * @param categoryUri
     * @param dueDate indicates' the latest due date for demands to be considered
     * @param buyerCountry
     * @param deliveryCountry
     * @param pageNo
     * @param limit
     * @return
     */
    List<DemandType> searchDemand(String queryTerm, String lang, String companyId, String categoryUri, String dueDate, String buyerCountry, String deliveryCountry, Integer pageNo, Integer limit,List<String> circularEconomyCertificates, List<String> otherCertificates);

    /**
     * Retrieves number of {@link DemandType}s for the given query parameters
     * @param queryTerm
     * @param lang
     * @param companyId
     * @param categoryUri
     * @param dueDate indicates' the latest due date for demands to be considered
     * @param buyerCountry
     * @param deliveryCountry
     * @return
     */
    int getDemandCount(String queryTerm, String lang, String companyId, String categoryUri, String dueDate, String buyerCountry, String deliveryCountry,List<String> circularEconomyCertificates, List<String> otherCertificates);

    List<DemandFacetResponse> getDemandFacets(String queryTerm, String lang, String companyId, String categoryUri, String dueDate, String buyerCountry, String deliveryCountry,
                                              List<String> circularEconomyCertificates, List<String> otherCertificates);
}
