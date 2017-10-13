package eu.nimble.service.catalogue.category.taxonomy.eclass.database;

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
    public static String TABLE_NAME_CLASSIFICATION_CLASS_PROPERTY_VALUE = "CLASSIFICATION_CLASS_PROPERTY_VALUE";
    public static String TABLE_NAME_PROPERTY = "PROPERTY";
    public static String TABLE_NAME_PROPERTY_VALUE = "PROPERTY_VALUE";
    public static String TABLE_NAME_KEYWORD_SYNONYM = "KEYWORD_SYNONYM";
    public static String TABLE_NAME_UNIT = "UNIT";
    public static String TABLE_NAME_ECLASS_VALUE = "ECLASS_VALUE";

    // Column name indices
    public static String COLUMN_CLASSIFICATION_CLASS_CODED_NAME = "CodedName";
    public static String COLUMN_CLASSIFICATION_CLASS_PREFERRED_NAME = "PreferredName";
    public static String COLUMN_CLASSIFICATION_CLASS_DEFINITION = "Definition";
    public static String COLUMN_CLASSIFICATION_CLASS_NOTE = "Note";
    public static String COLUMN_CLASSIFICATION_CLASS_REMARK = "Remark";
    public static String COLUMN_CLASSIFICATION_CLASS_LEVEL = "Level";
    public static String COLUMN_CLASSIFICATION_CLASS_MKSUBCLASS = "MKSubclass";
    public static String COLUMN_CLASSIFICATION_CLASS_IRDICC = "IrdiCC";

    public static String COLUMN_CLASSIFICATION_CLASS_PROPERTY_IRDI_CC = "IrdiCC";
    public static String COLUMN_CLASSIFICATION_CLASS_PROPERTY_IRDI_PR = "IrdiPR";

    public static String COLUMN_CLASSIFICATION_CLASS_PROPERTY_VALUE_IRDI_CC = "IrdiCC";
    public static String COLUMN_CLASSIFICATION_CLASS_PROPERTY_VALUE_IRDI_PR = "IrdiPR";
    public static String COLUMN_CLASSIFICATION_CLASS_PROPERTY_VALUE_IRDI_VA = "IrdiVA";

    public static String COLUMN_PROPERTY_IRDI_PR = "IrdiPR";
    public static String COLUMN_PROPERTY_PREFERRED_NAME = "PreferredName";
    public static String COLUMN_PROPERTY_SHORT_NAME = "ShortName";
    public static String COLUMN_PROPERTY_DEFINITION = "Definition";
    public static String COLUMN_PROPERTY_NOTE = "Note";
    public static String COLUMN_PROPERTY_REMARK = "Remark";
    public static String COLUMN_PROPERTY_PREFERRED_SYMBOL = "PreferredSymbol";
    public static String COLUMN_PROPERTY_IRDI_UN = "IrdiUN";
    public static String COLUMN_PROPERTY_CATEGORY = "Category";
    public static String COLUMN_PROPERTY_ATTRIBUTE_TYPE = "AttributeType";
    public static String COLUMN_PROPERTY_DATA_TYPE = "DataType";


    public static String COLUMN_KEYWORD_SYNONYM_KEYWORD_VALUE_SYNONYM_VALUE = "KeywordValueSynonymValue";
    public static String COLUMN_KEYWORD_SYNONYM_EXPLANATION = "Explanation";
    public static String COLUMN_KEYWORD_SYNONYM_IRDI_TARGET = "IrdiTarget";
    public static String COLUMN_KEYWORD_SYNONYM_IRDI_KW_IRDI_SY = "IrdiKWIrdiSY";
    public static String COLUMN_KEYWORD_SYNONYM_TYPE_OF_SE = "TypeOfSE";

    public static String COLUMN_UNIT_STRUCTURED_NAMING = "StructuredNaming";
    public static String COLUMN_UNIT_SHORT_NAME = "ShortName";
    public static String COLUMN_UNIT_DEFINITION = "Definition";
    public static String COLUMN_UNIT_SOURCE = "Source";
    public static String COLUMN_UNIT_COMMENT = "Comment";
    public static String COLUMN_UNIT_SI_NOTATION = "SINotation";
    public static String COLUMN_UNIT_SI_NAME = "SIName";
    public static String COLUMN_UNIT_DIN_NOTATION = "DINNotation";
    public static String COLUMN_UNIT_ECE_NAME = "ECEName";
    public static String COLUMN_UNIT_ECE_CODE = "ECECode";
    public static String COLUMN_UNIT_NIST_NAME = "NISTName";
    public static String COLUMN_UNIT_IEC_CLASSIFICATION = "IECClassification";
    public static String COLUMN_UNIT_NAME_OF_DEDICATED_QUANTITY = "NameOfDedicatedQuantity";
    public static String COLUMN_UNIT_IRDI_UN = "IrdiUN";

    public static String COLUMN_ECLASS_VALUE_IRDI_VA = "IrdiVA";
    public static String COLUMN_ECLASS_VALUE_DATA_TYPE = "DataType";
    public static String COLUMN_ECLASS_VALUE_PREFERRED_NAME = "PreferredName";
    public static String COLUMN_ECLASS_VALUE_SHORT_NAME = "ShortName";
    public static String COLUMN_ECLASS_VALUE_DEFINITION = "Definition";

    public static String eClassQuerySetPostgresDatabaseSchema(String schemaName) {
        return new StringBuilder("SET SEARCH_PATH = '").append(schemaName).append("'").toString();
    }

    public static String eClassQueryGetClassificationClassByName() {
        StringBuilder sb = new StringBuilder();
        sb.append("SELECT * FROM ").append(TABLE_NAME_CLASSIFICATION_CLASS)
                .append(" WHERE ")
                .append(COLUMN_CLASSIFICATION_CLASS_PREFERRED_NAME).append(" ILIKE ?");
        return sb.toString();
    }

    public static String eClassQueryGetClassificationClassByIdList(List<String> idList) {
        StringBuilder sb = new StringBuilder();
        sb.append("SELECT * FROM ").append(TABLE_NAME_CLASSIFICATION_CLASS)
                .append(" WHERE ");

        for (int i = 0; i < idList.size() - 1; i++) {
            sb.append(COLUMN_CLASSIFICATION_CLASS_IRDICC).append(" = ? OR ");
        }
        sb.append(COLUMN_CLASSIFICATION_CLASS_IRDICC).append(" = ?");
        return sb.toString();
    }

    public static String eClassQueryGetClassificationClassByLevel() {
        StringBuilder sb = new StringBuilder();
        sb.append("SELECT * FROM ").append(TABLE_NAME_CLASSIFICATION_CLASS)
                .append(" WHERE ")
                .append(COLUMN_CLASSIFICATION_CLASS_LEVEL).append(" = ?");
        return sb.toString();
    }

    public static String eClassQueryGetSubCategoryIds() {
        StringBuilder sb = new StringBuilder();
        sb.append("SELECT * FROM ").append(TABLE_NAME_CLASSIFICATION_CLASS)
                .append(" WHERE ")
                .append(COLUMN_CLASSIFICATION_CLASS_LEVEL).append(" = ? AND ")
                .append(COLUMN_CLASSIFICATION_CLASS_CODED_NAME).append(" ILIKE ?");
        return sb.toString();
    }

    public static String eClassQueryGetKeywordByValue() {
        StringBuilder sb = new StringBuilder();
        sb.append("SELECT ")
                .append(COLUMN_KEYWORD_SYNONYM_IRDI_TARGET)
                .append(" FROM ").append(TABLE_NAME_KEYWORD_SYNONYM)
                .append(" WHERE ")
                .append(COLUMN_KEYWORD_SYNONYM_TYPE_OF_SE).append(" = 'KW' AND ")
                .append(COLUMN_KEYWORD_SYNONYM_KEYWORD_VALUE_SYNONYM_VALUE).append(" ILIKE ?");
        return sb.toString();
    }

    public static String eClassQueryGetKeywordsForClass() {
        StringBuilder sb = new StringBuilder();
        sb.append("SELECT ")
                .append(COLUMN_KEYWORD_SYNONYM_KEYWORD_VALUE_SYNONYM_VALUE)
                .append(" FROM ").append(TABLE_NAME_KEYWORD_SYNONYM)
                .append(" WHERE ")
                .append(COLUMN_KEYWORD_SYNONYM_TYPE_OF_SE).append(" = 'KW' AND ")
                .append(COLUMN_KEYWORD_SYNONYM_IRDI_TARGET).append(" = ?");
        return sb.toString();
    }

    public static String eClassQueryGetPossiblePropertiesForCategory() {
        StringBuilder sb = new StringBuilder();
        sb.append("SELECT ")
                .append("pr.*, cc_pr.").append(COLUMN_CLASSIFICATION_CLASS_PROPERTY_IRDI_PR)
                .append(" FROM ").append(TABLE_NAME_PROPERTY).append(" pr, ")
                .append(TABLE_NAME_CLASSIFICATION_CLASS_PROPERTY).append(" cc_pr ")
                .append(" WHERE ")
                .append("cc_pr.").append(COLUMN_CLASSIFICATION_CLASS_PROPERTY_IRDI_CC).append("= ? AND ")
                .append("pr.").append(COLUMN_PROPERTY_IRDI_PR).append("=").append("cc_pr.").append(COLUMN_CLASSIFICATION_CLASS_PROPERTY_IRDI_PR);
        return sb.toString();
    }

    public static String eClassQueryGetValuesForProperties() {
        StringBuilder sb = new StringBuilder();
        sb.append("SELECT ")
                .append("cc_pr_va.irdipr, va.* FROM ")
                .append(TABLE_NAME_CLASSIFICATION_CLASS_PROPERTY_VALUE).append(" cc_pr_va, ")
                .append(TABLE_NAME_ECLASS_VALUE).append(" va")
                .append(" WHERE ")
                .append("cc_pr_va.").append(COLUMN_CLASSIFICATION_CLASS_PROPERTY_VALUE_IRDI_CC).append(" = ? AND ")
                .append(" va.").append(COLUMN_ECLASS_VALUE_IRDI_VA).append(" = cc_pr_va.").append(COLUMN_CLASSIFICATION_CLASS_PROPERTY_VALUE_IRDI_VA);
        return sb.toString();
    }

    public static String eClassQueryGetUnitsForProperties() {
        StringBuilder sb = new StringBuilder();
        sb.append("SELECT cc_pr.irdipr, un.* FROM ")
                .append(TABLE_NAME_CLASSIFICATION_CLASS_PROPERTY).append(" cc_pr, ")
                .append(TABLE_NAME_PROPERTY).append(" pr,")
                .append(TABLE_NAME_UNIT).append(" un")
                .append(" WHERE ")
                .append("cc_pr.").append(COLUMN_CLASSIFICATION_CLASS_PROPERTY_IRDI_CC).append(" = ? AND ")
                .append("cc_pr.").append(COLUMN_CLASSIFICATION_CLASS_PROPERTY_IRDI_PR).append(" = pr.").append(COLUMN_PROPERTY_IRDI_PR).append(" AND ")
                .append("pr.").append(COLUMN_PROPERTY_IRDI_UN).append(" = un.").append(COLUMN_UNIT_IRDI_UN);
        return sb.toString();
    }
}
