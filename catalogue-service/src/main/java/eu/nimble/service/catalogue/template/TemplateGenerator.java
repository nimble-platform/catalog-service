package eu.nimble.service.catalogue.template;

import eu.nimble.service.catalogue.model.category.Category;
import eu.nimble.service.catalogue.model.category.Property;
import eu.nimble.service.catalogue.model.category.Value;
import eu.nimble.service.catalogue.exception.TemplateParseException;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.ss.util.CellRangeAddressList;
import org.apache.poi.xssf.usermodel.XSSFDataValidationHelper;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static eu.nimble.service.catalogue.template.TemplateConfig.*;
import static eu.nimble.service.catalogue.template.TemplateConfig.TEMPLATE_DATA_TYPE_BOOLEAN;
import static eu.nimble.service.catalogue.template.TemplateConfig.TEMPLATE_DATA_TYPE_STRING;

/**
 * Created by suat on 12-Sep-17.
 */
public class TemplateGenerator {
    private Workbook template;
    private CellStyle headerCellStyle;
    private CellStyle mandatoryCellStyle;
    private CellStyle boldCellStyle;
    private CellStyle wordWrapStyle;
    private String defaultLanguage = "en";

    public TemplateGenerator() {
        template = new XSSFWorkbook();
        createStyles();
    }

    public Workbook generateTemplateForCategory(List<Category> categories) {
        Sheet infoTab = template.createSheet(TemplateConfig.TEMPLATE_TAB_INFORMATION);
        Sheet propertiesTab = template.createSheet(TemplateConfig.TEMPLATE_TAB_PRODUCT_PROPERTIES);
        Sheet tradingDeliveryTermsTab = template.createSheet(TemplateConfig.TEMPLATE_TAB_TRADING_DELIVERY_TERMS);
        Sheet propertyDetailsTab = template.createSheet(TemplateConfig.TEMPLATE_TAB_PROPERTY_DETAILS);
        Sheet valuesTab = template.createSheet(TemplateConfig.TEMPLATE_TAB_ALLOWED_VALUES_FOR_PROPERTIES);
        Sheet metadataTab = template.createSheet(TemplateConfig.TEMPLATE_TAB_METADATA);

        populateInfoTab(infoTab);
        populateProductPropertiesTab(categories, propertiesTab);
        populateTradingDeliveryTermsTab(tradingDeliveryTermsTab);
        populatePropertyDetailsTab(categories, propertyDetailsTab);
        populateAllowedValuesTab(categories, valuesTab);
        populateMetadataTab(categories, metadataTab);

        return template;
    }

