package eu.nimble.service.catalogue.config.interceptor;

import eu.nimble.service.catalogue.util.ExecutionContext;
import eu.nimble.service.catalogue.util.HttpResponseUtil;
import eu.nimble.utility.exception.AuthenticationException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * This interceptor injects the bearer token into the {@link ExecutionContext} for each Rest call
 *
 * Created by suat on 24-Jan-19.
 */
@Configuration
public class RestServiceInterceptor extends HandlerInterceptorAdapter {
    @Autowired
    private ExecutionContext executionContext;

    private final String swaggerPath = "swagger-resources";
    private final String apiDocsPath = "api-docs";

    @Override
    public boolean preHandle (HttpServletRequest request, HttpServletResponse response, Object handler) throws AuthenticationException {

        String bearerToken = request.getHeader(HttpHeaders.AUTHORIZATION);

        // do not validate the token for swagger operations
        if(bearerToken != null && !(request.getServletPath().contains(swaggerPath) || request.getServletPath().contains(apiDocsPath))){
            // validate token
            HttpResponseUtil.validateToken(bearerToken);
        }

        executionContext.setBearerToken(bearerToken);
        return true;
    }
}
