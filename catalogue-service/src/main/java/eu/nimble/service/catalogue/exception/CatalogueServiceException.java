package eu.nimble.service.catalogue.exception;

import eu.nimble.utility.exception.NimbleException;
import java.util.List;
/**
 * Created by suat on 07-Mar-17.
 */
public class CatalogueServiceException extends NimbleException {
    public CatalogueServiceException(String message, Exception cause) {
        super(message, cause);
    }

    public CatalogueServiceException(String messageCode, List<String> parameters) {
        super(messageCode, parameters);
    }
}
