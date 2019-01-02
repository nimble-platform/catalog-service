package eu.nimble.service.catalogue.persistence.util;

import eu.nimble.service.catalogue.util.SpringBridge;
import eu.nimble.service.model.ubl.catalogue.CatalogueType;
import eu.nimble.utility.persistence.JPARepositoryFactory;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

/**
 * Created by suat on 31-Dec-18.
 */
public class CataloguePersistenceUtil {
    private static final String QUERY_GET_BY_UUID = "SELECT catalogue FROM CatalogueType catalogue WHERE catalogue.UUID = :uuid";
    private static final String QUERY_GET_FOR_PARTY = "SELECT catalogue FROM CatalogueType as catalogue "
            + " JOIN catalogue.providerParty as catalogue_provider_party"
            + " WHERE catalogue.ID = :catalogueId"
            + " AND catalogue_provider_party.ID = :partyId";
    private static final String QUERY_CHECK_EXISTENCE_BY_ID = "SELECT COUNT(c) FROM CatalogueType c WHERE c.ID = :catalogueId and c.providerParty.ID = :partyId";
    private static final String QUERY_GET_CATALOGUE_IDS_FOR_PARTY = "SELECT catalogue.UUID FROM CatalogueType as catalogue" +
            " JOIN catalogue.providerParty as catalogue_provider_party " +
            " WHERE catalogue_provider_party.ID = :partyId";


    public static CatalogueType getCatalogueByUuid(String catalogueUuid) {
        return new JPARepositoryFactory().forCatalogueRepository().getSingleEntity(QUERY_GET_BY_UUID, new String[]{"uuid"}, new Object[]{catalogueUuid});
    }

    public static CatalogueType getCatalogueForParty(String catalogueId, String partyId) {
        return new JPARepositoryFactory().forCatalogueRepository().getSingleEntity(QUERY_GET_FOR_PARTY, new String[]{"catalogueId", "partyId"}, new Object[]{catalogueId, partyId});
    }

    public static Boolean checkCatalogueExistenceById(String catalogueId, String partyId) {
        long catalogueExists = new JPARepositoryFactory().forCatalogueRepository().getSingleEntity(QUERY_CHECK_EXISTENCE_BY_ID, new String[]{"catalogueId", "partyId"}, new Object[]{catalogueId, partyId});
        return catalogueExists == 1 ? true : false;
    }

    public static List<String> getCatalogueIdsForParty(String partyId) {
        return new JPARepositoryFactory().forCatalogueRepository().getEntities(QUERY_GET_CATALOGUE_IDS_FOR_PARTY, new String[]{"partyId"}, new Object[]{partyId});
    }
}
