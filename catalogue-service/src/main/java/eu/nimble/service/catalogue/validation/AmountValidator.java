package eu.nimble.service.catalogue.validation;

import eu.nimble.service.model.ubl.commonbasiccomponents.AmountType;
import eu.nimble.service.model.ubl.commonbasiccomponents.QuantityType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by suat on 11-Mar-19.
 */
public class AmountValidator extends Validator{
    private static Logger logger = LoggerFactory.getLogger(AmountValidator.class);

    private AmountType amountType;

    public AmountValidator(AmountType amountType) {
        this.amountType = amountType;
    }

    public boolean bothFieldsPopulated() {
        return amountType.getValue() != null && amountType.getCurrencyID() != null;
    }
}
