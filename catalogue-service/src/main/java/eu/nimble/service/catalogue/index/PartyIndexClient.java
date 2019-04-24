package eu.nimble.service.catalogue.index;

import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import eu.nimble.service.catalogue.util.CredentialsUtil;
import eu.nimble.service.model.solr.party.PartyType;
import eu.nimble.utility.JsonSerializationUtility;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

/**
 * Created by suat on 11-Feb-19.
 */
@Component
public class PartyIndexClient {
    private static final Logger logger = LoggerFactory.getLogger(PartyIndexClient.class);

    @Value("${nimble.indexing.url}")
    private String indexingUrl;
    @Value("${nimble.indexing.solr.url}")
    private String solrUrl;
    @Value("${nimble.indexing.solr.username}")
    private String solrUsername;
    @Value("${nimble.indexing.solr.password}")
    private String solrPassword;

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

            HttpResponse<String> response = Unirest.put(indexingUrl + "/party")
                    .header(HttpHeaders.AUTHORIZATION, credentialsUtil.getBearerToken())
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .body(partyJson)
                    .asString();

            if (response.getStatus() == HttpStatus.OK.value()) {
                logger.info("Indexed party successfully. party name: {}, id: {}", party.getPartyName().get(0).getName().getValue(), party.getPartyIdentification().get(0).getID());
                return;

            } else {
                String msg = String.format("Failed to index party. id: %s, indexing call status: %d, message: %s", party.getPartyIdentification().get(0).getID(), response.getStatus(), response.getBody());
                logger.error(msg);
                return;
            }

        } catch (UnirestException e) {
            String msg = String.format("Failed to index party. uri: %s", party.getPartyIdentification().get(0).getID());
            logger.error(msg, e);
            return;
        }
    }

    public void removeParty(String partyId) {
        try {
            HttpResponse<String> response;
            response = Unirest.delete(indexingUrl + "/party")
                    .header(HttpHeaders.AUTHORIZATION, credentialsUtil.getBearerToken())
                    .queryString("uri", partyId)
                    .asString();

            if (response.getStatus() == HttpStatus.OK.value()) {
                logger.info("Deleted indexed Party. partyId: {}", partyId);

            } else {
                logger.error("Failed to delete indexed Party. partyId: {}, indexing call status: {}, message: {}",
                        partyId, response.getStatus(), response.getBody());
            }

        } catch (UnirestException e) {
            logger.error("Failed to delete indexed Catalogue. uuid: {}", partyId, e);
        }
    }
}
