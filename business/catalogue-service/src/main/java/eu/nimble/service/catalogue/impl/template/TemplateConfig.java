package eu.nimble.service.catalogue.impl.template;

import eu.nimble.service.catalogue.category.datamodel.Property;
import eu.nimble.service.catalogue.category.datamodel.Unit;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.VerticalAlignment;
import org.apache.poi.ss.usermodel.Workbook;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by suat on 09-Mar-17.
 */
public class TemplateConfig {

    public static String TEMPLATE_TAB_INFORMATION = "Information";
    public static String TEMPLATE_TAB_TRADING_DELIVERY_TERMS = "Trading & Delivery Terms";
    public static String TEMPLATE_TAB_PRODUCT_PROPERTIES = "Product Properties";
    public static String TEMPLATE_TAB_PROPERTY_DETAILS = "Property Details";
    public static String TEMPLATE_TAB_ALLOWED_VALUES_FOR_PROPERTIES = "Allowed Values for Properties";
    public static String TEMPLATE_TAB_METADATA = "Publishing Metadata";

    public static String TEMPLATE_INFO_HOW_TO_FILL = "How to fill in this template?";
    public static String TEMPLATE_INFO_THIS_TAB_PROVIDES = "Below, we provide information about how to fill this template in by giving details about the other tabs of this template";
    public static String TEMPLATE_INFO_GENERIC_INFORMATION = "Generic Information";
    public static String TEMPLATE_INFO_YOU_SHOULD_EDIT = "*** You should edit only the \"Product Properties\" and \"Trading & Delivery Terms\" tabs. Do not edit the other tabs.";
    public static String TEMPLATE_INFO_MANDATORY_INFORMATION = "*** Mandatory information to be provided are colored with \"Red\" as this line";
    public static String TEMPLATE_INFO_DATA_FIELDS = "*** Data fields can be provided with multiple values by separating the values with the pipe (|) character.";
    public static String TEMPLATE_INFO_IN_THIS_WAY = "        In this way, product variations can be specified by providing alternative values for product properties. See the example below:";
    public static String TEMPLATE_INFO_EX1 = "                Ex: 250|500|1000";
    public static String TEMPLATE_INFO_EX2 = "                Ex: blue|yellow";
    public static String TEMPLATE_INFO_EX3 = "                Ex: C:\\Documents\\image1.png|C:\\Documents\\image2.png|C:\\Documents\\image3.png";
    public static String TEMPLATE_INFO_THE_TOP_FOUR_COLUMNS = "The top 4 rows of the Product Properties tab present  the information about the properties to be filled in for each product variation.";
    public static String TEMPLATE_INFO_THE_FIRST_ROW = "The 1st, i.e. the topmost, row shows categories for which this template is generated. Each product published through this template will be annotated with these categories.";
    public static String TEMPLATE_INFO_THE_SECOND_ROW = "The 2nd row contains three groups of properties as described below:";
    public static String TEMPLATE_INFO_FIXED_PROPERTIES = "        1) Fixed properties regardless from the selected categories such as product name, description, images, etc.";
    public static String TEMPLATE_INFO_PROPERTIES_ASSOCIATED = "        2) Properties associated to the selected categories. For each category, relevant properties are listed under the merged cell including the category name";
    public static String TEMPLATE_INFO_CUSTOM_PROPERTIES = "        3) Custom properties that can be created based for your own need. They should be specified right after the last property belonging to a category.";
    public static String TEMPLATE_INFO_DETAILS_OF_THE_PROPERTY = "Details of the property and allowed values can be investigated in " + TEMPLATE_TAB_PROPERTY_DETAILS + " and " + TEMPLATE_TAB_ALLOWED_VALUES_FOR_PROPERTIES + " tabs respectively based on the name of the property ";
    public static String TEMPLATE_INFO_THE_THIRD_ROW = "The 3rd row shows the data type of the property. Following properties are applicable:";
    public static String TEMPLATE_INFO_TEXT = "        TEXT : The data that should be set for properties representing textual values";
    public static String TEMPLATE_INFO_NUMBER = "        NUMBER : The data type that should be specified for properties representing numeric values";
    public static String TEMPLATE_INFO_QUANTITY = "        QUANTITY : If a numeric property must have an associated unit (e.g. a dimension property), such a property must have QUANTITY type.";
    public static String TEMPLATE_INFO_FILE = "        FILE : When a file (image, pdf, etc.) is to be specified for a property (e.g. images, safety sheet, etc.), this data type is used. You should specify the location of the file on your system e.g. C:\\Documents\\safetysheet.pdf.";
    public static String TEMPLATE_INFO_BOOLEAN = "        BOOLEAN : Properties indicating a yes/no or true/false values must be annotated with this data type. Available values are: yes, no, true, false.";
    public static String TEMPLATE_INFO_THE_FOURTH_ROW = "The 4th row shows the unit associated with the property value if there is any.";
    public static String TEMPLATE_INFO_THE_FIFTH_ROW = "The 5th row onwards, each row corresponds to a product (with potential variations based on the provided values) for the chosen product category";
    public static String TEMPLATE_INFO_TRADING_AND_DELIVERY = "Trading and delivery terms related information should be specified via this tab. Structure of this tab is the same as \"Product Properties\" tab.";
    public static String TEMPLATE_INFO_MANUFACTURER_ITEM_IDENTIFICATION = "*** Manufacturer item identification values must be consistent with the ones used in the \"Product Properties\" tab. In other words, same identifier must be used in the rows representing the same product.";
    public static String TEMPLATE_INFO_THIS_TAB_CONTAINS = "This tab contains additional information for each property associated with the chosen product category";
    public static String TEMPLATE_INFO_THIS_TAB_CONTAINS_VALUES = "This tab contains values that are allowed to be set for each property starting from the 4th row";
    public static String TEMPLATE_INFO_THIS_TAB_CONTAINS_INFORMATION = "This tab contains information about the publishing process such as selected category metadata, publishing party and so on.";

