package eu.nimble.service.catalogue.template;

import eu.nimble.service.catalogue.category.IndexCategoryService;
import eu.nimble.service.catalogue.exception.InvalidCategoryException;
import eu.nimble.service.catalogue.exception.NimbleExceptionMessageCode;
import eu.nimble.service.catalogue.exception.TemplateParseException;
import eu.nimble.service.catalogue.model.category.Category;
import eu.nimble.service.catalogue.model.category.Property;
import eu.nimble.service.catalogue.util.LanguageUtil;
import eu.nimble.service.catalogue.util.SpringBridge;
import eu.nimble.service.model.ubl.commonaggregatecomponents.*;
import eu.nimble.service.model.ubl.commonbasiccomponents.*;
import eu.nimble.service.model.ubl.commonbasiccomponents.AmountType;
import eu.nimble.service.model.ubl.commonbasiccomponents.BinaryObjectType;
import eu.nimble.service.model.ubl.commonbasiccomponents.CodeType;
import eu.nimble.service.model.ubl.commonbasiccomponents.QuantityType;
import eu.nimble.utility.country.CountryUtil;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.openxml4j.opc.OPCPackage;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

import static eu.nimble.service.catalogue.template.TemplateConfig.*;

/**
 * Created by suat on 04-Sep-17.
 */
public class TemplateParser {
    private static final Logger logger = LoggerFactory.getLogger(TemplateParser.class);

    private PartyType party;
    private Workbook wb;
    private String defaultLanguage = "en";

    static Map<String, Integer> defaultVats = new HashMap<>();

    static {
        defaultVats.put("Germany", 19);
        defaultVats.put("Italy", 22);
        defaultVats.put("Spain", 21);
        defaultVats.put("Sweden", 21);
    }

    public TemplateParser(PartyType party) {
        this.party = party;
    }

    public List<CatalogueLineType> getCatalogueLines(InputStream catalogueTemplate, Boolean includeVat) throws TemplateParseException, InvalidCategoryException {
        OPCPackage pkg = null;
        try {
            pkg = OPCPackage.open(catalogueTemplate);
            wb = new XSSFWorkbook(pkg);
        } catch (InvalidFormatException e) {
            throw new TemplateParseException(NimbleExceptionMessageCode.BAD_REQUEST_INVALID_TEMPLATE_FORMAT.toString(), e);
        } catch (IOException e) {
            throw new TemplateParseException(NimbleExceptionMessageCode.INTERNAL_SERVER_ERROR_READ_TEMPLATE.toString(), e);
        } finally {
            if (pkg != null) {
                try {
                    pkg.close();
                } catch (IOException e) {
                    logger.warn("Failed to close the OPC Package", e);
                }
            }
        }

        List<CatalogueLineType> results = parseProductPropertiesTab();
        parseTermsTab(results, includeVat);

        return results;
    }

    private List<CatalogueLineType> parseProductPropertiesTab() throws TemplateParseException, InvalidCategoryException {
        // get template language
        Sheet metadataTab = wb.getSheet(TemplateConfig.TEMPLATE_TAB_METADATA);
        Row metadataRow = metadataTab.getRow(2);
        if (metadataRow != null) {
            Cell cell = TemplateGenerator.getCellWithMissingCellPolicy(metadataRow, 0);
            if (cell != null) {
                defaultLanguage = TemplateGenerator.getCellStringValue(metadataTab.getRow(2).getCell(0));
            }
        }
        if (defaultLanguage.trim().equals("")) {
            defaultLanguage = "en";
        }

        Sheet productPropertiesTab = wb.getSheet(TemplateGenerator.getSheetName(TemplateTextCode.TEMPLATE_TAB_PRODUCT_PROPERTIES.toString(), defaultLanguage));
        List<Category> categories = getTemplateCategories(metadataTab);
        List<CatalogueLineType> results = new ArrayList<>();

        // identifiers of the products in template
        List<String> productIds = new ArrayList<>();

        int catalogSize = productPropertiesTab.getLastRowNum();
        // parse the sheet starting from the first editable row
        for (int rowNum = FIRST_EDITABLE_ROW_INDEX; rowNum <= catalogSize; rowNum++) {
            CatalogueLineType clt = new CatalogueLineType();
            GoodsItemType goodsItem = new GoodsItemType();
            ItemType item = new ItemType();
            List<CommodityClassificationType> classifications = new ArrayList<>();
            List<ItemPropertyType> itemProperties = new ArrayList<>();
            item.setManufacturerParty(party);
            goodsItem.setItem(item);
            clt.setGoodsItem(goodsItem);
            item.setCommodityClassification(classifications);
            item.setAdditionalItemProperty(itemProperties);
            results.add(clt);

            classifications.addAll(getCommodityClassification(categories));
            parseFixedProperties(productPropertiesTab, rowNum, clt);
            itemProperties.addAll(getCategoryRelatedItemProperties(categories, rowNum));
            itemProperties.addAll(0, getCustomItemProperties(categories, rowNum));

            String productId = item.getManufacturersItemIdentification().getID();
            // throw an exception if the same id is used for multiple products
            if (productIds.contains(productId)) {
                throw new TemplateParseException(NimbleExceptionMessageCode.BAD_REQUEST_MULTIPLE_PRODUCTS_IN_TEMPLATE.toString(), Arrays.asList(productId));
            }
            productIds.add(productId);
        }

        return results;
    }

    private List<CommodityClassificationType> getCommodityClassification(List<Category> categories) {
        List<CommodityClassificationType> classifications = new ArrayList<>();
        for (Category category : categories) {
            CommodityClassificationType classification = new CommodityClassificationType();
            CodeType classificationCode = new CodeType();
            classificationCode.setValue(category.getId());
            classificationCode.setName(category.getPreferredName(defaultLanguage));
            classificationCode.setListID(category.getTaxonomyId());
            classificationCode.setURI(category.getCategoryUri());
            classification.setItemClassificationCode(classificationCode);
            classifications.add(classification);
        }
        return classifications;
    }

