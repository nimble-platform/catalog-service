package eu.nimble.service.catalogue.exception;

import eu.nimble.utility.exception.NimbleException;
import java.util.List;

/**
 * Created by suat on 15-Sep-17.
 */
public class TemplateParseException extends NimbleException {
    public TemplateParseException(String message) {
        super(message);
    }

    public TemplateParseException(String message, Exception cause) {
        super(message, cause);
    }

    public TemplateParseException(String messageCode, List<String> parameters) {
        super(messageCode, parameters);
    }

    public TemplateParseException(String messageCode, List<String> parameters, Exception cause) {
        super(messageCode, parameters,cause);
    }
}
