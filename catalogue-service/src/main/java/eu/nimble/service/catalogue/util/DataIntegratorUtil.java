package eu.nimble.service.catalogue.util;

import com.google.common.base.Strings;
import eu.nimble.service.catalogue.category.IndexCategoryService;
import eu.nimble.service.catalogue.exception.InvalidCategoryException;
import eu.nimble.service.catalogue.model.catalogue.ProductStatus;
import eu.nimble.service.catalogue.model.category.Category;
import eu.nimble.service.catalogue.persistence.util.CatalogueDatabaseAdapter;
import eu.nimble.service.model.ubl.catalogue.CatalogueType;
import eu.nimble.service.model.ubl.commonaggregatecomponents.*;
import eu.nimble.service.model.ubl.commonbasiccomponents.CodeType;
import org.apache.commons.lang3.EnumUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

public class DataIntegratorUtil {

    private static String defaultLanguage = "en";

    /**
     * This method synchronizes the catalogue's party (by persisting if a new party or uses an existing one) and updates the catalogue's party with the
     * one managed in the database. Then, it calls the data integrity method for line.
     *
     * This method is supposed to be called for new catalogues
     * @param catalogue
     * @throws InvalidCategoryException
     */
    public static void ensureCatalogueDataIntegrityAndEnhancement(CatalogueType catalogue) throws InvalidCategoryException {
        PartyType partyType = CatalogueDatabaseAdapter.syncPartyInUBLDB(catalogue.getProviderParty());
        catalogue.setProviderParty(partyType);

        for(CatalogueLineType line : catalogue.getCatalogueLine()) {
            ensureCatalogueLineDataIntegrityAndEnhancement(line, catalogue);
        }
    }

    public static void ensureCatalogueLineDataIntegrityAndEnhancement(CatalogueLineType catalogueLine, CatalogueType catalogue) throws InvalidCategoryException {
        ensureCatalogueLineDataIntegrityAndEnhancement(catalogueLine, catalogue.getUUID(), catalogue.getProviderParty());
    }

    public static void ensureCatalogueLineDataIntegrityAndEnhancement(CatalogueLineType catalogueLine, String catalogueUuid, PartyType providerParty) {
        catalogueLine.getGoodsItem().getItem().setManufacturerParty(providerParty);
        setDefaultCategories(catalogueLine);
        setParentCategories(catalogueLine.getGoodsItem().getItem().getCommodityClassification());
        checkCatalogueLineIDs(catalogueLine);
        setCatalogueDocumentReference(catalogueUuid,catalogueLine);
        removePrecedingTrailingSpaces(catalogueLine);
        setCatalogueLineStatus(catalogueLine);
        checkDimensions(catalogueLine);
    }

    private static void checkDimensions(CatalogueLineType catalogueLineType){
        List<String> serviceRootCategories = SpringBridge.getInstance().getTaxonomyManager().getServiceRootCategories();
        // skip the default categories
        List<CommodityClassificationType> nonDefaultCommodityClassificationTypes = catalogueLineType.getGoodsItem().getItem().getCommodityClassification().stream().
                filter(commodityClassificationType -> !commodityClassificationType.getItemClassificationCode().getListID().contentEquals("Default")).collect(Collectors.toList());
        // check whether the item has any service category
        boolean hasServiceCategory = false;
        for (CommodityClassificationType cct : nonDefaultCommodityClassificationTypes) {
            if(serviceRootCategories.contains(cct.getItemClassificationCode().getURI())){
                hasServiceCategory = true;
                break;
            }
        }
        // remove the dimensions from the item if it is a service
        if(hasServiceCategory && catalogueLineType.getGoodsItem().getItem().getDimension() != null){
            catalogueLineType.getGoodsItem().getItem().getDimension().clear();
        }
    }
    private static void setCatalogueLineStatus(CatalogueLineType catalogueLineType){
        if(Strings.isNullOrEmpty(catalogueLineType.getProductStatusType()) || !EnumUtils.isValidEnum(ProductStatus.class,catalogueLineType.getProductStatusType())){
            catalogueLineType.setProductStatusType(ProductStatus.DRAFT.toString());
        }
    }

