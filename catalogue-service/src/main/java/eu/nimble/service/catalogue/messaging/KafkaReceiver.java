package eu.nimble.service.catalogue.messaging;

import eu.nimble.common.rest.trust.TrustClient;
import eu.nimble.service.catalogue.CatalogueDatabaseAdapter;
import eu.nimble.service.catalogue.sync.MarmottaSynchronizer;
import eu.nimble.service.catalogue.config.KafkaConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Created by Johannes Innerbichler on 27.09.18.
 */
@Component
public class KafkaReceiver {

    private static Logger logger = LoggerFactory.getLogger(KafkaReceiver.class);

    @Autowired
    private TrustClient trustClient;

    @KafkaListener(topics = "${nimble.kafka.topics.companyUpdates}")
    public void receiveCompanyUpdates(ConsumerRecord<String, KafkaConfig.AuthorizedCompanyUpdate> consumerRecord) {
        String companyID = consumerRecord.value().getCompanyId();
        logger.info("Received company updates for company with id: {}",companyID);
        String accessToken = consumerRecord.value().getAccessToken();

        CatalogueDatabaseAdapter.syncPartyInUBLDB(companyID,accessToken);
        MarmottaSynchronizer.getInstance().addRecord(companyID);

        logger.info("Updated party for the company with id: {} successfully",companyID);
    }

    @KafkaListener(topics = "${nimble.kafka.topics.trustScoreUpdates}")
    public void receiveTrustScoreUpdates(ConsumerRecord<String, KafkaConfig.AuthorizedCompanyUpdate> consumerRecord) {
        String companyID = consumerRecord.value().getCompanyId();
        logger.info("Received company trust updates for company with id: {}",companyID);
        String accessToken = consumerRecord.value().getAccessToken();

        CatalogueDatabaseAdapter.syncTrustScores(companyID, accessToken);
        MarmottaSynchronizer.getInstance().addRecord(companyID);
        logger.info("Processed company trust updates for company with id: {}", companyID);
    }
}
