package eu.nimble.service.catalogue.sync;

import eu.nimble.service.catalogue.category.taxonomy.TaxonomyEnum;
import eu.nimble.service.catalogue.model.category.Category;
import eu.nimble.service.model.solr.item.ItemType;
import eu.nimble.service.model.solr.owl.ClassType;
import eu.nimble.service.model.ubl.commonaggregatecomponents.*;
import eu.nimble.service.model.ubl.commonbasiccomponents.BinaryObjectType;
import eu.nimble.service.model.ubl.commonbasiccomponents.QuantityType;
import eu.nimble.service.model.ubl.extension.ItemPropertyValueQualifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.util.*;

/**
 * Created by suat on 23-Jan-19.
 */
public class IndexingWrapper {
    private static final Logger logger = LoggerFactory.getLogger(IndexingWrapper.class);

    public static ItemType toIndexItem(CatalogueLineType catalogueLine) {
        ItemType indexItem = new ItemType();

        indexItem.setCatalogueId(catalogueLine.getGoodsItem().getItem().getCatalogueDocumentReference().getID());
        indexItem.setUri(catalogueLine.getHjid().toString());
        indexItem.setName(getMultilingualLabels(catalogueLine.getGoodsItem().getItem().getName()));
        indexItem.setApplicableCountries(getCountries(catalogueLine));
        indexItem.setCertificateType(getCertificates(catalogueLine));
        indexItem.addPrice(catalogueLine.getRequiredItemLocationQuantity().getPrice().getPriceAmount().getCurrencyID(), catalogueLine.getRequiredItemLocationQuantity().getPrice().getPriceAmount().getValue().doubleValue());
        indexItem.addDeliveryTime(catalogueLine.getGoodsItem().getDeliveryTerms().getEstimatedDeliveryPeriod().getDurationMeasure().getUnitCode(), catalogueLine.getGoodsItem().getDeliveryTerms().getEstimatedDeliveryPeriod().getDurationMeasure().getValue().doubleValue());
        indexItem.setDescription(getMultilingualLabels(catalogueLine.getGoodsItem().getItem().getDescription()));
        indexItem.setFreeOfCharge(catalogueLine.isFreeOfChargeIndicator());
        indexItem.setImgageUri(getImageUris(catalogueLine));
        indexItem.setManufacturerId(catalogueLine.getGoodsItem().getItem().getManufacturerParty().getID());
        indexItem.addPackageAmounts(catalogueLine.getGoodsItem().getContainingPackage().getQuantity().getUnitCode(), Arrays.asList(catalogueLine.getGoodsItem().getContainingPackage().getQuantity().getValue().doubleValue()));
        transformCommodityClassifications(indexItem, catalogueLine);
        transformAdditionalItemProperties(indexItem, catalogueLine);

        // transport service related parameters
        indexItem.setEmissionStandard(catalogueLine.getGoodsItem().getItem().getTransportationServiceDetails().getEnvironmentalEmission().get(0).getEnvironmentalEmissionTypeCode().getName());
        indexItem.setServiceType(new HashSet<>(Arrays.asList(catalogueLine.getGoodsItem().getItem().getTransportationServiceDetails().getTransportServiceCode().getName())));
        indexItem.setSupportedCargoType(catalogueLine.getGoodsItem().getItem().getTransportationServiceDetails().getSupportedCommodityClassification().get(0).getCargoTypeCode().getName());
        indexItem.setSupportedProductNature(catalogueLine.getGoodsItem().getItem().getTransportationServiceDetails().getSupportedCommodityClassification().get(0).getNatureCode().getName());

        return indexItem;
    }

