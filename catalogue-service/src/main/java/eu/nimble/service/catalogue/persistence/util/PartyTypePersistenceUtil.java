package eu.nimble.service.catalogue.persistence.util;

import eu.nimble.service.model.ubl.commonaggregatecomponents.PartyIdentificationType;
import eu.nimble.service.model.ubl.commonaggregatecomponents.PartyType;
import eu.nimble.utility.persistence.JPARepositoryFactory;

import java.util.List;

/**
 * Created by suat on 28-Dec-18.
 */
public class PartyTypePersistenceUtil {
    private static final String QUERY_SELECT_BY_ID = "SELECT party FROM PartyType party JOIN party.partyIdentification partyIdentification WHERE partyIdentification.ID = :partyId AND party.federationInstanceID = :federationId";
    private static final String QUERY_GET_PARTY_ID_LIST = "SELECT party.partyIdentification FROM PartyType party WHERE party.hjid = :hjid";

    public static PartyType getPartyById(String partyId,String federationId) {
        return new JPARepositoryFactory().forCatalogueRepository(true).getSingleEntity(QUERY_SELECT_BY_ID, new String[]{"partyId","federationId"}, new Object[]{partyId,federationId});
    }

    public static List<PartyIdentificationType> getPartyIdList(Long partyHjid) {
        return new JPARepositoryFactory().forCatalogueRepository()
                .getEntities(QUERY_GET_PARTY_ID_LIST, new String[]{"hjid"}, new Object[]{partyHjid});
    }
}
