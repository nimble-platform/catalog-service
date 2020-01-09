package eu.nimble.service.catalogue.persistence.util;

import eu.nimble.service.model.ubl.commonaggregatecomponents.PartyType;
import eu.nimble.utility.persistence.JPARepositoryFactory;

/**
 * Created by suat on 28-Dec-18.
 */
public class PartyTypePersistenceUtil {
    private static final String QUERY_SELECT_BY_ID = "SELECT party FROM PartyType party JOIN party.partyIdentification partyIdentification WHERE partyIdentification.ID = :partyId AND party.federationInstanceID = :federationId";

    public static PartyType getPartyById(String partyId,String federationId) {
        return new JPARepositoryFactory().forCatalogueRepository(true).getSingleEntity(QUERY_SELECT_BY_ID, new String[]{"partyId","federationId"}, new Object[]{partyId,federationId});
    }
}