    public static void setParentCategories(List<CommodityClassificationType> commodityClassifications) throws InvalidCategoryException {
        // get all parents for the given commodity classifications
        IndexCategoryService csm = SpringBridge.getInstance().getIndexCategoryService();
        List<CodeType> parentCategories = csm.getParentCategories(
                commodityClassifications
                        .stream()
                        .map(CommodityClassificationType::getItemClassificationCode)
                        .collect(Collectors.toList()));

        // add parents of the selected category to commodity classifications of the item
        parentCategories.forEach(cat -> {
            CommodityClassificationType cct = new CommodityClassificationType();
            cct.setItemClassificationCode(cat);
            commodityClassifications.add(cct);
        });
    }

    private static void setCatalogueDocumentReference(String catalogueUUID,CatalogueLineType catalogueLine){
        // set references from items to the catalogue
        DocumentReferenceType docRef = new DocumentReferenceType();
        docRef.setID(catalogueUUID);
        catalogueLine.getGoodsItem().getItem().setCatalogueDocumentReference(docRef);
    }

    private static void removePrecedingTrailingSpaces(CatalogueLineType catalogueLine) {
        // product id
        catalogueLine.setID(catalogueLine.getID().trim());
        // product name
        catalogueLine.getGoodsItem().getItem().getName().stream().filter(textType -> textType.getValue() !=null).forEach(textType -> textType.setValue(textType.getValue().trim()));
        // additional properties
        catalogueLine.getGoodsItem().getItem().getAdditionalItemProperty().forEach(itemPropertyType -> {
            itemPropertyType.getName().forEach(textType -> textType.setValue(textType.getValue().trim()));
            itemPropertyType.getValue().forEach(textType -> textType.setValue(textType.getValue().trim()));
            itemPropertyType.getValueQuantity().stream().filter(quantityType -> quantityType.getUnitCode() != null).forEach(quantityType -> quantityType.setUnitCode(quantityType.getUnitCode().trim()));
            itemPropertyType.getValueBinary().forEach(binaryObjectType -> {
                binaryObjectType.setUri(binaryObjectType.getUri().trim());
                binaryObjectType.setFileName(binaryObjectType.getFileName().trim());
                binaryObjectType.setMimeCode(binaryObjectType.getMimeCode().trim());
            });
        });
    }

    private static void checkCatalogueLineIDs(CatalogueLineType line){
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
    }

    private static void setDefaultCategories(CatalogueLineType catalogueLine){
        CommodityClassificationType commodityClassificationType = getDefaultCategories(catalogueLine);
        if(commodityClassificationType != null){
            catalogueLine.getGoodsItem().getItem().getCommodityClassification().add(commodityClassificationType);
        }
    }

    // TODO move this method to a persistence util
    public static List<CommodityClassificationType> getLeafCategories(List<CommodityClassificationType> commodityClassifications) throws InvalidCategoryException {
        // get uris of the given categories
        List<String> categoryUris = new ArrayList<>();
        for(CommodityClassificationType commodityClassificationType:commodityClassifications){
            if(commodityClassificationType.getItemClassificationCode().getURI() != null){
                categoryUris.add(commodityClassificationType.getItemClassificationCode().getURI());
            }
        }

        for(CommodityClassificationType commodityClassificationType:commodityClassifications){
            if (commodityClassificationType.getItemClassificationCode().getURI() != null && !commodityClassificationType.getItemClassificationCode().getListID().contentEquals("Default")) {
                // find parent categories uris
                List<Category> parentCategories = SpringBridge.getInstance().getIndexCategoryService().getParentCategories(commodityClassificationType.getItemClassificationCode().getListID(),commodityClassificationType.getItemClassificationCode().getValue());
                List<String> parentCategoriesUris = new ArrayList<>();
                for(Category category:parentCategories){
                    if(!category.getCategoryUri().contentEquals(commodityClassificationType.getItemClassificationCode().getURI())){
                        parentCategoriesUris.add(category.getCategoryUri());
                    }
                }
                // remove parent categories
                categoryUris.removeAll(parentCategoriesUris);
            }
        }
        // get commodity classifications of leaf categories
        List<CommodityClassificationType> classificationTypes = new ArrayList<>();
        for (CommodityClassificationType commodityClassificationType:commodityClassifications){
            if(!commodityClassificationType.getItemClassificationCode().getListID().contentEquals("Default")
                    && !commodityClassificationType.getItemClassificationCode().getListID().contentEquals("Custom")
                    && commodityClassificationType.getItemClassificationCode().getURI() != null && categoryUris.contains(commodityClassificationType.getItemClassificationCode().getURI())){
                classificationTypes.add(commodityClassificationType);
            }
        }
        return classificationTypes;
    }

