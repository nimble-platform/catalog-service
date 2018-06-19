/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package eu.nimble.service.catalogue;

import eu.nimble.service.catalogue.category.CategoryServiceManager;
import eu.nimble.service.catalogue.model.category.Category;
import eu.nimble.service.model.ubl.commonaggregatecomponents.*;
import eu.nimble.service.model.ubl.commonbasiccomponents.CodeType;
import eu.nimble.utility.config.CatalogueServiceConfig;
import eu.nimble.service.catalogue.exception.CatalogueServiceException;
import eu.nimble.service.catalogue.exception.TemplateParseException;
import eu.nimble.service.catalogue.sync.MarmottaSynchronizer;
import eu.nimble.service.catalogue.template.TemplateGenerator;
import eu.nimble.service.catalogue.template.TemplateParser;
import eu.nimble.service.model.modaml.catalogue.TEXCatalogType;
import eu.nimble.service.model.ubl.catalogue.CatalogueType;
import eu.nimble.service.model.ubl.commonbasiccomponents.BinaryObjectType;
import eu.nimble.utility.Configuration;
import eu.nimble.utility.HibernateUtility;
import eu.nimble.utility.JAXBUtility;
import org.apache.commons.io.IOUtils;
import org.apache.poi.ss.usermodel.Workbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sun.reflect.generics.reflectiveObjects.NotImplementedException;

import javax.activation.MimetypesFileTypeMap;
import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * @author yildiray
 */
public class CatalogueServiceImpl implements CatalogueService {

    private static final Logger logger = LoggerFactory.getLogger(CatalogueServiceImpl.class);
    private static CatalogueService instance = null;
    private static CategoryServiceManager csmInstance = CategoryServiceManager.getInstance();

    private CatalogueServiceConfig config = CatalogueServiceConfig.getInstance();

    private CatalogueServiceImpl() {
    }

    public static CatalogueService getInstance() {
        if (instance == null) {
            return new CatalogueServiceImpl();
        } else {
            return instance;
        }
    }

    public static void main(String[] args) throws IOException {
        CatalogueServiceImpl csi = new CatalogueServiceImpl();

        String filePath = "C:\\Users\\suat\\Desktop\\multtemp" + System.currentTimeMillis() + ".xlsx";
        List<String> categoryIds = new ArrayList<>();
        categoryIds.add("0173-1#01-AKJ052#013");
        //categoryIds.add("http://www.semanticweb.org/ontologies/2017/8/FurnitureSectorOntology.owl#Glue");
        categoryIds.add("http://www.semanticweb.org/ontologies/2017/8/FurnitureSectorOntology.owl#MDFBoard");
        //categoryIds.add("0173-1#01-BAC439#012");
        List<String> taxonomyIds = new ArrayList<>();
        taxonomyIds.add("eClass");
        taxonomyIds.add("FurnitureOntology");
        //taxonomyIds.add("eClass");
        Workbook wb = csi.generateTemplateForCategory(categoryIds, taxonomyIds);
        wb.write(new FileOutputStream(filePath));
        wb.close();

//        String filePath = "C:\\Users\\suat\\Desktop\\multtemp.xlsx";
//        InputStream is = new FileInputStream(filePath);
//        PartyType party = new PartyType();
//        CatalogueType catalogue = csi.addCatalogue(is, party);
//        System.out.println(catalogue.getCatalogueLine().size());
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

        checkReferencesInCatalogue(catalogue);

        catalogue = addParentCategories(catalogue);
        // merge the hibernate object
        catalogue = (CatalogueType) HibernateUtility.getInstance(Configuration.UBL_PERSISTENCE_UNIT_NAME).update(catalogue);

        // add synchronization record
        MarmottaSynchronizer.getInstance().addRecord(MarmottaSynchronizer.SyncStatus.UPDATE, catalogue.getUUID());
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
        catalogue = addCatalogue(catalogue, standard);

        return catalogue;
    }

