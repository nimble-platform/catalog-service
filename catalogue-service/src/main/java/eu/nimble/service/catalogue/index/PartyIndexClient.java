package eu.nimble.service.catalogue.index;

import eu.nimble.service.catalogue.util.CredentialsUtil;
import eu.nimble.service.catalogue.util.SpringBridge;
import eu.nimble.service.model.solr.party.PartyType;
import eu.nimble.utility.JsonSerializationUtility;
import feign.Response;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

/**
 * Created by suat on 11-Feb-19.
 */
@Component
public class PartyIndexClient {
    private static final Logger logger = LoggerFactory.getLogger(PartyIndexClient.class);

    @Autowired
    private CredentialsUtil credentialsUtil;

    public void indexParty(eu.nimble.service.model.ubl.commonaggregatecomponents.PartyType party) {
        try {
            String partyJson;
            try {
                PartyType indexParty = IndexingWrapper.toIndexParty(party);
                partyJson = JsonSerializationUtility.getObjectMapper().writeValueAsString(indexParty);

            } catch (Exception e) {
                String serializedParty = JsonSerializationUtility.serializeEntitySilently(party);
                String msg = String.format("Failed to transporm party to index party.\nparty: %s", serializedParty);
                logger.error(msg, e);
                return;
            }

            Response response = SpringBridge.getInstance().getiIndexingServiceClient().setParty(credentialsUtil.getBearerToken(),partyJson);

            if (response.status() == HttpStatus.OK.value()) {
                logger.info("Indexed party successfully. party name: {}, id: {}", party.getPartyName().get(0).getName().getValue(), party.getPartyIdentification().get(0).getID());
                return;

            } else {
                String msg = String.format("Failed to index party. id: %s, indexing call status: %d, message: %s", party.getPartyIdentification().get(0).getID(), response.status(), IOUtils.toString(response.body().asInputStream()));
                logger.error(msg);
                return;
            }

        } catch (Exception e) {
            String msg = String.format("Failed to index party. uri: %s", party.getPartyIdentification().get(0).getID());
            logger.error(msg, e);
            return;
        }
    }

    public void removeParty(String partyId) {
        try {
            Response response = SpringBridge.getInstance().getiIndexingServiceClient().removeParty(credentialsUtil.getBearerToken(),partyId);

            if (response.status() == HttpStatus.OK.value()) {
                logger.info("Deleted indexed Party. partyId: {}", partyId);

            } else {
                logger.error("Failed to delete indexed Party. partyId: {}, indexing call status: {}, message: {}",
                        partyId, response.status(), IOUtils.toString(response.body().asInputStream()));
            }

        } catch (Exception e) {
            logger.error("Failed to delete indexed Catalogue. uuid: {}", partyId, e);
        }
    }
}
