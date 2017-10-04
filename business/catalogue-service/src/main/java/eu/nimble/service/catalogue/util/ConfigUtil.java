package eu.nimble.service.catalogue.util;

import eu.nimble.service.catalogue.exception.CatalogueServiceException;

import java.io.IOException;
import java.util.Properties;

/**
 * Created by suat on 25-Jul-17.
 */
public class ConfigUtil {
    public static final String CONFIG_CATALOGUE_PERSISTENCE_MARMOTTA_URL = "catalogue.persistence.marmotta.url";

    private static ConfigUtil configUtil = null;
    private static Properties props = null;

    private ConfigUtil() {

    }

    public static ConfigUtil getInstance() {
        if (configUtil == null) {
            configUtil = new ConfigUtil();
            props = new Properties();
            try {
                props.load(ConfigUtil.class.getClassLoader().getResourceAsStream("application.properties"));
            } catch (IOException e) {
                throw new RuntimeException("Failed to load configuration file", e);
            }
        }
        return configUtil;
    }

    public String getConfig(String key) {
        return props.getProperty(key);
    }
}