    private void populateInfoTab(Sheet infoTab) {
        int rowIndex = 0;
        // info tab info
        Row row = infoTab.createRow(rowIndex);
        Cell cell = row.createCell(0);
        cell.setCellValue(TemplateConfig.TEMPLATE_INFO_HOW_TO_FILL);
        cell.setCellStyle(headerCellStyle);
        row = infoTab.createRow(++rowIndex);
        row.createCell(0).setCellValue(TemplateConfig.TEMPLATE_INFO_THIS_TAB_PROVIDES);
        rowIndex++;

        // generic information
        row = infoTab.createRow(++rowIndex);
        cell = row.createCell(0);
        cell.setCellValue(TemplateConfig.TEMPLATE_INFO_GENERIC_INFORMATION);
        cell.setCellStyle(headerCellStyle);
        row = infoTab.createRow(++rowIndex);
        row.createCell(0).setCellValue(TemplateConfig.TEMPLATE_INFO_YOU_SHOULD_EDIT);
        row = infoTab.createRow(++rowIndex);
        cell = row.createCell(0);
        cell.setCellValue(TemplateConfig.TEMPLATE_INFO_MANDATORY_INFORMATION);
        cell.setCellStyle(mandatoryCellStyle);
        row = infoTab.createRow(++rowIndex);
        row.createCell(0).setCellValue(TemplateConfig.TEMPLATE_INFO_DATA_FIELDS);
        row = infoTab.createRow(++rowIndex);
        row.createCell(0).setCellValue(TemplateConfig.TEMPLATE_INFO_IN_THIS_WAY);
        row = infoTab.createRow(++rowIndex);
        row.createCell(0).setCellValue(TemplateConfig.TEMPLATE_INFO_EX1);
        row = infoTab.createRow(++rowIndex);
        row.createCell(0).setCellValue(TemplateConfig.TEMPLATE_INFO_EX2);
        row = infoTab.createRow(++rowIndex);
        row.createCell(0).setCellValue(TemplateConfig.TEMPLATE_INFO_EX3);
        rowIndex++;

        // product properties tab info
        row = infoTab.createRow(++rowIndex);
        cell = row.createCell(0);
        cell.setCellValue(TemplateConfig.TEMPLATE_TAB_PRODUCT_PROPERTIES);
        cell.setCellStyle(headerCellStyle);
        row = infoTab.createRow(++rowIndex);
        row.createCell(0).setCellValue(TemplateConfig.TEMPLATE_INFO_THE_TOP_FOUR_COLUMNS);
        row = infoTab.createRow(++rowIndex);
        row.createCell(0).setCellValue(TemplateConfig.TEMPLATE_INFO_THE_FIRST_ROW);
        rowIndex++;
        row = infoTab.createRow(++rowIndex);
        row.createCell(0).setCellValue(TemplateConfig.TEMPLATE_INFO_THE_SECOND_ROW);
        row = infoTab.createRow(++rowIndex);
        row.createCell(0).setCellValue(TemplateConfig.TEMPLATE_INFO_FIXED_PROPERTIES);
        row = infoTab.createRow(++rowIndex);
        row.createCell(0).setCellValue(TemplateConfig.TEMPLATE_INFO_PROPERTIES_ASSOCIATED);
        row = infoTab.createRow(++rowIndex);
        row.createCell(0).setCellValue(TemplateConfig.TEMPLATE_INFO_CUSTOM_PROPERTIES);
        row = infoTab.createRow(++rowIndex);
        row.createCell(0).setCellValue(TemplateConfig.TEMPLATE_INFO_DETAILS_OF_THE_PROPERTY);
        rowIndex++;
        row = infoTab.createRow(++rowIndex);
        row.createCell(0).setCellValue(TemplateConfig.TEMPLATE_INFO_THE_THIRD_ROW);
        row = infoTab.createRow(++rowIndex);
        row.createCell(0).setCellValue(TemplateConfig.TEMPLATE_INFO_TEXT);
        row = infoTab.createRow(++rowIndex);
        row.createCell(0).setCellValue(TemplateConfig.TEMPLATE_INFO_NUMBER);
        row = infoTab.createRow(++rowIndex);
        row.createCell(0).setCellValue(TemplateConfig.TEMPLATE_INFO_QUANTITY);
        row = infoTab.createRow(++rowIndex);
        row.createCell(0).setCellValue(TemplateConfig.TEMPLATE_INFO_FILE);
        row = infoTab.createRow(++rowIndex);
        row.createCell(0).setCellValue(TemplateConfig.TEMPLATE_INFO_BOOLEAN);
        rowIndex++;

        row = infoTab.createRow(++rowIndex);
        row.createCell(0).setCellValue(TemplateConfig.TEMPLATE_INFO_THE_FOURTH_ROW);
        row = infoTab.createRow(++rowIndex);
        row.createCell(0).setCellValue(TemplateConfig.TEMPLATE_INFO_THE_FIFTH_ROW);
        rowIndex++;

        // terms tab info
        row = infoTab.createRow(++rowIndex);
        cell = row.createCell(0);
        cell.setCellValue(TemplateConfig.TEMPLATE_TAB_TRADING_DELIVERY_TERMS);
        cell.setCellStyle(headerCellStyle);
        row = infoTab.createRow(++rowIndex);
        row.createCell(0).setCellValue(TemplateConfig.TEMPLATE_INFO_TRADING_AND_DELIVERY);
        row = infoTab.createRow(++rowIndex);
        row.createCell(0).setCellValue(TemplateConfig.TEMPLATE_INFO_MANUFACTURER_ITEM_IDENTIFICATION);
        row = infoTab.createRow(++rowIndex);
        row.createCell(0).setCellValue(TemplateConfig.TEMPLATE_INFO_NOTADDING_CUSTOM_PROPERTIES);
        rowIndex++;

        // product details tab info
        row = infoTab.createRow(++rowIndex);
        cell = row.createCell(0);
        cell.setCellValue(TemplateConfig.TEMPLATE_TAB_PROPERTY_DETAILS);
        cell.setCellStyle(headerCellStyle);
        row = infoTab.createRow(++rowIndex);
        row.createCell(0).setCellValue(TemplateConfig.TEMPLATE_INFO_THIS_TAB_CONTAINS);
        rowIndex++;

        // allowed values tab info
        row = infoTab.createRow(++rowIndex);
        cell = row.createCell(0);
        cell.setCellValue(TemplateConfig.TEMPLATE_TAB_ALLOWED_VALUES_FOR_PROPERTIES);
        cell.setCellStyle(headerCellStyle);
        row = infoTab.createRow(++rowIndex);
        row.createCell(0).setCellValue(TemplateConfig.TEMPLATE_INFO_THIS_TAB_CONTAINS_VALUES);
        rowIndex++;

        // metadata tab info
        row = infoTab.createRow(++rowIndex);
        cell = row.createCell(0);
        cell.setCellValue(TemplateConfig.TEMPLATE_TAB_METADATA);
        cell.setCellStyle(headerCellStyle);
        row = infoTab.createRow(++rowIndex);
        row.createCell(0).setCellValue(TemplateConfig.TEMPLATE_INFO_THIS_TAB_CONTAINS_INFORMATION);

        infoTab.autoSizeColumn(0);
    }

