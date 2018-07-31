package eu.nimble.service.catalogue.sync;

import eu.nimble.service.catalogue.CatalogueService;
import eu.nimble.service.catalogue.CatalogueServiceImpl;
import eu.nimble.service.catalogue.util.SpringBridge;
import eu.nimble.utility.config.CatalogueServiceConfig;
import eu.nimble.service.model.ubl.catalogue.CatalogueType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by suat on 01-Oct-17.
 * <p>
 * This class synchronizes the data stored in the catalogue database with Marmotta in a unidirectional way. Once an
 * operation e.g. add, delete, update is performed for relational DB and Marmotta, it assumes that both sides are in sync.
 * The synchronization is triggered when a new operation starts on the relational DB side. Then, the corresponding
 * operation is performed for Marmotta.
 */
public class MarmottaSynchronizer {

    private static final Logger logger = LoggerFactory.getLogger(MarmottaSynchronizer.class);

    private static final String SQL_GET_RECORDS =
            "SELECT * FROM SYNCSTATUS";

    private static final String SQL_DELETE_RECORD =
            "DELETE FROM SYNCSTATUS WHERE " +
                    "UUID = '%s';";

    private static final String SQL_ADD_RECORD =
            "INSERT INTO SYNCSTATUS VALUES" +
                    "('%s','%s');";

    private static final String SQL_CREATE_TABLE =
            "CREATE TABLE SYNCSTATUS (\n" +
                    "    UUID varchar(255),\n" +
                    "    STATUS varchar(255)\n" +
                    ");";

    private static MarmottaSynchronizer instance = null;
    private MarmottaClient marmottaClient = new MarmottaClient();
    private CatalogueService catalogueService = CatalogueServiceImpl.getInstance();
    private Thread syncThread = null;
    private boolean sync = true;

    private MarmottaSynchronizer() {

    }

    public static void main(String[] args) throws SQLException, InterruptedException {
        MarmottaSynchronizer sync = new MarmottaSynchronizer();
        /*sync.createStatusTable();
        String uuid = "catUuid";
        sync.addRecord(SyncStatus.UPDATE, uuid);
        List<SyncStatusRecord> records = sync.getStatusRecords();
        System.out.println(records.toString());
        sync.deleteStatusRecords(uuid);
        records = sync.getStatusRecords();
        System.out.println(records);
        System.out.println("done");*/
    }

    public static MarmottaSynchronizer getInstance() {
        if (instance == null) {
            instance = new MarmottaSynchronizer();
        }
        return instance;
    }

    public void stopSynchronization() {
        syncThread.interrupt();
        logger.info("Synchronization thread interrupted");
    }

    public void startSynchronization() {
        createStatusTable();
        syncThread = new Thread(() -> {
            long interval = SpringBridge.getInstance().getCatalogueServiceConfig().getSyncDbUpdateCheckInterval();
            while (sync) {
                try {
                    List<SyncStatusRecord> records = getStatusRecords();
                    if(records.size() > 0) {
                        logger.info("Size of catalogue synchronization records: {}", records.size());
                    }

                    for (SyncStatusRecord record : records) {
                        logger.info("Processing {} record for catalogue: {}", record.getSyncStatus(), record.getCatalogueUuid());
                        try {
                            if (record.getSyncStatus().equals(SyncStatus.ADD)) {
                                CatalogueType catalogue = catalogueService.getCatalogue(record.getCatalogueUuid());
                                marmottaClient.submitCatalogueDataToMarmotta(catalogue);
                                logger.info("Processed add sync status for catalogue: {}", record.getCatalogueUuid());

                            } else if (record.getSyncStatus().equals(SyncStatus.UPDATE)) {
                                CatalogueType catalogue = catalogueService.getCatalogue(record.getCatalogueUuid());
                                marmottaClient.deleteCatalogueFromMarmotta(catalogue.getUUID());
                                marmottaClient.submitCatalogueDataToMarmotta(catalogue);
                                logger.info("Processed update sync status for catalogue: {}", record.getCatalogueUuid());

                            } else if (record.getSyncStatus().equals(SyncStatus.DELETE)) {
                                marmottaClient.deleteCatalogueFromMarmotta(record.getCatalogueUuid());
                                logger.info("Processed delete sync status for catalogue: {}", record.getCatalogueUuid());
                            }
                            deleteStatusRecords(record.getCatalogueUuid());
                        } catch (Exception e) {
                            logger.error("An error occurred during the synchronization", e);
                        }
                    }
                    logger.debug("Processed sync status updates. Size: {}", records.size());

                } catch (Exception e) {
                    logger.error("Failed to get synchronization records", e);
                }

                try {
                    Thread.sleep(interval);
                } catch (InterruptedException e) {
                    logger.info("Wake up. Interrupted Marmotta Synchronizer thread", e);
                    break;
                }

                if (Thread.currentThread().isInterrupted()) {
                    logger.info("Interrupted Marmotta Synchronizer thread");
                    break;
                }
            }

            logger.info("Marmotta synchronization has been stopped");
        });
        syncThread.start();
        logger.info("Synchronization thread started");
    }

