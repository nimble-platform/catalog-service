package eu.nimble.service.catalogue.validation;

import com.google.common.base.Strings;
import eu.nimble.service.catalogue.category.IndexCategoryService;
import eu.nimble.service.catalogue.exception.NimbleExceptionMessageCode;
import eu.nimble.service.catalogue.model.category.Category;
import eu.nimble.service.catalogue.util.SpringBridge;
import eu.nimble.service.model.ubl.catalogue.CatalogueType;
import eu.nimble.service.model.ubl.commonaggregatecomponents.CatalogueLineType;
import eu.nimble.service.model.ubl.commonaggregatecomponents.CommodityClassificationType;
import eu.nimble.service.model.ubl.commonaggregatecomponents.ItemPropertyType;
import eu.nimble.service.model.ubl.commonaggregatecomponents.ItemType;
import eu.nimble.service.model.ubl.commonbasiccomponents.BinaryObjectType;
import eu.nimble.service.model.ubl.commonbasiccomponents.TextType;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Created by suat on 08-Aug-18.
 */
public class CatalogueLineValidator {

    private List<String> errorMessages;
    private List<List<String>> errorParameters;
    // UUID of the catalogue containing the line. Could be null in cases such as new template-based publishing and new catalogue creation
    private String owningCatalogueUuid;
    private String cataloguePartyId;
    private CatalogueLineType catalogueLine;
    private String extractedLineId;

    public CatalogueLineValidator(String owningCatalogueUuid, String cataloguePartyId, CatalogueLineType catalogueLine) {
        this(owningCatalogueUuid, catalogueLine, new ArrayList<>(), new ArrayList<>());
    }

    public CatalogueLineValidator(String owningCatalogueUuid, CatalogueLineType catalogueLine, List<String> errorMessages, List<List<String>> errorParameters) {
        this.owningCatalogueUuid = owningCatalogueUuid;
        this.errorMessages = errorMessages;
        this.catalogueLine = catalogueLine;
        this.errorParameters = errorParameters;
    }

    public ValidationMessages validateAll() {
        // set the ID to be used during the subsequent validations
        extractedLineId = !Strings.isNullOrEmpty(catalogueLine.getID()) ? catalogueLine.getID() : catalogueLine.getGoodsItem().getItem().getManufacturersItemIdentification().getID();

        idExists();
        idHasInvalidSpace();
        manufacturerIdExists();
        lineIdManufacturerIdMatches();
        checkProductNames();
        checkCommodityClassifications();
        partyIdsMatch();
        fileSizesLessThanTheMaximum();
        checkReferenceToCatalogue();

        return new ValidationMessages(errorMessages,errorParameters);
    }

    public void idExists() {
        if (extractedLineId == null) {
            errorMessages.add(NimbleExceptionMessageCode.BAD_REQUEST_NO_ID_FOR_LINE.toString());
            errorParameters.add(new ArrayList<>());
        }
    }

    public void idHasInvalidSpace() {
        if (extractedLineId != null && extractedLineId.length() != extractedLineId.trim().length()) {
            errorMessages.add(NimbleExceptionMessageCode.BAD_REQUEST_PRECEDING_TRAILING_IN_ID.toString());
            errorParameters.add(Arrays.asList(extractedLineId));
        }
    }
    public void checkReferenceToCatalogue() {
        if(owningCatalogueUuid != null){
            ItemType item = catalogueLine.getGoodsItem().getItem();
            if (!item.getCatalogueDocumentReference().getID().equals(owningCatalogueUuid)) {
                errorMessages.add(NimbleExceptionMessageCode.BAD_REQUEST_INVALID_REFERENCE.toString());
                errorParameters.add(Arrays.asList(extractedLineId));
            }
        }
    }

    public void manufacturerIdExists() {
        ItemType item = catalogueLine.getGoodsItem().getItem();
        if (Strings.isNullOrEmpty(item.getManufacturerParty().getPartyIdentification().get(0).getID())) {
            errorMessages.add(NimbleExceptionMessageCode.BAD_REQUEST_NO_MANUFACTURER_PARTY.toString());
            errorParameters.add(Arrays.asList(extractedLineId));
        }
    }

    public void lineIdManufacturerIdMatches() {
        ItemType item = catalogueLine.getGoodsItem().getItem();
        if (!Strings.isNullOrEmpty(catalogueLine.getID()) && !Strings.isNullOrEmpty(item.getManufacturersItemIdentification().getID())) {
            if (!catalogueLine.getID().contentEquals(item.getManufacturersItemIdentification().getID())) {
                errorMessages.add(NimbleExceptionMessageCode.BAD_REQUEST_IDS_DO_NOT_MATCH.toString());
                errorParameters.add(Arrays.asList(extractedLineId, item.getManufacturersItemIdentification().getID()));
            }
        }
    }

