package eu.nimble.service.catalogue.impl;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import eu.nimble.service.catalogue.CatalogueService;
import eu.nimble.service.catalogue.CatalogueServiceImpl;
import eu.nimble.utility.config.CatalogueServiceConfig;
import eu.nimble.service.model.modaml.catalogue.TEXCatalogType;
import eu.nimble.service.model.ubl.catalogue.CatalogueType;
import eu.nimble.service.model.ubl.commonaggregatecomponents.PartyType;
import eu.nimble.utility.Configuration;
import eu.nimble.utility.config.PersistenceConfig;
import org.apache.commons.io.IOUtils;
import org.apache.poi.ss.usermodel.Workbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.List;
import java.util.zip.ZipInputStream;

/**
 * Catalogue level REST services. A catalogue is a collection of products or services on which various business processes
 * can be executed. A catalogue contains contains catalogue lines each of which corresponds to a product or service.
 */
@Controller
@ComponentScan(basePackages = "eu")
public class CatalogueController {

    private static Logger log = LoggerFactory
            .getLogger(CatalogueController.class);

    private CatalogueService service = CatalogueServiceImpl.getInstance();

    @Autowired
    CatalogueServiceConfig conf;

    @Autowired
    PersistenceConfig ublConf;

//    @Autowired
//    private IdentityClient identityClient;

    /**
     * Retrieves the default catalgoue for the specified party. The catalague is supposed to have and ID field with
     * "default" value and be compliant with UBL standard.
     *
     * @param partyId
     * @return <li>200 along with the requested catalogue</li>
     * <li>204 if there is no UBL catalogue with "default" as the id value</li>
     */
    @CrossOrigin(origins = {"*"})
    @RequestMapping(value = "/catalogue/{partyId}/default",
            produces = {"application/json"},
            method = RequestMethod.GET)
    public ResponseEntity getDefaultCatalogue(@PathVariable String partyId) {
        log.info("Incoming request to get default catalogue for party: {}", partyId);
        CatalogueType catalogue;
        try {
            catalogue = service.getCatalogue("default", partyId);
        } catch (Exception e) {
            return createErrorResponseEntity("Failed to get default catalogue for party id: " + partyId, HttpStatus.INTERNAL_SERVER_ERROR, e);
        }

        if (catalogue == null) {
            log.info("No default catalogue for party: {}", partyId);
            return ResponseEntity.status(HttpStatus.NO_CONTENT).body(null);
        }

        log.info("Completed request to get default catalogue for party: {}", partyId);
        return ResponseEntity.ok(catalogue);
    }

    /**
     * Retrieves the catalogue for the given standard and uuid.
     *
     * @param standard
     * @param uuid
     * @return <li>200 along with the requested catalogue</li>
     * <li>204 if there is no catalogue for the specified parameters</li>
     * <li>400 if an invalid standard is provided</li>
     * @see @link getSupportedStandards method for supported standards
     */
    @CrossOrigin(origins = {"*"})
    @RequestMapping(value = "/catalogue/{standard}/{uuid}",
            produces = {"application/json"},
            method = RequestMethod.GET)
    public ResponseEntity getCatalogue(@PathVariable String standard, @PathVariable String uuid) {
        log.info("Incoming request to get catalogue for standard: {}, uuid: {}", standard, uuid);
        Configuration.Standard std;
        try {
            std = getStandardEnum(standard);
        } catch (Exception e) {
            return createErrorResponseEntity("Invalid standard: " + standard, HttpStatus.BAD_REQUEST, e);
        }
        Object catalogue;
        try {
            catalogue = service.getCatalogue(uuid, std);
        } catch (Exception e) {
            return createErrorResponseEntity("Failed to get catalogue for standard: " + standard + " uuid: " + uuid, HttpStatus.INTERNAL_SERVER_ERROR, e);
        }

        if (catalogue == null) {
            log.info("No default catalogue for uuid: {}", uuid);
            return ResponseEntity.status(HttpStatus.NO_CONTENT).body(null);
        }

        log.info("Completed request to get catalogue for standard: {}, uuid: {}", standard, uuid);
        return ResponseEntity.ok(catalogue);
    }