    private List<ItemPropertyType> getCategoryRelatedItemProperties(List<Category> categories, int rowIndex) throws TemplateParseException {
        Sheet productPropertiesTab = wb.getSheet(TemplateGenerator.getSheetName(TemplateTextCode.TEMPLATE_TAB_PRODUCT_PROPERTIES.toString(), defaultLanguage));
        List<ItemPropertyType> additionalItemProperties = new ArrayList<>();
        for (Category category : categories) {
            if (category.getProperties() != null) {
                for (Property property : category.getProperties()) {
                    // check unit
                    // if the user provided unit for a number data type
                    Integer columnIndex = TemplateGenerator.findCellIndexForProperty(productPropertiesTab, property.getPreferredName(), category.getPreferredName());
                    if (columnIndex == null) {
                        continue;
                    }

                    Row propertyRow = productPropertiesTab.getRow(rowIndex);
                    Cell cell = TemplateGenerator.getCellWithMissingCellPolicy(propertyRow, columnIndex);
                    // make sure that mandatory properties are filled
                    if (property.getRequired() != null && property.getRequired() && TemplateGenerator.getCellStringValue(cell).contentEquals("")) {
                        throw new TemplateParseException(NimbleExceptionMessageCode.BAD_REQUEST_MISSING_REQUIRED_PROPERTIES.toString(), Arrays.asList(property.getPreferredName(defaultLanguage)));
                    }
                    // for Quantity properties, get the cell containing the unit information
                    Cell unitCell = null;
                    if (property.getDataType().contentEquals(TEMPLATE_DATA_TYPE_QUANTITY)) {
                        unitCell = TemplateGenerator.getCellWithMissingCellPolicy(propertyRow, columnIndex + 1);
                    }
                    List<Object> values = (List<Object>) parseCell(productPropertiesTab, cell, unitCell, property.getPreferredName(defaultLanguage), property.getDataType(), true);
                    if (values.isEmpty()) {
                        continue;
                    }
                    ItemPropertyType itemProp = getItemPropertyFromCategoryProperty(category, property, values);
                    additionalItemProperties.add(itemProp);
                }
            }
        }

        return additionalItemProperties;
    }


    private ItemPropertyType getItemPropertyFromCategoryProperty(Category category, Property property, Object values) throws TemplateParseException {
        ItemPropertyType itemProp = new ItemPropertyType();
        CodeType associatedClassificationCode = new CodeType();
        itemProp.setItemClassificationCode(associatedClassificationCode);

        // copy names of Property to itemProp
        for (TextType textType : property.getPreferredName()) {
            TextType text = new TextType();
            text.setLanguageID(textType.getLanguageID());
            text.setValue(textType.getValue());
            itemProp.getName().add(text);
        }

        String valueQualifier = TemplateGenerator.normalizeDataTypeForTemplate(property.getDataType().toUpperCase());
        itemProp.setValueQualifier(property.getDataType());

        if (category != null) {
            itemProp.setID(property.getId());
            itemProp.setURI(property.getUri());
            associatedClassificationCode.setValue(category.getId());
            associatedClassificationCode.setName(category.getPreferredName(defaultLanguage));
            associatedClassificationCode.setListID(category.getTaxonomyId());
            associatedClassificationCode.setURI(category.getCategoryUri());
        } else {
            itemProp.setID(UUID.randomUUID().toString());
            associatedClassificationCode.setListID("Custom");
        }

        if (valueQualifier.contentEquals(TEMPLATE_DATA_TYPE_NUMBER)) {
            itemProp.setValueDecimal((List<BigDecimal>) values);

        } else if (valueQualifier.contentEquals(TEMPLATE_DATA_TYPE_FILE)) {
            itemProp.setValueBinary((List<BinaryObjectType>) values);

        } else if (valueQualifier.contentEquals(TEMPLATE_DATA_TYPE_BOOLEAN)) {
            List<Boolean> bools = (List<Boolean>) values;
            List<TextType> stringVals = new ArrayList<>();
            for (Boolean value : bools) {
                TextType booleanText = new TextType();
                booleanText.setValue(value.toString());
                booleanText.setLanguageID("en");
                stringVals.add(booleanText);
            }
            itemProp.setValue(stringVals);

        } else if (valueQualifier.contentEquals(TEMPLATE_DATA_TYPE_QUANTITY)) {
            List<QuantityType> quantities = (List<QuantityType>) values;
            itemProp.setValueQuantity(quantities);

        } else {
            for (TextType text : (List<TextType>) values) {
                itemProp.getValue().add(text);
            }
        }

        valueQualifier = TemplateGenerator.denormalizeDataTypeFromTemplate(valueQualifier);
        itemProp.setValueQualifier(valueQualifier);

        return itemProp;
    }

    private List<ItemPropertyType> getCustomItemProperties(List<Category> categories, int rowIndex) throws TemplateParseException {
        Sheet productPropertiesTab = wb.getSheet(TemplateGenerator.getSheetName(TemplateTextCode.TEMPLATE_TAB_PRODUCT_PROPERTIES.toString(), defaultLanguage));
        List<ItemPropertyType> itemProperties = new ArrayList<>();

        // find the offset for the custom properties
        int totalCategoryPropertyNumber = 0;
        for (Category category : categories) {
            List<Property> categoryProperties = TemplateGenerator.getPropertiesToBeIncludedInTemplate(category);
            for (Property property : categoryProperties) {
                // for quantity properties, since there are two columns (value and unit), we should increment total property number by two
                if (property.getDataType().contentEquals(TEMPLATE_DATA_TYPE_QUANTITY)) {
                    totalCategoryPropertyNumber++;
                }
                totalCategoryPropertyNumber++;
            }
        }
        int fixedPropNumber = TemplateConfig.getFixedPropertiesForProductPropertyTab(defaultLanguage).size() + 4;
        int customPropertyNum = productPropertiesTab.getRow(1).getLastCellNum() - (totalCategoryPropertyNumber + fixedPropNumber + 1);
        int columnIndex = 1 + fixedPropNumber + totalCategoryPropertyNumber;

        // traverse the custom properties
        for (int i = 0; i < customPropertyNum; i++) {

            // create a temporary property using the information regarding the custom property
            Row row = productPropertiesTab.getRow(1);
            List<TextType> propertyNames = new ArrayList<>();
            List<String> propertyNamesString = parseMultiValues(TemplateGenerator.getCellWithMissingCellPolicy(row, columnIndex));
            for (String propertyNameString : propertyNamesString) {
                // remove all the leading and trailing spaces
                propertyNameString = propertyNameString.trim();
                String languageId;
                String textValue;
                // if the value's length is smaller than 3, it does not have any language id
                if (propertyNameString.length() >= 3) {
                    String languageIdWithAtSign = propertyNameString.substring(propertyNameString.length() - 3);
                    // if no language id is provided, use en by default
                    if (languageIdWithAtSign.charAt(0) != '@') {
                        languageId = "en";
                        textValue = propertyNameString;
                    } else {
                        languageId = languageIdWithAtSign.substring(1);
                        textValue = propertyNameString.substring(0, propertyNameString.length() - 3);
                    }
                } else {
                    languageId = "en";
                    textValue = propertyNameString;
                }

                TextType textType = new TextType();
                textType.setLanguageID(languageId);
                textType.setValue(textValue);

                propertyNames.add(textType);
            }
            row = productPropertiesTab.getRow(2);
            String dataType = TemplateGenerator.getCellStringValue(TemplateGenerator.getCellWithMissingCellPolicy(row, columnIndex));
            // custom properties can not have TEXT data type
            if (dataType.contentEquals(TemplateConfig.TEMPLATE_DATA_TYPE_TEXT)) {
                throw new TemplateParseException(NimbleExceptionMessageCode.BAD_REQUEST_WRONG_DATA_TYPE_FOR_CUSTOM_PROPERTIES.toString());
            }
            dataType = TemplateGenerator.denormalizeDataTypeFromTemplate(dataType);

            // get the cell containing unit information
            Cell unitCell = null;
            if (dataType.contentEquals(TEMPLATE_DATA_TYPE_QUANTITY)) {
                unitCell = TemplateGenerator.getCellWithMissingCellPolicy(productPropertiesTab.getRow(rowIndex), columnIndex + 1);
                // if it is a quantity property, then we need to increase i by one
                i++;
            }

            Property property = new Property();
            property.getPreferredName().addAll(propertyNames);
            property.setDataType(dataType);

            // get the values for the custom property
            Cell cell = TemplateGenerator.getCellWithMissingCellPolicy(productPropertiesTab.getRow(rowIndex), columnIndex);
            List<Object> values = (List<Object>) parseCell(productPropertiesTab, cell, unitCell, property.getPreferredName(defaultLanguage), property.getDataType(), true);
            if (values.isEmpty()) {
                columnIndex++;
                continue;
            }
            // add the custom property to the beginning of additional item property list
            itemProperties.add(getItemPropertyFromCategoryProperty(null, property, values));
            columnIndex++;
            // for quantity properties,increment column index by one
            if (dataType.contentEquals(TEMPLATE_DATA_TYPE_QUANTITY)) {
                columnIndex++;
            }
        }

        return itemProperties;
    }

