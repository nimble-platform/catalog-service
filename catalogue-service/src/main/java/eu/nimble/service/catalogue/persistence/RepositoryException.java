package eu.nimble.service.catalogue.persistence;

/**
 * Created by suat on 17-Dec-18.
 */
// TODO it would be wiser to implement this as a regular Exception.
public class RepositoryException extends RuntimeException {
    public RepositoryException(String message) {
        super(message);
    }
}