    private void populateProductPropertiesTab(List<Category> categories, Sheet productPropertiesTab) {
        // create the top row containing the category names and ids on top of the corresponding properties
        // create the dimension tab
        Row topRow = productPropertiesTab.createRow(0);
        Cell cell = getCellWithMissingCellPolicy(topRow, 5);
        cell.setCellValue(TemplateConfig.TEMPLATE_PRODUCT_PROPERTIES_DIMENSIONS);
        cell.setCellStyle(headerCellStyle);
        CellRangeAddress cra = new CellRangeAddress(0, 0, 5, 7);
        productPropertiesTab.addMergedRegion(cra);

        // create the titles for categories
        int columnOffset = TemplateConfig.getFixedPropertiesForProductPropertyTab().size() + 1;
        for (int i = 0; i < categories.size(); i++) {
            if(categories.get(i).getProperties().size() > 0) {
                int colFrom = columnOffset;
                int colTo = columnOffset + categories.get(i).getProperties().size() - 1;
                cell = getCellWithMissingCellPolicy(topRow, colFrom);
                cell.setCellValue(categories.get(i).getPreferredName(defaultLanguage));
                cell.setCellStyle(headerCellStyle);
                cra = new CellRangeAddress(0, 0, colFrom, colTo);
                productPropertiesTab.addMergedRegion(cra);
                columnOffset = colTo + 1;
            }
        }

        // 2nd, 3rd and 4th rows
        // name, data type, unit label on the leftmost column
        int rowIndex = 1;
        Row secondRow = productPropertiesTab.createRow(rowIndex);
        cell = secondRow.createCell(0);
        cell.setCellValue(TemplateConfig.TEMPLATE_PRODUCT_PROPERTIES_PROPERTY_NAME);
        cell.setCellStyle(boldCellStyle);
        Row thirdRow = productPropertiesTab.createRow(++rowIndex);
        cell = thirdRow.createCell(0);
        cell.setCellValue(TemplateConfig.TEMPLATE_PRODUCT_PROPERTIES_PROPERTY_DATA_TYPE);
        cell.setCellStyle(boldCellStyle);
        Row fourthRow = productPropertiesTab.createRow(++rowIndex);
        cell = fourthRow.createCell(0);
        cell.setCellValue(TemplateConfig.TEMPLATE_PRODUCT_PROPERTIES_PROPERTY_UNIT);
        cell.setCellStyle(boldCellStyle);

        // common UBL-based properties
        columnOffset = 1;
        List<Property> properties = TemplateConfig.getFixedPropertiesForProductPropertyTab();
        for (Property property : properties) {
            cell = secondRow.createCell(columnOffset);
            cell.setCellValue(property.getPreferredName(defaultLanguage));
            cell.setCellStyle(boldCellStyle);
            checkMandatory(property, cell);
            thirdRow.createCell(columnOffset).setCellValue(normalizeDataTypeForTemplate(property));
            fourthRow.createCell(columnOffset).setCellValue(property.getUnit() != null ? property.getUnit().getShortName() : "");
            columnOffset++;
        }

        // columns for the properties obtained from the categories
        for (Category category : categories) {
            for (Property property : category.getProperties()) {
                cell = secondRow.createCell(columnOffset);
                cell.setCellValue(property.getPreferredName(defaultLanguage));
                cell.setCellStyle(boldCellStyle);
                thirdRow.createCell(columnOffset).setCellValue(normalizeDataTypeForTemplate(property));
                fourthRow.createCell(columnOffset).setCellValue(property.getUnit() != null ? property.getUnit().getShortName() : "");
                columnOffset++;
            }
        }

        autoSizeAllColumns(productPropertiesTab);
    }

