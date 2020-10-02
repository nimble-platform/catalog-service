package eu.nimble.service.catalogue.validation;

import com.google.common.base.Strings;
import eu.nimble.service.catalogue.exception.NimbleExceptionMessageCode;
import eu.nimble.service.catalogue.util.SpringBridge;
import eu.nimble.service.model.ubl.catalogue.CatalogueType;
import eu.nimble.service.model.ubl.commonaggregatecomponents.CatalogueLineType;
import eu.nimble.service.model.ubl.commonaggregatecomponents.ItemPropertyType;
import eu.nimble.service.model.ubl.commonaggregatecomponents.ItemType;
import eu.nimble.service.model.ubl.commonbasiccomponents.BinaryObjectType;
import eu.nimble.service.model.ubl.commonbasiccomponents.TextType;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Created by suat on 08-Aug-18.
 */
public class CatalogueLineValidator {

    private List<String> errorMessages;
    private List<List<String>> errorParameters;
    private CatalogueType owningCatalogue;
    private CatalogueLineType catalogueLine;
    private String extractedLineId;

    public CatalogueLineValidator(CatalogueType catalogueType, CatalogueLineType catalogueLine) {
        this(catalogueType, catalogueLine, new ArrayList<>(), new ArrayList<>());
    }

    public CatalogueLineValidator(CatalogueType catalogueType, CatalogueLineType catalogueLine, List<String> errorMessages, List<List<String>> errorParameters) {
        this.owningCatalogue = catalogueType;
        this.errorMessages = errorMessages;
        this.catalogueLine = catalogueLine;
        this.errorParameters = errorParameters;
    }

    public ValidationMessages validate() {
        // set the ID to be used during the subsequent validations
        extractedLineId = !Strings.isNullOrEmpty(catalogueLine.getID()) ? catalogueLine.getID() : catalogueLine.getGoodsItem().getItem().getManufacturersItemIdentification().getID();

        idExists();
        idHasInvalidSpace();
        manufacturerIdExists();
        lineIdManufacturerIdMatches();
        nameExists();
        commodityClassificationExists();
        partyIdsMatch();
        fileSizesLessThanTheMaximum();
        checkReferenceToCatalogue();
        checkPrecedingTrailingSpace();

        return new ValidationMessages(errorMessages,errorParameters);
    }

    private void checkPrecedingTrailingSpace() {
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

    private void idExists() {
        if (extractedLineId == null) {
            errorMessages.add(NimbleExceptionMessageCode.BAD_REQUEST_NO_ID_FOR_LINE.toString());
            errorParameters.add(new ArrayList<>());
        }
    }

    private void idHasInvalidSpace() {
        if (extractedLineId != null && extractedLineId.length() != extractedLineId.trim().length()) {
            errorMessages.add(NimbleExceptionMessageCode.BAD_REQUEST_PRECEDING_TRAILING_IN_ID.toString());
            errorParameters.add(Arrays.asList(extractedLineId));
        }
    }
    private void checkReferenceToCatalogue() {
        if(owningCatalogue.getUUID() != null){
            ItemType item = catalogueLine.getGoodsItem().getItem();
            if (!item.getCatalogueDocumentReference().getID().equals(owningCatalogue.getUUID())) {
                errorMessages.add(NimbleExceptionMessageCode.BAD_REQUEST_INVALID_REFERENCE.toString());
                errorParameters.add(Arrays.asList(extractedLineId));
            }
        }
    }

    private void manufacturerIdExists() {
        ItemType item = catalogueLine.getGoodsItem().getItem();
        if (Strings.isNullOrEmpty(item.getManufacturerParty().getPartyIdentification().get(0).getID())) {
            errorMessages.add(NimbleExceptionMessageCode.BAD_REQUEST_NO_MANUFACTURER_PARTY.toString());
            errorParameters.add(Arrays.asList(extractedLineId));
        }
    }

    private void lineIdManufacturerIdMatches() {
        ItemType item = catalogueLine.getGoodsItem().getItem();
        if (!Strings.isNullOrEmpty(catalogueLine.getID()) && !Strings.isNullOrEmpty(item.getManufacturersItemIdentification().getID())) {
            if (!catalogueLine.getID().contentEquals(item.getManufacturersItemIdentification().getID())) {
                errorMessages.add(NimbleExceptionMessageCode.BAD_REQUEST_IDS_DO_NOT_MATCH.toString());
                errorParameters.add(Arrays.asList(extractedLineId, item.getManufacturersItemIdentification().getID()));
            }
        }
    }

    private void nameExists() {
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
    }

    private void commodityClassificationExists() {
        ItemType item = catalogueLine.getGoodsItem().getItem();
        if (item.getCommodityClassification().size() == 0) {
            errorMessages.add(NimbleExceptionMessageCode.BAD_REQUEST_NO_COMMODITY_CLASSIFICATION.toString());
            errorParameters.add(Arrays.asList(extractedLineId));
        }
    }

    private void partyIdsMatch() {
        ItemType item = catalogueLine.getGoodsItem().getItem();
        String catalogueProviderPartyId = owningCatalogue.getProviderParty().getPartyIdentification().get(0).getID();
        String itemManufacturerPartyId = item.getManufacturerParty().getPartyIdentification().get(0).getID();
        if (!catalogueProviderPartyId.contentEquals(itemManufacturerPartyId)) {
            errorMessages.add(NimbleExceptionMessageCode.BAD_REQUEST_PARTY_IDS_DO_NOT_MATCH.toString());
            errorParameters.add(Arrays.asList(extractedLineId, catalogueProviderPartyId, itemManufacturerPartyId));
        }
    }

    private void fileSizesLessThanTheMaximum() {
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
