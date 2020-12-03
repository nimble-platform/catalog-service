package eu.nimble.service.catalogue.persistence.util;

import eu.nimble.service.model.ubl.commonaggregatecomponents.DemandType;
import eu.nimble.utility.persistence.JPARepositoryFactory;

import java.util.List;

public class DemandPersistenceUtil {
    private static final String QUERY_GET_DEMANDS_FOR_COMPANY =
            "SELECT demand FROM DemandType demand" +
                    " JOIN demand.metadata metadata" +
                    " JOIN metadata.ownerCompanyItems ownerCompanies" +
                    " WHERE ownerCompanies.item = :companyId" +
                    " ORDER BY metadata.modificationDateItem DESC";
    private static final String QUERY_GET_DEMAND_COUNT =
            "SELECT COUNT(demand.hjid) FROM DemandType demand" +
            " JOIN demand.metadata metadata" +
            " JOIN metadata.ownerCompanyItems ownerCompanies" +
            " WHERE ownerCompanies.item = :companyId";
    private static final String QUERY_GET_DEMANDS_FOR_HJIDS = "SELECT demand FROM DemandType demand WHERE demand.hjid IN :hjids";


    public static List<DemandType> getDemandsForParty(String companyId, Integer pageNo, Integer limit) {
        return new JPARepositoryFactory().forCatalogueRepository(true)
                .getEntities(QUERY_GET_DEMANDS_FOR_COMPANY, new String[]{"companyId"}, new Object[]{companyId}, limit, limit * pageNo);
    }

    public static int getDemandCount(String companyId) {
        Long count = new JPARepositoryFactory().forCatalogueRepository().getSingleEntity(QUERY_GET_DEMAND_COUNT, new String[]{"companyId"}, new Object[]{companyId});
        return count.intValue();
    }

    public static List<DemandType> getDemandsForHjids(List<Long> hjids) {
        return new JPARepositoryFactory().forCatalogueRepository(true).getEntities(QUERY_GET_DEMANDS_FOR_HJIDS, new String[]{"hjids"}, new Object[]{hjids});
    }
}
