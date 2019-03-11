package eu.nimble.service.catalogue.util.migration.r8;

import eu.nimble.service.catalogue.category.TaxonomyEnum;
import org.slf4j.LoggerFactory;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.PropertySource;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.sql.*;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Created by suat on 19-Feb-19.
 */
public class EClassNamespaceUpdater {
    private static final String QUERY_GET_ECLASS_ITEM_PROPERTIES = "select id from item_property_type where id like '0173-1%' or id like '%eclass%' or uri like '%eclass%'";
    private static final String QUERY_UPDATE_ITEM_PROPERTIES = "update item_property_type set id = ?, uri = ? where id = ?";
    private static final String QUERY_GET_FURNITURE_ONTOLOGY_ITEM_PROPERTIES = "select id from item_property_type where id like '%FurnitureSector%'";
    private static final String QUERY_GET_ECLASS_CODES = "select uri, value_ from code_type where uri like '%eclass%'";
    private static final String QUERY_UPDATE_CODES = "update code_type set uri = ?, value_ = ? where value_ = ?";
    private static final String QUERY_GET_FURNITURE_ONTOLOGY_CODES = "select uri, value_ from code_type where uri like '%FurnitureSector%'";
    private static final String QUERY_GET_FURNITURE_ONTOLOGY_CODES_WITH_NULL_URIS = "select value_ from code_type where uri is null and value_ like '%FurnitureSector%' and value_ not like '%::%'";

    private static final org.slf4j.Logger logger = LoggerFactory.getLogger(EClassNamespaceUpdater.class);

    private static String url;
    private static String username;
    private static String password;

    public static void main(String[] args) {
        EClassNamespaceUpdater script = new EClassNamespaceUpdater();
        Connection c = null;

        try {
            Map configs = script.getConfigs();
//            HibernateUtility hu = HibernateUtility.getInstance(Configuration.UBL_PERSISTENCE_UNIT_NAME, configs);

            Class.forName("org.postgresql.Driver");
            c = DriverManager.getConnection(url, username, password);
            logger.info("Connection obtained");

            // correct item property type uris
            script.fixUrisInItemProperties(c);
            // correct eclass uris in codes
            script.fixEClassCategoryUrisInCodes(c);
            // correct furniture ontology uris in codes
            script.fixFurnitureOntologyUrisInCodes(c);
            // correct furniture ontology uris with null codes
            script.fixFurnitureOntologyNullUrisInCodes(c);

        } catch (Exception e) {
            logger.error("", e);
            System.exit(0);

        } finally {
            try {
                if (c != null) {
                    c.close();
                }
            } catch (Exception e) {
                logger.error("", e);
            }
        }
    }

    private void fixUrisInItemProperties(Connection c) {
        Statement stmt = null;
        PreparedStatement pstmt = null;
        try {
            stmt = c.createStatement();

            ResultSet rs = stmt.executeQuery(QUERY_GET_ECLASS_ITEM_PROPERTIES);
            Set<String> ids = new HashSet<>();
            while (rs.next()) {
                ids.add(rs.getString(1));
            }
            rs.close();
            logger.info("Eclass ids obtained. size: {}", ids.size());

            pstmt = c.prepareStatement(QUERY_UPDATE_ITEM_PROPERTIES);
            for (String id : ids) {
                String uri = TaxonomyEnum.eClass.getNamespace() + id;
                pstmt.setString(1, uri);
                pstmt.setString(2, uri);
                pstmt.setString(3, id);
                pstmt.execute();
                pstmt.clearParameters();
            }
            logger.info("Eclass property uris are updated");

            rs = stmt.executeQuery(QUERY_GET_FURNITURE_ONTOLOGY_ITEM_PROPERTIES);
            ids = new HashSet<>();
            while (rs.next()) {
                ids.add(rs.getString(1));
            }
            rs.close();
            logger.info("Furniture ontology ids obtained. size: {}", ids.size());

            pstmt = c.prepareStatement(QUERY_UPDATE_ITEM_PROPERTIES);
            for (String id : ids) {
                pstmt.setString(1, id);
                pstmt.setString(2, id);
                pstmt.setString(3, id);
                pstmt.execute();
                pstmt.clearParameters();
            }
            logger.info("Furniture ontology property uris are updated");


        } catch (Exception e) {
            logger.error("", e);
            System.exit(0);
        } finally {
            try {
                if (stmt != null) {
                    stmt.close();
                }
            } catch (Exception e) {
                logger.error("", e);
            }
            try {
                if (pstmt != null) {
                    pstmt.close();
                }
            } catch (Exception e) {
                logger.error("", e);
            }
        }
    }

