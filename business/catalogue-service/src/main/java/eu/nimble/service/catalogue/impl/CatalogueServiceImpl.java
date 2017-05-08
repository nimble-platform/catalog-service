/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package eu.nimble.service.catalogue.impl;

import eu.nimble.data.transformer.ontmalizer.XML2OWLMapper;
import eu.nimble.data.transformer.ontmalizer.XSD2OWLMapper;
import eu.nimble.service.catalogue.CatalogueService;
import eu.nimble.service.catalogue.ProductCategoryService;
import eu.nimble.service.catalogue.category.datamodel.Category;
import eu.nimble.service.catalogue.category.datamodel.Property;
import eu.nimble.service.catalogue.category.datamodel.Unit;
import eu.nimble.service.catalogue.category.datamodel.Value;
import eu.nimble.service.catalogue.exception.CatalogueServiceException;
import eu.nimble.service.model.modaml.catalogue.TEXCatalogType;
import eu.nimble.service.model.ubl.catalogue.CatalogueType;
import eu.nimble.service.model.ubl.commonaggregatecomponents.*;
import eu.nimble.utility.Configuration;
import eu.nimble.utility.HibernateUtility;
import eu.nimble.utility.JAXBUtility;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.openxml4j.opc.OPCPackage;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.bind.*;
import javax.xml.namespace.QName;
import java.io.*;
import java.lang.reflect.Array;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import static eu.nimble.service.catalogue.impl.TemplateConfig.*;

/**
 * @author yildiray
 */
public class CatalogueServiceImpl implements CatalogueService {

    private static final Logger logger = LoggerFactory.getLogger(CatalogueServiceImpl.class);
    private static CatalogueService instance = null;
    private static ProductCategoryService pcsInstance = ProductCategoryServiceImpl.getInstance();

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
    public void addCatalogue(PartyType party, CatalogueType catalogue) {
        HibernateUtility.getInstance(Configuration.UBL_PERSISTENCE_UNIT_NAME).persist(catalogue);
    }

    @Override
    public void addCatalogue(PartyType party, TEXCatalogType catalogue) {
        HibernateUtility.getInstance(Configuration.MODAML_PERSISTENCE_UNIT_NAME).persist(catalogue);
    }

