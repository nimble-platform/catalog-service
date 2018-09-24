package eu.nimble.service.catalogue;

import eu.nimble.service.model.ubl.commonaggregatecomponents.PartyType;
import eu.nimble.utility.Configuration;
import eu.nimble.utility.HibernateUtility;

/**
 * Created by suat on 07-Aug-18.
 */
public class CatalogueDatabaseAdapter {
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
            HibernateUtility.getInstance(Configuration.UBL_PERSISTENCE_UNIT_NAME).persist(party);
            return party;
        }
        return partyType;
    }
}
