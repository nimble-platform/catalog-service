package eu.nimble.service.catalogue.impl;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import eu.nimble.common.rest.identity.IdentityClientTyped;
import eu.nimble.data.transformer.ontmalizer.XML2OWLMapper;
import eu.nimble.service.catalogue.CatalogueService;
import eu.nimble.service.catalogue.CatalogueServiceImpl;
import eu.nimble.service.catalogue.exception.CatalogueServiceException;
import eu.nimble.service.catalogue.persistence.util.CatalogueDatabaseAdapter;
import eu.nimble.service.catalogue.persistence.util.CataloguePersistenceUtil;
import eu.nimble.service.catalogue.persistence.util.PartyTypePersistenceUtil;
import eu.nimble.service.catalogue.sync.MarmottaClient;
import eu.nimble.service.catalogue.sync.MarmottaSynchronizationException;
import eu.nimble.service.catalogue.validation.CatalogueValidator;
import eu.nimble.service.model.modaml.catalogue.TEXCatalogType;
import eu.nimble.service.model.ubl.catalogue.CatalogueType;
import eu.nimble.service.model.ubl.commonaggregatecomponents.PartyType;
import eu.nimble.utility.Configuration;
import eu.nimble.utility.HttpResponseUtil;
import eu.nimble.utility.JAXBUtility;
import eu.nimble.utility.persistence.resource.ResourceValidationUtil;
import eu.nimble.utility.serialization.TransactionEnabledSerializationUtility;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import org.apache.commons.io.IOUtils;
import org.apache.poi.ss.usermodel.Workbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.logging.LogLevel;
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
//@Transactional(transactionManager = "ubldbTransactionManager")
public class CatalogueController {

    private static Logger log = LoggerFactory.getLogger(CatalogueController.class);

    private CatalogueService service = CatalogueServiceImpl.getInstance();

    @Autowired
    private IdentityClientTyped identityClient;
    @Autowired
    private TransactionEnabledSerializationUtility serializationUtility;
    @Autowired
    private ResourceValidationUtil resourceValidationUtil;

