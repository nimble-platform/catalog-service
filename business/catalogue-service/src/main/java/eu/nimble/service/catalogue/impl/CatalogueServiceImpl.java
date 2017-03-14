/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package eu.nimble.service.catalogue.impl;

import eu.nimble.service.catalogue.CatalogueService;
import eu.nimble.service.catalogue.ProductCategoryService;
import eu.nimble.service.catalogue.category.datamodel.Category;
import eu.nimble.service.catalogue.category.datamodel.Property;
import eu.nimble.service.catalogue.category.datamodel.Unit;
import eu.nimble.service.catalogue.category.datamodel.Value;
import eu.nimble.service.catalogue.exception.CatalogueServiceException;
import eu.nimble.service.model.modaml.catalogue.TEXCatalogType;
import eu.nimble.service.model.ubl.catalogue.CatalogueType;
import eu.nimble.service.model.ubl.commonaggregatecomponents.CatalogueLineType;
import eu.nimble.service.model.ubl.commonaggregatecomponents.ItemPropertyType;
import eu.nimble.service.model.ubl.commonaggregatecomponents.ItemType;
import eu.nimble.utility.Configuration;
import eu.nimble.utility.HibernateUtility;
import eu.nimble.utility.JAXBUtility;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.openxml4j.opc.OPCPackage;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

import static eu.nimble.service.catalogue.impl.TemplateConfig.*;

/**
 * @author yildiray
 */
public class CatalogueServiceImpl implements CatalogueService {

    private static final Logger logger = LoggerFactory.getLogger(CatalogueServiceImpl.class);
    private static CatalogueService instance = null;

    private CatalogueServiceImpl() {
    }

    public static CatalogueService getInstance() {
        if (instance == null) {
            return new CatalogueServiceImpl();
        } else {
            return instance;
        }
    }

    @Override
    public void addCatalogue(CatalogueType catalogue) {
        HibernateUtility.getInstance(Configuration.UBL_PERSISTENCE_UNIT_NAME).persist(catalogue);
    }

    @Override
    public void addCatalogue(TEXCatalogType catalogue) {
        HibernateUtility.getInstance(Configuration.MODAML_PERSISTENCE_UNIT_NAME).persist(catalogue);
    }

    @Override
    public void addCatalogue(String xml, Configuration.Standard standard) {
        if (standard == Configuration.Standard.UBL) {
            CatalogueType catalogue = (CatalogueType) JAXBUtility.deserialize(xml, Configuration.UBL_CATALOGUE_PACKAGENAME);
            addCatalogue(catalogue);
        } else if (standard == Configuration.Standard.MODAML) {
            TEXCatalogType catalogue = (TEXCatalogType) JAXBUtility.deserialize(xml, Configuration.MODAML_CATALOGUE_PACKAGENAME);
            addCatalogue(catalogue);
        }
    }

    @Override
    public Object getCatalogueByUUID(String uuid, Configuration.Standard standard) {
        List resultSet = null;

        if (standard == Configuration.Standard.UBL) {
            String query = "SELECT catalogue FROM CatalogueType catalogue "
                    + " JOIN FETCH catalogue.UUID catalogue_uuid "
                    + " WHERE catalogue_uuid.value = '" + uuid + "'";
            resultSet = (List<CatalogueType>) HibernateUtility.getInstance(Configuration.UBL_PERSISTENCE_UNIT_NAME)
                    .loadAll(query);
        } else if (standard == Configuration.Standard.MODAML) {
            String query = "SELECT catalogue FROM TEXCatalogType catalogue "
                    + " JOIN FETCH catalogue.TCheader catalogue_header "
                    + " WHERE catalogue_header.msgID = '" + uuid + "'";
            resultSet = (List<TEXCatalogType>) HibernateUtility.getInstance(Configuration.MODAML_PERSISTENCE_UNIT_NAME)
                    .loadAll(query);
        }

        if (resultSet.size() > 0) {
            return resultSet.get(0);
        }

        return null;
    }

