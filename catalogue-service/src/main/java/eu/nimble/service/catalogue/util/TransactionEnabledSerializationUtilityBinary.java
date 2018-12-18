package eu.nimble.service.catalogue.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import eu.nimble.service.model.ubl.commonbasiccomponents.BinaryObjectType;
import eu.nimble.utility.persistence.binary.BinaryObjectSerializerGetUris;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
public class TransactionEnabledSerializationUtilityBinary {
    private static Logger log = LoggerFactory.getLogger(TransactionEnabledSerializationUtilityBinary.class);

    @Transactional(transactionManager = "ubldbTransactionManager")
    public List<String> serializeBinaryObject(Object object) {
        ObjectMapper objectMapper = new ObjectMapper();
        SimpleModule simpleModule = new SimpleModule();
        List<String> uris;
        try {
            BinaryObjectSerializerGetUris binaryObjectSerializerGetUris = new BinaryObjectSerializerGetUris();

            simpleModule.addSerializer(BinaryObjectType.class,binaryObjectSerializerGetUris);
            objectMapper.registerModule(simpleModule);
            uris = binaryObjectSerializerGetUris.getListOfUris();

            objectMapper.writeValueAsString(object);
        } catch (JsonProcessingException e) {
            String msg = String.format("Failed to serialize object: %s", object.getClass().getName());
            log.error(msg);
            throw new RuntimeException(msg, e);
        }
        return uris;
    }
}