    private void fixEClassCategoryUrisInCodes(Connection c) {
        Statement stmt = null;
        PreparedStatement pstmt = null;
        try {
            stmt = c.createStatement();

            ResultSet rs = stmt.executeQuery(QUERY_GET_ECLASS_CODES);
            Set<String> ids = new HashSet<>();
            while (rs.next()) {
                ids.add(rs.getString("value_"));
            }
            rs.close();
            logger.info("Ids obtained. size: {}", ids.size());

            pstmt = c.prepareStatement(QUERY_UPDATE_CODES);
            for (String id : ids) {
                String uri = TaxonomyEnum.eClass.getNamespace() + id;
                pstmt.setString(1, uri);
                pstmt.setString(2, uri);
                pstmt.setString(3, id);
                pstmt.execute();
                pstmt.clearParameters();
            }
            logger.info("properties uris are updated");

        } catch (Exception e) {
            logger.error("", e);
            System.exit(0);
        } finally {
            try {
                if (stmt != null) {
                    stmt.close();
                }
            } catch (Exception e) {
                logger.error("", e);
            }
            try {
                if (pstmt != null) {
                    pstmt.close();
                }
            } catch (Exception e) {
                logger.error("", e);
            }
        }
    }

    private void fixFurnitureOntologyUrisInCodes(Connection c) {
        Statement stmt = null;
        PreparedStatement pstmt = null;
        try {
            stmt = c.createStatement();

            ResultSet rs = stmt.executeQuery(QUERY_GET_FURNITURE_ONTOLOGY_CODES);
            Set<String> values = new HashSet<>();
            while (rs.next()) {
                values.add(rs.getString("value_"));
            }
            rs.close();
            logger.info("Furniture ontology value obtained. size: {}", values.size());

            pstmt = c.prepareStatement(QUERY_UPDATE_CODES);
            for (String value : values) {
                pstmt.setString(1, value);
                pstmt.setString(2, value);
                pstmt.setString(3, value);
                pstmt.execute();
                pstmt.clearParameters();
            }
            logger.info("properties uris are updated");

        } catch (Exception e) {
            logger.error("", e);
            System.exit(0);
        } finally {
            try {
                if (stmt != null) {
                    stmt.close();
                }
            } catch (Exception e) {
                logger.error("", e);
            }
            try {
                if (pstmt != null) {
                    pstmt.close();
                }
            } catch (Exception e) {
                logger.error("", e);
            }
        }
    }

    private void fixFurnitureOntologyNullUrisInCodes(Connection c) {
        Statement stmt = null;
        PreparedStatement pstmt = null;
        try {
            stmt = c.createStatement();

            ResultSet rs = stmt.executeQuery(QUERY_GET_FURNITURE_ONTOLOGY_CODES_WITH_NULL_URIS);
            Set<String> values = new HashSet<>();
            while (rs.next()) {
                values.add(rs.getString("value_"));
            }
            rs.close();
            logger.info("Furniture ontology codes with null uris obtained. size: {}", values.size());

            pstmt = c.prepareStatement(QUERY_UPDATE_CODES);
            for (String value : values) {
                pstmt.setString(1, value);
                pstmt.setString(2, value);
                pstmt.setString(3, value);
                pstmt.execute();
                pstmt.clearParameters();
            }
            logger.info("null core uris are updated");

        } catch (Exception e) {
            logger.error("", e);
            System.exit(0);
        } finally {
            try {
                if (stmt != null) {
                    stmt.close();
                }
            } catch (Exception e) {
                logger.error("", e);
            }
            try {
                if (pstmt != null) {
                    pstmt.close();
                }
            } catch (Exception e) {
                logger.error("", e);
            }
        }
    }

    private Map getConfigs() {
        YamlPropertySourceLoader loader = new YamlPropertySourceLoader();
        try {
            PropertySource<?> applicationYamlPropertySource = loader.load(
                    "properties", new ClassPathResource("releases/r5/r5migration.yml"), null);

            Map map = ((MapPropertySource) applicationYamlPropertySource).getSource();

            String url = (String) map.get("hibernate.connection.url");
            url = url.replace("${DB_HOST}", System.getenv("DB_HOST")).
                    replace("${DB_PORT}", System.getenv("DB_PORT")).
                    replace("${DB_DATABASE}", System.getenv("DB_DATABASE"));

            // set staging parameters
            EClassNamespaceUpdater.url = url;
            EClassNamespaceUpdater.username = System.getenv("DB_USERNAME");
            EClassNamespaceUpdater.password = System.getenv("DB_PASSWORD");
            //

            map.put("hibernate.connection.url", url);
            map.put("hibernate.connection.username", EClassNamespaceUpdater.username);
            map.put("hibernate.connection.password", EClassNamespaceUpdater.password);


            return map;

        } catch (IOException e) {
            logger.error("", e);
            throw new RuntimeException();
        }
    }
}
