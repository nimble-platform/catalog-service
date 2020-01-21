package eu.nimble.service.catalogue.validation;

import java.util.List;

public class ValidationMessages {

    private List<String> errorMessages;
    private List<List<String>> errorParameters;

    public ValidationMessages(List<String> errorMessages, List<List<String>> errorParameters) {
        this.errorMessages = errorMessages;
        this.errorParameters = errorParameters;
    }

    public List<String> getErrorMessages() {
        return errorMessages;
    }

    public void setErrorMessages(List<String> errorMessages) {
        this.errorMessages = errorMessages;
    }

    public List<List<String>> getErrorParameters() {
        return errorParameters;
    }

    public void setErrorParameters(List<List<String>> errorParameters) {
        this.errorParameters = errorParameters;
    }
}
