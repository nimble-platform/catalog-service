/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package eu.nimble.service.catalogue.impl;

import eu.nimble.data.transformer.ontmalizer.XML2OWLMapper;
import eu.nimble.data.transformer.ontmalizer.XSD2OWLMapper;
import eu.nimble.service.catalogue.CatalogueService;
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
import sun.reflect.generics.reflectiveObjects.NotImplementedException;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.namespace.QName;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.rmi.server.UID;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.UUID;

import static eu.nimble.service.catalogue.impl.TemplateConfig.*;

/**
 * @author yildiray
 */
public class CatalogueServiceImpl implements CatalogueService {

    private static final Logger logger = LoggerFactory.getLogger(CatalogueServiceImpl.class);
    private static CatalogueService instance = null;
    private static CategoryServiceManager csmInstance = CategoryServiceManager.getInstance();

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
    public CatalogueType addCatalogue(CatalogueType catalogue) {
        return addCatalogue(catalogue, Configuration.Standard.UBL);
    }

    @Override
    public CatalogueType addCatalogue(String catalogueXml) {
        return addCatalogue(catalogueXml, Configuration.Standard.UBL);
    }

    @Override
    public CatalogueType getCatalogue(String uuid) {
        return getCatalogue(uuid, Configuration.Standard.UBL);
    }

    @Override
    public CatalogueType getCatalogue(String id, String partyId) {
        return getCatalogue(id, partyId, Configuration.Standard.UBL);
    }

    @Override
    public CatalogueType updateCatalogue(CatalogueType catalogue) {
        logger.info("Catalogue with uuid: {} will be updated", catalogue.getUUID());
        // merge the hibernate object
        HibernateUtility.getInstance(Configuration.UBL_PERSISTENCE_UNIT_NAME).update(catalogue);

        // delete the catalgoue from marmotta and submit once again
        deleteCatalogueFromMarmotta(catalogue.getUUID());

        // submit again
        submitCatalogueDataToMarmotta(catalogue);
        logger.info("Catalogue with uuid: {} updated", catalogue.getUUID());
        return catalogue;
    }

    @Override
    public void deleteCatalogue(String uuid) {
        deleteCatalogue(uuid, Configuration.Standard.UBL);
    }

    @Override
    public void deleteCatalogue(String id, String partyId) {
        CatalogueType catalogue = getCatalogue(id, partyId);
        deleteCatalogue(catalogue.getUUID());
    }

    @Override
    public <T> T addCatalogue(String catalogueXml, Configuration.Standard standard) {
        T catalogue = null;
        if (standard == Configuration.Standard.UBL) {
            CatalogueType ublCatalogue = (CatalogueType) JAXBUtility.deserialize(catalogueXml, Configuration.UBL_CATALOGUE_PACKAGENAME);
            catalogue = (T) ublCatalogue;

        } else if (standard == Configuration.Standard.MODAML) {
            catalogue = (T) JAXBUtility.deserialize(catalogueXml, Configuration.MODAML_CATALOGUE_PACKAGENAME);
        }
        addCatalogue(catalogue, standard);

        return catalogue;
    }

    @Override
    public <T> T addCatalogue(T catalogue, Configuration.Standard standard) {
        if (standard == Configuration.Standard.UBL) {
            // create a globally unique identifier
            CatalogueType ublCatalogue = (CatalogueType) catalogue;
            String uuid = UUID.randomUUID().toString();
            ublCatalogue.setUUID(uuid);

            // persist the catalogue in relational DB
            HibernateUtility.getInstance(Configuration.UBL_PERSISTENCE_UNIT_NAME).persist(ublCatalogue);
            logger.info("Catalogue with uuid: {} persisted in DB", uuid.toString());

            // persist the catalogue also in Marmotta
            submitCatalogueDataToMarmotta(ublCatalogue);

        } else if (standard == Configuration.Standard.MODAML) {
            HibernateUtility.getInstance(Configuration.MODAML_PERSISTENCE_UNIT_NAME).persist(catalogue);
        }
        return catalogue;
    }