    /**
     * Adds the catalogue passed in a serialized form. The serialized catalogue should be compliant with the specified
     * standard
     *
     * @param standard
     * @param serializedCatalogue
     * @return <li>200 along with the requested catalogue</li>
     * <li>400 if an invalid content type header or standard is provided</li>
     */
    @CrossOrigin(origins = {"*"})
    @RequestMapping(value = "/catalogue/{standard}",
            consumes = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE},
            produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE},
            method = RequestMethod.POST)
    public ResponseEntity addXMLCatalogue(@PathVariable String standard,
                                          @RequestBody String serializedCatalogue, HttpServletRequest request) {
        log.info("Incoming request to post catalogue with standard: {} standard", standard);
        log.debug("Catalogue content: {}", serializedCatalogue);

        String contentType = request.getContentType();
        if (contentType.contentEquals(MediaType.APPLICATION_JSON_VALUE)) {
            return addJSONCatalogue(serializedCatalogue, request);
        } else if (contentType.contentEquals(MediaType.APPLICATION_XML_VALUE)) {
            return addXMLCatalogue(serializedCatalogue, standard, Utils.baseUrl(request));
        } else {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Invalid content type: " + contentType);
        }
    }

    private ResponseEntity addXMLCatalogue(String catalogueXML, String standard, String baseUrl) {
        Configuration.Standard std;
        try {
            std = getStandardEnum(standard);
        } catch (Exception e) {
            return createErrorResponseEntity("Invalid standard: " + standard, HttpStatus.BAD_REQUEST, e);
        }

        Object catalogue;
        try {
            catalogue = service.addCatalogue(catalogueXML, std);
        } catch (Exception e) {
            return createErrorResponseEntity("Failed to post catalogue for standard: " + standard, HttpStatus.INTERNAL_SERVER_ERROR, e);
        }

        return createCreatedCatalogueResponse(catalogue, baseUrl);
    }

    private ResponseEntity addJSONCatalogue(@RequestBody String catalogueJson, HttpServletRequest request) {
        log.info("Incoming request to post catalogue");
        log.debug("Catalogue content: {}", catalogueJson);

        CatalogueType catalogue;
        try {
            catalogue = new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false).readValue(catalogueJson, CatalogueType.class);
        } catch (IOException e) {
            return createErrorResponseEntity("Failed to deserialize catalogue from json string", HttpStatus.BAD_REQUEST, e);
        }

        try {
            catalogue = service.addCatalogue(catalogue);
        } catch (Exception e) {
            return createErrorResponseEntity("Failed to add the catalogue", HttpStatus.INTERNAL_SERVER_ERROR, e);
        }
        log.info("Catalogue add with uuid: {} completed", catalogue.getUUID());

        return createCreatedCatalogueResponse(catalogue, Utils.baseUrl(request));
    }

    private ResponseEntity createCreatedCatalogueResponse(Object catalogue, String baseUrl) {
        String uuid;
        if (catalogue instanceof CatalogueType) {
            uuid = ((CatalogueType) catalogue).getUUID().toString();
        } else {
            uuid = ((TEXCatalogType) catalogue).getTCheader().getMsgID();
        }
        URI catalogueURI;
        try {

            catalogueURI = new URI(baseUrl + uuid);
        } catch (URISyntaxException  e) {
            String msg = "Failed to generate a URI for the newly created item";
            log.warn(msg, e);
            try {
                log.info("Completed request to add catalogue with an empty URI, uuid: {}", uuid);
                return ResponseEntity.created(new URI("")).body(catalogue);
            } catch (URISyntaxException e1) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("WTF");
            }
        }
        log.info("Completed request to add catalogue, uuid: {}", uuid);
        return ResponseEntity.created(catalogueURI).body(catalogue);
    }

    /**
     * Updates the catalogue represented in JSON serialization. The serialization should be compliant with the default standard, which is UBL
     *
     * @param catalogueJson
     * @return <li>200 along with the updated catalogue</li>
     * <li>400 in case of an invalid standard or invalid catalogue serialization</li>
     * <li>501 if a standard than ubl is passed</li>
     */
    @CrossOrigin(origins = {"*"})
    @RequestMapping(value = "/catalogue/{standard}",
            consumes = {"application/json"},
            produces = {"application/json"},
            method = RequestMethod.PUT)
    public ResponseEntity updateJSONCatalogue(@PathVariable String standard, @RequestBody String catalogueJson) {
        log.info("Incoming request to update catalogue");
        log.debug("Catalogue data: " + catalogueJson);

        Configuration.Standard std;
        try {
            std = getStandardEnum(standard);
            if (std != Configuration.Standard.UBL) {
                String msg = "Update operation is not support for " + standard;
                log.info(msg);
                return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).body(msg);
            }
        } catch (Exception e) {
            return createErrorResponseEntity("Invalid standard: " + standard, HttpStatus.BAD_REQUEST, e);
        }

        CatalogueType catalogue;
        try {
            catalogue = new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false).readValue(catalogueJson, CatalogueType.class);
        } catch (IOException e) {
            return createErrorResponseEntity("Failed to deserialize catalogue from json string", HttpStatus.INTERNAL_SERVER_ERROR, e);
        }

        try {
            service.updateCatalogue(catalogue);
        } catch (Exception e) {
            return createErrorResponseEntity("Failed to update the catalogue", HttpStatus.INTERNAL_SERVER_ERROR, e);
        }

        log.info("Completed request to update the catalogue. uuid: {}", catalogue.getUUID());
        return ResponseEntity.ok(catalogue);
    }

    @CrossOrigin(origins = {"*"})
    @RequestMapping(value = "/catalogue/{standard}/{uuid}",
            method = RequestMethod.DELETE)
    public ResponseEntity deleteCatalogue(@PathVariable String standard, @PathVariable String uuid) {
        log.info("Incoming request to delete catalogue with uuid: {}", uuid);

        Configuration.Standard std;
        try {
            std = getStandardEnum(standard);
        } catch (Exception e) {
            return createErrorResponseEntity("Invalid standard: " + standard, HttpStatus.BAD_REQUEST, e);
        }

        try {
            service.deleteCatalogue(uuid, std);
        } catch (Exception e) {
            return createErrorResponseEntity("Failed to delete catalogue", HttpStatus.INTERNAL_SERVER_ERROR, e);
        }

        log.info("Completed request to delete catalogue with uuid: {}", uuid);
        return ResponseEntity.status(HttpStatus.OK).build();
    }

    /**
     * Generates an excel-based template for the specified categories. Category ids and taxonomy ids must be provided in
     * comma separated manner and they must have the same number of elements. Taxonomy id must be provided such that
     * they specify the taxonomies including the specified categories. See the examples in parameter definitions.
     *
     * @param categoryIds Example category ids: http://www.aidimme.es/FurnitureSectorOntology.owl#MDFBoard,0173-1#01-ACH237#011
     * @param taxonomyIds Example taxonomy ids: FurnitureOntology,eClass
     * @param response
     */
    @CrossOrigin(origins = {"*"})
    @RequestMapping(value = "/catalogue/template",
            method = RequestMethod.GET,
            produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    public void downloadTemplate(
            @RequestParam("categoryIds") List<String> categoryIds,
            @RequestParam("taxonomyIds") List<String> taxonomyIds,
            HttpServletResponse response) {
        log.info("Incoming request to generate a template. Category ids: {}, taxonomy ids: {}", categoryIds, taxonomyIds);

        Workbook template;
        try {
            template = service.generateTemplateForCategory(categoryIds, taxonomyIds);
        } catch (Exception e) {
            String msg = "Failed to generate template\n" + e.getMessage();
            log.error(msg, e);
            response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
            try {
                response.getOutputStream().write(msg.getBytes());
            } catch (IOException e1) {
                log.error("Failed to write the error message to the output stream", e);
            }
            return;
        }

        try {
            String fileName = "product_data_template.xlsx";
            response.setHeader("Content-disposition", "attachment; filename=" + fileName);
            response.addHeader("Access-Control-Expose-Headers", "Content-Disposition");
            template.write(response.getOutputStream());
            response.flushBuffer();
            log.info("Completed the request to generate template");
        } catch (IOException e) {
            String msg = "Failed to write the template content to the response output stream\n" + e.getMessage();
            log.error(msg, e);
            response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
            try {
                response.getOutputStream().write(msg.getBytes());
            } catch (IOException e1) {
                log.error("Failed to write the error message to the output stream", e);
            }
        }
    }

    /**
     * Adds the catalogue specified with the provided template. The created catalogue is compliant with the default
     * standard, which is UBL. If there is a published catalogue already, the type of update is realized according to
     * the update mode. There are two update modes: append and replace. In the former mode, if some of the products were
     * already published, they are replaced with the new ones; furthermore, the brand new ones are appended to the
     * existing product list. In the latter mode, all previously published products are deleted, the new list of products is set as it is.
     *
     * @param file       Filled in excel-based template
     * @param uploadMode Upload mode. Default value is "append"
     * @param partyId    Identifier of the party submitting the template
     * @param partyName  Name of the party submitting the template
     * @return 200 along with the added catalogue
     * @see @link downloadTemplate method to download an empty template
     */
    @CrossOrigin(origins = {"*"})
    @RequestMapping(value = "/catalogue/template/upload",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE,
            method = RequestMethod.POST)
    public ResponseEntity uploadTemplate(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "uploadMode", defaultValue = "append") String uploadMode,
            @RequestParam("partyId") String partyId,
            @RequestParam("partyName") String partyName,
            HttpServletRequest request) {
        log.info("Incoming request to upload template upload mode: {}, party id: {}, party name: {}", uploadMode, partyId, partyName);
        CatalogueType catalogue;
        try {
            //TODO retrieve the party from the identity service
            /*PartyType party = identityClient.getParty(partyId);
            log.debug("Fetched party with Id {0}", party.getHjid());*/
            PartyType party = new PartyType();
            party.setName(partyName);
            party.setID(partyId);
            catalogue = service.addCatalogue(file.getInputStream(), uploadMode, party);
        } catch (IOException e) {
            return createErrorResponseEntity("Failed to retrieve the template", HttpStatus.INTERNAL_SERVER_ERROR, e);
        }

        URI catalogueURI;
        try {
            catalogueURI = new URI(Utils.baseUrl(request) + catalogue.getUUID());
        } catch (URISyntaxException e) {
            return createErrorResponseEntity("Failed to generate a URI for the newly created item", HttpStatus.INTERNAL_SERVER_ERROR, e);
        }

        log.info("Completed the request to upload template. Added catalogue uuid: {}", catalogue.getUUID());
        return ResponseEntity.created(catalogueURI).body(catalogue);
    }

    /**
     * Adds the images provided in the {@code pack} package object to relevant products. Each file in the package must start
     * with the manufacturer item identification of the product for which the image is provided. Otherwise, the image
     * would be ignored.
     *
     * @param pack          The package compressed as a Zip file, including the images
     * @param catalogueUuid Unique identifier of the catalogue including the products for which the images are provided
     * @return 200 along with the added catalogue
     */
    @CrossOrigin(origins = {"*"})
    @RequestMapping(value = "/catalogue/image/upload",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE,
            method = RequestMethod.POST)
    public ResponseEntity uploadImages(
            @RequestParam("package") MultipartFile pack,
            @RequestParam("catalogueUuid") String catalogueUuid) {
        log.info("Incoming request to upload images for catalogue: {}", catalogueUuid);

        ZipInputStream zis = null;
        try {
            zis = new ZipInputStream(pack.getInputStream());
            service.addImagesToProducts(zis, catalogueUuid);

        } catch (IOException e) {
            return createErrorResponseEntity("Failed obtain a Zip package from the provided data", HttpStatus.BAD_REQUEST, e);
        } finally {
            try {
                zis.close();
            } catch (IOException e) {
                log.warn("Failed to close Zip stream", e);
            }
        }

        log.info("Completed the request to upload images for catalogue: {}", catalogueUuid);
        return ResponseEntity.ok().build();
    }

    /**
     * Returns the example filled in template
     *
     * @return 200 along with the added catalogue
     */
    @CrossOrigin(origins = {"*"})
    @RequestMapping(value = "/catalogue/template/example",
            method = RequestMethod.GET,
            produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    public void downloadExampleTemplate(HttpServletResponse response) {
        log.info("Incoming request to get the example filled in template");

        InputStream is = CatalogueController.class.getResourceAsStream("/template/wooden_mallet_template.xlsx");

        try {
            String fileName = "wooden_mallet_template.xlsx";
            response.setHeader("Content-disposition", "attachment; filename=" + fileName);
            response.addHeader("Access-Control-Expose-Headers", "Content-Disposition");
            IOUtils.copy(is, response.getOutputStream());
            response.flushBuffer();
            is.close();
            log.info("Completed the request to get the example template");

        } catch (IOException e) {
            String msg = "Failed to write the template content to the response output stream\n" + e.getMessage();
            log.error(msg, e);
            response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
            try {
                response.getOutputStream().write(msg.getBytes());
            } catch (IOException e1) {
                log.error("Failed to write the error message to the output stream", e);
            }
        }
    }

    /**
     * Returns the supported standards
     *
     * @return
     */
    @CrossOrigin(origins = {"*"})
    @RequestMapping(value = "/catalogue/standards",
            produces = {"application/json"},
            method = RequestMethod.GET)
    public ResponseEntity getSupportedStandards() {
        log.info("Incoming request to retrieve the supported standards");

        List<Configuration.Standard> standards;
        try {
            standards = Arrays.asList(Configuration.Standard.values());
        } catch (Exception e) {
            return createErrorResponseEntity("Failed to get supported standards", HttpStatus.INTERNAL_SERVER_ERROR, e);
        }

        log.info("Completed request to retrieve the supported standards");
        return ResponseEntity.ok(standards);
    }

    private ResponseEntity createErrorResponseEntity(String msg, HttpStatus status, Exception e) {
        msg = msg + e.getMessage();
        log.error(msg, e);
        return ResponseEntity.status(status).body(msg);
    }

    private Configuration.Standard getStandardEnum(String standard) {
        standard = standard.toUpperCase();
        Configuration.Standard std = Configuration.Standard.valueOf(standard);
        return std;
    }
}
