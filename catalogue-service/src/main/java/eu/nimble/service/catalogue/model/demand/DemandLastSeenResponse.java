package eu.nimble.service.catalogue.model.demand;

public class DemandLastSeenResponse {

    private Long lastSeenDemandId;
    private int newDemandCount;

    public DemandLastSeenResponse(Long lastSeenDemandId, int newDemandCount) {
        this.lastSeenDemandId = lastSeenDemandId;
        this.newDemandCount = newDemandCount;
    }

    public DemandLastSeenResponse() {
    }

    public Long getLastSeenDemandId() {
        return this.lastSeenDemandId;
    }

    public void setLastSeenDemandId(Long lastSeenDemandId) {
        this.lastSeenDemandId = lastSeenDemandId;
    }

    public int getNewDemandCount() {
        return this.newDemandCount;
    }

    public void setNewDemandCount(int newDemandCount) {
        this.newDemandCount = newDemandCount;
    }
}
