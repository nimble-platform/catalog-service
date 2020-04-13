package eu.nimble.service.catalogue.template;

import eu.nimble.service.catalogue.model.category.Property;
import eu.nimble.service.catalogue.model.category.Unit;
import eu.nimble.service.catalogue.util.SpringBridge;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by suat on 09-Mar-17.
 */
public class TemplateConfig {

    public static String TEMPLATE_TAB_METADATA = "Publishing Metadata";

    public static String TEMPLATE_PRODUCT_PROPERTIES_WIDTH = "Width";
    public static String TEMPLATE_PRODUCT_PROPERTIES_LENGTH = "Length";

    public static String TEMPLATE_DATA_TYPE_TEXT = "TEXT";
    public static String TEMPLATE_DATA_TYPE_MULTILINGUAL_TEXT = "MULTILINGUAL TEXT";
    public static String TEMPLATE_DATA_TYPE_NUMBER = "NUMBER";
    public static String TEMPLATE_DATA_TYPE_FILE = "FILE";
    public static String TEMPLATE_DATA_TYPE_QUANTITY = "QUANTITY";
    public static String TEMPLATE_DATA_TYPE_REAL_MEASURE = "REAL_MEASURE";
    public static String TEMPLATE_DATA_TYPE_STRING = "STRING";
    public static String TEMPLATE_DATA_TYPE_STRING_TRANSLATABLE = "STRING_TRANSLATABLE";
    public static String TEMPLATE_DATA_TYPE_BOOLEAN = "BOOLEAN";

    public static String TEMPLATE_QUANTITY_VALUE = "QUANTITY VALUE";
    public static String TEMPLATE_QUANTITY_UNIT = "QUANTITY UNIT";

    public static String TEMPLATE_BOOLEAN_REFERENCE = "SourceList!$A$2:$A$3";
    public static String TEMPLATE_INCOTERMS_REFERENCE = "SourceList!$B$2:$B$12";
    public static String TEMPLATE_CURRENCY_REFERENCE = "SourceList!$C$2:$C$4";
    public static String TEMPLATE_DIMENSION_REFERENCE = "SourceList!$D$2:$D$4";
    public static String TEMPLATE_WARRANTY_REFERENCE = "SourceList!$E$2:$E$3";
    public static String TEMPLATE_DELIVERY_PERIOD_REFERENCE = "SourceList!$F$2:$F$4";

    public static List<Property> getFixedPropertiesForProductPropertyTab(String language) {
        List<Property> properties = new ArrayList<>();

        // item manufacturer id
        Property prop = new Property();
        prop.addPreferredName(SpringBridge.getInstance().getMessage(TemplateTextCode.TEMPLATE_PRODUCT_PROPERTIES_MANUFACTURER_ITEM_IDENTIFICATION.toString(), language), language);
        prop.setDataType("STRING");
        properties.add(prop);

        // name
        prop = new Property();
        prop.addPreferredName(SpringBridge.getInstance().getMessage(TemplateTextCode.TEMPLATE_PRODUCT_PROPERTIES_NAME.toString(), language) , language);
        prop.setDataType("STRING");
        properties.add(prop);

        // description
        prop = new Property();
        prop.addPreferredName(SpringBridge.getInstance().getMessage(TemplateTextCode.TEMPLATE_PRODUCT_PROPERTIES_DESCRIPTION.toString(), language) , language);
        prop.setDataType("STRING");
        properties.add(prop);

        // product data sheet
        /*prop = new Property();
        prop.setPreferredName(TEMPLATE_PRODUCT_PROPERTIES_PRODUCT_DATA_SHEET);
        prop.setDataType("FILE");
        properties.add(prop);

        // product safety sheet
        prop = new Property();
        prop.setPreferredName(TEMPLATE_PRODUCT_PROPERTIES_PRODUCT_SAFETY_SHEET);
        prop.setDataType("FILE");
        properties.add(prop);*/

        // width
        prop = new Property();
        prop.addPreferredName(SpringBridge.getInstance().getMessage(TemplateTextCode.TEMPLATE_PRODUCT_PROPERTIES_WIDTH.toString(), language) , language);
        prop.setDataType(TemplateConfig.TEMPLATE_DATA_TYPE_QUANTITY);
        properties.add(prop);

        // length
        prop = new Property();
        prop.addPreferredName(SpringBridge.getInstance().getMessage(TemplateTextCode.TEMPLATE_PRODUCT_PROPERTIES_LENGTH.toString(), language) , language);
        prop.setDataType(TemplateConfig.TEMPLATE_DATA_TYPE_QUANTITY);
        properties.add(prop);

        // height
        prop = new Property();
        prop.addPreferredName(SpringBridge.getInstance().getMessage(TemplateTextCode.TEMPLATE_PRODUCT_PROPERTIES_HEIGHT.toString(), language) , language);
        prop.setDataType(TemplateConfig.TEMPLATE_DATA_TYPE_QUANTITY);
        properties.add(prop);
        return properties;
    }

