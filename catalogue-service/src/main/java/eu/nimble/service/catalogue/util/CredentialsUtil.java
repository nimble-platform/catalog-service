package eu.nimble.service.catalogue.util;

import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.Unirest;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class CredentialsUtil {

    private static final Logger logger = LoggerFactory.getLogger(CredentialsUtil.class);

    @Value("${nimble.oauth.client.clientId}")
    private String oauthClientId;
    @Value("${nimble.oauth.client.clientSecret}")
    private String oauthClientSecret;
    @Value("${nimble.oauth.client.accessTokenUri}")
    private String oauthAccessTokenUri;

    @Autowired
    private ExecutionContext executionContext;

    public String getBearerToken(){
        // Firstly, try to get bearer token from the execution context
        // if it fails, then get the token from the authorization server
        try {
            return executionContext.getBearerToken();
        }catch (BeanCreationException exception){
            logger.warn("Failed to get bearer token from execution context" + exception.getMessage());
        }
        return getAccessTokenForCatalogueService();
    }

    private String getAccessTokenForCatalogueService(){

        try {
            HttpResponse<String> response = Unirest.post(oauthAccessTokenUri)
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .field("grant_type","client_credentials")
                    .field("client_id",oauthClientId)
                    .field("client_secret",oauthClientSecret)
                    .asString();

            if(response.getStatus() != 200){
                logger.error("Failed to get access token :" + response.getBody());
                return null;
            }

            JSONObject jsonObject = new JSONObject(response.getBody());
            return (String) jsonObject.get("access_token");
        } catch (Exception e) {
            logger.error("Failed to get access token :"+e.getMessage());
            return null;
        }
    }

}
