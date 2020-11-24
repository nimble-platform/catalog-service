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

    public static List<DemandType> getDemandsForParty(String companyId) {
        return new JPARepositoryFactory().forCatalogueRepository().getEntities(QUERY_GET_DEMANDS_FOR_COMPANY, new String[]{"companyId"}, new Object[]{companyId});
    }
}