    public static String TEMPLATE_PRODUCT_PROPERTIES_PROPERTY_NAME = "Property Name";
    public static String TEMPLATE_PRODUCT_PROPERTIES_PROPERTY_DATA_TYPE = "Property Data Type";
    public static String TEMPLATE_PRODUCT_PROPERTIES_PROPERTY_UNIT = "Property Unit";
    public static String TEMPLATE_PRODUCT_PROPERTIES_MANUFACTURER_ITEM_IDENTIFICATION = "Manufacturer Item Identification";
    public static String TEMPLATE_PRODUCT_PROPERTIES_NAME = "Name";
    public static String TEMPLATE_PRODUCT_PROPERTIES_DESCRIPTION = "Description";
    public static String TEMPLATE_PRODUCT_PROPERTIES_DIMENSIONS = "Dimensions";
    public static String TEMPLATE_PRODUCT_PROPERTIES_WIDTH = "Width";
    public static String TEMPLATE_PRODUCT_PROPERTIES_LENGTH = "Length";
    public static String TEMPLATE_PRODUCT_PROPERTIES_HEIGHT = "Height";
    public static String TEMPLATE_PRODUCT_PROPERTIES_IMAGES = "Images";
    public static String TEMPLATE_PRODUCT_PROPERTIES_CERTIFICATIONS = "Certifications";
    public static String TEMPLATE_PRODUCT_PROPERTIES_PRODUCT_DATA_SHEET = "Product Data Sheet";
    public static String TEMPLATE_PRODUCT_PROPERTIES_PRODUCT_SAFETY_SHEET = "Product Safety Sheet";

    public static String TEMPLATE_TRADING_DELIVERY_TRADING_DETAILS = "Trading Details";
    public static String TEMPLATE_TRADING_DELIVERY_PRICE_AMOUNT = "Price Amount";
    public static String TEMPLATE_TRADING_DELIVERY_PRICE_BASE_QUANTITY = "Price Base Quantity";
    public static String TEMPLATE_TRADING_DELIVERY_MINIMUM_ORDER_QUANTITY = "Minimum Order Quantity";
    public static String TEMPLATE_TRADING_DELIVERY_FREE_SAMPLE = "Free Sample";
    public static String TEMPLATE_TRADING_DELIVERY_WARRANTY = "Warranty";
    public static String TEMPLATE_TRADING_DELIVERY_WARRANTY_VALIDITY_PERIOD = "Warranty Validity Period";
    public static String TEMPLATE_TRADING_DELIVERY_WARRANTY_INFORMATION = "Warranty Information";
    public static String TEMPLATE_TRADING_DELIVERY_DELIVERY_TERMS = "Delivery Terms";
    public static String TEMPLATE_TRADING_DELIVERY_INCOTERMS = "Incoterms";
    public static String TEMPLATE_TRADING_DELIVERY_SPECIAL_TERMS = "Special Terms";
    public static String TEMPLATE_TRADING_DELIVERY_ESTIMATED_DELIVERY_PERIOD = "Estimated Delivery Period";
    public static String TEMPLATE_TRADING_DELIVERY_TRANSPORT_MODE = "Transport Mode";
    public static String TEMPLATE_TRADING_DELIVERY_PACKAGING = "Packaging";
    public static String TEMPLATE_TRADING_DELIVERY_PACKAGING_TYPE = "Packaging Type";
    public static String TEMPLATE_TRADING_DELIVERY_PACKAGE_QUANTITY = "Package Quantity";

