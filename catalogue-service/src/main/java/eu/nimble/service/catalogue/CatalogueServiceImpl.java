/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package eu.nimble.service.catalogue;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import eu.nimble.service.catalogue.category.CategoryServiceManager;
import eu.nimble.service.catalogue.exception.CatalogueServiceException;
import eu.nimble.service.catalogue.exception.TemplateParseException;
import eu.nimble.service.catalogue.model.category.Category;
import eu.nimble.service.catalogue.sync.MarmottaSynchronizer;
import eu.nimble.service.catalogue.template.TemplateGenerator;
import eu.nimble.service.catalogue.template.TemplateParser;
import eu.nimble.service.catalogue.util.BinaryContentUtil;
import eu.nimble.service.catalogue.util.DataIntegratorUtil;
import eu.nimble.service.catalogue.util.SpringBridge;
import eu.nimble.service.model.modaml.catalogue.TEXCatalogType;
import eu.nimble.service.model.ubl.catalogue.CatalogueType;
import eu.nimble.service.model.ubl.commonaggregatecomponents.CatalogueLineType;
import eu.nimble.service.model.ubl.commonaggregatecomponents.ItemType;
import eu.nimble.service.model.ubl.commonaggregatecomponents.PartyType;
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
//        CatalogueType catalogue = csi.parseCatalogue(is, party);
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

        DataIntegratorUtil.ensureCatalogueDataIntegrityAndEnhancement(catalogue);
        // merge the hibernate object
