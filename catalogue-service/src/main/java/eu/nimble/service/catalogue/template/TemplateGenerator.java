package eu.nimble.service.catalogue.template;

import eu.nimble.service.catalogue.model.category.Category;
import eu.nimble.service.catalogue.model.category.Property;
import eu.nimble.service.catalogue.model.category.Value;
import eu.nimble.service.catalogue.exception.TemplateParseException;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.ss.util.CellRangeAddressList;
import org.apache.poi.xssf.usermodel.*;

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
    private CellStyle readOnlyStyle;
    private CellStyle editableStyle;
    private CellStyle tabCellStyle;

    public TemplateGenerator() {
        template = new XSSFWorkbook();
        createStyles();
    }

    public Workbook generateTemplateForCategory(List<Category> categories) {
        Sheet infoTab = template.createSheet(TemplateConfig.TEMPLATE_TAB_INFORMATION);
        Sheet propertiesTab = template.createSheet(TemplateConfig.TEMPLATE_TAB_PRODUCT_PROPERTIES);
        Sheet propertiesExampleTab = template.createSheet(TemplateConfig.TEMPLATE_TAB_PRODUCT_PROPERTIES_EXAMPLE);
        Sheet tradingDeliveryTermsTab = template.createSheet(TemplateConfig.TEMPLATE_TAB_TRADING_DELIVERY_TERMS);
        Sheet tradingDeliveryTermsTabExample = template.createSheet(TemplateConfig.TEMPLATE_TAB_TRADING_DELIVERY_TERMS_EXAMPLE);
        Sheet propertyDetailsTab = template.createSheet(TemplateConfig.TEMPLATE_TAB_PROPERTY_DETAILS);
        Sheet valuesTab = template.createSheet(TemplateConfig.TEMPLATE_TAB_ALLOWED_VALUES_FOR_PROPERTIES);
        Sheet metadataTab = template.createSheet(TemplateConfig.TEMPLATE_TAB_METADATA);
        Sheet sourceList = template.createSheet(TemplateConfig.TEMPLATE_TAB_SOURCE_LIST);

        populateSourceList(sourceList);
        populateInfoTab(infoTab);
        populateProductPropertiesTab(categories, propertiesTab);
        populateProductPropertiesExampleTab(categories,propertiesExampleTab);
        populateTradingDeliveryTermsTab(tradingDeliveryTermsTab);
        populateTradingDeliveryTermsExampleTab(tradingDeliveryTermsTabExample);
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
        cell = row.createCell(0);
        cell.setCellValue(TemplateConfig.TEMPLATE_INFO_NOT_EDITABLE);
        cell.setCellStyle(readOnlyStyle);
        row = infoTab.createRow(++rowIndex);
        cell = row.createCell(0);
        cell.setCellValue(TemplateConfig.TEMPLATE_INFO_YOU_CAN_EDIT);
        cell.setCellStyle(editableStyle);
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

        // product properties example tab info
        row = infoTab.createRow(++rowIndex);
        cell = row.createCell(0);
        cell.setCellValue(TemplateConfig.TEMPLATE_TAB_PRODUCT_PROPERTIES_EXAMPLE);
        cell.setCellStyle(headerCellStyle);
        row = infoTab.createRow(++rowIndex);
        row.createCell(0).setCellValue(TemplateConfig.TEMPLATE_INFO_PRODUCT_PROPERTIES_EXAMPLE);
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

        // terms tab example info
        row = infoTab.createRow(++rowIndex);
        cell = row.createCell(0);
        cell.setCellValue(TemplateConfig.TEMPLATE_TAB_TRADING_DELIVERY_TERMS_EXAMPLE);
        cell.setCellStyle(headerCellStyle);
        row = infoTab.createRow(++rowIndex);
        row.createCell(0).setCellValue(TemplateConfig.TEMPLATE_INFO_TRADING_DELIVERY_EXAMPLE);
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
        // make first column read only
        productPropertiesTab.setDefaultColumnStyle(0,readOnlyStyle);

        // create the top row containing the category names and ids on top of the corresponding properties
        // create the dimension tab
        Row topRow = productPropertiesTab.createRow(0);

        // make first five columns read only
        for (int i=0;i<5;i++){
            topRow.createCell(i).setCellStyle(readOnlyStyle);
        }

        Cell cell = getCellWithMissingCellPolicy(topRow, 5);
        cell.setCellValue(TemplateConfig.TEMPLATE_PRODUCT_PROPERTIES_DIMENSIONS);
        cell.setCellStyle(tabCellStyle);
        CellRangeAddress cra = new CellRangeAddress(0, 0, 5, 7);
        productPropertiesTab.addMergedRegion(cra);

        // create the titles for categories
        int columnOffset = TemplateConfig.getFixedPropertiesForProductPropertyTab().size() + 1;
        for (int i = 0; i < categories.size(); i++) {
            if(categories.get(i).getProperties().size() > 0) {
                int colFrom = columnOffset;
                int colTo = columnOffset + categories.get(i).getProperties().size() - 1;
                cell = getCellWithMissingCellPolicy(topRow, colFrom);
                cell.setCellValue(categories.get(i).getPreferredName());
                cell.setCellStyle(tabCellStyle);
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
        productPropertiesTab.createRow(++rowIndex);

        // common UBL-based properties
        columnOffset = 1;
        List<Property> properties = TemplateConfig.getFixedPropertiesForProductPropertyTab();
        for (Property property : properties) {
            cell = secondRow.createCell(columnOffset);
            cell.setCellValue(property.getPreferredName());
            cell.setCellStyle(boldCellStyle);
            if(checkMandatory(property, cell)){
                productPropertiesTab.getRow(4).createCell(columnOffset).setCellStyle(mandatoryCellStyle);
            }
            else {
                productPropertiesTab.getRow(4).createCell(columnOffset).setCellStyle(editableStyle);
            }
            Cell thirdRowCell = thirdRow.createCell(columnOffset);
            thirdRowCell.setCellValue(normalizeDataTypeForTemplate(property));
            // make thirdRow read only
            thirdRowCell.setCellStyle(readOnlyStyle);

            fourthRow.createCell(columnOffset).setCellValue(property.getUnit() != null ? property.getUnit().getShortName() : "");

            if (property.getDataType().equals("BOOLEAN")){
                CellRangeAddressList cellRangeAddressList = new CellRangeAddressList(4,4,columnOffset,columnOffset);
                DataValidationHelper dataValidationHelper = productPropertiesTab.getDataValidationHelper();
                DataValidationConstraint dataValidationConstraint = dataValidationHelper.createFormulaListConstraint(TemplateConfig.TEMPLATE_BOOLEAN_LIST);
                DataValidation dataValidation  = dataValidationHelper.createValidation(dataValidationConstraint, cellRangeAddressList);
                dataValidation.setSuppressDropDownArrow(true);
                // error box
                dataValidation.setShowErrorBox(true);
                dataValidation.createErrorBox("Invalid input !","Please, select one of the available options");
                // empty cell
                dataValidation.setEmptyCellAllowed(true);
                productPropertiesTab.addValidationData(dataValidation);
            }

            if(property.getPreferredName().equals(TEMPLATE_PRODUCT_PROPERTIES_WIDTH) || property.getPreferredName().equals(TEMPLATE_PRODUCT_PROPERTIES_LENGTH) || property.getPreferredName().equals(TEMPLATE_PRODUCT_PROPERTIES_HEIGHT)){
                CellRangeAddressList cellRangeAddressList = new CellRangeAddressList(3,3,columnOffset,columnOffset);
                DataValidationHelper dataValidationHelper = productPropertiesTab.getDataValidationHelper();
                DataValidationConstraint dataValidationConstraint = dataValidationHelper.createFormulaListConstraint(TemplateConfig.TEMPLATE_DIMENSION_LIST);
                DataValidation dataValidation  = dataValidationHelper.createValidation(dataValidationConstraint, cellRangeAddressList);
                dataValidation.setSuppressDropDownArrow(true);
                // error box
                dataValidation.setShowErrorBox(true);
                dataValidation.createErrorBox("Invalid input !","Please, select one of the available options");
                // empty cell
                dataValidation.setEmptyCellAllowed(true);
                productPropertiesTab.addValidationData(dataValidation);
            }

            // check whether the property needs a unit
            if(!property.getDataType().equals("AMOUNT") && !property.getDataType().equals("QUANTITY")){
                fourthRow.getCell(columnOffset).setCellStyle(readOnlyStyle);
            }
            else {
                fourthRow.getCell(columnOffset).setCellStyle(editableStyle);
            }

            columnOffset++;
        }

        // columns for the properties obtained from the categories
        for (Category category : categories) {
            for (Property property : category.getProperties()) {
                cell = secondRow.createCell(columnOffset);
                cell.setCellValue(property.getPreferredName());
                cell.setCellStyle(boldCellStyle);
                Cell thirdRowCell = thirdRow.createCell(columnOffset);
                thirdRowCell.setCellValue(normalizeDataTypeForTemplate(property));
                // make thirdRow read only
                thirdRowCell.setCellStyle(readOnlyStyle);
                fourthRow.createCell(columnOffset).setCellValue(property.getUnit() != null ? property.getUnit().getShortName() : "");

                productPropertiesTab.getRow(4).createCell(columnOffset).setCellStyle(editableStyle);

                if (property.getDataType().equals("BOOLEAN")){
                    CellRangeAddressList cellRangeAddressList = new CellRangeAddressList(4,4,columnOffset,columnOffset);
                    DataValidationHelper dataValidationHelper = productPropertiesTab.getDataValidationHelper();
                    DataValidationConstraint dataValidationConstraint = dataValidationHelper.createFormulaListConstraint(TemplateConfig.TEMPLATE_BOOLEAN_LIST);
                    DataValidation dataValidation  = dataValidationHelper.createValidation(dataValidationConstraint, cellRangeAddressList);
                    dataValidation.setSuppressDropDownArrow(true);
                    // error box
                    dataValidation.setShowErrorBox(true);
                    dataValidation.createErrorBox("Invalid input !","Please, select one of the available options");
                    // empty cell
                    dataValidation.setEmptyCellAllowed(true);
                    productPropertiesTab.addValidationData(dataValidation);
                }

                // check whether the property needs a unit
                if(!property.getDataType().equals("AMOUNT") && !property.getDataType().equals("QUANTITY")){
                    fourthRow.getCell(columnOffset).setCellStyle(readOnlyStyle);
                }
                else {
                    fourthRow.getCell(columnOffset).setCellStyle(editableStyle);
                }

                columnOffset++;
            }
        }

        autoSizeAllColumns(productPropertiesTab);
    }

    private void populateProductPropertiesExampleTab(List<Category> categories,Sheet productPropertiesExampleTab){
        // make first column read only
        productPropertiesExampleTab.setDefaultColumnStyle(0,readOnlyStyle);

        // create the top row containing the category names and ids on top of the corresponding properties
        // create the dimension tab
        Row topRow = productPropertiesExampleTab.createRow(0);

        // make first five columns read only
        for (int i=0;i<5;i++){
            topRow.createCell(i).setCellStyle(readOnlyStyle);
        }

        Cell cell = getCellWithMissingCellPolicy(topRow, 5);
        cell.setCellValue(TemplateConfig.TEMPLATE_PRODUCT_PROPERTIES_DIMENSIONS);
        cell.setCellStyle(tabCellStyle);
        CellRangeAddress cra = new CellRangeAddress(0, 0, 5, 7);
        productPropertiesExampleTab.addMergedRegion(cra);

        // create the titles for categories
        int columnOffset = TemplateConfig.getFixedPropertiesForProductPropertyTab().size() + 1;
        for (int i = 0; i < categories.size(); i++) {
            if(categories.get(i).getProperties().size() > 0) {
                int colFrom = columnOffset;
                int colTo = columnOffset + categories.get(i).getProperties().size() - 1;
                cell = getCellWithMissingCellPolicy(topRow, colFrom);
                cell.setCellValue(categories.get(i).getPreferredName());
                cell.setCellStyle(tabCellStyle);
                cra = new CellRangeAddress(0, 0, colFrom, colTo);
                productPropertiesExampleTab.addMergedRegion(cra);
                columnOffset = colTo + 1;
            }
        }

        // 2nd, 3rd and 4th rows
        // name, data type, unit label on the leftmost column
        int rowIndex = 1;
        Row secondRow = productPropertiesExampleTab.createRow(rowIndex);
        cell = secondRow.createCell(0);
        cell.setCellValue(TemplateConfig.TEMPLATE_PRODUCT_PROPERTIES_PROPERTY_NAME);
        cell.setCellStyle(boldCellStyle);
        Row thirdRow = productPropertiesExampleTab.createRow(++rowIndex);
        cell = thirdRow.createCell(0);
        cell.setCellValue(TemplateConfig.TEMPLATE_PRODUCT_PROPERTIES_PROPERTY_DATA_TYPE);
        cell.setCellStyle(boldCellStyle);
        Row fourthRow = productPropertiesExampleTab.createRow(++rowIndex);
        cell = fourthRow.createCell(0);
        cell.setCellValue(TemplateConfig.TEMPLATE_PRODUCT_PROPERTIES_PROPERTY_UNIT);
        cell.setCellStyle(boldCellStyle);
        productPropertiesExampleTab.createRow(++rowIndex);

        // common UBL-based properties
        columnOffset = 1;
        List<Property> properties = TemplateConfig.getFixedPropertiesForProductPropertyTab();
        for (Property property : properties) {
            cell = secondRow.createCell(columnOffset);
            cell.setCellValue(property.getPreferredName());
            cell.setCellStyle(boldCellStyle);
            if(checkMandatory(property, cell)){
                productPropertiesExampleTab.getRow(4).createCell(columnOffset).setCellStyle(mandatoryCellStyle);
            }
            else {
                productPropertiesExampleTab.getRow(4).createCell(columnOffset).setCellStyle(editableStyle);
            }
            Cell thirdRowCell = thirdRow.createCell(columnOffset);
            thirdRowCell.setCellValue(normalizeDataTypeForTemplate(property));
            // make thirdRow read only
            thirdRowCell.setCellStyle(readOnlyStyle);

            fourthRow.createCell(columnOffset).setCellValue(property.getUnit() != null ? property.getUnit().getShortName() : "");

            if(property.getPreferredName().equals(TEMPLATE_PRODUCT_PROPERTIES_MANUFACTURER_ITEM_IDENTIFICATION)){
                productPropertiesExampleTab.getRow(4).getCell(columnOffset).setCellValue("Product_id1");
                productPropertiesExampleTab.createRow(5).createCell(columnOffset).setCellValue("Product_id2");
                productPropertiesExampleTab.createRow(6).createCell(columnOffset).setCellValue("Product_id3");
            }
            else if(property.getPreferredName().equals(TEMPLATE_PRODUCT_PROPERTIES_NAME)){
                productPropertiesExampleTab.getRow(4).getCell(columnOffset).setCellValue("Plastic-head mallet");
                productPropertiesExampleTab.getRow(5).createCell(columnOffset).setCellValue("Iron-head mallet");
                productPropertiesExampleTab.getRow(6).createCell(columnOffset).setCellValue("Wood-head mallet");
            }
            else if(property.getPreferredName().equals(TEMPLATE_PRODUCT_PROPERTIES_DESCRIPTION)){
                productPropertiesExampleTab.getRow(4).getCell(columnOffset).setCellValue("Mallet that can be used mosaic tiling");
                productPropertiesExampleTab.getRow(5).createCell(columnOffset).setCellValue("Strong mallet");
                productPropertiesExampleTab.getRow(6).createCell(columnOffset).setCellValue("Great for metal working");
            }
            else if(property.getPreferredName().equals(TEMPLATE_PRODUCT_PROPERTIES_CERTIFICATIONS)){
                productPropertiesExampleTab.getRow(4).getCell(columnOffset).setCellValue("Mineral Oil MSDB");
                productPropertiesExampleTab.getRow(5).createCell(columnOffset).setCellValue("ISO9001");
                productPropertiesExampleTab.getRow(6).createCell(columnOffset).setCellValue("SGS Test Report|Wood Spoon Food Safe Test Report");
            }
            else if(property.getPreferredName().equals(TEMPLATE_PRODUCT_PROPERTIES_WIDTH) || property.getPreferredName().equals(TEMPLATE_PRODUCT_PROPERTIES_LENGTH) || property.getPreferredName().equals(TEMPLATE_PRODUCT_PROPERTIES_HEIGHT)){
                CellRangeAddressList cellRangeAddressList = new CellRangeAddressList(3,3,columnOffset,columnOffset);
                DataValidationHelper dataValidationHelper = productPropertiesExampleTab.getDataValidationHelper();
                DataValidationConstraint dataValidationConstraint = dataValidationHelper.createFormulaListConstraint(TemplateConfig.TEMPLATE_DIMENSION_LIST);
                DataValidation dataValidation  = dataValidationHelper.createValidation(dataValidationConstraint, cellRangeAddressList);
                dataValidation.setSuppressDropDownArrow(true);
                // error box
                dataValidation.setShowErrorBox(true);
                dataValidation.createErrorBox("Invalid input !","Please, select one of the available options");
                // empty cell
                dataValidation.setEmptyCellAllowed(true);
                productPropertiesExampleTab.addValidationData(dataValidation);

                productPropertiesExampleTab.getRow(3).getCell(columnOffset).setCellValue("mm");

                if(property.getPreferredName().equals(TEMPLATE_PRODUCT_PROPERTIES_WIDTH)){
                    productPropertiesExampleTab.getRow(4).getCell(columnOffset).setCellValue("83");
                    productPropertiesExampleTab.getRow(5).createCell(columnOffset).setCellValue("78");
                    productPropertiesExampleTab.getRow(6).createCell(columnOffset).setCellValue("80");
                }
                else if(property.getPreferredName().equals(TEMPLATE_PRODUCT_PROPERTIES_LENGTH)){
                    productPropertiesExampleTab.getRow(4).getCell(columnOffset).setCellValue("43");
                    productPropertiesExampleTab.getRow(5).createCell(columnOffset).setCellValue("35");
                    productPropertiesExampleTab.getRow(6).createCell(columnOffset).setCellValue("40");
                }
                else if(property.getPreferredName().equals(TEMPLATE_PRODUCT_PROPERTIES_HEIGHT)){
                    productPropertiesExampleTab.getRow(4).getCell(columnOffset).setCellValue("315");
                    productPropertiesExampleTab.getRow(5).createCell(columnOffset).setCellValue("300");
                    productPropertiesExampleTab.getRow(6).createCell(columnOffset).setCellValue("320");
                }
            }

            // check whether the property needs a unit
            if(!property.getDataType().equals("AMOUNT") && !property.getDataType().equals("QUANTITY")){
                fourthRow.getCell(columnOffset).setCellStyle(readOnlyStyle);
            }
            else {
                fourthRow.getCell(columnOffset).setCellStyle(editableStyle);
            }

            columnOffset++;
        }

        // columns for the properties obtained from the categories
        for (Category category : categories) {
            for (Property property : category.getProperties()) {
                cell = secondRow.createCell(columnOffset);
                cell.setCellValue(property.getPreferredName());
                cell.setCellStyle(boldCellStyle);
                Cell thirdRowCell = thirdRow.createCell(columnOffset);
                thirdRowCell.setCellValue(normalizeDataTypeForTemplate(property));
                // make thirdRow read only
                thirdRowCell.setCellStyle(readOnlyStyle);
                fourthRow.createCell(columnOffset).setCellValue(property.getUnit() != null ? property.getUnit().getShortName() : "");

                productPropertiesExampleTab.getRow(4).createCell(columnOffset).setCellStyle(editableStyle);

                if (property.getDataType().equals("BOOLEAN")){
                    CellRangeAddressList cellRangeAddressList = new CellRangeAddressList(4,4,columnOffset,columnOffset);
                    DataValidationHelper dataValidationHelper = productPropertiesExampleTab.getDataValidationHelper();
                    DataValidationConstraint dataValidationConstraint = dataValidationHelper.createFormulaListConstraint(TemplateConfig.TEMPLATE_BOOLEAN_LIST);
                    DataValidation dataValidation  = dataValidationHelper.createValidation(dataValidationConstraint, cellRangeAddressList);
                    dataValidation.setSuppressDropDownArrow(true);
                    // error box
                    dataValidation.setShowErrorBox(true);
                    dataValidation.createErrorBox("Invalid input !","Please, select one of the available options");
                    // empty cell
                    dataValidation.setEmptyCellAllowed(true);
                    productPropertiesExampleTab.addValidationData(dataValidation);
                }

                // check whether the property needs a unit
                if(!property.getDataType().equals("AMOUNT") && !property.getDataType().equals("QUANTITY")){
                    fourthRow.getCell(columnOffset).setCellStyle(readOnlyStyle);
                }
                else {
                    fourthRow.getCell(columnOffset).setCellStyle(editableStyle);
                }

                columnOffset++;
            }
        }

        autoSizeAllColumns(productPropertiesExampleTab);
    }

    private void populateTradingDeliveryTermsTab(Sheet termsTab) {
        // make first column read only
        termsTab.setDefaultColumnStyle(0,readOnlyStyle);

        // create the top row containing the property categories
        // trading details block
        Row topRow = termsTab.createRow(0);
        // make first five columns read only
        for (int i=0;i<5;i++){
            topRow.createCell(i).setCellStyle(readOnlyStyle);
        }
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
        termsTab.createRow(++rowIndex);

        // common UBL-based properties
        columnIndex = 1;
        List<Property> properties = TemplateConfig.getFixedPropertiesForTermsTab();
        for (Property property : properties) {

            cell = secondRow.createCell(columnIndex);
            cell.setCellValue(property.getPreferredName());
            cell.setCellStyle(boldCellStyle);
            if(checkMandatory(property, cell)){
                termsTab.getRow(4).createCell(columnIndex).setCellStyle(mandatoryCellStyle);
            }
            else {
                termsTab.getRow(4).createCell(columnIndex).setCellStyle(editableStyle);
            }
            Cell thirdRowCell = thirdRow.createCell(columnIndex);
            thirdRowCell.setCellValue(property.getDataType());
            // make thirdRow read only
            thirdRowCell.setCellStyle(readOnlyStyle);
            fourthRow.createCell(columnIndex).setCellValue(property.getUnit() != null ? property.getUnit().getShortName() : "");

            // dropdown menu for incoterms
            if(property.getPreferredName().equals(TEMPLATE_TRADING_DELIVERY_INCOTERMS)){
                CellRangeAddressList cellRangeAddressList = new CellRangeAddressList(4,4,columnIndex,columnIndex);
                DataValidationHelper dataValidationHelper = termsTab.getDataValidationHelper();
                DataValidationConstraint dataValidationConstraint = dataValidationHelper.createFormulaListConstraint(TemplateConfig.TEMPLATE_INCOTERMS_LIST);
                DataValidation dataValidation  = dataValidationHelper.createValidation(dataValidationConstraint, cellRangeAddressList);
                dataValidation.setSuppressDropDownArrow(true);
                // error box
                dataValidation.setShowErrorBox(true);
                dataValidation.createErrorBox("Invalid input !","Please, select one of the available options");
                // empty cell
                dataValidation.setEmptyCellAllowed(true);
                termsTab.addValidationData(dataValidation);

            }
            else if (property.getDataType().equals("BOOLEAN")){
                CellRangeAddressList cellRangeAddressList = new CellRangeAddressList(4,4,columnIndex,columnIndex);
                DataValidationHelper dataValidationHelper = termsTab.getDataValidationHelper();
                DataValidationConstraint dataValidationConstraint = dataValidationHelper.createFormulaListConstraint(TemplateConfig.TEMPLATE_BOOLEAN_LIST);
                DataValidation dataValidation  = dataValidationHelper.createValidation(dataValidationConstraint, cellRangeAddressList);
                dataValidation.setSuppressDropDownArrow(true);
                // error box
                dataValidation.setShowErrorBox(true);
                dataValidation.createErrorBox("Invalid input !","Please, select one of the available options");
                // empty cell
                dataValidation.setEmptyCellAllowed(true);
                termsTab.addValidationData(dataValidation);
            }
            else if(property.getPreferredName().equals(TemplateConfig.TEMPLATE_TRADING_DELIVERY_WARRANTY_VALIDITY_PERIOD)){
                CellRangeAddressList cellRangeAddressList = new CellRangeAddressList(3,3,columnIndex,columnIndex);
                DataValidationHelper dataValidationHelper = termsTab.getDataValidationHelper();
                DataValidationConstraint dataValidationConstraint = dataValidationHelper.createFormulaListConstraint(TemplateConfig.TEMPLATE_WARRANTY_VALIDITY_LIST);
                DataValidation dataValidation  = dataValidationHelper.createValidation(dataValidationConstraint, cellRangeAddressList);
                dataValidation.setSuppressDropDownArrow(true);
                // error box
                dataValidation.setShowErrorBox(true);
                dataValidation.createErrorBox("Invalid input !","Please, select one of the available options");
                // empty cell
                dataValidation.setEmptyCellAllowed(true);
                termsTab.addValidationData(dataValidation);
            }
            else if(property.getPreferredName().equals(TemplateConfig.TEMPLATE_TRADING_DELIVERY_ESTIMATED_DELIVERY_PERIOD)){
                CellRangeAddressList cellRangeAddressList = new CellRangeAddressList(3,3,columnIndex,columnIndex);
                DataValidationHelper dataValidationHelper = termsTab.getDataValidationHelper();
                DataValidationConstraint dataValidationConstraint = dataValidationHelper.createFormulaListConstraint(TemplateConfig.TEMPLATE_DELIVERY_PERIOD_LIST);
                DataValidation dataValidation  = dataValidationHelper.createValidation(dataValidationConstraint, cellRangeAddressList);
                dataValidation.setSuppressDropDownArrow(true);
                // error box
                dataValidation.setShowErrorBox(true);
                dataValidation.createErrorBox("Invalid input !","Please, select one of the available options");
                // empty cell
                dataValidation.setEmptyCellAllowed(true);
                termsTab.addValidationData(dataValidation);
            }
            // check whether the property needs a unit
            if(!property.getDataType().equals("AMOUNT") && !property.getDataType().equals("QUANTITY")){
                fourthRow.getCell(columnIndex).setCellStyle(readOnlyStyle);
            }
            else if(property.getDataType().equals("AMOUNT")){
                fourthRow.getCell(columnIndex).setCellStyle(editableStyle);
                CellRangeAddressList cellRangeAddressList = new CellRangeAddressList(3,3,columnIndex,columnIndex);
                DataValidationHelper dataValidationHelper = termsTab.getDataValidationHelper();
                DataValidationConstraint dataValidationConstraint = dataValidationHelper.createFormulaListConstraint(TemplateConfig.TEMPLATE_CURRENCY_LIST);
                DataValidation dataValidation  = dataValidationHelper.createValidation(dataValidationConstraint, cellRangeAddressList);
                dataValidation.setSuppressDropDownArrow(true);
                // error box
                dataValidation.setShowErrorBox(true);
                dataValidation.createErrorBox("Invalid input !","Please, select one of the available options");
                // empty cell
                dataValidation.setEmptyCellAllowed(true);
                termsTab.addValidationData(dataValidation);
            }
            else {
                fourthRow.getCell(columnIndex).setCellStyle(editableStyle);
            }
            columnIndex++;
        }

        autoSizeAllColumns(termsTab);
    }

    private void populateTradingDeliveryTermsExampleTab(Sheet termsExampleTab) {
        // make first column read only
        termsExampleTab.setDefaultColumnStyle(0,readOnlyStyle);

        // create the top row containing the property categories
        // trading details block
        Row topRow = termsExampleTab.createRow(0);
        // make first five columns read only
        for (int i=0;i<5;i++){
            topRow.createCell(i).setCellStyle(readOnlyStyle);
        }
        Cell cell = getCellWithMissingCellPolicy(topRow, 2);
        cell.setCellValue(TemplateConfig.TEMPLATE_TRADING_DELIVERY_TRADING_DETAILS);
        cell.setCellStyle(boldCellStyle);
        CellRangeAddress cra = new CellRangeAddress(0, 0, 2, 5);
        termsExampleTab.addMergedRegion(cra);

        // warranty block
        cell = getCellWithMissingCellPolicy(topRow, 6);
        cell.setCellValue(TemplateConfig.TEMPLATE_TRADING_DELIVERY_WARRANTY);
        cell.setCellStyle(boldCellStyle);
        cra = new CellRangeAddress(0, 0, 6, 7);
        termsExampleTab.addMergedRegion(cra);

        // delivery terms block
        cell = getCellWithMissingCellPolicy(topRow, 8);
        cell.setCellValue(TemplateConfig.TEMPLATE_TRADING_DELIVERY_DELIVERY_TERMS);
        cell.setCellStyle(boldCellStyle);
        cra = new CellRangeAddress(0, 0, 8, 12);
        termsExampleTab.addMergedRegion(cra);

        // packaging block
        cell = getCellWithMissingCellPolicy(topRow, 13);
        cell.setCellValue(TemplateConfig.TEMPLATE_TRADING_DELIVERY_PACKAGING);
        cell.setCellStyle(boldCellStyle);
        cra = new CellRangeAddress(0, 0, 13, 14);
        termsExampleTab.addMergedRegion(cra);


        // 2nd, 3rd and 4th rows
        // name, data type, unit label on the leftmost column
        int rowIndex = 1;
        int columnIndex = 0;
        Row secondRow = termsExampleTab.createRow(rowIndex);
        cell = secondRow.createCell(columnIndex);
        cell.setCellValue(TemplateConfig.TEMPLATE_PRODUCT_PROPERTIES_PROPERTY_NAME);
        cell.setCellStyle(boldCellStyle);
        Row thirdRow = termsExampleTab.createRow(++rowIndex);
        cell = thirdRow.createCell(columnIndex);
        cell.setCellValue(TemplateConfig.TEMPLATE_PRODUCT_PROPERTIES_PROPERTY_DATA_TYPE);
        cell.setCellStyle(boldCellStyle);
        Row fourthRow = termsExampleTab.createRow(++rowIndex);
        cell = fourthRow.createCell(columnIndex);
        cell.setCellValue(TemplateConfig.TEMPLATE_PRODUCT_PROPERTIES_PROPERTY_UNIT);
        cell.setCellStyle(boldCellStyle);
        termsExampleTab.createRow(++rowIndex);

        // common UBL-based properties
        columnIndex = 1;
        List<Property> properties = TemplateConfig.getFixedPropertiesForTermsTab();
        for (Property property : properties) {

            cell = secondRow.createCell(columnIndex);
            cell.setCellValue(property.getPreferredName());
            cell.setCellStyle(boldCellStyle);
            if(checkMandatory(property, cell)){
                termsExampleTab.getRow(4).createCell(columnIndex).setCellStyle(mandatoryCellStyle);
            }
            else {
                termsExampleTab.getRow(4).createCell(columnIndex).setCellStyle(editableStyle);
            }
            Cell thirdRowCell = thirdRow.createCell(columnIndex);
            thirdRowCell.setCellValue(property.getDataType());
            // make thirdRow read only
            thirdRowCell.setCellStyle(readOnlyStyle);
            fourthRow.createCell(columnIndex).setCellValue(property.getUnit() != null ? property.getUnit().getShortName() : "");

            // fill cells with example values
            if (property.getPreferredName().equals(TEMPLATE_PRODUCT_PROPERTIES_MANUFACTURER_ITEM_IDENTIFICATION)){
                termsExampleTab.getRow(4).getCell(columnIndex).setCellValue("Product_id1");
                termsExampleTab.createRow(5).createCell(columnIndex).setCellValue("Product_id2");
                termsExampleTab.createRow(6).createCell(columnIndex).setCellValue("Product_id3");
            }
            else if(property.getPreferredName().equals(TEMPLATE_TRADING_DELIVERY_PRICE_BASE_QUANTITY)){
                termsExampleTab.getRow(3).getCell(columnIndex).setCellValue("piece");
                termsExampleTab.getRow(4).getCell(columnIndex).setCellValue("1");
                termsExampleTab.getRow(5).createCell(columnIndex).setCellValue("1");
                termsExampleTab.getRow(6).createCell(columnIndex).setCellValue("1");
            }
            else if(property.getPreferredName().equals(TEMPLATE_TRADING_DELIVERY_MINIMUM_ORDER_QUANTITY)){
                termsExampleTab.getRow(3).getCell(columnIndex).setCellValue("piece");
                termsExampleTab.getRow(4).getCell(columnIndex).setCellValue("3000");
                termsExampleTab.getRow(5).createCell(columnIndex).setCellValue("3000");
                termsExampleTab.getRow(6).createCell(columnIndex).setCellValue("1000");
            }
//            else if(property.getPreferredName().equals(TEMPLATE_TRADING_DELIVERY_WARRANTY_INFORMATION)){
//                termsExampleTab.getRow(4).getCell(columnIndex).setCellValue("Warranty information here");
//                termsExampleTab.getRow(5).createCell(columnIndex).setCellValue("Warranty information here");
//                termsExampleTab.getRow(6).createCell(columnIndex).setCellValue("Warranty information here");
//            }
//            else if(property.getPreferredName().equals(TEMPLATE_TRADING_DELIVERY_SPECIAL_TERMS)){
//                termsExampleTab.getRow(4).getCell(columnIndex).setCellValue("Special terms here");
//                termsExampleTab.getRow(5).createCell(columnIndex).setCellValue("Special terms here");
//                termsExampleTab.getRow(6).createCell(columnIndex).setCellValue("Special terms here");
//            }
            else if(property.getPreferredName().equals(TEMPLATE_TRADING_DELIVERY_APPLICABLE_ADDRESS_COUNTRY)){
                termsExampleTab.getRow(4).getCell(columnIndex).setCellValue("China");
                termsExampleTab.getRow(5).createCell(columnIndex).setCellValue("");
                termsExampleTab.getRow(6).createCell(columnIndex).setCellValue("");
            }
            else if(property.getPreferredName().equals(TEMPLATE_TRADING_DELIVERY_TRANSPORT_MODE)){
                termsExampleTab.getRow(4).getCell(columnIndex).setCellValue("Sea | Air");
                termsExampleTab.getRow(5).createCell(columnIndex).setCellValue("Road");
                termsExampleTab.getRow(6).createCell(columnIndex).setCellValue("Road");
            }
            else if(property.getPreferredName().equals(TEMPLATE_TRADING_DELIVERY_PACKAGING_TYPE)){
                termsExampleTab.getRow(4).getCell(columnIndex).setCellValue("box");
                termsExampleTab.getRow(5).createCell(columnIndex).setCellValue("cartons");
                termsExampleTab.getRow(6).createCell(columnIndex).setCellValue("polybag");
            }
            else if(property.getPreferredName().equals(TEMPLATE_TRADING_DELIVERY_PACKAGE_QUANTITY)){
                termsExampleTab.getRow(3).getCell(columnIndex).setCellValue("items");
                termsExampleTab.getRow(4).getCell(columnIndex).setCellValue("10");
                termsExampleTab.getRow(5).createCell(columnIndex).setCellValue("30");
                termsExampleTab.getRow(6).createCell(columnIndex).setCellValue("1");
            }
            // dropdown menu for incoterms
            else if(property.getPreferredName().equals(TEMPLATE_TRADING_DELIVERY_INCOTERMS)){
                CellRangeAddressList cellRangeAddressList = new CellRangeAddressList(4,4,columnIndex,columnIndex);
                DataValidationHelper dataValidationHelper = termsExampleTab.getDataValidationHelper();
                DataValidationConstraint dataValidationConstraint = dataValidationHelper.createFormulaListConstraint(TemplateConfig.TEMPLATE_INCOTERMS_LIST);
                DataValidation dataValidation  = dataValidationHelper.createValidation(dataValidationConstraint, cellRangeAddressList);
                dataValidation.setSuppressDropDownArrow(true);
                // error box
                dataValidation.setShowErrorBox(true);
                dataValidation.createErrorBox("Invalid input !","Please, select one of the available options");
                // empty cell
                dataValidation.setEmptyCellAllowed(true);
                termsExampleTab.addValidationData(dataValidation);

                termsExampleTab.getRow(4).getCell(columnIndex).setCellValue("CIF (Cost,Insurance and Freight)");
                termsExampleTab.getRow(5).createCell(columnIndex).setCellValue("FOB (Free on Board)");
                termsExampleTab.getRow(6).createCell(columnIndex).setCellValue("DAT (Delivered at Terminal)");
            }
            else if (property.getDataType().equals("BOOLEAN")){
                CellRangeAddressList cellRangeAddressList = new CellRangeAddressList(4,4,columnIndex,columnIndex);
                DataValidationHelper dataValidationHelper = termsExampleTab.getDataValidationHelper();
                DataValidationConstraint dataValidationConstraint = dataValidationHelper.createFormulaListConstraint(TemplateConfig.TEMPLATE_BOOLEAN_LIST);
                DataValidation dataValidation  = dataValidationHelper.createValidation(dataValidationConstraint, cellRangeAddressList);
                dataValidation.setSuppressDropDownArrow(true);
                // error box
                dataValidation.setShowErrorBox(true);
                dataValidation.createErrorBox("Invalid input !","Please, select one of the available options");
                // empty cell
                dataValidation.setEmptyCellAllowed(true);
                termsExampleTab.addValidationData(dataValidation);

                termsExampleTab.getRow(4).getCell(columnIndex).setCellValue("TRUE");
                termsExampleTab.getRow(5).createCell(columnIndex).setCellValue("FALSE");
                termsExampleTab.getRow(6).createCell(columnIndex).setCellValue("FALSE");
            }
            else if(property.getPreferredName().equals(TemplateConfig.TEMPLATE_TRADING_DELIVERY_WARRANTY_VALIDITY_PERIOD)){
                CellRangeAddressList cellRangeAddressList = new CellRangeAddressList(3,3,columnIndex,columnIndex);
                DataValidationHelper dataValidationHelper = termsExampleTab.getDataValidationHelper();
                DataValidationConstraint dataValidationConstraint = dataValidationHelper.createFormulaListConstraint(TemplateConfig.TEMPLATE_WARRANTY_VALIDITY_LIST);
                DataValidation dataValidation  = dataValidationHelper.createValidation(dataValidationConstraint, cellRangeAddressList);
                dataValidation.setSuppressDropDownArrow(true);
                // error box
                dataValidation.setShowErrorBox(true);
                dataValidation.createErrorBox("Invalid input !","Please, select one of the available options");
                // empty cell
                dataValidation.setEmptyCellAllowed(true);
                termsExampleTab.addValidationData(dataValidation);

                termsExampleTab.getRow(3).getCell(columnIndex).setCellValue("year");
                termsExampleTab.getRow(4).getCell(columnIndex).setCellValue("1");
                termsExampleTab.getRow(5).createCell(columnIndex).setCellValue("3");
                termsExampleTab.getRow(6).createCell(columnIndex).setCellValue("2");
            }
            else if(property.getPreferredName().equals(TemplateConfig.TEMPLATE_TRADING_DELIVERY_ESTIMATED_DELIVERY_PERIOD)){
                CellRangeAddressList cellRangeAddressList = new CellRangeAddressList(3,3,columnIndex,columnIndex);
                DataValidationHelper dataValidationHelper = termsExampleTab.getDataValidationHelper();
                DataValidationConstraint dataValidationConstraint = dataValidationHelper.createFormulaListConstraint(TemplateConfig.TEMPLATE_DELIVERY_PERIOD_LIST);
                DataValidation dataValidation  = dataValidationHelper.createValidation(dataValidationConstraint, cellRangeAddressList);
                dataValidation.setSuppressDropDownArrow(true);
                // error box
                dataValidation.setShowErrorBox(true);
                dataValidation.createErrorBox("Invalid input !","Please, select one of the available options");
                // empty cell
                dataValidation.setEmptyCellAllowed(true);
                termsExampleTab.addValidationData(dataValidation);

                termsExampleTab.getRow(3).getCell(columnIndex).setCellValue("weeks");
                termsExampleTab.getRow(4).getCell(columnIndex).setCellValue("1");
                termsExampleTab.getRow(5).createCell(columnIndex).setCellValue("4");
                termsExampleTab.getRow(6).createCell(columnIndex).setCellValue("2");
            }
            // check whether the property needs a unit
            if(!property.getDataType().equals("AMOUNT") && !property.getDataType().equals("QUANTITY")){
                fourthRow.getCell(columnIndex).setCellStyle(readOnlyStyle);
            }
            else if(property.getDataType().equals("AMOUNT")){
                fourthRow.getCell(columnIndex).setCellStyle(editableStyle);
                CellRangeAddressList cellRangeAddressList = new CellRangeAddressList(3,3,columnIndex,columnIndex);
                DataValidationHelper dataValidationHelper = termsExampleTab.getDataValidationHelper();
                DataValidationConstraint dataValidationConstraint = dataValidationHelper.createFormulaListConstraint(TemplateConfig.TEMPLATE_CURRENCY_LIST);
                DataValidation dataValidation  = dataValidationHelper.createValidation(dataValidationConstraint, cellRangeAddressList);
                dataValidation.setSuppressDropDownArrow(true);
                // error box
                dataValidation.setShowErrorBox(true);
                dataValidation.createErrorBox("Invalid input !","Please, select one of the available options");
                // empty cell
                dataValidation.setEmptyCellAllowed(true);
                termsExampleTab.addValidationData(dataValidation);

                termsExampleTab.getRow(3).getCell(columnIndex).setCellValue("EUR");
                termsExampleTab.getRow(4).getCell(columnIndex).setCellValue("4");
                termsExampleTab.getRow(5).createCell(columnIndex).setCellValue("6");
                termsExampleTab.getRow(6).createCell(columnIndex).setCellValue("1");
            }
            else {
                fourthRow.getCell(columnIndex).setCellStyle(editableStyle);
            }
            columnIndex++;
        }

        autoSizeAllColumns(termsExampleTab);
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
                valueCell.setCellValue(categories.get(i).getPreferredName());
                valueCell.setCellStyle(headerCellStyle);
                CellRangeAddress cra = new CellRangeAddress(rowFrom, rowTo, 0, 0);
                propertyDetailsTab.addMergedRegion(cra);
                rowIndex = rowTo + 1;
            }
        }
        propertyDetailsTab.autoSizeColumn(0, true);

        // header row containing the labels of property related fields
        rowIndex = 0;
        columnIndex = 0;
        Row row = propertyDetailsTab.createRow(rowIndex);
        Cell cell = row.createCell(columnIndex);
        cell.setCellStyle(tabCellStyle);
        cell = row.createCell(++columnIndex);
        cell.setCellValue(TemplateConfig.TEMPLATE_PROPERTY_DETAILS_PROPERTY_NAME);
        cell.setCellStyle(tabCellStyle);
        cell = row.createCell(++columnIndex);
        cell.setCellValue(TemplateConfig.TEMPLATE_PROPERTY_DETAILS_SHORT_NAME);
        cell.setCellStyle(tabCellStyle);
        cell = row.createCell(++columnIndex);
        cell.setCellValue(TemplateConfig.TEMPLATE_PROPERTY_DETAILS_DEFINITION);
        cell.setCellStyle(tabCellStyle);
        cell = row.createCell(++columnIndex);
        cell.setCellValue(TemplateConfig.TEMPLATE_PROPERTY_DETAILS_NOTE);
        cell.setCellStyle(tabCellStyle);
        cell = row.createCell(++columnIndex);
        cell.setCellValue(TemplateConfig.TEMPLATE_PROPERTY_DETAILS_REMARK);
        cell.setCellStyle(tabCellStyle);
        cell = row.createCell(++columnIndex);
        cell.setCellValue(TemplateConfig.TEMPLATE_PROPERTY_DETAILS_PREFERRED_SYMBOL);
        cell.setCellStyle(tabCellStyle);
        cell = row.createCell(++columnIndex);
        cell.setCellValue(TemplateConfig.TEMPLATE_PROPERTY_DETAILS_UNIT);
        cell.setCellStyle(tabCellStyle);
        cell = row.createCell(++columnIndex);
        cell.setCellValue(TemplateConfig.TEMPLATE_PROPERTY_DETAILS_IEC_CATEGORY);
        cell.setCellStyle(tabCellStyle);
        cell = row.createCell(++columnIndex);
        cell.setCellValue(TemplateConfig.TEMPLATE_PROPERTY_DETAILS_ATTRIBUTE_TYPE);
        cell.setCellStyle(tabCellStyle);
        cell = row.createCell(++columnIndex);
        cell.setCellValue(TemplateConfig.TEMPLATE_PROPERTY_DETAILS_DATA_TYPE);
        cell.setCellStyle(tabCellStyle);

        // fill in the details about the properties obtained from categories
        for (Category category : categories) {
            for (Property property : category.getProperties()) {
                row = getRow(propertyDetailsTab, ++rowIndex);

                columnIndex = 1;
                row.createCell(columnIndex).setCellValue(property.getPreferredName());
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
                    cell.setCellValue(property.getPreferredName());
                    cell.setCellStyle(boldCellStyle);

                    for (int j = 0; j < values.size(); j++) {
                        row = getRow(valuesTab, rowIndex++);
                        row.createCell(columnIndex).setCellValue(values.get(j).getPreferredName());
                    }

                    rowIndex = 0;
                    columnIndex++;

                    // update the number of properties with specific value constraints
                    if (!propNums.containsKey(category.getPreferredName())) {
                        propNums.put(category.getPreferredName(), 1);
                    } else {
                        int count = propNums.get(category.getPreferredName());
                        propNums.put(category.getPreferredName(), ++count);
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
        // make this sheet hidden
        template.setSheetHidden(template.getSheetIndex(metadataTab),true);
    }

    private void populateSourceList(Sheet sourceList){
        // values for boolean
        sourceList.createRow(0).createCell(0).setCellValue(TemplateConfig.TEMPLATE_BOOLEAN_LIST);
        sourceList.createRow(1).createCell(0).setCellValue("TRUE");
        sourceList.createRow(2).createCell(0).setCellValue("FALSE");

        Name namedCell = template.createName();
        namedCell.setNameName(TemplateConfig.TEMPLATE_BOOLEAN_LIST);
        namedCell.setRefersToFormula(TemplateConfig.TEMPLATE_BOOLEAN_REFERENCE);

        // values for incoterms
        sourceList.getRow(0).createCell(1).setCellValue(TemplateConfig.TEMPLATE_INCOTERMS_LIST);
        sourceList.getRow(1).createCell(1).setCellValue("CIF (Cost,Insurance and Freight)");
        sourceList.getRow(2).createCell(1).setCellValue("CIP (Carriage and Insurance Paid to)");
        sourceList.createRow(3).createCell(1).setCellValue("CFR (Cost and Freight)");
        sourceList.createRow(4).createCell(1).setCellValue("CPT (Carriage paid to)");
        sourceList.createRow(5).createCell(1).setCellValue("DAT (Delivered at Terminal)");
        sourceList.createRow(6).createCell(1).setCellValue("DAP (Delivered at Place)");
        sourceList.createRow(7).createCell(1).setCellValue("DDP (Delivery Duty Paid)");
        sourceList.createRow(8).createCell(1).setCellValue("EXW (Ex Works)");
        sourceList.createRow(9).createCell(1).setCellValue("FAS (Free Alongside Ship)");
        sourceList.createRow(10).createCell(1).setCellValue("FCA (Free Carrier)");
        sourceList.createRow(11).createCell(1).setCellValue("FOB (Free on Board)");

        namedCell = template.createName();
        namedCell.setNameName(TemplateConfig.TEMPLATE_INCOTERMS_LIST);
        namedCell.setRefersToFormula(TemplateConfig.TEMPLATE_INCOTERMS_REFERENCE);

        // values for currency
        sourceList.getRow(0).createCell(2).setCellValue(TemplateConfig.TEMPLATE_CURRENCY_LIST);
        sourceList.getRow(1).createCell(2).setCellValue("EUR");
        sourceList.getRow(2).createCell(2).setCellValue("USD");
        sourceList.getRow(3).createCell(2).setCellValue("SEK");

        namedCell = template.createName();
        namedCell.setNameName(TemplateConfig.TEMPLATE_CURRENCY_LIST);
        namedCell.setRefersToFormula(TemplateConfig.TEMPLATE_CURRENCY_REFERENCE);

        // values for dimensions
        sourceList.getRow(0).createCell(3).setCellValue(TemplateConfig.TEMPLATE_DIMENSION_LIST);
        sourceList.getRow(1).createCell(3).setCellValue("mm");
        sourceList.getRow(2).createCell(3).setCellValue("cm");
        sourceList.getRow(3).createCell(3).setCellValue("m");

        namedCell = template.createName();
        namedCell.setNameName(TemplateConfig.TEMPLATE_DIMENSION_LIST);
        namedCell.setRefersToFormula(TemplateConfig.TEMPLATE_DIMENSION_REFERENCE);

        // values for Warranty Validity Period
        sourceList.getRow(0).createCell(4).setCellValue(TemplateConfig.TEMPLATE_WARRANTY_VALIDITY_LIST);
        sourceList.getRow(1).createCell(4).setCellValue("year");
        sourceList.getRow(2).createCell(4).setCellValue("month");

        namedCell = template.createName();
        namedCell.setNameName(TemplateConfig.TEMPLATE_WARRANTY_VALIDITY_LIST);
        namedCell.setRefersToFormula(TemplateConfig.TEMPLATE_WARRANTY_REFERENCE);

        // values for Estimated Delivery Period
        sourceList.getRow(0).createCell(5).setCellValue(TemplateConfig.TEMPLATE_TRADING_DELIVERY_ESTIMATED_DELIVERY_PERIOD);
        sourceList.getRow(1).createCell(5).setCellValue("working days");
        sourceList.getRow(2).createCell(5).setCellValue("days");
        sourceList.getRow(3).createCell(5).setCellValue("weeks");

        namedCell = template.createName();
        namedCell.setNameName(TemplateConfig.TEMPLATE_DELIVERY_PERIOD_LIST);
        namedCell.setRefersToFormula(TemplateConfig.TEMPLATE_DELIVERY_PERIOD_REFERENCE);

        // set sheet hidden
        template.setSheetHidden(template.getSheetIndex(TemplateConfig.TEMPLATE_TAB_SOURCE_LIST),true);
    }

    private boolean checkMandatory(Property property, Cell cell) {
        if (property.getPreferredName().contentEquals(TemplateConfig.TEMPLATE_PRODUCT_PROPERTIES_MANUFACTURER_ITEM_IDENTIFICATION) ||
                property.getPreferredName().contentEquals(TemplateConfig.TEMPLATE_PRODUCT_PROPERTIES_NAME)) {
            return true;
        }
        return false;
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
        mandatoryCellStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        mandatoryCellStyle.setFillForegroundColor(IndexedColors.RED.getIndex());

        boldCellStyle = template.createCellStyle();
        font = template.createFont();
        font.setBold(true);
        boldCellStyle.setFont(font);
        boldCellStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        boldCellStyle.setFillForegroundColor(IndexedColors.GREY_40_PERCENT.getIndex());

        wordWrapStyle = template.createCellStyle();
        wordWrapStyle.setWrapText(true);

        readOnlyStyle = template.createCellStyle();
        readOnlyStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        readOnlyStyle.setFillForegroundColor(IndexedColors.GREY_40_PERCENT.getIndex());

        editableStyle = template.createCellStyle();
        editableStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        editableStyle.setFillForegroundColor(IndexedColors.BRIGHT_GREEN.getIndex());

        tabCellStyle = template.createCellStyle();
        tabCellStyle.setVerticalAlignment(VerticalAlignment.CENTER);
        tabCellStyle.setFont(font);
        tabCellStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        tabCellStyle.setFillForegroundColor(IndexedColors.GREY_40_PERCENT.getIndex());
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