    private void parseFixedProperties(Sheet productPropertiesTab, int rowIndex, CatalogueLineType catalogueLineType) throws TemplateParseException {
        Row propertiesRow = productPropertiesTab.getRow(rowIndex);
        List<Property> properties = TemplateConfig.getFixedPropertiesForProductPropertyTab(defaultLanguage);
        int columnIndex = 1;
        for (int i = 0; i < properties.size(); i++) {
            Property property = properties.get(i);
            Cell cell = TemplateGenerator.getCellWithMissingCellPolicy(propertiesRow, columnIndex);
            if (property.getPreferredName(defaultLanguage).equals(SpringBridge.getInstance().getMessage(TemplateTextCode.TEMPLATE_PRODUCT_PROPERTIES_MANUFACTURER_ITEM_IDENTIFICATION.toString(), defaultLanguage))) {
                if (TemplateGenerator.getCellStringValue(cell).contentEquals("")) {
                    throw new TemplateParseException(NimbleExceptionMessageCode.BAD_REQUEST_NO_MANUFACTURER_ITEM_IDENTIFICATION.toString());
                }
                ItemIdentificationType itemId = new ItemIdentificationType();
                itemId.setID((String) parseCell(cell, SpringBridge.getInstance().getMessage(TemplateTextCode.TEMPLATE_PRODUCT_PROPERTIES_MANUFACTURER_ITEM_IDENTIFICATION.toString(), defaultLanguage), TemplateConfig.TEMPLATE_DATA_TYPE_TEXT, false));
                catalogueLineType.getGoodsItem().getItem().setManufacturersItemIdentification(itemId);

            } else if (property.getPreferredName(defaultLanguage).equals(SpringBridge.getInstance().getMessage(TemplateTextCode.TEMPLATE_PRODUCT_PROPERTIES_NAME.toString(), defaultLanguage))) {
                if (TemplateGenerator.getCellStringValue(cell).contentEquals("")) {
                    throw new TemplateParseException(NimbleExceptionMessageCode.BAD_REQUEST_NO_NAME_FOR_ITEM.toString(), Arrays.asList(catalogueLineType.getGoodsItem().getItem().getManufacturersItemIdentification().getID()));
                }
                List<TextType> productNames = (List<TextType>) parseCell(cell, SpringBridge.getInstance().getMessage(TemplateTextCode.TEMPLATE_PRODUCT_PROPERTIES_NAME.toString(), defaultLanguage), TEMPLATE_DATA_TYPE_MULTILINGUAL_TEXT, true);

                for (TextType productName : productNames) {
                    catalogueLineType.getGoodsItem().getItem().getName().add(productName);
                }

            } else if (property.getPreferredName(defaultLanguage).equals(SpringBridge.getInstance().getMessage(TemplateTextCode.TEMPLATE_PRODUCT_PROPERTIES_DESCRIPTION.toString(), defaultLanguage))) {
                List<TextType> productDescriptions = (List<TextType>) parseCell(cell, SpringBridge.getInstance().getMessage(TemplateTextCode.TEMPLATE_PRODUCT_PROPERTIES_DESCRIPTION.toString(), defaultLanguage), TEMPLATE_DATA_TYPE_MULTILINGUAL_TEXT, true);
                // be sure that each item name has a corresponding item description
                for (TextType itemName : catalogueLineType.getGoodsItem().getItem().getName()) {
                    boolean descriptionFound = false;
                    for (TextType itemDescription : productDescriptions) {
                        // there is a description for the name
                        if (itemName.getLanguageID().contentEquals(itemDescription.getLanguageID())) {
                            descriptionFound = true;
                            catalogueLineType.getGoodsItem().getItem().getDescription().add(itemDescription);
                            productDescriptions.remove(itemDescription);
                            break;
                        }
                    }
                    // there is no description for the name
                    if (!descriptionFound) {
                        TextType text = new TextType();
                        text.setLanguageID(itemName.getLanguageID());
                        catalogueLineType.getGoodsItem().getItem().getDescription().add(text);
                    }
                }
                // add name-description pair for the remaining descriptions
                if (productDescriptions.size() > 0) {
                    for (TextType itemDescription : productDescriptions) {
                        // add description
                        TextType description = new TextType();
                        description.setValue(itemDescription.getValue());
                        description.setLanguageID(itemDescription.getLanguageID());
                        catalogueLineType.getGoodsItem().getItem().getDescription().add(description);
                        // create a name
                        TextType name = new TextType();
                        name.setLanguageID(itemDescription.getLanguageID());
                        catalogueLineType.getGoodsItem().getItem().getName().add(name);
                    }
                }
            }  /*else if (property.getPreferredName().equals(TemplateConfig.TEMPLATE_PRODUCT_PROPERTIES_PRODUCT_DATA_SHEET)) {
                List<BinaryObjectType> documents = (List<BinaryObjectType>) parseCell(cell, TEMPLATE_DATA_TYPE_FILE, true);
                List<DocumentReferenceType> docRefs = new ArrayList<>();
                for (BinaryObjectType document : documents) {
                    DocumentReferenceType docRef = new DocumentReferenceType();
                    AttachmentType file = new AttachmentType();
                    file.setEmbeddedDocumentBinaryObject(document);
                    docRef.setAttachment(file);
                    docRefs.add(docRef);
                }
                item.setProductDataSheet(docRefs);
            } else if (property.getPreferredName().equals(TemplateConfig.TEMPLATE_PRODUCT_PROPERTIES_PRODUCT_SAFETY_SHEET)) {
                List<BinaryObjectType> documents = (List<BinaryObjectType>) parseCell(cell, TEMPLATE_DATA_TYPE_FILE, true);
                List<DocumentReferenceType> docRefs = new ArrayList<>();
                for (BinaryObjectType document : documents) {
                    DocumentReferenceType docRef = new DocumentReferenceType();
                    AttachmentType file = new AttachmentType();
                    file.setEmbeddedDocumentBinaryObject(document);
                    docRef.setAttachment(file);
                    docRefs.add(docRef);
                }
                item.setSafetyDataSheet(docRefs);
            }*/ else if (property.getPreferredName(defaultLanguage).equals(SpringBridge.getInstance().getMessage(TemplateTextCode.TEMPLATE_PRODUCT_PROPERTIES_WIDTH.toString(), defaultLanguage))) {
                Cell unitCell = TemplateGenerator.getCellWithMissingCellPolicy(propertiesRow, ++columnIndex);
                // just to initialize the dimension array
                catalogueLineType.getGoodsItem().getItem().getDimension();
                List<QuantityType> widths;
                try {
                    widths = (List<QuantityType>) parseCell(productPropertiesTab, cell, unitCell, SpringBridge.getInstance().getMessage(TemplateTextCode.TEMPLATE_PRODUCT_PROPERTIES_WIDTH.toString(), defaultLanguage), TEMPLATE_DATA_TYPE_QUANTITY, true);
                } catch (TemplateParseException e) {
                    throw new TemplateParseException(NimbleExceptionMessageCode.BAD_REQUEST_INVALID_WIDTH_DIMENSION.toString(), e);
                }
                for (QuantityType width : widths) {
                    DimensionType dimension = new DimensionType();
                    dimension.setAttributeID("Width");
                    dimension.setMeasure(width);
                    catalogueLineType.getGoodsItem().getItem().getDimension().add(dimension);
                }

            } else if (property.getPreferredName(defaultLanguage).equals(SpringBridge.getInstance().getMessage(TemplateTextCode.TEMPLATE_PRODUCT_PROPERTIES_LENGTH.toString(), defaultLanguage))) {
                Cell unitCell = TemplateGenerator.getCellWithMissingCellPolicy(propertiesRow, ++columnIndex);
                List<QuantityType> lengths;
                try {
                    lengths = (List<QuantityType>) parseCell(productPropertiesTab, cell, unitCell, SpringBridge.getInstance().getMessage(TemplateTextCode.TEMPLATE_PRODUCT_PROPERTIES_LENGTH.toString(), defaultLanguage), TEMPLATE_DATA_TYPE_QUANTITY, true);
                } catch (TemplateParseException e) {
                    throw new TemplateParseException(NimbleExceptionMessageCode.BAD_REQUEST_INVALID_LENGTH_DIMENSION.toString(), e);
                }
                for (QuantityType width : lengths) {
                    DimensionType dimension = new DimensionType();
                    dimension.setAttributeID("Length");
                    dimension.setMeasure(width);
                    catalogueLineType.getGoodsItem().getItem().getDimension().add(dimension);
                }

            } else if (property.getPreferredName(defaultLanguage).equals(SpringBridge.getInstance().getMessage(TemplateTextCode.TEMPLATE_PRODUCT_PROPERTIES_HEIGHT.toString(), defaultLanguage))) {
                Cell unitCell = TemplateGenerator.getCellWithMissingCellPolicy(propertiesRow, ++columnIndex);
                List<QuantityType> widths;
                try {
                    widths = (List<QuantityType>) parseCell(productPropertiesTab, cell, unitCell, SpringBridge.getInstance().getMessage(TemplateTextCode.TEMPLATE_PRODUCT_PROPERTIES_HEIGHT.toString(), defaultLanguage), TEMPLATE_DATA_TYPE_QUANTITY, true);
                } catch (TemplateParseException e) {
                    throw new TemplateParseException(NimbleExceptionMessageCode.BAD_REQUEST_INVALID_HEIGHT_DIMENSION.toString(), e);
                }
                for (QuantityType width : widths) {
                    DimensionType dimension = new DimensionType();
                    dimension.setAttributeID("Height");
                    dimension.setMeasure(width);
                    catalogueLineType.getGoodsItem().getItem().getDimension().add(dimension);
                }
            } else if (property.getPreferredName(defaultLanguage).equals(SpringBridge.getInstance().getMessage(TemplateTextCode.TEMPLATE_PRODUCT_PROPERTIES_WEIGHT.toString(), defaultLanguage))) {
                Cell unitCell = TemplateGenerator.getCellWithMissingCellPolicy(propertiesRow, ++columnIndex);
                List<QuantityType> widths;
                try {
                    widths = (List<QuantityType>) parseCell(productPropertiesTab, cell, unitCell, SpringBridge.getInstance().getMessage(TemplateTextCode.TEMPLATE_PRODUCT_PROPERTIES_WEIGHT.toString(), defaultLanguage), TEMPLATE_DATA_TYPE_QUANTITY, true);
                } catch (TemplateParseException e) {
                    throw new TemplateParseException(NimbleExceptionMessageCode.BAD_REQUEST_INVALID_WEIGHT_DIMENSION.toString(), e);
                }
                for (QuantityType width : widths) {
                    DimensionType dimension = new DimensionType();
                    dimension.setAttributeID("Weight");
                    dimension.setMeasure(width);
                    catalogueLineType.getGoodsItem().getItem().getDimension().add(dimension);
                }
            }
            columnIndex++;
        }
    }

