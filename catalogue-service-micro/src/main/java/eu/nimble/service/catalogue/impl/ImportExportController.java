package eu.nimble.service.catalogue.impl;

import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import eu.nimble.service.catalogue.CatalogueService;
import eu.nimble.service.catalogue.CatalogueServiceImpl;
import eu.nimble.service.catalogue.util.SpringBridge;
import eu.nimble.service.model.ubl.catalogue.CatalogueType;
import eu.nimble.service.model.ubl.commonaggregatecomponents.CatalogueLineType;
import eu.nimble.service.model.ubl.commonaggregatecomponents.PartyType;
import eu.nimble.service.model.ubl.commonaggregatecomponents.PersonType;
import eu.nimble.utility.Configuration;
import eu.nimble.utility.HttpResponseUtil;
import eu.nimble.utility.JsonSerializationUtility;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.logging.LogLevel;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

@Controller
public class ImportExportController {

    private static Logger log = LoggerFactory
            .getLogger(ImportExportController.class);

    private CatalogueService service = CatalogueServiceImpl.getInstance();

    /**
     * This service accepts a serialized catalogue with database identifiers and imports it to the configured database
     * after removing the database identifiers. The service replaces the {@link PartyType} information in the given
     * catalogue with the {@link PartyType} obtained from the identity service based on the user calling the service.
     *
     * @param serializedCatalogue
     * @param bearerToken
     * @return
     */
    @CrossOrigin(origins = {"*"})
    @RequestMapping(value = "/catalogue/import",
            method = RequestMethod.POST,
            consumes = {MediaType.APPLICATION_JSON_VALUE})
    public ResponseEntity importCatalogue(@RequestBody String serializedCatalogue,
                                          @RequestHeader(value = "Authorization") String bearerToken) {
        try {
            log.info("Importing catalogue ...");
            // remove hjid fields of catalogue
            JSONObject object = new JSONObject(serializedCatalogue);
            JsonSerializationUtility.removeHjidFields(object);
            CatalogueType catalogue = JsonSerializationUtility.getObjectMapper().readValue(object.toString(), CatalogueType.class);

            // get person using the given bearer token
            PersonType person = SpringBridge.getInstance().getIdentityClientTyped().getPerson(bearerToken);
            // get party for the person
            PartyType party = SpringBridge.getInstance().getIdentityClientTyped().getPartyByPersonID(person.getID()).get(0);

            // remove hjid fields of party
            JSONObject partyObject = new JSONObject(party);
            JsonSerializationUtility.removeHjidFields(partyObject);
            party = JsonSerializationUtility.getObjectMapper().readValue(partyObject.toString(), PartyType.class);

            // replaced provider party of the catalogue with the party
            catalogue.setProviderParty(party);
            for (CatalogueLineType catalogueLineType : catalogue.getCatalogueLine()) {
                // replaced manufacturer parties of catalogue lines with the party
                catalogueLineType.getGoodsItem().getItem().setManufacturerParty(party);
            }
            // add the catalogue
            catalogue = service.addCatalogueWithUUID(catalogue, Configuration.Standard.UBL, catalogue.getUUID());
            log.info("Imported the catalogue successfully");
            return ResponseEntity.ok().body(JsonSerializationUtility.serializeEntity(catalogue));

        } catch (Exception e) {
            String msg = String.format("Failed to import catalogue: %s", serializedCatalogue);
            log.error(msg, e);
            return HttpResponseUtil.createResponseEntityAndLog(msg, e, HttpStatus.INTERNAL_SERVER_ERROR, LogLevel.ERROR);
        }
    }
}
