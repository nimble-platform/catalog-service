package eu.nimble.service.catalogue.util.migration.r8;

import com.fasterxml.jackson.core.type.TypeReference;
import eu.nimble.common.rest.identity.IIdentityClientTyped;
import eu.nimble.service.catalogue.index.ItemIndexClient;
import eu.nimble.service.catalogue.persistence.util.CataloguePersistenceUtil;
import eu.nimble.service.model.ubl.catalogue.CatalogueType;
import eu.nimble.utility.JsonSerializationUtility;
import feign.Response;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.List;

/**
 * Created by suat on 18-Feb-19.
 */
@Component
public class CatalogueIndexLoader {

    private static final Logger logger = LoggerFactory.getLogger(CatalogueIndexLoader.class);

    @Autowired
    private ItemIndexClient itemIndexClient;
    @Autowired
    private IIdentityClientTyped iIdentityClientTyped;

    public void indexCatalogues(String partyId) {
        // get catalogues
        List<CatalogueType> catalogues;
        if(partyId == null){
            catalogues = CataloguePersistenceUtil.getAllProductCatalogues();
        }
        else {
            catalogues = CataloguePersistenceUtil.getAllCataloguesForParty(partyId);
        }
        for(CatalogueType catalogue : catalogues) {
            try {
                itemIndexClient.indexCatalogue(catalogue);
            } catch (Exception e) {
                // do nothing
            }
        }
        logger.info("Indexing catalogues completed");
    }

    public void indexVerifiedCompanyCatalogues(String bearer) throws Exception {
        // get catalogues
        List<CatalogueType> catalogues;

            Response response = iIdentityClientTyped.getVerifiedPartyIds(bearer);
            if (response.status() == HttpStatus.OK.value()) {
                try {
                    List<String> partyIDs;
                    String responseBody = IOUtils.toString(response.body().asInputStream());
                    partyIDs = JsonSerializationUtility.deserializeContent(responseBody, new TypeReference<List<String>>(){});
                    logger.info("Retrieved verified party IDs successfully. IDs : {} ", partyIDs);

                    for(String partyID : partyIDs){
                        logger.info("Retrieving catalogues from the company {}", partyID );
                        catalogues = CataloguePersistenceUtil.getAllProductCataloguesForParty(partyID);
                        for(CatalogueType catalogue : catalogues) {
                            try {
                                itemIndexClient.indexCatalogue(catalogue);
                            } catch (Exception e) {
                                logger.error("Failed to index catalogues" , e);
                            }
                        }

                    }
                    logger.info("Indexing catalogues completed");

                } catch (IOException e) {
                    logger.error("Failed to parse party response", e);
                }

            } else {
                logger.error("Failed to retrieve parties response, identity call status: {}, message: {}",
                        response.status(), IOUtils.toString(response.body().asInputStream()));
            }

    }
}
