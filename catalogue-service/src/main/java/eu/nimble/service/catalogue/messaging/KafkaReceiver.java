package eu.nimble.service.catalogue.messaging;

import eu.nimble.utility.config.KafkaConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Created by Johannes Innerbichler on 27.09.18.
 */
@Component
public class KafkaReceiver {
    @KafkaListener(topics = "${nimble.kafka.topics.companyUpdates}")
    public void receiveCompanyUpdates(ConsumerRecord<String, KafkaConfig.CompanyUpdate> consumerRecord) {
        String companyID = consumerRecord.value().getCompanyId();
        String accessToken = consumerRecord.value().getAccessToken();
        System.out.println("Receiver: " + companyID);
    }
}
