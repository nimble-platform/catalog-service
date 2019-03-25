package eu.nimble.service.catalogue;

import eu.nimble.service.catalogue.category.IndexCategoryService;
import eu.nimble.service.catalogue.exception.CatalogueServiceException;
import eu.nimble.service.catalogue.exception.TemplateParseException;
import eu.nimble.service.catalogue.model.catalogue.CatalogueLineSortOptions;
import eu.nimble.service.catalogue.model.catalogue.CataloguePaginationResponse;
import eu.nimble.service.catalogue.model.category.Category;
import eu.nimble.service.catalogue.persistence.util.CatalogueLinePersistenceUtil;
import eu.nimble.service.catalogue.persistence.util.CataloguePersistenceUtil;
import eu.nimble.service.catalogue.index.ItemIndexClient;
import eu.nimble.service.catalogue.template.TemplateGenerator;
import eu.nimble.service.catalogue.template.TemplateParser;
import eu.nimble.service.catalogue.util.DataIntegratorUtil;
import eu.nimble.service.catalogue.util.LanguageUtil;
import eu.nimble.service.catalogue.validation.CatalogueValidator;
import eu.nimble.service.catalogue.validation.ValidationException;
import eu.nimble.service.model.modaml.catalogue.TEXCatalogType;
import eu.nimble.service.model.ubl.catalogue.CatalogueType;
import eu.nimble.service.model.ubl.commonaggregatecomponents.CatalogueLineType;
import eu.nimble.service.model.ubl.commonaggregatecomponents.CommodityClassificationType;
import eu.nimble.service.model.ubl.commonaggregatecomponents.ItemType;
import eu.nimble.service.model.ubl.commonaggregatecomponents.PartyType;
import eu.nimble.service.model.ubl.commonbasiccomponents.BinaryObjectType;
import eu.nimble.utility.Configuration;
import eu.nimble.utility.HibernateUtility;
import eu.nimble.utility.JAXBUtility;
import eu.nimble.utility.persistence.resource.EntityIdAwareRepositoryWrapper;
import org.apache.commons.io.IOUtils;
import org.apache.poi.ss.usermodel.Workbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import sun.reflect.generics.reflectiveObjects.NotImplementedException;

import javax.activation.MimetypesFileTypeMap;
import javax.xml.crypto.Data;
import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.stream.IntStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * @author yildiray
 */
@Component
public class CatalogueServiceImpl implements CatalogueService {

    private static final Logger logger = LoggerFactory.getLogger(CatalogueServiceImpl.class);

    @Autowired
    private ItemIndexClient itemIndexClient;
    @Autowired
    private IndexCategoryService indexCategoryService;

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
        logger.info("Catalogue with uuid: {} updated in DB", catalogue.getUUID());

        // index catalogue
        itemIndexClient.indexCatalogue(catalogue);
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

            // index the catalogue
            itemIndexClient.indexCatalogue((CatalogueType) catalogue);

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
    public CataloguePaginationResponse getCataloguePaginationResponse(String id, String partyId, String categoryName, String searchText, String languageId, CatalogueLineSortOptions sortOption, int limit, int offset) {
        return getCataloguePaginationResponse(id,partyId,categoryName,Configuration.Standard.UBL,searchText,languageId,sortOption,limit,offset);
    }

    @Override
    public <T> T getCataloguePaginationResponse(String id, String partyId,String categoryName, Configuration.Standard standard,String searchText,String languageId,CatalogueLineSortOptions sortOption, int limit, int offset) {
        T catalogueResponse = null;

        if (standard == Configuration.Standard.UBL) {
            catalogueResponse = (T) CataloguePersistenceUtil.getCatalogueLinesForParty(id, partyId,categoryName,searchText,languageId,sortOption,limit,offset);

        } else if (standard == Configuration.Standard.MODAML) {
            logger.warn("Getting CataloguePaginationResponse with catalogue id and party id from MODAML repository is not implemented yet");
            throw new NotImplementedException();
        }

        return catalogueResponse;
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

                // delete indexed catalogue
                itemIndexClient.deleteCatalogue(uuid);
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
            Category category = indexCategoryService.getCategory(taxonomyIds.get(i), categoryIds.get(i));
            categories.add(category);
        }