    private void parseTermsTab(List<CatalogueLineType> catalogueLines, Boolean includeVat) throws TemplateParseException {
        Sheet termsTab = wb.getSheet(TemplateGenerator.getSheetName(TemplateTextCode.TEMPLATE_TAB_TRADING_DELIVERY_TERMS.toString(), defaultLanguage));
        for (CatalogueLineType catalogueLine : catalogueLines) {
            ItemType item = catalogueLine.getGoodsItem().getItem();
            // find row corresponding to the provided item
            int rowIndex = FIRST_EDITABLE_ROW_INDEX;
            Row row = null;
            Cell cell;
            String value;
            for (; rowIndex <= termsTab.getLastRowNum(); rowIndex++) {
                row = termsTab.getRow(rowIndex);
                cell = row.getCell(1);
                if (cell == null) {
                    row = null;
                    continue;
                } else {
                    if (TemplateGenerator.getCellStringValue(cell).contentEquals(item.getManufacturersItemIdentification().getID())) {
                        break;
                    } else {
                        row = null;
                    }
                }
            }
            if (row == null) {
                throw new TemplateParseException(NimbleExceptionMessageCode.BAD_REQUEST_NO_TRADING_DELIVERY_TERMS_FOR_ITEM.toString(), Arrays.asList(LanguageUtil.getValue(item.getName(), defaultLanguage), item.getManufacturersItemIdentification().getID()));
            }

            // parse the terms
            int columnIndex = 1;
            List<Property> termRelatedProperties = TemplateConfig.getFixedPropertiesForTermsTab(defaultLanguage);
            for (Property property : termRelatedProperties) {
                cell = TemplateGenerator.getCellWithMissingCellPolicy(row, columnIndex);
                if (property.getPreferredName(defaultLanguage).contentEquals(SpringBridge.getInstance().getMessage(TemplateTextCode.TEMPLATE_TRADING_DELIVERY_PRICE_AMOUNT.toString(), defaultLanguage))) {
                    ItemLocationQuantityType itemLocationQuantity = new ItemLocationQuantityType();
                    catalogueLine.setRequiredItemLocationQuantity(itemLocationQuantity);
                    PriceType price = new PriceType();
                    itemLocationQuantity.setPrice(price);
                    AmountType amount = new AmountType();
                    price.setPriceAmount(amount);
                    // parse price amount
                    Boolean priceNotExist = TemplateGenerator.getCellStringValue(cell).contentEquals("");

                    // parse currency
                    Cell tmp = TemplateGenerator.getCellWithMissingCellPolicy(row, ++columnIndex);
                    Boolean currencyNotExist = (tmp == null || TemplateGenerator.getCellStringValue(tmp).contentEquals(""));

                    if ((priceNotExist && !currencyNotExist) || (!priceNotExist && currencyNotExist)) {
                        throw new TemplateParseException(NimbleExceptionMessageCode.BAD_REQUEST_AMOUNT_CURRENCY_REQUIRED_FOR_PRICE.toString(), Arrays.asList(LanguageUtil.getValue(item.getName(), defaultLanguage), item.getManufacturersItemIdentification().getID()));
                    }

                    amount.setValue((BigDecimal) parseCell(cell, SpringBridge.getInstance().getMessage(TemplateTextCode.TEMPLATE_TRADING_DELIVERY_PRICE_AMOUNT.toString(), defaultLanguage), TEMPLATE_DATA_TYPE_NUMBER, false));

                    value = TemplateGenerator.getCellStringValue(tmp);
                    amount.setCurrencyID(value);

                } else if (property.getPreferredName(defaultLanguage).contentEquals(SpringBridge.getInstance().getMessage(TemplateTextCode.TEMPLATE_TRADING_DELIVERY_PRICE_BASE_QUANTITY.toString(), defaultLanguage))) {
                    Cell unitCell = TemplateGenerator.getCellWithMissingCellPolicy(row, ++columnIndex);
                    QuantityType baseQuantity = (QuantityType) parseCell(termsTab, cell, unitCell, SpringBridge.getInstance().getMessage(TemplateTextCode.TEMPLATE_TRADING_DELIVERY_PRICE_BASE_QUANTITY.toString(), defaultLanguage), TEMPLATE_DATA_TYPE_QUANTITY, false);
                    if (baseQuantity == null) {
                        baseQuantity = new QuantityType();
                    }
                    catalogueLine.getRequiredItemLocationQuantity().getPrice().setBaseQuantity(baseQuantity);

                } else if (property.getPreferredName(defaultLanguage).contentEquals(SpringBridge.getInstance().getMessage(TemplateTextCode.TEMPLATE_TRADING_DELIVERY_MINIMUM_ORDER_QUANTITY.toString(), defaultLanguage))) {
                    Cell unitCell = TemplateGenerator.getCellWithMissingCellPolicy(row, ++columnIndex);
                    QuantityType minimumOrderQuantity = (QuantityType) parseCell(termsTab, cell, unitCell, SpringBridge.getInstance().getMessage(TemplateTextCode.TEMPLATE_TRADING_DELIVERY_MINIMUM_ORDER_QUANTITY.toString(), defaultLanguage), TEMPLATE_DATA_TYPE_QUANTITY, false);
                    if (minimumOrderQuantity != null) {
                        if (minimumOrderQuantity.getUnitCode() == null) {
                            throw new TemplateParseException(NimbleExceptionMessageCode.BAD_REQUEST_UNIT_REQUIRED_FOR_MINIMUM_ORDER_QUANTITY.toString(), Arrays.asList(LanguageUtil.getValue(item.getName(), defaultLanguage), item.getManufacturersItemIdentification().getID()));
                        }
                    } else {
                        minimumOrderQuantity = new QuantityType();
                    }
                    catalogueLine.setMinimumOrderQuantity(minimumOrderQuantity);

                } else if (property.getPreferredName(defaultLanguage).contentEquals(SpringBridge.getInstance().getMessage(TemplateTextCode.TEMPLATE_TRADING_DELIVERY_FREE_SAMPLE.toString(), defaultLanguage))) {
                    catalogueLine.setFreeOfChargeIndicator((Boolean) parseCell(cell, SpringBridge.getInstance().getMessage(TemplateTextCode.TEMPLATE_TRADING_DELIVERY_FREE_SAMPLE.toString(), defaultLanguage), TEMPLATE_DATA_TYPE_BOOLEAN, false));

                } else if (property.getPreferredName(defaultLanguage).contentEquals(SpringBridge.getInstance().getMessage(TemplateTextCode.TEMPLATE_TRADING_DELIVERY_WARRANTY_VALIDITY_PERIOD.toString(), defaultLanguage))) {
                    Cell unitCell = TemplateGenerator.getCellWithMissingCellPolicy(row, ++columnIndex);
                    QuantityType warrantyValidityPeriod = (QuantityType) parseCell(termsTab, cell, unitCell, SpringBridge.getInstance().getMessage(TemplateTextCode.TEMPLATE_TRADING_DELIVERY_WARRANTY_VALIDITY_PERIOD.toString(), defaultLanguage), TEMPLATE_DATA_TYPE_QUANTITY, false);
                    if (warrantyValidityPeriod != null) {
                        if (warrantyValidityPeriod.getUnitCode() == null) {
                            throw new TemplateParseException(NimbleExceptionMessageCode.BAD_REQUEST_UNIT_REQUIRED_FOR_WARRANTY_PERIOD.toString(), Arrays.asList(LanguageUtil.getValue(item.getName(), defaultLanguage), item.getManufacturersItemIdentification().getID()));
                        }
                    } else {
                        warrantyValidityPeriod = new QuantityType();
                    }
                    PeriodType period = new PeriodType();
                    catalogueLine.setWarrantyValidityPeriod(period);
                    period.setDurationMeasure(warrantyValidityPeriod);

                } else if (property.getPreferredName(defaultLanguage).contentEquals(SpringBridge.getInstance().getMessage(TemplateTextCode.TEMPLATE_TRADING_DELIVERY_WARRANTY_INFORMATION.toString(), defaultLanguage))) {
                    if (cell != null) {
                        List<String> values = (List<String>) parseCell(cell, SpringBridge.getInstance().getMessage(TemplateTextCode.TEMPLATE_TRADING_DELIVERY_WARRANTY_INFORMATION.toString(), defaultLanguage), TemplateConfig.TEMPLATE_DATA_TYPE_TEXT, true);
                        if (values.size() > 0) {
                            catalogueLine.getWarrantyInformation().addAll(values);
                        }
                    }

                } else if (property.getPreferredName(defaultLanguage).contentEquals(SpringBridge.getInstance().getMessage(TemplateTextCode.TEMPLATE_TRADING_DELIVERY_INCOTERMS.toString(), defaultLanguage))) {
                    DeliveryTermsType deliveryTerms = new DeliveryTermsType();
                    catalogueLine.getGoodsItem().setDeliveryTerms(deliveryTerms);
                    value = (String) parseCell(cell, SpringBridge.getInstance().getMessage(TemplateTextCode.TEMPLATE_TRADING_DELIVERY_INCOTERMS.toString(), defaultLanguage), TEMPLATE_DATA_TYPE_TEXT, false);
                    if (value != null) {
                        value = value.replace("_", " ");
                    }
                    catalogueLine.getGoodsItem().getDeliveryTerms().setIncoterms(value);

                } else if (property.getPreferredName(defaultLanguage).contentEquals(SpringBridge.getInstance().getMessage(TemplateTextCode.TEMPLATE_TRADING_DELIVERY_SPECIAL_TERMS.toString(), defaultLanguage))) {
                    List<String> values = (List<String>) parseCell(cell, SpringBridge.getInstance().getMessage(TemplateTextCode.TEMPLATE_TRADING_DELIVERY_SPECIAL_TERMS.toString(), defaultLanguage), TemplateConfig.TEMPLATE_DATA_TYPE_TEXT, true);

                    for (String stvalue : values) {
                        TextType textType = new TextType();
                        textType.setLanguageID(defaultLanguage);
                        textType.setValue(stvalue);

                        catalogueLine.getGoodsItem().getDeliveryTerms().getSpecialTerms().add(textType);
                    }
                } else if (property.getPreferredName(defaultLanguage).contentEquals(SpringBridge.getInstance().getMessage(TemplateTextCode.TEMPLATE_TRADING_DELIVERY_ESTIMATED_DELIVERY_PERIOD.toString(), defaultLanguage))) {
                    Cell unitCell = TemplateGenerator.getCellWithMissingCellPolicy(row, ++columnIndex);
                    QuantityType estimatedDeliveryQuantity = (QuantityType) parseCell(termsTab, cell, unitCell, SpringBridge.getInstance().getMessage(TemplateTextCode.TEMPLATE_TRADING_DELIVERY_ESTIMATED_DELIVERY_PERIOD.toString(), defaultLanguage), TEMPLATE_DATA_TYPE_QUANTITY, false);
                    if (estimatedDeliveryQuantity != null) {
                        if (estimatedDeliveryQuantity.getUnitCode() == null) {
                            throw new TemplateParseException(NimbleExceptionMessageCode.BAD_REQUEST_UNIT_REQUIRED_FOR_DELIVERY_PERIOD.toString(), Arrays.asList(LanguageUtil.getValue(item.getName(), defaultLanguage), item.getManufacturersItemIdentification().getID()));
                        }

                    } else {
                        estimatedDeliveryQuantity = new QuantityType();
                    }

                    PeriodType period = new PeriodType();
                    catalogueLine.getGoodsItem().getDeliveryTerms().setEstimatedDeliveryPeriod(period);
                    period.setDurationMeasure(estimatedDeliveryQuantity);

                } else if (property.getPreferredName(defaultLanguage).contentEquals(SpringBridge.getInstance().getMessage(TemplateTextCode.TEMPLATE_TRADING_DELIVERY_TRANSPORT_MODE.toString(), defaultLanguage))) {
                    cell = TemplateGenerator.getCellWithMissingCellPolicy(row, columnIndex);
                    if (cell != null) {
                        value = TemplateGenerator.getCellStringValue(cell);
                        CodeType transportModeCode = new CodeType();
                        transportModeCode.setValue(value);
                        catalogueLine.getGoodsItem().getDeliveryTerms().setTransportModeCode(transportModeCode);
                    }

                } else if (property.getPreferredName(defaultLanguage).contentEquals(SpringBridge.getInstance().getMessage(TemplateTextCode.TEMPLATE_TRADING_DELIVERY_PACKAGING_TYPE.toString(), defaultLanguage))) {
                    cell = TemplateGenerator.getCellWithMissingCellPolicy(row, columnIndex);
                    CodeType packagingType = new CodeType();
                    if (cell != null) {
                        value = TemplateGenerator.getCellStringValue(cell);
                        packagingType.setValue(value);
                    }

                    PackageType packaging = new PackageType();
                    catalogueLine.getGoodsItem().setContainingPackage(packaging);
                    packaging.setPackagingTypeCode(packagingType);

                } else if (property.getPreferredName(defaultLanguage).contentEquals(SpringBridge.getInstance().getMessage(TemplateTextCode.TEMPLATE_TRADING_DELIVERY_PACKAGE_QUANTITY.toString(), defaultLanguage))) {
                    Cell unitCell = TemplateGenerator.getCellWithMissingCellPolicy(row, ++columnIndex);
                    QuantityType packageQuantity = (QuantityType) parseCell(termsTab, cell, unitCell, SpringBridge.getInstance().getMessage(TemplateTextCode.TEMPLATE_TRADING_DELIVERY_PACKAGE_QUANTITY.toString(), defaultLanguage), TEMPLATE_DATA_TYPE_QUANTITY, false);
                    if (packageQuantity != null) {
                        if (packageQuantity.getUnitCode() == null) {
                            throw new TemplateParseException(NimbleExceptionMessageCode.BAD_REQUEST_UNIT_REQUIRED_FOR_PACKAGE_QUANTITY.toString(), Arrays.asList(LanguageUtil.getValue(item.getName(), defaultLanguage), item.getManufacturersItemIdentification().getID()));
                        }
                    } else {
                        packageQuantity = new QuantityType();
                    }
                    catalogueLine.getGoodsItem().getContainingPackage().setQuantity(packageQuantity);
                } else if (property.getPreferredName(defaultLanguage).contentEquals(SpringBridge.getInstance().getMessage(TemplateTextCode.TEMPLATE_TRADING_DELIVERY_CUSTOMIZABLE.toString(), defaultLanguage))) {
                    catalogueLine.getGoodsItem().getItem().setCustomizable((Boolean) parseCell(cell, SpringBridge.getInstance().getMessage(TemplateTextCode.TEMPLATE_TRADING_DELIVERY_CUSTOMIZABLE.toString(), defaultLanguage), TEMPLATE_DATA_TYPE_BOOLEAN, false));
                } else if (SpringBridge.getInstance().getCatalogueServiceConfig().getSparePartEnabled() && property.getPreferredName(defaultLanguage).contentEquals(SpringBridge.getInstance().getMessage(TemplateTextCode.TEMPLATE_TRADING_DELIVERY_SPARE_PART.toString(), defaultLanguage))) {
                    catalogueLine.getGoodsItem().getItem().setSparePart((Boolean) parseCell(cell, SpringBridge.getInstance().getMessage(TemplateTextCode.TEMPLATE_TRADING_DELIVERY_SPARE_PART.toString(), defaultLanguage), TEMPLATE_DATA_TYPE_BOOLEAN, false));
                }
                columnIndex++;
            }

            // price base quantity should be provided if price amount is provided
            if (catalogueLine.getRequiredItemLocationQuantity().getPrice().getPriceAmount().getValue() != null && catalogueLine.getRequiredItemLocationQuantity().getPrice().getBaseQuantity().getValue() == null) {
                throw new TemplateParseException(NimbleExceptionMessageCode.BAD_REQUEST_BASE_QUANTITY_REQUIRED.toString(), Arrays.asList(catalogueLine.getGoodsItem().getItem().getManufacturersItemIdentification().getID()));
            }
            // unit must be the same for price base quantity and minimum order quantity
            if (catalogueLine.getRequiredItemLocationQuantity().getPrice().getBaseQuantity().getUnitCode() != null && catalogueLine.getMinimumOrderQuantity().getUnitCode() != null &&
                    !catalogueLine.getRequiredItemLocationQuantity().getPrice().getBaseQuantity().getUnitCode().contentEquals(catalogueLine.getMinimumOrderQuantity().getUnitCode())) {
                throw new TemplateParseException(NimbleExceptionMessageCode.BAD_REQUEST_SAME_UNIT_REQUIRED_FOR_BASE_AND_MINIMUM_ORDER_QUANTITIES.toString(), Arrays.asList(catalogueLine.getGoodsItem().getItem().getManufacturersItemIdentification().getID()));
            }
        }

        // create vats for catalogue lines
        createVatsForCatalogueLines(catalogueLines, includeVat);
    }