    @Override
    public <T> T addCatalogue(T catalogue, Configuration.Standard standard) {
        if (standard == Configuration.Standard.UBL) {
            // create a globally unique identifier
            CatalogueType ublCatalogue = (CatalogueType) catalogue;
            String uuid = UUID.randomUUID().toString();
            ublCatalogue.setUUID(uuid);

            ublCatalogue = addParentCategories(ublCatalogue);
            // set references from items to the catalogue
            for(CatalogueLineType line : ublCatalogue.getCatalogueLine()) {
                DocumentReferenceType docRef = new DocumentReferenceType();
                docRef.setID(((CatalogueType) catalogue).getUUID());
                line.getGoodsItem().getItem().setCatalogueDocumentReference(docRef);

                // Transport Service
                if(line.getGoodsItem().getItem().getTransportationServiceDetails() != null){
                    CommodityClassificationType commodityClassificationType = new CommodityClassificationType();
                    CodeType codeType = new CodeType();
                    codeType.setListID("Default");
                    codeType.setName("Transport Service");
                    codeType.setValue("Transport Service");
                    commodityClassificationType.setItemClassificationCode(codeType);
                    line.getGoodsItem().getItem().getCommodityClassification().add(commodityClassificationType);
                }
                // Product
                else{
                    CommodityClassificationType commodityClassificationType = new CommodityClassificationType();
                    CodeType codeType = new CodeType();
                    codeType.setListID("Default");
                    codeType.setName("Product");
                    codeType.setValue("Product");
                    commodityClassificationType.setItemClassificationCode(codeType);
                    line.getGoodsItem().getItem().getCommodityClassification().add(commodityClassificationType);
                }
            }

            checkReferencesInCatalogue(ublCatalogue);

            // persist the catalogue in relational DB
            HibernateUtility.getInstance(Configuration.UBL_PERSISTENCE_UNIT_NAME).persist(ublCatalogue);
            logger.info("Catalogue with uuid: {} persisted in DB", uuid.toString());

            // add synchronization record
            MarmottaSynchronizer.getInstance().addRecord(MarmottaSynchronizer.SyncStatus.ADD, uuid);

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
                    + " WHERE catalogue.UUID = '" + uuid + "'";

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
            query = "SELECT catalogue FROM CatalogueType as catalogue "
                    + " JOIN catalogue.providerParty as catalogue_provider_party"
                    + " WHERE catalogue.ID = '" + id + "'"
                    + " AND catalogue_provider_party.ID = '" + partyId + "'";

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

                // add synchronization record
                MarmottaSynchronizer.getInstance().addRecord(MarmottaSynchronizer.SyncStatus.DELETE, uuid);
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

    @Override
    public Workbook generateTemplateForCategory(List<String> categoryIds, List<String> taxonomyIds) {
        List<Category> categories = new ArrayList<>();
        for (int i = 0; i < categoryIds.size(); i++) {
            Category category = csmInstance.getCategory(taxonomyIds.get(i), categoryIds.get(i));
            categories.add(category);
        }

        TemplateGenerator templateGenerator = new TemplateGenerator();
        Workbook template = templateGenerator.generateTemplateForCategory(categories);
        return template;
    }

    @Override
    public CatalogueType addCatalogue(InputStream catalogueTemplate, String uploadMode, PartyType party) {
        CatalogueType catalogue = getCatalogue("default", party.getID());
        boolean newCatalogue = false;
        if (catalogue == null) {
            newCatalogue = true;
        }

        TemplateParser templateParser = new TemplateParser(party);
        List<CatalogueLineType> catalogueLines = null;
        try {
            catalogueLines = templateParser.getCatalogueLines(catalogueTemplate);

        } catch (TemplateParseException e) {
            String msg = e.getMessage();
            msg = msg != null ? msg : "Failed to parse the template";
            throw new CatalogueServiceException(msg, e);
        }

        if (newCatalogue) {
            catalogue = new CatalogueType();
            catalogue.setID("default");
            catalogue.setProviderParty(party);
            catalogue.setCatalogueLine(catalogueLines);
            checkReferencesInCatalogue(catalogue);

            return addCatalogue(catalogue);

        } else {
            updateLinesForUploadMode(catalogue, uploadMode, catalogueLines);
            checkReferencesInCatalogue(catalogue);

            return updateCatalogue(catalogue);
        }
    }

    /**
     * Populates catalogue line list of the catalogue based on the given update mode.
     */
    private void updateLinesForUploadMode(CatalogueType catalogue, String uploadMode, List<CatalogueLineType> newLines) {
        List<CatalogueLineType> mergedList = new ArrayList<>();
        if(uploadMode.compareToIgnoreCase("replace") == 0) {
            mergedList = newLines;

        } else {

            // first process the existing list by also considering potential new versions
            for (int i = 0; i < catalogue.getCatalogueLine().size(); i++) {
                CatalogueLineType lineToMerge = catalogue.getCatalogueLine().get(i);
                for (int j = 0; j < newLines.size(); j++) {
                    if (newLines.get(j).getGoodsItem().getItem().getManufacturersItemIdentification().getID().equals(
                            catalogue.getCatalogueLine().get(i).getGoodsItem().getItem().getManufacturersItemIdentification().getID())) {
                        lineToMerge = newLines.get(j);
                        break;
                    }
                }
                mergedList.add(lineToMerge);

                // insert new items
                for (CatalogueLineType newLine : newLines) {
                    boolean alreadyMerged = false;
                    for (CatalogueLineType mergedLine : mergedList) {
                        if (newLine.getGoodsItem().getItem().getManufacturersItemIdentification().getID().equals(
                                mergedLine.getGoodsItem().getItem().getManufacturersItemIdentification().getID())) {
                            alreadyMerged = true;
                            break;
                        }
                    }
                    if (!alreadyMerged) {
                        mergedList.add(newLine);
                    }
                }
            }
        }
        catalogue.setCatalogueLine(mergedList);
    }

    @Override
    public void addImagesToProducts(ZipInputStream imagePackage, String catalogueUuid) {
        try {
            CatalogueType catalogue = getCatalogue(catalogueUuid);
            ZipEntry ze = imagePackage.getNextEntry();

            while (ze != null) {
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                try {
                    String fileName = ze.getName();
                    String mimeType = MimetypesFileTypeMap.getDefaultFileTypeMap().getContentType(fileName);
                    String prefix = fileName.split("\\.")[0];

                    // find the item according to the prefix provided in the image name
                    ItemType item = null;
                    for(CatalogueLineType line : catalogue.getCatalogueLine()) {
                        if(line.getGoodsItem().getItem().getManufacturersItemIdentification().getID().contentEquals(prefix)) {
                            item = line.getGoodsItem().getItem();
                            break;
                        }
                    }
                    if(item == null) {
                        logger.warn("No product to assign image with prefix: {}", prefix);

                    } else {
                        IOUtils.copy(imagePackage, baos);
                        BinaryObjectType binaryObject = new BinaryObjectType();
                        binaryObject.setMimeCode(mimeType);
                        binaryObject.setFileName(ze.getName());
                        binaryObject.setValue(baos.toByteArray());
                        item.getProductImage().add(binaryObject);
                        logger.info("Image {} added to item {}", fileName, item.getManufacturersItemIdentification().getID());
                    }

                } catch (IOException e) {
                    logger.warn("Failed to get data from the zip entry: {}", ze.getName(), e);
                } finally {
                    try {
                        baos.close();
                    } catch (IOException e) {
                        logger.warn("Failed to close baos", e);
                    }
                }
                imagePackage.closeEntry();
                ze = imagePackage.getNextEntry();
            }

            updateCatalogue(catalogue);
        } catch (IOException e) {
            String msg = "Failed to get next entry";
            logger.error(msg, e);
            throw new CatalogueServiceException(msg, e);
        }
    }

    @Override
    public List<Configuration.Standard> getSupportedStandards() {
        return Arrays.asList(Configuration.Standard.values());
    }

    /*
     * Catalogue-line level endpoints
     */

    @Override
    public <T> T getCatalogueLine(String catalogueId, String catalogueLineId) {
        T catalogueLine = null;
        List<T> resultSet;

        String query = "SELECT cl FROM CatalogueLineType as cl, CatalogueType as c "
                + " JOIN c.catalogueLine as clj"
                + " WHERE c.UUID = '" + catalogueId + "' "
                + " AND cl.ID = '" + catalogueLineId + "' "
                + " AND clj.ID = cl.ID ";

        resultSet = (List<T>) HibernateUtility.getInstance(Configuration.UBL_PERSISTENCE_UNIT_NAME)
                .loadAll(query);
        if (resultSet.size() > 0) {
            catalogueLine = (T) resultSet.get(0);
        }

        return catalogueLine;
    }

    // TODO test
    @Override
    public CatalogueLineType addLineToCatalogue(CatalogueType catalogue, CatalogueLineType catalogueLine) {
        // add parents of the selected category to commodity classifications of the item
        for(CommodityClassificationType cct : getParentCategories(catalogueLine.getGoodsItem().getItem().getCommodityClassification())){
            catalogueLine.getGoodsItem().getItem().getCommodityClassification().add(cct);
        }
        // Transport Service
        if(catalogueLine.getGoodsItem().getItem().getTransportationServiceDetails() != null){
            CommodityClassificationType commodityClassificationType = new CommodityClassificationType();
            CodeType codeType = new CodeType();
            codeType.setListID("Default");
            codeType.setName("Transport Service");
            codeType.setValue("Transport Service");
            commodityClassificationType.setItemClassificationCode(codeType);
            catalogueLine.getGoodsItem().getItem().getCommodityClassification().add(commodityClassificationType);
        }
        // Product
        else{
            CommodityClassificationType commodityClassificationType = new CommodityClassificationType();
            CodeType codeType = new CodeType();
            codeType.setListID("Default");
            codeType.setName("Product");
            codeType.setValue("Product");
            commodityClassificationType.setItemClassificationCode(codeType);
            catalogueLine.getGoodsItem().getItem().getCommodityClassification().add(commodityClassificationType);
        }
        catalogue.getCatalogueLine().add(catalogueLine);
        checkReferencesInCatalogue(catalogue);

        catalogue = (CatalogueType) HibernateUtility.getInstance(Configuration.UBL_PERSISTENCE_UNIT_NAME).update(catalogue);
        catalogueLine = catalogue.getCatalogueLine().get(catalogue.getCatalogueLine().size()-1);

        // add synchronization record
        MarmottaSynchronizer.getInstance().addRecord(MarmottaSynchronizer.SyncStatus.UPDATE, catalogue.getUUID());

        return catalogueLine;
    }

    @Override
    public CatalogueLineType updateCatalogueLine(CatalogueLineType catalogueLine) {
        // add parents of the selected category to commodity classifications of the item
        for(CommodityClassificationType cct : getParentCategories(catalogueLine.getGoodsItem().getItem().getCommodityClassification())){
            catalogueLine.getGoodsItem().getItem().getCommodityClassification().add(cct);
        }
        catalogueLine = (CatalogueLineType) HibernateUtility.getInstance(Configuration.UBL_PERSISTENCE_UNIT_NAME).update(catalogueLine);

        // add synchronization record
        // Not UUID but ID of the document reference should be used.
        // While UUID is the unique identifier of the reference itself, ID keeps the unique identifier of the catalogue.
        MarmottaSynchronizer.getInstance().addRecord(MarmottaSynchronizer.SyncStatus.UPDATE, catalogueLine.getGoodsItem().getItem().getCatalogueDocumentReference().getID());

        return catalogueLine;
    }

    @Override
    public void deleteCatalogueLineById(String catalogueId, String id) {
        // delete catalogue from relational db
        CatalogueLineType catalogueLine = getCatalogueLine(catalogueId, id);

        if (catalogueLine != null) {
            Long hjid = catalogueLine.getHjid();
            HibernateUtility.getInstance(Configuration.UBL_PERSISTENCE_UNIT_NAME).delete(CatalogueLineType.class, hjid);

            // add synchronization record
            MarmottaSynchronizer.getInstance().addRecord(MarmottaSynchronizer.SyncStatus.UPDATE, catalogueId);
        }
    }

    private void checkReferencesInCatalogue(CatalogueType catalogue) {
        for(CatalogueLineType line : catalogue.getCatalogueLine()) {
            // check catalogue line ids
            // make sure that line IDs and the manufacturer item IDs are the same
            String manufacturerItemId = line.getGoodsItem().getItem().getManufacturersItemIdentification().getID();
            if(manufacturerItemId == null && line.getID() == null) {
                line.setID(UUID.randomUUID().toString());
                line.getGoodsItem().getItem().getManufacturersItemIdentification().setID(line.getID());

            } else if(line.getID() == null) {
                line.setID(manufacturerItemId);

            } else if(manufacturerItemId == null) {
                line.getGoodsItem().getItem().getManufacturersItemIdentification().setID(line.getID());
            }

            // set references from items to the catalogue
            DocumentReferenceType docRef = new DocumentReferenceType();
            docRef.setID(catalogue.getUUID());
            line.getGoodsItem().getItem().setCatalogueDocumentReference(docRef);
        }
    }

    private CatalogueType addParentCategories(CatalogueType catalogueType){
        for(CatalogueLineType line : catalogueType.getCatalogueLine()) {
            // add parents of the selected category to commodity classifications of the item
            for(CommodityClassificationType cct : getParentCategories(line.getGoodsItem().getItem().getCommodityClassification())){
                line.getGoodsItem().getItem().getCommodityClassification().add(cct);
            }
        }
        return catalogueType;
    }

    private List<CommodityClassificationType> getParentCategories(List<CommodityClassificationType> commodityClassifications){
        List<CommodityClassificationType> commodityClassificationTypeList = new ArrayList<>();
        // find parents of the selected categories
        for(CommodityClassificationType cct : commodityClassifications){
            // Default categories have no parents
            if(cct.getItemClassificationCode().getListID().contentEquals("Default")){
                continue;
            }
            CategoryServiceManager csm = CategoryServiceManager.getInstance();
            List<Category> parentCategories = csm.getParentCategories(cct.getItemClassificationCode().getListID(),cct.getItemClassificationCode().getValue());

            for(int i = 0; i< parentCategories.size()-1;i++){
                Category category = parentCategories.get(i);
                CommodityClassificationType commodityClassificationType = new CommodityClassificationType();
                CodeType codeType = new CodeType();
                codeType.setValue(category.getId());
                codeType.setName(category.getPreferredName());
                codeType.setListID(category.getTaxonomyId());
                codeType.setURI(category.getCategoryUri());
                commodityClassificationType.setItemClassificationCode(codeType);
                if(!commodityClassificationTypeList.contains(commodityClassificationType) && !commodityClassifications.contains(commodityClassificationType)){
                    commodityClassificationTypeList.add(commodityClassificationType);
                }
            }
        }
        return commodityClassificationTypeList;
    }

    @Override
    public boolean existCatalogueLineById(String catalogueId, String lineId,Long hjid) {
        List<Long> resultSet;

        String query;
        // addCatalogueLine
        if(hjid == null){
            query = "SELECT COUNT(cl) FROM CatalogueLineType as cl, CatalogueType as c "
                    + " JOIN c.catalogueLine as clj"
                    + " WHERE c.UUID = '" + catalogueId + "' "
                    + " AND cl.ID = '" + lineId + "' "
                    + " AND clj.ID = cl.ID ";
        }
        // updateCatalogueLine
        else{
            query = "SELECT COUNT(clj) FROM CatalogueType as c "
                    + " JOIN c.catalogueLine as clj"
                    + " WHERE c.UUID = '" + catalogueId + "'"
                    + " AND clj.hjid <> "+hjid
                    + " AND clj.ID = '"+lineId+"'";
        }
        resultSet = (List<Long>) HibernateUtility.getInstance(Configuration.UBL_PERSISTENCE_UNIT_NAME)
                .loadAll(query);
        if (resultSet.size() > 0 && resultSet.get(0) > 0) {
            return true;
        }

        return false;
    }
}
