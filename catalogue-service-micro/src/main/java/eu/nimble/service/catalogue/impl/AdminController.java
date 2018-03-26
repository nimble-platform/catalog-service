package eu.nimble.service.catalogue.impl;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import eu.nimble.data.transformer.ontmalizer.XML2OWLMapper;
import eu.nimble.service.catalogue.sync.MarmottaClient;
import eu.nimble.service.catalogue.sync.MarmottaSynchronizationException;
import eu.nimble.service.model.ubl.catalogue.CatalogueType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * Created by suat on 02-Mar-18.
 */
@Controller
public class AdminController {
    private static Logger log = LoggerFactory.getLogger(AdminController.class);

    @CrossOrigin(origins = {"*"})
    @RequestMapping(value = "/admin/catalogue/{standard}",
            consumes = {MediaType.APPLICATION_JSON_VALUE},
            produces = {MediaType.APPLICATION_XML_VALUE, "application/rdf+xml"},
            method = RequestMethod.POST)
    public ResponseEntity transformCatalogue(@PathVariable String standard, @RequestBody String serializedCatalogue, HttpServletRequest request, HttpServletResponse response) {
        log.info("Incoming request for transforming catalogue");

        String contentType = request.getContentType();
        CatalogueType catalogue;
        try {
            catalogue = new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false).readValue(serializedCatalogue, CatalogueType.class);
        } catch (IOException e) {
            log.error("Failed to parse the catalogue: {}", serializedCatalogue, e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Failed to parse the catalogue");
        }

        MarmottaClient marmottaClient = new MarmottaClient();
        XML2OWLMapper rdfGenerator;
        try {
            rdfGenerator = marmottaClient.transformCatalogueToRDF(catalogue);
        } catch (MarmottaSynchronizationException e) {
            log.error("Failed to transform the catalogue to RDF: {}", serializedCatalogue, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to transform the catalogue to RDF");
        }

        try {
            rdfGenerator.writeModel(response.getOutputStream(), "RDF/XML");
        } catch (IOException e) {
            log.error("Failed to write the  RDF transformation to response output stream: {}", serializedCatalogue, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to write the  RDF transformation to response output stream");
        }

        log.info("Catalogue transformed to XML");
        return ResponseEntity.status(HttpStatus.OK).build();
    }
}
