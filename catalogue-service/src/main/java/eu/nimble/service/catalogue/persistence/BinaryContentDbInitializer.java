package eu.nimble.service.catalogue.persistence;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.sql.DataSource;
import java.sql.*;

/**
 * This component checks whether the binary_content table is available in the binarycontentdb during the initialization
 * of the service. If not, the table is created.
 *
 * Created by suat on 19-Dec-18.
 */
@Component
public class BinaryContentDbInitializer {

    private static final String CREATE_TABLE_SQL =
            "CREATE TABLE public.binary_content (" +
                    "id varchar(2048) NOT NULL," +
                    "mime_code varchar(255) NULL," +
                    "file_name text NULL," +
                    "value_ bytea NULL," +
                    "CONSTRAINT binary_content_pkey PRIMARY KEY (id)" +
                    ")" +
                    "WITH ( OIDS=FALSE) ;";

    private static final String QUERY_TABLE_SQL = "SELECT to_regclass('binary_content')";

    private static Logger logger = LoggerFactory.getLogger(BinaryContentDbInitializer.class);

    @Autowired
    @Qualifier("binarycontentdbDataSource")
    private DataSource binaryContentDataSource;

    @PostConstruct
    public void initializeBinaryContentDb() {
        Connection c = null;
        Statement s = null;
        ResultSet rs = null;
        try {
            c = binaryContentDataSource.getConnection();
            s = c.createStatement();
            rs = s.executeQuery(QUERY_TABLE_SQL);

            if(rs.next()) {
                String tableName = rs.getString(1);
                if(tableName == null) {
                    s.execute(CREATE_TABLE_SQL);
                    logger.info("created binary_content table in the binary content database");
                } else {
                    logger.info("binary_content table is already available in the binary content database");
                }
            }

        } catch (SQLException e) {
            String msg = "Failed to initialize binary_content table";
            logger.error(msg, e);
            throw new RuntimeException(msg, e);
        } finally {
            closeResources(c, s, rs);
        }
    }

    private void closeResources(Connection c, Statement ps, ResultSet rs) {
        if (c != null) {
            try {
                if (!c.isClosed()) {
                    c.close();
                }
            } catch (SQLException e) {
                logger.warn("Failed to close connection", e);
            }
        }
        if (ps != null) {
            try {
                if (!ps.isClosed()) {
                    ps.close();
                }
            } catch (SQLException e) {
                logger.warn("Failed to close statement", e);
            }
        }
        if (rs != null) {
            try {
                if (!rs.isClosed()) {
                    rs.close();
                }
            } catch (SQLException e) {
                logger.warn("Failed to close result set", e);
            }
        }
    }
}
