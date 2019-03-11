package eu.nimble.service.catalogue.validation;

import eu.nimble.service.model.ubl.commonbasiccomponents.QuantityType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by suat on 11-Mar-19.
 */
public class QuantityValidator extends Validator{
    private static Logger logger = LoggerFactory.getLogger(QuantityValidator.class);

    private QuantityType quantityType;

    public QuantityValidator(QuantityType quantityType) {
        this.quantityType = quantityType;
    }

    public boolean bothFieldsPopulated() {
        return quantityType.getValue() != null && quantityType.getUnitCode() != null;
    }
}