    private void populateTradingDeliveryTermsTab(Sheet termsTab) {
        // create the top row containing the property categories
        // trading details block
        Row topRow = termsTab.createRow(0);
        Cell cell = getCellWithMissingCellPolicy(topRow, 2);
        cell.setCellValue(TemplateConfig.TEMPLATE_TRADING_DELIVERY_TRADING_DETAILS);
        cell.setCellStyle(boldCellStyle);
        CellRangeAddress cra = new CellRangeAddress(0, 0, 2, 5);
        termsTab.addMergedRegion(cra);

        // warranty block
        cell = getCellWithMissingCellPolicy(topRow, 6);
        cell.setCellValue(TemplateConfig.TEMPLATE_TRADING_DELIVERY_WARRANTY);
        cell.setCellStyle(boldCellStyle);
        cra = new CellRangeAddress(0, 0, 6, 7);
        termsTab.addMergedRegion(cra);

        // delivery terms block
        cell = getCellWithMissingCellPolicy(topRow, 8);
        cell.setCellValue(TemplateConfig.TEMPLATE_TRADING_DELIVERY_DELIVERY_TERMS);
        cell.setCellStyle(boldCellStyle);
        cra = new CellRangeAddress(0, 0, 8, 12);
        termsTab.addMergedRegion(cra);

        // packaging block
        cell = getCellWithMissingCellPolicy(topRow, 13);
        cell.setCellValue(TemplateConfig.TEMPLATE_TRADING_DELIVERY_PACKAGING);
        cell.setCellStyle(boldCellStyle);
        cra = new CellRangeAddress(0, 0, 13, 14);
        termsTab.addMergedRegion(cra);


        // 2nd, 3rd and 4th rows
        // name, data type, unit label on the leftmost column
        int rowIndex = 1;
        int columnIndex = 0;
        Row secondRow = termsTab.createRow(rowIndex);
        cell = secondRow.createCell(columnIndex);
        cell.setCellValue(TemplateConfig.TEMPLATE_PRODUCT_PROPERTIES_PROPERTY_NAME);
        cell.setCellStyle(boldCellStyle);
        Row thirdRow = termsTab.createRow(++rowIndex);
        cell = thirdRow.createCell(columnIndex);
        cell.setCellValue(TemplateConfig.TEMPLATE_PRODUCT_PROPERTIES_PROPERTY_DATA_TYPE);
        cell.setCellStyle(boldCellStyle);
        Row fourthRow = termsTab.createRow(++rowIndex);
        cell = fourthRow.createCell(columnIndex);
        cell.setCellValue(TemplateConfig.TEMPLATE_PRODUCT_PROPERTIES_PROPERTY_UNIT);
        cell.setCellStyle(boldCellStyle);

        // common UBL-based properties
        columnIndex = 1;
        List<Property> properties = TemplateConfig.getFixedPropertiesForTermsTab();
        for (Property property : properties) {
            // dropdown menu for incoterms
            if(property.getPreferredName(defaultLanguage).equals(TEMPLATE_TRADING_DELIVERY_INCOTERMS)){
                cell = secondRow.createCell(columnIndex);
                cell.setCellValue(property.getPreferredName(defaultLanguage));
                cell.setCellStyle(boldCellStyle);
                checkMandatory(property, cell);
                thirdRow.createCell(columnIndex).setCellValue(property.getDataType());

                CellRangeAddressList cellRangeAddressList = new CellRangeAddressList(4,4,columnIndex,columnIndex);
                DataValidationHelper dataValidationHelper = new XSSFDataValidationHelper((XSSFSheet) termsTab);
                DataValidationConstraint dataValidationConstraint =dataValidationHelper.createExplicitListConstraint(new String[]{
                        "CIF (Cost, Insurance and Freight)","CIP (Carriage and Insurance Paid to)",
                        "CFR (Cost and Freight)","CPT (Carriage paid to)","DAT (Delivered at Terminal)",
                        "DAP (Delivered at Place)","DDP (Delivery Duty Paid)","EXW (Ex Works)","FAS (Free Alongside Ship)",
                        "FCA (Free Carrier)","FOB (Free on Board)"});
                DataValidation dataValidation  = dataValidationHelper.createValidation(dataValidationConstraint, cellRangeAddressList);
                dataValidation.setSuppressDropDownArrow(true);
                // error box
                dataValidation.setShowErrorBox(true);
                dataValidation.createErrorBox("Invalid input !","Please, select one of the available options");
                // empty cell
                dataValidation.setEmptyCellAllowed(true);
                termsTab.addValidationData(dataValidation);

                columnIndex++;
            }
            else{
                cell = secondRow.createCell(columnIndex);
                cell.setCellValue(property.getPreferredName(defaultLanguage));
                cell.setCellStyle(boldCellStyle);
                checkMandatory(property, cell);
                thirdRow.createCell(columnIndex).setCellValue(property.getDataType());
                fourthRow.createCell(columnIndex).setCellValue(property.getUnit() != null ? property.getUnit().getShortName() : "");
                columnIndex++;
            }
        }

        autoSizeAllColumns(termsTab);
    }

