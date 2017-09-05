package eu.nimble.service.catalogue.impl;

import eu.nimble.service.catalogue.category.datamodel.Category;
import eu.nimble.service.catalogue.category.datamodel.Property;
import eu.nimble.service.catalogue.category.datamodel.Value;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.util.List;

import static eu.nimble.service.catalogue.impl.TemplateConfig.*;
import static eu.nimble.service.catalogue.impl.TemplateConfig.TEMPLATE_PROPERTY_DETAILS_DATA_TYPE;

/**
 * Created by suat on 04-Sep-17.
 */
public class TemplateParser {
    public Workbook generateTemplateForCategory(Category category) {
        Workbook template = new XSSFWorkbook();
        Sheet infoTab = template.createSheet(TEMPLATE_TAB_INFORMATION);
        Sheet propertiesTab = template.createSheet(TEMPLATE_TAB_PRODUCT_PROPERTIES);
        Sheet propertyDetailsTab = template.createSheet(TEMPLATE_TAB_PROPERTY_DETAILS);
        Sheet valuesTab = template.createSheet(TEMPLATE_TAB_ALLOWED_VALUES_FOR_PROPERTIES);

        populateInfoTab(infoTab);
        populateProductPropertiesTab(category, propertiesTab);
        populatePropertyDetailsTab(category, propertyDetailsTab);
        populateAllowedValuesTab(category, valuesTab);

        return template;
    }

    private void populateInfoTab(Sheet infoTab) {
        int rowIndex = 0;
        Row row = infoTab.createRow(rowIndex);
        row.createCell(0).setCellValue(TEMPLATE_INFO_HOW_TO_FILL);
        row = infoTab.createRow(++rowIndex);
        row.createCell(0).setCellValue(TEMPLATE_INFO_THIS_TAB_PROVIDES);
        rowIndex++;
        row = infoTab.createRow(++rowIndex);
        row.createCell(0).setCellValue(TEMPLATE_TAB_PRODUCT_PROPERTIES);
        row = infoTab.createRow(++rowIndex);
        row.createCell(0).setCellValue(TEMPLATE_INFO_TOP_THREE_COLUMNS);
        row = infoTab.createRow(++rowIndex);
        row.createCell(0).setCellValue(TEMPLATE_INFO_THE_FIRST_ROW);
        row = infoTab.createRow(++rowIndex);
        row.createCell(0).setCellValue(TEMPLATE_INFO_THE_SECOND_ROW);
        row = infoTab.createRow(++rowIndex);
        row.createCell(0).setCellValue(TEMPLATE_INFO_THE_THIRD_ROW);
        row = infoTab.createRow(++rowIndex);
        row.createCell(0).setCellValue(TEMPLATE_INFO_DETAILS_OF_THE_PROPERTY);
        row = infoTab.createRow(++rowIndex);
        row.createCell(0).setCellValue(TEMPLATE_INFO_THE_FOURTH_ROW);
        rowIndex++;
        row = infoTab.createRow(++rowIndex);
        row.createCell(0).setCellValue(TEMPLATE_TAB_PROPERTY_DETAILS);
        row = infoTab.createRow(++rowIndex);
        row.createCell(0).setCellValue(TEMPLATE_INFO_THIS_TAB_CONTAINS);
        rowIndex++;
        row = infoTab.createRow(++rowIndex);
        row.createCell(0).setCellValue(TEMPLATE_TAB_ALLOWED_VALUES_FOR_PROPERTIES);
        row = infoTab.createRow(++rowIndex);
        row.createCell(0).setCellValue(TEMPLATE_INFO_THIS_TAB_CONTAINS_VALUES);
    }

