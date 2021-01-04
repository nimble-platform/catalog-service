package eu.nimble.service.catalogue.model.demand;

import eu.nimble.service.model.ubl.commonaggregatecomponents.DemandType;

import java.util.List;

public class DemandPaginationResponse {
    private int totalCount;
    private List<DemandType> demands;

    public DemandPaginationResponse(int totalCount, List<DemandType> demands) {
        this.totalCount = totalCount;
        this.demands = demands;
    }

    public int getTotalCount() {
        return totalCount;
    }

    public void setTotalCount(int totalCount) {
        this.totalCount = totalCount;
    }

    public List<DemandType> getDemands() {
        return demands;
    }

    public void setDemands(List<DemandType> demands) {
        this.demands = demands;
    }
}
