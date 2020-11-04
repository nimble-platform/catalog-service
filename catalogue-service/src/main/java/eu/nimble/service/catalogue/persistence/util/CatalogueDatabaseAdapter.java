package eu.nimble.service.catalogue.persistence.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import eu.nimble.service.catalogue.util.SpringBridge;
import eu.nimble.service.model.ubl.commonaggregatecomponents.PartyIdentificationType;
import eu.nimble.service.model.ubl.commonaggregatecomponents.PartyType;
import eu.nimble.utility.JsonSerializationUtility;
import eu.nimble.utility.persistence.GenericJPARepository;
import eu.nimble.utility.persistence.JPARepositoryFactory;
import org.hibernate.Hibernate;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.List;

/**
 * Created by suat on 07-Aug-18.
 */
public class CatalogueDatabaseAdapter {
    private static final Logger logger = LoggerFactory.getLogger(CatalogueDatabaseAdapter.class);

    public static PartyType syncPartyInUBLDB(String partyId, String bearerToken) {
        try {
            SpringBridge.getInstance().getLockPool().getLockForParty(partyId).writeLock().lock();

            PartyType catalogueParty = PartyTypePersistenceUtil.getPartyById(partyId,SpringBridge.getInstance().getFederationId());
            if (catalogueParty == null) {
                PartyType identityParty;
                try {
                    identityParty = SpringBridge.getInstance().getiIdentityClientTyped().getParty(bearerToken, partyId);
                    identityParty = checkPartyIntegrity(identityParty);

                } catch (IOException e) {
                    String msg = String.format("Failed to get party with id: %s", partyId);
                    logger.error(msg, e);
                    throw new RuntimeException(msg, e);
                }
                new JPARepositoryFactory().forCatalogueRepository().persistEntity(identityParty);
                return identityParty;

            } else {
                return catalogueParty;
            }
        } finally {
            SpringBridge.getInstance().getLockPool().getLockForParty(partyId).writeLock().unlock();
        }
    }

    public static PartyType syncPartyInUBLDB(PartyType party) {
        if (party == null) {
            return null;
        }
        PartyType catalogueParty = PartyTypePersistenceUtil.getPartyById(party.getPartyIdentification().get(0).getID(),party.getFederationInstanceID());
        if(catalogueParty != null) {
            return catalogueParty;
        } else {
            party = checkPartyIntegrity(party);
            new JPARepositoryFactory().forCatalogueRepository().persistEntity(party);
            return party;
        }
    }

    private static PartyType checkPartyIntegrity(PartyType party) {
        party = removePartyHjids(party);
        // TODO do not store any other party information in ubldb than ID. Todo that the user interface and
        // other places relying on the party information (stored in ubldb) should be checked/updated
        return party;
    }

    private static PartyType removePartyHjids(PartyType party) {
        try {
        ObjectMapper objectMapper = JsonSerializationUtility.getObjectMapper();
        JSONObject object = new JSONObject(objectMapper.writeValueAsString(party));
        JsonSerializationUtility.removeHjidFields(object);
        party = objectMapper.readValue(object.toString(), PartyType.class);
        return party;
        } catch (IOException e) {
            String msg = String.format("Failed to remove hjid fields from the party with id: %s", party.getPartyIdentification().get(0).getID());
            logger.error(msg, e);
            throw new RuntimeException(msg, e);
        }
    }
}