    private void createVatsForCatalogueLines(List<CatalogueLineType> catalogueLines, Boolean includeVat) {
        for (CatalogueLineType line : catalogueLines) {
            if (line.getRequiredItemLocationQuantity().getApplicableTaxCategory() != null &&
                    line.getRequiredItemLocationQuantity().getApplicableTaxCategory().size() > 0) {
                continue;
            }

            Integer vatRate = null;
            if (includeVat) {
                AddressType address = line.getGoodsItem().getItem().getManufacturerParty().getPostalAddress();
                if (address != null && address.getCountry() != null && address.getCountry().getIdentificationCode() != null) {
                    String country = CountryUtil.getCountryNameByISOCode(line.getGoodsItem().getItem().getManufacturerParty().getPostalAddress().getCountry().getIdentificationCode().getValue());
                    vatRate = defaultVats.get(country);
                }

                if (vatRate == null) {
                    vatRate = 20;
                }
            } else {
                vatRate = 0;
            }

            TaxCategoryType taxCategory = new TaxCategoryType();
            line.getRequiredItemLocationQuantity().getApplicableTaxCategory().add(taxCategory);
            taxCategory.setPercent(new BigDecimal(vatRate));
        }
    }

    private Object parseCell(Cell cell, String propertyName, String dataType, boolean multiValue) throws TemplateParseException {
        return parseCell(null, cell, null, propertyName, dataType, multiValue);
    }