    @Override
    public void addCatalogue(PartyType party, String xml, Configuration.Standard standard) {
        if (standard == Configuration.Standard.UBL) {
            CatalogueType catalogue = (CatalogueType) JAXBUtility.deserialize(xml, Configuration.UBL_CATALOGUE_PACKAGENAME);
            addCatalogue(party, catalogue);
        } else if (standard == Configuration.Standard.MODAML) {
            TEXCatalogType catalogue = (TEXCatalogType) JAXBUtility.deserialize(xml, Configuration.MODAML_CATALOGUE_PACKAGENAME);
            addCatalogue(party, catalogue);
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
    public Workbook generateTemplateForCategory(String categoryId) {
        Category category = pcsInstance.getCategory(categoryId);

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
    public void addCatalogue(PartyType party, InputStream template) {
        OPCPackage pkg = null;
        try {
            pkg = OPCPackage.open(template);
            XSSFWorkbook wb = new XSSFWorkbook(pkg);
            List<CatalogueLineType> products = getCatalogueLines(wb);
            CatalogueType catalogue = new CatalogueType();
            catalogue.setCatalogueLine(products);
            catalogue.setProviderParty(party);

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            String packageName = catalogue.getClass().getPackage().getName();
            JAXBContext jc = JAXBContext.newInstance(packageName);
            Marshaller marsh = jc.createMarshaller();
            marsh.setProperty("jaxb.formatted.output", true);
            JAXBElement element = new JAXBElement(
                    new QName(Configuration.UBL_CATALOGUE_NS, "Catalogue"), catalogue.getClass(), catalogue);
            marsh.marshal(element, baos);

            //HibernateUtility.getInstance(Configuration.UBL_PERSISTENCE_UNIT_NAME).persist(catalogue);
            /*PrintWriter writer = new PrintWriter("serialized_ubl_catalogue.xml");
            String serializedCatalogue = JAXBUtility.serialize(catalogue, Configuration.UBL_CATALOGUE_NS, "Catalogue");
            writer.println(serializedCatalogue);
            writer.flush();
            writer.close();*/

            URL url = CatalogueServiceImpl.class.getResource(Configuration.UBL_CATALOGUE_SCHEMA);
            XSD2OWLMapper mapping = new XSD2OWLMapper(url);
            mapping.setObjectPropPrefix("");
            mapping.setDataTypePropPrefix("");
            mapping.convertXSD2OWL();

            /*FileOutputStream ont;
            try {
                File f = new File("ubl_catalogue_ontology.n3");
                ont = new FileOutputStream(f);
                mapping.writeOntology(ont, "N3");
                ont.close();
            } catch (Exception e) {
                e.printStackTrace();
            }*/


            XML2OWLMapper generator = new XML2OWLMapper(new ByteArrayInputStream(baos.toByteArray()), mapping);
            generator.convertXML2OWL();

            // This part prints the RDF data model to the specified file.
            /*try {
                File f = new File("ubl_catalogue_serv.n3");
                FileOutputStream fout = new FileOutputStream(f);
                generator.writeModel(fout, "RDF/XML");
                fout.close();

            } catch (Exception e) {
                e.printStackTrace();
            }*/

            submitCatalogueDataToMarmotta(generator);

        } catch (InvalidFormatException e) {
            throw new CatalogueServiceException("Invalid format for the submitted template", e);
        } catch (IOException e) {
            throw new CatalogueServiceException("Failed to read the submitted template", e);
        } catch (JAXBException e) {
            e.printStackTrace();
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

    private void submitCatalogueDataToMarmotta(XML2OWLMapper generator) {
        // TODO: use party id as the context
        // TODO: properly configure the Marmotta URL
        URL marmottaURL = null;
        try {
            marmottaURL = new URL("http://localhost:8080/marmotta/import/upload?context=catalog");
        } catch (MalformedURLException e) {
            throw new CatalogueServiceException("Invalid format for the submitted template", e);
        }
        HttpURLConnection conn = null;
        try {
            conn = (HttpURLConnection) marmottaURL.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "text/n3");
            conn.setDoOutput(true);

            OutputStream os = conn.getOutputStream();
            generator.writeModel(os, "N3");
            os.flush();

            logger.info("Catalogue submitted to Marmotta. Received HTTP response: {}", conn.getResponseCode());

            conn.disconnect();
        } catch (IOException e) {
            throw new CatalogueServiceException("Failed to submit catalogue to Marmotta", e);
        }
    }

    private List<CatalogueLineType> getCatalogueLines(Workbook template) {
        List<CatalogueLineType> results = new ArrayList<>();

        Sheet productPropertiesTab = template.getSheet(TEMPLATE_TAB_PRODUCT_PROPERTIES);
        Sheet productDetailsTab = template.getSheet(TEMPLATE_TAB_PROPERTY_DETAILS);
        int propertyNum = productDetailsTab.getRow(0).getLastCellNum();
        int catalogSize = productPropertiesTab.getLastRowNum();
        int fixedPropNumber = TemplateConfig.getFixedProperties().size();

        List<Property> properties = new ArrayList<>();
        for (int i = 1; i < propertyNum; i++) {
            int columnNum = 0;
            Row row = productDetailsTab.getRow(i);
            String propertyName = getCellWithMissingCellPolicy(row, columnNum++).getStringCellValue();
            String shortName = getCellWithMissingCellPolicy(row, columnNum++).getStringCellValue();
            String definition = getCellWithMissingCellPolicy(row, columnNum++).getStringCellValue();
            String note = getCellWithMissingCellPolicy(row, columnNum++).getStringCellValue();
            String remark = getCellWithMissingCellPolicy(row, columnNum++).getStringCellValue();
            String preferredSymbol = getCellWithMissingCellPolicy(row, columnNum++).getStringCellValue();
            String unit = getCellWithMissingCellPolicy(row, columnNum++).getStringCellValue();
            String iecCategory = getCellWithMissingCellPolicy(row, columnNum++).getStringCellValue();
            String attributeType = getCellWithMissingCellPolicy(row, columnNum++).getStringCellValue();
            String dataType = getCellWithMissingCellPolicy(row, columnNum++).getStringCellValue();

            Property property = new Property();
            property.setPreferredName(propertyName);
            property.setShortName(shortName);
            property.setDefinition(definition);
            property.setNote(note);
            property.setRemark(remark);
            property.setPreferredSymbol(preferredSymbol);
            property.setIecCategory(iecCategory);
            property.setAttributeType(attributeType);
            property.setDataType(dataType);

            Unit unitObj = new Unit();
            unitObj.setShortName(unit);
            property.setUnit(unitObj);

            properties.add(property);
        }

        // first three rows contains fixed values
        for (int rowNum = 3; rowNum <= catalogSize; rowNum++) {
            CatalogueLineType clt = new CatalogueLineType();
            GoodsItemType goodsItem = new GoodsItemType();
            ItemType item = new ItemType();
            List<ItemPropertyType> itemProperties = new ArrayList<>();
            goodsItem.setItem(item);
            clt.setGoodsItem(goodsItem);
            item.setAdditionalItemProperty(itemProperties);
            results.add(clt);

            Row row = productPropertiesTab.getRow(rowNum);
            parseFixedProperties(row, item);
            for (int i = fixedPropNumber; i < properties.size(); i++) {
                Cell cell = getCellWithMissingCellPolicy(row, i);
                ItemPropertyType itemProp = new ItemPropertyType();
                List<String>  values= new ArrayList<>();
                values.add(getCellStringValue(cell));
                itemProp.setValue(values);
                itemProp.setName(properties.get(i).getPreferredName());
                itemProp.setValueQualifier(properties.get(i).getDataType());
                itemProperties.add(itemProp);
            }
        }
        return results;
    }

    private void parseFixedProperties(Row propertiesRow, ItemType item) {
        List<Property> properties = TemplateConfig.getFixedProperties();
        for (int i = 0; i < properties.size(); i++) {
            Property property = properties.get(i);
            Cell cell = getCellWithMissingCellPolicy(propertiesRow, i);
            if (property.equals(TEMPLATE_FIXED_PROPERTY_NAME)) {
                item.setName(getCellStringValue(cell));
            } else if (property.equals(TEMPLATE_FIXED_PROPERTY_DESCRIPTION)) {
                item.setDescription(getCellStringValue(cell));
            }
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

    @Override
    public void addProduct(PartyType party, GoodsItemType item) {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Override
    public void addPropertyToProduct(Long itemId, String value, String propertyURI) {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Override
    public void addPropertyToProduct(Long itemId, String propertyName, String value, String minValue, String maxValue, String valueQualifier, String unit) {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Override
    public void addPropertyToProduct(Long itemId, String propertyName, String binaryValue, String mimeCode, String contentURI) {
        throw new UnsupportedOperationException("Not implemented yet");
    }
}
