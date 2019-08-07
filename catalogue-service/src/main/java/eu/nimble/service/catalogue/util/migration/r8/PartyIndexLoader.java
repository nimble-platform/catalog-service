package eu.nimble.service.catalogue.util.migration.r8;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import eu.nimble.common.rest.identity.IIdentityClientTyped;
import eu.nimble.common.rest.trust.TrustClient;
import eu.nimble.service.catalogue.index.PartyIndexClient;
import eu.nimble.service.catalogue.util.CredentialsUtil;
import eu.nimble.service.model.ubl.commonaggregatecomponents.PartyType;
import eu.nimble.service.model.ubl.commonaggregatecomponents.QualityIndicatorType;
import eu.nimble.service.model.ubl.commonbasiccomponents.QuantityType;
import eu.nimble.utility.JsonSerializationUtility;
import feign.Response;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.List;

/**
 * Created by suat on 18-Feb-19.
 */
@Profile("!test")
@Component
public class PartyIndexLoader {
    private static final Logger logger = LoggerFactory.getLogger(PartyIndexLoader.class);

    @Autowired
    private PartyIndexClient partyIndexClient;
    @Autowired
    private IIdentityClientTyped iIdentityClientTyped;
    @Autowired
    private TrustClient trustClient;
    @Autowired
    private CredentialsUtil credentialsUtil;

    public void indexParties() {
        // get all parties from the identity service
        Response partiesResponse = null;
        try {
            partiesResponse = iIdentityClientTyped.getPartyPartiesInUBL(credentialsUtil.getBearerToken(), "0", "false", Integer.MAX_VALUE + "");
        } catch (Exception e) {
            logger.error("Unexpected error while getting parties from identity service:",e);
            return;
        }
        if (partiesResponse.status() != 200) {
            logger.error("Failed to get parties from identity client. Error code: {}, body: {}", partiesResponse.status(), partiesResponse.body().toString());
        }

        List<PartyType> parties;
        try {
            ObjectMapper mapper = JsonSerializationUtility.getObjectMapper();
            JsonNode responseJson = mapper.readTree(partiesResponse.body().asInputStream());
            JsonNode partiesNode = responseJson.get("content");
            parties = mapper.readValue(partiesNode.toString(), new TypeReference<List<PartyType>>(){});

        } catch (IOException e) {
            logger.error("Failed to obtain body response while getting the parties", e);
            return;
        }
        logger.info("Retrieved parties from identity service");


        // get all the trust values from the trust service
        partiesResponse = trustClient.getAllTrustValues(credentialsUtil.getBearerToken());
        if (partiesResponse.status() != 200) {
            logger.error("Failed to get parties from trust client. Error code: {}, body: {}", partiesResponse.status(), partiesResponse.body().toString());
        }

        List<PartyType> trustParties;
        try {
            String responseBody = IOUtils.toString(partiesResponse.body().asInputStream());
            trustParties = JsonSerializationUtility.deserializeContent(responseBody, new TypeReference<List<PartyType>>(){});

        } catch (IOException e) {
            logger.error("Failed to obtain body response while getting the trust parties", e);
            return;
        }
        logger.info("Retrieved parties from trust service");

        for(PartyType party : parties) {
            for(PartyType trustParty : trustParties) {
                if(party.getPartyIdentification().get(0).equals(trustParty.getPartyIdentification().get(0))) {
                    for(QualityIndicatorType qualityIndicator : trustParty.getQualityIndicator()) {
                        if(qualityIndicator.getQuantity() == null || qualityIndicator.getQualityParameter() == null) {
                            continue;
                        }

                        QualityIndicatorType indicator = new QualityIndicatorType();
                        indicator.setQualityParameter(qualityIndicator.getQualityParameter());
                        QuantityType quantity = new QuantityType();
                        quantity.setValue(qualityIndicator.getQuantity().getValue());
                        indicator.setQuantity(quantity);
                        party.getQualityIndicator().add(indicator);
                    }
                }
            }

            partyIndexClient.indexParty(party);

        }
        logger.info("Indexing parties completed");
    }
}
