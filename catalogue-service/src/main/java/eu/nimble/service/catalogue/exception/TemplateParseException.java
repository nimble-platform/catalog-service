package eu.nimble.service.catalogue.exception;

import java.io.IOException;

/**
 * Created by suat on 15-Sep-17.
 */
public class TemplateParseException extends Exception {
    public TemplateParseException(String message) {
        super(message);
    }

    public TemplateParseException(String message, Exception cause) {
        super(message, cause);
    }
}