    private Object parseCell(Sheet sheet, Cell cell, Cell adjacentCell, String propertyName, String dataType, boolean multiValue) throws TemplateParseException {
        List<String> values = parseMultiValues(cell);
        List<Object> results = new ArrayList<>();
        String normalizedDataType = TemplateGenerator.normalizeDataTypeForTemplate(dataType);
        for (String value : values) {
            if (normalizedDataType.contentEquals("BOOLEAN")) {
                results.add(parseBoolean(propertyName, value));
            } else if (normalizedDataType.compareToIgnoreCase("TEXT") == 0) {
                results.add(value);
            } else if (normalizedDataType.compareToIgnoreCase("MULTILINGUAL TEXT") == 0) {
                // remove all the leading and trailing spaces
                value = value.trim();
                String languageId;
                String textValue;
                // if the value's length is smaller than 3, it does not have any language id
                if (value.length() >= 3) {
                    String languageIdWithAtSign = value.substring(value.length() - 3);
                    // if no language id is provided, use en by default
                    if (languageIdWithAtSign.charAt(0) != '@') {
                        languageId = "en";
                        textValue = value;
                    } else {
                        languageId = languageIdWithAtSign.substring(1);
                        textValue = value.substring(0, value.length() - 3);
                    }
                } else {
                    languageId = "en";
                    textValue = value;
                }

                // create text type instance
                TextType textType = new TextType();
                textType.setValue(textValue);
                textType.setLanguageID(languageId);
                // add it to the results
                results.add(textType);
            } else if (normalizedDataType.compareToIgnoreCase("NUMBER") == 0) {
                try {
                    results.add(new BigDecimal(value));
                } catch (NumberFormatException e) {
                    //  logger.warn("Invalid value passed for number: {}", value);
                    throw new TemplateParseException(NimbleExceptionMessageCode.BAD_REQUEST_NUMBER_REQUIRED_FOR_PROPERTY.toString(), Arrays.asList(propertyName, value));
                }
            } else if (normalizedDataType.compareToIgnoreCase(TemplateConfig.TEMPLATE_DATA_TYPE_QUANTITY) == 0) {
                results.add(parseQuantity(sheet, propertyName, value, cell, adjacentCell));
            } else if (normalizedDataType.compareToIgnoreCase("FILE") == 0) {
                results.add(parseBinaryObject(value));
            }
        }

        if (multiValue == false) {
            if (results.size() > 0) {
                return results.get(0);
            } else {
                return null;
            }
        }
        return results;
    }

