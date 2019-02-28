package eu.nimble.service.catalogue.validation;

import com.google.common.base.Strings;
import eu.nimble.service.model.ubl.catalogue.CatalogueType;
import eu.nimble.service.model.ubl.commonaggregatecomponents.CatalogueLineType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Created by suat on 07-Aug-18.
 */
public class CatalogueValidator extends Validator {

    private static Logger logger = LoggerFactory.getLogger(CatalogueValidator.class);

    private CatalogueType catalogueType;

    public CatalogueValidator(CatalogueType catalogueType) {
        this.catalogueType = catalogueType;
    }

    public void validate() throws ValidationException {
        idExists();
        validateLines();
        throwExceptionIfError();
        logger.info("Catalogue: {} validated", catalogueType.getUUID());
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
