package eu.nimble.service.catalogue.util;

import eu.nimble.service.catalogue.category.IndexCategoryService;
import eu.nimble.service.catalogue.model.category.Category;
import eu.nimble.service.catalogue.persistence.util.CatalogueDatabaseAdapter;
import eu.nimble.service.model.ubl.catalogue.CatalogueType;
import eu.nimble.service.model.ubl.commonaggregatecomponents.CatalogueLineType;
import eu.nimble.service.model.ubl.commonaggregatecomponents.CommodityClassificationType;
import eu.nimble.service.model.ubl.commonaggregatecomponents.DocumentReferenceType;
import eu.nimble.service.model.ubl.commonaggregatecomponents.PartyType;
import eu.nimble.service.model.ubl.commonbasiccomponents.CodeType;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class DataIntegratorUtil {

    private static String defaultLanguage = "en";

    public static void ensureCatalogueDataIntegrityAndEnhancement(CatalogueType catalogue){
        PartyType partyType = CatalogueDatabaseAdapter.syncPartyInUBLDB(catalogue.getProviderParty());
        catalogue.setProviderParty(partyType);

        for(CatalogueLineType line : catalogue.getCatalogueLine()) {
            ensureCatalogueLineDataIntegrityAndEnhancement(line, catalogue);
        }
    }

    public static void ensureCatalogueLineDataIntegrityAndEnhancement(CatalogueLineType catalogueLine, CatalogueType catalogue){
        catalogueLine.getGoodsItem().getItem().setManufacturerParty(catalogue.getProviderParty());
        setDefaultCategories(catalogueLine);
        setParentCategories(catalogueLine.getGoodsItem().getItem().getCommodityClassification());
        checkCatalogueLineIDs(catalogueLine);
        setCatalogueDocumentReference(catalogue.getUUID(),catalogueLine);
    }

    public static void setParentCategories(List<CommodityClassificationType> commodityClassifications){
        // add parents of the selected category to commodity classifications of the item
        for(CommodityClassificationType cct : getParentCategories(commodityClassifications)){
            commodityClassifications.add(cct);
        }
    }

    private static void setCatalogueDocumentReference(String catalogueUUID,CatalogueLineType catalogueLine){
        // set references from items to the catalogue
        DocumentReferenceType docRef = new DocumentReferenceType();
        docRef.setID(catalogueUUID);
        catalogueLine.getGoodsItem().getItem().setCatalogueDocumentReference(docRef);
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

    private static List<CommodityClassificationType> getParentCategories(List<CommodityClassificationType> commodityClassifications){
        // get uris of the given categories
        List<String> uris = new ArrayList<>();
        for(CommodityClassificationType commodityClassificationType:commodityClassifications){
            if(commodityClassificationType.getItemClassificationCode().getURI() != null){
                uris.add(commodityClassificationType.getItemClassificationCode().getURI());
            }
        }
        List<CommodityClassificationType> commodityClassificationTypeList = new ArrayList<>();
        // find parents of the selected categories
        for(CommodityClassificationType cct : commodityClassifications){
            // Default categories have no parents
            if(cct.getItemClassificationCode().getListID().contentEquals("Default")){
                continue;
            }
            IndexCategoryService csm = SpringBridge.getInstance().getIndexCategoryService();
            List<Category> parentCategories = csm.getParentCategories(cct.getItemClassificationCode().getListID(),cct.getItemClassificationCode().getValue());

            for(int i = 0; i< parentCategories.size();i++){
                Category category = parentCategories.get(i);
                CommodityClassificationType commodityClassificationType = new CommodityClassificationType();
                CodeType codeType = new CodeType();
                codeType.setValue(category.getId());
                codeType.setName(category.getPreferredName(defaultLanguage));
                codeType.setListID(category.getTaxonomyId());
                codeType.setURI(category.getCategoryUri());
                commodityClassificationType.setItemClassificationCode(codeType);
                // check whether it is one of the given categories or it is already added to the list
                if(!commodityClassificationTypeList.contains(commodityClassificationType) && !uris.contains(commodityClassificationType.getItemClassificationCode().getURI())){
                    commodityClassificationTypeList.add(commodityClassificationType);
                }
            }
        }
        return commodityClassificationTypeList;
    }

    public static List<CommodityClassificationType> getLeafCategories(List<CommodityClassificationType> commodityClassifications){
        // get uris of the given categories
        List<String> categoryUris = new ArrayList<>();
        for(CommodityClassificationType commodityClassificationType:commodityClassifications){
            if(commodityClassificationType.getItemClassificationCode().getURI() != null){
                categoryUris.add(commodityClassificationType.getItemClassificationCode().getURI());
            }
        }

        for(CommodityClassificationType commodityClassificationType:commodityClassifications){
            if (commodityClassificationType.getItemClassificationCode().getURI() != null) {
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

        CommodityClassificationType commodityClassificationType = new CommodityClassificationType();
        // Transport Service
        if(catalogueLine.getGoodsItem().getItem().getTransportationServiceDetails() != null){
            CodeType codeType = new CodeType();
            codeType.setListID("Default");
            codeType.setName("Transport Service");
            codeType.setValue("Transport Service");
            commodityClassificationType.setItemClassificationCode(codeType);
        }
        // Product
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
    public static List<String> getCategoryUris(CatalogueLineType catalogueLine){
        List<String> uris = new ArrayList<>();
        // get uri of categories
        for(CommodityClassificationType classificationType: catalogueLine.getGoodsItem().getItem().getCommodityClassification()){
            if(!classificationType.getItemClassificationCode().getListID().contentEquals("Default")){
                uris.add(classificationType.getItemClassificationCode().getURI());
            }
        }
        List<CommodityClassificationType> parentCategories = getParentCategories(catalogueLine.getGoodsItem().getItem().getCommodityClassification());
        // get uri of parent categories
        for(CommodityClassificationType classificationType:parentCategories){
            uris.add(classificationType.getItemClassificationCode().getURI());
        }
        return uris;
    }
}
