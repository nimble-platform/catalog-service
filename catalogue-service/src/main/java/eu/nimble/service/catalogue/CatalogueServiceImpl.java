package eu.nimble.service.catalogue;

import eu.nimble.service.catalogue.category.CategoryServiceManager;
import eu.nimble.service.catalogue.exception.CatalogueServiceException;
import eu.nimble.service.catalogue.exception.TemplateParseException;
import eu.nimble.service.catalogue.model.category.Category;
import eu.nimble.service.catalogue.persistence.util.CatalogueLinePersistenceUtil;
import eu.nimble.service.catalogue.persistence.util.CataloguePersistenceUtil;
import eu.nimble.service.catalogue.sync.MarmottaSynchronizer;
import eu.nimble.service.catalogue.template.TemplateGenerator;
import eu.nimble.service.catalogue.template.TemplateParser;
import eu.nimble.service.catalogue.util.DataIntegratorUtil;
import eu.nimble.service.catalogue.util.SpringBridge;
import eu.nimble.service.catalogue.validation.CatalogueValidator;
import eu.nimble.service.catalogue.validation.ValidationException;
import eu.nimble.service.model.modaml.catalogue.TEXCatalogType;
import eu.nimble.service.model.ubl.catalogue.CatalogueType;
import eu.nimble.service.model.ubl.commonaggregatecomponents.CatalogueLineType;
import eu.nimble.service.model.ubl.commonaggregatecomponents.ItemType;
import eu.nimble.service.model.ubl.commonaggregatecomponents.PartyType;
import eu.nimble.service.model.ubl.commonbasiccomponents.BinaryObjectType;
import eu.nimble.utility.Configuration;
import eu.nimble.utility.HibernateUtility;
import eu.nimble.utility.HttpResponseUtil;
import eu.nimble.utility.JAXBUtility;
import eu.nimble.utility.persistence.resource.EntityIdAwareRepositoryWrapper;
import org.apache.commons.io.IOUtils;
import org.apache.poi.ss.usermodel.Workbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.logging.LogLevel;
import org.springframework.http.HttpStatus;
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
import java.util.stream.IntStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * @author yildiray
 */
public class CatalogueServiceImpl implements CatalogueService {

    private static final Logger logger = LoggerFactory.getLogger(CatalogueServiceImpl.class);
    private static CatalogueService instance = null;
    private static CategoryServiceManager csmInstance = CategoryServiceManager.getInstance();
    private String defaultLanguage = "en";

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
        Workbook wb = csi.generateTemplateForCategory(categoryIds, taxonomyIds,"en");
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
        EntityIdAwareRepositoryWrapper repositoryWrapper = new EntityIdAwareRepositoryWrapper(catalogue.getProviderParty().getPartyIdentification().get(0).getID());
        catalogue = repositoryWrapper.updateEntity(catalogue);

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
            EntityIdAwareRepositoryWrapper repositoryWrapper = new EntityIdAwareRepositoryWrapper(ublCatalogue.getProviderParty().getPartyIdentification().get(0).getID());
            catalogue = repositoryWrapper.updateEntityForPersistCases((T) ublCatalogue);
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
            catalogue = (T) CataloguePersistenceUtil.getCatalogueByUuid(uuid);

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