    public static String TEMPLATE_PROPERTY_DETAILS_PROPERTY_NAME = "Property Name";
    public static String TEMPLATE_PROPERTY_DETAILS_SHORT_NAME = "Short Name";
    public static String TEMPLATE_PROPERTY_DETAILS_DEFINITION = "Definition";
    public static String TEMPLATE_PROPERTY_DETAILS_NOTE = "Note";
    public static String TEMPLATE_PROPERTY_DETAILS_REMARK = "Remark";
    public static String TEMPLATE_PROPERTY_DETAILS_PREFERRED_SYMBOL = "Preferred Symbol";
    public static String TEMPLATE_PROPERTY_DETAILS_UNIT = "Unit";
    public static String TEMPLATE_PROPERTY_DETAILS_IEC_CATEGORY = "IEC Category";
    public static String TEMPLATE_PROPERTY_DETAILS_ATTRIBUTE_TYPE = "Attribute Type";
    public static String TEMPLATE_PROPERTY_DETAILS_DATA_TYPE = "Data Type";

    public static String TEMPLATE_DATA_TYPE_TEXT = "TEXT";
    public static String TEMPLATE_DATA_TYPE_NUMBER = "NUMBER";
    public static String TEMPLATE_DATA_TYPE_FILE = "FILE";
    public static String TEMPLATE_DATA_TYPE_QUANTITY = "QUANTITY";
    public static String TEMPLATE_DATA_TYPE_BINARY = "BINARY";
    public static String TEMPLATE_DATA_TYPE_INT = "INT";
    public static String TEMPLATE_DATA_TYPE_FLOAT = "FLOAT";
    public static String TEMPLATE_DATA_TYPE_DOUBLE = "DOUBLE";
    public static String TEMPLATE_DATA_TYPE_REAL_MEASURE = "REAL_MEASURE";
    public static String TEMPLATE_DATA_TYPE_STRING = "STRING";
    public static String TEMPLATE_DATA_TYPE_STRING_TRANSLATABLE = "STRING_TRANSLATABLE";
    public static String TEMPLATE_DATA_TYPE_BOOLEAN = "BOOLEAN";

    public static List<Property> getFixedPropertiesForProductPropertyTab() {
        List<Property> properties = new ArrayList<>();

        // item manufacturer id
        Property prop = new Property();
        prop.setPreferredName(TEMPLATE_PRODUCT_PROPERTIES_MANUFACTURER_ITEM_IDENTIFICATION);
        prop.setDataType("STRING");
        properties.add(prop);

        // name
        prop = new Property();
        prop.setPreferredName(TEMPLATE_PRODUCT_PROPERTIES_NAME);
        prop.setDataType("STRING");
        properties.add(prop);

        // description
        prop = new Property();
        prop.setPreferredName(TEMPLATE_PRODUCT_PROPERTIES_DESCRIPTION);
        prop.setDataType("STRING");
        properties.add(prop);

        // images
        prop = new Property();
        prop.setPreferredName(TEMPLATE_PRODUCT_PROPERTIES_IMAGES);
        prop.setDataType("FILE");
        properties.add(prop);

        // certifications
        prop = new Property();
        prop.setPreferredName(TEMPLATE_PRODUCT_PROPERTIES_CERTIFICATIONS);
        prop.setDataType("STRING");
        properties.add(prop);

        // product data sheet
        prop = new Property();
        prop.setPreferredName(TEMPLATE_PRODUCT_PROPERTIES_PRODUCT_DATA_SHEET);
        prop.setDataType("FILE");
        properties.add(prop);

        // product safety sheet
        prop = new Property();
        prop.setPreferredName(TEMPLATE_PRODUCT_PROPERTIES_PRODUCT_SAFETY_SHEET);
        prop.setDataType("FILE");
        properties.add(prop);

        // width
        prop = new Property();
        prop.setPreferredName(TEMPLATE_PRODUCT_PROPERTIES_WIDTH);
        prop.setDataType("QUANTITY");
        properties.add(prop);

        // length
        prop = new Property();
        prop.setPreferredName(TEMPLATE_PRODUCT_PROPERTIES_LENGTH);
        prop.setDataType("QUANTITY");
        properties.add(prop);

        // height
        prop = new Property();
        prop.setPreferredName(TEMPLATE_PRODUCT_PROPERTIES_HEIGHT);
        prop.setDataType("QUANTITY");
        properties.add(prop);
        return properties;
    }