    @Override
    public void deleteCatalogueByUUID(String uuid, Configuration.Standard standard) {
        if (standard == Configuration.Standard.UBL) {
            CatalogueType catalogue = (CatalogueType) getCatalogueByUUID(uuid, standard);
            HibernateUtility.getInstance(Configuration.UBL_PERSISTENCE_UNIT_NAME).delete(CatalogueType.class, catalogue.getHjid());
        } else if (standard == Configuration.Standard.MODAML) {
            TEXCatalogType catalogue = (TEXCatalogType) getCatalogueByUUID(uuid, standard);
            HibernateUtility.getInstance(Configuration.MODAML_PERSISTENCE_UNIT_NAME).delete(TEXCatalogType.class, catalogue.getHjid());
        }
    }

    @Override
    public OutputStream generateTemplateForCategory(String categoryId) {
        ProductCategoryService pcs = new ProductCategoryServiceImpl();
        Category category = pcs.getCategory(categoryId);

        Workbook template = new XSSFWorkbook();
        Sheet infoTab = template.createSheet("Information");
        Sheet propertiesTab = template.createSheet(SHEET_NAME_PRODUCT_PROPERTIES);
        Sheet propertyDetailsTab = template.createSheet("Property Details");
        Sheet valuesTab = template.createSheet("Allowed Values for Properties");

        populateInfoTab(infoTab);
        populateProductPropertiesTab(category, propertiesTab);
        populatePropertyDetailsTab(category, propertyDetailsTab);
        populateAllowedValuesTab(category, valuesTab);

        FileOutputStream fileOut = null;
        try {
            fileOut = new FileOutputStream(categoryId + "-catalogue-template.xlsx");
            template.write(fileOut);
            fileOut.close();
        } catch (java.io.IOException e) {
            throw new CatalogueServiceException("Failed to create template for category: " + categoryId, e);
        }

        return fileOut;
    }

    private void populateInfoTab(Sheet infoTab) {
        int rowIndex = 0;
        Row row = infoTab.createRow(rowIndex);
        row.createCell(0).setCellValue("How to fill in this template?");
        row = infoTab.createRow(++rowIndex);
        row.createCell(0).setCellValue("This tab provides information about the other tabs of this template");
        rowIndex++;
        row = infoTab.createRow(++rowIndex);
        row.createCell(0).setCellValue("Product Properties");
        row = infoTab.createRow(++rowIndex);
        row.createCell(0).setCellValue("Top 3 columns of the Product Properties tab includes the information about the properties to be filled in for each product variation.");
        row = infoTab.createRow(++rowIndex);
        row.createCell(0).setCellValue("The first row is the name of the property. and the second row shows the unit associated with the property value if there is any.");
        row = infoTab.createRow(++rowIndex);
        row.createCell(0).setCellValue("The second row shows the data type of the property.");
        row = infoTab.createRow(++rowIndex);
        row.createCell(0).setCellValue("The third row shows the unit associated with the property value if there is any.");
        row = infoTab.createRow(++rowIndex);
        row.createCell(0).setCellValue("Details of the property and allowed values can be investigated in \"Property Details\" and \"Allowed Values for Properties\" tabs respectively based on the name of the property ");
        row = infoTab.createRow(++rowIndex);
        row.createCell(0).setCellValue("The 4th row onwards, each row corresponds to a product variation for the chosen product category");
        rowIndex++;
        row = infoTab.createRow(++rowIndex);
        row.createCell(0).setCellValue("Property Details");
        row = infoTab.createRow(++rowIndex);
        row.createCell(0).setCellValue("This tab contains additional information for each property associated with the chosen product category");
        rowIndex++;
        row = infoTab.createRow(++rowIndex);
        row.createCell(0).setCellValue("Allowed Values for Properties");
        row = infoTab.createRow(++rowIndex);
        row.createCell(0).setCellValue("This tab contains values that are allowed to be set for each property starting from the 4th row");
    }