//        catalogue = (CatalogueType) HibernateUtility.getInstance(Configuration.UBL_PERSISTENCE_UNIT_NAME).update(catalogue);

        // while updating catalogue, be sure that binary contents are updated as well
        // get existing catalogue to compare it with the updated one
        CatalogueType existingCatalogue = SpringBridge.getInstance().getCatalogueRepository().getCatalogueByUuid(catalogue.getUUID());
        // get uris of binary contents in existing catalogue
        List<String> existingUris = SpringBridge.getInstance().getTransactionEnabledSerializationUtilityBinary().serializeBinaryObject(existingCatalogue);
        // get uris of binary contents in the updated catalogue
        List<String> uris = SpringBridge.getInstance().getTransactionEnabledSerializationUtilityBinary().serializeBinaryObject(catalogue);

        // remove binary contents which do not exist in the updated catalogue
        List<String> urisToBeDeleted = new ArrayList<>();
        for (String uri : existingUris) {
            if (!uris.contains(uri)) {
                urisToBeDeleted.add(uri);
            }
        }
        BinaryContentUtil.removeBinaryContentFromDatabase(urisToBeDeleted);

        // then, add new binary contents to database
        try {
            catalogue = BinaryContentUtil.removeBinaryContentFromCatalogue(new ObjectMapper().writeValueAsString(catalogue));
        } catch (Exception e) {
            logger.error("Failed to remove binary contents from the catalogue with id {}: ", catalogue.getUUID(), e);
        }

        catalogue = SpringBridge.getInstance().getCatalogueRepository().save(catalogue);

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
        return addCatalogueWithUUID(catalogue, standard, null);
    }

    @Override
    public <T> T addCatalogueWithUUID(T catalogue, Configuration.Standard standard, String uuid) {
        if (standard == Configuration.Standard.UBL) {
            CatalogueType ublCatalogue = (CatalogueType) catalogue;
            if (uuid != null) {
                ublCatalogue.setUUID(uuid);
            } else {
                uuid = UUID.randomUUID().toString();
                // create a globally unique identifier
                ublCatalogue.setUUID(uuid);
            }

            DataIntegratorUtil.ensureCatalogueDataIntegrityAndEnhancement(ublCatalogue);
            // persist the catalogue in relational DB
//            HibernateUtility.getInstance(Configuration.UBL_PERSISTENCE_UNIT_NAME).update(ublCatalogue);
            // before adding catalogue, remove binary contents from it and save them to binary content database
            try {
                ublCatalogue = BinaryContentUtil.removeBinaryContentFromCatalogue(new ObjectMapper().writeValueAsString(ublCatalogue));
            } catch (Exception e) {
                logger.error("Failed to remove binary content from the catalogue with id:{}", ublCatalogue.getUUID(), e);
            }

            SpringBridge.getInstance().getCatalogueRepository().updateEntity(ublCatalogue);
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
//            query = "SELECT catalogue FROM CatalogueType catalogue "
//                    + " WHERE catalogue.UUID = '" + uuid + "'";
//
//            resultSet = (List<T>) HibernateUtility.getInstance(Configuration.UBL_PERSISTENCE_UNIT_NAME)
//                    .loadAll(query);
//            if (resultSet.size() > 0) {
//                catalogue = (T) resultSet.get(0);
//            }
            catalogue = (T) SpringBridge.getInstance().getCatalogueRepository().getCatalogueByUuid(uuid);

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

        String query;
        if (standard == Configuration.Standard.UBL) {
//            query = "SELECT catalogue FROM CatalogueType as catalogue "
//                    + " JOIN catalogue.providerParty as catalogue_provider_party"
//                    + " WHERE catalogue.ID = '" + id + "'"
//                    + " AND catalogue_provider_party.ID = '" + partyId + "'";
//
//            resultSet = (List<T>) HibernateUtility.getInstance(Configuration.UBL_PERSISTENCE_UNIT_NAME)
//                    .loadAll(query);
            catalogue = (T) SpringBridge.getInstance().getCatalogueRepository().getCatalogueForParty(id, partyId);

        } else if (standard == Configuration.Standard.MODAML) {
            logger.warn("Fetching catalogues with id and party id from MODAML repository is not implemented yet");
            throw new NotImplementedException();
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
//                HibernateUtility.getInstance(Configuration.UBL_PERSISTENCE_UNIT_NAME).delete(CatalogueType.class, hjid);
                SpringBridge.getInstance().getCatalogueRepository().delete(hjid);

                // add synchronization record
                MarmottaSynchronizer.getInstance().addRecord(MarmottaSynchronizer.SyncStatus.DELETE, uuid);
                logger.info("Deleted catalogue with uuid: {}", uuid);
            } else {
                logger.info("No catalogue for uuid: {}", uuid);
            }
            // remove binary contents of the catalogue from the binary content database
            try {
                BinaryContentUtil.removeBinaryContentFromDatabase(catalogue);
            } catch (Exception e) {
                logger.error("Failed to remove binary contents from the database for catalogue with id: {}", catalogue.getUUID(), e);
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
    public CatalogueType parseCatalogue(InputStream catalogueTemplate, String uploadMode, PartyType party) {
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
            return catalogue;

        } else {
            updateLinesForUploadMode(catalogue, uploadMode, catalogueLines);
            return catalogue;
        }
    }

    /**
     * Populates catalogue line list of the catalogue based on the given update mode.
     */
    private void updateLinesForUploadMode(CatalogueType catalogue, String uploadMode, List<CatalogueLineType> newLines) {
        List<CatalogueLineType> mergedList = new ArrayList<>();
        if (uploadMode.compareToIgnoreCase("replace") == 0) {
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
        catalogue.getCatalogueLine().clear();
        catalogue.getCatalogueLine().addAll(mergedList);
    }

    @Override
    public CatalogueType addImagesToProducts(ZipInputStream imagePackage, String catalogueUuid) {
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
                    for (CatalogueLineType line : catalogue.getCatalogueLine()) {
                        if (line.getGoodsItem().getItem().getManufacturersItemIdentification().getID().contentEquals(prefix)) {
                            item = line.getGoodsItem().getItem();
                            break;
                        }
                    }
                    if (item == null) {
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

            return catalogue;

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

        String query = "SELECT cl FROM CatalogueLineType as cl, CatalogueType as c "
                + " JOIN c.catalogueLine as clj"
                + " WHERE c.UUID = '" + catalogueId + "' "
                + " AND cl.ID = '" + catalogueLineId + "' "
                + " AND clj.ID = cl.ID ";

//        resultSet = (List<T>) HibernateUtility.getInstance(Configuration.UBL_PERSISTENCE_UNIT_NAME)
//                .loadAll(query);
//        if (resultSet.size() > 0) {
//            catalogueLine = (T) resultSet.get(0);
//        }
        catalogueLine = (T) SpringBridge.getInstance().getCatalogueRepository().getCatalogueLine(catalogueId, catalogueLineId);
        return catalogueLine;
    }

    @Override
    public <T> List<T> getCatalogueLines(String catalogueId, List<String> catalogueLineIds) {

        if (catalogueLineIds.size() == 0) {
            return null;
        }

        List<T> catalogueLines = null;
        List<String> parameterNames = new ArrayList<>();
        List<Object> parameterValues = new ArrayList<>();

        String query = "SELECT cl FROM CatalogueLineType as cl, CatalogueType as c "
                + " JOIN c.catalogueLine as clj"
                + " WHERE c.UUID = :catalogueId "
                + " AND (";

        parameterNames.add("catalogueId");
        parameterValues.add(catalogueId);

        int size = catalogueLineIds.size();
        for (int i = 0; i < size; i++) {
            if (i == size - 1) {
                query += "cl.ID = :lineId" + i + ")";
            } else {
                query += "cl.ID = :lineId" + i + " OR ";
            }

            parameterNames.add("lineId" + i);
            parameterValues.add(catalogueLineIds.get(i));
        }
        query += " AND clj.ID = cl.ID ";

//        catalogueLines = (List<T>) HibernateUtility.getInstance(Configuration.UBL_PERSISTENCE_UNIT_NAME)
//                .loadAll(query);
        catalogueLines = SpringBridge.getInstance().getCatalogueRepository().getEntities(query, parameterNames.toArray(new String[parameterNames.size()]), parameterValues.toArray());

        return catalogueLines;
    }

    // TODO test
    @Override
    public CatalogueLineType addLineToCatalogue(CatalogueType catalogue, CatalogueLineType catalogueLine) {
        catalogue.getCatalogueLine().add(catalogueLine);
        DataIntegratorUtil.ensureCatalogueDataIntegrityAndEnhancement(catalogue);

//        catalogue = (CatalogueType) HibernateUtility.getInstance(Configuration.UBL_PERSISTENCE_UNIT_NAME).update(catalogue);
        // before saving the catalogue catalogue, we remove binary contents from the catalogue and save them to binary content database
        try {
            catalogue = BinaryContentUtil.removeBinaryContentFromCatalogue(new ObjectMapper().writeValueAsString(catalogue));
        } catch (Exception e) {
            logger.error("Failed to remove binary content from the catalogue with id {} : ", catalogue.getUUID(), e);
        }

        catalogue = SpringBridge.getInstance().getCatalogueRepository().updateEntity(catalogue);
        catalogueLine = catalogue.getCatalogueLine().get(catalogue.getCatalogueLine().size() - 1);

        // add synchronization record
        MarmottaSynchronizer.getInstance().addRecord(MarmottaSynchronizer.SyncStatus.UPDATE, catalogue.getUUID());

        return catalogueLine;
    }

    @Override
    public CatalogueLineType updateCatalogueLine(CatalogueLineType catalogueLine) {
        CatalogueType catalogue = getCatalogue(catalogueLine.getGoodsItem().getItem().getCatalogueDocumentReference().getID());
        DataIntegratorUtil.ensureCatalogueLineDataIntegrityAndEnhancement(catalogueLine, catalogue);
//        catalogueLine = (CatalogueLineType) HibernateUtility.getInstance(Configuration.UBL_PERSISTENCE_UNIT_NAME).update(catalogueLine);
        // firstly, remove all binary contents belong to the catalogue line from the database

        // get existing catalogue line
        CatalogueLineType existingCatalogueLine = null;
        for (CatalogueLineType catalogueLineType : catalogue.getCatalogueLine()) {
            if (catalogueLineType.getID().equals(catalogueLine.getID())) {
                existingCatalogueLine = catalogueLineType;
            }
        }
        // before updating catalogue line, be sure that we update binary contents as well.
        // get uris of binary contents of existing catalogue line
        List<String> existingUris = SpringBridge.getInstance().getTransactionEnabledSerializationUtilityBinary().serializeBinaryObject(existingCatalogueLine);
        // get uris of binary contents of the updated catalogue line
        List<String> uris = SpringBridge.getInstance().getTransactionEnabledSerializationUtilityBinary().serializeBinaryObject(catalogueLine);
        // remove binary contents which do not exist in the updated catalogue line
        List<String> urisToBeDeleted = new ArrayList<>();
        for (String uri : existingUris) {
            if (!uris.contains(uri)) {
                urisToBeDeleted.add(uri);
            }
        }
        BinaryContentUtil.removeBinaryContentFromDatabase(urisToBeDeleted);
        // then, add new binary contents to database
        try {
            catalogueLine = BinaryContentUtil.removeBinaryContentFromCatalogueLine(new ObjectMapper().writeValueAsString(catalogueLine));
        } catch (Exception e) {
            logger.error("Failed to remove binary content from the catalogue line with id:{}", catalogueLine.getID(), e);
        }
        catalogueLine = SpringBridge.getInstance().getCatalogueLineRepository().save(catalogueLine);

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
//            HibernateUtility.getInstance(Configuration.UBL_PERSISTENCE_UNIT_NAME).delete(CatalogueLineType.class, hjid);
            SpringBridge.getInstance().getCatalogueLineRepository().delete(hjid);

            // delete binary content from the database
            try {
                BinaryContentUtil.removeBinaryContentFromDatabase(catalogueLine);
            } catch (JsonProcessingException e) {
                logger.error("Failed to remove binary content from database for catalogue line with id: {}", catalogueId, e);
            }

            // add synchronization record
            MarmottaSynchronizer.getInstance().addRecord(MarmottaSynchronizer.SyncStatus.UPDATE, catalogueId);
        }
    }
}
