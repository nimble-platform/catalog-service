package eu.nimble.service.catalogue.persistence.util;

import eu.nimble.service.model.ubl.commonaggregatecomponents.DemandType;
import eu.nimble.service.model.ubl.commonbasiccomponents.TextType;
import eu.nimble.utility.persistence.GenericJPARepositoryImpl;
import eu.nimble.utility.persistence.JPARepositoryFactory;

import java.util.List;

public class DemandPersistenceUtil {
    private static final String QUERY_GET_DEMANDS_FOR_COMPANY =
            "SELECT demand FROM DemandType demand" +
                    " JOIN demand.metadata metadata" +
                    " JOIN metadata.ownerCompanyItems ownerCompanies" +
                    " WHERE ownerCompanies.item = :companyId" +
                    " ORDER BY metadata.modificationDateItem DESC";
    private static final String QUERY_INSERT_INDEX_DATA = "UPDATE demand_type SET search_index = to_tsvector('%s') WHERE hjid = :hjid";


    public static List<DemandType> getDemandsForParty(String companyId) {
        return new JPARepositoryFactory().forCatalogueRepository(true).getEntities(QUERY_GET_DEMANDS_FOR_COMPANY, new String[]{"companyId"}, new Object[]{companyId});
    }

    public static void indexDemandText(DemandType demand) {
        StringBuilder indexData = new StringBuilder();
        for (TextType title : demand.getTitle()) {
            indexData.append(title.getValue()).append(" ");
        }
        for (TextType description : demand.getDescription()) {
            indexData.append(description.getValue()).append(" ");
        }
        String query = String.format(QUERY_INSERT_INDEX_DATA, indexData.toString());
        new JPARepositoryFactory().forCatalogueRepository().executeUpdate(query, new String[]{"hjid"}, new Object[]{demand.getHjid()}, true);
    }
}
