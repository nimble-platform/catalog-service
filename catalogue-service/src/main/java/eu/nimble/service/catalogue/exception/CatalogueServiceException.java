package eu.nimble.service.catalogue.exception;

/**
 * Created by suat on 07-Mar-17.
 */
public class CatalogueServiceException extends RuntimeException {
    public CatalogueServiceException(String message) {
        super(message);
    }

    public CatalogueServiceException(String message, Throwable cause) {
        super(message, cause);
    }
}
