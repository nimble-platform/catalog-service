package eu.nimble.service.catalogue.messaging;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Created by Johannes Innerbichler on 27.09.18.
 */
@Component
public class KafkaReceiver {

    @KafkaListener(topics = "${nimble.kafka.topics.companyUpdates}")
    public void receiveCompanyUpdates(ConsumerRecord<?, ?> consumerRecord) {
        String companyID = consumerRecord.value().toString();
        System.out.println("Received updated for company with ID: " + companyID);
    }
}