    private void populatePropertyDetailsTab(List<Category> categories, Sheet propertyDetailsTab) {
        // merged cell showing the category names
        int rowIndex = 1;
        int columnIndex = 0;
        for (int i = 0; i < categories.size(); i++) {
            if(categories.get(i).getProperties().size() > 0) {
                int rowFrom = rowIndex;
                int rowTo = rowIndex + categories.get(i).getProperties().size() - 1;
                Row row = propertyDetailsTab.createRow(rowIndex);
                Cell valueCell = getCellWithMissingCellPolicy(row, 0);
                valueCell.setCellValue(categories.get(i).getPreferredName(defaultLanguage));
                valueCell.setCellStyle(headerCellStyle);
                CellRangeAddress cra = new CellRangeAddress(rowFrom, rowTo, 0, 0);
                propertyDetailsTab.addMergedRegion(cra);
                rowIndex = rowTo + 1;
            }
        }
        propertyDetailsTab.autoSizeColumn(0, true);

        // header row containing the labels of property related fields
        rowIndex = 0;
        columnIndex = 1;
        Row row = propertyDetailsTab.createRow(rowIndex);
        Cell cell = row.createCell(columnIndex);
        cell.setCellValue(TemplateConfig.TEMPLATE_PROPERTY_DETAILS_PROPERTY_NAME);
        cell.setCellStyle(headerCellStyle);
        cell = row.createCell(++columnIndex);
        cell.setCellValue(TemplateConfig.TEMPLATE_PROPERTY_DETAILS_SHORT_NAME);
        cell.setCellStyle(headerCellStyle);
        cell = row.createCell(++columnIndex);
        cell.setCellValue(TemplateConfig.TEMPLATE_PROPERTY_DETAILS_DEFINITION);
        cell.setCellStyle(headerCellStyle);
        cell = row.createCell(++columnIndex);
        cell.setCellValue(TemplateConfig.TEMPLATE_PROPERTY_DETAILS_NOTE);
        cell.setCellStyle(headerCellStyle);
        cell = row.createCell(++columnIndex);
        cell.setCellValue(TemplateConfig.TEMPLATE_PROPERTY_DETAILS_REMARK);
        cell.setCellStyle(headerCellStyle);
        cell = row.createCell(++columnIndex);
        cell.setCellValue(TemplateConfig.TEMPLATE_PROPERTY_DETAILS_PREFERRED_SYMBOL);
        cell.setCellStyle(headerCellStyle);
        cell = row.createCell(++columnIndex);
        cell.setCellValue(TemplateConfig.TEMPLATE_PROPERTY_DETAILS_UNIT);
        cell.setCellStyle(headerCellStyle);
        cell = row.createCell(++columnIndex);
        cell.setCellValue(TemplateConfig.TEMPLATE_PROPERTY_DETAILS_IEC_CATEGORY);
        cell.setCellStyle(headerCellStyle);
        cell = row.createCell(++columnIndex);
        cell.setCellValue(TemplateConfig.TEMPLATE_PROPERTY_DETAILS_ATTRIBUTE_TYPE);
        cell.setCellStyle(headerCellStyle);
        cell = row.createCell(++columnIndex);
        cell.setCellValue(TemplateConfig.TEMPLATE_PROPERTY_DETAILS_DATA_TYPE);
        cell.setCellStyle(headerCellStyle);

        // fill in the details about the properties obtained from categories
        for (Category category : categories) {
            for (Property property : category.getProperties()) {
                row = getRow(propertyDetailsTab, ++rowIndex);

                columnIndex = 1;
                row.createCell(columnIndex).setCellValue(property.getPreferredName(defaultLanguage));
                row.createCell(++columnIndex).setCellValue(property.getShortName());
                cell = row.createCell(++columnIndex);
                cell.setCellValue(property.getDefinition());
                cell.setCellStyle(wordWrapStyle);
                row.createCell(++columnIndex).setCellValue(property.getNote());
                cell = row.createCell(++columnIndex);
                cell.setCellValue(property.getRemark());
                cell.setCellStyle(wordWrapStyle);
                row.createCell(++columnIndex).setCellValue(property.getPreferredSymbol());
                row.createCell(++columnIndex).setCellValue(property.getUnit() != null ? property.getUnit().getShortName() : "");
                row.createCell(++columnIndex).setCellValue(property.getIecCategory());
                row.createCell(++columnIndex).setCellValue(property.getAttributeType());
                row.createCell(++columnIndex).setCellValue(property.getDataType());
            }
        }

        autoSizeAllColumns(propertyDetailsTab);
        propertyDetailsTab.setColumnWidth(3, 256*50);
        propertyDetailsTab.setColumnWidth(5, 256*50);
    }

