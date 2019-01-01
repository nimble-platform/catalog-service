package eu.nimble.service.catalogue.persistence.util;

import eu.nimble.service.catalogue.util.SpringBridge;
import eu.nimble.service.model.ubl.commonaggregatecomponents.PartyType;

/**
 * Created by suat on 28-Dec-18.
 */
public class PartyTypePersistenceUtil {
    private static final String QUERY_SELECT_BY_ID = "SELECT party FROM PartyType party WHERE party.ID = :partyId";

    public static PartyType getPartyById(String partyId) {
        return SpringBridge.getInstance().getGenericJPARepository().getSingleEntity(QUERY_SELECT_BY_ID, new String[]{"partyId"}, new Object[]{partyId});
    }
}