    public static List<Property> getFixedPropertiesForTermsTab() {
        List<Property> properties = new ArrayList<>();

        // item manufacturer id
        Property prop = new Property();
        prop.setPreferredName(TEMPLATE_PRODUCT_PROPERTIES_MANUFACTURER_ITEM_IDENTIFICATION);
        prop.setDataType("STRING");
        properties.add(prop);

        // price amount
        prop = new Property();
        prop.setPreferredName(TEMPLATE_TRADING_DELIVERY_PRICE_AMOUNT);
        prop.setDataType("AMOUNT");
        properties.add(prop);
        Unit unit = new Unit();
        unit.setShortName("EUR");
        prop.setUnit(unit);


        // price base quantity
        prop = new Property();
        prop.setPreferredName(TEMPLATE_TRADING_DELIVERY_PRICE_BASE_QUANTITY);
        prop.setDataType("QUANTITY");
        properties.add(prop);

        // minimum order quantity
        prop = new Property();
        prop.setPreferredName(TEMPLATE_TRADING_DELIVERY_MINIMUM_ORDER_QUANTITY);
        prop.setDataType("QUANTITY");
        properties.add(prop);

        // free sample
        prop = new Property();
        prop.setPreferredName(TEMPLATE_TRADING_DELIVERY_FREE_SAMPLE);
        prop.setDataType("BOOLEAN");
        properties.add(prop);

        // validity period
        prop = new Property();
        prop.setPreferredName(TEMPLATE_TRADING_DELIVERY_WARRANTY_VALIDITY_PERIOD);
        prop.setDataType("QUANTITY");
        properties.add(prop);

        // warranty information
        prop = new Property();
        prop.setPreferredName(TEMPLATE_TRADING_DELIVERY_WARRANTY_INFORMATION);
        prop.setDataType("TEXT");
        properties.add(prop);

        // incoterms
        prop = new Property();
        prop.setPreferredName(TEMPLATE_TRADING_DELIVERY_INCOTERMS);
        prop.setDataType("TEXT");
        properties.add(prop);

        // special terms
        prop = new Property();
        prop.setPreferredName(TEMPLATE_TRADING_DELIVERY_SPECIAL_TERMS);
        prop.setDataType("TEXT");
        properties.add(prop);

        // estimated delivery period
        prop = new Property();
        prop.setPreferredName(TEMPLATE_TRADING_DELIVERY_ESTIMATED_DELIVERY_PERIOD);
        prop.setDataType("QUANTITY");
        properties.add(prop);

        // transport mode
        prop = new Property();
        prop.setPreferredName(TEMPLATE_TRADING_DELIVERY_TRANSPORT_MODE);
        prop.setDataType("TEXT");
        properties.add(prop);

        // packaging type
        prop = new Property();
        prop.setPreferredName(TEMPLATE_TRADING_DELIVERY_PACKAGING_TYPE);
        prop.setDataType("TEXT");
        properties.add(prop);

        // packaging quantity
        prop = new Property();
        prop.setPreferredName(TEMPLATE_TRADING_DELIVERY_PACKAGE_QUANTITY);
        prop.setDataType("QUANTITY");
        properties.add(prop);

        return properties;
    }
}
