package eu.nimble.service.catalogue.impl;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import eu.nimble.service.catalogue.CatalogueService;
import eu.nimble.service.model.ubl.catalogue.CatalogueType;
import eu.nimble.service.model.ubl.commonaggregatecomponents.CatalogueLineType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;

/**
 * Created by suat on 22-Aug-17.
 */
@Controller
@RequestMapping(value = "/catalogueline")
public class CatalogueLineController {
    private static Logger log = LoggerFactory.getLogger(CatalogueLineController.class);

    private CatalogueService service = CatalogueServiceImpl.getInstance();

    @CrossOrigin(origins = {"*"})
    @RequestMapping(value = "/{id}",
            produces = {"application/json"},
            method = RequestMethod.GET)
    public ResponseEntity<CatalogueLineType> getCatalogueLine(@PathVariable String id) {
        log.info("Getting catalogue line with id: {}", id);
        CatalogueLineType catalogueLine = service.getCatalogueLine(id);
        if (catalogueLine == null) {
            return ResponseEntity.status(HttpStatus.NO_CONTENT).body(null);
        }
        log.info("Returning catalogue line with id: {}", id);
        return ResponseEntity.ok(catalogueLine);
    }

    @CrossOrigin(origins = {"*"})
    @RequestMapping(value = "/{catalogueUuid}",
            consumes = {"application/json"},
            produces = {"application/json"},
            method = RequestMethod.POST)
    public ResponseEntity addCatalogueLine(@PathVariable String catalogueUuid, @RequestBody String catalogueLineJson) {
        log.info("Adding catalogue line to catalogue: {}", catalogueUuid);
        log.debug("Adding catalogue line to catalog: {}. Catalogue line: {}", catalogueLineJson);
        CatalogueType catalogue = null;
        CatalogueLineType catalogueLine = null;
        try {
            catalogue = service.getCatalogue(catalogueUuid);
            catalogueLine = new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                    .readValue(catalogueLineJson, CatalogueLineType.class);

            service.addLineToCatalogue(catalogue, catalogueLine);

        } catch (IOException e) {
            log.error("Failed to deserialize catalogue line from json string", e);
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }

        log.info("Added catalogue line to catalogue: {}", catalogueUuid);
        return ResponseEntity.status(HttpStatus.OK).build();
    }

    @CrossOrigin(origins = {"*"})
    @RequestMapping(consumes = {"application/json"},
            produces = {"application/json"},
            method = RequestMethod.PUT)
    public ResponseEntity updateCatalogueLine(@RequestBody String catalogueLineJson) {
        log.info("Updating catalogue line");
        CatalogueLineType catalogueLine = null;
        try {
            catalogueLine = new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                    .readValue(catalogueLineJson, CatalogueLineType.class);
            log.info("Catalogue line id: {}", catalogueLine.getID());
        } catch (IOException e) {
            log.error("Failed to deserialize catalogue line from json string", e);
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }

        service.updateCatalogueLine(catalogueLine);
        log.info("Updated catalogue line: {}", catalogueLine.getID());
        return ResponseEntity.status(HttpStatus.OK).build();
    }

    @CrossOrigin(origins = {"*"})
    @RequestMapping(value = "/{id}",
            produces = {"application/json"},
            method = RequestMethod.DELETE)
    public ResponseEntity deleteCatalogueLineById(@PathVariable String id) {
        log.info("Deleting catalogue line: {}", id);
        service.deleteCatalogueLineById(id);
        log.info("Deleted catalogue line: {}", id);
        return ResponseEntity.status(HttpStatus.OK).build();
    }
}
