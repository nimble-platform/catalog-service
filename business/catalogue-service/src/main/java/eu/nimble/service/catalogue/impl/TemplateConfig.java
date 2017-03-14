package eu.nimble.service.catalogue.impl;

import eu.nimble.service.catalogue.category.datamodel.Property;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by suat on 09-Mar-17.
 */
public class TemplateConfig {

    public static String TEMPLATE_TAB_INFORMATION = "Information";
    public static String TEMPLATE_TAB_PRODUCT_PROPERTIES = "Product Properties";
    public static String TEMPLATE_TAB_PROPERTY_DETAILS = "Property Details";
    public static String TEMPLATE_TAB_ALLOWED_VALUES_FOR_PROPERTIES = "Allowed Values for Properties";

    public static String TEMPLATE_INFO_HOW_TO_FILL = "How to fill in this template?";
    public static String TEMPLATE_INFO_THIS_TAB_PROVIDES = "This tab provides information about the other tabs of this template";
    public static String TEMPLATE_INFO_TOP_THREE_COLUMNS = "Top 3 columns of the Product Properties tab includes the information about the properties to be filled in for each product variation.";
    public static String TEMPLATE_INFO_THE_FIRST_ROW = "The first row is the name of the property. and the second row shows the unit associated with the property value if there is any.";
    public static String TEMPLATE_INFO_DETAILS_OF_THE_PROPERTY = "Details of the property and allowed values can be investigated in " + TEMPLATE_TAB_PROPERTY_DETAILS + " and " + TEMPLATE_TAB_ALLOWED_VALUES_FOR_PROPERTIES + " tabs respectively based on the name of the property ";
    public static String TEMPLATE_INFO_THE_SECOND_ROW = "The second row shows the data type of the property.";
    public static String TEMPLATE_INFO_THE_THIRD_ROW = "The third row shows the unit associated with the property value if there is any.";
    public static String TEMPLATE_INFO_THE_FOURTH_ROW = "The 4th row onwards, each row corresponds to a product variation for the chosen product category";
    public static String TEMPLATE_INFO_THIS_TAB_CONTAINS = "This tab contains additional information for each property associated with the chosen product category";
    public static String TEMPLATE_INFO_THIS_TAB_CONTAINS_VALUES = "This tab contains values that are allowed to be set for each property starting from the 4th row";

    public static String TEMPLATE_PRODUCT_PROPERTIES_PROPERTY_NAME = "Property Name";
    public static String TEMPLATE_PRODUCT_PROPERTIES_PROPERTY_DATA_TYPE = "Property Data Type";
    public static String TEMPLATE_PRODUCT_PROPERTIES_PROPERTY_UNIT = "Property Unit";

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

    public static String TEMPLATE_FIXED_PROPERTY_NAME = "Name";
    public static String TEMPLATE_FIXED_PROPERTY_DESCRIPTION = "Description";

    public static List<Property> getFixedProperties() {
        List<Property> ublSpecificProperties = new ArrayList<>();
        Property prop = new Property();
        prop.setPreferredName(TEMPLATE_FIXED_PROPERTY_NAME);
        prop.setDataType("STRING");
        ublSpecificProperties.add(prop);

        prop = new Property();
        prop.setPreferredName(TEMPLATE_FIXED_PROPERTY_DESCRIPTION);
        prop.setDataType("STRING");
        ublSpecificProperties.add(prop);
        return ublSpecificProperties;
    }
}