    @Override
    public <T> T getCatalogue(String uuid, Configuration.Standard standard) {
        T catalogue = null;
        List<T> resultSet = null;

        String query;
        if (standard == Configuration.Standard.UBL) {
            query = "SELECT catalogue FROM CatalogueType catalogue "
                    + " JOIN catalogue.UUID as catalogue_uuid "
                    + " WHERE catalogue_uuid.value = '" + uuid + "'";

            resultSet = (List<T>) HibernateUtility.getInstance(Configuration.UBL_PERSISTENCE_UNIT_NAME)
                    .loadAll(query);
            if (resultSet.size() > 0) {
                catalogue = (T) resultSet.get(0);
            }

        } else if (standard == Configuration.Standard.MODAML) {
            query = "SELECT catalogue FROM TEXCatalogType catalogue "
                    + " JOIN FETCH catalogue.TCheader catalogue_header "
                    + " WHERE catalogue_header.msgID = '" + uuid + "'";
            resultSet = (List<T>) HibernateUtility.getInstance(Configuration.MODAML_PERSISTENCE_UNIT_NAME)
                    .loadAll(query);

        }
        if (resultSet != null && resultSet.size() > 0) {
            catalogue = resultSet.get(0);
        }

        return catalogue;
    }

    @Override
    public <T> T getCatalogue(String id, String partyId, Configuration.Standard standard) {
        T catalogue = null;
        List<T> resultSet = null;

        String query;
        if (standard == Configuration.Standard.UBL) {
            /*query = "SELECT catalogue FROM CatalogueType as catalogue "
                    + " JOIN catalogue.ID as catalogue_id "
                    + " JOIN catalogue.providerParty as catalogue_provider_party"
                    + " JOIN catalogue_provider_party.ID as party_id"
                    + " WHERE catalogue_id.value = '" + id + "'"
                    + " AND party_id.value = '" + partyId + "'";*/
            query = "SELECT catalogue FROM CatalogueType as catalogue "
                    + " JOIN catalogue.providerParty as catalogue_provider_party"
                    + " WHERE catalogue.id = '" + id + "'"
                    + " AND catalogue_provider_party.id = '" + partyId + "'";

            resultSet = (List<T>) HibernateUtility.getInstance(Configuration.UBL_PERSISTENCE_UNIT_NAME)
                    .loadAll(query);

        } else if (standard == Configuration.Standard.MODAML) {
            logger.warn("Fetching catalogues with id and party id from MODAML repository is not implemented yet");
            throw new NotImplementedException();
        }

        if (resultSet.size() > 0) {
            catalogue = resultSet.get(0);
        }

        return catalogue;
    }

    @Override
    public void deleteCatalogue(String uuid, Configuration.Standard standard) {
        if (standard == Configuration.Standard.UBL) {
            logger.info("Deleting catalogue with uuid: {}", uuid);
            // delete catalogue from relational db
            CatalogueType catalogue = getCatalogue(uuid);

            if (catalogue != null) {
                Long hjid = catalogue.getHjid();
                HibernateUtility.getInstance(Configuration.UBL_PERSISTENCE_UNIT_NAME).delete(CatalogueType.class, hjid);

                // delete catalogue from marmotta
                deleteCatalogueFromMarmotta(uuid);
                logger.info("Deleted catalogue with uuid: {}", uuid);
            } else {
                logger.info("No catalogue for uuid: {}", uuid);
            }

        } else if (standard == Configuration.Standard.MODAML) {
            TEXCatalogType catalogue = getCatalogue(uuid, Configuration.Standard.MODAML);
            Long hjid = catalogue.getHjid();
            HibernateUtility.getInstance(Configuration.MODAML_PERSISTENCE_UNIT_NAME).delete(TEXCatalogType.class, hjid);
        }
    }

