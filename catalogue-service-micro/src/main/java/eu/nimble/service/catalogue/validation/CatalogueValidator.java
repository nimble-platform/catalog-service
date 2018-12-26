package eu.nimble.service.catalogue.validation;

import com.google.common.base.Strings;
import eu.nimble.service.model.ubl.catalogue.CatalogueType;
import eu.nimble.service.model.ubl.commonaggregatecomponents.CatalogueLineType;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by suat on 07-Aug-18.
 */
public class CatalogueValidator {

    private CatalogueType catalogueType;
    private List<String> errorMessages;

    public CatalogueValidator(CatalogueType catalogueType) {
        this.catalogueType = catalogueType;
        this.errorMessages = new ArrayList<>();
    }

    public List<String> validate() {
        idExists();
        validateLines();
        return errorMessages;
    }

    private void idExists() {
        if(Strings.isNullOrEmpty(catalogueType.getID())) {
            errorMessages.add("No ID set for the catalogue");
        }
    }

    private void validateLines() {
        for (CatalogueLineType line : catalogueType.getCatalogueLine()) {
            CatalogueLineValidator catalogueLineValidator = new CatalogueLineValidator(catalogueType, line, errorMessages);
            catalogueLineValidator.validate();
        }
    }
}
