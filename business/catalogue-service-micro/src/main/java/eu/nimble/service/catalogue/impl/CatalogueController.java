package eu.nimble.service.catalogue.impl;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import eu.nimble.service.catalogue.CatalogueService;
import eu.nimble.service.catalogue.client.IdentityClient;
import eu.nimble.service.model.modaml.catalogue.TEXCatalogType;
import eu.nimble.service.model.ubl.catalogue.CatalogueType;
import eu.nimble.service.model.ubl.commonaggregatecomponents.GoodsItemType;
import eu.nimble.service.model.ubl.commonaggregatecomponents.PartyNameType;
import eu.nimble.service.model.ubl.commonaggregatecomponents.PartyType;
import eu.nimble.service.model.ubl.commonbasiccomponents.IdentifierType;
import eu.nimble.utility.Configuration;
import org.apache.poi.ss.usermodel.Workbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

@Controller
public class CatalogueController {

    private static Logger log = LoggerFactory
            .getLogger(CatalogueController.class);

    private CatalogueService service = CatalogueServiceImpl.getInstance();

    @Autowired
    private IdentityClient identityClient;

    @RequestMapping(value = "/catalogue/{uuid}",
            produces = {"application/json"},
            method = RequestMethod.GET)
    public ResponseEntity<CatalogueType> getUBLCatalogueByUUID(@PathVariable String uuid) {
        CatalogueType catalogue = (CatalogueType) service.getCatalogue(uuid);
        return ResponseEntity.ok(catalogue);
    }

    @CrossOrigin(origins = {"${catalogue.cross.origins}"})
    @RequestMapping(value = "/catalogue/{partyId}/default",
            produces = {"application/json"},
            method = RequestMethod.GET)
    public ResponseEntity<CatalogueType> getDefaultCatalogue(@PathVariable String partyId) {
        CatalogueType catalogue = service.getCatalogue("default", partyId);
        if(catalogue == null) {
            return ResponseEntity.status(HttpStatus.NO_CONTENT).body(null);
        }
        return ResponseEntity.ok(catalogue);
    }

    @RequestMapping(value = "/catalogue/modaml/{uuid}",
            produces = {"application/json"},
            method = RequestMethod.GET)
    public ResponseEntity<TEXCatalogType> getMODAMLCatalogueByUUID(@PathVariable String uuid) {
        TEXCatalogType catalogue = service.getCatalogue(uuid, Configuration.Standard.MODAML);
        return ResponseEntity.ok(catalogue);
    }

    @RequestMapping(value = "/catalogue/ubl",
            method = RequestMethod.POST)
    public ResponseEntity<CatalogueType> addUBLCatalogue(@RequestBody String catalogueXML, @RequestParam String partyId) {

        PartyType party = identityClient.getParty(partyId);
        log.debug("Fetched party with Id {0}", party.getHjid());

        CatalogueType catalogue = service.addCatalogue(catalogueXML, party);
        return ResponseEntity.ok(catalogue);
    }

    @RequestMapping(value = "/catalogue/modaml",
            method = RequestMethod.POST)
    public ResponseEntity<Void> addMODAMLCatalogue(@RequestBody String catalogueXML, @RequestParam String partyId) {

        PartyType party = identityClient.getParty(partyId);
        log.debug("Fetched party with Id {0}", party.getHjid());

        service.addCatalogue(catalogueXML, party, Configuration.Standard.MODAML);
        return ResponseEntity.ok(null);
    }

    @CrossOrigin(origins = {"${catalogue.cross.origins}"})
    @RequestMapping(value = "/catalogue",
            consumes = {"application/json"},
            produces = {"application/json"},
            method = RequestMethod.POST)
    public ResponseEntity submitCatalogue(@RequestBody String catalogueJson, @RequestParam("partyId") String partyId) {
        log.debug("Catalogue submitting party id: {}", partyId);
        log.debug("Submitted catalogue: " + catalogueJson);

        CatalogueType catalogue = null;
        try {
            catalogue = new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false).readValue(catalogueJson, CatalogueType.class);
        } catch (IOException e) {
            log.error("Failed to deserialize catalogue from json string", e);
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }

        //TODO fetch party with a given user id
        //PartyType party = identityClient.getParty(partyId);
        //log.debug("Fetched party with Id {0}", party.getHjid());
        PartyType dummyParty = new PartyType();
        IdentifierType id = new IdentifierType();
        id.setValue("pid");
        dummyParty.setID(id);
        PartyNameType name = new PartyNameType();
        name.setName("Dummy Party");
        dummyParty.setPartyName(name);

        service.addCatalogue(catalogue, dummyParty);

        URI catalogueURI;
        try {
            // TODO make the url below configurable
            catalogueURI = new URI("${catalogue.application.url}/catalogue/" + catalogue.getUUID().getValue());
        } catch (URISyntaxException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to generate a URI for the newly created item");
        }
        return ResponseEntity.created(catalogueURI).body(catalogue);
    }

    @CrossOrigin(origins = {"${catalogue.cross.origins}"})
    @RequestMapping(value = "/catalogue/{uuid]",
            consumes = {"application/json"},
            produces = {"application/json"},
            method = RequestMethod.PUT)
    public ResponseEntity updateCatalogue(@RequestBody String catalogueJson, @PathVariable("uuid") String string) {
        log.debug("Submitted catalogue: " + catalogueJson);

        CatalogueType catalogue = null;
        try {
            catalogue = new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false).readValue(catalogueJson, CatalogueType.class);
        } catch (IOException e) {
            log.error("Failed to deserialize catalogue from json string", e);
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }

        service.updateCatalogue(catalogue);

        return ResponseEntity.status(HttpStatus.OK).build();
    }

    @CrossOrigin(origins = {"${catalogue.cross.origins}"})
    @RequestMapping(value = "/catalogue/template",
            method = RequestMethod.GET,
            produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    public void downloadTemplate(@RequestParam String categoryId, HttpServletResponse response) throws IOException {
        Workbook template = service.generateTemplateForCategory(categoryId);

        FileOutputStream fileOut = null;
        try {
            fileOut = new FileOutputStream(categoryId + "-catalogue-template.xlsx");
            template.write(fileOut);
            fileOut.close();
        } catch (java.io.IOException e) {
            log.error("Failed to create template for category: " + categoryId, e);
        }

        template.write(response.getOutputStream());
        response.flushBuffer();
    }

    @CrossOrigin(origins = {"${catalogue.cross.origins}"})
    @RequestMapping(value = "/catalogue/template/upload", method = RequestMethod.POST)
    public ResponseEntity uploadTemplate(
            @RequestParam("file") MultipartFile file,
            @RequestParam("companyId") String partyId) {
        try {
            PartyType party = identityClient.getParty(partyId);
            log.debug("Fetched party with Id {0}", party.getHjid());

            service.addCatalogue(file.getInputStream(), party);

        } catch (IOException e) {
            e.printStackTrace();
        }
        return ResponseEntity.ok().build();
    }
}