    private void populateAllowedValuesTab(List<Category> categories, Sheet valuesTab) {
        int rowIndex = 1;
        int columnIndex = 0;
        Map<String, Integer> propNums = new LinkedHashMap<>();

        // first fill in the restricted values for relevant properties
        for (Category category : categories) {
            for (Property property : category.getProperties()) {
                List<Value> values = property.getValues();
                if (values.size() > 0) {
                    Row row = getRow(valuesTab, rowIndex++);
                    Cell cell = row.createCell(columnIndex);
                    cell.setCellValue(property.getPreferredName(defaultLanguage));
                    cell.setCellStyle(boldCellStyle);

                    for (int j = 0; j < values.size(); j++) {
                        row = getRow(valuesTab, rowIndex++);
                        row.createCell(columnIndex).setCellValue(values.get(j).getPreferredName());
                    }

                    rowIndex = 0;
                    columnIndex++;

                    // update the number of properties with specific value constraints
                    if (!propNums.containsKey(category.getPreferredName(defaultLanguage))) {
                        propNums.put(category.getPreferredName(defaultLanguage), 1);
                    } else {
                        int count = propNums.get(category.getPreferredName(defaultLanguage));
                        propNums.put(category.getPreferredName(defaultLanguage), ++count);
                    }
                }
            }
        }

        // create the header row using the counts collected above
        Row topRow = valuesTab.createRow(0);
        int columnOffset = 0;
        for (Map.Entry<String, Integer> e : propNums.entrySet()) {
            int colFrom = columnOffset;
            int colTo = columnOffset + e.getValue() - 1;
            Cell valueCell = getCellWithMissingCellPolicy(topRow, colFrom);
            valueCell.setCellValue(e.getKey());
            valueCell.setCellStyle(headerCellStyle);
            if (e.getValue() > 1) {
                CellRangeAddress cra = new CellRangeAddress(0, 0, colFrom, colTo);
                valuesTab.addMergedRegion(cra);
            }
            columnOffset = colTo + 1;
        }

        autoSizeAllColumns(valuesTab);
    }

