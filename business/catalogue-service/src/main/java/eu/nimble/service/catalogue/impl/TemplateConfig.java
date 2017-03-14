package eu.nimble.service.catalogue.impl;

import eu.nimble.service.catalogue.category.datamodel.Property;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by suat on 09-Mar-17.
 */
public class TemplateConfig {
    public static String SHEET_NAME_PRODUCT_PROPERTIES = "Product Properties";

    public static String PROPERTY_NAME_NAME = "Name";
    public static String PROPERTY_NAME_DESCRIPTION = "Description";

    public static List<Property> getFixedProperties() {
        List<Property> ublSpecificProperties = new ArrayList<>();
        Property prop = new Property();
        prop.setPreferredName(PROPERTY_NAME_NAME);
        prop.setDataType("STRING");
        ublSpecificProperties.add(prop);

        prop = new Property();
        prop.setPreferredName(PROPERTY_NAME_DESCRIPTION);
        prop.setDataType("STRING");
        ublSpecificProperties.add(prop);
        return ublSpecificProperties;
    }
}
