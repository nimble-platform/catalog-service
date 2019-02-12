package eu.nimble.service.catalogue.validation;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by suat on 06-Feb-19.
 */
public class Validator {
     protected List<String> errorMessages = new ArrayList<>();

     protected void throwExceptionIfError() throws ValidationException{
         if (errorMessages.size() > 0) {
             StringBuilder sb = new StringBuilder("");
             for (String error : errorMessages) {
                 sb.append(error).append(System.lineSeparator());
             }
             throw new ValidationException(sb.toString());
         }
     }
}