    private Boolean parseBoolean(String propertyName, String value) throws TemplateParseException {
        if (!(value.compareToIgnoreCase("TRUE") == 0 ||
                value.compareToIgnoreCase("FALSE") == 0 ||
                value.compareToIgnoreCase("YES") == 0 ||
                value.compareToIgnoreCase("NO") == 0)) {
            throw new TemplateParseException(NimbleExceptionMessageCode.BAD_REQUEST_INVALID_VALUE_FOR_BOOLEAN_PROPERTY.toString(), Arrays.asList(propertyName));
        }
        if (value.compareToIgnoreCase("YES") == 0) {
            value = "TRUE";
        }
        return new Boolean(value);
    }

    private QuantityType parseQuantity(Sheet sheet, String propertyName, String value, Cell cell, Cell adjacentCell) throws TemplateParseException {
        QuantityType quantity = new QuantityType();

        if (cell == null) {
            return null;
        }

        try {
            quantity.setValue(new BigDecimal(value));
        } catch (NumberFormatException e) {
            //  logger.warn("Invalid number passed for quantity: {}", value, e);
            //  return null;
            throw new TemplateParseException(NimbleExceptionMessageCode.BAD_REQUEST_NUMBER_REQUIRED_FOR_PROPERTY.toString(), Arrays.asList(propertyName, value));
        }

        // parse unit
        if (adjacentCell == null) {
            throw new TemplateParseException(NimbleExceptionMessageCode.BAD_REQUEST_VALUE_AND_UNIT_REQUIRED_FOR_PROPERTY.toString(), Arrays.asList(propertyName));
        } else {
            // check the data type of the quantity unit
            int columnIndex = adjacentCell.getColumnIndex();
            Row row = sheet.getRow(2);
            if (!TemplateGenerator.getCellStringValue(row.getCell(columnIndex)).contentEquals(TemplateConfig.TEMPLATE_QUANTITY_UNIT)) {
                throw new TemplateParseException(NimbleExceptionMessageCode.BAD_REQUEST_WRONG_DATA_TYPE_FOR_QUANTITY.toString());
            }

            value = TemplateGenerator.getCellStringValue(adjacentCell);
            if (value.contentEquals("")) {
                throw new TemplateParseException(NimbleExceptionMessageCode.BAD_REQUEST_VALUE_AND_UNIT_REQUIRED_FOR_PROPERTY.toString(), Arrays.asList(propertyName));
            }
        }

        quantity.setUnitCode(value);
        return quantity;
    }

