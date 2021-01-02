package eu.nimble.service.catalogue.template;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import eu.nimble.service.catalogue.exception.InvalidCategoryException;
import eu.nimble.service.catalogue.exception.NimbleExceptionMessageCode;
import eu.nimble.service.catalogue.model.category.Category;
import eu.nimble.service.catalogue.model.category.Property;
import eu.nimble.service.catalogue.model.category.Value;
import eu.nimble.service.catalogue.exception.TemplateParseException;
import eu.nimble.service.catalogue.util.SpringBridge;
import eu.nimble.service.model.solr.owl.CodedType;
import eu.nimble.service.model.solr.owl.PropertyType;
import eu.nimble.service.model.ubl.commonaggregatecomponents.*;
import eu.nimble.service.model.ubl.commonbasiccomponents.QuantityType;
import eu.nimble.service.model.ubl.commonbasiccomponents.TextType;
import eu.nimble.utility.JsonSerializationUtility;
import feign.Response;
import org.apache.commons.io.IOUtils;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.ss.util.CellRangeAddressList;
import org.apache.poi.xssf.usermodel.*;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.util.*;
import java.util.stream.Collectors;

import static eu.nimble.service.catalogue.template.TemplateConfig.*;
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
    private String defaultLanguage = "en";
    private Sheet sourceList;
    // fields to be used to add source lists to the sheet dynamically
    private int sourceListCellIndex = 0;
    private char sourceListColumn = 'H';

    public TemplateGenerator() {
        template = new XSSFWorkbook();
        createStyles();
    }

    public Workbook generateTemplateForCategory(List<Category> categories,String templateLanguage,String bearerToken) {
        return generateTemplateForCategory(categories,templateLanguage,false,bearerToken);
    }

    /**
     * @param usePropertyId whether we should use property name or id while generating the excel
     * */
    public Workbook generateTemplateForCategory(List<Category> categories,String templateLanguage, Boolean usePropertyId, String bearerToken) {
        // set defaultLanguage
        defaultLanguage = templateLanguage;
        Sheet infoTab = template.createSheet(SpringBridge.getInstance().getMessage(TemplateTextCode.TEMPLATE_TAB_INFORMATION.toString(), defaultLanguage));
        Sheet propertiesTab = template.createSheet(SpringBridge.getInstance().getMessage(TemplateTextCode.TEMPLATE_TAB_PRODUCT_PROPERTIES.toString(), defaultLanguage));
        Sheet propertiesExampleTab = template.createSheet(SpringBridge.getInstance().getMessage(TemplateTextCode.TEMPLATE_TAB_PRODUCT_PROPERTIES_EXAMPLE.toString(), defaultLanguage));
        Sheet tradingDeliveryTermsTab = template.createSheet(SpringBridge.getInstance().getMessage(TemplateTextCode.TEMPLATE_TAB_TRADING_DELIVERY_TERMS.toString(), defaultLanguage));
        Sheet tradingDeliveryTermsTabExample = template.createSheet(SpringBridge.getInstance().getMessage(TemplateTextCode.TEMPLATE_TAB_TRADING_DELIVERY_TERMS_EXAMPLE.toString(), defaultLanguage));
        Sheet propertyDetailsTab = template.createSheet(SpringBridge.getInstance().getMessage(TemplateTextCode.TEMPLATE_TAB_PROPERTY_DETAILS.toString(), defaultLanguage));
        Sheet valuesTab = template.createSheet(SpringBridge.getInstance().getMessage(TemplateTextCode.TEMPLATE_TAB_ALLOWED_VALUES_FOR_PROPERTIES.toString(), defaultLanguage));
        Sheet metadataTab = template.createSheet(TemplateConfig.TEMPLATE_TAB_METADATA);
        sourceList = template.createSheet(SpringBridge.getInstance().getMessage(TemplateTextCode.TEMPLATE_TAB_SOURCE_LIST.toString(), defaultLanguage));

        populateSourceList();
        populateInfoTab(infoTab);
        populateProductPropertiesTab(categories, propertiesTab, usePropertyId,bearerToken);
        populateProductPropertiesExampleTab(categories,propertiesExampleTab);
        populateTradingDeliveryTermsTab(tradingDeliveryTermsTab);
        populateTradingDeliveryTermsExampleTab(tradingDeliveryTermsTabExample);
        populatePropertyDetailsTab(categories, propertyDetailsTab);
        populateAllowedValuesTab(categories, valuesTab);
        populateMetadataTab(categories, metadataTab);

        return template;
    }

    public Workbook generateTemplateForCatalogueLines(List<CatalogueLineType> catalogueLines, List<Category> categories, String languageId,String bearerToken) throws InvalidCategoryException {
        // use property id while generating the excel
        Workbook template = generateTemplateForCategory(categories,languageId, true,bearerToken);
        fillProductPropertiesTab(template.getSheet(TemplateGenerator.getSheetName(TemplateTextCode.TEMPLATE_TAB_PRODUCT_PROPERTIES.toString(), defaultLanguage)),catalogueLines);
        fillTradingDeliveryTermsTab(template.getSheet(TemplateGenerator.getSheetName(TemplateTextCode.TEMPLATE_TAB_TRADING_DELIVERY_TERMS.toString(), defaultLanguage)),catalogueLines);
        fillCustomProperties(template.getSheet(TemplateGenerator.getSheetName(TemplateTextCode.TEMPLATE_TAB_PRODUCT_PROPERTIES.toString(), defaultLanguage)),catalogueLines,categories);
        fillCategoryProperties(template.getSheet(TemplateGenerator.getSheetName(TemplateTextCode.TEMPLATE_TAB_PRODUCT_PROPERTIES.toString(), defaultLanguage)),catalogueLines,categories);
        // we need to replace property ids with preferred names
        replaceCategoryPropertyIdsWithNames(template.getSheet(TemplateGenerator.getSheetName(TemplateTextCode.TEMPLATE_TAB_PRODUCT_PROPERTIES.toString(), defaultLanguage)),categories);
        return template;
    }

    private void fillProductPropertiesTab(Sheet productPropertiesTab,List<CatalogueLineType> catalogueLines){
        // 5th row is the first editable row
        int rowIndex = 4;

        for(CatalogueLineType catalogueLine : catalogueLines){
            Row row;
            // we have already created a row for rowIndex = 4, use it
            if(rowIndex == 4){
                row = productPropertiesTab.getRow(rowIndex);
            }
            else{
                row = productPropertiesTab.createRow(rowIndex);
            }
            // fill fixed properties

            // manufacturer item identification
            Cell cell = row.createCell(1);
            cell.setCellValue(catalogueLine.getID());
            if(rowIndex == 4){
                cell.setCellStyle(mandatoryCellStyle);
            }

            // name
            cell = row.createCell(2);
            cell.setCellValue(this.getMultiValueText(catalogueLine.getGoodsItem().getItem().getName()));
            if(rowIndex == 4){
                cell.setCellStyle(mandatoryCellStyle);
            }

            // description
            cell = row.createCell(3);
            cell.setCellValue(this.getMultiValueText(catalogueLine.getGoodsItem().getItem().getDescription()));
            if(rowIndex == 4){
                cell.setCellStyle(editableStyle);
            }

            // dimensions
            for(DimensionType dimensionType : catalogueLine.getGoodsItem().getItem().getDimension()){
                int columnIndex = 10;
                if(dimensionType.getAttributeID().contentEquals(TemplateConfig.TEMPLATE_PRODUCT_PROPERTIES_WIDTH)){
                    columnIndex = 4;
                }
                else if(dimensionType.getAttributeID().contentEquals(TemplateConfig.TEMPLATE_PRODUCT_PROPERTIES_LENGTH)){
                    columnIndex = 6;
                }
                else if(dimensionType.getAttributeID().contentEquals(TEMPLATE_PRODUCT_PROPERTIES_HEIGHT)){
                    columnIndex = 8;
                }
                cell = row.createCell(columnIndex);
                // get unit cell
                Cell unitCell = row.createCell(columnIndex+1);
                // set value and unit
                unitCell.setCellValue(dimensionType.getMeasure().getUnitCode());
                unitCell.setCellStyle(editableStyle);
                if(dimensionType.getMeasure().getValue() != null){
                    cell.setCellValue(new DecimalFormat(".00").format(dimensionType.getMeasure().getValue()));
                }
                if(rowIndex == 4){
                    cell.setCellStyle(editableStyle);
                }
            }
            rowIndex++;
        }
    }

    private void fillTradingDeliveryTermsTab(Sheet termsTab,List<CatalogueLineType> catalogueLines){
        // 5th row is the first editable row
        int rowIndex = 4;

        for(CatalogueLineType catalogueLine : catalogueLines){
            Row row;
            // we have already created a row for rowIndex = 4, use it
            if(rowIndex == 4){
                row = termsTab.getRow(rowIndex);
            }
            else{
                row = termsTab.createRow(rowIndex);
            }

            int columnIndex = 1;
            // fill fixed properties

            // manufacturer item identification
            Cell cell = row.createCell(columnIndex++);
            cell.setCellValue(catalogueLine.getID());
            if(rowIndex == 4){
                cell.setCellStyle(mandatoryCellStyle);
            }

            // price amount
            cell = row.createCell(columnIndex++);
            // get unit cell
            Cell unitCell = row.createCell(columnIndex++);
            // set value and unit
            unitCell.setCellValue(catalogueLine.getRequiredItemLocationQuantity().getPrice().getPriceAmount().getCurrencyID());
            if(catalogueLine.getRequiredItemLocationQuantity().getPrice().getPriceAmount().getValue() != null){
                cell.setCellValue(new DecimalFormat(".00").format(catalogueLine.getRequiredItemLocationQuantity().getPrice().getPriceAmount().getValue()));
            }
            if(rowIndex == 4){
                cell.setCellStyle(editableStyle);
                unitCell.setCellStyle(editableStyle);
            }

            // price base quantity
            cell = row.createCell(columnIndex++);
            // get unit cell
            unitCell = row.createCell(columnIndex++);
            // set value and unit
            unitCell.setCellValue(catalogueLine.getRequiredItemLocationQuantity().getPrice().getBaseQuantity().getUnitCode());
            if(catalogueLine.getRequiredItemLocationQuantity().getPrice().getBaseQuantity().getValue() != null){
                cell.setCellValue(new DecimalFormat(".00").format(catalogueLine.getRequiredItemLocationQuantity().getPrice().getBaseQuantity().getValue()));
            }
            if(rowIndex == 4){
                unitCell.setCellStyle(editableStyle);
                cell.setCellStyle(editableStyle);
            }

            // minimum order quantity
            cell = row.createCell(columnIndex++);
            // get unit cell
            unitCell = row.createCell(columnIndex++);
            // set value and unit
            if(catalogueLine.getMinimumOrderQuantity() != null){
                unitCell.setCellValue(catalogueLine.getMinimumOrderQuantity().getUnitCode());
                if(catalogueLine.getMinimumOrderQuantity().getValue() != null){
                    cell.setCellValue(new DecimalFormat(".00").format(catalogueLine.getMinimumOrderQuantity().getValue()));
                }
            }
            if(rowIndex == 4){
                cell.setCellStyle(editableStyle);
                unitCell.setCellStyle(editableStyle);
            }

            // free sample
            cell = row.createCell(columnIndex++);
            String freeSample = catalogueLine.isFreeOfChargeIndicator() == null ? "": catalogueLine.isFreeOfChargeIndicator() ? "TRUE":"FALSE";
            cell.setCellValue(freeSample);
            if(rowIndex == 4){
                cell.setCellStyle(editableStyle);
            }

            // warranty validity period
            cell = row.createCell(columnIndex++);
            // get unit cell
            unitCell = row.createCell(columnIndex++);
            // set value and unit
            unitCell.setCellValue(catalogueLine.getWarrantyValidityPeriod().getDurationMeasure().getUnitCode());
            if(catalogueLine.getWarrantyValidityPeriod().getDurationMeasure().getValue() != null){
                cell.setCellValue(new DecimalFormat(".00").format(catalogueLine.getWarrantyValidityPeriod().getDurationMeasure().getValue()));
            }
            if(rowIndex == 4){
                unitCell.setCellStyle(editableStyle);
                cell.setCellStyle(editableStyle);
            }

            // warranty information
            cell = row.createCell(columnIndex++);
            cell.setCellValue(getMultiValueRepresentation(catalogueLine.getWarrantyInformation(),TemplateConfig.TEMPLATE_DATA_TYPE_STRING));
            if(rowIndex == 4){
                cell.setCellStyle(editableStyle);
            }

            // incoterms
            cell = row.createCell(columnIndex++);
            cell.setCellValue(catalogueLine.getGoodsItem().getDeliveryTerms().getIncoterms());
            if(rowIndex == 4){
                cell.setCellStyle(editableStyle);
            }

            // special terms
            // although special terms are multilingual, we assume that they are not.
            List<String> values = new ArrayList<>();
            for(TextType textType:catalogueLine.getGoodsItem().getDeliveryTerms().getSpecialTerms()){
                values.add(textType.getValue());
            }
            cell = row.createCell(columnIndex++);
            cell.setCellValue(getMultiValueRepresentation(values,TemplateConfig.TEMPLATE_DATA_TYPE_TEXT));
            if(rowIndex == 4){
                cell.setCellStyle(editableStyle);
            }
            // estimated delivery period
            cell = row.createCell(columnIndex++);
            // get unit cell
            unitCell = row.createCell(columnIndex++);
            // set value and unit
            unitCell.setCellValue(catalogueLine.getGoodsItem().getDeliveryTerms().getEstimatedDeliveryPeriod().getDurationMeasure().getUnitCode());
            if(catalogueLine.getGoodsItem().getDeliveryTerms().getEstimatedDeliveryPeriod().getDurationMeasure().getValue() != null){
                cell.setCellValue(new DecimalFormat(".00").format(catalogueLine.getGoodsItem().getDeliveryTerms().getEstimatedDeliveryPeriod().getDurationMeasure().getValue()));
            }
            if(rowIndex == 4){
                unitCell.setCellStyle(editableStyle);
                cell.setCellStyle(editableStyle);
            }
            // transport mode
            cell = row.createCell(columnIndex++);
            if(catalogueLine.getGoodsItem().getDeliveryTerms().getTransportModeCode() != null){
                cell.setCellValue(catalogueLine.getGoodsItem().getDeliveryTerms().getTransportModeCode().getValue());
            }
            if(rowIndex == 4){
                cell.setCellStyle(editableStyle);
            }
            // packaging type
            cell = row.createCell(columnIndex++);
            cell.setCellValue(catalogueLine.getGoodsItem().getContainingPackage().getPackagingTypeCode().getValue());
            if(rowIndex == 4){
                cell.setCellStyle(editableStyle);
            }
            // package quantity
            cell = row.createCell(columnIndex++);
            // get unit cell
            unitCell = row.createCell(columnIndex++);
            // set value and unit
            unitCell.setCellValue(catalogueLine.getGoodsItem().getContainingPackage().getQuantity().getUnitCode());
            if(catalogueLine.getGoodsItem().getContainingPackage().getQuantity().getValue() != null){
                cell.setCellValue(new DecimalFormat(".00").format(catalogueLine.getGoodsItem().getContainingPackage().getQuantity().getValue()));
            }
            if(rowIndex == 4){
                unitCell.setCellStyle(editableStyle);
                cell.setCellStyle(editableStyle);
            }

            rowIndex++;
        }
    }

    private void fillCustomProperties(Sheet productPropertiesTab,List<CatalogueLineType> catalogueLines,List<Category> categories){
        // find the offset for the custom properties
        int totalCategoryPropertyNumber = 0;
        for (Category category : categories) {
            if(category.getProperties() != null){
                for(Property property : category.getProperties()){
                    totalCategoryPropertyNumber++;
                    if(property.getDataType().contentEquals(TemplateConfig.TEMPLATE_DATA_TYPE_QUANTITY)){
                        totalCategoryPropertyNumber++;
                    }
                }
            }
        }
        int customPropertyColumnIndex = 4 + TemplateConfig.getFixedPropertiesForProductPropertyTab(defaultLanguage).size() + totalCategoryPropertyNumber;

        int rowIndex = 4;
        for(CatalogueLineType catalogueLine:catalogueLines){
            int propertyColumnIndex = customPropertyColumnIndex;
            for(ItemPropertyType itemProperty:catalogueLine.getGoodsItem().getItem().getAdditionalItemProperty()){
                // consider only custom properties
                if(itemProperty.getItemClassificationCode().getListID().contentEquals("Custom")){
                    // set the name of property
                    Cell cell = productPropertiesTab.getRow(1).createCell(propertyColumnIndex);
                    cell.setCellValue(getMultiValueText(itemProperty.getName()));
                    // set data type of property
                    String dataType = normalizeDataTypeForTemplate(itemProperty.getValueQualifier());
                    cell = productPropertiesTab.getRow(2).createCell(propertyColumnIndex);
                    cell.setCellValue(dataType);
                    // set the value of property
                    if(dataType.contentEquals(TemplateConfig.TEMPLATE_DATA_TYPE_QUANTITY)){
                        if(itemProperty.getValueQuantity().size() > 0){
                            // set the value
                            cell = productPropertiesTab.getRow(rowIndex).createCell(propertyColumnIndex);
                            cell.setCellValue(getMultiValueRepresentation(itemProperty.getValueQuantity(),TemplateConfig.TEMPLATE_DATA_TYPE_QUANTITY));
                            // we need to set unit as well
                            Cell unitCell = productPropertiesTab.getRow(rowIndex).createCell(++propertyColumnIndex);
                            unitCell.setCellValue(itemProperty.getValueQuantity().get(0).getUnitCode());
                            productPropertiesTab.getRow(2).createCell(propertyColumnIndex).setCellValue(TemplateConfig.TEMPLATE_QUANTITY_UNIT);
                        }
                    } else if(dataType.contentEquals(TemplateConfig.TEMPLATE_DATA_TYPE_MULTILINGUAL_TEXT)){
                        cell = productPropertiesTab.getRow(rowIndex).createCell(propertyColumnIndex);
                        cell.setCellValue(getMultiValueText(itemProperty.getValue()));
                    } else if(dataType.contentEquals(TemplateConfig.TEMPLATE_DATA_TYPE_BOOLEAN)){
                        cell = productPropertiesTab.getRow(rowIndex).createCell(propertyColumnIndex);
                        cell.setCellValue(getMultiValueText(itemProperty.getValue(),false));
                    } else if(dataType.contentEquals(TemplateConfig.TEMPLATE_DATA_TYPE_NUMBER)){
                        cell = productPropertiesTab.getRow(rowIndex).createCell(propertyColumnIndex);
                        cell.setCellValue(getMultiValueRepresentation(itemProperty.getValueDecimal(),TemplateConfig.TEMPLATE_DATA_TYPE_NUMBER));
                    }
                    propertyColumnIndex++;
                }
            }
            rowIndex++;
        }
    }

    private void fillCategoryProperties(Sheet productPropertiesTab,List<CatalogueLineType> catalogueLines,List<Category> categories) throws InvalidCategoryException {

        // create category - parent category uris map
        Map<Category,List<String>> parentCategoryUriMap = new HashMap<>();
        for(Category category:categories){
            if(category.getCategoryUri() != null){
                List<Category> parentCategories = SpringBridge.getInstance().getIndexCategoryService().getParentCategories(category.getTaxonomyId(),category.getCategoryUri());
                List<String> uriList = new ArrayList<>();
                parentCategories.forEach(category1 -> uriList.add(category1.getCategoryUri()));
                parentCategoryUriMap.put(category,uriList);
            }
        }
        
        int rowIndex = 4;
        for(CatalogueLineType catalogueLine:catalogueLines){
            for(ItemPropertyType itemProperty:catalogueLine.getGoodsItem().getItem().getAdditionalItemProperty()){
                // consider category properties
                if(!itemProperty.getItemClassificationCode().getListID().contentEquals("Custom")){
                    // get the category names
                    List<TextType> categoryNames = getNamesOfCategory(parentCategoryUriMap,itemProperty.getItemClassificationCode().getURI());
                    // get column index
                    Integer columnIndex = findCellIndexForProperty(productPropertiesTab,itemProperty.getID(),categoryNames);
                    if(columnIndex == null){
                        continue;
                    }
                    String dataType = normalizeDataTypeForTemplate(itemProperty.getValueQualifier());
                    // set the value of property
                    if(dataType.contentEquals(TemplateConfig.TEMPLATE_DATA_TYPE_QUANTITY)){
                        if(itemProperty.getValueQuantity().size() > 0){
                            // set the value
                            Cell cell = productPropertiesTab.getRow(rowIndex).createCell(columnIndex);
                            cell.setCellValue(getMultiValueRepresentation(itemProperty.getValueQuantity(),TemplateConfig.TEMPLATE_DATA_TYPE_QUANTITY));
                            // we need to set unit as well
                            Cell unitCell = productPropertiesTab.getRow(rowIndex).createCell(++columnIndex);
                            unitCell.setCellValue(itemProperty.getValueQuantity().get(0).getUnitCode());
                            if(rowIndex == 4){
                                cell.setCellStyle(editableStyle);
                                unitCell.setCellStyle(editableStyle);
                            }
                        }
                    } else if(dataType.contentEquals(TemplateConfig.TEMPLATE_DATA_TYPE_MULTILINGUAL_TEXT)){
                        Cell cell = productPropertiesTab.getRow(rowIndex).createCell(columnIndex);
                        cell.setCellValue(getMultiValueText(itemProperty.getValue()));
                        if(rowIndex == 4){
                            cell.setCellStyle(editableStyle);
                        }
                    } else if(dataType.contentEquals(TemplateConfig.TEMPLATE_DATA_TYPE_BOOLEAN)){
                        Cell cell = productPropertiesTab.getRow(rowIndex).createCell(columnIndex);
                        cell.setCellValue(getMultiValueText(itemProperty.getValue(),false));
                        if(rowIndex == 4){
                            cell.setCellStyle(editableStyle);
                        }
                    } else if(dataType.contentEquals(TemplateConfig.TEMPLATE_DATA_TYPE_NUMBER)){
                        Cell cell = productPropertiesTab.getRow(rowIndex).createCell(columnIndex);
                        cell.setCellValue(getMultiValueRepresentation(itemProperty.getValueDecimal(),TemplateConfig.TEMPLATE_DATA_TYPE_NUMBER));
                        if(rowIndex == 4){
                            cell.setCellStyle(editableStyle);
                        }
                    }
                }
            }
            rowIndex++;
        }
    }

    private void populateInfoTab(Sheet infoTab) {
        int rowIndex = 0;
        // info tab info
        Row row = infoTab.createRow(rowIndex);
        Cell cell = row.createCell(0);
        cell.setCellValue(SpringBridge.getInstance().getMessage(TemplateTextCode.TEMPLATE_INFO_HOW_TO_FILL.toString(), defaultLanguage));
        cell.setCellStyle(headerCellStyle);
        row = infoTab.createRow(++rowIndex);
        row.createCell(0).setCellValue(SpringBridge.getInstance().getMessage(TemplateTextCode.TEMPLATE_INFO_THIS_TAB_PROVIDES.toString(), defaultLanguage));
        rowIndex++;

        // generic information
        row = infoTab.createRow(++rowIndex);
        cell = row.createCell(0);
        cell.setCellValue(SpringBridge.getInstance().getMessage(TemplateTextCode.TEMPLATE_INFO_GENERIC_INFORMATION.toString(), defaultLanguage));
        cell.setCellStyle(headerCellStyle);
        row = infoTab.createRow(++rowIndex);
        row.createCell(0).setCellValue(SpringBridge.getInstance().getMessage(TemplateTextCode.TEMPLATE_INFO_YOU_SHOULD_EDIT.toString(), defaultLanguage));
        row = infoTab.createRow(++rowIndex);
        cell = row.createCell(0);
        cell.setCellValue(SpringBridge.getInstance().getMessage(TemplateTextCode.TEMPLATE_INFO_MANDATORY_INFORMATION.toString(), defaultLanguage));
        cell.setCellStyle(mandatoryCellStyle);
        row = infoTab.createRow(++rowIndex);
        cell = row.createCell(0);
        cell.setCellValue(SpringBridge.getInstance().getMessage(TemplateTextCode.TEMPLATE_INFO_NOT_EDITABLE.toString(), defaultLanguage));
        cell.setCellStyle(readOnlyStyle);
        row = infoTab.createRow(++rowIndex);
        cell = row.createCell(0);
        cell.setCellValue(SpringBridge.getInstance().getMessage(TemplateTextCode.TEMPLATE_INFO_YOU_CAN_EDIT.toString(), defaultLanguage));
        cell.setCellStyle(editableStyle);
        row = infoTab.createRow(++rowIndex);
        row.createCell(0).setCellValue(SpringBridge.getInstance().getMessage(TemplateTextCode.TEMPLATE_INFO_DATA_FIELDS.toString(), defaultLanguage));
        row = infoTab.createRow(++rowIndex);
        row.createCell(0).setCellValue("        "+SpringBridge.getInstance().getMessage(TemplateTextCode.TEMPLATE_INFO_IN_THIS_WAY.toString(), defaultLanguage));
        row = infoTab.createRow(++rowIndex);
        row.createCell(0).setCellValue("                "+SpringBridge.getInstance().getMessage(TemplateTextCode.TEMPLATE_INFO_EX1.toString(), defaultLanguage));
        row = infoTab.createRow(++rowIndex);
        row.createCell(0).setCellValue("                "+SpringBridge.getInstance().getMessage(TemplateTextCode.TEMPLATE_INFO_EX2.toString(), defaultLanguage));
        row = infoTab.createRow(++rowIndex);
        row.createCell(0).setCellValue("                "+SpringBridge.getInstance().getMessage(TemplateTextCode.TEMPLATE_INFO_EX3.toString(), defaultLanguage));
        row = infoTab.createRow(++rowIndex);
        row.createCell(0).setCellValue(SpringBridge.getInstance().getMessage(TemplateTextCode.TEMPLATE_INFO_MULTILINGUALITY.toString(), defaultLanguage));
        row = infoTab.createRow(++rowIndex);
        row.createCell(0).setCellValue("       "+SpringBridge.getInstance().getMessage(TemplateTextCode.TEMPLATE_INFO_MULTILINGUALITY_FORMAT.toString(), defaultLanguage));
        row = infoTab.createRow(++rowIndex);
        row.createCell(0).setCellValue("      "+SpringBridge.getInstance().getMessage(TemplateTextCode.TEMPLATE_INFO_MULTILINGUALITY_REMAINDER.toString(), defaultLanguage));
        row = infoTab.createRow(++rowIndex);
        row.createCell(0).setCellValue("                "+SpringBridge.getInstance().getMessage(TemplateTextCode.TEMPLATE_INFO_MULTILINGUALITY_EXAMPLE.toString(), defaultLanguage));

        rowIndex++;

        // product properties tab info
        row = infoTab.createRow(++rowIndex);
        cell = row.createCell(0);
        cell.setCellValue(SpringBridge.getInstance().getMessage(TemplateTextCode.TEMPLATE_TAB_PRODUCT_PROPERTIES.toString(), defaultLanguage));
        cell.setCellStyle(headerCellStyle);
        row = infoTab.createRow(++rowIndex);
        row.createCell(0).setCellValue(SpringBridge.getInstance().getMessage(TemplateTextCode.TEMPLATE_INFO_THE_TOP_FOUR_COLUMNS.toString(), defaultLanguage));
        row = infoTab.createRow(++rowIndex);
        row.createCell(0).setCellValue(SpringBridge.getInstance().getMessage(TemplateTextCode.TEMPLATE_INFO_THE_FIRST_ROW.toString(), defaultLanguage));
        rowIndex++;
        row = infoTab.createRow(++rowIndex);
        row.createCell(0).setCellValue(SpringBridge.getInstance().getMessage(TemplateTextCode.TEMPLATE_INFO_THE_SECOND_ROW.toString(), defaultLanguage));
        row = infoTab.createRow(++rowIndex);
        row.createCell(0).setCellValue("        "+SpringBridge.getInstance().getMessage(TemplateTextCode.TEMPLATE_INFO_FIXED_PROPERTIES.toString(), defaultLanguage));
        row = infoTab.createRow(++rowIndex);
        row.createCell(0).setCellValue("        "+SpringBridge.getInstance().getMessage(TemplateTextCode.TEMPLATE_INFO_PROPERTIES_ASSOCIATED.toString(), defaultLanguage));
        row = infoTab.createRow(++rowIndex);
        row.createCell(0).setCellValue("        "+SpringBridge.getInstance().getMessage(TemplateTextCode.TEMPLATE_INFO_CUSTOM_PROPERTIES.toString(), defaultLanguage));
        row = infoTab.createRow(++rowIndex);
        cell = row.createCell(0);
        cell.setCellValue("                "+SpringBridge.getInstance().getMessage(TemplateTextCode.TEMPLATE_INFO_CUSTOM_PROPERTIES_REMAINDER.toString(), defaultLanguage));
        cell.setCellStyle(mandatoryCellStyle);
        row = infoTab.createRow(++rowIndex);
        row.createCell(0).setCellValue(SpringBridge.getInstance().getMessage(TemplateTextCode.TEMPLATE_INFO_DETAILS_OF_THE_PROPERTY.toString(), defaultLanguage));
        rowIndex++;
        row = infoTab.createRow(++rowIndex);
        row.createCell(0).setCellValue(SpringBridge.getInstance().getMessage(TemplateTextCode.TEMPLATE_INFO_THE_THIRD_ROW.toString(), defaultLanguage));
        row = infoTab.createRow(++rowIndex);
        row.createCell(0).setCellValue("        "+SpringBridge.getInstance().getMessage(TemplateTextCode.TEMPLATE_INFO_TEXT.toString(), defaultLanguage));
        row = infoTab.createRow(++rowIndex);
        row.createCell(0).setCellValue("        "+SpringBridge.getInstance().getMessage(TemplateTextCode.TEMPLATE_INFO_MULTILINGUAL_TEXT.toString(), defaultLanguage));
        row = infoTab.createRow(++rowIndex);
        row.createCell(0).setCellValue("        "+SpringBridge.getInstance().getMessage(TemplateTextCode.TEMPLATE_INFO_NUMBER.toString(), defaultLanguage));
        row = infoTab.createRow(++rowIndex);
        row.createCell(0).setCellValue("        "+SpringBridge.getInstance().getMessage(TemplateTextCode.TEMPLATE_INFO_QUANTITY.toString(), defaultLanguage));
        row = infoTab.createRow(++rowIndex);
        row.createCell(0).setCellValue("        "+SpringBridge.getInstance().getMessage(TemplateTextCode.TEMPLATE_INFO_FILE.toString(), defaultLanguage));
        row = infoTab.createRow(++rowIndex);
        row.createCell(0).setCellValue("        "+SpringBridge.getInstance().getMessage(TemplateTextCode.TEMPLATE_INFO_BOOLEAN.toString(), defaultLanguage));
        rowIndex++;

        row = infoTab.createRow(++rowIndex);
        row.createCell(0).setCellValue(SpringBridge.getInstance().getMessage(TemplateTextCode.TEMPLATE_INFO_THE_FOURTH_ROW.toString(), defaultLanguage));
        row = infoTab.createRow(++rowIndex);
        row.createCell(0).setCellValue(SpringBridge.getInstance().getMessage(TemplateTextCode.TEMPLATE_INFO_THE_FIFTH_ROW.toString(), defaultLanguage));
        rowIndex++;

        // product properties example tab info
        row = infoTab.createRow(++rowIndex);
        cell = row.createCell(0);
        cell.setCellValue(SpringBridge.getInstance().getMessage(TemplateTextCode.TEMPLATE_TAB_PRODUCT_PROPERTIES_EXAMPLE.toString(), defaultLanguage));
        cell.setCellStyle(headerCellStyle);
        row = infoTab.createRow(++rowIndex);
        row.createCell(0).setCellValue(SpringBridge.getInstance().getMessage(TemplateTextCode.TEMPLATE_INFO_PRODUCT_PROPERTIES_EXAMPLE.toString(), defaultLanguage));
        rowIndex++;

        // terms tab info
        row = infoTab.createRow(++rowIndex);
        cell = row.createCell(0);
        cell.setCellValue(SpringBridge.getInstance().getMessage(TemplateTextCode.TEMPLATE_TAB_TRADING_DELIVERY_TERMS.toString(), defaultLanguage));
        cell.setCellStyle(headerCellStyle);
        row = infoTab.createRow(++rowIndex);
        row.createCell(0).setCellValue(SpringBridge.getInstance().getMessage(TemplateTextCode.TEMPLATE_INFO_TRADING_AND_DELIVERY.toString(), defaultLanguage));
        row = infoTab.createRow(++rowIndex);
        row.createCell(0).setCellValue(SpringBridge.getInstance().getMessage(TemplateTextCode.TEMPLATE_INFO_MANUFACTURER_ITEM_IDENTIFICATION.toString(), defaultLanguage));
        row = infoTab.createRow(++rowIndex);
        row.createCell(0).setCellValue(SpringBridge.getInstance().getMessage(TemplateTextCode.TEMPLATE_INFO_NOTADDING_CUSTOM_PROPERTIES.toString(), defaultLanguage));
        rowIndex++;

        // terms tab example info
        row = infoTab.createRow(++rowIndex);
        cell = row.createCell(0);
        cell.setCellValue(SpringBridge.getInstance().getMessage(TemplateTextCode.TEMPLATE_TAB_TRADING_DELIVERY_TERMS_EXAMPLE.toString(), defaultLanguage));
        cell.setCellStyle(headerCellStyle);
        row = infoTab.createRow(++rowIndex);
        row.createCell(0).setCellValue(SpringBridge.getInstance().getMessage(TemplateTextCode.TEMPLATE_INFO_TRADING_DELIVERY_EXAMPLE.toString(), defaultLanguage));
        rowIndex++;

        // product details tab info
        row = infoTab.createRow(++rowIndex);
        cell = row.createCell(0);
        cell.setCellValue(SpringBridge.getInstance().getMessage(TemplateTextCode.TEMPLATE_TAB_PROPERTY_DETAILS.toString(), defaultLanguage));
        cell.setCellStyle(headerCellStyle);
        row = infoTab.createRow(++rowIndex);
        row.createCell(0).setCellValue(SpringBridge.getInstance().getMessage(TemplateTextCode.TEMPLATE_INFO_THIS_TAB_CONTAINS.toString(), defaultLanguage));
        rowIndex++;

        // allowed values tab info
        row = infoTab.createRow(++rowIndex);
        cell = row.createCell(0);
        cell.setCellValue(SpringBridge.getInstance().getMessage(TemplateTextCode.TEMPLATE_TAB_ALLOWED_VALUES_FOR_PROPERTIES.toString(), defaultLanguage));
        cell.setCellStyle(headerCellStyle);
        row = infoTab.createRow(++rowIndex);
        row.createCell(0).setCellValue(SpringBridge.getInstance().getMessage(TemplateTextCode.TEMPLATE_INFO_THIS_TAB_CONTAINS_VALUES.toString(), defaultLanguage));
        rowIndex++;

        // metadata tab info
        row = infoTab.createRow(++rowIndex);
        cell = row.createCell(0);
        cell.setCellValue(TemplateConfig.TEMPLATE_TAB_METADATA);
        cell.setCellStyle(headerCellStyle);
        row = infoTab.createRow(++rowIndex);
        row.createCell(0).setCellValue(SpringBridge.getInstance().getMessage(TemplateTextCode.TEMPLATE_INFO_THIS_TAB_CONTAINS_INFORMATION.toString(), defaultLanguage));

        infoTab.autoSizeColumn(0);
    }

    private void populateProductPropertiesTab(List<Category> categories, Sheet productPropertiesTab, Boolean usePropertyId,String bearerToken) {
        // make first column read only
        productPropertiesTab.setDefaultColumnStyle(0,readOnlyStyle);

        // create the top row containing the category names and ids on top of the corresponding properties
        // create the dimension tab
        Row topRow = productPropertiesTab.createRow(0);

        // make first five columns read only
        for (int i=0;i<5;i++){
            topRow.createCell(i).setCellStyle(readOnlyStyle);
        }

        Cell cell = getCellWithMissingCellPolicy(topRow, 4);
        cell.setCellValue(SpringBridge.getInstance().getMessage(TemplateTextCode.TEMPLATE_PRODUCT_PROPERTIES_DIMENSIONS.toString(), defaultLanguage));
        cell.setCellStyle(tabCellStyle);
        CellRangeAddress cra = new CellRangeAddress(0, 0, 4, 11);
        productPropertiesTab.addMergedRegion(cra);

        // create the titles for categories
        int columnOffset = TemplateConfig.getFixedPropertiesForProductPropertyTab(defaultLanguage).size() + 5;
        for (int i = 0; i < categories.size(); i++) {
            List<Property> properties = getPropertiesToBeIncludedInTemplate(categories.get(i));
            if(properties.size() > 0) {
                // get the number of properties the category have
                // for each quantity, we need to increment it by two since we need a column for the quantity unit
                int propertiesSize = getColumnCountForCategory(categories.get(i));
                int colFrom = columnOffset;
                int colTo = columnOffset + propertiesSize - 1;
                cell = getCellWithMissingCellPolicy(topRow, colFrom);
                cell.setCellValue(categories.get(i).getPreferredName(defaultLanguage));
                cell.setCellStyle(tabCellStyle);
                if (colFrom < colTo) {
                    cra = new CellRangeAddress(0, 0, colFrom, colTo);
                    productPropertiesTab.addMergedRegion(cra);
                }
                columnOffset = colTo + 1;
            }
        }

        // 2nd, 3rd and 4th rows
        // name, data type, unit label on the leftmost column
        int rowIndex = 1;
        Row secondRow = productPropertiesTab.createRow(rowIndex);
        cell = secondRow.createCell(0);
        cell.setCellValue(SpringBridge.getInstance().getMessage(TemplateTextCode.TEMPLATE_PRODUCT_PROPERTIES_PROPERTY_NAME.toString(), defaultLanguage));
        cell.setCellStyle(boldCellStyle);
        Row thirdRow = productPropertiesTab.createRow(++rowIndex);
        cell = thirdRow.createCell(0);
        cell.setCellValue(SpringBridge.getInstance().getMessage(TemplateTextCode.TEMPLATE_PRODUCT_PROPERTIES_PROPERTY_DATA_TYPE.toString(), defaultLanguage));
        cell.setCellStyle(boldCellStyle);
        Row fourthRow = productPropertiesTab.createRow(++rowIndex);
        cell = fourthRow.createCell(0);
        cell.setCellValue(SpringBridge.getInstance().getMessage(TemplateTextCode.TEMPLATE_PRODUCT_PROPERTIES_PROPERTY_UNIT.toString(), defaultLanguage));
        cell.setCellStyle(boldCellStyle);
        productPropertiesTab.createRow(++rowIndex);

        // common UBL-based properties
        columnOffset = 1;
        List<Property> properties = TemplateConfig.getFixedPropertiesForProductPropertyTab(defaultLanguage);
        for (Property property : properties) {
            cell = secondRow.createCell(columnOffset);
            cell.setCellValue(property.getPreferredName(defaultLanguage));
            cell.setCellStyle(boldCellStyle);
            if(checkMandatory(property)){
                productPropertiesTab.getRow(4).createCell(columnOffset).setCellStyle(mandatoryCellStyle);
            }
            else {
                productPropertiesTab.getRow(4).createCell(columnOffset).setCellStyle(editableStyle);
            }
            Cell thirdRowCell = thirdRow.createCell(columnOffset);
            // get data type of the property
            // if its data type is Quantity or Amount, we should add Value to its data type to have a consistent template view
            String dataType = normalizeDataTypeForTemplate(property);
            // for some cases, we need to use TEXT data type instead of MULTILINGUAL_TEXT
            if(property.getPreferredName(defaultLanguage).contentEquals(SpringBridge.getInstance().getMessage(TemplateTextCode.TEMPLATE_PRODUCT_PROPERTIES_MANUFACTURER_ITEM_IDENTIFICATION.toString(), defaultLanguage))){
                thirdRowCell.setCellValue(TemplateConfig.TEMPLATE_DATA_TYPE_TEXT);
            }
            else{
                thirdRowCell.setCellValue(dataType.contentEquals(TemplateConfig.TEMPLATE_DATA_TYPE_QUANTITY) ? TemplateConfig.TEMPLATE_QUANTITY_VALUE
                        : dataType.contentEquals(TEMPLATE_DATA_TYPE_AMOUNT) ? TEMPLATE_DATA_TYPE_AMOUNT_VALUE
                        : dataType);
            }
            // make thirdRow read only
            thirdRowCell.setCellStyle(readOnlyStyle);

            fourthRow.createCell(columnOffset).setCellStyle(readOnlyStyle);
            if (property.getDataType().equals("BOOLEAN")){
                CellRangeAddressList cellRangeAddressList = new CellRangeAddressList(4,4,columnOffset,columnOffset);
                DataValidationHelper dataValidationHelper = productPropertiesTab.getDataValidationHelper();
                DataValidationConstraint dataValidationConstraint = dataValidationHelper.createFormulaListConstraint(SpringBridge.getInstance().getMessage(TemplateTextCode.TEMPLATE_BOOLEAN_LIST.toString(), defaultLanguage));
                DataValidation dataValidation  = dataValidationHelper.createValidation(dataValidationConstraint, cellRangeAddressList);
                dataValidation.setSuppressDropDownArrow(true);
                // error box
                dataValidation.setShowErrorBox(true);
                dataValidation.createErrorBox(SpringBridge.getInstance().getMessage(TemplateTextCode.TEMPLATE_INVALID_INPUT.toString(), defaultLanguage),SpringBridge.getInstance().getMessage(TemplateTextCode.TEMPLATE_INVALID_INPUT_EXPLANATION.toString(), defaultLanguage));
                // empty cell
                dataValidation.setEmptyCellAllowed(true);
                productPropertiesTab.addValidationData(dataValidation);
            }

            if(property.getPreferredName(defaultLanguage).equals(SpringBridge.getInstance().getMessage(TemplateTextCode.TEMPLATE_PRODUCT_PROPERTIES_WIDTH.toString(), defaultLanguage) ) || property.getPreferredName(defaultLanguage).equals(SpringBridge.getInstance().getMessage(TemplateTextCode.TEMPLATE_PRODUCT_PROPERTIES_LENGTH.toString(), defaultLanguage) ) || property.getPreferredName(defaultLanguage).equals(SpringBridge.getInstance().getMessage(TemplateTextCode.TEMPLATE_PRODUCT_PROPERTIES_HEIGHT.toString(), defaultLanguage) ) || property.getPreferredName(defaultLanguage).equals(SpringBridge.getInstance().getMessage(TemplateTextCode.TEMPLATE_PRODUCT_PROPERTIES_WEIGHT.toString(), defaultLanguage) )){
                // quantity unit
                cell = secondRow.createCell(++columnOffset);
                cell.setCellStyle(readOnlyStyle);
                cell = thirdRow.createCell(columnOffset);
                cell.setCellValue(TemplateConfig.TEMPLATE_QUANTITY_UNIT);
                cell.setCellStyle(readOnlyStyle);
                cell = fourthRow.createCell(columnOffset);
                cell.setCellStyle(readOnlyStyle);
                productPropertiesTab.getRow(4).createCell(columnOffset).setCellStyle(editableStyle);

                CellRangeAddressList cellRangeAddressList = new CellRangeAddressList(4,4,columnOffset,columnOffset);
                DataValidationHelper dataValidationHelper = productPropertiesTab.getDataValidationHelper();
                String constraints = property.getPreferredName(defaultLanguage).equals(SpringBridge.getInstance().getMessage(TemplateTextCode.TEMPLATE_PRODUCT_PROPERTIES_WEIGHT.toString(), defaultLanguage)) ? SpringBridge.getInstance().getMessage(TemplateTextCode.TEMPLATE_WEIGHT_UNIT_LIST.toString(), defaultLanguage): SpringBridge.getInstance().getMessage(TemplateTextCode.TEMPLATE_DIMENSION_LIST.toString(), defaultLanguage);
                DataValidationConstraint dataValidationConstraint = dataValidationHelper.createFormulaListConstraint(constraints);
                DataValidation dataValidation  = dataValidationHelper.createValidation(dataValidationConstraint, cellRangeAddressList);
                dataValidation.setSuppressDropDownArrow(true);
                // error box
                dataValidation.setShowErrorBox(true);
                dataValidation.createErrorBox(SpringBridge.getInstance().getMessage(TemplateTextCode.TEMPLATE_INVALID_INPUT.toString(), defaultLanguage),SpringBridge.getInstance().getMessage(TemplateTextCode.TEMPLATE_INVALID_INPUT_EXPLANATION.toString(), defaultLanguage));
                // empty cell
                dataValidation.setEmptyCellAllowed(true);
                productPropertiesTab.addValidationData(dataValidation);
            }
            columnOffset++;
        }

        // columns for the properties obtained from the categories
        for (Category category : categories) {
            List<Property> categoryProperties = getPropertiesToBeIncludedInTemplate(category);
            for (Property property : categoryProperties) {
                cell = secondRow.createCell(columnOffset);
                // set the value of cell according to the usePropertyId parameter
                cell.setCellValue(usePropertyId ? property.getId(): property.getPreferredName(defaultLanguage));
                cell.setCellStyle(boldCellStyle);
                Cell thirdRowCell = thirdRow.createCell(columnOffset);
                // get data type of the property
                // if its data type is Quantity or Amount, we should add Value to its data type to have a consistent template view
                String dataType = normalizeDataTypeForTemplate(property);
                thirdRowCell.setCellValue(dataType.contentEquals(TemplateConfig.TEMPLATE_DATA_TYPE_QUANTITY) ? TemplateConfig.TEMPLATE_QUANTITY_VALUE
                        : dataType.contentEquals(TEMPLATE_DATA_TYPE_AMOUNT) ? TEMPLATE_DATA_TYPE_AMOUNT_VALUE
                        : dataType);
                // make thirdRow read only
                thirdRowCell.setCellStyle(readOnlyStyle);

                if(checkMandatory(property)){
                    productPropertiesTab.getRow(4).createCell(columnOffset).setCellStyle(mandatoryCellStyle);
                }
                else {
                    productPropertiesTab.getRow(4).createCell(columnOffset).setCellStyle(editableStyle);
                }

                fourthRow.createCell(columnOffset).setCellStyle(readOnlyStyle);
                if (property.getDataType().equals("BOOLEAN")){
                    CellRangeAddressList cellRangeAddressList = new CellRangeAddressList(4,4,columnOffset,columnOffset);
                    DataValidationHelper dataValidationHelper = productPropertiesTab.getDataValidationHelper();
                    DataValidationConstraint dataValidationConstraint = dataValidationHelper.createFormulaListConstraint(SpringBridge.getInstance().getMessage(TemplateTextCode.TEMPLATE_BOOLEAN_LIST.toString(), defaultLanguage));
                    DataValidation dataValidation  = dataValidationHelper.createValidation(dataValidationConstraint, cellRangeAddressList);
                    dataValidation.setSuppressDropDownArrow(true);
                    // error box
                    dataValidation.setShowErrorBox(true);
                    dataValidation.createErrorBox(SpringBridge.getInstance().getMessage(TemplateTextCode.TEMPLATE_INVALID_INPUT.toString(), defaultLanguage),SpringBridge.getInstance().getMessage(TemplateTextCode.TEMPLATE_INVALID_INPUT_EXPLANATION.toString(), defaultLanguage));
                    // empty cell
                    dataValidation.setEmptyCellAllowed(true);
                    productPropertiesTab.addValidationData(dataValidation);
                }

                // we assume that mandatory properties has predefined values. Therefore, create a source list for their predefined values
                if(property.getRequired() != null && property.getRequired()){
                    try{
                        // retrieve property details
                        Response response = SpringBridge.getInstance().getiIndexingServiceClient().getProperty(bearerToken, property.getId());
                        String responseBody = IOUtils.toString(response.body().asInputStream());
                        PropertyType propertyType = JsonSerializationUtility.deserializeContent(responseBody, new TypeReference<PropertyType>(){});
                        // retrieve predefined options for the property
                        String q = String.format("codedList:\"%s\"",propertyType.getCodeListId());
                        response = SpringBridge.getInstance().getiIndexingServiceClient().selectCode(bearerToken, q);

                        ObjectMapper mapper = JsonSerializationUtility.getObjectMapper();
                        JsonNode responseJson = mapper.readTree(response.body().asInputStream());
                        JsonNode result = responseJson.get("result");

                        List<CodedType> codedTypes = JsonSerializationUtility.deserializeContent(result.toString(), new TypeReference<List<CodedType>>(){});
                        // create the validation
                        CellRangeAddressList cellRangeAddressList = new CellRangeAddressList(4,4,columnOffset,columnOffset);
                        DataValidationHelper dataValidationHelper = productPropertiesTab.getDataValidationHelper();

                        String constraints = String.format("TEMPLATE_%s_LIST",propertyType.getLabel().get("en"));

                        DataValidationConstraint dataValidationConstraint = dataValidationHelper.createFormulaListConstraint(constraints);
                        DataValidation dataValidation  = dataValidationHelper.createValidation(dataValidationConstraint, cellRangeAddressList);
                        dataValidation.setSuppressDropDownArrow(true);
                        // error box
                        dataValidation.setShowErrorBox(true);
                        dataValidation.createErrorBox(SpringBridge.getInstance().getMessage(TemplateTextCode.TEMPLATE_INVALID_INPUT.toString(), defaultLanguage),SpringBridge.getInstance().getMessage(TemplateTextCode.TEMPLATE_INVALID_INPUT_EXPLANATION.toString(), defaultLanguage));
                        // empty cell
                        dataValidation.setEmptyCellAllowed(false);
                        productPropertiesTab.addValidationData(dataValidation);
                        // extend source list
                        extendSourceList(codedTypes.stream().map(codedType -> codedType.getLabel().get("en")).collect(Collectors.toList()), constraints);
                    } catch (Exception e){
                        throw new TemplateParseException(NimbleExceptionMessageCode.INTERNAL_SERVER_ERROR_GENERATE_TEMPLATE.toString(),e);
                    }
                }

                // check whether the property needs a unit
                if(property.getDataType().equals(TEMPLATE_DATA_TYPE_AMOUNT)){
                    // quantity unit
                    productPropertiesTab.getRow(0).createCell(++columnOffset).setCellStyle(readOnlyStyle);
                    cell = secondRow.createCell(columnOffset);
                    cell.setCellStyle(readOnlyStyle);
                    cell = thirdRow.createCell(columnOffset);
                    cell.setCellValue("CURRENCY");
                    cell.setCellStyle(readOnlyStyle);
                    cell = fourthRow.createCell(columnOffset);
                    cell.setCellStyle(readOnlyStyle);
                    productPropertiesTab.getRow(4).createCell(columnOffset).setCellStyle(editableStyle);
                }
                else if(property.getDataType().equals(TemplateConfig.TEMPLATE_DATA_TYPE_QUANTITY)){
                    // quantity unit
                    productPropertiesTab.getRow(0).createCell(++columnOffset).setCellStyle(readOnlyStyle);
                    cell = secondRow.createCell(columnOffset);
                    cell.setCellStyle(readOnlyStyle);
                    cell = thirdRow.createCell(columnOffset);
                    cell.setCellValue(TemplateConfig.TEMPLATE_QUANTITY_UNIT);
                    cell.setCellStyle(readOnlyStyle);
                    cell = fourthRow.createCell(columnOffset);
                    cell.setCellStyle(readOnlyStyle);
                    productPropertiesTab.getRow(4).createCell(columnOffset).setCellStyle(editableStyle);
                }
                fourthRow.getCell(columnOffset).setCellValue(property.getUnit() != null ? property.getUnit().getShortName() : "");
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

        Cell cell = getCellWithMissingCellPolicy(topRow, 4);
        cell.setCellValue(SpringBridge.getInstance().getMessage(TemplateTextCode.TEMPLATE_PRODUCT_PROPERTIES_DIMENSIONS.toString(), defaultLanguage));
        cell.setCellStyle(tabCellStyle);
        CellRangeAddress cra = new CellRangeAddress(0, 0, 4, 11);
        productPropertiesExampleTab.addMergedRegion(cra);

        // create the titles for categories
        int columnOffset = TemplateConfig.getFixedPropertiesForProductPropertyTab(defaultLanguage).size() + 5;
        for (int i = 0; i < categories.size(); i++) {
            List<Property> properties = getPropertiesToBeIncludedInTemplate(categories.get(i));
            if(properties.size() > 0) {
                // get the number of properties the category have
                // for each quantity, we need to increment it by two since we need a column for the quantity unit
                int propertiesSize = getColumnCountForCategory(categories.get(i));
                int colFrom = columnOffset;
                int colTo = columnOffset + propertiesSize - 1;
                cell = getCellWithMissingCellPolicy(topRow, colFrom);
                cell.setCellValue(categories.get(i).getPreferredName(defaultLanguage));
                cell.setCellStyle(tabCellStyle);
                if (colFrom < colTo) {
                    cra = new CellRangeAddress(0, 0, colFrom, colTo);
                    productPropertiesExampleTab.addMergedRegion(cra);
                }
                columnOffset = colTo + 1;
            }
        }

        // 2nd, 3rd and 4th rows
        // name, data type, unit label on the leftmost column
        int rowIndex = 1;
        Row secondRow = productPropertiesExampleTab.createRow(rowIndex);
        cell = secondRow.createCell(0);
        cell.setCellValue(SpringBridge.getInstance().getMessage(TemplateTextCode.TEMPLATE_PRODUCT_PROPERTIES_PROPERTY_NAME.toString(), defaultLanguage));
        cell.setCellStyle(boldCellStyle);
        Row thirdRow = productPropertiesExampleTab.createRow(++rowIndex);
        cell = thirdRow.createCell(0);
        cell.setCellValue(SpringBridge.getInstance().getMessage(TemplateTextCode.TEMPLATE_PRODUCT_PROPERTIES_PROPERTY_DATA_TYPE.toString(), defaultLanguage));
        cell.setCellStyle(boldCellStyle);
        Row fourthRow = productPropertiesExampleTab.createRow(++rowIndex);
        cell = fourthRow.createCell(0);
        cell.setCellValue(SpringBridge.getInstance().getMessage(TemplateTextCode.TEMPLATE_PRODUCT_PROPERTIES_PROPERTY_UNIT.toString(), defaultLanguage));
        cell.setCellStyle(boldCellStyle);
        productPropertiesExampleTab.createRow(++rowIndex);

        // common UBL-based properties
        columnOffset = 1;
        List<Property> properties = TemplateConfig.getFixedPropertiesForProductPropertyTab(defaultLanguage);
        for (Property property : properties) {
            cell = secondRow.createCell(columnOffset);
            cell.setCellValue(property.getPreferredName(defaultLanguage));
            cell.setCellStyle(boldCellStyle);
            if(checkMandatory(property)){
                productPropertiesExampleTab.getRow(4).createCell(columnOffset).setCellStyle(mandatoryCellStyle);
            }
            else {
                productPropertiesExampleTab.getRow(4).createCell(columnOffset).setCellStyle(editableStyle);
            }
            Cell thirdRowCell = thirdRow.createCell(columnOffset);
            // get data type of the property
            // if its data type is Quantity or Amount, we should add Value to its data type to have a consistent template view
            String dataType = normalizeDataTypeForTemplate(property);
            // for some cases, we need to use TEXT data type instead of MULTILINGUAL_TEXT
            if(property.getPreferredName(defaultLanguage).contentEquals(SpringBridge.getInstance().getMessage(TemplateTextCode.TEMPLATE_PRODUCT_PROPERTIES_MANUFACTURER_ITEM_IDENTIFICATION.toString(), defaultLanguage))){
                thirdRowCell.setCellValue(TemplateConfig.TEMPLATE_DATA_TYPE_TEXT);
            }
            else{
                thirdRowCell.setCellValue(dataType.contentEquals(TemplateConfig.TEMPLATE_DATA_TYPE_QUANTITY) ? TemplateConfig.TEMPLATE_QUANTITY_VALUE
                        : dataType.contentEquals(TEMPLATE_DATA_TYPE_AMOUNT) ? TEMPLATE_DATA_TYPE_AMOUNT_VALUE
                        : dataType);
            }
            // make thirdRow read only
            thirdRowCell.setCellStyle(readOnlyStyle);

            fourthRow.createCell(columnOffset).setCellStyle(readOnlyStyle);

            if(property.getPreferredName(defaultLanguage).equals(SpringBridge.getInstance().getMessage(TemplateTextCode.TEMPLATE_PRODUCT_PROPERTIES_MANUFACTURER_ITEM_IDENTIFICATION.toString(), defaultLanguage))){
                productPropertiesExampleTab.getRow(4).getCell(columnOffset).setCellValue("Product_id1");
                productPropertiesExampleTab.createRow(5).createCell(columnOffset).setCellValue("Product_id2");
                productPropertiesExampleTab.createRow(6).createCell(columnOffset).setCellValue("Product_id3");
            }
            else if(property.getPreferredName(defaultLanguage).equals(SpringBridge.getInstance().getMessage(TemplateTextCode.TEMPLATE_PRODUCT_PROPERTIES_NAME.toString(), defaultLanguage))){
                productPropertiesExampleTab.getRow(4).getCell(columnOffset).setCellValue(SpringBridge.getInstance().getMessage(TemplateTextCode.TEMPLATE_PLASTIC_HEAD_MALLET.toString(), defaultLanguage));
                productPropertiesExampleTab.getRow(5).createCell(columnOffset).setCellValue(SpringBridge.getInstance().getMessage(TemplateTextCode.TEMPLATE_IRON_HEAD_MALLET.toString(), defaultLanguage));
                productPropertiesExampleTab.getRow(6).createCell(columnOffset).setCellValue(SpringBridge.getInstance().getMessage(TemplateTextCode.TEMPLATE_WOOD_HEAD_MALLET.toString(), defaultLanguage));
            }
            else if(property.getPreferredName(defaultLanguage).equals(SpringBridge.getInstance().getMessage(TemplateTextCode.TEMPLATE_PRODUCT_PROPERTIES_DESCRIPTION.toString(), defaultLanguage))){
                productPropertiesExampleTab.getRow(4).getCell(columnOffset).setCellValue(SpringBridge.getInstance().getMessage(TemplateTextCode.TEMPLATE_PLASTIC_HEAD_MALLET_DESCRIPTION.toString(), defaultLanguage));
                productPropertiesExampleTab.getRow(5).createCell(columnOffset).setCellValue(SpringBridge.getInstance().getMessage(TemplateTextCode.TEMPLATE_IRON_HEAD_MALLET_DESCRIPTION.toString(), defaultLanguage));
                productPropertiesExampleTab.getRow(6).createCell(columnOffset).setCellValue(SpringBridge.getInstance().getMessage(TemplateTextCode.TEMPLATE_WOOD_HEAD_MALLET_DESCRIPTION.toString(), defaultLanguage));
            }
            else if(property.getPreferredName(defaultLanguage).equals(SpringBridge.getInstance().getMessage(TemplateTextCode.TEMPLATE_PRODUCT_PROPERTIES_WIDTH.toString(), defaultLanguage)) || property.getPreferredName(defaultLanguage).equals(SpringBridge.getInstance().getMessage(TemplateTextCode.TEMPLATE_PRODUCT_PROPERTIES_LENGTH.toString(), defaultLanguage)) || property.getPreferredName(defaultLanguage).equals(SpringBridge.getInstance().getMessage(TemplateTextCode.TEMPLATE_PRODUCT_PROPERTIES_HEIGHT.toString(), defaultLanguage)) || property.getPreferredName(defaultLanguage).equals(SpringBridge.getInstance().getMessage(TemplateTextCode.TEMPLATE_PRODUCT_PROPERTIES_WEIGHT.toString(), defaultLanguage))){
                String exampleUnit = "cm";
                if(property.getPreferredName(defaultLanguage).equals(SpringBridge.getInstance().getMessage(TemplateTextCode.TEMPLATE_PRODUCT_PROPERTIES_WIDTH.toString(), defaultLanguage))){
                    productPropertiesExampleTab.getRow(4).getCell(columnOffset).setCellValue("3");
                    productPropertiesExampleTab.getRow(5).createCell(columnOffset).setCellValue("78");
                    productPropertiesExampleTab.getRow(6).createCell(columnOffset).setCellValue("80");
                    productPropertiesExampleTab.getRow(5).createCell(++columnOffset).setCellValue("cm");
                    productPropertiesExampleTab.getRow(6).createCell(columnOffset).setCellValue("mm");
                }
                else if(property.getPreferredName(defaultLanguage).equals(SpringBridge.getInstance().getMessage(TemplateTextCode.TEMPLATE_PRODUCT_PROPERTIES_LENGTH.toString(), defaultLanguage))){
                    productPropertiesExampleTab.getRow(4).getCell(columnOffset).setCellValue("4");
                    productPropertiesExampleTab.getRow(5).createCell(columnOffset).setCellValue("35");
                    productPropertiesExampleTab.getRow(6).createCell(columnOffset).setCellValue("40");
                    productPropertiesExampleTab.getRow(5).createCell(++columnOffset).setCellValue("mm");
                    productPropertiesExampleTab.getRow(6).createCell(columnOffset).setCellValue("mm");
                }
                else if(property.getPreferredName(defaultLanguage).equals(SpringBridge.getInstance().getMessage(TemplateTextCode.TEMPLATE_PRODUCT_PROPERTIES_HEIGHT.toString(), defaultLanguage))){
                    productPropertiesExampleTab.getRow(4).getCell(columnOffset).setCellValue("2");
                    productPropertiesExampleTab.getRow(5).createCell(columnOffset).setCellValue("3");
                    productPropertiesExampleTab.getRow(6).createCell(columnOffset).setCellValue("320");
                    productPropertiesExampleTab.getRow(5).createCell(++columnOffset).setCellValue("cm");
                    productPropertiesExampleTab.getRow(6).createCell(columnOffset).setCellValue("mm");
                }
                else if(property.getPreferredName(defaultLanguage).equals(SpringBridge.getInstance().getMessage(TemplateTextCode.TEMPLATE_PRODUCT_PROPERTIES_WEIGHT.toString(), defaultLanguage))){
                    exampleUnit = "g";
                    productPropertiesExampleTab.getRow(4).getCell(columnOffset).setCellValue("20");
                    productPropertiesExampleTab.getRow(5).createCell(columnOffset).setCellValue("30");
                    productPropertiesExampleTab.getRow(6).createCell(columnOffset).setCellValue("300");
                    productPropertiesExampleTab.getRow(5).createCell(++columnOffset).setCellValue(exampleUnit);
                    productPropertiesExampleTab.getRow(6).createCell(columnOffset).setCellValue(exampleUnit);
                }
                // quantity unit
                cell = secondRow.createCell(columnOffset);
                cell.setCellStyle(readOnlyStyle);
                cell = thirdRow.createCell(columnOffset);
                cell.setCellValue(TemplateConfig.TEMPLATE_QUANTITY_UNIT);
                cell.setCellStyle(readOnlyStyle);
                cell = fourthRow.createCell(columnOffset);
                cell.setCellStyle(readOnlyStyle);
                productPropertiesExampleTab.getRow(4).createCell(columnOffset).setCellStyle(editableStyle);

                CellRangeAddressList cellRangeAddressList = new CellRangeAddressList(4,4,columnOffset,columnOffset);
                DataValidationHelper dataValidationHelper = productPropertiesExampleTab.getDataValidationHelper();
                String constraints = property.getPreferredName(defaultLanguage).equals(SpringBridge.getInstance().getMessage(TemplateTextCode.TEMPLATE_PRODUCT_PROPERTIES_WEIGHT.toString(), defaultLanguage)) ? SpringBridge.getInstance().getMessage(TemplateTextCode.TEMPLATE_WEIGHT_UNIT_LIST.toString(), defaultLanguage): SpringBridge.getInstance().getMessage(TemplateTextCode.TEMPLATE_DIMENSION_LIST.toString(), defaultLanguage);
                DataValidationConstraint dataValidationConstraint = dataValidationHelper.createFormulaListConstraint(constraints);
                DataValidation dataValidation  = dataValidationHelper.createValidation(dataValidationConstraint, cellRangeAddressList);
                dataValidation.setSuppressDropDownArrow(true);
                // error box
                dataValidation.setShowErrorBox(true);
                dataValidation.createErrorBox(SpringBridge.getInstance().getMessage(TemplateTextCode.TEMPLATE_INVALID_INPUT.toString(), defaultLanguage),SpringBridge.getInstance().getMessage(TemplateTextCode.TEMPLATE_INVALID_INPUT_EXPLANATION.toString(), defaultLanguage));
                // empty cell
                dataValidation.setEmptyCellAllowed(true);
                productPropertiesExampleTab.addValidationData(dataValidation);

                productPropertiesExampleTab.getRow(4).createCell(columnOffset).setCellValue(exampleUnit);
                productPropertiesExampleTab.getRow(4).getCell(columnOffset).setCellStyle(editableStyle);
            }
            columnOffset++;
        }

        // columns for the properties obtained from the categories
        for (Category category : categories) {
            List<Property> categoryProperties = getPropertiesToBeIncludedInTemplate(category);
            for (Property property : categoryProperties) {
                cell = secondRow.createCell(columnOffset);
                cell.setCellValue(property.getPreferredName(defaultLanguage));
                cell.setCellStyle(boldCellStyle);
                Cell thirdRowCell = thirdRow.createCell(columnOffset);
                // get data type of the property
                // if its data type is Quantity or Amount, we should add Value to its data type to have a consistent template view
                String dataType = normalizeDataTypeForTemplate(property);
                thirdRowCell.setCellValue(dataType.contentEquals(TemplateConfig.TEMPLATE_DATA_TYPE_QUANTITY) ? TemplateConfig.TEMPLATE_QUANTITY_VALUE
                        : dataType.contentEquals(TEMPLATE_DATA_TYPE_AMOUNT) ? TEMPLATE_DATA_TYPE_AMOUNT_VALUE
                        : dataType);
                // make thirdRow read only
                thirdRowCell.setCellStyle(readOnlyStyle);
                productPropertiesExampleTab.getRow(4).createCell(columnOffset).setCellStyle(editableStyle);
                fourthRow.createCell(columnOffset).setCellStyle(readOnlyStyle);
                if (property.getDataType().equals("BOOLEAN")){
                    CellRangeAddressList cellRangeAddressList = new CellRangeAddressList(4,4,columnOffset,columnOffset);
                    DataValidationHelper dataValidationHelper = productPropertiesExampleTab.getDataValidationHelper();
                    DataValidationConstraint dataValidationConstraint = dataValidationHelper.createFormulaListConstraint(SpringBridge.getInstance().getMessage(TemplateTextCode.TEMPLATE_BOOLEAN_LIST.toString(), defaultLanguage));
                    DataValidation dataValidation  = dataValidationHelper.createValidation(dataValidationConstraint, cellRangeAddressList);
                    dataValidation.setSuppressDropDownArrow(true);
                    // error box
                    dataValidation.setShowErrorBox(true);
                    dataValidation.createErrorBox(SpringBridge.getInstance().getMessage(TemplateTextCode.TEMPLATE_INVALID_INPUT.toString(), defaultLanguage),SpringBridge.getInstance().getMessage(TemplateTextCode.TEMPLATE_INVALID_INPUT_EXPLANATION.toString(), defaultLanguage));
                    // empty cell
                    dataValidation.setEmptyCellAllowed(true);
                    productPropertiesExampleTab.addValidationData(dataValidation);
                }

                // check whether the property needs a unit
                if(property.getDataType().equals(TEMPLATE_DATA_TYPE_AMOUNT)){
                    // quantity unit
                    productPropertiesExampleTab.getRow(0).createCell(++columnOffset).setCellStyle(readOnlyStyle);
                    cell = secondRow.createCell(columnOffset);
                    cell.setCellStyle(readOnlyStyle);
                    cell = thirdRow.createCell(columnOffset);
                    cell.setCellValue("CURRENCY");
                    cell.setCellStyle(readOnlyStyle);
                    cell = fourthRow.createCell(columnOffset);
                    cell.setCellStyle(readOnlyStyle);
                    productPropertiesExampleTab.getRow(4).createCell(columnOffset).setCellStyle(editableStyle);
                }
                else if(property.getDataType().equals(TemplateConfig.TEMPLATE_DATA_TYPE_QUANTITY)){
                    // quantity unit
                    productPropertiesExampleTab.getRow(0).createCell(++columnOffset).setCellStyle(readOnlyStyle);
                    cell = secondRow.createCell(columnOffset);
                    cell.setCellStyle(readOnlyStyle);
                    cell = thirdRow.createCell(columnOffset);
                    cell.setCellValue(TemplateConfig.TEMPLATE_QUANTITY_UNIT);
                    cell.setCellStyle(readOnlyStyle);
                    cell = fourthRow.createCell(columnOffset);
                    cell.setCellStyle(readOnlyStyle);
                    productPropertiesExampleTab.getRow(4).createCell(columnOffset).setCellStyle(editableStyle);
                }
                fourthRow.getCell(columnOffset).setCellValue(property.getUnit() != null ? property.getUnit().getShortName() : "");
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
        cell.setCellValue(SpringBridge.getInstance().getMessage(TemplateTextCode.TEMPLATE_TRADING_DELIVERY_TRADING_DETAILS.toString(), defaultLanguage));
        cell.setCellStyle(boldCellStyle);
        CellRangeAddress cra = new CellRangeAddress(0, 0, 2, 8);
        termsTab.addMergedRegion(cra);

        // warranty block
        cell = getCellWithMissingCellPolicy(topRow, 9);
        cell.setCellValue(SpringBridge.getInstance().getMessage(TemplateTextCode.TEMPLATE_TRADING_DELIVERY_WARRANTY.toString(), defaultLanguage));
        cell.setCellStyle(boldCellStyle);
        cra = new CellRangeAddress(0, 0, 9, 11);
        termsTab.addMergedRegion(cra);

        // delivery terms block
        cell = getCellWithMissingCellPolicy(topRow, 12);
        cell.setCellValue(SpringBridge.getInstance().getMessage(TemplateTextCode.TEMPLATE_TRADING_DELIVERY_DELIVERY_TERMS.toString(), defaultLanguage));
        cell.setCellStyle(boldCellStyle);
        cra = new CellRangeAddress(0, 0, 12, 16);
        termsTab.addMergedRegion(cra);

        // packaging block
        cell = getCellWithMissingCellPolicy(topRow, 17);
        cell.setCellValue(SpringBridge.getInstance().getMessage(TemplateTextCode.TEMPLATE_TRADING_DELIVERY_PACKAGING.toString(), defaultLanguage));
        cell.setCellStyle(boldCellStyle);
        cra = new CellRangeAddress(0, 0, 17, 19);
        termsTab.addMergedRegion(cra);

        // customization block
        cell = getCellWithMissingCellPolicy(topRow, 20);
        cell.setCellValue(SpringBridge.getInstance().getMessage(TemplateTextCode.TEMPLATE_TRADING_DELIVERY_CUSTOMIZATION.toString(), defaultLanguage));
        cell.setCellStyle(boldCellStyle);

        // spare part block
        if(SpringBridge.getInstance().getCatalogueServiceConfig().getSparePartEnabled()){
            cell = getCellWithMissingCellPolicy(topRow, 21);
            cell.setCellValue(SpringBridge.getInstance().getMessage(TemplateTextCode.TEMPLATE_TRADING_DELIVERY_SPARE_PART.toString(), defaultLanguage));
            cell.setCellStyle(boldCellStyle);
        }

        // 2nd, 3rd and 4th rows
        // name, data type, unit label on the leftmost column
        int rowIndex = 1;
        int columnIndex = 0;
        Row secondRow = termsTab.createRow(rowIndex);
        cell = secondRow.createCell(columnIndex);
        cell.setCellValue(SpringBridge.getInstance().getMessage(TemplateTextCode.TEMPLATE_PRODUCT_PROPERTIES_PROPERTY_NAME.toString(), defaultLanguage));
        cell.setCellStyle(boldCellStyle);
        Row thirdRow = termsTab.createRow(++rowIndex);
        cell = thirdRow.createCell(columnIndex);
        cell.setCellValue(SpringBridge.getInstance().getMessage(TemplateTextCode.TEMPLATE_PRODUCT_PROPERTIES_PROPERTY_DATA_TYPE.toString(), defaultLanguage));
        cell.setCellStyle(boldCellStyle);
        Row fourthRow = termsTab.createRow(++rowIndex);
        cell = fourthRow.createCell(columnIndex);
        cell.setCellValue(SpringBridge.getInstance().getMessage(TemplateTextCode.TEMPLATE_PRODUCT_PROPERTIES_PROPERTY_UNIT.toString(), defaultLanguage));
        cell.setCellStyle(boldCellStyle);
        termsTab.createRow(++rowIndex);

        // common UBL-based properties
        columnIndex = 1;
        List<Property> properties = TemplateConfig.getFixedPropertiesForTermsTab(defaultLanguage);
        for (Property property : properties) {

            cell = secondRow.createCell(columnIndex);
            cell.setCellValue(property.getPreferredName(defaultLanguage));
            cell.setCellStyle(boldCellStyle);
            if(checkMandatory(property)){
                termsTab.getRow(4).createCell(columnIndex).setCellStyle(mandatoryCellStyle);
            }
            else {
                termsTab.getRow(4).createCell(columnIndex).setCellStyle(editableStyle);
            }
            Cell thirdRowCell = thirdRow.createCell(columnIndex);
            // get data type of the property
            // if its data type is Quantity or Amount, we should add Value to its data type to have a consistent template view
            String dataType = normalizeDataTypeForTemplate(property);
            // in this tab, all properties with STRING data type can be represented with TEXT data type instead of MULTILINGUAL_TEXT
            if(dataType.contentEquals(TemplateConfig.TEMPLATE_DATA_TYPE_MULTILINGUAL_TEXT)){
                thirdRowCell.setCellValue(TemplateConfig.TEMPLATE_DATA_TYPE_TEXT);
            } else{
                thirdRowCell.setCellValue(dataType.contentEquals(TemplateConfig.TEMPLATE_DATA_TYPE_QUANTITY) ? TemplateConfig.TEMPLATE_QUANTITY_VALUE
                        : dataType.contentEquals(TEMPLATE_DATA_TYPE_AMOUNT) ? TEMPLATE_DATA_TYPE_AMOUNT_VALUE
                        : dataType);
            }
            // make thirdRow read only
            thirdRowCell.setCellStyle(readOnlyStyle);
            fourthRow.createCell(columnIndex).setCellStyle(readOnlyStyle);
            // check whether the property needs a unit
            if(property.getDataType().equals(TEMPLATE_DATA_TYPE_AMOUNT)){
                // quantity unit
                termsTab.getRow(0).createCell(++columnIndex).setCellStyle(readOnlyStyle);
                cell = secondRow.createCell(columnIndex);
                cell.setCellStyle(readOnlyStyle);
                cell = thirdRow.createCell(columnIndex);
                cell.setCellValue("CURRENCY");
                cell.setCellStyle(readOnlyStyle);
                cell = fourthRow.createCell(columnIndex);
                cell.setCellStyle(readOnlyStyle);
                termsTab.getRow(4).createCell(columnIndex).setCellStyle(editableStyle);

                CellRangeAddressList cellRangeAddressList = new CellRangeAddressList(4,4,columnIndex,columnIndex);
                DataValidationHelper dataValidationHelper = termsTab.getDataValidationHelper();
                DataValidationConstraint dataValidationConstraint = dataValidationHelper.createFormulaListConstraint(SpringBridge.getInstance().getMessage(TemplateTextCode.TEMPLATE_CURRENCY_LIST.toString(), defaultLanguage));
                DataValidation dataValidation  = dataValidationHelper.createValidation(dataValidationConstraint, cellRangeAddressList);
                dataValidation.setSuppressDropDownArrow(true);
                // error box
                dataValidation.setShowErrorBox(true);
                dataValidation.createErrorBox(SpringBridge.getInstance().getMessage(TemplateTextCode.TEMPLATE_INVALID_INPUT.toString(), defaultLanguage),SpringBridge.getInstance().getMessage(TemplateTextCode.TEMPLATE_INVALID_INPUT_EXPLANATION.toString(), defaultLanguage));
                // empty cell
                dataValidation.setEmptyCellAllowed(true);
                termsTab.addValidationData(dataValidation);
            }
            else if(property.getDataType().equals(TemplateConfig.TEMPLATE_DATA_TYPE_QUANTITY)){
                // quantity unit
                termsTab.getRow(0).createCell(++columnIndex).setCellStyle(readOnlyStyle);
                cell = secondRow.createCell(columnIndex);
                cell.setCellStyle(readOnlyStyle);
                cell = thirdRow.createCell(columnIndex);
                cell.setCellValue(TemplateConfig.TEMPLATE_QUANTITY_UNIT);
                cell.setCellStyle(readOnlyStyle);
                cell = fourthRow.createCell(columnIndex);
                cell.setCellStyle(readOnlyStyle);
                termsTab.getRow(4).createCell(columnIndex).setCellStyle(editableStyle);
            }
            // dropdown menu for incoterms
            if(property.getPreferredName(defaultLanguage).equals(SpringBridge.getInstance().getMessage(TemplateTextCode.TEMPLATE_TRADING_DELIVERY_INCOTERMS.toString(), defaultLanguage))){
                CellRangeAddressList cellRangeAddressList = new CellRangeAddressList(4,4,columnIndex,columnIndex);
                DataValidationHelper dataValidationHelper = termsTab.getDataValidationHelper();
                DataValidationConstraint dataValidationConstraint = dataValidationHelper.createFormulaListConstraint(SpringBridge.getInstance().getMessage(TemplateTextCode.TEMPLATE_INCOTERMS_LIST.toString(), defaultLanguage));
                DataValidation dataValidation  = dataValidationHelper.createValidation(dataValidationConstraint, cellRangeAddressList);
                dataValidation.setSuppressDropDownArrow(true);
                // error box
                dataValidation.setShowErrorBox(true);
                dataValidation.createErrorBox(SpringBridge.getInstance().getMessage(TemplateTextCode.TEMPLATE_INVALID_INPUT.toString(), defaultLanguage),SpringBridge.getInstance().getMessage(TemplateTextCode.TEMPLATE_INVALID_INPUT_EXPLANATION.toString(), defaultLanguage));
                // empty cell
                dataValidation.setEmptyCellAllowed(true);
                termsTab.addValidationData(dataValidation);

            }
            else if (property.getDataType().equals("BOOLEAN")){
                CellRangeAddressList cellRangeAddressList = new CellRangeAddressList(4,4,columnIndex,columnIndex);
                DataValidationHelper dataValidationHelper = termsTab.getDataValidationHelper();
                DataValidationConstraint dataValidationConstraint = dataValidationHelper.createFormulaListConstraint(SpringBridge.getInstance().getMessage(TemplateTextCode.TEMPLATE_BOOLEAN_LIST.toString(), defaultLanguage));
                DataValidation dataValidation  = dataValidationHelper.createValidation(dataValidationConstraint, cellRangeAddressList);
                dataValidation.setSuppressDropDownArrow(true);
                // error box
                dataValidation.setShowErrorBox(true);
                dataValidation.createErrorBox(SpringBridge.getInstance().getMessage(TemplateTextCode.TEMPLATE_INVALID_INPUT.toString(), defaultLanguage),SpringBridge.getInstance().getMessage(TemplateTextCode.TEMPLATE_INVALID_INPUT_EXPLANATION.toString(), defaultLanguage));
                // empty cell
                dataValidation.setEmptyCellAllowed(true);
                termsTab.addValidationData(dataValidation);
            }
            else if(property.getPreferredName(defaultLanguage).equals(SpringBridge.getInstance().getMessage(TemplateTextCode.TEMPLATE_TRADING_DELIVERY_WARRANTY_VALIDITY_PERIOD.toString(), defaultLanguage))){
                CellRangeAddressList cellRangeAddressList = new CellRangeAddressList(4,4,columnIndex,columnIndex);
                DataValidationHelper dataValidationHelper = termsTab.getDataValidationHelper();
                DataValidationConstraint dataValidationConstraint = dataValidationHelper.createFormulaListConstraint(SpringBridge.getInstance().getMessage(TemplateTextCode.TEMPLATE_WARRANTY_VALIDITY_LIST.toString(), defaultLanguage));
                DataValidation dataValidation  = dataValidationHelper.createValidation(dataValidationConstraint, cellRangeAddressList);
                dataValidation.setSuppressDropDownArrow(true);
                // error box
                dataValidation.setShowErrorBox(true);
                dataValidation.createErrorBox(SpringBridge.getInstance().getMessage(TemplateTextCode.TEMPLATE_INVALID_INPUT.toString(), defaultLanguage),SpringBridge.getInstance().getMessage(TemplateTextCode.TEMPLATE_INVALID_INPUT_EXPLANATION.toString(), defaultLanguage));
                // empty cell
                dataValidation.setEmptyCellAllowed(true);
                termsTab.addValidationData(dataValidation);
            }
            else if(property.getPreferredName(defaultLanguage).equals(SpringBridge.getInstance().getMessage(TemplateTextCode.TEMPLATE_TRADING_DELIVERY_ESTIMATED_DELIVERY_PERIOD.toString(), defaultLanguage))){
                CellRangeAddressList cellRangeAddressList = new CellRangeAddressList(4,4,columnIndex,columnIndex);
                DataValidationHelper dataValidationHelper = termsTab.getDataValidationHelper();
                DataValidationConstraint dataValidationConstraint = dataValidationHelper.createFormulaListConstraint(SpringBridge.getInstance().getMessage(TemplateTextCode.TEMPLATE_DELIVERY_PERIOD_LIST.toString(), defaultLanguage));
                DataValidation dataValidation  = dataValidationHelper.createValidation(dataValidationConstraint, cellRangeAddressList);
                dataValidation.setSuppressDropDownArrow(true);
                // error box
                dataValidation.setShowErrorBox(true);
                dataValidation.createErrorBox(SpringBridge.getInstance().getMessage(TemplateTextCode.TEMPLATE_INVALID_INPUT.toString(), defaultLanguage),SpringBridge.getInstance().getMessage(TemplateTextCode.TEMPLATE_INVALID_INPUT_EXPLANATION.toString(), defaultLanguage));
                // empty cell
                dataValidation.setEmptyCellAllowed(true);
                termsTab.addValidationData(dataValidation);
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
        cell.setCellValue(SpringBridge.getInstance().getMessage(TemplateTextCode.TEMPLATE_TRADING_DELIVERY_TRADING_DETAILS.toString(), defaultLanguage));
        cell.setCellStyle(boldCellStyle);
        CellRangeAddress cra = new CellRangeAddress(0, 0, 2, 8);
        termsExampleTab.addMergedRegion(cra);

        // warranty block
        cell = getCellWithMissingCellPolicy(topRow, 9);
        cell.setCellValue(SpringBridge.getInstance().getMessage(TemplateTextCode.TEMPLATE_TRADING_DELIVERY_WARRANTY.toString(), defaultLanguage));
        cell.setCellStyle(boldCellStyle);
        cra = new CellRangeAddress(0, 0, 9, 11);
        termsExampleTab.addMergedRegion(cra);

        // delivery terms block
        cell = getCellWithMissingCellPolicy(topRow, 12);
        cell.setCellValue(SpringBridge.getInstance().getMessage(TemplateTextCode.TEMPLATE_TRADING_DELIVERY_DELIVERY_TERMS.toString(), defaultLanguage));
        cell.setCellStyle(boldCellStyle);
        cra = new CellRangeAddress(0, 0, 12, 16);
        termsExampleTab.addMergedRegion(cra);

        // packaging block
        cell = getCellWithMissingCellPolicy(topRow, 17);
        cell.setCellValue(SpringBridge.getInstance().getMessage(TemplateTextCode.TEMPLATE_TRADING_DELIVERY_PACKAGING.toString(), defaultLanguage));
        cell.setCellStyle(boldCellStyle);
        cra = new CellRangeAddress(0, 0, 17, 19);
        termsExampleTab.addMergedRegion(cra);

        // customization block
        cell = getCellWithMissingCellPolicy(topRow, 20);
        cell.setCellValue(SpringBridge.getInstance().getMessage(TemplateTextCode.TEMPLATE_TRADING_DELIVERY_CUSTOMIZATION.toString(), defaultLanguage));
        cell.setCellStyle(boldCellStyle);

        // spare part block
        if(SpringBridge.getInstance().getCatalogueServiceConfig().getSparePartEnabled()){
            cell = getCellWithMissingCellPolicy(topRow, 21);
            cell.setCellValue(SpringBridge.getInstance().getMessage(TemplateTextCode.TEMPLATE_TRADING_DELIVERY_SPARE_PART.toString(), defaultLanguage));
            cell.setCellStyle(boldCellStyle);
        }
        // 2nd, 3rd and 4th rows
        // name, data type, unit label on the leftmost column
        int rowIndex = 1;
        int columnIndex = 0;
        Row secondRow = termsExampleTab.createRow(rowIndex);
        cell = secondRow.createCell(columnIndex);
        cell.setCellValue(SpringBridge.getInstance().getMessage(TemplateTextCode.TEMPLATE_PRODUCT_PROPERTIES_PROPERTY_NAME.toString(), defaultLanguage));
        cell.setCellStyle(boldCellStyle);
        Row thirdRow = termsExampleTab.createRow(++rowIndex);
        cell = thirdRow.createCell(columnIndex);
        cell.setCellValue(SpringBridge.getInstance().getMessage(TemplateTextCode.TEMPLATE_PRODUCT_PROPERTIES_PROPERTY_DATA_TYPE.toString(), defaultLanguage));
        cell.setCellStyle(boldCellStyle);
        Row fourthRow = termsExampleTab.createRow(++rowIndex);
        cell = fourthRow.createCell(columnIndex);
        cell.setCellValue(SpringBridge.getInstance().getMessage(TemplateTextCode.TEMPLATE_PRODUCT_PROPERTIES_PROPERTY_UNIT.toString(), defaultLanguage));
        cell.setCellStyle(boldCellStyle);
        termsExampleTab.createRow(++rowIndex);

        // common UBL-based properties
        columnIndex = 1;
        List<Property> properties = TemplateConfig.getFixedPropertiesForTermsTab(defaultLanguage);
        for (Property property : properties) {

            cell = secondRow.createCell(columnIndex);
            cell.setCellValue(property.getPreferredName(defaultLanguage));
            cell.setCellStyle(boldCellStyle);
            if(checkMandatory(property)){
                termsExampleTab.getRow(4).createCell(columnIndex).setCellStyle(mandatoryCellStyle);
            }
            else {
                termsExampleTab.getRow(4).createCell(columnIndex).setCellStyle(editableStyle);
            }
            Cell thirdRowCell = thirdRow.createCell(columnIndex);
            // get data type of the property
            // if its data type is Quantity or Amount, we should add Value to its data type to have a consistent template view
            String dataType = normalizeDataTypeForTemplate(property);
            // in this tab, all properties with STRING data type can be represented with TEXT data type instead of MULTILINGUAL_TEXT
            if(dataType.contentEquals(TemplateConfig.TEMPLATE_DATA_TYPE_MULTILINGUAL_TEXT)){
                thirdRowCell.setCellValue(TemplateConfig.TEMPLATE_DATA_TYPE_TEXT);
            } else{
                thirdRowCell.setCellValue(dataType.contentEquals(TemplateConfig.TEMPLATE_DATA_TYPE_QUANTITY) ? TemplateConfig.TEMPLATE_QUANTITY_VALUE
                        : dataType.contentEquals(TEMPLATE_DATA_TYPE_AMOUNT) ? TEMPLATE_DATA_TYPE_AMOUNT_VALUE
                        : dataType);
            }
            // make thirdRow read only
            thirdRowCell.setCellStyle(readOnlyStyle);
            fourthRow.createCell(columnIndex).setCellStyle(readOnlyStyle);

            // fill cells with example values
            if (property.getPreferredName(defaultLanguage).equals(SpringBridge.getInstance().getMessage(TemplateTextCode.TEMPLATE_PRODUCT_PROPERTIES_MANUFACTURER_ITEM_IDENTIFICATION.toString(), defaultLanguage))){
                termsExampleTab.getRow(4).getCell(columnIndex).setCellValue("Product_id1");
                termsExampleTab.createRow(5).createCell(columnIndex).setCellValue("Product_id2");
                termsExampleTab.createRow(6).createCell(columnIndex).setCellValue("Product_id3");
            }
            else if(property.getPreferredName(defaultLanguage).equals(SpringBridge.getInstance().getMessage(TemplateTextCode.TEMPLATE_TRADING_DELIVERY_PRICE_BASE_QUANTITY.toString(), defaultLanguage))){
                termsExampleTab.getRow(4).getCell(columnIndex).setCellValue("1");
                termsExampleTab.getRow(5).createCell(columnIndex).setCellValue("1");
                termsExampleTab.getRow(6).createCell(columnIndex).setCellValue("1");
                // quantity unit
                termsExampleTab.getRow(0).createCell(++columnIndex).setCellStyle(readOnlyStyle);
                cell = secondRow.createCell(columnIndex);
                cell.setCellStyle(readOnlyStyle);
                cell = thirdRow.createCell(columnIndex);
                cell.setCellValue(TemplateConfig.TEMPLATE_QUANTITY_UNIT);
                cell.setCellStyle(readOnlyStyle);
                cell = fourthRow.createCell(columnIndex);
                cell.setCellStyle(readOnlyStyle);
                termsExampleTab.getRow(4).createCell(columnIndex).setCellStyle(editableStyle);

                termsExampleTab.getRow(4).getCell(columnIndex).setCellValue(SpringBridge.getInstance().getMessage(TemplateTextCode.TEMPLATE_EXAMPLE_MINIMUM_ORDER_QUANTITY_UNIT.toString(), defaultLanguage));
                termsExampleTab.getRow(5).createCell(columnIndex).setCellValue(SpringBridge.getInstance().getMessage(TemplateTextCode.TEMPLATE_EXAMPLE_MINIMUM_ORDER_QUANTITY_UNIT.toString(), defaultLanguage));
                termsExampleTab.getRow(6).createCell(columnIndex).setCellValue(SpringBridge.getInstance().getMessage(TemplateTextCode.TEMPLATE_EXAMPLE_MINIMUM_ORDER_QUANTITY_UNIT.toString(), defaultLanguage));

            }
            else if(property.getPreferredName(defaultLanguage).equals(SpringBridge.getInstance().getMessage(TemplateTextCode.TEMPLATE_TRADING_DELIVERY_MINIMUM_ORDER_QUANTITY.toString(), defaultLanguage))){
                termsExampleTab.getRow(4).getCell(columnIndex).setCellValue("3000");
                termsExampleTab.getRow(5).createCell(columnIndex).setCellValue("3000");
                termsExampleTab.getRow(6).createCell(columnIndex).setCellValue("1000");
                // quantity unit
                termsExampleTab.getRow(0).createCell(++columnIndex).setCellStyle(readOnlyStyle);
                cell = secondRow.createCell(columnIndex);
                cell.setCellStyle(readOnlyStyle);
                cell = thirdRow.createCell(columnIndex);
                cell.setCellValue(TemplateConfig.TEMPLATE_QUANTITY_UNIT);
                cell.setCellStyle(readOnlyStyle);
                cell = fourthRow.createCell(columnIndex);
                cell.setCellStyle(readOnlyStyle);
                termsExampleTab.getRow(4).createCell(columnIndex).setCellStyle(editableStyle);

                termsExampleTab.getRow(4).getCell(columnIndex).setCellValue(SpringBridge.getInstance().getMessage(TemplateTextCode.TEMPLATE_EXAMPLE_MINIMUM_ORDER_QUANTITY_UNIT.toString(), defaultLanguage));
                termsExampleTab.getRow(5).createCell(columnIndex).setCellValue(SpringBridge.getInstance().getMessage(TemplateTextCode.TEMPLATE_EXAMPLE_MINIMUM_ORDER_QUANTITY_UNIT.toString(), defaultLanguage));
                termsExampleTab.getRow(6).createCell(columnIndex).setCellValue(SpringBridge.getInstance().getMessage(TemplateTextCode.TEMPLATE_EXAMPLE_MINIMUM_ORDER_QUANTITY_UNIT.toString(), defaultLanguage));
            }
            else if(property.getPreferredName(defaultLanguage).equals(SpringBridge.getInstance().getMessage(TemplateTextCode.TEMPLATE_TRADING_DELIVERY_TRANSPORT_MODE.toString(), defaultLanguage))){
                termsExampleTab.getRow(4).getCell(columnIndex).setCellValue(SpringBridge.getInstance().getMessage(TemplateTextCode.TEMPLATE_PLASTIC_HEAD_MALLET_TRANSPORT_MODE.toString(), defaultLanguage));
                termsExampleTab.getRow(5).createCell(columnIndex).setCellValue(SpringBridge.getInstance().getMessage(TemplateTextCode.TEMPLATE_IRON_HEAD_MALLET_TRANSPORT_MODE.toString(), defaultLanguage));
                termsExampleTab.getRow(6).createCell(columnIndex).setCellValue(SpringBridge.getInstance().getMessage(TemplateTextCode.TEMPLATE_WOOD_HEAD_MALLET_TRANSPORT_MODE.toString(), defaultLanguage));
            }
            else if(property.getPreferredName(defaultLanguage).equals(SpringBridge.getInstance().getMessage(TemplateTextCode.TEMPLATE_TRADING_DELIVERY_PACKAGING_TYPE.toString(), defaultLanguage))){
                termsExampleTab.getRow(4).getCell(columnIndex).setCellValue(SpringBridge.getInstance().getMessage(TemplateTextCode.TEMPLATE_PLASTIC_HEAD_MALLET_PACKAGING_TYPE.toString(), defaultLanguage));
                termsExampleTab.getRow(5).createCell(columnIndex).setCellValue(SpringBridge.getInstance().getMessage(TemplateTextCode.TEMPLATE_IRON_HEAD_MALLET_PACKAGING_TYPE.toString(), defaultLanguage));
                termsExampleTab.getRow(6).createCell(columnIndex).setCellValue(SpringBridge.getInstance().getMessage(TemplateTextCode.TEMPLATE_WOOD_HEAD_MALLET_PACKAGING_TYPE.toString(), defaultLanguage));
            }
            else if(property.getPreferredName(defaultLanguage).equals(SpringBridge.getInstance().getMessage(TemplateTextCode.TEMPLATE_TRADING_DELIVERY_CUSTOMIZABLE.toString(), defaultLanguage))){
                termsExampleTab.getRow(4).getCell(columnIndex).setCellValue(true);
                termsExampleTab.getRow(5).createCell(columnIndex).setCellValue(true);
                termsExampleTab.getRow(6).createCell(columnIndex).setCellValue(false);
            }
            else if(SpringBridge.getInstance().getCatalogueServiceConfig().getSparePartEnabled() && property.getPreferredName(defaultLanguage).equals(SpringBridge.getInstance().getMessage(TemplateTextCode.TEMPLATE_TRADING_DELIVERY_SPARE_PART.toString(), defaultLanguage))){
                termsExampleTab.getRow(4).getCell(columnIndex).setCellValue(false);
                termsExampleTab.getRow(5).createCell(columnIndex).setCellValue(true);
                termsExampleTab.getRow(6).createCell(columnIndex).setCellValue(false);
            }
            else if(property.getPreferredName(defaultLanguage).equals(SpringBridge.getInstance().getMessage(TemplateTextCode.TEMPLATE_TRADING_DELIVERY_PACKAGE_QUANTITY.toString(), defaultLanguage))){
                termsExampleTab.getRow(4).getCell(columnIndex).setCellValue("10");
                termsExampleTab.getRow(5).createCell(columnIndex).setCellValue("30");
                termsExampleTab.getRow(6).createCell(columnIndex).setCellValue("1");
                // quantity unit
                termsExampleTab.getRow(0).createCell(++columnIndex).setCellStyle(readOnlyStyle);
                cell = secondRow.createCell(columnIndex);
                cell.setCellStyle(readOnlyStyle);
                cell = thirdRow.createCell(columnIndex);
                cell.setCellValue(TemplateConfig.TEMPLATE_QUANTITY_UNIT);
                cell.setCellStyle(readOnlyStyle);
                cell = fourthRow.createCell(columnIndex);
                cell.setCellStyle(readOnlyStyle);
                termsExampleTab.getRow(4).createCell(columnIndex).setCellStyle(editableStyle);

                termsExampleTab.getRow(4).getCell(columnIndex).setCellValue(SpringBridge.getInstance().getMessage(TemplateTextCode.TEMPLATE_EXAMPLE_PACKAGE_QUANTITY_UNIT.toString(), defaultLanguage));
                termsExampleTab.getRow(5).createCell(columnIndex).setCellValue(SpringBridge.getInstance().getMessage(TemplateTextCode.TEMPLATE_EXAMPLE_PACKAGE_QUANTITY_UNIT.toString(), defaultLanguage));
                termsExampleTab.getRow(6).createCell(columnIndex).setCellValue(SpringBridge.getInstance().getMessage(TemplateTextCode.TEMPLATE_EXAMPLE_PACKAGE_QUANTITY_UNIT.toString(), defaultLanguage));
            }
            // dropdown menu for incoterms
            else if(property.getPreferredName(defaultLanguage).equals(SpringBridge.getInstance().getMessage(TemplateTextCode.TEMPLATE_TRADING_DELIVERY_INCOTERMS.toString(), defaultLanguage))){
                CellRangeAddressList cellRangeAddressList = new CellRangeAddressList(4,4,columnIndex,columnIndex);
                DataValidationHelper dataValidationHelper = termsExampleTab.getDataValidationHelper();
                DataValidationConstraint dataValidationConstraint = dataValidationHelper.createFormulaListConstraint(SpringBridge.getInstance().getMessage(TemplateTextCode.TEMPLATE_INCOTERMS_LIST.toString(), defaultLanguage));
                DataValidation dataValidation  = dataValidationHelper.createValidation(dataValidationConstraint, cellRangeAddressList);
                dataValidation.setSuppressDropDownArrow(true);
                // error box
                dataValidation.setShowErrorBox(true);
                dataValidation.createErrorBox(SpringBridge.getInstance().getMessage(TemplateTextCode.TEMPLATE_INVALID_INPUT.toString(), defaultLanguage),SpringBridge.getInstance().getMessage(TemplateTextCode.TEMPLATE_INVALID_INPUT_EXPLANATION.toString(), defaultLanguage));
                // empty cell
                dataValidation.setEmptyCellAllowed(true);
                termsExampleTab.addValidationData(dataValidation);

                termsExampleTab.getRow(4).getCell(columnIndex).setCellValue("CIF (Cost,Insurance and Freight)");
                termsExampleTab.getRow(5).createCell(columnIndex).setCellValue("FOB (Free on Board)");
                termsExampleTab.getRow(6).createCell(columnIndex).setCellValue("DPU (Delivery at Place Unloaded)");
            }
            else if (property.getDataType().equals("BOOLEAN")){
                CellRangeAddressList cellRangeAddressList = new CellRangeAddressList(4,4,columnIndex,columnIndex);
                DataValidationHelper dataValidationHelper = termsExampleTab.getDataValidationHelper();
                DataValidationConstraint dataValidationConstraint = dataValidationHelper.createFormulaListConstraint(SpringBridge.getInstance().getMessage(TemplateTextCode.TEMPLATE_BOOLEAN_LIST.toString(), defaultLanguage));
                DataValidation dataValidation  = dataValidationHelper.createValidation(dataValidationConstraint, cellRangeAddressList);
                dataValidation.setSuppressDropDownArrow(true);
                // error box
                dataValidation.setShowErrorBox(true);
                dataValidation.createErrorBox(SpringBridge.getInstance().getMessage(TemplateTextCode.TEMPLATE_INVALID_INPUT.toString(), defaultLanguage),SpringBridge.getInstance().getMessage(TemplateTextCode.TEMPLATE_INVALID_INPUT_EXPLANATION.toString(), defaultLanguage));
                // empty cell
                dataValidation.setEmptyCellAllowed(true);
                termsExampleTab.addValidationData(dataValidation);

                termsExampleTab.getRow(4).getCell(columnIndex).setCellValue("TRUE");
                termsExampleTab.getRow(5).createCell(columnIndex).setCellValue("FALSE");
                termsExampleTab.getRow(6).createCell(columnIndex).setCellValue("FALSE");
            }
            else if(property.getPreferredName(defaultLanguage).equals(SpringBridge.getInstance().getMessage(TemplateTextCode.TEMPLATE_TRADING_DELIVERY_WARRANTY_VALIDITY_PERIOD.toString(), defaultLanguage))){
                termsExampleTab.getRow(4).getCell(columnIndex).setCellValue("1");
                termsExampleTab.getRow(5).createCell(columnIndex).setCellValue("3");
                termsExampleTab.getRow(6).createCell(columnIndex).setCellValue("2");
                // quantity unit
                termsExampleTab.getRow(0).createCell(++columnIndex).setCellStyle(readOnlyStyle);
                cell = secondRow.createCell(columnIndex);
                cell.setCellStyle(readOnlyStyle);
                cell = thirdRow.createCell(columnIndex);
                cell.setCellValue(TemplateConfig.TEMPLATE_QUANTITY_UNIT);
                cell.setCellStyle(readOnlyStyle);
                cell = fourthRow.createCell(columnIndex);
                cell.setCellStyle(readOnlyStyle);
                termsExampleTab.getRow(4).createCell(columnIndex).setCellStyle(editableStyle);

                CellRangeAddressList cellRangeAddressList = new CellRangeAddressList(4,4,columnIndex,columnIndex);
                DataValidationHelper dataValidationHelper = termsExampleTab.getDataValidationHelper();
                DataValidationConstraint dataValidationConstraint = dataValidationHelper.createFormulaListConstraint(SpringBridge.getInstance().getMessage(TemplateTextCode.TEMPLATE_WARRANTY_VALIDITY_LIST.toString(), defaultLanguage));
                DataValidation dataValidation  = dataValidationHelper.createValidation(dataValidationConstraint, cellRangeAddressList);
                dataValidation.setSuppressDropDownArrow(true);
                // error box
                dataValidation.setShowErrorBox(true);
                dataValidation.createErrorBox(SpringBridge.getInstance().getMessage(TemplateTextCode.TEMPLATE_INVALID_INPUT.toString(), defaultLanguage),SpringBridge.getInstance().getMessage(TemplateTextCode.TEMPLATE_INVALID_INPUT_EXPLANATION.toString(), defaultLanguage));
                // empty cell
                dataValidation.setEmptyCellAllowed(true);
                termsExampleTab.addValidationData(dataValidation);

                termsExampleTab.getRow(4).getCell(columnIndex).setCellValue("year");
                termsExampleTab.getRow(5).createCell(columnIndex).setCellValue("month");
                termsExampleTab.getRow(6).createCell(columnIndex).setCellValue("month");
            }
            else if(property.getPreferredName(defaultLanguage).equals(SpringBridge.getInstance().getMessage(TemplateTextCode.TEMPLATE_TRADING_DELIVERY_ESTIMATED_DELIVERY_PERIOD.toString(), defaultLanguage))){
                termsExampleTab.getRow(4).getCell(columnIndex).setCellValue("1");
                termsExampleTab.getRow(5).createCell(columnIndex).setCellValue("4");
                termsExampleTab.getRow(6).createCell(columnIndex).setCellValue("2");
                // quantity unit
                termsExampleTab.getRow(0).createCell(++columnIndex).setCellStyle(readOnlyStyle);
                termsExampleTab.getRow(1).createCell(columnIndex).setCellStyle(readOnlyStyle);
                cell = termsExampleTab.getRow(2).createCell(columnIndex);
                cell.setCellValue(TemplateConfig.TEMPLATE_QUANTITY_UNIT);
                cell.setCellStyle(readOnlyStyle);
                termsExampleTab.getRow(3).createCell(columnIndex).setCellStyle(readOnlyStyle);
                termsExampleTab.getRow(4).createCell(columnIndex).setCellStyle(editableStyle);

                CellRangeAddressList cellRangeAddressList = new CellRangeAddressList(4,4,columnIndex,columnIndex);
                DataValidationHelper dataValidationHelper = termsExampleTab.getDataValidationHelper();
                DataValidationConstraint dataValidationConstraint = dataValidationHelper.createFormulaListConstraint(SpringBridge.getInstance().getMessage(TemplateTextCode.TEMPLATE_DELIVERY_PERIOD_LIST.toString(), defaultLanguage));
                DataValidation dataValidation  = dataValidationHelper.createValidation(dataValidationConstraint, cellRangeAddressList);
                dataValidation.setSuppressDropDownArrow(true);
                // error box
                dataValidation.setShowErrorBox(true);
                dataValidation.createErrorBox(SpringBridge.getInstance().getMessage(TemplateTextCode.TEMPLATE_INVALID_INPUT.toString(), defaultLanguage),SpringBridge.getInstance().getMessage(TemplateTextCode.TEMPLATE_INVALID_INPUT_EXPLANATION.toString(), defaultLanguage));
                // empty cell
                dataValidation.setEmptyCellAllowed(true);
                termsExampleTab.addValidationData(dataValidation);

                termsExampleTab.getRow(4).getCell(columnIndex).setCellValue("working days");
                termsExampleTab.getRow(5).createCell(columnIndex).setCellValue("days");
                termsExampleTab.getRow(6).createCell(columnIndex).setCellValue("weeks");
            }
            // check whether the property needs a unit
            if(property.getDataType().equals(TEMPLATE_DATA_TYPE_AMOUNT)){
                termsExampleTab.getRow(4).getCell(columnIndex).setCellValue("4");
                termsExampleTab.getRow(5).createCell(columnIndex).setCellValue("6");
                termsExampleTab.getRow(6).createCell(columnIndex).setCellValue("1");

                // quantity unit
                termsExampleTab.getRow(0).createCell(++columnIndex).setCellStyle(readOnlyStyle);
                termsExampleTab.getRow(1).createCell(columnIndex).setCellStyle(readOnlyStyle);
                cell = termsExampleTab.getRow(2).createCell(columnIndex);
                cell.setCellValue("CURRENCY");
                cell.setCellStyle(readOnlyStyle);
                termsExampleTab.getRow(3).createCell(columnIndex).setCellStyle(readOnlyStyle);
                termsExampleTab.getRow(4).createCell(columnIndex).setCellStyle(editableStyle);

                CellRangeAddressList cellRangeAddressList = new CellRangeAddressList(4,4,columnIndex,columnIndex);
                DataValidationHelper dataValidationHelper = termsExampleTab.getDataValidationHelper();
                DataValidationConstraint dataValidationConstraint = dataValidationHelper.createFormulaListConstraint(SpringBridge.getInstance().getMessage(TemplateTextCode.TEMPLATE_CURRENCY_LIST.toString(), defaultLanguage));
                DataValidation dataValidation  = dataValidationHelper.createValidation(dataValidationConstraint, cellRangeAddressList);
                dataValidation.setSuppressDropDownArrow(true);
                // error box
                dataValidation.setShowErrorBox(true);
                dataValidation.createErrorBox(SpringBridge.getInstance().getMessage(TemplateTextCode.TEMPLATE_INVALID_INPUT.toString(), defaultLanguage),SpringBridge.getInstance().getMessage(TemplateTextCode.TEMPLATE_INVALID_INPUT_EXPLANATION.toString(), defaultLanguage));
                // empty cell
                dataValidation.setEmptyCellAllowed(true);
                termsExampleTab.addValidationData(dataValidation);

                termsExampleTab.getRow(4).getCell(columnIndex).setCellValue("EUR");
                termsExampleTab.getRow(5).createCell(columnIndex).setCellValue("USD");
                termsExampleTab.getRow(6).createCell(columnIndex).setCellValue("SEK");
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
            if(categories.get(i).getProperties() != null){
                if(categories.get(i).getProperties().size() > 0) {
                    int rowFrom = rowIndex;
                    int rowTo = rowIndex + categories.get(i).getProperties().size() - 1;
                    Row row = propertyDetailsTab.createRow(rowIndex);
                    Cell valueCell = getCellWithMissingCellPolicy(row, 0);
                    valueCell.setCellValue(categories.get(i).getPreferredName(defaultLanguage));
                    valueCell.setCellStyle(headerCellStyle);
                    if (rowFrom < rowTo) {
                        CellRangeAddress cra = new CellRangeAddress(rowFrom, rowTo, 0, 0);
                        propertyDetailsTab.addMergedRegion(cra);
                    }
                    rowIndex = rowTo + 1;
                }
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
        cell.setCellValue(SpringBridge.getInstance().getMessage(TemplateTextCode.TEMPLATE_PROPERTY_DETAILS_PROPERTY_NAME.toString(), defaultLanguage));
        cell.setCellStyle(tabCellStyle);
        cell = row.createCell(++columnIndex);
        cell.setCellValue(SpringBridge.getInstance().getMessage(TemplateTextCode.TEMPLATE_PROPERTY_DETAILS_SHORT_NAME.toString(), defaultLanguage));
        cell.setCellStyle(tabCellStyle);
        cell = row.createCell(++columnIndex);
        cell.setCellValue(SpringBridge.getInstance().getMessage(TemplateTextCode.TEMPLATE_PROPERTY_DETAILS_DEFINITION.toString(), defaultLanguage));
        cell.setCellStyle(tabCellStyle);
        cell = row.createCell(++columnIndex);
        cell.setCellValue(SpringBridge.getInstance().getMessage(TemplateTextCode.TEMPLATE_PROPERTY_DETAILS_NOTE.toString(), defaultLanguage));
        cell.setCellStyle(tabCellStyle);
        cell = row.createCell(++columnIndex);
        cell.setCellValue(SpringBridge.getInstance().getMessage(TemplateTextCode.TEMPLATE_PROPERTY_DETAILS_REMARK.toString(), defaultLanguage));
        cell.setCellStyle(tabCellStyle);
        cell = row.createCell(++columnIndex);
        cell.setCellValue(SpringBridge.getInstance().getMessage(TemplateTextCode.TEMPLATE_PROPERTY_DETAILS_PREFERRED_SYMBOL.toString(), defaultLanguage));
        cell.setCellStyle(tabCellStyle);
        cell = row.createCell(++columnIndex);
        cell.setCellValue(SpringBridge.getInstance().getMessage(TemplateTextCode.TEMPLATE_PROPERTY_DETAILS_UNIT.toString(), defaultLanguage));
        cell.setCellStyle(tabCellStyle);
        cell = row.createCell(++columnIndex);
        cell.setCellValue(SpringBridge.getInstance().getMessage(TemplateTextCode.TEMPLATE_PROPERTY_DETAILS_IEC_CATEGORY.toString(), defaultLanguage));
        cell.setCellStyle(tabCellStyle);
        cell = row.createCell(++columnIndex);
        cell.setCellValue(SpringBridge.getInstance().getMessage(TemplateTextCode.TEMPLATE_PROPERTY_DETAILS_ATTRIBUTE_TYPE.toString(), defaultLanguage));
        cell.setCellStyle(tabCellStyle);
        cell = row.createCell(++columnIndex);
        cell.setCellValue(SpringBridge.getInstance().getMessage(TemplateTextCode.TEMPLATE_PROPERTY_DETAILS_DATA_TYPE.toString(), defaultLanguage));
        cell.setCellStyle(tabCellStyle);

        // fill in the details about the properties obtained from categories
        for (Category category : categories) {
            if(category.getProperties() != null){
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
                    cell.setCellValue(property.getRemark(defaultLanguage));
                    cell.setCellStyle(wordWrapStyle);
                    row.createCell(++columnIndex).setCellValue(property.getPreferredSymbol());
                    row.createCell(++columnIndex).setCellValue(property.getUnit() != null ? property.getUnit().getShortName() : "");
                    row.createCell(++columnIndex).setCellValue(property.getIecCategory());
                    row.createCell(++columnIndex).setCellValue(property.getAttributeType());
                    row.createCell(++columnIndex).setCellValue(property.getDataType());
                }
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
            if(category.getProperties() != null){
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
        Row thirdRow = metadataTab.createRow(2);
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
        thirdRow.createCell(0).setCellValue(defaultLanguage);
        // make this sheet hidden
        template.setSheetHidden(template.getSheetIndex(metadataTab),true);
    }

    private void populateSourceList(){
        // values for boolean
        sourceList.createRow(0).createCell(sourceListCellIndex).setCellValue(SpringBridge.getInstance().getMessage(TemplateTextCode.TEMPLATE_BOOLEAN_LIST.toString(), defaultLanguage));
        sourceList.createRow(1).createCell(sourceListCellIndex).setCellValue("TRUE");
        sourceList.createRow(2).createCell(sourceListCellIndex++).setCellValue("FALSE");

        Name namedCell = template.createName();
        namedCell.setNameName(SpringBridge.getInstance().getMessage(TemplateTextCode.TEMPLATE_BOOLEAN_LIST.toString(), defaultLanguage));
        namedCell.setRefersToFormula(TemplateConfig.TEMPLATE_BOOLEAN_REFERENCE);

        // values for incoterms
        sourceList.getRow(0).createCell(sourceListCellIndex).setCellValue(SpringBridge.getInstance().getMessage(TemplateTextCode.TEMPLATE_INCOTERMS_LIST.toString(), defaultLanguage));
        sourceList.getRow(1).createCell(sourceListCellIndex).setCellValue("CIF (Cost,Insurance and Freight)");
        sourceList.getRow(2).createCell(sourceListCellIndex).setCellValue("CIP (Carriage and Insurance Paid to)");
        sourceList.createRow(3).createCell(sourceListCellIndex).setCellValue("CFR (Cost and Freight)");
        sourceList.createRow(4).createCell(sourceListCellIndex).setCellValue("CPT (Carriage paid to)");
        sourceList.createRow(5).createCell(sourceListCellIndex).setCellValue("DAP (Delivered at Place)");
        sourceList.createRow(6).createCell(sourceListCellIndex).setCellValue("DDP (Delivery Duty Paid)");
        sourceList.createRow(7).createCell(sourceListCellIndex).setCellValue("DPU (Delivery at Place Unloaded)");
        sourceList.createRow(8).createCell(sourceListCellIndex).setCellValue("EXW (Ex Works)");
        sourceList.createRow(9).createCell(sourceListCellIndex).setCellValue("FAS (Free Alongside Ship)");
        sourceList.createRow(10).createCell(sourceListCellIndex).setCellValue("FCA (Free Carrier)");
        sourceList.createRow(11).createCell(sourceListCellIndex++).setCellValue("FOB (Free on Board)");

        namedCell = template.createName();
        namedCell.setNameName(SpringBridge.getInstance().getMessage(TemplateTextCode.TEMPLATE_INCOTERMS_LIST.toString(), defaultLanguage));
        namedCell.setRefersToFormula(TemplateConfig.TEMPLATE_INCOTERMS_REFERENCE);

        // values for currency
        sourceList.getRow(0).createCell(sourceListCellIndex).setCellValue(SpringBridge.getInstance().getMessage(TemplateTextCode.TEMPLATE_CURRENCY_LIST.toString(), defaultLanguage));
        sourceList.getRow(1).createCell(sourceListCellIndex).setCellValue("EUR");
        sourceList.getRow(2).createCell(sourceListCellIndex).setCellValue("USD");
        sourceList.getRow(3).createCell(sourceListCellIndex++).setCellValue("SEK");

        namedCell = template.createName();
        namedCell.setNameName(SpringBridge.getInstance().getMessage(TemplateTextCode.TEMPLATE_CURRENCY_LIST.toString(), defaultLanguage));
        namedCell.setRefersToFormula(TemplateConfig.TEMPLATE_CURRENCY_REFERENCE);

        // values for dimensions
        sourceList.getRow(0).createCell(sourceListCellIndex).setCellValue(SpringBridge.getInstance().getMessage(TemplateTextCode.TEMPLATE_DIMENSION_LIST.toString(), defaultLanguage));
        sourceList.getRow(1).createCell(sourceListCellIndex).setCellValue("mm");
        sourceList.getRow(2).createCell(sourceListCellIndex).setCellValue("cm");
        sourceList.getRow(3).createCell(sourceListCellIndex++).setCellValue("m");

        namedCell = template.createName();
        namedCell.setNameName(SpringBridge.getInstance().getMessage(TemplateTextCode.TEMPLATE_DIMENSION_LIST.toString(), defaultLanguage));
        namedCell.setRefersToFormula(TemplateConfig.TEMPLATE_DIMENSION_REFERENCE);

        // values for Warranty Validity Period
        sourceList.getRow(0).createCell(sourceListCellIndex).setCellValue(SpringBridge.getInstance().getMessage(TemplateTextCode.TEMPLATE_WARRANTY_VALIDITY_LIST.toString(), defaultLanguage));
        sourceList.getRow(1).createCell(sourceListCellIndex).setCellValue("year");
        sourceList.getRow(2).createCell(sourceListCellIndex++).setCellValue("month");

        namedCell = template.createName();
        namedCell.setNameName(SpringBridge.getInstance().getMessage(TemplateTextCode.TEMPLATE_WARRANTY_VALIDITY_LIST.toString(), defaultLanguage));
        namedCell.setRefersToFormula(TemplateConfig.TEMPLATE_WARRANTY_REFERENCE);

        // values for Estimated Delivery Period
        sourceList.getRow(0).createCell(sourceListCellIndex).setCellValue(SpringBridge.getInstance().getMessage(TemplateTextCode.TEMPLATE_TRADING_DELIVERY_ESTIMATED_DELIVERY_PERIOD.toString(), defaultLanguage));
        sourceList.getRow(1).createCell(sourceListCellIndex).setCellValue("working days");
        sourceList.getRow(2).createCell(sourceListCellIndex).setCellValue("days");
        sourceList.getRow(3).createCell(sourceListCellIndex++).setCellValue("weeks");

        namedCell = template.createName();
        namedCell.setNameName(SpringBridge.getInstance().getMessage(TemplateTextCode.TEMPLATE_DELIVERY_PERIOD_LIST.toString(), defaultLanguage));
        namedCell.setRefersToFormula(TemplateConfig.TEMPLATE_DELIVERY_PERIOD_REFERENCE);

        // values for weight units
        sourceList.getRow(0).createCell(sourceListCellIndex).setCellValue(SpringBridge.getInstance().getMessage(TemplateTextCode.TEMPLATE_WEIGHT_UNIT_LIST.toString(), defaultLanguage));
        sourceList.getRow(1).createCell(sourceListCellIndex).setCellValue("g");
        sourceList.getRow(2).createCell(sourceListCellIndex).setCellValue("kg");
        sourceList.getRow(3).createCell(sourceListCellIndex++).setCellValue("ton");

        namedCell = template.createName();
        namedCell.setNameName(SpringBridge.getInstance().getMessage(TemplateTextCode.TEMPLATE_WEIGHT_UNIT_LIST.toString(), defaultLanguage));
        namedCell.setRefersToFormula(TEMPLATE_WEIGHT_UNITS_REFERENCE);
        // set sheet hidden
        template.setSheetHidden(template.getSheetIndex(SpringBridge.getInstance().getMessage(TemplateTextCode.TEMPLATE_TAB_SOURCE_LIST.toString(), defaultLanguage)),true);
    }

    private void extendSourceList(List<String> values, String listId){

        int rowIndex = 0;
        sourceList.getRow(rowIndex++).createCell(sourceListCellIndex).setCellValue(listId);
        for (String value : values) {
            sourceList.getRow(rowIndex++).createCell(sourceListCellIndex).setCellValue(value);
        }
        sourceListCellIndex++;

        Name namedCell = template.createName();
        namedCell.setNameName(listId);
        namedCell.setRefersToFormula(String.format("SourceList!$%s$2:$%s$4",sourceListColumn,sourceListColumn));
        sourceListColumn++;
    }

    private boolean checkMandatory(Property property) {
        return property.getPreferredName(defaultLanguage).contentEquals(SpringBridge.getInstance().getMessage(TemplateTextCode.TEMPLATE_PRODUCT_PROPERTIES_MANUFACTURER_ITEM_IDENTIFICATION.toString(), defaultLanguage)) ||
                property.getPreferredName(defaultLanguage).contentEquals(SpringBridge.getInstance().getMessage(TemplateTextCode.TEMPLATE_PRODUCT_PROPERTIES_NAME.toString(), defaultLanguage)) ||
                (property.getRequired() != null && property.getRequired());
    }

    private Row getRow(Sheet sheet, int rowNum) {
        Row row = sheet.getRow(rowNum);
        if (row == null) {
            row = sheet.createRow(rowNum);
        }
        return row;
    }

    public static Cell getCellWithMissingCellPolicy(Row row, int cellNum) {
        return row.getCell(cellNum, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK);
    }

    private void replaceCategoryPropertyIdsWithNames(Sheet sheet,List<Category> categories) {
        // get property id-preferred name map
        Map<String, String> propertyIdPreferredNameMap = getPropertyIdPreferredNameMap(categories);
        // get the row where property names are specified
        Row row = sheet.getRow(1);
        for(int i = 10; i<row.getLastCellNum(); i++) {
            Cell cell = getCellWithMissingCellPolicy(row, i);
            // null cell means we reach to the end of the template
            if(cell == null) {
                break;
            }
            // the identifier of the property
            String propertyId = getCellStringValue(cell);
            // get the name of property
            String propertyName = propertyIdPreferredNameMap.get(propertyId);
            // replace property id with property name
            cell.setCellValue(propertyName);
        }
    }

    private Map<String, String> getPropertyIdPreferredNameMap(List<Category> categories){
        Map<String, String> propertyIdPreferredNameMap = new HashMap<>();
        for (Category category : categories) {
            List<Property> properties = category.getProperties();
            if(properties != null){
                for (Property property : properties) {
                    propertyIdPreferredNameMap.put(property.getId(), property.getPreferredName(defaultLanguage));
                }
            }
        }
        return propertyIdPreferredNameMap;
    }

    public static Integer findCellIndexForProperty(Sheet sheet,String propertyId, List<TextType> categoryNames){
        return findCellIndexForProperty(sheet, propertyId, null, categoryNames);
    }

    public static Integer findCellIndexForProperty(Sheet sheet, List<TextType> propertyNames ,List<TextType> categoryNames){
        return findCellIndexForProperty(sheet,null,propertyNames,categoryNames);
    }

    public static Integer findCellIndexForProperty(Sheet sheet,String propertyId, List<TextType> propertyNames ,List<TextType> categoryNames){
        // get category names
        List<String> categoryNamesString = new ArrayList<>();
        for(TextType text: categoryNames){
            categoryNamesString.add(text.getValue());
        }

        // get property names
        List<String> propertyNamesString = null;
        if(propertyNames != null){
            // get property names
            propertyNamesString = new ArrayList<>();
            for(TextType text: propertyNames){
                propertyNamesString.add(text.getValue());
            }
        }

        Integer categoryIndex = null;
        // get the row where category names are specified
        Row row = sheet.getRow(0);
        for(int i = 0; i<row.getLastCellNum(); i++) {
            Cell cell = getCellWithMissingCellPolicy(row, i);
            // null cell means we reach to the end of the template
            if(cell == null) {
                break;
            }
            String value = getCellStringValue(cell);
            if(categoryNamesString.contains(value)) {
                categoryIndex = i;
                break;
            }
        }

        if(categoryIndex != null){
            // get the row where property names are specified
            row = sheet.getRow(1);
            for(int i = categoryIndex; i<row.getLastCellNum(); i++) {
                Cell cell = TemplateGenerator.getCellWithMissingCellPolicy(row, i);
                // null cell means we reach to the end of the template
                if(cell == null) {
                    break;
                }
                String value = getCellStringValue(cell);
                // if we have property name, search for it in the excel,
                // otherwise, search for property id
                if(propertyNames != null && propertyNamesString.contains(value)) {
                    return i;
                }
                else if(propertyId != null && value.contentEquals(propertyId)){
                    return i;
                }
            }
        }

        return null;
    }

    public static String getCellStringValue(Cell cell) {
        if(cell == null) {
            return "";
        }

        cell.setCellType(CellType.STRING);
        switch (cell.getCellTypeEnum()) {
            case STRING:
                return cell.getStringCellValue();
            case NUMERIC:
                if (DateUtil.isCellDateFormatted(cell)) {
                    return cell.getDateCellValue().toString();
                } else {
                    return cell.getNumericCellValue() + "";
                }
            case BOOLEAN:
                return cell.getBooleanCellValue() ? "True" : "False";
            default:
                return "";
        }
    }

    // Maximum length of a Sheet name is 31 characters. Therefore, we need to return first 31 characters of the sheet name
    // for the given code
    public static String getSheetName(String code,String language){
        String sheetName = SpringBridge.getInstance().getMessage(code, language);
        if(sheetName.length() > 31){
            return sheetName.substring(0,31);
        }
        return sheetName;
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
        if (dataType.compareToIgnoreCase(TemplateConfig.TEMPLATE_DATA_TYPE_STRING) == 0) {
            normalizedType = TemplateConfig.TEMPLATE_DATA_TYPE_MULTILINGUAL_TEXT;

        } else {
            normalizedType = dataType;
        }
        return normalizedType;
    }


    public static String denormalizeDataTypeFromTemplate(String datatypeStr) throws TemplateParseException{
        String denormalizedDatatype;
        if (datatypeStr.compareToIgnoreCase(TEMPLATE_DATA_TYPE_NUMBER) == 0) {
            denormalizedDatatype = TEMPLATE_DATA_TYPE_NUMBER;

        } else if (datatypeStr.compareToIgnoreCase(TEMPLATE_DATA_TYPE_FILE) == 0) {
            denormalizedDatatype = TEMPLATE_DATA_TYPE_FILE;

            // we should also accept 'QUANTITY VALUE' as QUANTITY property
        } else if (datatypeStr.compareToIgnoreCase(TEMPLATE_DATA_TYPE_QUANTITY) == 0 || datatypeStr.compareToIgnoreCase(TEMPLATE_QUANTITY_VALUE) == 0) {
            denormalizedDatatype = TEMPLATE_DATA_TYPE_QUANTITY;

        } else if (datatypeStr.compareToIgnoreCase(TEMPLATE_DATA_TYPE_BOOLEAN) == 0) {
            denormalizedDatatype = TEMPLATE_DATA_TYPE_BOOLEAN;

        } else if(datatypeStr.compareToIgnoreCase(TEMPLATE_DATA_TYPE_TEXT) == 0 || datatypeStr.compareToIgnoreCase(TEMPLATE_DATA_TYPE_MULTILINGUAL_TEXT) == 0){
            denormalizedDatatype = TEMPLATE_DATA_TYPE_STRING;
        } else {
            // for text or other unknown properties
            throw new TemplateParseException(NimbleExceptionMessageCode.BAD_REQUEST_INVALID_DATA_TYPE_FOR_PROPERTY.toString(),Arrays.asList(datatypeStr));
        }
        return denormalizedDatatype;
    }

    private String getMultiValueText(List<TextType> textTypes){
        return getMultiValueText(textTypes,true);
    }

    private String getMultiValueText(List<TextType> textTypes,boolean withLanguageId){
        StringBuilder stringBuilder = new StringBuilder();
        int size = textTypes.size();
        for(int i = 0;i < size;i++){
            String value = textTypes.get(i).getValue();
            if(value != null && !value.contentEquals("")){
                stringBuilder.append(textTypes.get(i).getValue());
                if(withLanguageId){
                    stringBuilder.append("@");
                    stringBuilder.append(textTypes.get(i).getLanguageID());
                }
                if(i != size-1){
                    stringBuilder.append("|");
                }
            }
        }
        return stringBuilder.toString();
    }

    private String getMultiValueRepresentation(List values, String valueQualifier){
        if(values != null && values.size() > 0){
            // strings which are used to create the representation of the given values
            List<String> strings = new ArrayList<>();

            if(valueQualifier.contentEquals(TemplateConfig.TEMPLATE_DATA_TYPE_STRING)){
                strings = (List<String>) values;
            }
            else if(valueQualifier.contentEquals(TemplateConfig.TEMPLATE_DATA_TYPE_NUMBER)){
                for(BigDecimal bigDecimal:(List<BigDecimal>)values){
                    strings.add(new DecimalFormat(".00").format(bigDecimal));
                }
            }
            else if(valueQualifier.contentEquals(TemplateConfig.TEMPLATE_DATA_TYPE_QUANTITY)){
                for(QuantityType quantityType:(List<QuantityType>)values){
                    strings.add(new DecimalFormat(".00").format(quantityType.getValue()));
                }
            }

            // create the representation
            StringBuilder stringBuilder = new StringBuilder();
            int size = strings.size();
            for(int i = 0;i < size;i++){
                stringBuilder.append(strings.get(i));
                if(i != size-1){
                    stringBuilder.append("|");
                }
            }
            return stringBuilder.toString();
        }
        return "";
    }

    // the category with the given id is the parent of at least one of the leaf categories.
    // the method finds a leaf category satisfying this condition and returns its name
    private List<TextType> getNamesOfCategory(Map<Category,List<String>> parentCategoryUriMap,String categoryUri){
        
        for(Category category:parentCategoryUriMap.keySet()){
            if(parentCategoryUriMap.get(category).contains(categoryUri)){
                return category.getPreferredName();
            }
        }
        
        return null;
    }

    private int getColumnCountForCategory(Category category) {
        int propertiesSize = 0;
        List<Property> properties = getPropertiesToBeIncludedInTemplate(category);
        for(Property property : properties){
            propertiesSize++;
            if(property.getDataType().contentEquals(TemplateConfig.TEMPLATE_DATA_TYPE_QUANTITY)){
                propertiesSize++;
            }
        }
        return propertiesSize;
    }

    public static List<Property> getPropertiesToBeIncludedInTemplate(Category category) {
        List<Property> properties = new ArrayList<>();
        if(category.getProperties() != null) {
            for(Property property : category.getProperties()) {
                if(!property.getDataType().contentEquals(TEMPLATE_DATA_TYPE_FILE)) {
                    properties.add(property);
                }
            }
        }
        return properties;
    }
}
