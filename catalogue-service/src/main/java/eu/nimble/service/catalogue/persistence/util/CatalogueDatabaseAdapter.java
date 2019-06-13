package eu.nimble.service.catalogue.persistence.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import eu.nimble.service.catalogue.util.SpringBridge;
import eu.nimble.service.model.ubl.commonaggregatecomponents.PartyType;
import eu.nimble.service.model.ubl.commonaggregatecomponents.PersonType;
import eu.nimble.utility.JsonSerializationUtility;
import eu.nimble.utility.persistence.JPARepositoryFactory;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * Created by suat on 07-Aug-18.
 */
public class CatalogueDatabaseAdapter {
    private static final Logger logger = LoggerFactory.getLogger(CatalogueDatabaseAdapter.class);

    public static PartyType syncPartyInUBLDB(PartyType party, String partyId, String bearerToken){
        if(party != null){
            PartyType catalogueParty = PartyTypePersistenceUtil.getPartyById(party.getPartyIdentification().get(0).getID());
            if(catalogueParty != null) {
                return catalogueParty;
            }
            party = checkPartyIntegrity(party);
            new JPARepositoryFactory().forCatalogueRepository().persistEntity(party);
            return party;
        }
        else if(bearerToken != null && partyId != null){
            try {
                SpringBridge.getInstance().getLockPool().getLockForParty(partyId).writeLock().lock();

                PartyType catalogueParty = PartyTypePersistenceUtil.getPartyById(partyId);
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
        else if(bearerToken != null){
            try{
                // get person using the given bearer token
                PersonType person = SpringBridge.getInstance().getiIdentityClientTyped().getPerson(bearerToken);
                // get party for the person
                PartyType identityParty = SpringBridge.getInstance().getiIdentityClientTyped().getPartyByPersonID(person.getID()).get(0);
                if(identityParty != null){
                    PartyType catalogueParty = PartyTypePersistenceUtil.getPartyById(identityParty.getPartyIdentification().get(0).getID());
                    if(catalogueParty == null){
                        identityParty = checkPartyIntegrity(identityParty);
                        return identityParty;
                    }
                    else{
                        return catalogueParty;
                    }
                }
            } catch (Exception e){
                logger.error("Failed to get party for bearer token: {}",bearerToken,e);
            }
        }
        return null;
    }

    public static PartyType syncPartyInUBLDB(PartyType party,String bearerToken) {
        return syncPartyInUBLDB(party,null,bearerToken);
    }

    public static PartyType syncPartyInUBLDB(PartyType party) {
        return syncPartyInUBLDB(party,null,null);
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