        if (standard == Configuration.Standard.UBL) {
            catalogue = (T) CataloguePersistenceUtil.getCatalogueForParty(id, partyId);

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
                EntityIdAwareRepositoryWrapper repositoryWrapper = new EntityIdAwareRepositoryWrapper(catalogue.getProviderParty().getPartyIdentification().get(0).getID());
                repositoryWrapper.deleteEntity(catalogue);

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
    public Workbook generateTemplateForCategory(List<String> categoryIds, List<String> taxonomyIds,String templateLanguage) {
        List<Category> categories = new ArrayList<>();
        for (int i = 0; i < categoryIds.size(); i++) {
            Category category = csmInstance.getCategory(taxonomyIds.get(i), categoryIds.get(i));
            categories.add(category);
        }

        TemplateGenerator templateGenerator = new TemplateGenerator();
        Workbook template = templateGenerator.generateTemplateForCategory(categories,templateLanguage);
        return template;
    }

    @Override
    public CatalogueType parseCatalogue(InputStream catalogueTemplate, String uploadMode, PartyType party) {
        CatalogueType catalogue = getCatalogue("default", party.getPartyIdentification().get(0).getID());
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

                        // item is available
                    } else {
                        // prepare the new binary content
                        IOUtils.copy(imagePackage, baos);
                        BinaryObjectType binaryObject = new BinaryObjectType();
                        binaryObject.setMimeCode(mimeType);
                        binaryObject.setFileName(ze.getName());
                        binaryObject.setValue(baos.toByteArray());

                        // check whether the image is already attached to the item
                        ItemType finalItem = item;
                        ZipEntry finalZe = ze;
                        int itemIndex = IntStream.range(0, item.getProductImage().size())
                                .filter(i -> finalItem.getProductImage().get(i).getFileName().contentEquals(finalZe.getName()))
                                .findFirst()
                                .orElse(-1);
                        // if an image exists with the same name put it to the previous index
                        if(itemIndex != -1) {
                            item.getProductImage().remove(itemIndex);
                            item.getProductImage().add(itemIndex, binaryObject);
                        } else {
                            item.getProductImage().add(binaryObject);
                        }

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

            CatalogueValidator catalogueValidator = new CatalogueValidator(catalogue);
            try {
                catalogueValidator.validate();
            } catch (ValidationException e) {
                String msg = e.getMessage();
                logger.error(msg, e);
                throw new CatalogueServiceException(msg, e);
            }

            updateCatalogue(catalogue);

            return catalogue;

        } catch (IOException e) {
            String msg = "Failed to get next entry";
            logger.error(msg, e);
            throw new CatalogueServiceException(msg, e);
        }
    }

    @Override
    public CatalogueType removeAllImagesFromCatalogue(CatalogueType catalogueType) {

        return null;
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
        T catalogueLine = (T) CatalogueLinePersistenceUtil.getCatalogueLine(catalogueId, catalogueLineId);
        return catalogueLine;
    }

    @Override
    public CatalogueLineType addLineToCatalogue(CatalogueType catalogue, CatalogueLineType catalogueLine) {
        catalogue.getCatalogueLine().add(catalogueLine);
        DataIntegratorUtil.ensureCatalogueDataIntegrityAndEnhancement(catalogue);
        EntityIdAwareRepositoryWrapper repositoryWrapper = new EntityIdAwareRepositoryWrapper(catalogue.getProviderParty().getPartyIdentification().get(0).getID());
        catalogue = repositoryWrapper.updateEntity(catalogue);
        catalogueLine = catalogue.getCatalogueLine().get(catalogue.getCatalogueLine().size() - 1);

        // add synchronization record
        MarmottaSynchronizer.getInstance().addRecord(MarmottaSynchronizer.SyncStatus.UPDATE, catalogue.getUUID());

        return catalogueLine;
    }

    @Override
    public CatalogueLineType updateCatalogueLine(CatalogueLineType catalogueLine) {
        CatalogueType catalogue = getCatalogue(catalogueLine.getGoodsItem().getItem().getCatalogueDocumentReference().getID());
        DataIntegratorUtil.ensureCatalogueLineDataIntegrityAndEnhancement(catalogueLine, catalogue);
        EntityIdAwareRepositoryWrapper repositoryWrapper = new EntityIdAwareRepositoryWrapper(catalogueLine.getGoodsItem().getItem().getManufacturerParty().getPartyIdentification().get(0).getID());
        catalogueLine = repositoryWrapper.updateEntity(catalogueLine);

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
            EntityIdAwareRepositoryWrapper repositoryWrapper = new EntityIdAwareRepositoryWrapper(catalogueLine.getGoodsItem().getItem().getManufacturerParty().getPartyIdentification().get(0).getID());
            repositoryWrapper.deleteEntityByHjid(CatalogueLineType.class, hjid);

            // add synchronization record
            MarmottaSynchronizer.getInstance().addRecord(MarmottaSynchronizer.SyncStatus.UPDATE, catalogueId);
        }
    }
}