    /**
     * Retrieves the default catalogue for the specified party. The catalogue is supposed to have and ID field with
     * "default" value and be compliant with UBL standard.
     *
     * @param partyId
     * @return <li>200 along with the requested catalogue</li>
     * <li>204 if there is no UBL catalogue with "default" as the id value</li>
     */
    @CrossOrigin(origins = {"*"})
    @ApiOperation(value = "", notes = "Retrieve the default catalogue for the specified party")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Retrieved the default catalogue for the specified party successfully", response = CatalogueType.class),
            @ApiResponse(code = 204, message = "No default catalogue for the party"),
            @ApiResponse(code = 500, message = "Failed to get default catalogue for the party")
    })
    @RequestMapping(value = "/catalogue/{partyId}/default",
            produces = {"application/json"},
            method = RequestMethod.GET)
    public ResponseEntity getDefaultCatalogue(@PathVariable String partyId) {
        log.info("Incoming request to get default catalogue for party: {}", partyId);
        // TODO : Check whether the given party id is valid or not.
        CatalogueType catalogue;
        try {
            catalogue = service.getCatalogue("default", partyId);
        } catch (Exception e) {
            return createErrorResponseEntity("Failed to get default catalogue for party id: " + partyId, HttpStatus.INTERNAL_SERVER_ERROR, e);
        }

        if (catalogue == null) {
            log.info("No default catalogue for party: {}", partyId);
            return ResponseEntity.status(HttpStatus.NO_CONTENT).body(String.format("No default catalogue for party: %s", partyId));
        }

        log.info("Completed request to get default catalogue for party: {}", partyId);
        return ResponseEntity.ok(serializationUtility.serializeUBLObject(catalogue));
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
    @ApiOperation(value = "", notes = "Retrieve the catalogue for the given standard and uuid")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Retrieved the catalogue successfully", response = CatalogueType.class),
            @ApiResponse(code = 204, message = "No default catalogue for the given uuid"),
            @ApiResponse(code = 500, message = "Failed to get catalogue for the given standard and uuid"),
            @ApiResponse(code = 400, message = "Invalid standard"),
    })
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
            log.info("No catalogue for uuid: {}", uuid);
            return ResponseEntity.status(HttpStatus.NO_CONTENT).body(String.format("No catalogue for uuid: %s", uuid));
        }

        log.info("Completed request to get catalogue for standard: {}, uuid: {}", standard, uuid);
        return ResponseEntity.ok(serializationUtility.serializeUBLObject(catalogue));
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
    @ApiOperation(value = "", notes = "Add the catalogue passed in a serialized form")
    @ApiResponses(value = {
            @ApiResponse(code = 400, message = "Invalid content type"),
            @ApiResponse(code = 409, message = "A catalogue with the same ID exists for the publisher party")
    })
    @RequestMapping(value = "/catalogue/{standard}",
            consumes = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE},
            produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE},
            method = RequestMethod.POST)
    public <T> ResponseEntity addXMLCatalogue(@PathVariable String standard,
                                              @RequestBody String serializedCatalogue, HttpServletRequest request) {
        try {
            log.info("Incoming request to post catalogue with standard: {} standard", standard);

            // get standard
            Configuration.Standard std;
            try {
                std = getStandardEnum(standard);
            } catch (Exception e) {
                return createErrorResponseEntity("Invalid standard: " + standard, HttpStatus.BAD_REQUEST, e);
            }

            String contentType = request.getContentType();
            // get catalogue
            T catalogue;
            try {
                catalogue = parseCatalogue(contentType, serializedCatalogue, std);
            } catch (Exception e) {
                return createErrorResponseEntity(String.format("Failed to deserialize catalogue: %s", serializedCatalogue), HttpStatus.BAD_REQUEST, e);
            }

            // for ubl catalogues, do the following validations
            if (std.equals(Configuration.Standard.UBL)) {
                CatalogueType ublCatalogue = (CatalogueType) catalogue;

                // validate the content of the catalogue
                CatalogueValidator catalogueValidator = new CatalogueValidator(ublCatalogue);
                List<String> errors = catalogueValidator.validate();
                if (errors.size() > 0) {
                    StringBuilder sb = new StringBuilder("");
                    for (String error : errors) {
                        sb.append(error).append(System.lineSeparator());
                    }
                    return HttpResponseUtil.createResponseEntityAndLog(sb.toString(), null, HttpStatus.BAD_REQUEST, LogLevel.WARN);
                }

                // check catalogue with the same id exists
                boolean catalogueExists = CataloguePersistenceUtil.checkCatalogueExistenceById(ublCatalogue.getID(), ublCatalogue.getProviderParty().getID());
                if (catalogueExists) {
                    return HttpResponseUtil.createResponseEntityAndLog(String.format("Catalogue with ID: '%s' already exists", ublCatalogue.getID()), null, HttpStatus.CONFLICT, LogLevel.INFO);
                }

                // check the entity ids
                boolean hjidsExists = resourceValidationUtil.hjidsExit(catalogue);
                if(hjidsExists) {
                    return HttpResponseUtil.createResponseEntityAndLog(String.format("Entity IDs (hjid fields) found in the passed catalogue: %s. Make sure they are null", serializedCatalogue), null, HttpStatus.BAD_REQUEST, LogLevel.INFO);
                }
            }

            catalogue = service.addCatalogue(catalogue, std);

            return createCreatedCatalogueResponse(catalogue, HttpResponseUtil.baseUrl(request));

        } catch (Exception e) {
            return HttpResponseUtil.createResponseEntityAndLog(String.format("Unexpected error while adding the catalogue: %s", serializedCatalogue), e, HttpStatus.INTERNAL_SERVER_ERROR, LogLevel.ERROR);
        }
    }

    private <T> T parseCatalogue(String contentType, String serializedCatalogue, Configuration.Standard standard) throws IOException {
        T catalogue = null;
        if (contentType.contentEquals(MediaType.APPLICATION_XML_VALUE)) {
            if (standard == Configuration.Standard.UBL) {
                CatalogueType ublCatalogue = (CatalogueType) JAXBUtility.deserialize(serializedCatalogue, Configuration.UBL_CATALOGUE_PACKAGENAME);
                catalogue = (T) ublCatalogue;

            } else if (standard == Configuration.Standard.MODAML) {
                catalogue = (T) JAXBUtility.deserialize(serializedCatalogue, Configuration.MODAML_CATALOGUE_PACKAGENAME);
            }

        } else if (contentType.contentEquals(MediaType.APPLICATION_JSON_VALUE)) {
            catalogue = (T) new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false).readValue(serializedCatalogue, CatalogueType.class);
        }
        return catalogue;
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
        } catch (URISyntaxException e) {
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
        return ResponseEntity.created(catalogueURI).body(serializationUtility.serializeUBLObject(catalogue));
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
    @ApiOperation(value = "", notes = "Update the catalogue represented in JSON serialization")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Updated the catalogue successfully", response = CatalogueType.class),
            @ApiResponse(code = 501, message = "Update operation is not support for the given standard"),
            @ApiResponse(code = 400, message = "Invalid standard"),
            @ApiResponse(code = 500, message = "Failed to update the catalogue")
    })
    @RequestMapping(value = "/catalogue/{standard}",
            consumes = {"application/json"},
            produces = {"application/json"},
            method = RequestMethod.PUT)
    public ResponseEntity updateJSONCatalogue(@PathVariable String standard, @RequestBody String catalogueJson) {
        try {
            log.info("Incoming request to update catalogue");

            // check standard
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

            // parse catalogue
            CatalogueType catalogue;
            try {
                catalogue = new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false).readValue(catalogueJson, CatalogueType.class);
            } catch (IOException e) {
                return createErrorResponseEntity(String.format("Failed to deserialize catalogue: %s", catalogueJson), HttpStatus.INTERNAL_SERVER_ERROR, e);
            }

            // validate the catalogue content
            CatalogueValidator catalogueValidator = new CatalogueValidator(catalogue);
            List<String> errors = catalogueValidator.validate();
            if (errors.size() > 0) {
                StringBuilder sb = new StringBuilder("");
                for (String error : errors) {
                    sb.append(error).append(System.lineSeparator());
                }
                return HttpResponseUtil.createResponseEntityAndLog(sb.toString(), null, HttpStatus.BAD_REQUEST, LogLevel.WARN);
            }

            // validate the entity ids
            boolean hjidsBelongToCompany = resourceValidationUtil.hjidsBelongsToParty(catalogue, catalogue.getProviderParty().getID(), Configuration.Standard.UBL.toString());
            if(!hjidsBelongToCompany) {
                return HttpResponseUtil.createResponseEntityAndLog(String.format("Some of the identifiers (hjid fields) do not belong to the party in the passed catalogue: %s", catalogueJson), null, HttpStatus.BAD_REQUEST, LogLevel.INFO);
            }

            try {
                catalogue = service.updateCatalogue(catalogue);
            } catch (Exception e) {
                log.warn("Failed to update the following catalogue: {}", catalogueJson);
                return createErrorResponseEntity("Failed to update the catalogue", HttpStatus.INTERNAL_SERVER_ERROR, e);
            }

            log.info("Completed request to update the catalogue. uuid: {}", catalogue.getUUID());
            return ResponseEntity.ok(serializationUtility.serializeUBLObject(catalogue));

        } catch (Exception e) {
            return HttpResponseUtil.createResponseEntityAndLog(String.format("Unexpected error while adding the catalogue: %s", catalogueJson), e, HttpStatus.INTERNAL_SERVER_ERROR, LogLevel.ERROR);
        }
    }

    @CrossOrigin(origins = {"*"})
    @ApiOperation(value = "", notes = "Delete the given catalogue")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Deleted the given catalogue successfully"),
            @ApiResponse(code = 400, message = "Invalid standard"),
            @ApiResponse(code = 500, message = "Failed to delete catalogue")
    })
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
    @ApiOperation(value = "", notes = "Generate an excel-based template for the specified categories")
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
    @ApiOperation(value = "", notes = "Add the catalogue specified with the provided template")
    @CrossOrigin(origins = {"*"})
    @RequestMapping(value = "/catalogue/template/upload",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE,
            method = RequestMethod.POST)
    public ResponseEntity uploadTemplate(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "uploadMode", defaultValue = "append") String uploadMode,
            @RequestParam("partyId") String partyId,
            @RequestParam("partyName") String partyName,
            @RequestHeader(value = "Authorization", required = true) String bearerToken,
            HttpServletRequest request) {
        try {
            log.info("Incoming request to upload template upload mode: {}, party id: {}, party name: {}", uploadMode, partyId, partyName);
            CatalogueType catalogue;
            PartyType party = PartyTypePersistenceUtil.getPartyById(partyId);
            if(party == null) {
                party = identityClient.getParty(bearerToken, partyId);
                party = CatalogueDatabaseAdapter.syncPartyInUBLDB(party);
            }

            // parse catalogue
            try {
                catalogue = service.parseCatalogue(file.getInputStream(), uploadMode, party);
            } catch (Exception e) {
                String msg = e.getMessage() != null ? e.getMessage() : "Failed to retrieve the template";
                return HttpResponseUtil.createResponseEntityAndLog(msg, e, HttpStatus.BAD_REQUEST, LogLevel.ERROR);
            }

            // save catalogue
            // check whether an insert or update operations is needed
            if(catalogue.getHjid() == null) {
                catalogue = service.addCatalogue(catalogue, Configuration.Standard.UBL);
            } else {
                catalogue = service.updateCatalogue(catalogue);
            }

            URI catalogueURI;
            try {
                catalogueURI = new URI(HttpResponseUtil.baseUrl(request) + catalogue.getUUID());
            } catch (URISyntaxException e) {
                return createErrorResponseEntity("Failed to generate a URI for the newly created item", HttpStatus.INTERNAL_SERVER_ERROR, e);
            }

            log.info("Completed the request to upload template. Added catalogue uuid: {}", catalogue.getUUID());
            return ResponseEntity.created(catalogueURI).body(serializationUtility.serializeUBLObject(catalogue));

        } catch (Exception e) {
            return HttpResponseUtil.createResponseEntityAndLog("Unexpected error while uploading the template", e, HttpStatus.INTERNAL_SERVER_ERROR, LogLevel.ERROR);
        }
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
    @ApiOperation(value = "", notes = "Add the images provided in the package object to relevant products")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Added the images provided in the package object to relevant products successfully"),
            @ApiResponse(code = 400, message = "Failed obtain a Zip package from the provided data"),
            @ApiResponse(code = 404, message = "Catalogue with the given uuid does not exist")
    })
    @RequestMapping(value = "/catalogue/image/upload",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE,
            method = RequestMethod.POST)
    public ResponseEntity uploadImages(
            @RequestParam("package") MultipartFile pack,
            @RequestParam("catalogueUuid") String catalogueUuid) {
        log.info("Incoming request to upload images for catalogue: {}", catalogueUuid);

        if (service.getCatalogue(catalogueUuid) == null) {
            log.error("Catalogue with uuid : {} does not exist", catalogueUuid);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(String.format("Catalogue with uuid %s does not exist", catalogueUuid));
        }

        ZipInputStream zis = null;
        try {
            zis = new ZipInputStream(pack.getInputStream());
            CatalogueType catalogue = service.addImagesToProducts(zis, catalogueUuid);
            CatalogueValidator catalogueValidator = new CatalogueValidator(catalogue);
            List<String> errors = catalogueValidator.validate();
            if (errors.size() > 0) {
                StringBuilder sb = new StringBuilder("");
                for (String error : errors) {
                    sb.append(error).append(System.lineSeparator());
                }
                return HttpResponseUtil.createResponseEntityAndLog(sb.toString(), null, HttpStatus.BAD_REQUEST, LogLevel.WARN);
            }
            service.updateCatalogue(catalogue);

        } catch (IOException e) {
            return createErrorResponseEntity("Failed obtain a Zip package from the provided data", HttpStatus.BAD_REQUEST, e);
        } catch (CatalogueServiceException e) {
            return createErrorResponseEntity(e.getMessage(), HttpStatus.BAD_REQUEST, e);
        }finally {
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
    @ApiOperation(value = "", notes = "Return the example filled in template")
    @ApiResponses(value = {
            @ApiResponse(code = 500, message = "Failed to write the template content to the response output stream")
    })
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
    @ApiOperation(value = "", notes = "Retrieve the supported standards")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Retrieve the supported standards successfully", response = String.class, responseContainer = "List"),
            @ApiResponse(code = 500, message = "Failed to get supported standards")
    })
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

    /**
     * Returns a catalogue in semantic format
     */
    @CrossOrigin(origins = {"*"})
    @RequestMapping(value = "/catalogue/semantic/{uuid}",
            method = RequestMethod.GET)
    public ResponseEntity getCatalogueInSemanticFormat(@PathVariable String uuid,
                                                       @RequestParam(value = "semanticContentType", required = false) String semanticContentType,
                                                       @RequestHeader("Authorization") String authorization,
                                                       HttpServletResponse response) {
        try {
            log.info("Incoming request to get catalogue in semantic format, uuid: {}, content type: {}", uuid, semanticContentType);
            Object catalogue;
            try {
                catalogue = service.getCatalogue(uuid, Configuration.Standard.UBL);
            } catch (Exception e) {
                return createErrorResponseEntity("Failed to get catalogue for uuid: " + uuid, HttpStatus.INTERNAL_SERVER_ERROR, e);
            }

            if (catalogue == null) {
                log.info("No catalogue for uuid: {}", uuid);
                return ResponseEntity.status(HttpStatus.NO_CONTENT).body(String.format("No default catalogue for uuid: %s", uuid));
            }

            // transform content to other semantic formats
            if (semanticContentType == null) {
                semanticContentType = "RDF/XML";
            }

            MarmottaClient marmottaClient = new MarmottaClient();
            try {
                XML2OWLMapper rdfGenerator = marmottaClient.transformCatalogueToRDF((CatalogueType) catalogue);
                rdfGenerator.writeModel(response.getOutputStream(), semanticContentType);
                response.flushBuffer();
            } catch (IOException | MarmottaSynchronizationException e) {
                return HttpResponseUtil.createResponseEntityAndLog(String.format("Failed to get catalogue with uuid: %s", uuid), e, HttpStatus.INTERNAL_SERVER_ERROR, LogLevel.ERROR);
            }

            log.info("Completed request to get catalogue, uuid: {}", uuid);
            return ResponseEntity.ok().build();

        } catch (Exception e) {
            return HttpResponseUtil.createResponseEntityAndLog(String.format("Unexpected error while getting the catalogue in semantic format. uuid: %s, content-type: %s", uuid, semanticContentType), e, HttpStatus.INTERNAL_SERVER_ERROR, LogLevel.ERROR);
        }
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
