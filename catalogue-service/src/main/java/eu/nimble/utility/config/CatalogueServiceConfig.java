package eu.nimble.utility.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.PropertySource;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.util.Arrays;

/**
 * Created by suat on 10-Oct-17.
 */
@Component
@PropertySource("classpath:bootstrap.yml")
public class CatalogueServiceConfig {

    @Autowired
    private Environment environment;

    @Value("${persistence.categorydb.bluemix.connection.uri}")
    private String categoryDbBluemixConnectionUri;

    @Value("${spring.application.url}")
    private String springApplicationUrl;

    @Value("${persistence.syncdb.driver}")
    private String syncDbDriver;
    @Value("${persistence.syncdb.connection.url}")
    private String syncdbConnectionUrl;
    @Value("${persistence.syncdb.username}")
    private String syncDbUsername;
    @Value("${persistence.syncdb.password}")
    private String syncDbPassword;
    @Value("${persistence.syncdb.update_check_interval}")
    private Long syncDbUpdateCheckInterval;

    @Value("${persistence.categorydb.driver}")
    private String categoryDbDriver;
    @Value("${persistence.categorydb.connection.url}")
    private String categoryDbConnectionUrl;
    @Value("${persistence.categorydb.username}")
    private String categoryDbUsername;
    @Value("${persistence.categorydb.password}")
    private String categoryDbPassword;
    @Value("${persistence.categorydb.schema}")
    private String categoryDbScheme;

    @Value("${persistence.marmotta.url}")
    private String marmottaUrl;

    private static CatalogueServiceConfig instance;

    private CatalogueServiceConfig() {
        instance = this;
    }

    public static CatalogueServiceConfig getInstance() {
        return instance;
    }

    public String getCategoryDbBluemixConnectionUri() {
        return categoryDbBluemixConnectionUri;
    }

    public void setCategoryDbBluemixConnectionUri(String categoryDbBluemixConnectionUri) {
        this.categoryDbBluemixConnectionUri = categoryDbBluemixConnectionUri;
        // update persistence properties if kubernetes profile is active
        if(Arrays.stream(environment.getActiveProfiles()).anyMatch(profile -> profile.contentEquals("kubernetes"))) {
            BluemixDatabaseConfig config = new BluemixDatabaseConfig(categoryDbBluemixConnectionUri);
            setCategoryDbConnectionUrl(config.getUrl());
            setCategoryDbUsername(config.getUsername());
            setCategoryDbPassword(config.getPassword());
            setCategoryDbDriver(config.getDriver());
            setCategoryDbDriver(config.getSchema());
        }
    }

    public String getSpringApplicationUrl() {
        return springApplicationUrl;
    }

    public void setSpringApplicationUrl(String springApplicationUrl) {
        this.springApplicationUrl = springApplicationUrl;
    }

    public String getSyncDbDriver() {
        return syncDbDriver;
    }

    public void setSyncDbDriver(String syncDbDriver) {
        this.syncDbDriver = syncDbDriver;
    }

    public String getSyncdbConnectionUrl() {
        return syncdbConnectionUrl;
    }

    public void setSyncdbConnectionUrl(String syncdbConnectionUrl) {
        this.syncdbConnectionUrl = syncdbConnectionUrl;
    }

    public String getSyncDbUsername() {
        return syncDbUsername;
    }

    public void setSyncDbUsername(String syncDbUsername) {
        this.syncDbUsername = syncDbUsername;
    }

    public String getSyncDbPassword() {
        return syncDbPassword;
    }

    public void setSyncDbPassword(String syncDbPassword) {
        this.syncDbPassword = syncDbPassword;
    }

    public Long getSyncDbUpdateCheckInterval() {
        return syncDbUpdateCheckInterval;
    }

    public void setSyncDbUpdateCheckInterval(Long syncDbUpdateCheckInterval) {
        this.syncDbUpdateCheckInterval = syncDbUpdateCheckInterval;
    }

    public String getCategoryDbDriver() {
        return categoryDbDriver;
    }

    public void setCategoryDbDriver(String categoryDbDriver) {
        this.categoryDbDriver = categoryDbDriver;
    }

    public String getCategoryDbConnectionUrl() {
        return categoryDbConnectionUrl;
    }

    public void setCategoryDbConnectionUrl(String categoryDbConnectionUrl) {
        this.categoryDbConnectionUrl = categoryDbConnectionUrl;
    }

    public String getCategoryDbUsername() {
        return categoryDbUsername;
    }

    public void setCategoryDbUsername(String categoryDbUsername) {
        this.categoryDbUsername = categoryDbUsername;
    }

    public String getCategoryDbPassword() {
        return categoryDbPassword;
    }

    public void setCategoryDbPassword(String categoryDbPassword) {
        this.categoryDbPassword = categoryDbPassword;
    }

    public String getCategoryDbScheme() {
        return categoryDbScheme;
    }

    public void setCategoryDbScheme(String categoryDbScheme) {
        this.categoryDbScheme = categoryDbScheme;
    }

    public String getMarmottaUrl() {
        return marmottaUrl;
    }

    public void setMarmottaUrl(String marmottaUrl) {
        this.marmottaUrl = marmottaUrl;
    }
}