    private void populateMetadataTab(List<Category> categories, Sheet metadataTab) {
        // category information
        if(categories.size() == 0) {
            return;
        }
        Row firstRow = metadataTab.createRow(0);
        Row secondRow = metadataTab.createRow(1);
        StringBuilder categoryIds = new StringBuilder(""), taxonomyIds = new StringBuilder("");

        for (int i = 0; i < categories.size() - 1; i++) {
            // category ids separated by comma
            categoryIds.append(categories.get(i).getId()).append(",");
            // taxonomy ids separated by comma
            taxonomyIds.append(categories.get(i).getTaxonomyId()).append(",");
        }
        categoryIds.append(categories.get(categories.size() - 1).getId());
        taxonomyIds.append(categories.get(categories.size() - 1).getTaxonomyId());

        firstRow.createCell(0).setCellValue(categoryIds.toString());
        secondRow.createCell(0).setCellValue(taxonomyIds.toString());

    }

    private void checkMandatory(Property property, Cell cell) {
        if (property.getPreferredName(defaultLanguage).contentEquals(TemplateConfig.TEMPLATE_PRODUCT_PROPERTIES_MANUFACTURER_ITEM_IDENTIFICATION) ||
                property.getPreferredName(defaultLanguage).contentEquals(TemplateConfig.TEMPLATE_PRODUCT_PROPERTIES_NAME)) {
            cell.setCellStyle(mandatoryCellStyle);
        }
    }

    private Row getRow(Sheet sheet, int rowNum) {
        Row row = sheet.getRow(rowNum);
        if (row == null) {
            row = sheet.createRow(rowNum);
        }
        return row;
    }

