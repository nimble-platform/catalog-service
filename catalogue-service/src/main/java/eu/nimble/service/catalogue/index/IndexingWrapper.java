package eu.nimble.service.catalogue.index;

import eu.nimble.service.catalogue.category.TaxonomyEnum;
import eu.nimble.service.catalogue.model.category.Category;
import eu.nimble.service.catalogue.model.category.Property;
import eu.nimble.service.catalogue.template.TemplateConfig;
import eu.nimble.service.catalogue.validation.AmountValidator;
import eu.nimble.service.catalogue.validation.QuantityValidator;
import eu.nimble.service.model.solr.item.ItemType;
import eu.nimble.service.model.solr.owl.ClassType;
import eu.nimble.service.model.solr.owl.PropertyType;
import eu.nimble.service.model.solr.owl.ValueQualifier;
import eu.nimble.service.model.ubl.commonaggregatecomponents.*;
import eu.nimble.service.model.ubl.commonbasiccomponents.BinaryObjectType;
import eu.nimble.service.model.ubl.commonbasiccomponents.QuantityType;
import eu.nimble.service.model.ubl.commonbasiccomponents.TextType;
import eu.nimble.service.model.ubl.extension.ItemPropertyValueQualifier;
import eu.nimble.service.model.ubl.extension.QualityIndicatorParameter;
import eu.nimble.utility.JsonSerializationUtility;
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

    private static final List<String> languagePriorityForCustomProperties = Arrays.asList("en", "es", "de", "tr", "it");

    public static ItemType toIndexItem(CatalogueLineType catalogueLine) {
        ItemType indexItem = new ItemType();

        indexItem.setCatalogueId(catalogueLine.getGoodsItem().getItem().getCatalogueDocumentReference().getID());
        indexItem.setUri(catalogueLine.getHjid().toString());
        indexItem.setManufactuerItemId(catalogueLine.getID());
        indexItem.setLocalName(catalogueLine.getHjid().toString());
        catalogueLine.getGoodsItem().getItem().getName().forEach(name -> indexItem.addLabel(name.getLanguageID(), name.getValue()));
        indexItem.setApplicableCountries(getCountries(catalogueLine));
        indexItem.setCertificateType(getCertificates(catalogueLine));
        AmountValidator amountValidator = new AmountValidator(catalogueLine.getRequiredItemLocationQuantity().getPrice().getPriceAmount());
        if(amountValidator.bothFieldsPopulated()) {
            indexItem.addPrice(catalogueLine.getRequiredItemLocationQuantity().getPrice().getPriceAmount().getCurrencyID(), catalogueLine.getRequiredItemLocationQuantity().getPrice().getPriceAmount().getValue().doubleValue());
        }
        QuantityValidator quantityValidator = new QuantityValidator(catalogueLine.getGoodsItem().getDeliveryTerms().getEstimatedDeliveryPeriod().getDurationMeasure());
        if(quantityValidator.bothFieldsPopulated()) {
            indexItem.addDeliveryTime(catalogueLine.getGoodsItem().getDeliveryTerms().getEstimatedDeliveryPeriod().getDurationMeasure().getUnitCode(), catalogueLine.getGoodsItem().getDeliveryTerms().getEstimatedDeliveryPeriod().getDurationMeasure().getValue().doubleValue());
        }
        indexItem.setDescription(getLabelMapFromMultilingualLabels(catalogueLine.getGoodsItem().getItem().getDescription()));
        indexItem.setFreeOfCharge(catalogueLine.isFreeOfChargeIndicator());
        indexItem.setImgageUri(getImageUris(catalogueLine));
        indexItem.setManufacturerId(catalogueLine.getGoodsItem().getItem().getManufacturerParty().getPartyIdentification().get(0).getID());
        quantityValidator = new QuantityValidator(catalogueLine.getGoodsItem().getContainingPackage().getQuantity());
        if(quantityValidator.bothFieldsPopulated()) {
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
            String propertyQualifier = getIndexPropertyQualifier(itemProperty);
            if(propertyQualifier == null) {
                String serializedItemProperty = JsonSerializationUtility.serializeEntitySilently(itemProperty);
                logger.warn("Null qualifier for property: {}. This property won't be indexed", serializedItemProperty);
                continue;
            }
            boolean isStandardProperty = isStandardProperty(itemProperty);

            if(isStandardProperty) {
                if (ItemPropertyValueQualifier.valueOfAlternative(itemProperty.getValueQualifier()).equals(ItemPropertyValueQualifier.TEXT)) {
                    for (TextType value : itemProperty.getValue()) {
                        indexItem.addProperty(propertyQualifier, value.getValue() + "@" + value.getLanguageID());
                    }
                } else if (ItemPropertyValueQualifier.valueOfAlternative(itemProperty.getValueQualifier()).equals(ItemPropertyValueQualifier.NUMBER)) {
                    for (BigDecimal value : itemProperty.getValueDecimal()) {
                        indexItem.addProperty(propertyQualifier, value.doubleValue());
                    }

                } else if (ItemPropertyValueQualifier.valueOfAlternative(itemProperty.getValueQualifier()).equals(ItemPropertyValueQualifier.QUANTITY)) {
                    for (QuantityType value : itemProperty.getValueQuantity()) {
                        if(value.getUnitCode() != null && value.getValue() != null) {
                            indexItem.addProperty(propertyQualifier, value.getUnitCode(), value.getValue().doubleValue());
                        }
                    }

                } else if (ItemPropertyValueQualifier.valueOfAlternative(itemProperty.getValueQualifier()).equals(ItemPropertyValueQualifier.BOOLEAN)) {
                    if(itemProperty.getValue().size() > 0) {
                        indexItem.setProperty(propertyQualifier, Boolean.valueOf(itemProperty.getValue().get(0).getValue()));
                    }

                } else if (ItemPropertyValueQualifier.valueOfAlternative(itemProperty.getValueQualifier()).equals(ItemPropertyValueQualifier.FILE)) {
                    // binary properties are not indexed
                }

                // add custom properties to the item
            } else {
                PropertyType customPropertyData = createPropertyMetadataForCustomProperty(itemProperty);

                if (ItemPropertyValueQualifier.valueOfAlternative(itemProperty.getValueQualifier()).equals(ItemPropertyValueQualifier.TEXT)) {
                    for (TextType value : itemProperty.getValue()) {
                        indexItem.addProperty(propertyQualifier, value.getValue() + "@" + value.getLanguageID(), customPropertyData);
                    }
                } else if (ItemPropertyValueQualifier.valueOfAlternative(itemProperty.getValueQualifier()).equals(ItemPropertyValueQualifier.NUMBER)) {
                    for (BigDecimal value : itemProperty.getValueDecimal()) {
                        indexItem.addProperty(propertyQualifier, value.doubleValue(), customPropertyData);
                    }

                } else if (ItemPropertyValueQualifier.valueOfAlternative(itemProperty.getValueQualifier()).equals(ItemPropertyValueQualifier.QUANTITY)) {
                    for (QuantityType value : itemProperty.getValueQuantity()) {
                        if(value.getUnitCode() != null && value.getValue() != null) {
                            indexItem.addProperty(propertyQualifier, value.getUnitCode(), value.getValue().doubleValue(), customPropertyData);
                        }
                    }

                } else if (ItemPropertyValueQualifier.valueOfAlternative(itemProperty.getValueQualifier()).equals(ItemPropertyValueQualifier.BOOLEAN)) {
                    if(itemProperty.getValue().size() > 0) {
                        indexItem.setProperty(propertyQualifier, Boolean.valueOf(itemProperty.getValue().get(0).getValue()), customPropertyData);
                    }

                } else if (ItemPropertyValueQualifier.valueOfAlternative(itemProperty.getValueQualifier()).equals(ItemPropertyValueQualifier.FILE)) {
                    // binary properties are not indexed
                }
            }
        }
    }

    private static String getIndexPropertyQualifier(ItemPropertyType itemProperty) {
        boolean isStandardProperty = isStandardProperty(itemProperty);
        if (isStandardProperty) {
            return itemProperty.getURI();
        } else {
            for(String langId : languagePriorityForCustomProperties) {
                for(TextType name : itemProperty.getName()) {
                    if(name.getLanguageID().contentEquals(langId)) {
                        return name.getValue();
                    }
                }
            }
            String itemPropertySerialization = JsonSerializationUtility.serializeEntitySilently(itemProperty);
            String msg = String.format("No valid name in the ItemProperty: %s", itemPropertySerialization);
            logger.warn(msg);
            throw new RuntimeException(msg);
        }
    }

    private static boolean isStandardProperty(ItemPropertyType itemProperty) {
        boolean isStandardProperty = Arrays.asList(TaxonomyEnum.values()).stream()
                .filter(taxonomy -> itemProperty.getItemClassificationCode().getListID() != null && taxonomy.getId().contentEquals(itemProperty.getItemClassificationCode().getListID()))
                .findFirst()
                .isPresent();
        return isStandardProperty;
    }

    private static PropertyType createPropertyMetadataForCustomProperty(ItemPropertyType itemProperty) {
        PropertyType property = new PropertyType();
        itemProperty.getName().stream().forEach(label -> property.addLabel(label.getLanguageID(), label.getValue()));
        return property;
    }

    private static void transformCommodityClassifications(ItemType indexItem, CatalogueLineType catalogueLine) {
        List<String> classificationUris = new ArrayList<>();
        for(CommodityClassificationType commodityClassification : catalogueLine.getGoodsItem().getItem().getCommodityClassification()) {
            classificationUris.add(commodityClassification.getItemClassificationCode().getURI());
            // no need to fill the classifications list
        }
        indexItem.setClassificationUri(classificationUris);
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
        // this distinction is made in order not to break the functionality in the UI. Codes of eClass concetps are
        // being used in the UI
        if(indexCategory.getUri().startsWith(TaxonomyEnum.eClass.getNamespace())) {
            category.setCode(indexCategory.getCode());
        } else {
            category.setCode(indexCategory.getLocalName());
        }
        category.setId(indexCategory.getUri());
        category.setDefinition(getLabelListFromMap(indexCategory.getDescription()));
        category.setPreferredName(getLabelListFromMap(indexCategory.getLabel()));
        category.setRemark(getSingleLabel(indexCategory.getComment()));
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
        property.setPreferredName(getLabelListFromMap(indexProperty.getLabel()));
        property.setDefinition(getSingleLabel(indexProperty.getDescription()));
        property.setRemark(getLabelListFromMap(indexProperty.getComment()));

        // TODO the data type is currently keeping the value qualifier. However, it should be stored in the value qualifier field.
        if(indexProperty.getValueQualifier() != null) {
            // This is so in order not to break the functionality on the front-end
            property.setDataType(indexProperty.getValueQualifier().toString());
        } else {
            property.setDataType(getValueQualifierForRange(indexProperty.getRange()));
        }

        // TODO text qualifier is converted to string in order not to break the UI functionality
        if(property.getDataType().contentEquals(ItemPropertyValueQualifier.TEXT.toString())) {
            property.setDataType(TemplateConfig.TEMPLATE_DATA_TYPE_STRING);
        }
        property.setUri(indexProperty.getUri());
        property.setId(indexProperty.getUri());
        // units are not supported by the new indexing mechanism
        return property;
    }

    private static String getSingleLabel(Map<String, String> multilingualLabels) {
        if(multilingualLabels != null && multilingualLabels.size() > 0) {
            return multilingualLabels.values().iterator().next();
        }
        return null;
    }

    private static Map<String, String> getLabelMapFromMultilingualLabels(List<TextType> labelList) {
        Map<String, String> labels = new HashMap<>();
        labelList.stream().forEach(label -> labels.put(label.getLanguageID(), label.getValue()));
        return labels;
    }

    private static List<TextType> getLabelListFromMap(Map<String, String> multilingualLabels) {
        List<TextType> labelList = new ArrayList<>();
        if(multilingualLabels != null) {
            multilingualLabels.keySet().forEach(language -> {
                TextType label = new TextType();
                label.setLanguageID(language);
                label.setValue(multilingualLabels.get(language));
                labelList.add(label);

            });
        }
        return labelList;
    }

    public static TaxonomyEnum extractTaxonomyFromUri(String uri) {
        for(TaxonomyEnum taxonomy : TaxonomyEnum.values()) {
            if(uri.startsWith(taxonomy.getNamespace())) {
                return taxonomy;
            }
        }
        return null;
    }

    public static String getValueQualifierForRange(String range) {
        try {
            return ItemPropertyValueQualifier.getQualifier(range).toString();
        } catch (RuntimeException e) {
            logger.warn("No qualifier for range: {}", range);
            return ItemPropertyValueQualifier.TEXT.toString();
        }
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
        TaxonomyEnum taxonomy = extractTaxonomyFromUri(category.getCategoryUri());
        indexCategory.setUri(category.getCategoryUri());
        indexCategory.setCode(category.getCode());


        if(taxonomy.equals(TaxonomyEnum.eClass)) {
            indexCategory.setLocalName(category.getId());
            category.getPreferredName().stream().forEach(label -> indexCategory.addLabel(label.getLanguageID(), label.getValue()));
            indexCategory.setLevel(category.getLevel());
            indexCategory.setNameSpace(TaxonomyEnum.eClass.getNamespace());
            // TODO below by default the english language is assumed
            indexCategory.addComment("en", category.getRemark());

        } else {
            indexCategory.setLocalName(getRemainder(category.getCategoryUri(), taxonomy.getNamespace()));
            category.getPreferredName().forEach(label -> indexCategory.addLabel(label.getLanguageID(), label.getValue()));
            indexCategory.setNameSpace(taxonomy.getNamespace());
        }
        indexCategory.setParents(directParentUris);
        indexCategory.setAllParents(allParentUris);
        indexCategory.setChildren(directChildrenUris);
        indexCategory.setAllChildren(allChildrenUris);

        category.getDefinition().forEach(definition -> indexCategory.addDescription(definition.getLanguageID(), definition.getValue()));
        indexCategory.setLanguages(indexCategory.getLabel().keySet());
        if(category.getProperties() != null) {
            indexCategory.setProperties(new HashSet<>());
            category.getProperties().stream().forEach(property -> indexCategory.getProperties().add(property.getUri()));
        }
        return indexCategory;
    }

    public static PropertyType toIndexProperty(Property property, Set<String> associatedCategoryUris) {
        PropertyType indexProperty = new PropertyType();
        TaxonomyEnum taxonomy = extractTaxonomyFromUri(property.getUri());
        indexProperty.setUri(property.getUri());
        // TODO below by default the english language is assumed
        indexProperty.addDescription("en", property.getDefinition());
        property.getPreferredName().forEach(label -> indexProperty.addLabel(label.getLanguageID(), label.getValue()));
        if(taxonomy.equals(TaxonomyEnum.eClass)) {
            indexProperty.setLocalName(getRemainder(indexProperty.getUri(), TaxonomyEnum.eClass.getNamespace()));
            indexProperty.setNameSpace(TaxonomyEnum.eClass.getNamespace());
            indexProperty.setItemFieldNames(Arrays.asList(ItemType.dynamicFieldPart(property.getUri())));
            property.getRemark().forEach(label -> indexProperty.addComment(label.getLanguageID(), label.getValue()));

        } else {
            indexProperty.setLocalName(getRemainder(indexProperty.getUri(), taxonomy.getNamespace()));
            indexProperty.setNameSpace(taxonomy.getNamespace());
            indexProperty.setItemFieldNames(Arrays.asList(ItemType.dynamicFieldPart(property.getUri())));
        }

        try {
            indexProperty.setValueQualifier(ValueQualifier.valueOf(ItemPropertyValueQualifier.valueOfAlternative(property.getValueQualifier()).toString()));
        } catch (RuntimeException e) {
            logger.warn("Unsupported value qualifier: {}, reverting to {}", property.getValueQualifier(), ValueQualifier.TEXT.toString());
            indexProperty.setValueQualifier(ValueQualifier.TEXT);
        }

        try {
            indexProperty.setRange(ItemPropertyValueQualifier.getRange(property.getValueQualifier()));
        } catch (RuntimeException e) {
            logger.warn("Unsupported value qualifier: {}, reverting to {}", property.getValueQualifier(), XSD_NS + "string");
            indexProperty.setRange(XSD_NS + "string");
        }

        // all properties are assumed to be a datatype property (including the quantity properties)
        indexProperty.setPropertyType("DatatypeProperty");
        indexProperty.setProduct(associatedCategoryUris);
        indexProperty.setLanguages(indexProperty.getLabel().keySet());
        return indexProperty;
    }

    public static eu.nimble.service.model.solr.party.PartyType toIndexParty(PartyType party) {
        eu.nimble.service.model.solr.party.PartyType indexParty = new eu.nimble.service.model.solr.party.PartyType();
        party.getBrandName().forEach(name -> indexParty.addBrandName(name.getLanguageID(), name.getValue()));
        indexParty.setLegalName(party.getPartyName().get(0).getName().getValue());
        String originLang = party.getPostalAddress().getCountry().getName().getLanguageID() != null ? party.getPostalAddress().getCountry().getName().getLanguageID() : "";
        indexParty.addOrigin(originLang, party.getPostalAddress().getCountry().getName().getValue());
        indexParty.setId(party.getHjid().toString());
        indexParty.setUri(party.getHjid().toString());

        // TODO currently we do not support multilingual certificate types
        party.getCertificate().stream().forEach(certificate -> indexParty.addCertificateType("", certificate.getCertificateTypeCode().getName()));
        if(party.getPpapCompatibilityLevel() != null) {
            indexParty.setPpapComplianceLevel(party.getPpapCompatibilityLevel().intValue());
        }
        // TODO currently we do not support multilingual ppap document types
        party.getPpapDocumentReference().forEach(ppapDocument -> indexParty.addPpapDocumentType("", ppapDocument.getDocumentType()));

        // get trust scores
        party.getQualityIndicator().forEach(qualityIndicator -> {
            if(qualityIndicator.getQualityParameter() != null && qualityIndicator.getQuantity() != null) {
                if(qualityIndicator.getQualityParameter().contentEquals(QualityIndicatorParameter.COMPANY_RATING.toString())) {
                    indexParty.setTrustRating(qualityIndicator.getQuantity().getValue().doubleValue());
                } else if(qualityIndicator.getQualityParameter().contentEquals(QualityIndicatorParameter.TRUST_SCORE.toString())) {
                    indexParty.setTrustScore(qualityIndicator.getQuantity().getValue().doubleValue());
                } else if(qualityIndicator.getQualityParameter().contentEquals(QualityIndicatorParameter.DELIVERY_PACKAGING.toString())) {
                    indexParty.setTrustDeliveryPackaging(qualityIndicator.getQuantity().getValue().doubleValue());
                } else if(qualityIndicator.getQualityParameter().contentEquals(QualityIndicatorParameter.FULFILLMENT_OF_TERMS.toString())) {
                    indexParty.setTrustFullfillmentOfTerms(qualityIndicator.getQuantity().getValue().doubleValue());
                } else if(qualityIndicator.getQualityParameter().contentEquals(QualityIndicatorParameter.SELLER_COMMUNICATION.toString())) {
                    indexParty.setTrustSellerCommunication(qualityIndicator.getQuantity().getValue().doubleValue());
                } else if(qualityIndicator.getQualityParameter().contentEquals(QualityIndicatorParameter.NUMBER_OF_TRANSACTIONS.toString())) {
                    indexParty.setTrustNumberOfTransactions(qualityIndicator.getQuantity().getValue().doubleValue());
                } else if(qualityIndicator.getQualityParameter().contentEquals(QualityIndicatorParameter.TRADING_VOLUME.toString())) {
                    indexParty.setTrustTradingVolume(qualityIndicator.getQuantity().getValue().doubleValue());
                } else if(qualityIndicator.getQualityParameter().contentEquals(QualityIndicatorParameter.Number_OF_EVALUATIONS.toString())) {
                    indexParty.setTrustNumberOfEvaluations(qualityIndicator.getQuantity().getValue().doubleValue());
                }
            }
        });
        return indexParty;
    }
}
