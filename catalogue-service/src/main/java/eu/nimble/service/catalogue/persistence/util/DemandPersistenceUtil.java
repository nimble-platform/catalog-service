package eu.nimble.service.catalogue.persistence.util;

import eu.nimble.service.model.ubl.commonaggregatecomponents.DemandInterestCount;
import eu.nimble.service.model.ubl.commonaggregatecomponents.DemandType;
import eu.nimble.utility.persistence.GenericJPARepositoryImpl;
import eu.nimble.utility.persistence.JPARepositoryFactory;
import org.ehcache.shadow.org.terracotta.context.query.QueryBuilder;

import javax.xml.datatype.XMLGregorianCalendar;
import java.util.ArrayList;
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
    private static final String QUERY_GET_DEMAND_INTEREST_COUNT = "SELECT interestCount FROM DemandInterestCount interestCount WHERE demandHJID = :demandHjid AND partyID = :partyId";


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

    public static void saveDemandInterestActivity(Long demandHjid, String visitorCompanyId) {
        GenericJPARepositoryImpl repo = new JPARepositoryFactory().forCatalogueRepository();
        DemandInterestCount interestCount = repo.getSingleEntity(
                QUERY_GET_DEMAND_INTEREST_COUNT, new String[]{"demandHjid", "partyId"}, new Object[]{demandHjid, visitorCompanyId}
        );
        if (interestCount == null) {
            interestCount = new DemandInterestCount();
            interestCount.setDemandHJID(demandHjid);
            interestCount.setPartyID(visitorCompanyId);
            interestCount.setInterestCount(1);
            repo.persistEntity(interestCount);
        } else {
            interestCount.setInterestCount(interestCount.getInterestCount() + 1);
            repo.updateEntity(interestCount);
        }
    }

    public static List<DemandInterestCount> getInterestCounts() {
        List<DemandInterestCount> interestCounts = new JPARepositoryFactory().forCatalogueRepository().getEntities(DemandInterestCount.class);
        return interestCounts;
    }
}
