package eu.nimble.service.catalogue.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.logging.LogLevel;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Created by suat on 16-May-18.
 */
@Component
public class HttpResponseUtil {
    private static final Logger logger = LoggerFactory.getLogger(HttpResponseUtil.class);

    public static ResponseEntity createResponseEntityAndLog(String msg, Exception e, HttpStatus httpStatus, LogLevel logLevel) {
        if(logLevel == null || logLevel == LogLevel.INFO) {
            if(e != null) {
                logger.info(msg, e);
            } else {
                logger.info(msg);
            }
        } else if (logLevel == LogLevel.WARN) {
            if (e != null) {
                logger.warn(msg, e);
            } else {
                logger.warn(msg);
            }
        } else if (logLevel == LogLevel.ERROR) {
            if (e != null) {
                logger.error(msg, e);
            } else {
                logger.error(msg);
            }
        }
        return ResponseEntity.status(httpStatus).body(msg);
    }
}
