package eu.nimble.service.catalogue.util.migration.r9;

import eu.nimble.service.catalogue.index.ItemIndexClient;
import eu.nimble.service.catalogue.util.migration.DBConnector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;

/**
 * Created by suat on 18-Apr-19.
 */
@Component
public class LogisticsServiceUpdater extends DBConnector {
    private static final String QUERY_UPDATE_TRANSPORT_SERVICE_CODES = "UPDATE code_type SET uri = 'nimble:category:TransportService' WHERE name_ = 'Transport Service' AND value_ = 'Transport Service'";

    private static final Logger logger = LoggerFactory.getLogger(LogisticsServiceUpdater.class);

    @Autowired
    private ItemIndexClient itemIndexClient;

    public void addUrisToDefaultTransportServiceCategories() {
        this.getConfigs();
        Connection c = null;

        try {
            Class.forName("org.postgresql.Driver");
            c = DriverManager.getConnection(url, username, password);
            logger.info("Connection obtained");

            Statement stmt = c.createStatement();
            int updatedCodeCount = stmt.executeUpdate(QUERY_UPDATE_TRANSPORT_SERVICE_CODES);
            logger.info("Updated code count: {}", updatedCodeCount);


        } catch (Exception e) {
            logger.error("", e);
            System.exit(0);

        } finally {
            closeConnection(c);
        }
        // reindex the catalogues
        itemIndexClient.indexAllCatalogues();

        logger.info("Transport service code update completed");
    }
}
