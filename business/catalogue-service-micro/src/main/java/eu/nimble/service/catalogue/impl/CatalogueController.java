package eu.nimble.service.catalogue.impl;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import eu.nimble.service.catalogue.CatalogueService;
import eu.nimble.service.catalogue.client.IdentityClient;
import eu.nimble.service.model.modaml.catalogue.TEXCatalogType;
import eu.nimble.service.model.ubl.catalogue.CatalogueType;
import eu.nimble.service.model.ubl.commonaggregatecomponents.CatalogueLineType;
import eu.nimble.service.model.ubl.commonaggregatecomponents.PartyNameType;
import eu.nimble.service.model.ubl.commonaggregatecomponents.PartyType;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

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

    @CrossOrigin(origins = {"*"})
    @RequestMapping(value = "/catalogue/{partyId}/default",
            produces = {"application/json"},
            method = RequestMethod.GET)
    public ResponseEntity<CatalogueType> getDefaultCatalogue(@PathVariable String partyId) {
        CatalogueType catalogue = service.getCatalogue("default", partyId);
        if (catalogue == null) {
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
    public ResponseEntity<CatalogueType> addUBLCatalogue(@RequestBody String catalogueXML) {

        CatalogueType catalogue = service.addCatalogue(catalogueXML);
        return ResponseEntity.ok(catalogue);
    }

    @RequestMapping(value = "/catalogue/modaml",
            method = RequestMethod.POST)
    public ResponseEntity<Void> addMODAMLCatalogue(@RequestBody String catalogueXML) {
        service.addCatalogue(catalogueXML, Configuration.Standard.MODAML);
        return ResponseEntity.ok(null);
    }

    @CrossOrigin(origins = {"*"})
    @RequestMapping(value = "/catalogue",
            consumes = {"application/json"},
            produces = {"application/json"},
            method = RequestMethod.POST)
    public ResponseEntity addCatalogue(@RequestBody String catalogueJson) {
        log.debug("Submitted catalogue: " + catalogueJson);

        CatalogueType catalogue = null;
        try {
            catalogue = new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false).readValue(catalogueJson, CatalogueType.class);
        } catch (IOException e) {
            log.error("Failed to deserialize catalogue from json string", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }

        service.addCatalogue(catalogue);
        log.info("Request for adding catalogue with uuid: {} completed", catalogue.getUUID());

        URI catalogueURI;
        try {
            Properties prop = new Properties();
            prop.load(CatalogueServiceImpl.class.getClassLoader().getResourceAsStream("application.properties"));
            catalogueURI = new URI(prop.getProperty("catalogue.application.url") + "/" + catalogue.getUUID());
        } catch (URISyntaxException |IOException e) {
            String msg = "Failed to generate a URI for the newly created item";
            log.error(msg, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(msg);
        }
        return ResponseEntity.created(catalogueURI).body(catalogue);
    }

    @CrossOrigin(origins = {"*"})
    @RequestMapping(value = "/catalogue",
            consumes = {"application/json"},
            produces = {"application/json"},
            method = RequestMethod.PUT)
    public ResponseEntity updateCatalogue(@RequestBody String catalogueJson) {
        log.debug("Updated catalogue: " + catalogueJson);

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

    @CrossOrigin(origins = {"*"})
    @RequestMapping(value = "/catalogue/template",
            method = RequestMethod.GET,
            produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    public void downloadTemplate(@RequestParam("taxonomyId") String taxonomyId, @RequestParam("categoryId") String categoryId, HttpServletResponse response) throws IOException {
        Workbook template = service.generateTemplateForCategory(taxonomyId, categoryId);
        String fileName = categoryId + "-catalogue-template.xlsx";
        response.setHeader("Content-disposition","attachment; filename=" + fileName);

        template.write(response.getOutputStream());
        response.flushBuffer();
    }

    @CrossOrigin(origins = {"*"})
    @RequestMapping(value = "/catalogue/template/upload", method = RequestMethod.POST)
    public ResponseEntity uploadTemplate(
            @RequestParam("file") MultipartFile file,
            @RequestParam("companyId") String partyId,
            @RequestParam("companyName") String partyName) {
        try {
            /*PartyType party = identityClient.getParty(partyId);
            log.debug("Fetched party with Id {0}", party.getHjid());*/
            PartyType party = new PartyType();
            List<PartyNameType> names = new ArrayList<>();
            PartyNameType name = new PartyNameType();
            name.setName(partyName);
            names.add(name);
            party.setID(partyId);
            party.setPartyName(names);
            service.addCatalogue(file.getInputStream(), party);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return ResponseEntity.ok().build();
    }

    @RequestMapping(value = "/catalogue/{uuid}",
            produces = {"application/json"},
            method = RequestMethod.DELETE)
    public ResponseEntity deleteCatalogueByUUID(@PathVariable String uuid) {
        log.info("Request for deleting catalogue with uuid: {}", uuid);
        service.deleteCatalogue(uuid);
        log.info("Request processed for deleting catalogue with uuid: {}", uuid);
        return ResponseEntity.status(HttpStatus.OK).build();
    }

    /*
     * Catalogue-line level endpoints
     */

    @CrossOrigin(origins = {"*"})
    @RequestMapping(value = "/catalogue-line/{id}",
            produces = {"application/json"},
            method = RequestMethod.GET)
    public ResponseEntity<CatalogueLineType> getCatalogueLine(@PathVariable String id) {
        CatalogueLineType catalogueLine = service.getCatalogueLine(id);
        if (catalogueLine == null) {
            return ResponseEntity.status(HttpStatus.NO_CONTENT).body(null);
        }
        return ResponseEntity.ok(catalogueLine);
    }

    @CrossOrigin(origins = {"*"})
    @RequestMapping(value = "/catalogue-line/{catalogueUuid}",
            consumes = {"application/json"},
            produces = {"application/json"},
            method = RequestMethod.POST)
    public ResponseEntity addCatalogueLine(@PathVariable String catalogueUuid, @RequestBody String catalogueLineJson) {

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

        return ResponseEntity.status(HttpStatus.OK).build();
    }

    @CrossOrigin(origins = {"*"})
    @RequestMapping(value = "/catalogue-line",
            consumes = {"application/json"},
            produces = {"application/json"},
            method = RequestMethod.PUT)
    public ResponseEntity updateCatalogueLine(@RequestBody String catalogueLineJson) {

        CatalogueLineType catalogueLine = null;
        try {
            System.out.println(catalogueLineJson);
            catalogueLine = new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                    .readValue(catalogueLineJson, CatalogueLineType.class);
        } catch (IOException e) {
            log.error("Failed to deserialize catalogue line from json string", e);
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }

        service.updateCatalogueLine(catalogueLine);

        return ResponseEntity.status(HttpStatus.OK).build();
    }

    @RequestMapping(value = "/catalogue-line/{id}",
            produces = {"application/json"},
            method = RequestMethod.DELETE)
    public ResponseEntity deleteCatalogueLineById(@PathVariable String id) {
        service.deleteCatalogueLineById(id);
        return ResponseEntity.status(HttpStatus.OK).build();
    }
}