    private void populateProductPropertiesTab(Category category, Sheet productPropertiesTab) {
        int rowIndex = 0;
        Row firstRow = productPropertiesTab.createRow(rowIndex);
        firstRow.createCell(0).setCellValue(TEMPLATE_PRODUCT_PROPERTIES_PROPERTY_NAME);
        Row secondRow = productPropertiesTab.createRow(++rowIndex);
        secondRow.createCell(0).setCellValue(TEMPLATE_PRODUCT_PROPERTIES_PROPERTY_DATA_TYPE);
        Row thirdRow = productPropertiesTab.createRow(++rowIndex);
        thirdRow.createCell(0).setCellValue(TEMPLATE_PRODUCT_PROPERTIES_PROPERTY_UNIT);

        List<Property> properties = TemplateConfig.getFixedProperties();
        properties.addAll(category.getProperties());
        for (int i = 0; i < properties.size(); i++) {
            Property property = properties.get(i);
            firstRow.createCell(i + 1).setCellValue(property.getPreferredName());
            secondRow.createCell(i + 1).setCellValue(property.getDataType());
            thirdRow.createCell(i + 1).setCellValue(property.getUnit() != null ? property.getUnit().getShortName() : "");
        }
    }

    private void populatePropertyDetailsTab(Category category, Sheet propertyDetailsTab) {
        int rowIndex = 0;
        int columnIndex = 0;
        Row row = propertyDetailsTab.createRow(rowIndex);
        row.createCell(columnIndex).setCellValue(TEMPLATE_PROPERTY_DETAILS_PROPERTY_NAME);
        row.createCell(++columnIndex).setCellValue(TEMPLATE_PROPERTY_DETAILS_SHORT_NAME);
        row.createCell(++columnIndex).setCellValue(TEMPLATE_PROPERTY_DETAILS_DEFINITION);
        row.createCell(++columnIndex).setCellValue(TEMPLATE_PROPERTY_DETAILS_NOTE);
        row.createCell(++columnIndex).setCellValue(TEMPLATE_PROPERTY_DETAILS_REMARK);
        row.createCell(++columnIndex).setCellValue(TEMPLATE_PROPERTY_DETAILS_PREFERRED_SYMBOL);
        row.createCell(++columnIndex).setCellValue(TEMPLATE_PROPERTY_DETAILS_UNIT);
        row.createCell(++columnIndex).setCellValue(TEMPLATE_PROPERTY_DETAILS_IEC_CATEGORY);
        row.createCell(++columnIndex).setCellValue(TEMPLATE_PROPERTY_DETAILS_ATTRIBUTE_TYPE);
        row.createCell(++columnIndex).setCellValue(TEMPLATE_PROPERTY_DETAILS_DATA_TYPE);

        List<Property> properties = category.getProperties();
        for (int i = 0; i < properties.size(); i++) {
            Property property = properties.get(i);
            row = (propertyDetailsTab.createRow(++rowIndex));

            columnIndex = 0;
            row.createCell(columnIndex).setCellValue(property.getPreferredName());
            row.createCell(++columnIndex).setCellValue(property.getShortName());
            row.createCell(++columnIndex).setCellValue(property.getDefinition());
            row.createCell(++columnIndex).setCellValue(property.getNote());
            row.createCell(++columnIndex).setCellValue(property.getRemark());
            row.createCell(++columnIndex).setCellValue(property.getPreferredSymbol());
            row.createCell(++columnIndex).setCellValue(property.getUnit() != null ? property.getUnit().getShortName() : "");
            row.createCell(++columnIndex).setCellValue(property.getIecCategory());
            row.createCell(++columnIndex).setCellValue(property.getAttributeType());
            row.createCell(++columnIndex).setCellValue(property.getDataType());
        }
    }

    private void populateAllowedValuesTab(Category category, Sheet valuesTab) {
        int rowIndex = 0;
        int columnIndex = 0;

        List<Property> properties = category.getProperties();
        for (int i = 0; i < properties.size(); i++) {
            Property property = properties.get(i);
            List<Value> values = property.getValues();
            if (values.size() > 0) {
                Row row = getRow(valuesTab, rowIndex++);
                row.createCell(columnIndex).setCellValue(property.getPreferredName());

                for (int j = 0; j < values.size(); j++) {
                    row = getRow(valuesTab, rowIndex++);
                    row.createCell(columnIndex).setCellValue(values.get(j).getPreferredName());
                }

                rowIndex = 0;
                columnIndex++;
            }
        }
    }

    private Row getRow(Sheet sheet, int rowNum) {
        Row row = sheet.getRow(rowNum);
        if (row == null) {
            row = sheet.createRow(rowNum);
        }
        return row;
    }
}
