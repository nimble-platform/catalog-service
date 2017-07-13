package eu.nimble.service.catalogue.impl;

import javax.servlet.http.HttpServletRequest;

public class Utils {

    public static String baseUrl(HttpServletRequest request) {
        String baseUrl = request.getRequestURL().toString();
        if(!baseUrl.endsWith("/"))
            baseUrl += "/";
        return baseUrl;
    }
}
