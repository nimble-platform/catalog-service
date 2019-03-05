package eu.nimble.service.catalogue.config;

import eu.nimble.utility.config.BluemixDatabaseConfig;
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

    @Value("${persistence.solr.url}")
    private String solrURL;
    @Value("${persistence.solr.properties-index}")
    private String solrPropertyIndex;

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

    public String getSolrURL() {
        return solrURL;
    }

    public void setSolrURL(String solrURL) {
        this.solrURL = solrURL;
    }

    public String getSolrPropertyIndex() {
        return solrPropertyIndex;
    }

    public void setSolrPropertyIndex(String solrPropertyIndex) {
        this.solrPropertyIndex = solrPropertyIndex;
    }
}
