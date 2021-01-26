package eu.nimble.service.catalogue.util.email;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import eu.nimble.service.model.ubl.commonaggregatecomponents.PartyType;
import eu.nimble.service.model.ubl.commonaggregatecomponents.PersonType;
import eu.nimble.utility.JsonSerializationUtility;
import eu.nimble.utility.email.EmailService;
import eu.nimble.utility.validation.NimbleRole;
import org.json.JSONObject;
import org.keycloak.representations.idm.UserRepresentation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.thymeleaf.context.Context;

import java.io.IOException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

@Component
public class EmailSenderUtil {
        private final Logger logger = LoggerFactory.getLogger(this.getClass());

        @Value("${nimble.oauth.eFactoryClient.clientId}")
        private String eFactoryClientId;

        @Value("${nimble.oauth.eFactoryClient.clientSecret}")
        private String eFactoryClientSecret;

        @Value("${nimble.oauth.eFactoryClient.accessTokenUri}")
        private String eFactoryAccessTokenUri;

        @Value("${nimble.oauth.eFactoryClient.userDetailsUri}")
        private String eFactoryUserDetailsUri;
        @Value("${nimble.frontend.url}")
        private String frontEndURL;
        @Value("${spring.mail.platformName}")
        private String platformName;

        private final String CLIENT_CREDENTIALS_FLOW = "client_credentials";

        @Autowired
        private EmailService emailService;

        /**
         * Sends an email for the catalogue exchange request
         * @param requestDetails the request details as text
         * @param catalogueName the name of catalogue to be requested for exchange
         * @param requesterCompanyName the requester company name
         * @param catalogueProvider the party which provides the catalogue
         * */
        public void requestCatalogExchange(String requestDetails, String catalogueName, String requesterCompanyName,PersonType requesterUser, PartyType catalogueProvider){

            // construct the email list
            List<String> emailList = new ArrayList<>();
            // sales officers, monitor or legal representative receive the email notification
            List<String> legalRepresentativeEmailList = new ArrayList<>();
            List<String> monitorEmailList = new ArrayList<>();
            for (PersonType p : catalogueProvider.getPerson()) {
                if (p.getRole().contains(NimbleRole.SALES_OFFICER.getName())) {
                    emailList.add(p.getContact().getElectronicMail());
                }
                if(p.getRole().contains(NimbleRole.MONITOR.getName())){
                    monitorEmailList.add(p.getContact().getElectronicMail());
                }
                if(p.getRole().contains(NimbleRole.LEGAL_REPRESENTATIVE.getName())){
                    legalRepresentativeEmailList.add(p.getContact().getElectronicMail());
                }
            }
            if(emailList.size() == 0){
                // if there is no users with Sales Officer role, then, send it to monitors
                if(monitorEmailList.size() > 0)
                    emailList = monitorEmailList;
                // if there is no users with Monitor role, then, send it to legal representative
                else
                    emailList = legalRepresentativeEmailList;
            }
            // mail subject
            String subject = "Request for catalog exchange";
            // set variables for mail template
            Context context = new Context();
            context.setVariable("details", requestDetails);
            context.setVariable("requesterUserName",String.format("%s %s",requesterUser.getFirstName(),requesterUser.getFamilyName()));
            context.setVariable("requesterCompanyName",requesterCompanyName);
            context.setVariable("catalogName",catalogueName);
            context.setVariable("platformName",platformName);
            context.setVariable("partyName",catalogueProvider.getPartyName().get(0).getName().getValue());
            context.setVariable("url", frontEndURL+"/#/dashboard");
            // send the mail
            emailService.send(emailList.toArray(new String[0]), new String[]{requesterUser.getContact().getElectronicMail()},subject,"catalog_exchange",context);
        }

        public void offerProducts(String offerDetails, List<String> vatNumbers, List<String> catalogueUuids, List<String> lineIds, String companyName) throws IOException, UnirestException {
            String url = eFactoryAccessTokenUri;
            RestTemplate restTemplate = new RestTemplate();
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

            MultiValueMap<String, String> map= new LinkedMultiValueMap<String, String>();

            map.add("grant_type", CLIENT_CREDENTIALS_FLOW);
            map.add("client_id", eFactoryClientId);
            map.add("client_secret", eFactoryClientSecret);

            HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<MultiValueMap<String, String>>(map, headers);

            List<UserRepresentation> userRepresentationsForMail = new ArrayList<>();
            try {
                ResponseEntity<String> response = restTemplate.postForEntity(url, request, String.class);

                JSONObject jsonObject = new JSONObject(response.getBody());
                String accessToken = jsonObject.get("access_token").toString();

                ObjectMapper mapper = JsonSerializationUtility.getObjectMapper();
                HttpResponse<String> httpResponse = Unirest.get(eFactoryUserDetailsUri)
                        .header("Authorization","Bearer "+accessToken).asString();
                List<UserRepresentation> userRepresentations = mapper.readValue(httpResponse.getBody(), new TypeReference<List<UserRepresentation>>(){});
                for (UserRepresentation userRepresentation : userRepresentations) {
                    if(userRepresentation.getAttributes() != null){
                        List<String> userVatNumbers = userRepresentation.getAttributes().get("vatin");
                        if(userVatNumbers != null && userVatNumbers.size() > 0 && vatNumbers.contains(userVatNumbers.get(0))){
                            userRepresentationsForMail.add(userRepresentation);
                        }
                    }
                }

            } catch (Exception e) {
                logger.error("Failed to retrieve user emails for the vat numbers: {}",vatNumbers,e);
                throw e;
            }

            List<String> productDetailUrls = new ArrayList<>();
            int size = catalogueUuids.size();
            for(int i = 0; i < size; i++){
                productDetailUrls.add(frontEndURL+String.format("/#/product-details?catalogueId=%s&id=%s",catalogueUuids.get(i), URLEncoder.encode(lineIds.get(i),"UTF-8")));
            }

            if(userRepresentationsForMail.size() == 0){
                logger.info("No email address found to send the offer");
            }
            else{

                String subject = "Offer Letter";
                for (UserRepresentation userRepresentation : userRepresentationsForMail) {
                    String toEmail = userRepresentation.getEmail();
                    String userName = String.format("%s %s",userRepresentation.getFirstName(), userRepresentation.getLastName());

                    Context context = new Context();
                    context.setVariable("details", offerDetails);
                    context.setVariable("user",userName);
                    context.setVariable("companyName",companyName);
                    context.setVariable("platformName",platformName);
                    context.setVariable("urls", String.join("\n",productDetailUrls));
                    emailService.send(new String[]{toEmail},subject,"offer_product",context);
                }

            }
        }
}
