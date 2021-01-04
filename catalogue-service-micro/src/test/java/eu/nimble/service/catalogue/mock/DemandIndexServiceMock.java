package eu.nimble.service.catalogue.mock;

import eu.nimble.service.catalogue.DemandIndexService;
import eu.nimble.service.catalogue.model.demand.DemandFacetResponse;
import eu.nimble.service.catalogue.model.demand.DemandFacetValue;
import eu.nimble.service.model.ubl.commonaggregatecomponents.DemandType;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@Profile("test")
public class DemandIndexServiceMock implements DemandIndexService {
    @Override
    public void indexDemandText(DemandType demand) {

    }

    @Override
    public List<DemandType> searchDemand(String queryTerm, String lang, String companyId, String categoryUri, String dueDate, String buyerCountry, String deliveryCountry, Integer pageNo, Integer limit) {
        return null;
    }

    @Override
    public int getDemandCount(String queryTerm, String lang, String companyId, String categoryUri, String dueDate, String buyerCountry, String deliveryCountry) {
        return 0;
    }

    @Override
    public List<DemandFacetResponse> getDemandFacets(String queryTerm, String lang, String companyId, String categoryUri, String dueDate, String buyerCountry, String deliveryCountry) {
        return null;
    }
}
