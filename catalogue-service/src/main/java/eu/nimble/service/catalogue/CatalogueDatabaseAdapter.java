package eu.nimble.service.catalogue;

import eu.nimble.utility.Configuration;
import eu.nimble.utility.HibernateUtility;

/**
 * Created by suat on 07-Aug-18.
 */
public class CatalogueDatabaseAdapter {
    public static boolean catalogueExists(String partyId, String partySpecificCatalogueId) {
        String queryStr = "SELECT COUNT(c) FROM CatalogueType c WHERE c.ID = ? and c.providerParty.ID = ?";
        int catalogueExists = ((Long) HibernateUtility.getInstance(Configuration.UBL_PERSISTENCE_UNIT_NAME).getCount(queryStr, partySpecificCatalogueId, partyId)).intValue();
        return catalogueExists == 1 ? true : false;
    }
}