    private void deleteCatalogueFromMarmotta(String uuid) {
        /*logger.info("Catalogue with uuid: {} will be deleted from Marmotta", uuid);

        URL marmottaURL;
        try {
            Properties prop = new Properties();
            prop.load(CatalogueServiceImpl.class.getClassLoader().getResourceAsStream("application.properties"));
            marmottaURL = new URL(prop.getProperty("marmotta.url") + "/context/" + uuid);
        } catch (IOException e) {
            throw new CatalogueServiceException("Failed to read Marmotta URL from config file", e);
        }

        HttpURLConnection conn;
        try {
            conn = (HttpURLConnection) marmottaURL.openConnection();
            conn.setRequestMethod("DELETE");
            conn.setDoOutput(true);

            OutputStream os = conn.getOutputStream();
            os.flush();

            logger.info("Marmotta response for deleting catalogue with uuid: {}: {}", uuid, conn.getResponseCode());

            conn.disconnect();
        } catch (IOException e) {
            throw new CatalogueServiceException("Failed to submit catalogue to Marmotta", e);
        }
        logger.info("Catalogue with uuid: {} deleted from Marmotta", uuid);*/
    }

    @Override
    public Workbook generateTemplateForCategory(String taxonomyId, String categoryId) {
        Category category = csmInstance.getCategory(taxonomyId, categoryId);

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

    @Override
    public void addCatalogue(InputStream catalogueTemplate, PartyType party) {
        CatalogueType catalogue;
        OPCPackage pkg = null;
        try {
            pkg = OPCPackage.open(catalogueTemplate);
            XSSFWorkbook wb = new XSSFWorkbook(pkg);
            List<CatalogueLineType> products = getCatalogueLines(wb, party);
            catalogue = new CatalogueType();
            catalogue.setCatalogueLine(products);
            catalogue.setProviderParty(party);

            addCatalogue(catalogue);

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

    private XML2OWLMapper transformCatalogueToRDF(CatalogueType catalogue) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try {
            String packageName = catalogue.getClass().getPackage().getName();
            JAXBContext jc = JAXBContext.newInstance(packageName);

            Marshaller marsh = jc.createMarshaller();
            marsh.setProperty("jaxb.formatted.output", true);
            JAXBElement element = new JAXBElement(
                    new QName(Configuration.UBL_CATALOGUE_NS, "Catalogue"), catalogue.getClass(), catalogue);
            marsh.marshal(element, baos);

        } catch (JAXBException e) {
            throw new CatalogueServiceException("Failed to serialize the catalogue instance to XML", e);
        }

            /*PrintWriter writer = new PrintWriter("serialized_ubl_catalogue.xml");
            String serializedCatalogue = JAXBUtility.serialize(catalogue, "Catalogue");
            writer.println(serializedCatalogue);
            writer.flush();
            writer.close();*/

        URL url = CatalogueServiceImpl.class.getResource(Configuration.UBL_CATALOGUE_SCHEMA);
        XSD2OWLMapper mapping = new XSD2OWLMapper(url);
        mapping.setObjectPropPrefix("");
        mapping.setDataTypePropPrefix("");
        mapping.convertXSD2OWL();

        FileOutputStream ont;
        try {
            File f = new File("ubl_catalogue_ontology.n3");
            ont = new FileOutputStream(f);
            mapping.writeOntology(ont, "N3");
            ont.close();
        } catch (Exception e) {
            e.printStackTrace();
        }


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
        return generator;
    }

    private void submitCatalogueDataToMarmotta(CatalogueType catalogue) {
        /*logger.info("Catalogue with uuid: {} will be submitted to Marmotta.", catalogue.getUUID());
        XML2OWLMapper rdfGenerator = transformCatalogueToRDF(catalogue);
        logger.info("Transformed catalogue with uuid: {} to RDF", catalogue.getUUID());

        URL marmottaURL;
        try {
            Properties prop = new Properties();
            prop.load(CatalogueServiceImpl.class.getClassLoader().getResourceAsStream("application.properties"));
            marmottaURL = new URL(prop.getProperty("marmotta.url") + "/import/upload?context=" + catalogue.getUUID());
        } catch (MalformedURLException e) {
            throw new CatalogueServiceException("Invalid format for the submitted template", e);
        } catch (IOException e) {
            throw new CatalogueServiceException("Failed to read Marmotta URL from config file", e);
        }

        HttpURLConnection conn = null;
        try {
            conn = (HttpURLConnection) marmottaURL.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "text/n3");
            conn.setDoOutput(true);

            OutputStream os = conn.getOutputStream();
            rdfGenerator.writeModel(os, "N3");
            os.flush();

            logger.info("Catalogue with uuid: {} submitted to Marmotta. Received HTTP response: {}", catalogue.getUUID(), conn.getResponseCode());

            conn.disconnect();
        } catch (IOException e) {
            throw new CatalogueServiceException("Failed to submit catalogue to Marmotta", e);
        }*/
    }

    private List<CatalogueLineType> getCatalogueLines(Workbook template, PartyType party) {
        List<CatalogueLineType> results = new ArrayList<>();

        Sheet productPropertiesTab = template.getSheet(TEMPLATE_TAB_PRODUCT_PROPERTIES);
        Sheet propertyDetailsTab = template.getSheet(TEMPLATE_TAB_PROPERTY_DETAILS);
        int standardPropertyNum = propertyDetailsTab.getLastRowNum();
        int fixedPropNumber = TemplateConfig.getFixedProperties().size();
        int customPropertyNum = productPropertiesTab.getRow(0).getLastCellNum() - (standardPropertyNum + fixedPropNumber + 1);
        int catalogSize = productPropertiesTab.getLastRowNum();

        List<Property> properties = new ArrayList<>();

        // properties included in the selected category
        for (int i = 1; i <= standardPropertyNum; i++) {
            int columnNum = 0;
            Row row = propertyDetailsTab.getRow(i);
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

        // custom properties
        int columnNum = fixedPropNumber + standardPropertyNum + 1;
        for (int i = 0; i < customPropertyNum; i++) {
            Row row = productPropertiesTab.getRow(0);
            String propertyName = getCellWithMissingCellPolicy(row, columnNum).getStringCellValue();
            row = productPropertiesTab.getRow(1);
            String unit = getCellWithMissingCellPolicy(row, columnNum).getStringCellValue();
            row = productPropertiesTab.getRow(2);
            String dataType = getCellWithMissingCellPolicy(row, columnNum).getStringCellValue();
            if (dataType.contentEquals("")) {
                dataType = "STRING";
            }

            Property property = new Property();
            property.setPreferredName(propertyName);
            property.setDataType(dataType);

            Unit unitObj = new Unit();
            unitObj.setShortName(unit);
            property.setUnit(unitObj);

            properties.add(property);
            columnNum++;
        }

        // first three rows contains fixed values
        for (int rowNum = 3; rowNum <= catalogSize; rowNum++) {
            CatalogueLineType clt = new CatalogueLineType();
            GoodsItemType goodsItem = new GoodsItemType();
            ItemType item = new ItemType();
            item.setManufacturerParty(party);
            List<ItemPropertyType> itemProperties = new ArrayList<>();
            goodsItem.setItem(item);
            clt.setGoodsItem(goodsItem);
            item.setAdditionalItemProperty(itemProperties);
            results.add(clt);

            Row row = productPropertiesTab.getRow(rowNum);
            parseFixedProperties(row, item);
            for (int i = 0; i < properties.size(); i++) {
                Cell cell = getCellWithMissingCellPolicy(row, i + fixedPropNumber + 1);
                ItemPropertyType itemProp = new ItemPropertyType();
                List<String> values = new ArrayList<>();
                String value = getCellStringValue(cell);
                values.add(value);
                if (value.equals("")) {
                    continue;
                }

                itemProp.setValue(values);
                itemProp.setName(properties.get(i).getPreferredName());
                itemProp.setValueQualifier(properties.get(i).getDataType());
                itemProperties.add(itemProp);
            }
        }
        return results;
    }

    /**
     * Parses the properties fixed in the common data model
     *
     * @param propertiesRow
     * @param item
     */
    private void parseFixedProperties(Row propertiesRow, ItemType item) {
        List<Property> properties = TemplateConfig.getFixedProperties();
        for (int i = 0; i < properties.size(); i++) {
            Property property = properties.get(i);
            Cell cell = getCellWithMissingCellPolicy(propertiesRow, i + 1);
            if (property.getPreferredName().equals(TEMPLATE_FIXED_PROPERTY_NAME)) {
                item.setName(getCellStringValue(cell));
            } else if (property.getPreferredName().equals(TEMPLATE_FIXED_PROPERTY_DESCRIPTION)) {
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
}
