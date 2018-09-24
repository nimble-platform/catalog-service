package eu.nimble.service.catalogue;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import eu.nimble.service.model.ubl.commonaggregatecomponents.PartyType;
import eu.nimble.utility.Configuration;
import eu.nimble.utility.HibernateUtility;
import eu.nimble.utility.JsonSerializationUtility;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by suat on 07-Aug-18.
 */
public class CatalogueDatabaseAdapter {
    private static final Logger logger = LoggerFactory.getLogger(CatalogueDatabaseAdapter.class);

    public static boolean catalogueExists(String partyId, String partySpecificCatalogueId) {
        String queryStr = "SELECT COUNT(c) FROM CatalogueType c WHERE c.ID = ? and c.providerParty.ID = ?";
        int catalogueExists = ((Long) HibernateUtility.getInstance(Configuration.UBL_PERSISTENCE_UNIT_NAME).load(queryStr, partySpecificCatalogueId, partyId)).intValue();
        return catalogueExists == 1 ? true : false;
    }

    public static PartyType getParty(PartyType party){
        if(party == null){
            return null;
        }
        String query = "SELECT party FROM PartyType party WHERE party.ID = ?";
        PartyType partyType = HibernateUtility.getInstance(Configuration.UBL_PERSISTENCE_UNIT_NAME).load(query,party.getID());
        if(partyType == null){
            ObjectMapper objectMapper = new ObjectMapper();
            objectMapper = objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
            try {
                JSONObject object = new JSONObject(objectMapper.writeValueAsString(party));
                JsonSerializationUtility.removeHjidFields(object);
                party = objectMapper.readValue(object.toString(), PartyType.class);
            }
            catch (Exception e){
                logger.error("Failed to remove hjid fields from the party",e);
            }

            HibernateUtility.getInstance(Configuration.UBL_PERSISTENCE_UNIT_NAME).persist(party);
            return party;
        }
        return partyType;
    }
}
