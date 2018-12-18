package eu.nimble.service.catalogue.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import eu.nimble.service.model.ubl.catalogue.CatalogueType;
import eu.nimble.service.model.ubl.commonaggregatecomponents.CatalogueLineType;
import eu.nimble.service.model.ubl.commonbasiccomponents.BinaryObjectType;

import java.io.IOException;
import java.util.List;

public class BinaryContentUtil {

    // removes binary content from the catalogue and saves them to binary content database
    public static CatalogueType removeBinaryContentFromCatalogue(String serializedCatalogue) throws IOException {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper = objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        SimpleModule simpleModule = new SimpleModule();
        simpleModule.addDeserializer(BinaryObjectType.class, SpringBridge.getInstance().getBinaryObjectDeserializer());
        objectMapper.registerModule(simpleModule);
        return objectMapper.readValue(serializedCatalogue, CatalogueType.class);
    }

    // removes binary content from the catalogue line and saves them to binary content database
    public static CatalogueLineType removeBinaryContentFromCatalogueLine(String serializedCatalogueLine) throws IOException {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper = objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        SimpleModule simpleModule = new SimpleModule();
        simpleModule.addDeserializer(BinaryObjectType.class, SpringBridge.getInstance().getBinaryObjectDeserializer());
        objectMapper.registerModule(simpleModule);
        return objectMapper.readValue(serializedCatalogueLine, CatalogueLineType.class);
    }

    // These two methods are used to delete binary content from the database permanently

    public static void removeBinaryContentFromDatabase(CatalogueType catalogue) throws JsonProcessingException {
        ObjectMapper objectMapper = new ObjectMapper();
        SimpleModule simpleModule = new SimpleModule();
        simpleModule.addSerializer(BinaryObjectType.class, SpringBridge.getInstance().getBinaryObjectSerializerDelete());
        objectMapper.registerModule(simpleModule);
        objectMapper.writeValueAsString(catalogue);
    }

    public static void removeBinaryContentFromDatabase(CatalogueLineType catalogueLine) throws JsonProcessingException {
        ObjectMapper objectMapper = new ObjectMapper();
        SimpleModule simpleModule = new SimpleModule();
        simpleModule.addSerializer(BinaryObjectType.class, SpringBridge.getInstance().getBinaryObjectSerializerDelete());
        objectMapper.registerModule(simpleModule);
        objectMapper.writeValueAsString(catalogueLine);
    }

    // removes binary contents with the given ids from the database
    public static void removeBinaryContentFromDatabase(List<String> uris) {
        for (String uri : uris) {
            SpringBridge.getInstance().getBinaryContentService().deleteContent(uri);
        }
    }
}
