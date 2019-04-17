package eu.nimble.service.catalogue.category.eclass.database;

import eu.nimble.service.catalogue.category.TaxonomyEnum;
import eu.nimble.service.catalogue.model.category.*;
import eu.nimble.service.catalogue.util.SpringBridge;
import eu.nimble.service.catalogue.config.CatalogueServiceConfig;
import eu.nimble.service.catalogue.exception.CategoryDatabaseException;
import eu.nimble.service.catalogue.template.TemplateConfig;
import eu.nimble.service.model.ubl.extension.ItemPropertyValueQualifier;
import eu.nimble.service.model.ubl.commonbasiccomponents.TextType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.*;
import java.util.stream.Collectors;

import static eu.nimble.service.catalogue.category.eclass.database.EClassCategoryDatabaseConfig.*;
import static eu.nimble.service.catalogue.category.eclass.database.EClassCategoryDatabaseConfig.eClassQueryGetRootCategories;

/**
 * Created by suat on 03-Mar-17.
 */
public class EClassCategoryDatabaseAdapter {
    private static final Logger logger = LoggerFactory.getLogger(EClassCategoryDatabaseAdapter.class);

    private String defaultLanguage = "en";
    /**
     * The record counts below are specified based on the eClass Basic 10.0 version. In case of any eclass taxonomy update,
     * the numbers should also be updated.
     */
    private static final int RECORD_COUNT_CLASSIFICATION_CLASS = 41647;
    private static final int RECORD_COUNT_CLASSIFICATION_CLASS_PROPERTY = 1274609;
    private static final int RECORD_COUNT_CLASSIFICATION_CLASS_PROPERTY_VALUE = 9664205;
    private static final int RECORD_COUNT_ECLASS_VALUE = 15708;
    private static final int RECORD_COUNT_KEYWORD_SYNONYM = 55497;
    private static final int RECORD_COUNT_PROPERTY = 17342;
    private static final int RECORD_COUNT_PROPERTY_VALUE = 7782;
    private static final int RECORD_COUNT_UNIT = 997;

    public static void main(String[] args) throws CategoryDatabaseException, SQLException, ClassNotFoundException {
        /*EClassCategoryDatabaseAdapter e = new EClassCategoryDatabaseAdapter();
        Connection c = e.getConnection();
        List<Category> results = e.getClassificationClassesByName("yarn");
        //List<Category> results = e.getClassificationClassesByLevel(1);
        //List<Category> results = e.getSubCategories("0173-1#01-ADG629#002");
        for (Category cc : results) {
            System.out.println(cc.getPreferredName());
        }
        c.close();*/
    }

