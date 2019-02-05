package eu.nimble.service.catalogue.util;

import eu.nimble.service.catalogue.persistence.util.CatalogueDatabaseAdapter;
import eu.nimble.service.catalogue.category.CategoryServiceManager;
import eu.nimble.service.catalogue.model.category.Category;
import eu.nimble.service.model.ubl.catalogue.CatalogueType;
import eu.nimble.service.model.ubl.commonaggregatecomponents.*;
import eu.nimble.service.model.ubl.commonbasiccomponents.CodeType;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class DataIntegratorUtil {

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
        // check whether we need to add a default category or not
        for (CommodityClassificationType classificationType : catalogueLine.getGoodsItem().getItem().getCommodityClassification()){
            if(classificationType.getItemClassificationCode().getListID().equals("Default")){
                return;
            }
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
    }

    private static List<CommodityClassificationType> getParentCategories(List<CommodityClassificationType> commodityClassifications){
        List<CommodityClassificationType> commodityClassificationTypeList = new ArrayList<>();
        // find parents of the selected categories
        for(CommodityClassificationType cct : commodityClassifications){
            // Default categories have no parents
            if(cct.getItemClassificationCode().getListID().contentEquals("Default")){
                continue;
            }
            CategoryServiceManager csm = CategoryServiceManager.getInstance();
            List<Category> parentCategories = csm.getParentCategories(cct.getItemClassificationCode().getListID(),cct.getItemClassificationCode().getValue());

            for(int i = 0; i< parentCategories.size();i++){
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
}
