package eu.nimble.service.catalogue.index;

import eu.nimble.service.catalogue.category.TaxonomyEnum;
import eu.nimble.service.catalogue.model.category.Category;
import eu.nimble.service.catalogue.model.category.Property;
import eu.nimble.service.catalogue.template.TemplateConfig;
import eu.nimble.service.model.solr.item.ItemType;
import eu.nimble.service.model.solr.owl.ClassType;
import eu.nimble.service.model.solr.owl.PropertyType;
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

    private static final String XSD_NS = "http://www.w3.org/2001/XMLSchema#";

    public static ItemType toIndexItem(CatalogueLineType catalogueLine) {
        ItemType indexItem = new ItemType();

        indexItem.setCatalogueId(catalogueLine.getGoodsItem().getItem().getCatalogueDocumentReference().getID());
        indexItem.setUri(catalogueLine.getHjid().toString());
        indexItem.setLocalName(catalogueLine.getHjid().toString());
        indexItem.setLabel(getMultilingualLabels(catalogueLine.getGoodsItem().getItem().getName()));
        indexItem.setApplicableCountries(getCountries(catalogueLine));
        indexItem.setCertificateType(getCertificates(catalogueLine));
        if(catalogueLine.getRequiredItemLocationQuantity().getPrice().getPriceAmount().getValue() != null) {
            indexItem.addPrice(catalogueLine.getRequiredItemLocationQuantity().getPrice().getPriceAmount().getCurrencyID(), catalogueLine.getRequiredItemLocationQuantity().getPrice().getPriceAmount().getValue().doubleValue());
        }
        if(catalogueLine.getGoodsItem().getDeliveryTerms().getEstimatedDeliveryPeriod().getDurationMeasure().getValue() != null) {
            indexItem.addDeliveryTime(catalogueLine.getGoodsItem().getDeliveryTerms().getEstimatedDeliveryPeriod().getDurationMeasure().getUnitCode(), catalogueLine.getGoodsItem().getDeliveryTerms().getEstimatedDeliveryPeriod().getDurationMeasure().getValue().doubleValue());
        }
        indexItem.setDescription(getMultilingualLabels(catalogueLine.getGoodsItem().getItem().getDescription()));
        indexItem.setFreeOfCharge(catalogueLine.isFreeOfChargeIndicator());
        indexItem.setImgageUri(getImageUris(catalogueLine));
        indexItem.setManufacturerId(catalogueLine.getGoodsItem().getItem().getManufacturerParty().getID());
        if(catalogueLine.getGoodsItem().getContainingPackage().getQuantity().getValue() != null) {
            indexItem.addPackageAmounts(catalogueLine.getGoodsItem().getContainingPackage().getQuantity().getUnitCode(), Arrays.asList(catalogueLine.getGoodsItem().getContainingPackage().getQuantity().getValue().doubleValue()));
        }
        transformCommodityClassifications(indexItem, catalogueLine);
        transformAdditionalItemProperties(indexItem, catalogueLine);

        // transport service related parameters
        if(catalogueLine.getGoodsItem().getItem().getTransportationServiceDetails() != null) {
            indexItem.setEmissionStandard(catalogueLine.getGoodsItem().getItem().getTransportationServiceDetails().getEnvironmentalEmission().get(0).getEnvironmentalEmissionTypeCode().getName());
            indexItem.setServiceType(new HashSet<>(Arrays.asList(catalogueLine.getGoodsItem().getItem().getTransportationServiceDetails().getTransportServiceCode().getName())));
            indexItem.setSupportedCargoType(catalogueLine.getGoodsItem().getItem().getTransportationServiceDetails().getSupportedCommodityClassification().get(0).getCargoTypeCode().getName());
            indexItem.setSupportedProductNature(catalogueLine.getGoodsItem().getItem().getTransportationServiceDetails().getSupportedCommodityClassification().get(0).getNatureCode().getName());
        }

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
                indexItem.setProperty(itemProperty.getName(), Boolean.valueOf(itemProperty.getValue().get(0)));

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

    // TODO update while switching to multilinguality branch
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
            certificates.add(certificate.getCertificateType());
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

    public static List<Category> toCategories(List<ClassType> indexCategories) {
        List<Category> categories = new ArrayList<>();
        for(ClassType indexCategory : indexCategories) {
            categories.add(toCategory(indexCategory));
        }
        return categories;
    }

    public static Category toCategory(ClassType indexCategory) {
        Category category = new Category();
        if(indexCategory.getUri().startsWith(TaxonomyEnum.eClass.getNamespace())) {
            category.setId(indexCategory.getLocalName());
            category.setCode(indexCategory.getCode());
        } else {
            category.setId(indexCategory.getUri());
            category.setCode(indexCategory.getLocalName());
        }
        category.setDefinition(getFirstLabel(indexCategory.getDescription()));
        category.setPreferredName(getFirstLabel(indexCategory.getLabel()));
        if(indexCategory.getLevel() != null) {
            category.setLevel(indexCategory.getLevel());
        }
        category.setCategoryUri(indexCategory.getUri());
        category.setTaxonomyId(extractTaxonomyFromUri(indexCategory.getUri()).getId());
        return category;
    }

    public static List<Property> toProperties(List<PropertyType> indexProperties) {
        List<Property> properties = new ArrayList<>();
        for(PropertyType indexProperty : indexProperties) {
            properties.add(toProperty(indexProperty));
        }
        return properties;
    }

    public static Property toProperty(PropertyType indexProperty) {
        Property property = new Property();
        property.setPreferredName(getFirstLabel(indexProperty.getLabel()));
        property.setDefinition(getFirstLabel(indexProperty.getDescription()));
        if(indexProperty.getUri().startsWith(TaxonomyEnum.eClass.getNamespace())) {
            property.setDataType(indexProperty.getValueQualifier());
        } else {
            property.setDataType(getNormalizedDatatype(getRemainder(indexProperty.getRange(), XSD_NS)).toUpperCase());
        }
        property.setUri(indexProperty.getUri());
        // units are not supported by the new indexing mechanism
        return property;
    }

    // TODO update when multilinguality branch is merged
    private static String getFirstLabel(Map<String, String> multilingualLabels) {
        if(multilingualLabels != null && multilingualLabels.size() > 0) {
            return multilingualLabels.values().iterator().next();
        }
        return null;
    }

    public static TaxonomyEnum extractTaxonomyFromUri(String uri) {
        if(uri.startsWith(TaxonomyEnum.eClass.getNamespace())) {
            return TaxonomyEnum.eClass;

        } else if (uri.startsWith(TaxonomyEnum.FurnitureOntology.getNamespace())) {
            return TaxonomyEnum.FurnitureOntology;
        }
        return null;
    }

    public static String getNormalizedDatatype(String dataType) {
        String normalizedType;
        if (dataType.compareToIgnoreCase(TemplateConfig.TEMPLATE_DATA_TYPE_INT) == 0 ||
                dataType.compareToIgnoreCase(TemplateConfig.TEMPLATE_DATA_TYPE_FLOAT) == 0 ||
                dataType.compareToIgnoreCase(TemplateConfig.TEMPLATE_DATA_TYPE_DOUBLE) == 0) {
            normalizedType = TemplateConfig.TEMPLATE_DATA_TYPE_REAL_MEASURE;

        } else if (dataType.compareToIgnoreCase(TemplateConfig.TEMPLATE_DATA_TYPE_STRING) == 0) {
            normalizedType = TemplateConfig.TEMPLATE_DATA_TYPE_STRING;

        } else if (dataType.compareToIgnoreCase(TemplateConfig.TEMPLATE_DATA_TYPE_BOOLEAN) == 0) {
            normalizedType = TemplateConfig.TEMPLATE_DATA_TYPE_BOOLEAN;

        } else {
            logger.warn("Unknown data type encountered: {}", dataType);
            normalizedType = TemplateConfig.TEMPLATE_DATA_TYPE_STRING;
        }
        return normalizedType;
    }

    private static String getRemainder(String value, String prefix) {
        if(value.startsWith(prefix)){
            return value.substring(prefix.length());
        }
        return value;
    }

    /**
     * Catalogue service model to index model transformers
     */


    public static ClassType toIndexCategory(Category category, Set<String> directParentUris, Set<String> allParentUris, Set<String> directChildrenUris, Set<String> allChildrenUris) {
        ClassType indexCategory = new ClassType();
        indexCategory.setUri(category.getCategoryUri());
        indexCategory.setCode(category.getCode());
        if(category.getCategoryUri().contains(TaxonomyEnum.eClass.getId().toLowerCase())) {
            indexCategory.setLocalName(category.getId());
            // TODO update on switching to multilinguality
            indexCategory.addLabel("en", category.getPreferredName());
            indexCategory.setLevel(category.getLevel());
            indexCategory.setNameSpace(TaxonomyEnum.eClass.getNamespace());
        } else if(category.getCategoryUri().startsWith(TaxonomyEnum.FurnitureOntology.getNamespace())) {
            indexCategory.setLocalName(getRemainder(category.getCategoryUri(), TaxonomyEnum.FurnitureOntology.getNamespace()));
            // TODO update on switching to multilinguality
            indexCategory.addLabel("en", category.getPreferredName());
            indexCategory.setNameSpace(TaxonomyEnum.FurnitureOntology.getNamespace());
        }
        indexCategory.setParents(directParentUris);
        indexCategory.setAllParents(allParentUris);
        indexCategory.setChildren(directChildrenUris);
        indexCategory.setAllChildren(allChildrenUris);

        // TODO update on switching to multilinguality
        indexCategory.addDescription("en", category.getDefinition());
        indexCategory.setLanguages(indexCategory.getLabel().keySet());
        if(category.getProperties() != null) {
            indexCategory.setProperties(new HashSet<>());
            category.getProperties().stream().forEach(property -> indexCategory.getProperties().add(property.getUri()));
        }
        return indexCategory;
    }

    public static PropertyType toIndexProperty(Property property, Set<String> associatedCategoryUris) {
        PropertyType indexProperty = new PropertyType();
        indexProperty.setUri(property.getUri());
        indexProperty.addDescription("en", property.getDefinition());
        if(property.getUri().contains(TaxonomyEnum.eClass.getId().toLowerCase())) {
            indexProperty.setLocalName(property.getId());
            // TODO update on switching to multilinguality
            indexProperty.addLabel("en", property.getPreferredName());
            indexProperty.setNameSpace(TaxonomyEnum.eClass.getNamespace());
        } else if(property.getUri().startsWith(TaxonomyEnum.FurnitureOntology.getNamespace())) {
            indexProperty.setLocalName(getRemainder(indexProperty.getUri(), TaxonomyEnum.FurnitureOntology.getNamespace()));
            // TODO update on switching to multilinguality
            indexProperty.addLabel("en", property.getPreferredName());
            indexProperty.setNameSpace(TaxonomyEnum.FurnitureOntology.getNamespace());
        }
        if(property.getUnit() != null) {
            indexProperty.setValueQualifier(TemplateConfig.TEMPLATE_DATA_TYPE_QUANTITY);

        } else if(ItemPropertyValueQualifier.TEXT.getAlternatives().stream().anyMatch(property.getDataType()::equalsIgnoreCase)) {
            indexProperty.setValueQualifier(TemplateConfig.TEMPLATE_DATA_TYPE_STRING);
            indexProperty.setRange("http://www.w3.org/2001/XMLSchema#string");

        } else if(ItemPropertyValueQualifier.NUMBER.getAlternatives().stream().anyMatch(property.getDataType()::equalsIgnoreCase)) {
            indexProperty.setValueQualifier(TemplateConfig.TEMPLATE_DATA_TYPE_NUMBER);
            indexProperty.setRange("http://www.w3.org/2001/XMLSchema#float");

        } else if(ItemPropertyValueQualifier.BOOLEAN.getAlternatives().stream().anyMatch(property.getDataType()::equalsIgnoreCase)) {
            indexProperty.setValueQualifier(TemplateConfig.TEMPLATE_DATA_TYPE_BOOLEAN);
            indexProperty.setRange("http://www.w3.org/2001/XMLSchema#boolean");
        }
        indexProperty.setPropertyType(property.getDataType());
        indexProperty.setProduct(associatedCategoryUris);
        indexProperty.setItemFieldNames(Arrays.asList(ItemType.dynamicFieldPart(property.getPreferredName())));
        indexProperty.setLanguages(indexProperty.getLabel().keySet());
        return indexProperty;
    }
}