    private Connection getConnection() throws CategoryDatabaseException {
        try {
            CatalogueServiceConfig config = SpringBridge.getInstance().getCatalogueServiceConfig();

            Class.forName(config.getCategoryDbDriver());
            Connection connection = DriverManager
                    .getConnection(config.getCategoryDbConnectionUrl(), config.getCategoryDbUsername(), config.getCategoryDbPassword());

            PreparedStatement preparedStatement = connection.prepareStatement(eClassQuerySetPostgresDatabaseSchema(config.getCategoryDbScheme()));
            preparedStatement.execute();

            return connection;

        } catch (SQLException | ClassNotFoundException e) {
            throw new CategoryDatabaseException("Failed to get connection", e);
        }

        /*Class.forName(PRODUCT_CATEGORY_H2_CONFIG_DRIVER);
        Connection connection = DriverManager
                .getConnection(PRODUCT_CATEGORY_H2_CONFIG_URL, PRODUCT_CATEGORY_H2_CONFIG_USER, PRODUCT_CATEGORY_H2_CONFIG_PASSWORD);*/
        // check h2 if there is metadata about a table to deduce whether it exists in the H2 DB already
        /*ResultSet meta = connection.getMetaData().getTables(null, null, TABLE_NAME_CLASSIFICATION_CLASS, null);
        if(meta.next() == false) {
            InputStream is = EClassCategoryDatabaseAdapter.class.getResourceAsStream("/eClassLoader.sql");
            try {
                String initScript = IOUtils.toString(is);
                initScript = initScript.replace("{dataPath}", EClassCategoryDatabaseConfig.PRODUCT_CATEGORY_DATA_PATH);
                Statement stmt = connection.createStatement();
                stmt.execute(initScript);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }*/
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

    public List<Category> getAllCategories() throws Exception{
        Connection connection = null;
        List<Category> results;
        try {
            connection = getConnection();
            PreparedStatement preparedStatement = connection.prepareStatement(eClassQueryGetAllCategories());
            ResultSet rs = preparedStatement.executeQuery();
            results = extractClassificationClassesFromResultSet(rs);
            rs.close();
            preparedStatement.close();

        } finally {
            closeConnection(connection);
        }
        return results;
    }

    public Map<String, List<Property>> getAllProperties() throws Exception {
        Connection connection = null;

        try {
            connection = getConnection();

            // get properties without the unit and allowed values. They are queried separately.
            Map<String, List<Property>> properties;
            PreparedStatement preparedStatement = connection.prepareStatement(eClassQueryGetAllProperties());
            ResultSet rs = preparedStatement.executeQuery();
            properties = extractCategoryPropertyMapFromResultSet(rs);
            rs.close();
            preparedStatement.close();

            return properties;
        } catch (SQLException e) {
            throw new CategoryDatabaseException("Failed to retrieve properties for the category", e);
        } finally {
            closeConnection(connection);
        }
    }



    private List<Category> extractClassificationClassesFromResultSet(ResultSet rs) throws SQLException {
        List<Category> results = new ArrayList<>();
        while (rs.next()) {
            Category cc = new Category();
            cc.setCode(rs.getString(COLUMN_CLASSIFICATION_CLASS_CODED_NAME));
            // create a TextType for category definition
            TextType textType = new TextType();
            textType.setLanguageID(defaultLanguage);
            textType.setValue(rs.getString(COLUMN_CLASSIFICATION_CLASS_DEFINITION));
            cc.setDefinition(Arrays.asList(textType));
            cc.setId(rs.getString(COLUMN_CLASSIFICATION_CLASS_IRDICC));
            cc.setLevel(Integer.valueOf(rs.getString(COLUMN_CLASSIFICATION_CLASS_LEVEL)));

            cc.addPreferredName(rs.getString(COLUMN_CLASSIFICATION_CLASS_PREFERRED_NAME), defaultLanguage);
            cc.setNote(rs.getString(COLUMN_CLASSIFICATION_CLASS_NOTE));
            cc.setRemark(rs.getString(COLUMN_CLASSIFICATION_CLASS_REMARK));
            cc.setTaxonomyId("eClass");
            cc.setCategoryUri(TaxonomyEnum.eClass.getNamespace() + cc.getId());
            results.add(cc);
        }
        return results;
    }

    private Map<String, List<Property>> extractCategoryPropertyMapFromResultSet(ResultSet rs) throws SQLException {
        // category id - property map
        Map<String, List<Property>> results = new LinkedHashMap<>();
        while (rs.next()) {
            String categoryId = rs.getString(COLUMN_CLASSIFICATION_CLASS_PROPERTY_IRDI_CC);
            List<Property> categoryProperties = results.get(categoryId);
            if(categoryProperties == null) {
                categoryProperties = new ArrayList<>();
                results.put(categoryId, categoryProperties);
            }

            Property prop = new Property();
            prop.setId(TaxonomyEnum.eClass.getNamespace() + rs.getString(COLUMN_PROPERTY_IRDI_PR));
            // create a TextType for property definition
            TextType textType = new TextType();
            textType.setLanguageID(defaultLanguage);
            textType.setValue(rs.getString(COLUMN_PROPERTY_PREFERRED_NAME));
            prop.setPreferredName(Arrays.asList(textType));
            prop.setShortName(rs.getString(COLUMN_PROPERTY_SHORT_NAME));
            prop.setDefinition(rs.getString(COLUMN_PROPERTY_DEFINITION));
            prop.setNote(rs.getString(COLUMN_PROPERTY_NOTE));
            prop.setRemark(rs.getString(COLUMN_PROPERTY_REMARK));
            prop.setPreferredSymbol(rs.getString(COLUMN_PROPERTY_PREFERRED_SYMBOL));
            prop.setIecCategory(rs.getString(COLUMN_PROPERTY_CATEGORY));
            prop.setAttributeType(rs.getString(COLUMN_PROPERTY_ATTRIBUTE_TYPE));
            prop.setValueQualifier(rs.getString(COLUMN_PROPERTY_DATA_TYPE));
            prop.setUri(TaxonomyEnum.eClass.getNamespace() + rs.getString(COLUMN_PROPERTY_IRDI_PR));

            categoryProperties.add(prop);
        }
        return results;
    }

    public void checkTableCounts() {
        Connection c = null;
        Statement s = null;
        try {
            c = getConnection();
            s = c.createStatement();

            String query = "Select Count(*) From classification_class";
            ResultSet rs = s.executeQuery(query);
            int count;
            if(rs.next()) {
                count = rs.getInt(1);
                if(count != RECORD_COUNT_CLASSIFICATION_CLASS) {
                    logger.warn("Number of records in classification_class table is {}, should be {}", count, RECORD_COUNT_CLASSIFICATION_CLASS);
                }
            }
            rs.close();

            query = "Select Count(*) From classification_class_property";
            rs = s.executeQuery(query);
            if(rs.next()) {
                count = rs.getInt(1);
                if(count != RECORD_COUNT_CLASSIFICATION_CLASS_PROPERTY) {
                    logger.warn("Number of records in classification_class_property table is {}, should be {}", count, RECORD_COUNT_CLASSIFICATION_CLASS_PROPERTY);
                }
            }
            rs.close();

            query = "Select Count(*) From classification_class_property_value";
            rs = s.executeQuery(query);
            if(rs.next()) {
                count = rs.getInt(1);
                if(count != RECORD_COUNT_CLASSIFICATION_CLASS_PROPERTY_VALUE) {
                    logger.warn("Number of records in classification_class_property_value table is {}, should be {}", count, RECORD_COUNT_CLASSIFICATION_CLASS_PROPERTY_VALUE);
                }
            }
            rs.close();

            query = "Select Count(*) From eclass_value";
            rs = s.executeQuery(query);
            if(rs.next()) {
                count = rs.getInt(1);
                if(count != RECORD_COUNT_ECLASS_VALUE) {
                    logger.warn("Number of records in eclass_value table is {}, should be {}", count, RECORD_COUNT_ECLASS_VALUE);
                }
            }
            rs.close();

            query = "Select Count(*) From keyword_synonym";
            rs = s.executeQuery(query);
            if(rs.next()) {
                count = rs.getInt(1);
                if(count != RECORD_COUNT_KEYWORD_SYNONYM) {
                    logger.warn("Number of records in keyword_synonym table is {}, should be {}", count, RECORD_COUNT_KEYWORD_SYNONYM);
                }
            }
            rs.close();

            query = "Select Count(*) From property";
            rs = s.executeQuery(query);
            if(rs.next()) {
                count = rs.getInt(1);
                if(count != RECORD_COUNT_PROPERTY) {
                    logger.warn("Number of records in property table is {}, should be {}", count, RECORD_COUNT_PROPERTY);
                }
            }
            rs.close();

            query = "Select Count(*) From property_value";
            rs = s.executeQuery(query);
            if(rs.next()) {
                count = rs.getInt(1);
                if(count != RECORD_COUNT_PROPERTY_VALUE) {
                    logger.warn("Number of records in property_value table is {}, should be {}", count, RECORD_COUNT_PROPERTY_VALUE);
                }
            }
            rs.close();

            query = "Select Count(*) From unit";
            rs = s.executeQuery(query);
            if(rs.next()) {
                count = rs.getInt(1);
                if(count != RECORD_COUNT_UNIT) {
                    logger.warn("Number of records in unit table is {}, should be {}", count, RECORD_COUNT_UNIT);
                }
            }
            rs.close();
            s.close();

            logger.info("Finished count check on eClass database");

        } catch (Exception e) {
            logger.error("Failed to check record counts from the eclass database", e);
        } finally {
            closeConnection(c);
        }
    }
}
