package eu.nimble.service.catalogue.sync;

/**
 * Created by suat on 07-Mar-17.
 */
public class MarmottaSynchronizationException extends Exception {
    public MarmottaSynchronizationException(String message) {
        super(message);
    }

    public MarmottaSynchronizationException(String message, Throwable cause) {
        super(message, cause);
    }
}