    private void populateProductPropertiesTab(Category category, Sheet productPropertiesTab) {
        int rowIndex = 0;
        Row firstRow = productPropertiesTab.createRow(rowIndex);
        firstRow.createCell(0).setCellValue("Property Name");
        Row secondRow = productPropertiesTab.createRow(++rowIndex);
        secondRow.createCell(0).setCellValue("Property Data Type");
        Row thirdRow = productPropertiesTab.createRow(++rowIndex);
        thirdRow.createCell(0).setCellValue("Property Unit");

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
        row.createCell(columnIndex).setCellValue("Property Name");
        row.createCell(++columnIndex).setCellValue("Short Name");
        row.createCell(++columnIndex).setCellValue("Definition");
        row.createCell(++columnIndex).setCellValue("Note");
        row.createCell(++columnIndex).setCellValue("Remark");
        row.createCell(++columnIndex).setCellValue("Preferred Symbol");
        row.createCell(++columnIndex).setCellValue("Unit");
        row.createCell(++columnIndex).setCellValue("IEC Category");
        row.createCell(++columnIndex).setCellValue("Attribute Type");
        row.createCell(++columnIndex).setCellValue("Data Type");

        List<Property> properties = category.getProperties();
        for (int i = 0; i < properties.size(); i++) {
            Property property = properties.get(i);
            row = (propertyDetailsTab.createRow(++rowIndex));

            columnIndex = 0;
            row.createCell(columnIndex).setCellValue(property.getPreferredName());
            row.createCell(++columnIndex).setCellValue(property.getPreferredName());
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

    @Override
    public void addCatalogue(InputStream template) {
        OPCPackage pkg = null;
        try {
            pkg = OPCPackage.open(template);
            XSSFWorkbook wb = new XSSFWorkbook(pkg);
            List<CatalogueLineType> products = getCatalogueLines(wb);
            CatalogueType catalogue = new CatalogueType();
            catalogue.setCatalogueLine(products);

            HibernateUtility.getInstance(Configuration.UBL_PERSISTENCE_UNIT_NAME).persist(catalogue);

        } catch (InvalidFormatException e) {
            throw new CatalogueServiceException("Invalid format for the submitted template", e);
        } catch (IOException e) {
            throw new CatalogueServiceException("Failed to read the submitted template", e);
        } finally {
            if (pkg != null) {
                try {
                    pkg.close();
                } catch (IOException e) {
                    logger.warn("Failed to close the OPC Package", e);
                }
            }
        }
    }

    private List<CatalogueLineType> getCatalogueLines(Workbook template) {
        List<CatalogueLineType> results = new ArrayList<>();

        Sheet productPropertiesTab = template.getSheet(SHEET_NAME_PRODUCT_PROPERTIES);
        int propertyNum = productPropertiesTab.getRow(0).getLastCellNum();
        int catalogSize = productPropertiesTab.getLastRowNum();
        int fixedPropNumber = TemplateConfig.getFixedProperties().size();

        List<Property> properties = new ArrayList<>();
        for (int i = 0; i < propertyNum; i++) {
            Row propNameRow = productPropertiesTab.getRow(0);
            Row propDataTypeRow = productPropertiesTab.getRow(1);
            Row propUnitRow = productPropertiesTab.getRow(2);

            Property property = new Property();
            property.setPreferredName(propNameRow.getCell(i).getStringCellValue());
            property.setDataType(propDataTypeRow.getCell(i).getStringCellValue());
            Unit unit = new Unit();
            unit.setShortName(propUnitRow.getCell(i).getStringCellValue());
            property.setUnit(unit);
            properties.add(property);
        }

        // first three rows contains fixed values
        for (int rowNum = 3; rowNum <= catalogSize; rowNum++) {
            CatalogueLineType clt = new CatalogueLineType();
            ItemType item = new ItemType();
            List<ItemPropertyType> itemProperties = new ArrayList<>();
            clt.setItem(item);
            item.setAdditionalItemProperty(itemProperties);
            results.add(clt);

            Row row = productPropertiesTab.getRow(rowNum);
            parseFixedProperties(row, item);
            for (int i = fixedPropNumber; i < properties.size(); i++) {
                Cell cell = row.getCell(i);
                if (cell != null) {
                    ItemPropertyType itemProp = new ItemPropertyType();
                    itemProp.setValue(getCellStringValue(cell));
                    itemProp.setName(properties.get(i).getPreferredName());
                    itemProp.setValueQualifier(properties.get(i).getDataType());
                    itemProperties.add(itemProp);
                }
            }
        }
        return results;
    }

    private void parseFixedProperties(Row propertiesRow, ItemType item) {
        List<Property> properties = TemplateConfig.getFixedProperties();
        for (int i = 0; i < properties.size(); i++) {
            Property property = properties.get(i);
            Cell cell = propertiesRow.getCell(i);
            if (cell != null) {
                if (property.equals(PROPERTY_NAME_NAME)) {
                    item.setName(getCellStringValue(cell));
                } else if (property.equals(PROPERTY_NAME_DESCRIPTION)) {
                    item.setDescription(getCellStringValue(cell));
                }
            }
        }
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
}
