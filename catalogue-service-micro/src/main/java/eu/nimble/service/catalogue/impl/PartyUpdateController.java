package eu.nimble.service.catalogue.impl;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import eu.nimble.service.catalogue.CatalogueDatabaseAdapter;
import eu.nimble.service.catalogue.sync.MarmottaSynchronizer;
import eu.nimble.service.catalogue.util.HttpResponseUtil;
import eu.nimble.service.model.ubl.catalogue.CatalogueType;
import eu.nimble.service.model.ubl.commonaggregatecomponents.PartyType;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.logging.LogLevel;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;

/**
 * Created by suat on 24-Sep-18.
 */
@Controller
public class PartyUpdateController {
    private static Logger logger = LoggerFactory.getLogger(CatalogueController.class);

    @CrossOrigin(origins = {"*"})
    @ApiOperation(value = "onPartyUpdateInIdentity", notes = "" +
            "Triggers a re-index operation based on an update on company information in the identity service. ")
    @RequestMapping(value = "/catalogue/party/{partyId}/identity",
            method = RequestMethod.POST)
    public ResponseEntity onPartyUpdateInIdentity(@ApiParam(value = "Identifier of the updated party") @PathVariable String partyId,
                                                  @ApiParam(value = "Authorization header to be obtained via login to the NIMBLE platform") @RequestHeader(value = "Authorization") String bearerToken) {
        try {
            logger.info("Handling the party identity update for party: {}", partyId);
            CatalogueDatabaseAdapter.syncPartyInUBLDB(partyId, bearerToken);
            MarmottaSynchronizer.getInstance().addRecord(partyId);
            logger.info("Handled the party identity update for party: {}", partyId);
            return ResponseEntity.ok().build();

        } catch (Exception e) {
            String msg = String.format("Unexpected error while handling the party synchronization update for party: %s", partyId);
            return HttpResponseUtil.createResponseEntityAndLog(msg, e, HttpStatus.INTERNAL_SERVER_ERROR, LogLevel.ERROR);
        }
    }

    @CrossOrigin(origins = {"*"})
    @ApiOperation(value = "", notes = "" +
            "Triggers a re-index operation based on an update on the trust values of a company in the trust service. ")
    @RequestMapping(value = "/catalogue/party/{partyId}/trust",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            method = RequestMethod.POST)
    public ResponseEntity onPartyTrustUpdate(@ApiParam(value = "Serialization of a PartyType instance via a Jackson ObjectMapper initialized with default configurations") @RequestBody String serializedParty,
                                             @ApiParam(value = "Authorization header to be obtained via login to the NIMBLE platform") @RequestHeader(value = "Authorization") String bearerToken) {

        try {
            logger.info("Handling the party trust score update");
            ObjectMapper objectMapper = new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
            PartyType trustParty;
            try {
                trustParty = objectMapper.readValue(serializedParty, PartyType.class);
            } catch (IOException e) {
                String msg = String.format("Failed to parse the provided party data: %s", serializedParty);
                return HttpResponseUtil.createResponseEntityAndLog(msg, e, HttpStatus.BAD_REQUEST, LogLevel.WARN);
            }
            CatalogueDatabaseAdapter.syncTrustScores(trustParty);
            logger.info("Handled the party identity update for party: {}", trustParty.getID());
            return ResponseEntity.ok().build();

        } catch (Exception e) {
            String msg = "Unexpected error while handling the party synchronization update";
            return HttpResponseUtil.createResponseEntityAndLog(msg, e, HttpStatus.INTERNAL_SERVER_ERROR, LogLevel.ERROR);
        }
    }
}