    public void checkProductNames() {
        ItemType item = catalogueLine.getGoodsItem().getItem();

        boolean nameExists = false;
        for (TextType textType:item.getName()){
            if(!Strings.isNullOrEmpty(textType.getValue())){
                nameExists = true;
            }
        }

        if (!nameExists) {
            errorMessages.add(NimbleExceptionMessageCode.BAD_REQUEST_NO_NAME_FOR_LINE.toString());
            errorParameters.add(Arrays.asList(extractedLineId));
        }

        // check whether there is at most one product name for each language id
        Set<String> productNameLanguageIds = new HashSet<>();
        item.getName().forEach(textType -> {
            if(productNameLanguageIds.contains(textType.getLanguageID())){
                errorMessages.add(NimbleExceptionMessageCode.BAD_REQUEST_MULTIPLE_NAME_FOR_SAME_LANGUAGE_ID.toString());
                errorParameters.add(Arrays.asList(textType.getLanguageID(),extractedLineId));
            } else {
                productNameLanguageIds.add(textType.getLanguageID());
            }
        });
    }

    public void checkCommodityClassifications() {
        ItemType item = catalogueLine.getGoodsItem().getItem();
        // check whether the item has at least one category
        if (item.getCommodityClassification().size() == 0) {
            errorMessages.add(NimbleExceptionMessageCode.BAD_REQUEST_NO_COMMODITY_CLASSIFICATION.toString());
            errorParameters.add(Arrays.asList(extractedLineId));
        }
        // check whether all the categories are the same type (i.e, Product or Service categories)
        IndexCategoryService csm = SpringBridge.getInstance().getIndexCategoryService();
        // service root categories
        List<String> serviceRootCategories = SpringBridge.getInstance().getTaxonomyManager().getServiceRootCategories();
        // skip the default categories
        List<CommodityClassificationType> nonDefaultCommodityClassificationTypes = item.getCommodityClassification().stream().filter(commodityClassificationType -> !commodityClassificationType.getItemClassificationCode().getListID().contentEquals("Default")).collect(Collectors.toList());
        // find the service categories included in the item
        List<CommodityClassificationType> serviceCommodityClassifications = new ArrayList<>();
        for (CommodityClassificationType cct : nonDefaultCommodityClassificationTypes) {
            List<Category> parentCategories = csm.getParentCategories(cct.getItemClassificationCode().getListID(),cct.getItemClassificationCode().getValue());
            for (Category parentCategory : parentCategories) {
                if(serviceRootCategories.contains(parentCategory.getCategoryUri())){
                    serviceCommodityClassifications.add(cct);
                    break;
                }
            }
        }
        // error if the types of categories are different
        if(serviceCommodityClassifications.size() != 0 && nonDefaultCommodityClassificationTypes.size() != serviceCommodityClassifications.size()){
            errorMessages.add(NimbleExceptionMessageCode.BAD_REQUEST_MIXED_COMMODITY_CLASSIFICATION.toString());
            errorParameters.add(Arrays.asList(extractedLineId));
        }
    }

    public void partyIdsMatch() {
        ItemType item = catalogueLine.getGoodsItem().getItem();
        String itemManufacturerPartyId = item.getManufacturerParty().getPartyIdentification().get(0).getID();
        if (!this.cataloguePartyId.contentEquals(itemManufacturerPartyId)) {
            errorMessages.add(NimbleExceptionMessageCode.BAD_REQUEST_PARTY_IDS_DO_NOT_MATCH.toString());
            errorParameters.add(Arrays.asList(extractedLineId, this.cataloguePartyId, itemManufacturerPartyId));
        }
    }

    public void fileSizesLessThanTheMaximum() {
        // validate images
        int maxFileSize = SpringBridge.getInstance().getCatalogueServiceConfig().getMaxFileSize() * 1024 * 1024;

        for (BinaryObjectType bo : catalogueLine.getGoodsItem().getItem().getProductImage()) {
            if (bo.getValue().length > maxFileSize) {
                errorMessages.add(NimbleExceptionMessageCode.BAD_REQUEST_LARGER_THAN_ALLOWED_SIZE.toString());
                errorParameters.add(Arrays.asList(bo.getFileName(), Integer.toString(maxFileSize)));
            }
        }

        // validate properties getting binary content
        for (ItemPropertyType itemProperty : catalogueLine.getGoodsItem().getItem().getAdditionalItemProperty()) {
            if (itemProperty.getValueQualifier().contentEquals("FILE")) {
                for (BinaryObjectType bo : itemProperty.getValueBinary()) {
                    if(bo.getValue() == null) {
                        if(!bo.getUri().startsWith(SpringBridge.getInstance().getCatalogueServiceConfig().getBinaryContentUrl())) {
                            errorMessages.add(NimbleExceptionMessageCode.BAD_REQUEST_NO_BINARY_CONTENT_FOR_THE_FILE.toString());
                            errorParameters.add(Arrays.asList(bo.getFileName()));
                        }
                        continue;
                    }
                    if (bo.getValue().length > maxFileSize) {
                        errorMessages.add(NimbleExceptionMessageCode.BAD_REQUEST_LARGER_THAN_ALLOWED_SIZE.toString());
                        errorParameters.add(Arrays.asList(bo.getFileName(), Integer.toString(maxFileSize)));
                    }
                }
            }
        }
    }
}
