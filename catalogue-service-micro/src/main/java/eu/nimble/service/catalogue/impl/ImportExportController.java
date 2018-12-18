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
import eu.nimble.utility.JsonSerializationUtility;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

@Controller
public class ImportExportController {

    private static Logger log = LoggerFactory
            .getLogger(ImportExportController.class);

    private CatalogueService service = CatalogueServiceImpl.getInstance();

    @CrossOrigin(origins = {"*"})
    @RequestMapping(value = "/import",
            method = RequestMethod.POST,
            consumes = {MediaType.APPLICATION_JSON_VALUE})
    public ResponseEntity importCatalogue(@RequestBody String serializedCatalogue,
                                          @RequestHeader(value = "Authorization") String bearerToken) {
        try {
            log.info("Importing catalogue ...");
            // remove hjid fields of catalogue
            JSONObject object = new JSONObject(serializedCatalogue);
            JsonSerializationUtility.removeHjidFields(object);
            CatalogueType catalogue = new ObjectMapper().readValue(object.toString(), CatalogueType.class);

            // get person using the given bearer token
            PersonType person = SpringBridge.getInstance().getIdentityClientTyped().getPerson(bearerToken);
            // get party for the person
            PartyType party = SpringBridge.getInstance().getIdentityClientTyped().getPartyByPersonID(person.getID());
            // remove hjid fields of party
            JSONObject partyObject = new JSONObject(party);
            JsonSerializationUtility.removeHjidFields(partyObject);
            party = new ObjectMapper().configure(MapperFeature.ACCEPT_CASE_INSENSITIVE_PROPERTIES, true).readValue(partyObject.toString(), PartyType.class);

            // replaced provider party of the catalogue with the party
            catalogue.setProviderParty(party);
            for (CatalogueLineType catalogueLineType : catalogue.getCatalogueLine()) {
                // replaced manufacturer parties of catalogue lines with the party
                catalogueLineType.getGoodsItem().getItem().setManufacturerParty(party);
            }
            // add the catalogue
            service.addCatalogueWithUUID(catalogue, Configuration.Standard.UBL, catalogue.getUUID());
            log.info("Imported the catalogue successfully");
            return ResponseEntity.ok(null);
        } catch (Exception e) {
            System.out.println(e);
        }
        return ResponseEntity.ok(null);
    }
}
