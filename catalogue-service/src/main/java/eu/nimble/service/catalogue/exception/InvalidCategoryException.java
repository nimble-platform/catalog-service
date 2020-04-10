package eu.nimble.service.catalogue.exception;

import eu.nimble.utility.exception.NimbleException;
import java.util.Arrays;

public class InvalidCategoryException extends NimbleException {

    public InvalidCategoryException(String categoryId) {
        super(NimbleExceptionMessageCode.NOT_FOUND_NO_CATEGORY.toString(), Arrays.asList(categoryId));
    }
}
