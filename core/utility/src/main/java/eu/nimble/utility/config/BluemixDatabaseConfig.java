package eu.nimble.utility.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.json.BasicJsonParser;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by suat on 11-Oct-17.
 */
public class BluemixDatabaseConfig {
    private static final Logger logger = LoggerFactory.getLogger(BluemixDatabaseConfig.class);

    private String url;
    private String username;
    private String password;
    private String driver;
    private String schema;

    public BluemixDatabaseConfig(String jsonConfig) {
            BasicJsonParser parser = new BasicJsonParser();
            String originalUrl = (String) parser.parseMap(jsonConfig).get("uri");

            // construct data from 'postgres://username:password@host:port/database'
            Matcher matcher = Pattern.compile("^postgres://(.*?):(.*?)@").matcher(originalUrl);
            matcher.find();
            username = matcher.group(1);
            password = matcher.group(2);
            url = "jdbc:postgresql://" + matcher.replaceAll("");
            driver = "org.postgresql.Driver";
            schema = "public";

            logger.info("Parsed db config url {}, username: {}, password: {} driver: {}", url, username, password, driver, schema);
    }


    public void copyToHibernatePersistenceParameters(BluemixDatabaseConfig bluemixConfig, Map<String, String> existingProperties) {
        existingProperties.put("hibernate.connection.url", bluemixConfig.getUrl());
        existingProperties.put("hibernate.connection.username", bluemixConfig.getUsername());
        existingProperties.put("hibernate.connection.password", bluemixConfig.getPassword());
        existingProperties.put("hibernate.connection.driver_class", bluemixConfig.getDriver());
    }

    public String getUrl() {
        return url;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    public String getDriver() {
        return driver;
    }

    public String getSchema() {
        return schema;
    }
}
