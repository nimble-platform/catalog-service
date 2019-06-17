package eu.nimble.service.catalogue.util;

import org.springframework.boot.logging.LogLevel;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.io.IOException;

/**
 * Created by suat on 09-Jan-19.
 */
public class HttpResponseUtil {

    /**
     * Returns a non-empty {@link ResponseEntity} if there is no associated user for the provided token. Otherwise
     * {@code null} is returned.
     *
     * @param token
     * @return
     */
    public static ResponseEntity checkToken(String token) {
        System.out.println(SpringBridge.getInstance().getCatalogueServiceConfig().getCheckToken());
        if(SpringBridge.getInstance().getCatalogueServiceConfig().getCheckToken()){
            try {
                // check token
                boolean isValid = SpringBridge.getInstance().getiIdentityClientTyped().getUserInfo(token);
                if (!isValid) {
                    String msg = String.format("No user exists for the given token : %s", token);
                    return eu.nimble.utility.HttpResponseUtil.createResponseEntityAndLog(msg, null, HttpStatus.UNAUTHORIZED, LogLevel.INFO);
                }
            } catch (IOException e) {
                String msg = String.format("Failed to check user authorization for token: %s", token);
                return eu.nimble.utility.HttpResponseUtil.createResponseEntityAndLog(msg, e, HttpStatus.INTERNAL_SERVER_ERROR, LogLevel.ERROR);
            }
        }
        return null;
    }
}
