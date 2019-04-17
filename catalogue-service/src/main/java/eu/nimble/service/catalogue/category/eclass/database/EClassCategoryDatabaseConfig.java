package eu.nimble.service.catalogue.category.eclass.database;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.util.List;

/**
 * Created by suat on 03-Mar-17.
 */
public class EClassCategoryDatabaseConfig {
    public static void main(String[] a)
            throws Exception {
        Class.forName("org.h2.Driver");
        Connection conn = DriverManager.
                getConnection("jdbc:h2:~/nimble/eclass", "sa", "");
        // add application code here

        ResultSet meta = conn.getMetaData().getTables(null, null, "TABLE%", new String[]{"TABLE"});
        System.out.println(meta.next());

        conn.close();
    }
    // Connection configurations
    public static String PRODUCT_CATEGORY_DATA_PATH = "D:/srdc/projects/NIMBLE/project_starts/WP2/T2.2/eClass/eClass10_0/173100000enUSbasicCSV01";

    // Local Postgres DB configurations
    /*public static String PRODUCT_CATEGORY_POSTGRESQL_CONFIG_DRIVER = "org.postgresql.Driver";
    public static String PRODUCT_CATEGORY_POSTGRESQL_CONFIG_URL = "jdbc:postgresql://localhost:5432/postgres";
    public static String PRODUCT_CATEGORY_POSTGRESQL_CONFIG_USER = "postgres";
    public static String PRODUCT_CATEGORY_POSTGRESQL_CONFIG_PASSWORD = "nimble";
    public static String PRODUCT_CATEGORY_POSTGRESQL_CONFIG_SCHEMA = "eClass";*/

    // Bluemix Postgres DB service configurations
    public static String PRODUCT_CATEGORY_POSTGRESQL_CONFIG_DRIVER = "org.postgresql.Driver";
    public static String PRODUCT_CATEGORY_POSTGRESQL_CONFIG_URL = "jdbc:postgresql://qdjjtnkv.db.elephantsql.com:5432/mnocjzwc";
    public static String PRODUCT_CATEGORY_POSTGRESQL_CONFIG_USER = "mnocjzwc";
    public static String PRODUCT_CATEGORY_POSTGRESQL_CONFIG_PASSWORD = "bkryPYM3rj651059qZ73Q_oOvbgg3MDV";
    public static String PRODUCT_CATEGORY_POSTGRESQL_CONFIG_SCHEMA = "public";

    public static String PRODUCT_CATEGORY_H2_CONFIG_DRIVER = "org.h2.Driver";
    //public static String PRODUCT_CATEGORY_H2_CONFIG_URL = "jdbc:h2:~/eClass;INIT=create schema if not exists test\\;runscript from '~/sql/init.sql'\"";
    public static String PRODUCT_CATEGORY_H2_CONFIG_URL = "jdbc:h2:~/nimble/eClass;IGNORECASE=TRUE";
    public static String PRODUCT_CATEGORY_H2_CONFIG_USER = "sa";
    public static String PRODUCT_CATEGORY_H2_CONFIG_PASSWORD = "";

    // Table names
    public static String TABLE_NAME_CLASSIFICATION_CLASS = "CLASSIFICATION_CLASS";
    public static String TABLE_NAME_CLASSIFICATION_CLASS_PROPERTY = "CLASSIFICATION_CLASS_PROPERTY";
    public static String TABLE_NAME_PROPERTY = "PROPERTY";

    // Column name indices
    public static String COLUMN_CLASSIFICATION_CLASS_CODED_NAME = "CodedName";
    public static String COLUMN_CLASSIFICATION_CLASS_PREFERRED_NAME = "PreferredName";
    public static String COLUMN_CLASSIFICATION_CLASS_DEFINITION = "Definition";
    public static String COLUMN_CLASSIFICATION_CLASS_NOTE = "Note";
    public static String COLUMN_CLASSIFICATION_CLASS_REMARK = "Remark";
    public static String COLUMN_CLASSIFICATION_CLASS_LEVEL = "Level";
    public static String COLUMN_CLASSIFICATION_CLASS_IRDICC = "IrdiCC";

    public static String COLUMN_CLASSIFICATION_CLASS_PROPERTY_IRDI_CC = "IrdiCC";
    public static String COLUMN_CLASSIFICATION_CLASS_PROPERTY_IRDI_PR = "IrdiPR";

    public static String COLUMN_PROPERTY_IRDI_PR = "IrdiPR";
    public static String COLUMN_PROPERTY_PREFERRED_NAME = "PreferredName";
    public static String COLUMN_PROPERTY_SHORT_NAME = "ShortName";
    public static String COLUMN_PROPERTY_DEFINITION = "Definition";
    public static String COLUMN_PROPERTY_NOTE = "Note";
    public static String COLUMN_PROPERTY_REMARK = "Remark";
    public static String COLUMN_PROPERTY_PREFERRED_SYMBOL = "PreferredSymbol";
    public static String COLUMN_PROPERTY_CATEGORY = "Category";
    public static String COLUMN_PROPERTY_ATTRIBUTE_TYPE = "AttributeType";
    public static String COLUMN_PROPERTY_DATA_TYPE = "DataType";

    public static String eClassQuerySetPostgresDatabaseSchema(String schemaName) {
        return new StringBuilder("SET SEARCH_PATH = '").append(schemaName).append("'").toString();
    }

    public static String eClassQueryGetRootCategories(){
        StringBuilder sb = new StringBuilder();
        sb.append("SELECT * FROM ").append(TABLE_NAME_CLASSIFICATION_CLASS)
                .append(" WHERE ")
                .append(COLUMN_CLASSIFICATION_CLASS_LEVEL).append(" = '1'");
        return sb.toString();
    }

    public static String eClassQueryGetAllCategories(){
        StringBuilder sb = new StringBuilder();
        sb.append("SELECT * FROM ").append(TABLE_NAME_CLASSIFICATION_CLASS);
        return sb.toString();
    }

    public static String eClassQueryGetAllProperties() {
        StringBuilder sb = new StringBuilder();
        sb.append("SELECT ")
                .append("pr.*, cc_pr.").append(COLUMN_CLASSIFICATION_CLASS_PROPERTY_IRDI_CC)
                .append(" FROM ").append(TABLE_NAME_PROPERTY).append(" pr, ").append(TABLE_NAME_CLASSIFICATION_CLASS_PROPERTY).append(" cc_pr ")
                .append(" WHERE ")
                .append("pr.").append(COLUMN_PROPERTY_IRDI_PR).append("=").append("cc_pr.").append(COLUMN_CLASSIFICATION_CLASS_PROPERTY_IRDI_PR);
        return sb.toString();
    }
}