    public static CommodityClassificationType getDefaultCategories(CatalogueLineType catalogueLine){
        // check whether we need to add a default category or not
        for (CommodityClassificationType classificationType : catalogueLine.getGoodsItem().getItem().getCommodityClassification()){
            if(classificationType.getItemClassificationCode().getListID().equals("Default")){
                return null;
            }
        }
        // get logistics category uris
        Map<String,Map<String,String>> logisticsCategories =  SpringBridge.getInstance().getIndexCategoryService().getLogisticsRelatedServices("All");
        List<String> logisticCategoryUris = new ArrayList<>();

        logisticsCategories.forEach((taxonomyId, logisticServiceMap) -> logisticServiceMap.forEach((logisticService, categoryUri) -> logisticCategoryUris.add(categoryUri)));

        CommodityClassificationType commodityClassificationType = new CommodityClassificationType();

        // check whether the catalogue line belongs to a logistics service or not
        boolean isLogisticsService = false;
        for(CommodityClassificationType classificationType: catalogueLine.getGoodsItem().getItem().getCommodityClassification()){
            if(logisticCategoryUris.contains(classificationType.getItemClassificationCode().getURI())){
                isLogisticsService = true;
                break;
            }
        }

        // add default category to catalogue line
        // logistics service
        if(isLogisticsService){
            if(catalogueLine.getGoodsItem().getItem().getTransportationServiceDetails() != null){
                CodeType codeType = new CodeType();
                codeType.setListID("Default");
                codeType.setName("Transport Service");
                codeType.setValue("Transport Service");
                codeType.setURI("nimble:category:TransportService");
                commodityClassificationType.setItemClassificationCode(codeType);
            }
            else{
                CodeType codeType = new CodeType();
                codeType.setListID("Default");
                codeType.setName("Logistics Service");
                codeType.setValue("Logistics Service");
                codeType.setURI("nimble:category:LogisticsService");
                commodityClassificationType.setItemClassificationCode(codeType);
            }
        }
        // product
        else{
            CodeType codeType = new CodeType();
            codeType.setListID("Default");
            codeType.setName("Product");
            codeType.setValue("Product");
            commodityClassificationType.setItemClassificationCode(codeType);
        }

        return commodityClassificationType;
    }

    // this method returns uris of categories ( and their parents) which are included in the given catalogue line
    public static List<String> getCategoryUris(CatalogueLineType catalogueLine) throws InvalidCategoryException {
        List<String> uris = new ArrayList<>();
        // get uri of categories
        for(CommodityClassificationType classificationType: catalogueLine.getGoodsItem().getItem().getCommodityClassification()){
            if(!classificationType.getItemClassificationCode().getListID().contentEquals("Default")){
                uris.add(classificationType.getItemClassificationCode().getURI());
            }
        }

        IndexCategoryService csm = SpringBridge.getInstance().getIndexCategoryService();
        List<CodeType> parentCategories = csm.getParentCategories(
                catalogueLine.getGoodsItem().getItem().getCommodityClassification()
                        .stream()
                        .map(CommodityClassificationType::getItemClassificationCode)
                        .collect(Collectors.toList()));

        // add parent category uris to the uris of existing categories
        parentCategories.forEach(cat -> uris.add(cat.getURI()));
        return uris;
    }

    public static void enhanceDemandWithParentCategories(DemandType demand) {
        IndexCategoryService csm = SpringBridge.getInstance().getIndexCategoryService();
        List<CodeType> parentCategories = csm.getParentCategories(demand.getItemClassificationCode());
        demand.getItemClassificationCode().addAll(parentCategories);
    }
}
