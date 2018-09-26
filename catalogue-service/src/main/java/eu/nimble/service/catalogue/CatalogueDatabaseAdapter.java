package eu.nimble.service.catalogue;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import eu.nimble.service.catalogue.util.SpringBridge;
import eu.nimble.service.model.ubl.catalogue.CatalogueType;
import eu.nimble.service.model.ubl.commonaggregatecomponents.PartyType;
import eu.nimble.service.model.ubl.commonaggregatecomponents.QualityIndicatorType;
import eu.nimble.service.model.ubl.commonbasiccomponents.QuantityType;
import eu.nimble.utility.Configuration;
import eu.nimble.utility.HibernateUtility;
import eu.nimble.utility.JsonSerializationUtility;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import util.DataModelUtility;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by suat on 07-Aug-18.
 */
public class CatalogueDatabaseAdapter {
    private static final Logger logger = LoggerFactory.getLogger(CatalogueDatabaseAdapter.class);

    private static final String CATALOGUE_EXISTS_QUERY = "SELECT COUNT(c) FROM CatalogueType c WHERE c.ID = ? and c.providerParty.ID = ?";
    private static final String GET_PARTY_QUERY = "SELECT party FROM PartyType party WHERE party.ID = ?";
    private static final String GET_PARTY_CATALOGUES_QUERY = "SELECT catalogue.ID FROM CatalogueType as catalogue" +
            " JOIN catalogue.providerParty as catalogue_provider_party " +
            " WHERE catalogue_provider_party.ID = ?";

    public static boolean catalogueExists(String partyId, String partySpecificCatalogueId) {
        int catalogueExists = ((Long) HibernateUtility.getInstance(Configuration.UBL_PERSISTENCE_UNIT_NAME).load(CATALOGUE_EXISTS_QUERY, partySpecificCatalogueId, partyId)).intValue();
        return catalogueExists == 1 ? true : false;
    }

    public static PartyType updateParty(String partyId, String bearerToken) {
        PartyType party = SpringBridge.getInstance().getIdentityClientTyped().getParty(bearerToken, partyId);
        PartyType catalogueParty = getParty(partyId);
        if (party == null) {
            party = removePartyHjids(party);
            HibernateUtility.getInstance(Configuration.UBL_PERSISTENCE_UNIT_NAME).persist(party);

        } else {
            long hjid = party.getHjid();
            party = new PartyType();
            party.setHjid(hjid);
            party.setName("Deneme 1 - 2");
        }
        return party;
    }

    public static void syncPartyInUBLDB(String partyId, String bearerToken) {
        PartyType catalogueParty = getParty(partyId);
        PartyType identityParty = SpringBridge.getInstance().getIdentityClientTyped().getParty(bearerToken, partyId);
        if(catalogueParty == null) {
            identityParty = checkPartyIntegrity(identityParty);
            HibernateUtility.getInstance(Configuration.UBL_PERSISTENCE_UNIT_NAME).persist(identityParty);

        } else {
            DataModelUtility.nullifyPartyFields(catalogueParty);
            DataModelUtility.copyParty(catalogueParty, identityParty);
            HibernateUtility.getInstance(Configuration.UBL_PERSISTENCE_UNIT_NAME).update(catalogueParty);
        }
    }

    public static void syncTrustScores(PartyType trustParty) {
        PartyType catalogueParty = getParty(trustParty.getID());
        if(catalogueParty.getQualityIndicator() == null) {
            catalogueParty.setQualityIndicator(new ArrayList<>());
        }

        for(QualityIndicatorType qualityIndicator : trustParty.getQualityIndicator()) {
            if(qualityIndicator.getQuantity() == null) {
                continue;
            }

            // update existing indicators
            boolean indicatorExists = false;
            for(QualityIndicatorType qualityIndicatorExisting : catalogueParty.getQualityIndicator()) {
                if(qualityIndicator.getQualityParameter().contentEquals(qualityIndicatorExisting.getQualityParameter())) {
                    if(qualityIndicatorExisting.getQuantity() == null) {
                        qualityIndicatorExisting.setQuantity(new QuantityType());
                    }
                    qualityIndicatorExisting.getQuantity().setValue(qualityIndicator.getQuantity().getValue());
                    indicatorExists = true;
                    break;
                }
            }
            // create new indicator
            if(!indicatorExists) {
                QualityIndicatorType indicator = new QualityIndicatorType();
                indicator.setQualityParameter(qualityIndicator.getQualityParameter());
                QuantityType quantity = new QuantityType();
                quantity.setValue(qualityIndicator.getQuantity().getValue());
                indicator.setQuantity(quantity);
                catalogueParty.getQualityIndicator().add(indicator);
            }
        }

        HibernateUtility.getInstance(Configuration.UBL_PERSISTENCE_UNIT_NAME).update(catalogueParty);
    }

    public static PartyType syncPartyInUBLDB(PartyType party) {
        if (party == null) {
            return null;
        }
        PartyType catalogueParty = getParty(party.getID());
        if(catalogueParty != null) {
            return catalogueParty;
        } else {
            party = checkPartyIntegrity(party);
            HibernateUtility.getInstance(Configuration.UBL_PERSISTENCE_UNIT_NAME).persist(party);
            return party;
        }
    }

    public static PartyType getParty(String partyId) {
        return HibernateUtility.getInstance(Configuration.UBL_PERSISTENCE_UNIT_NAME).load(GET_PARTY_QUERY, partyId);
    }

    public static List<String> getCatalogueIdsOfParty(String partyId) {
        List<String> catalogueIds = (List<String>) HibernateUtility.getInstance(Configuration.UBL_PERSISTENCE_UNIT_NAME).loadAll(GET_PARTY_CATALOGUES_QUERY, partyId);
        return catalogueIds;
    }

    private static PartyType checkPartyIntegrity(PartyType party) {
        party = removePartyHjids(party);
        // person information is not stored in ubldb
        // TODO do not store any other party information in ubldb than ID. Todo that the user interface and
        // other places relying on the party information (stored in ubldb) should be checked/updated
        party.setPerson(null);
        return party;
    }

    private static PartyType removePartyHjids(PartyType party) {
        try {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper = objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        JSONObject object = new JSONObject(objectMapper.writeValueAsString(party));
        JsonSerializationUtility.removeHjidFields(object);
        party = objectMapper.readValue(object.toString(), PartyType.class);
        return party;
        } catch (IOException e) {
            String msg = String.format("Failed to remove hjid fields from the party with id: %s", party.getID());
            logger.error(msg, e);
            throw new RuntimeException(msg, e);
        }
    }
}
