package eu.nimble.service.catalogue.validation;

import com.google.common.base.Strings;
import eu.nimble.service.model.ubl.catalogue.CatalogueType;
import eu.nimble.service.model.ubl.commonaggregatecomponents.CatalogueLineType;
import eu.nimble.utility.exception.NimbleExceptionMessageCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by suat on 07-Aug-18.
 */
public class CatalogueValidator {

    private static Logger logger = LoggerFactory.getLogger(CatalogueValidator.class);

    private List<String> errorMessages = new ArrayList<>();
    private List<List<String>> errorParameters = new ArrayList<>();

    private CatalogueType catalogueType;

    public CatalogueValidator(CatalogueType catalogueType) {
        this.catalogueType = catalogueType;
    }

    public ValidationMessages validate() {
        idExists();
        validateLines();

        logger.info("Catalogue: {} validated", catalogueType.getUUID());
        return new ValidationMessages(errorMessages,errorParameters);
    }

    private void idExists() {
        if(Strings.isNullOrEmpty(catalogueType.getID())) {
            errorMessages.add(NimbleExceptionMessageCode.BAD_REQUEST_NO_ID_FOR_CATALOGUE.toString());
            errorParameters.add(new ArrayList<>());
        }
    }

    private void validateLines() {
        for (CatalogueLineType line : catalogueType.getCatalogueLine()) {
            CatalogueLineValidator catalogueLineValidator = new CatalogueLineValidator(catalogueType, line);
            ValidationMessages validationMessages = catalogueLineValidator.validate();
            errorMessages.addAll(validationMessages.getErrorMessages());
            errorParameters.addAll(validationMessages.getErrorParameters());
        }
    }
}
