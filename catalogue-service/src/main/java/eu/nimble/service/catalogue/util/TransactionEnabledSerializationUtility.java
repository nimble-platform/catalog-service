package eu.nimble.service.catalogue.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * In a Spring-managed component serialization of lazy collections requires transaction management. Therefore, the
 * the client classes should be accessing the serialization utility by Spring means i.e. by Autowiring.
 *
 * Created by suat on 28-Nov-18.
 */
@Component
public class TransactionEnabledSerializationUtility {

    private static Logger log = LoggerFactory.getLogger(TransactionEnabledSerializationUtility.class);

    @Transactional(transactionManager = "ubldbTransactionManager")
    public String serializeUBLObject(Object object) {
        ObjectMapper objectMapper = new ObjectMapper();
        String serializedObject = null;
        try {
            serializedObject = objectMapper.writeValueAsString(object);
        } catch (JsonProcessingException e) {
            String msg = String.format("Failed to serialize object: %s", object.getClass().getName());
            log.error(msg);
            throw new RuntimeException(msg, e);
        }
        return serializedObject;
    }
}