    private BinaryObjectType parseBinaryObject(String filePath) throws TemplateParseException {
        BinaryObjectType binaryObject = new BinaryObjectType();
        File file = new File(filePath);
        String mimeType = "";
        try {
            mimeType = Files.probeContentType(file.toPath());
        } catch (IOException e) {
            logger.warn("Failed to determine the mimetype of the file in: {}", filePath, e);
        }
        binaryObject.setMimeCode(mimeType);
        binaryObject.setFileName(file.getName());
        try {
            byte[] srcBytes = Files.readAllBytes(Paths.get(filePath));
            binaryObject.setValue(srcBytes);
        } catch (IOException e) {
            throw new TemplateParseException(NimbleExceptionMessageCode.INTERNAL_SERVER_ERROR_GET_FILE_IN_BASE_64_ENCODING.toString(), Arrays.asList(filePath), e);
        }
        return binaryObject;
    }

    private List<String> parseMultiValues(Cell cell) {
        List<String> values = new ArrayList<>();
        if (cell == null) {
            return values;
        }

        String value = TemplateGenerator.getCellStringValue(cell);

        if (value.equals("")) {
            return values;
        } else {
            return Arrays.asList(value.split("\\|"));
        }
    }

    private List<Category> getTemplateCategories(Sheet metadataTab) throws InvalidCategoryException {
        List<Category> categories = new ArrayList<>();
        Row row = metadataTab.getRow(0);
        // if there is no category
        if (row == null) {
            return categories;
        }

        String categoryIdsStr = TemplateGenerator.getCellStringValue(metadataTab.getRow(0).getCell(0));
        String taxonomyIdsStr = TemplateGenerator.getCellStringValue(metadataTab.getRow(1).getCell(0));

        List<String> categoryIds = Arrays.asList(categoryIdsStr.split(","));
        List<String> taxonomyIds = Arrays.asList(taxonomyIdsStr.split(","));

        IndexCategoryService categoryServicecsm = SpringBridge.getInstance().getIndexCategoryService();
        for (int i = 0; i < categoryIds.size(); i++) {
            Category category = categoryServicecsm.getCategory(taxonomyIds.get(i), categoryIds.get(i));
            categories.add(category);
        }

        return categories;
    }

}