package eu.nimble.utility.config;

import com.sun.istack.NotNull;
import eu.nimble.utility.Configuration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.PropertySource;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Map;

/**
 * Created by suat on 10-Oct-17.
 */
@Component
@EnableConfigurationProperties
@ConfigurationProperties(prefix = "persistence.orm")
@PropertySource("classpath:bootstrap.yml")
public class PersistenceConfig {

    private static PersistenceConfig instance;

    private PersistenceConfig() {
        instance = this;
    }

    public static PersistenceConfig getInstance() {
        return instance;
    }

    @Autowired
    private Environment environment;

    private Map<String, String> ubl;
    private Map<String, String> modaml;

    @Value("persistence.orm.ubl.bluemix.connection.uri")
    private String bluemixUblDbUri;

    @Value("persistence.orm.modaml.bluemix.connection.uri")
    private String bluemixModamlDbUri;

    public Map<String, String> getUbl() {
        return ubl;
    }

    public void setUbl(Map<String, String> ubl) {
        this.ubl = ubl;
    }

    public Map<String, String> getModaml() {
        return modaml;
    }

    public void setModaml(Map<String, String> modaml) {
        this.modaml = modaml;
    }

    public String getBluemixUblDbUri() {
        return bluemixUblDbUri;
    }

    public void setBluemixUblDbUri(@NotNull String bluemixUblDbUri) {
        this.bluemixUblDbUri = bluemixUblDbUri;
        // update persistence properties if kubernetes profile is active
        if(Arrays.stream(environment.getActiveProfiles()).anyMatch(profile -> profile.contentEquals("kubernetes"))) {
            BluemixDatabaseConfig config = new BluemixDatabaseConfig(bluemixUblDbUri);
            config.copyToHibernatePersistenceParameters(config, ubl);
        }
    }

    public String getBluemixModamlDbUri() {
        return bluemixModamlDbUri;
    }

    public void setBluemixModamlDbUri(String bluemixModamlDbUri) {
        this.bluemixModamlDbUri = bluemixModamlDbUri;
        // update persistence properties if kubernetes profile is active
        if(Arrays.stream(environment.getActiveProfiles()).anyMatch(profile -> profile.contentEquals("kubernetes"))) {
            BluemixDatabaseConfig config = new BluemixDatabaseConfig(bluemixModamlDbUri);
            config.copyToHibernatePersistenceParameters(config, modaml);
        }
    }

    public Map<String, String> getPersistenceParameters(String persistenceUnitName) {
        Map<String, String> persistenceParameters = null;
        if(persistenceUnitName.contentEquals(Configuration.UBL_PERSISTENCE_UNIT_NAME)) {
            persistenceParameters = ubl;
        } else if(persistenceUnitName.contentEquals(Configuration.MODAML_PERSISTENCE_UNIT_NAME)) {
            persistenceParameters = modaml;
        } else {
            throw new RuntimeException("Unknown persistence unit name: " + persistenceUnitName);
        }
        return persistenceParameters;
    }

}