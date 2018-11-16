package eu.nimble.utility.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.PropertySource;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.Arrays;

/**
 * Created by suat on 10-Oct-17.
 */
@Component
@PropertySource("classpath:bootstrap.yml")
public class CatalogueServiceConfig {

    private static final Logger logger = LoggerFactory.getLogger(CatalogueServiceConfig.class);

    @Autowired
    private Environment environment;

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
    @Value("${persistence.marmotta.sync}")
    private boolean marmottaSync;
    @Value("${nimble.identity.url}")
    private String identityUrl;
    @Value("${persistence.solr.properties-index}")
    private String solrIndex;
    @Value("${persistence.solr.url}")
    private String solrURL;

    @PostConstruct
    private void setupDBConnections() {
        if (environment != null) {
            // check for "kubernetes" profile
            if (Arrays.stream(environment.getActiveProfiles()).anyMatch(profile -> profile.contentEquals("kubernetes"))) {

                // setup category database
                String categoryDBCredentialsJson = environment.getProperty("persistence.categorydb.bluemix.credentials_json");
                BluemixDatabaseConfig categoryDBconfig = new BluemixDatabaseConfig(categoryDBCredentialsJson);
                setCategoryDbConnectionUrl(categoryDBconfig.getUrl());
                setCategoryDbUsername(categoryDBconfig.getUsername());
                setCategoryDbPassword(categoryDBconfig.getPassword());
                setCategoryDbDriver(categoryDBconfig.getDriver());
                setCategoryDbScheme(categoryDBconfig.getSchema());
            }
        } else {
            logger.warn("Environment not initialised!");
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

    public String getIdentityUrl() {
        return identityUrl;
    }

    public void setIdentityUrl(String identityUrl) {
        this.identityUrl = identityUrl;
    }

    public boolean isMarmottaSync() {
        return marmottaSync;
    }

    public void setMarmottaSync(boolean marmottaSync) {
        this.marmottaSync = marmottaSync;
    }

    public String getSolrIndex() {
        return solrIndex;
    }

    public void setSolrIndex(String solrIndex) {
        this.solrIndex = solrIndex;
    }

    public String getSolrURL() {
        return solrURL;
    }

    public void setSolrURL(String solrURL) {
        this.solrURL = solrURL;
    }
}
