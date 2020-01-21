package eu.nimble.service.catalogue.util;

import eu.nimble.utility.exception.AuthenticationException;
import eu.nimble.utility.exception.NimbleException;
import eu.nimble.utility.exception.NimbleExceptionMessageCode;
import org.springframework.http.HttpStatus;

import java.io.IOException;
import java.util.Arrays;

/**
 * Created by suat on 09-Jan-19.
 */
public class HttpResponseUtil {

    /**
     * Throws a {@link NimbleException} if there is no associated user for the provided token.
     *
     * @param token
     * @return
     */
    public static void checkToken(String token) {
        if(SpringBridge.getInstance().getCatalogueServiceConfig().getCheckToken()){
            try {
                // check token
                boolean isValid = SpringBridge.getInstance().getiIdentityClientTyped().getUserInfo(token);
                if (!isValid) {
                    throw new NimbleException(NimbleExceptionMessageCode.UNAUTHORIZED_NO_USER_FOR_TOKEN.toString(), Arrays.asList(token));
                }
            } catch (IOException e) {
                throw new NimbleException(NimbleExceptionMessageCode.INTERNAL_SERVER_ERROR_FAILED_TO_CHECK_TOKEN.toString(), Arrays.asList(token),e);
            }
        }
    }

    public static void validateToken(String token) throws AuthenticationException {
        try {
            // check token
            boolean isValid = SpringBridge.getInstance().getiIdentityClientTyped().getUserInfo(token);
            if (!isValid) {
                String msg = String.format("No user exists for the given token : %s", token);
                throw new AuthenticationException(msg);
            }
        } catch (IOException e) {
            throw new AuthenticationException(String.format("Failed to check user authorization for token: %s", token), e);
        }
    }
}
