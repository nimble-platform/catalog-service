package eu.nimble.service.catalogue.impl.template;

import eu.nimble.service.catalogue.category.datamodel.Category;
import eu.nimble.service.catalogue.category.datamodel.Property;
import eu.nimble.service.catalogue.category.datamodel.Unit;
import eu.nimble.service.catalogue.exception.TemplateParseException;
import eu.nimble.service.catalogue.exception.TemplateParseException;
import eu.nimble.service.catalogue.impl.CategoryServiceManager;
import eu.nimble.service.model.ubl.commonaggregatecomponents.*;
import eu.nimble.service.model.ubl.commonbasiccomponents.AmountType;
import eu.nimble.service.model.ubl.commonbasiccomponents.BinaryObjectType;
import eu.nimble.service.model.ubl.commonbasiccomponents.CodeType;
import eu.nimble.service.model.ubl.commonbasiccomponents.QuantityType;
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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static eu.nimble.service.catalogue.impl.template.TemplateConfig.*;

/**
 * Created by suat on 04-Sep-17.
 */
public class TemplateParser {
    private static final Logger logger = LoggerFactory.getLogger(TemplateParser.class);

    private PartyType party;
    private Workbook wb;

    public TemplateParser(PartyType party) {
        this.party = party;
    }

    public List<CatalogueLineType> getCatalogueLines(InputStream catalogueTemplate) throws TemplateParseException {
        OPCPackage pkg = null;
        try {
            pkg = OPCPackage.open(catalogueTemplate);
            wb = new XSSFWorkbook(pkg);
        } catch (InvalidFormatException e) {
            throw new TemplateParseException("Invalid format for the submitted template", e);
        } catch (IOException e) {
            throw new TemplateParseException("Failed to read the submitted template", e);
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
        parseTermsTab(results);

        return results;
    }

    private List<CatalogueLineType> parseProductPropertiesTab() throws TemplateParseException {
        Sheet productPropertiesTab = wb.getSheet(TemplateConfig.TEMPLATE_TAB_PRODUCT_PROPERTIES);
        Sheet metadataTab = productPropertiesTab.getWorkbook().getSheet(TemplateConfig.TEMPLATE_TAB_METADATA);
        List<Category> categories = getTemplateCategories(metadataTab);
        List<CatalogueLineType> results = new ArrayList<>();

        int catalogSize = productPropertiesTab.getLastRowNum();
        // first four rows contains fixed values
        for (int rowNum = 4; rowNum <= catalogSize; rowNum++) {
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
            parseFixedProperties(productPropertiesTab, rowNum, item);
            itemProperties.addAll(getCategoryRelatedItemProperties(categories, rowNum));
            itemProperties.addAll(0, getCustomItemProperties(categories, rowNum));
        }

        return results;
    }

    private List<CommodityClassificationType> getCommodityClassification(List<Category> categories) {
        List<CommodityClassificationType> classifications = new ArrayList<>();
        for (Category category : categories) {
            CommodityClassificationType classification = new CommodityClassificationType();
            CodeType classificationCode = new CodeType();
            classificationCode.setValue(category.getId());
            classificationCode.setName(category.getPreferredName());
            classificationCode.setListID(category.getTaxonomyId());
            classification.setItemClassificationCode(classificationCode);
            classifications.add(classification);
        }
        return classifications;
    }

    private List<ItemPropertyType> getCategoryRelatedItemProperties(List<Category> categories, int rowIndex) throws TemplateParseException {
        Sheet productPropertiesTab = wb.getSheet(TemplateConfig.TEMPLATE_TAB_PRODUCT_PROPERTIES);
        int columnIndex = TemplateConfig.getFixedPropertiesForProductPropertyTab().size() + 1;
        List<ItemPropertyType> additionalItemProperties = new ArrayList<>();
        for (Category category : categories) {
            for (Property property : category.getProperties()) {
                Cell cell = getCellWithMissingCellPolicy(productPropertiesTab.getRow(rowIndex), columnIndex);
                List<Object> values = (List<Object>) parseCell(cell, property.getDataType(), true);
                if (values.isEmpty()) {
                    columnIndex++;
                    continue;
                }
                ItemPropertyType itemProp = getItemPropertyFromCategoryProperty(category, property, values);
                additionalItemProperties.add(itemProp);
                columnIndex++;
            }
        }

        return additionalItemProperties;
    }

    private ItemPropertyType getItemPropertyFromCategoryProperty(Category category, Property property, Object values) {
        ItemPropertyType itemProp = new ItemPropertyType();
        CodeType associatedClassificationCode = new CodeType();
        itemProp.setItemClassificationCode(associatedClassificationCode);
        itemProp.setName(property.getPreferredName());
        String valueQualifier = TemplateGenerator.normalizeDataTypeForTemplate(property.getDataType().toUpperCase());
        itemProp.setValueQualifier(property.getDataType());

        if (category != null) {
            itemProp.setID(property.getId());
            associatedClassificationCode.setValue(category.getId());
            associatedClassificationCode.setName(category.getPreferredName());
            associatedClassificationCode.setListID(category.getTaxonomyId());
        } else {
            itemProp.setID(UUID.randomUUID().toString());
            associatedClassificationCode.setListID("Custom");
            associatedClassificationCode.setListID("Custom");
            associatedClassificationCode.setListID("Custom");
        }

        if (valueQualifier.contentEquals(TEMPLATE_DATA_TYPE_NUMBER)) {
            valueQualifier = TEMPLATE_DATA_TYPE_REAL_MEASURE;
            itemProp.setValueDecimal((List<BigDecimal>) values);

        } else if (valueQualifier.contentEquals(TEMPLATE_DATA_TYPE_FILE)) {
            valueQualifier = TEMPLATE_DATA_TYPE_BINARY;
            itemProp.setValueBinary((List<BinaryObjectType>) values);

        } else if (valueQualifier.contentEquals(TEMPLATE_DATA_TYPE_BOOLEAN)) {
            List<Boolean> bools = (List<Boolean>) values;
            List<String> stringVals = new ArrayList<>();
            for (Boolean value : bools) {
                stringVals.add(value.toString());
            }
            itemProp.setValue(stringVals);

        } else if (valueQualifier.contentEquals(TEMPLATE_DATA_TYPE_QUANTITY)) {
            valueQualifier = TEMPLATE_DATA_TYPE_REAL_MEASURE;
            List<QuantityType> quantities = (List<QuantityType>) values;
            for (QuantityType quantity : quantities) {
                quantity.setUnitID(property.getUnit().getId());
            }
            itemProp.setValueQuantity(quantities);

        } else {
            valueQualifier = TEMPLATE_DATA_TYPE_STRING;
            itemProp.setValue((List<String>) values);
        }

        itemProp.setValueQualifier(valueQualifier);

        return itemProp;
    }

    private List<ItemPropertyType> getCustomItemProperties(List<Category> categories, int rowIndex) throws TemplateParseException {
        Sheet productPropertiesTab = wb.getSheet(TEMPLATE_TAB_PRODUCT_PROPERTIES);
        List<ItemPropertyType> itemProperties = new ArrayList<>();

        // find the offset for the custom properties
        int totalCategoryPropertyNumber = 0;
        for (Category category : categories) {
            totalCategoryPropertyNumber += category.getProperties().size();
        }
        int fixedPropNumber = TemplateConfig.getFixedPropertiesForProductPropertyTab().size();
        int customPropertyNum = productPropertiesTab.getRow(1).getLastCellNum() - (totalCategoryPropertyNumber + fixedPropNumber + 1);
        int columnIndex = 1 + TemplateConfig.getFixedPropertiesForProductPropertyTab().size() + totalCategoryPropertyNumber;

        // traverse the custom properties
        for (int i = 0; i < customPropertyNum; i++) {

            // create a temporary property using the information regarding the custom property
            Row row = productPropertiesTab.getRow(1);
            String propertyName = getCellWithMissingCellPolicy(row, columnIndex).getStringCellValue();
            row = productPropertiesTab.getRow(2);
            String dataType = getCellWithMissingCellPolicy(row, columnIndex).getStringCellValue();
            if (dataType.contentEquals("")) {
                dataType = "STRING";
            }
            row = productPropertiesTab.getRow(3);
            String unit = getCellWithMissingCellPolicy(row, columnIndex).getStringCellValue();

            Property property = new Property();
            property.setPreferredName(propertyName);
            property.setDataType(dataType);

            if (!unit.contentEquals("")) {
                Unit unitObj = new Unit();
                unitObj.setShortName(unit);
                property.setUnit(unitObj);
            }

            // get the values for the custom property
            Cell cell = getCellWithMissingCellPolicy(productPropertiesTab.getRow(rowIndex), columnIndex);
            Object values = parseCell(cell, property.getDataType(), true);
            // add the custom property to the beginning of additional item property list
            itemProperties.add(getItemPropertyFromCategoryProperty(null, property, values));
            columnIndex++;
        }

        return itemProperties;
    }

    private void parseFixedProperties(Sheet productPropertiesTab, int rowIndex, ItemType item) throws TemplateParseException {
        Row propertiesRow = productPropertiesTab.getRow(rowIndex);
        List<Property> properties = TemplateConfig.getFixedPropertiesForProductPropertyTab();
        for (int i = 0; i < properties.size(); i++) {
            Property property = properties.get(i);
            Cell cell = getCellWithMissingCellPolicy(propertiesRow, i + 1);
            if (property.getPreferredName().equals(TemplateConfig.TEMPLATE_PRODUCT_PROPERTIES_MANUFACTURER_ITEM_IDENTIFICATION)) {
                ItemIdentificationType itemId = new ItemIdentificationType();
                itemId.setID((String) parseCell(cell, TEMPLATE_DATA_TYPE_TEXT, false));
                item.setManufacturersItemIdentification(itemId);

            } else if (property.getPreferredName().equals(TemplateConfig.TEMPLATE_PRODUCT_PROPERTIES_NAME)) {
                item.setName((String) parseCell(cell, TEMPLATE_DATA_TYPE_TEXT, false));

            } else if (property.getPreferredName().equals(TemplateConfig.TEMPLATE_PRODUCT_PROPERTIES_DESCRIPTION)) {
                item.setDescription((String) parseCell(cell, TEMPLATE_DATA_TYPE_TEXT, false));

            } else if (property.getPreferredName().equals(TemplateConfig.TEMPLATE_PRODUCT_PROPERTIES_IMAGES)) {
                item.setProductImage((List<BinaryObjectType>) parseCell(cell, TEMPLATE_DATA_TYPE_FILE, true));

            } else if (property.getPreferredName().equals(TemplateConfig.TEMPLATE_PRODUCT_PROPERTIES_CERTIFICATIONS)) {
                List<String> certificateNames = (List<String>) parseCell(cell, TEMPLATE_DATA_TYPE_TEXT, true);
                List<CertificateType> certificates = new ArrayList<>();
                for (String name : certificateNames) {
                    CertificateType cert = new CertificateType();
                    cert.setCertificateType(name);
                    certificates.add(cert);
                }
                item.setCertificate(certificates);

            } else if (property.getPreferredName().equals(TemplateConfig.TEMPLATE_PRODUCT_PROPERTIES_PRODUCT_DATA_SHEET)) {
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

            } else if (property.getPreferredName().equals(TemplateConfig.TEMPLATE_PRODUCT_PROPERTIES_WIDTH)) {
                // just to initialize the dimension array
                item.getDimension();
                List<QuantityType> widths;
                try {
                    widths = (List<QuantityType>) parseCell(cell, TEMPLATE_DATA_TYPE_QUANTITY, true);
                } catch (TemplateParseException e) {
                    throw new TemplateParseException("Failed to parse width dimension. Check the corresponding unit", e);
                }
                for (QuantityType width : widths) {
                    DimensionType dimension = new DimensionType();
                    dimension.setAttributeID("Width");
                    dimension.setMeasure(width);
                    item.getDimension().add(dimension);
                }

            } else if (property.getPreferredName().equals(TemplateConfig.TEMPLATE_PRODUCT_PROPERTIES_LENGTH)) {
                List<QuantityType> lengths;
                try {
                    lengths = (List<QuantityType>) parseCell(cell, TEMPLATE_DATA_TYPE_QUANTITY, true);
                } catch (TemplateParseException e) {
                    throw new TemplateParseException("Failed to parse length dimension. Check the corresponding unit", e);
                }
                for (QuantityType width : lengths) {
                    DimensionType dimension = new DimensionType();
                    dimension.setAttributeID("Length");
                    dimension.setMeasure(width);
                    item.getDimension().add(dimension);
                }

            } else if (property.getPreferredName().equals(TemplateConfig.TEMPLATE_PRODUCT_PROPERTIES_HEIGHT)) {
                List<QuantityType> widths;
                try {
                    widths = (List<QuantityType>) parseCell(cell, TEMPLATE_DATA_TYPE_QUANTITY, true);
                } catch (TemplateParseException e) {
                    throw new TemplateParseException("Failed to parse width dimension. Check the corresponding unit", e);
                }
                for (QuantityType width : widths) {
                    DimensionType dimension = new DimensionType();
                    dimension.setAttributeID("Height");
                    dimension.setMeasure(width);
                    item.getDimension().add(dimension);
                }
            }
        }
    }

    private void parseTermsTab(List<CatalogueLineType> catalogueLines) throws TemplateParseException {
        Sheet termsTab = wb.getSheet(TemplateConfig.TEMPLATE_TAB_TRADING_DELIVERY_TERMS);
        for (CatalogueLineType catalogueLine : catalogueLines) {
            ItemType item = catalogueLine.getGoodsItem().getItem();
            // find row corresponding to the provided item
            int rowIndex = 4;
            Row row = null;
            Cell cell;
            String value;
            for (; rowIndex <= termsTab.getLastRowNum(); rowIndex++) {
                row = termsTab.getRow(rowIndex);
                cell = row.getCell(1);
                if (cell == null) {
                    continue;
                } else {
                    if (getCellStringValue(cell).contentEquals(item.getManufacturersItemIdentification().getID())) {
                        break;
                    } else {
                        row = null;
                    }
                }
            }
            if (row == null) {
                throw new TemplateParseException("No trading & delivery terms for item: " + item.getName() + " id: " + item.getManufacturersItemIdentification().getID());
            }

            // parse the terms
            int columnIndex = 1;
            List<Property> termRelatedProperties = TemplateConfig.getFixedPropertiesForTermsTab();
            for (Property property : termRelatedProperties) {
                cell = getCellWithMissingCellPolicy(row, columnIndex);
                if (property.getPreferredName().contentEquals(TEMPLATE_TRADING_DELIVERY_PRICE_AMOUNT)) {
                    ItemLocationQuantityType itemLocationQuantity = new ItemLocationQuantityType();
                    catalogueLine.setRequiredItemLocationQuantity(itemLocationQuantity);
                    PriceType price = new PriceType();
                    itemLocationQuantity.setPrice(price);
                    AmountType amount = new AmountType();
                    price.setPriceAmount(amount);

                    // parse price amount
                    if (cell == null) {
                        throw new TemplateParseException("No price provided for the item: " + item.getName() + " id: " + item.getManufacturersItemIdentification().getID());
                    }

                    amount.setValue((BigDecimal) parseCell(cell, TEMPLATE_DATA_TYPE_NUMBER, false));

                    // parse currency
                    Row unitRow = termsTab.getRow(3);
                    cell = getCellWithMissingCellPolicy(unitRow, columnIndex);
                    if (cell == null) {
                        throw new TemplateParseException("No currency provided for the price of the item: " + item.getName() + " id: " + item.getManufacturersItemIdentification().getID());
                    }

                    value = cell.getStringCellValue();
                    amount.setCurrencyID(value);

                } else if (property.getPreferredName().contentEquals(TEMPLATE_TRADING_DELIVERY_PRICE_BASE_QUANTITY)) {
                    QuantityType baseQuantity = (QuantityType) parseCell(cell, TEMPLATE_DATA_TYPE_QUANTITY, false);
                    if (baseQuantity == null) {
                        throw new TemplateParseException("A base quantity and an associated unit must be provided for the item: " + item.getName() + " id: " + item.getManufacturersItemIdentification().getID());
                    } else if (baseQuantity.getUnitCode() == null) {
                        throw new TemplateParseException("A unit must be provided for the base quantity of the item: " + item.getName() + " id: " + item.getManufacturersItemIdentification().getID());
                    }
                    catalogueLine.getRequiredItemLocationQuantity().getPrice().setBaseQuantity(baseQuantity);

                } else if (property.getPreferredName().contentEquals(TEMPLATE_TRADING_DELIVERY_MINIMUM_ORDER_QUANTITY)) {
                    QuantityType minimumOrderQuantity = (QuantityType) parseCell(cell, TEMPLATE_DATA_TYPE_QUANTITY, false);
                    if (minimumOrderQuantity != null) {
                        if (minimumOrderQuantity.getUnitCode() == null) {
                            throw new TemplateParseException("A unit must be provided for the minimum order quantity of the item: " + item.getName() + " id: " + item.getManufacturersItemIdentification().getID());
                        }
                    } else {
                        minimumOrderQuantity = new QuantityType();
                    }
                    catalogueLine.setMinimumOrderQuantity(minimumOrderQuantity);

                } else if (property.getPreferredName().contentEquals(TEMPLATE_TRADING_DELIVERY_FREE_SAMPLE)) {
                    catalogueLine.setFreeOfChargeIndicator((Boolean) parseCell(cell, TEMPLATE_DATA_TYPE_BOOLEAN, false));

                } else if (property.getPreferredName().contentEquals(TEMPLATE_TRADING_DELIVERY_WARRANTY_VALIDITY_PERIOD)) {
                    QuantityType warrantyValidityPeriod = (QuantityType) parseCell(cell, TEMPLATE_DATA_TYPE_QUANTITY, false);
                    if (warrantyValidityPeriod != null) {
                        if (warrantyValidityPeriod.getUnitCode() == null) {
                            throw new TemplateParseException("A unit must be provided for the warranty validity period of the item: " + item.getName() + " id: " + item.getManufacturersItemIdentification().getID());
                        }
                    } else {
                        warrantyValidityPeriod = new QuantityType();
                    }
                    PeriodType period = new PeriodType();
                    catalogueLine.setWarrantyValidityPeriod(period);
                    period.setDurationMeasure(warrantyValidityPeriod);

                } else if (property.getPreferredName().contentEquals(TEMPLATE_TRADING_DELIVERY_WARRANTY_INFORMATION)) {
                    if (cell != null) {
                        List<String> values = (List<String>) parseCell(cell, TEMPLATE_DATA_TYPE_TEXT, true);
                        if (values.size() > 0) {
                            catalogueLine.getWarrantyInformation().addAll(values);
                        }
                    }

                } else if (property.getPreferredName().contentEquals(TEMPLATE_TRADING_DELIVERY_INCOTERMS)) {
                    DeliveryTermsType deliveryTerms = new DeliveryTermsType();
                    catalogueLine.getGoodsItem().setDeliveryTerms(deliveryTerms);
                    value = (String) parseCell(cell, TEMPLATE_DATA_TYPE_TEXT, false);
                    catalogueLine.getGoodsItem().getDeliveryTerms().setIncoterms(value);

                } else if (property.getPreferredName().contentEquals(TEMPLATE_TRADING_DELIVERY_SPECIAL_TERMS)) {
                    value = (String) parseCell(cell, TEMPLATE_DATA_TYPE_TEXT, false);
                    catalogueLine.getGoodsItem().getDeliveryTerms().setSpecialTerms(value);

                } else if (property.getPreferredName().contentEquals(TEMPLATE_TRADING_DELIVERY_ESTIMATED_DELIVERY_PERIOD)) {
                    QuantityType estimatedDeliveryQuantity = (QuantityType) parseCell(cell, TEMPLATE_DATA_TYPE_QUANTITY, false);
                    if (estimatedDeliveryQuantity != null) {
                        if (estimatedDeliveryQuantity.getUnitCode() == null) {
                            throw new TemplateParseException("A unit must be provided for the estimated delivery period of the item: " + item.getName() + " id: " + item.getManufacturersItemIdentification().getID());
                        }

                    } else {
                        estimatedDeliveryQuantity = new QuantityType();
                    }

                    PeriodType period = new PeriodType();
                    catalogueLine.getGoodsItem().getDeliveryTerms().setEstimatedDeliveryPeriod(period);
                    period.setDurationMeasure(estimatedDeliveryQuantity);

                } else if (property.getPreferredName().contentEquals(TEMPLATE_TRADING_DELIVERY_TRANSPORT_MODE)) {
                    cell = getCellWithMissingCellPolicy(row, columnIndex);
                    if (cell != null) {
                        value = cell.getStringCellValue();
                        CodeType transportModeCode = new CodeType();
                        transportModeCode.setValue(value);
                        catalogueLine.getGoodsItem().getDeliveryTerms().setTransportModeCode(transportModeCode);
                    }

                } else if (property.getPreferredName().contentEquals(TEMPLATE_TRADING_DELIVERY_PACKAGING_TYPE)) {
                    cell = getCellWithMissingCellPolicy(row, columnIndex);
                    CodeType packagingType = new CodeType();
                    if (cell != null) {
                        value = cell.getStringCellValue();
                        packagingType.setValue(value);
                    }

                    PackageType packaging = new PackageType();
                    catalogueLine.getGoodsItem().setContainingPackage(packaging);
                    packaging.setPackagingTypeCode(packagingType);

                } else if (property.getPreferredName().contentEquals(TEMPLATE_TRADING_DELIVERY_PACKAGE_QUANTITY)) {
                    QuantityType packageQuantity = (QuantityType) parseCell(cell, TEMPLATE_DATA_TYPE_QUANTITY, false);
                    if (packageQuantity != null) {
                        if (packageQuantity.getUnitCode() == null) {
                            throw new TemplateParseException("A unit must be provided for the package quantity of the item: " + item.getName() + " id: " + item.getManufacturersItemIdentification().getID());
                        }
                    } else {
                        packageQuantity = new QuantityType();
                    }
                    catalogueLine.getGoodsItem().getContainingPackage().setQuantity(packageQuantity);
                }
                columnIndex++;
            }
        }
    }

    private Object parseCell(Cell cell, String dataType, boolean multiValue) throws TemplateParseException {
        List<String> values = parseMultiValues(cell);
        List<Object> results = new ArrayList<>();
        String normalizedDataType = TemplateGenerator.normalizeDataTypeForTemplate(dataType);
        for (String value : values) {
            if (normalizedDataType.contentEquals("BOOLEAN")) {
                results.add(parseBoolean(value));
            } else if (normalizedDataType.compareToIgnoreCase("TEXT") == 0) {
                results.add(value);
            } else if (normalizedDataType.compareToIgnoreCase("NUMBER") == 0) {
                results.add(new BigDecimal(value));
            } else if (normalizedDataType.compareToIgnoreCase("QUANTITY") == 0) {
                results.add(parseQuantity(value, cell));
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

    private Boolean parseBoolean(String value) throws TemplateParseException {
        if (!(value.compareToIgnoreCase("TRUE") == 0 ||
                value.compareToIgnoreCase("FALSE") == 0 ||
                value.compareToIgnoreCase("YES") == 0 ||
                value.compareToIgnoreCase("NO") == 0)) {
            throw new TemplateParseException("Free Sample property can only have true/false or yes/no values");
        }
        if (value.compareToIgnoreCase("YES") == 0) {
            value = "TRUE";
        }
        return new Boolean(value);
    }

    private QuantityType parseQuantity(String value, Cell cell) throws TemplateParseException {
        QuantityType quantity = new QuantityType();

        if (cell == null) {
            return null;
        }

        Row row = cell.getRow();
        quantity.setValue(new BigDecimal(value));

        // parse unit
        row = row.getSheet().getRow(3);
        cell = getCellWithMissingCellPolicy(row, cell.getColumnIndex());
        if (cell == null) {
            throw new TemplateParseException("Both value and unit must be provided for quantity values");
        } else {
            value = cell.getStringCellValue();
            if(value.contentEquals("")) {
                throw new TemplateParseException("Both value and unit must be provided for quantity values");
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
            throw new TemplateParseException("Failed to get the file at " + filePath + " in base64 encoding", e);
        }
        return binaryObject;
    }

    private List<String> parseMultiValues(Cell cell) {
        List<String> values = new ArrayList<>();
        if (cell == null) {
            return values;
        }

        String value = getCellStringValue(cell);

        if (value.equals("")) {
            return values;
        } else {
            return Arrays.asList(value.split("\\|"));
        }
    }

    private Cell getCellWithMissingCellPolicy(Row row, int cellNum) {
        return row.getCell(cellNum, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK);
    }

    private String getCellStringValue(Cell cell) {
        switch (cell.getCellTypeEnum()) {
            case STRING:
                return cell.getStringCellValue();
            case NUMERIC:
                if (DateUtil.isCellDateFormatted(cell)) {
                    return cell.getDateCellValue().toString();
                } else {
                    return cell.getNumericCellValue() + "";
                }
            default:
                return "";
        }
    }

    private List<Category> getTemplateCategories(Sheet metadataTab) {
        List<Category> categories = new ArrayList<>();

        List<String> categoryIds = Arrays.asList(metadataTab.getRow(0).getCell(0).getStringCellValue().split(","));
        List<String> taxonomyIds = Arrays.asList(metadataTab.getRow(1).getCell(0).getStringCellValue().split(","));

        CategoryServiceManager csm = CategoryServiceManager.getInstance();
        for (int i = 0; i < categoryIds.size(); i++) {
            Category category = csm.getCategory(taxonomyIds.get(i), categoryIds.get(i));
            categories.add(category);
        }

        return categories;
    }
}