    private List<SyncStatusRecord> getStatusRecords() {
        Connection c = getConnection();
        if(c == null) {
            logger.info("No connection to delete status record");
            return new ArrayList<>();
        }

        Statement s = null;
        List<SyncStatusRecord> records = new ArrayList<>();

        try {
            String query = String.format(SQL_GET_RECORDS);
            s = c.createStatement();
            ResultSet rs = s.executeQuery(query);

            while (rs.next()) {
                SyncStatusRecord record = new SyncStatusRecord();
                record.setCatalogueUuid(rs.getString("UUID"));
                record.setSyncStatus(SyncStatus.valueOf(rs.getString("STATUS")));
                records.add(record);
            }

            logger.debug("Retrieved sync records. Size: {}", records.size());
            return records;

        } catch (SQLException e) {
            logger.error("Failed to get sync status records ", e);
        } finally {
            closeStatement(s);
            closeConnection(c);
        }
        return records;
    }

    public void addRecord(SyncStatus updateType, String catalogueUuid) {
        Connection c = getConnection();
        if(c == null) {
            logger.info("No connection to add status record");
            return;
        }
        Statement s = null;

        try {
            String addQuery = String.format(SQL_ADD_RECORD, catalogueUuid, updateType);
            String deleteQuery = String.format(SQL_DELETE_RECORD, catalogueUuid);
            s = c.createStatement();
            // first remove the existing record for preventing multiple processing of the same catalogue
            s.addBatch(deleteQuery);
            s.addBatch(addQuery);
            s.executeBatch();

            logger.info("Added sync record for catalogue: {} update type: {}", catalogueUuid, updateType);

        } catch (SQLException e) {
            logger.error("Failed to add sync status record for catalogue: {} update type: {}", catalogueUuid, updateType, e);
        } finally {
            closeStatement(s);
            closeConnection(c);
        }
    }

    private void deleteStatusRecords(String catalogueUuid) {
        Connection c = getConnection();
        if(c == null) {
            logger.info("No connection to delete status record");
            return;
        }

        Statement s = null;

        try {
            String query = String.format(SQL_DELETE_RECORD, catalogueUuid);
            s = c.createStatement();
            s.execute(query);
            logger.info("Deleted sync record(s) for catalogue: {}", catalogueUuid);

        } catch (SQLException e) {
            logger.error("Failed to delete sync status record for catalogue: {}", catalogueUuid, e);
        } finally {
            closeStatement(s);
            closeConnection(c);
        }
    }

    private void createStatusTable() {
        Connection c = getConnection();
        if(c == null) {
            logger.info("No connection to create status table");
            return;
        }

        try {
            // first execute a dummy select query to check whether the table exists
            Statement stmt = c.createStatement();
            stmt.executeQuery("SELECT * FROM syncStatus LIMIT 1");
            logger.info("Sync status table exists");

        } catch (SQLException e) {
            if (e.getSQLState().contentEquals("42S02") || e.getSQLState().contentEquals("42P01")) {
                logger.info("Sync status table does not exist");

                try {
                    Statement stmt = c.createStatement();
                    stmt.execute(SQL_CREATE_TABLE);
                    stmt.close();
                    logger.info("Sync status table created");

                } catch (SQLException e1) {
                    String msg = "Failed to create sync status table";
                    logger.error(msg, e1);
                    throw new RuntimeException(msg, e1);
                }

            } else {
                String msg = "Error while executing the dummy select query";
                logger.error(msg, e);
                throw new RuntimeException(msg, e);
            }

        } finally {
            closeConnection(c);
        }
    }

    private Connection getConnection() {
        try {
            CatalogueServiceConfig config = SpringBridge.getInstance().getCatalogueServiceConfig();
            Class.forName(config.getSyncDbDriver());
            Connection connection = DriverManager
                    .getConnection(config.getSyncdbConnectionUrl(), config.getSyncDbUsername(), config.getSyncDbPassword());

            return connection;

        } catch (SQLException | ClassNotFoundException e) {
            String msg = "Failed to get DB connection";
            logger.error(msg, e);
            return null;
        }
    }

    private void closeStatement(Statement statement) {
        try {
            if (statement != null) {
                statement.close();
            }
        } catch (SQLException e) {
            logger.error("Failed to close statement", e);
        }
    }

    private void closeConnection(Connection connection) {
        try {
            if (connection != null) {
                connection.close();
            }
        } catch (SQLException e) {
            logger.error("Failed to close connection", e);
        }
    }

    public enum SyncStatus {
        ADD, UPDATE, DELETE
    }

    private class SyncStatusRecord {
        private String catalogueUuid;
        private SyncStatus syncStatus;

        public String getCatalogueUuid() {
            return catalogueUuid;
        }

        public void setCatalogueUuid(String catalogueUuid) {
            this.catalogueUuid = catalogueUuid;
        }

        public SyncStatus getSyncStatus() {
            return syncStatus;
        }

        public void setSyncStatus(SyncStatus syncStatus) {
            this.syncStatus = syncStatus;
        }
    }
}