    private Cell getCellWithMissingCellPolicy(Row row, int cellNum) {
        return row.getCell(cellNum, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK);
    }

    private void createStyles() {
        headerCellStyle = template.createCellStyle();
        headerCellStyle.setVerticalAlignment(VerticalAlignment.CENTER);
        Font font = template.createFont();
        font.setBold(true);
        font.setFontHeightInPoints((short) 12);
        headerCellStyle.setFont(font);

        mandatoryCellStyle = template.createCellStyle();
        font = template.createFont();
        font.setBold(true);
        mandatoryCellStyle.setFont(font);
        mandatoryCellStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        mandatoryCellStyle.setFillForegroundColor(IndexedColors.RED.getIndex());

        boldCellStyle = template.createCellStyle();
        font = template.createFont();
        font.setBold(true);
        boldCellStyle.setFont(font);

        wordWrapStyle = template.createCellStyle();
        wordWrapStyle.setWrapText(true);
    }

    private void autoSizeAllColumns(Sheet sheet) {
        int maxColumnIndex = getMaxColumnIndex(sheet);
        for (int i = 0; i <= maxColumnIndex; i++) {
            sheet.autoSizeColumn(i, true);
        }
    }

    private int getMaxColumnIndex(Sheet sheet) {
        int maxColumnIndex = 0;
        for (int i = 0; i <= sheet.getLastRowNum(); i++) {
            Row row = sheet.getRow(i);
            if (row != null) {
                if (row.getLastCellNum() > maxColumnIndex) {
                    maxColumnIndex = row.getLastCellNum();
                }
            }
        }
        return maxColumnIndex;
    }

    private static String normalizeDataTypeForTemplate(Property property) {
        return normalizeDataTypeForTemplate(property.getUnit() != null ? TemplateConfig.TEMPLATE_DATA_TYPE_QUANTITY : property.getDataType());
    }

    public static String normalizeDataTypeForTemplate(String dataType) {
        String normalizedType;
        if (dataType.compareToIgnoreCase(TemplateConfig.TEMPLATE_DATA_TYPE_REAL_MEASURE) == 0) {
            normalizedType = TemplateConfig.TEMPLATE_DATA_TYPE_NUMBER;

        } else if (dataType.compareToIgnoreCase(TemplateConfig.TEMPLATE_DATA_TYPE_STRING) == 0) {
            normalizedType = TemplateConfig.TEMPLATE_DATA_TYPE_TEXT;

        } else {
            normalizedType = dataType;
        }
        return normalizedType;
    }


    public static String denormalizeDataTypeFromTemplate(String datatypeStr) throws TemplateParseException{
        String denormalizedDatatype;
        if (datatypeStr.compareToIgnoreCase(TEMPLATE_DATA_TYPE_NUMBER) == 0) {
            denormalizedDatatype = TEMPLATE_DATA_TYPE_REAL_MEASURE;

        } else if (datatypeStr.compareToIgnoreCase(TEMPLATE_DATA_TYPE_FILE) == 0) {
            denormalizedDatatype = TEMPLATE_DATA_TYPE_BINARY;

        } else if (datatypeStr.compareToIgnoreCase(TEMPLATE_DATA_TYPE_QUANTITY) == 0) {
            denormalizedDatatype = TEMPLATE_DATA_TYPE_QUANTITY;

        } else if (datatypeStr.compareToIgnoreCase(TEMPLATE_DATA_TYPE_BOOLEAN) == 0) {
            denormalizedDatatype = TEMPLATE_DATA_TYPE_BOOLEAN;

        } else if(datatypeStr.compareToIgnoreCase(TEMPLATE_DATA_TYPE_TEXT) == 0){
            denormalizedDatatype = TEMPLATE_DATA_TYPE_STRING;
        } else {
            // for text or other unknown properties
            throw new TemplateParseException("The data type of the property can not be '" + datatypeStr+"'" );
        }
        return denormalizedDatatype;
    }
}