    public static List<Property> getFixedPropertiesForTermsTab(String language) {
        List<Property> properties = new ArrayList<>();

        // item manufacturer id
        Property prop = new Property();
        prop.addPreferredName(SpringBridge.getInstance().getMessage(TemplateTextCode.TEMPLATE_PRODUCT_PROPERTIES_MANUFACTURER_ITEM_IDENTIFICATION.toString(), language) , language);
        prop.setDataType("STRING");
        properties.add(prop);

        // price amount
        prop = new Property();
        prop.addPreferredName(SpringBridge.getInstance().getMessage(TemplateTextCode.TEMPLATE_TRADING_DELIVERY_PRICE_AMOUNT.toString(), language) , language);
        prop.setDataType("AMOUNT");
        properties.add(prop);
        Unit unit = new Unit();
        prop.setUnit(unit);


        // price base quantity
        prop = new Property();
        prop.addPreferredName(SpringBridge.getInstance().getMessage(TemplateTextCode.TEMPLATE_TRADING_DELIVERY_PRICE_BASE_QUANTITY.toString(), language) , language);
        prop.setDataType("QUANTITY");
        properties.add(prop);

        // minimum order quantity
        prop = new Property();
        prop.addPreferredName(SpringBridge.getInstance().getMessage(TemplateTextCode.TEMPLATE_TRADING_DELIVERY_MINIMUM_ORDER_QUANTITY.toString(), language) , language);
        prop.setDataType("QUANTITY");
        properties.add(prop);

        // free sample
        prop = new Property();
        prop.addPreferredName(SpringBridge.getInstance().getMessage(TemplateTextCode.TEMPLATE_TRADING_DELIVERY_FREE_SAMPLE.toString(), language) , language);
        prop.setDataType("BOOLEAN");
        properties.add(prop);

        // validity period
        prop = new Property();
        prop.addPreferredName(SpringBridge.getInstance().getMessage(TemplateTextCode.TEMPLATE_TRADING_DELIVERY_WARRANTY_VALIDITY_PERIOD.toString(), language) , language);
        prop.setDataType("QUANTITY");
        properties.add(prop);

        // warranty information
        prop = new Property();
        prop.addPreferredName(SpringBridge.getInstance().getMessage(TemplateTextCode.TEMPLATE_TRADING_DELIVERY_WARRANTY_INFORMATION.toString(), language) , language);
        prop.setDataType("TEXT");
        properties.add(prop);

        // incoterms
        prop = new Property();
        prop.addPreferredName(SpringBridge.getInstance().getMessage(TemplateTextCode.TEMPLATE_TRADING_DELIVERY_INCOTERMS.toString(), language) , language);
        prop.setDataType("TEXT");
        properties.add(prop);

        // special terms
        prop = new Property();
        prop.addPreferredName(SpringBridge.getInstance().getMessage(TemplateTextCode.TEMPLATE_TRADING_DELIVERY_SPECIAL_TERMS.toString(), language) , language);
        prop.setDataType("TEXT");
        properties.add(prop);

        // estimated delivery period
        prop = new Property();
        prop.addPreferredName(SpringBridge.getInstance().getMessage(TemplateTextCode.TEMPLATE_TRADING_DELIVERY_ESTIMATED_DELIVERY_PERIOD.toString(), language) , language);
        prop.setDataType(TemplateConfig.TEMPLATE_DATA_TYPE_QUANTITY);
        properties.add(prop);

        // applicable address territory country
        prop = new Property();
        prop.addPreferredName(SpringBridge.getInstance().getMessage(TemplateTextCode.TEMPLATE_TRADING_DELIVERY_APPLICABLE_ADDRESS_COUNTRY.toString(), language) , language);
        prop.setDataType("TEXT");
        properties.add(prop);

        // applicable address territory city
        /*prop = new Property();
        prop.setPreferredName(TEMPLATE_TRADING_DELIVERY_APPLICABLE_ADDRESS_CITY);
        prop.setDataType("TEXT");
        properties.add(prop);*/

        // transport mode
        prop = new Property();
        prop.addPreferredName(SpringBridge.getInstance().getMessage(TemplateTextCode.TEMPLATE_TRADING_DELIVERY_TRANSPORT_MODE.toString(), language) , language);
        prop.setDataType("TEXT");
        properties.add(prop);

        // packaging type
        prop = new Property();
        prop.addPreferredName(SpringBridge.getInstance().getMessage(TemplateTextCode.TEMPLATE_TRADING_DELIVERY_PACKAGING_TYPE.toString(), language) , language);
        prop.setDataType("TEXT");
        properties.add(prop);

        // packaging quantity
        prop = new Property();
        prop.addPreferredName(SpringBridge.getInstance().getMessage(TemplateTextCode.TEMPLATE_TRADING_DELIVERY_PACKAGE_QUANTITY.toString(), language) , language);
        prop.setDataType(TemplateConfig.TEMPLATE_DATA_TYPE_QUANTITY);
        properties.add(prop);

        return properties;
    }
}
