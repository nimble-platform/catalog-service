package eu.nimble.service.catalogue.impl;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import eu.nimble.service.catalogue.CatalogueService;
import eu.nimble.service.catalogue.CatalogueServiceImpl;
import eu.nimble.utility.config.CatalogueServiceConfig;
import eu.nimble.service.model.ubl.catalogue.CatalogueType;
import eu.nimble.service.model.ubl.commonaggregatecomponents.CatalogueLineType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

/**
 * Created by suat on 22-Aug-17.
 * <p>
 * Catalogue line-level REST services. All services defined in this class are prefixed with the
 * "/catalogue/{catalogueUuid}/catalogueline" path where the {@catalogueId} is the unique identifier of the specified
 * catalogue. All services in this class work only on the UBL based catalogues.
 */
@Controller
@RequestMapping(value = "/catalogue/{catalogueUuid}/catalogueline")
public class CatalogueLineController {
    private static Logger log = LoggerFactory.getLogger(CatalogueLineController.class);

    private CatalogueService service = CatalogueServiceImpl.getInstance();

    /**
     * Retrieves the catalogue line specified with the {@code lineId} parameter
     *
     * @param catalogueUuid
     * @param lineId
     * @return <li>200 along with the requested catalogue line</li>
     * <li>204 if there does not exists a catalogue line with the given lineId</li>
     */
    @CrossOrigin(origins = {"*"})
    @RequestMapping(value = "/{lineId}",
            produces = {"application/json"},
            method = RequestMethod.GET)
    public ResponseEntity<CatalogueLineType> getCatalogueLine(@PathVariable String catalogueUuid, @PathVariable String lineId) {
        log.info("Incoming request to get catalogue line with lineId: {}", lineId);

        CatalogueLineType catalogueLine;
        try {
            catalogueLine = service.getCatalogueLine(catalogueUuid, lineId);
        } catch (Exception e) {
            return createErrorResponseEntity("Failed to get catalogue line", HttpStatus.BAD_REQUEST, e);
        }

        if (catalogueLine == null) {
            return ResponseEntity.status(HttpStatus.NO_CONTENT).body(null);
        }
        log.info("Completed the request to get catalogue line with lineId: {}", lineId);
        return ResponseEntity.ok(catalogueLine);
    }

    /**
     * Adds the provided line
     *
     * @param catalogueUuid
     * @param catalogueLineJson
     * @return
     */
    @CrossOrigin(origins = {"*"})
    @RequestMapping(value = "/{catalogueUuid}",
            consumes = {"application/json"},
            produces = {"application/json"},
            method = RequestMethod.POST)
    public ResponseEntity addCatalogueLine(@PathVariable String catalogueUuid, @RequestBody String catalogueLineJson) {
        log.info("Incoming request to add catalogue line to catalogue: {}", catalogueUuid);
        log.debug("Catalogue line content: {}", catalogueLineJson);
        CatalogueType catalogue;
        CatalogueLineType catalogueLine;
        try {
            catalogue = service.getCatalogue(catalogueUuid);
            catalogueLine = new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                    .readValue(catalogueLineJson, CatalogueLineType.class);

            catalogueLine = service.addLineToCatalogue(catalogue, catalogueLine);

        } catch (IOException e) {
            return createErrorResponseEntity("Failed to deserialize catalogue line from json string", HttpStatus.BAD_REQUEST, e);
        } catch (Exception e) {
            return createErrorResponseEntity("Failed to add the provided catalogue line", HttpStatus.INTERNAL_SERVER_ERROR, e);
        }

        return createCreatedCatalogueLineResponse(catalogueUuid, catalogueLine);
    }

    private ResponseEntity createCreatedCatalogueLineResponse(String catalogueUuid, CatalogueLineType line) {
        URI lineURI;
        try {
            String applicationUrl = CatalogueServiceConfig.getInstance().getSpringApplicationUrl();
            lineURI = new URI(applicationUrl + "/catalogue/" + catalogueUuid + "/" + line.getID());
        } catch (URISyntaxException e) {
            String msg = "Failed to generate a URI for the newly created item";
            log.warn(msg, e);
            try {
                log.info("Completed request to add catalogue line with an empty URI, catalogue uuid: {}, lineId: {}", catalogueUuid, line.getID());
                return ResponseEntity.created(new URI("")).body(line);
            } catch (URISyntaxException e1) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("WTF");
            }
        }
        log.info("Completed request to add catalogue, catalogue uuid: {}, lineId: {}", catalogueUuid, line.getID());
        return ResponseEntity.created(lineURI).body(line);
    }

    /**
     * Updates the catalogue line
     *
     * @param catalogueUuid
     * @param catalogueLineJson updated catalogue line information
     * @return
     */
    @CrossOrigin(origins = {"*"})
    @RequestMapping(consumes = {"application/json"},
            produces = {"application/json"},
            method = RequestMethod.PUT)
    public ResponseEntity updateCatalogueLine(@PathVariable String catalogueUuid, @RequestBody String catalogueLineJson) {
        log.info("Incoming request to update catalogue line. Catalogue uuid: {}", catalogueUuid);
        CatalogueLineType catalogueLine = null;
        try {
            catalogueLine = new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                    .readValue(catalogueLineJson, CatalogueLineType.class);

            service.updateCatalogueLine(catalogueLine);

        } catch (IOException e) {
            return createErrorResponseEntity("Failed to deserialize catalogue line from json string", HttpStatus.BAD_REQUEST, e);
        } catch (Exception e) {
            return createErrorResponseEntity("Failed to add the provided catalogue line", HttpStatus.INTERNAL_SERVER_ERROR, e);
        }


        log.info("Completed the request to add catalogue line catalogue uuid, line lineId: {}", catalogueUuid, catalogueLine.getID());
        return ResponseEntity.ok(catalogueLine);
    }

    /**
     * Deletes the specified catalogue line
     *
     * @param catalogueUuid
     * @param lineId
     * @return
     */
    @CrossOrigin(origins = {"*"})
    @RequestMapping(value = "/{lineId}",
            produces = {"application/json"},
            method = RequestMethod.DELETE)
    public ResponseEntity deleteCatalogueLineById(@PathVariable String catalogueUuid, @PathVariable String lineId) {
        log.info("Incoming request to delete catalogue line. catalogue uuid: {}: line lineId {}", catalogueUuid, lineId);
        try {
            service.deleteCatalogueLineById(catalogueUuid, lineId);
        } catch (Exception e) {
            return createErrorResponseEntity("Failed to delete the catalogue line. catalogue uuid: " + catalogueUuid + " line id: " + lineId, HttpStatus.INTERNAL_SERVER_ERROR, e);
        }
        log.info("Completed the request to delete catalogue line: catalogue uuid: {}, lineId: {}", catalogueUuid, lineId);
        return ResponseEntity.status(HttpStatus.OK).build();
    }

    private ResponseEntity createErrorResponseEntity(String msg, HttpStatus status, Exception e) {
        msg = msg + e.getMessage();
        log.error(msg, e);
        return ResponseEntity.status(status).body(msg);
    }
}