        TemplateGenerator templateGenerator = new TemplateGenerator();
        Workbook template = templateGenerator.generateTemplateForCategory(categories,templateLanguage);
        return template;
    }

    @Override
    public Map<Workbook,String> generateTemplateForCatalogue(CatalogueType catalogue,String languageId) {
        Map<HashSet<String>,List<CatalogueLineType>> categoryCatalogueLineMap = new HashMap<>();
        // create category-catalogue lines map
        for(CatalogueLineType catalogueLine:catalogue.getCatalogueLine()){
            List<String> uris = new ArrayList<>();
            for(CommodityClassificationType commodityClassification:catalogueLine.getGoodsItem().getItem().getCommodityClassification()){
                if(commodityClassification.getItemClassificationCode().getURI() != null){
                    uris.add(commodityClassification.getItemClassificationCode().getURI());
                }
            }

            HashSet<String> categories = new HashSet<>(uris);
            if(categoryCatalogueLineMap.containsKey(categories)){
                List<CatalogueLineType> catalogueLineTypes = new ArrayList<>();
                catalogueLineTypes.add(catalogueLine);
                catalogueLineTypes.addAll(categoryCatalogueLineMap.get(categories));
                categoryCatalogueLineMap.put(categories,catalogueLineTypes);
            } else{
                categoryCatalogueLineMap.put(categories,Arrays.asList(catalogueLine));
            }
        }
        // workbook-file name map
        Map<Workbook,String> workbooks = new HashMap<>();

        for (Map.Entry<HashSet<String>, List<CatalogueLineType>> entry : categoryCatalogueLineMap.entrySet()) {
            // get categories which are not Default or Custom categories
            List<Category> categories = new ArrayList<>();
            List<CommodityClassificationType> leafCommodityClassifications = DataIntegratorUtil.getLeafCategories(entry.getValue().get(0).getGoodsItem().getItem().getCommodityClassification());
            for(CommodityClassificationType commodityClassification:leafCommodityClassifications){
                categories.add(indexCategoryService.getCategory(commodityClassification.getItemClassificationCode().getListID(),commodityClassification.getItemClassificationCode().getValue()));
            }
            // generate a template for the catalogue lines
            TemplateGenerator templateGenerator = new TemplateGenerator();
            Workbook template = templateGenerator.generateTemplateForCatalogueLines(entry.getValue(),categories,languageId);
            // add it to the map
            workbooks.put(template,createWorkbookName(categories,languageId));
        }
        return workbooks;
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
    private void updateLinesForUploadMode(CatalogueType catalogue, String uploadMode, List<CatalogueLineType> catalogueLines) {
        List<CatalogueLineType> newCatalogueLines = new ArrayList<>();
        if (uploadMode.compareToIgnoreCase("replace") == 0) {
            // since each catalogue line has the same categories, it is OK to get categories using the first one
            CommodityClassificationType defaultCategory = DataIntegratorUtil.getDefaultCategories(catalogueLines.get(0));
            List<String> categoriesUris = DataIntegratorUtil.getCategoryUris(catalogueLines.get(0));
            // catalogue lines which will be removed and will be replaced by the new ones
            List<CatalogueLineType> catalogueLinesToBeRemoved = new ArrayList<>();
            for(CatalogueLineType catalogueLine:catalogue.getCatalogueLine()){
                // firstly,both catalogue line should have the same number of categories,otherwise that means that they do not have the same categories.
                if(catalogueLine.getGoodsItem().getItem().getCommodityClassification().size() == categoriesUris.size() + 1){
                    boolean remove = true;
                    // check catalogue line's categories
                    for (CommodityClassificationType classificationType:catalogueLine.getGoodsItem().getItem().getCommodityClassification()){
                        if(classificationType.getItemClassificationCode().getListID().contentEquals("Default")){
                            if(!defaultCategory.getItemClassificationCode().getValue().contentEquals(classificationType.getItemClassificationCode().getValue())){
                                remove = false;
                                break;
                            }
                        } else if(!categoriesUris.contains(classificationType.getItemClassificationCode().getURI())){
                            remove = false;
                            break;
                        }
                    }

                    if(remove){
                        catalogueLinesToBeRemoved.add(catalogueLine);
                    }
                }
            }

            catalogue.getCatalogueLine().removeAll(catalogueLinesToBeRemoved);
            catalogue.getCatalogueLine().addAll(catalogueLines);
        } else {

            // add catalogue lines which do not match with the existing ones to the new catalogue line list
            for(CatalogueLineType catalogueLine : catalogueLines){
                boolean isNewLine = true;
                for(CatalogueLineType existingCatalogueLine : catalogue.getCatalogueLine()){
                    if(catalogueLine.getGoodsItem().getItem().getManufacturersItemIdentification().getID().contentEquals(
                            existingCatalogueLine.getGoodsItem().getItem().getManufacturersItemIdentification().getID())){
                        isNewLine = false;
                        break;
                    }
                }

                if(isNewLine){
                    newCatalogueLines.add(catalogueLine);
                }
            }

            // find catalogue lines which we need to update
            for(CatalogueLineType catalogueLine : catalogueLines){
                for(CatalogueLineType existingCatalogueLine : catalogue.getCatalogueLine()){
                    if(catalogueLine.getGoodsItem().getItem().getManufacturersItemIdentification().getID().contentEquals(
                            existingCatalogueLine.getGoodsItem().getItem().getManufacturersItemIdentification().getID())){
                        // update existing catalogue line
                        updateExistingCatalogueLine(existingCatalogueLine,catalogueLine);
                    }
                }
            }
        }
        // add new catalogue lines
        catalogue.getCatalogueLine().addAll(newCatalogueLines);
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

            catalogue = updateCatalogue(catalogue);

            return catalogue;

        } catch (IOException e) {
            String msg = "Failed to get next entry";
            logger.error(msg, e);
            throw new CatalogueServiceException(msg, e);
        }
    }

    @Override
    public CatalogueType removeAllImagesFromCatalogue(CatalogueType catalogueType) {
        for(CatalogueLineType catalogueLine:catalogueType.getCatalogueLine()){
            // remove product images from the item
            catalogueLine.getGoodsItem().getItem().getProductImage().clear();
        }
        // update the catalogue
        catalogueType = updateCatalogue(catalogueType);
        return catalogueType;
    }

    @Override
    public List<Configuration.Standard> getSupportedStandards() {
        return Arrays.asList(Configuration.Standard.values());
    }

    /*
     * Catalogue-line level endpoints
     */

    @Override
    public CatalogueLineType getCatalogueLine(long hjid) {
        return CatalogueLinePersistenceUtil.getCatalogueLine(hjid);
    }

    @Override
    public List<CatalogueLineType> getCatalogueLines(List<Long> hjids,CatalogueLineSortOptions sortOption,int limit, int pageNo) {
        return CatalogueLinePersistenceUtil.getCatalogueLines(hjids,sortOption,limit,pageNo);
    }

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

        // index the line
        itemIndexClient.indexCatalogueLine(catalogueLine);

        return catalogueLine;
    }

    @Override
    public CatalogueLineType updateCatalogueLine(CatalogueLineType catalogueLine) {
        CatalogueType catalogue = getCatalogue(catalogueLine.getGoodsItem().getItem().getCatalogueDocumentReference().getID());
        DataIntegratorUtil.ensureCatalogueLineDataIntegrityAndEnhancement(catalogueLine, catalogue);
        EntityIdAwareRepositoryWrapper repositoryWrapper = new EntityIdAwareRepositoryWrapper(catalogueLine.getGoodsItem().getItem().getManufacturerParty().getPartyIdentification().get(0).getID());
        catalogueLine = repositoryWrapper.updateEntity(catalogueLine);

        // index the line
        // Not UUID but ID of the document reference should be used.
        // While UUID is the unique identifier of the reference itself, ID keeps the unique identifier of the catalogue.
        itemIndexClient.indexCatalogueLine(catalogueLine);

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

            // delete indexed item
            itemIndexClient.deleteCatalogueLine(catalogueLine.getHjid());
        }
    }

    private void updateExistingCatalogueLine(CatalogueLineType existingCatalogueLine, CatalogueLineType newCatalogueLine){
        existingCatalogueLine.getGoodsItem().getItem().setName(newCatalogueLine.getGoodsItem().getItem().getName());
        existingCatalogueLine.getGoodsItem().getItem().setDescription(newCatalogueLine.getGoodsItem().getItem().getDescription());
        existingCatalogueLine.getGoodsItem().getItem().setDimension(newCatalogueLine.getGoodsItem().getItem().getDimension());
        existingCatalogueLine.getGoodsItem().getItem().setAdditionalItemProperty(newCatalogueLine.getGoodsItem().getItem().getAdditionalItemProperty());

        existingCatalogueLine.getRequiredItemLocationQuantity().getPrice().setPriceAmount(newCatalogueLine.getRequiredItemLocationQuantity().getPrice().getPriceAmount());
        existingCatalogueLine.getRequiredItemLocationQuantity().getPrice().setBaseQuantity(newCatalogueLine.getRequiredItemLocationQuantity().getPrice().getBaseQuantity());
        existingCatalogueLine.setMinimumOrderQuantity(newCatalogueLine.getMinimumOrderQuantity());
        existingCatalogueLine.setFreeOfChargeIndicator(newCatalogueLine.isFreeOfChargeIndicator());
        existingCatalogueLine.setWarrantyValidityPeriod(newCatalogueLine.getWarrantyValidityPeriod());
        existingCatalogueLine.getWarrantyInformationItems().clear();
        existingCatalogueLine.setWarrantyInformation(newCatalogueLine.getWarrantyInformation());
        existingCatalogueLine.getGoodsItem().getDeliveryTerms().setIncoterms(newCatalogueLine.getGoodsItem().getDeliveryTerms().getIncoterms());
        existingCatalogueLine.getGoodsItem().getDeliveryTerms().setSpecialTerms(newCatalogueLine.getGoodsItem().getDeliveryTerms().getSpecialTerms());
        existingCatalogueLine.getGoodsItem().getDeliveryTerms().setEstimatedDeliveryPeriod(newCatalogueLine.getGoodsItem().getDeliveryTerms().getEstimatedDeliveryPeriod());
        existingCatalogueLine.getRequiredItemLocationQuantity().setApplicableTerritoryAddress(newCatalogueLine.getRequiredItemLocationQuantity().getApplicableTerritoryAddress());
        existingCatalogueLine.getGoodsItem().getDeliveryTerms().setTransportModeCode(newCatalogueLine.getGoodsItem().getDeliveryTerms().getTransportModeCode());
        existingCatalogueLine.getGoodsItem().setContainingPackage(newCatalogueLine.getGoodsItem().getContainingPackage());
    }

    // using the given categories' names, it creates a name for the workbook consisting of the given categories
    private String createWorkbookName(List<Category> categories,String languageId){
        // get category names
        StringBuilder stringBuilder = new StringBuilder();
        int size = categories.size();
        for(int i = 0 ; i < size; i++){
            stringBuilder.append(LanguageUtil.getValue(categories.get(i).getPreferredName(),languageId));
            if(i != size-1){
                stringBuilder.append("_");
            }
        }
        // check whether the filename exceeds the limit (256 characters including the extension)
        if(stringBuilder.length() > 250){
            stringBuilder = new StringBuilder(stringBuilder.substring(0,247));
            stringBuilder.append("...");
        }
        // add the extension
        stringBuilder.append(".xlsx");
        return stringBuilder.toString();
    }
}
