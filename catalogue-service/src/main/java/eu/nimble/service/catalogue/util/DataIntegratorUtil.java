package eu.nimble.service.catalogue.util;

import eu.nimble.service.catalogue.CatalogueDatabaseAdapter;
import eu.nimble.service.model.ubl.catalogue.CatalogueType;
import eu.nimble.service.model.ubl.commonaggregatecomponents.CatalogueLineType;
import eu.nimble.service.model.ubl.commonaggregatecomponents.CommodityClassificationType;
import eu.nimble.service.model.ubl.commonaggregatecomponents.DocumentReferenceType;
import eu.nimble.service.model.ubl.commonaggregatecomponents.PartyType;
import eu.nimble.service.model.ubl.commonbasiccomponents.CodeType;

public class DataIntegratorUtil {

    public static void setCatalogueDocumentReference(String catalogueUUID,CatalogueLineType catalogueLine){
        // set references from items to the catalogue
        DocumentReferenceType docRef = new DocumentReferenceType();
        docRef.setID(catalogueUUID);
        catalogueLine.getGoodsItem().getItem().setCatalogueDocumentReference(docRef);
    }

    public static void setDefaultCategories(CatalogueLineType catalogueLine){
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

    public static void checkExistingParties(CatalogueType catalogue){
        PartyType partyType = CatalogueDatabaseAdapter.getParty(catalogue.getProviderParty());
        catalogue.setProviderParty(partyType);
        for (CatalogueLineType catalogueLineType : catalogue.getCatalogueLine()){
            catalogueLineType.getGoodsItem().getItem().setManufacturerParty(partyType);
        }
    }

}