    private static void transformAdditionalItemProperties(ItemType indexItem, CatalogueLineType catalogueLine) {
        for(ItemPropertyType itemProperty : catalogueLine.getGoodsItem().getItem().getAdditionalItemProperty()) {
            if(ItemPropertyValueQualifier.valueOfAlternative(itemProperty.getValueQualifier()).equals(ItemPropertyValueQualifier.TEXT)) {
                for(String value : itemProperty.getValue()) {
                    indexItem.addProperty(itemProperty.getName(), value);
                }
            } else if(ItemPropertyValueQualifier.valueOfAlternative(itemProperty.getValueQualifier()).equals(ItemPropertyValueQualifier.NUMBER)) {
                for(BigDecimal value : itemProperty.getValueDecimal()) {
                    indexItem.addProperty(itemProperty.getName(), value.doubleValue());
                }

            } else if(ItemPropertyValueQualifier.valueOfAlternative(itemProperty.getValueQualifier()).equals(ItemPropertyValueQualifier.QUANTITY)) {
                for(QuantityType value : itemProperty.getValueQuantity()) {
                    indexItem.addProperty(itemProperty.getName(), value.getUnitCode(), value.getValue().doubleValue());
                }

            } else if(ItemPropertyValueQualifier.valueOfAlternative(itemProperty.getValueQualifier()).equals(ItemPropertyValueQualifier.BOOLEAN)) {
                logger.warn("{} boolean property is not mapped", itemProperty.getName());

            } else if(ItemPropertyValueQualifier.valueOfAlternative(itemProperty.getValueQualifier()).equals(ItemPropertyValueQualifier.FILE)) {
                // binary properties are not indexed
            }
        }
    }

    private static void transformCommodityClassifications(ItemType indexItem, CatalogueLineType catalogueLine) {
        List<String> classificationUris = new ArrayList<>();
        for(CommodityClassificationType commodityClassification : catalogueLine.getGoodsItem().getItem().getCommodityClassification()) {
            classificationUris.add(commodityClassification.getItemClassificationCode().getURI());
            // no need to fill the classifications list
        }
        indexItem.setClassificationUri(classificationUris);
    }

    private static Map<String, String> getMultilingualLabels(String name) {
        Map<String, String> labels = new HashMap<>();
        labels.put("en", name);
        return labels;
    }

    private static Set<String> getCountries(CatalogueLineType catalogueLine) {
        Set<String> countries = new HashSet<>();
        for(AddressType address : catalogueLine.getRequiredItemLocationQuantity().getApplicableTerritoryAddress()) {
            address.getCountry().getName();
        }
        return countries;
    }

    private static Set<String> getCertificates(CatalogueLineType catalogueLine) {
        Set<String> certificates = new HashSet<>();
        for(CertificateType certificate : catalogueLine.getGoodsItem().getItem().getCertificate()) {
            certificates.add(certificate.getCertificateTypeCode().getName());
        }
        return certificates;
    }

    private static Set<String> getImageUris(CatalogueLineType catalogueLine) {
        Set<String> imagesUris = new HashSet<>();
        for(BinaryObjectType image : catalogueLine.getGoodsItem().getItem().getProductImage()) {
            imagesUris.add(image.getUri());
        }
        return imagesUris;
    }

    public static Category toCategory(ClassType indexCategory) {
        Category category = new Category();
        category.setCategoryUri(indexCategory.getUri());
        // TODO check which field to set as id. For eClass we have been using 0173-1#01-ACI033#011 kind of identifiers
        // but the assumption was to use 8-digit codes
        category.setId(extractIdFromUri(indexCategory.getUri()));
        category.setDefinition(getFirstLabel(indexCategory.getDescription()));
        category.setPreferredName(getFirstLabel(indexCategory.getLabel()));
        category.setLevel(indexCategory.getLevel());
        category.setCode(indexCategory.getLocalName());
        category.setCategoryUri(indexCategory.getUri());
        category.setTaxonomyId(extractTaxonomyFromUri(indexCategory.getUri()).getId());
        return category;
    }

    // TODO update when multilinguality branch is merged
    private static String getFirstLabel(Map<String, String> multilingualLabels) {
        if(multilingualLabels.size() > 0) {
            return multilingualLabels.values().iterator().next();
        }
        return null;
    }

    private static TaxonomyEnum extractTaxonomyFromUri(String uri) {
        if(uri.startsWith(TaxonomyEnum.eClass.getNamespace())) {
            return TaxonomyEnum.eClass;

        } else if (uri.startsWith(TaxonomyEnum.FurnitureOntology.getNamespace())) {
            return TaxonomyEnum.FurnitureOntology;
        }
        return null;
    }

    private static String extractIdFromUri(String uri) {
        if(uri.startsWith(TaxonomyEnum.eClass.getNamespace())) {
            return uri.substring(TaxonomyEnum.eClass.getNamespace().length());

        } else if (uri.startsWith(TaxonomyEnum.FurnitureOntology.getNamespace())) {
            return uri;
        }
        return null;
    }
}
