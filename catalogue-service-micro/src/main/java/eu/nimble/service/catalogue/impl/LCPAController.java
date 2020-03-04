package eu.nimble.service.catalogue.impl;

import eu.nimble.service.catalogue.config.RoleConfig;
import eu.nimble.service.catalogue.model.lcpa.ItemLCPAInput;
import eu.nimble.service.catalogue.persistence.util.CatalogueLinePersistenceUtil;
import eu.nimble.service.catalogue.util.ExecutionContext;
import eu.nimble.service.model.ubl.commonaggregatecomponents.CatalogueLineType;
import eu.nimble.service.model.ubl.commonaggregatecomponents.LCPAOutputType;
import eu.nimble.utility.JsonSerializationUtility;
import eu.nimble.utility.exception.NimbleException;
import eu.nimble.utility.exception.NimbleExceptionMessageCode;
import eu.nimble.utility.persistence.GenericJPARepository;
import eu.nimble.utility.persistence.JPARepositoryFactory;
import eu.nimble.utility.validation.IValidationUtil;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.List;

/**
 * Created by suat on 27-Mar-19.
 */
@Controller
@RequestMapping(value = "/lcpa")
public class LCPAController {
    private static Logger logger = LoggerFactory.getLogger(PriceConfigurationController.class);

    @Autowired
    private IValidationUtil validationUtil;
    @Autowired
    private ExecutionContext executionContext;

    @CrossOrigin(origins = {"*"})
    @ApiOperation(value = "", notes = "Returns the catalogue uuid/catalogue line information along with the corresponding " +
            "LCPA Details")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Returned products with valid LCPA input", response = ItemLCPAInput.class, responseContainer = "List"),
            @ApiResponse(code = 401, message = "Invalid token. "),
            @ApiResponse(code = 500, message = "Unexpected error")
    })
    @RequestMapping(value = "/products-with-lcpa-input",
            produces = {MediaType.APPLICATION_JSON_VALUE},
            method = RequestMethod.GET)
    public ResponseEntity getProductsWithoutLCPAProcessing(@ApiParam(value = "The Bearer token provided by the identity service", required = true) @RequestHeader(value = "Authorization") String bearerToken) {
        try {
            // set request log of ExecutionContext
            String requestLog = "Incoming request to get product with LCPA input but not output";
            executionContext.setRequestLog(requestLog);

            logger.info(requestLog);
            // validate role
            if(!validationUtil.validateRole(bearerToken, RoleConfig.REQUIRED_ROLES_CATALOGUE)) {
                throw new NimbleException(NimbleExceptionMessageCode.UNAUTHORIZED_INVALID_ROLE.toString());
            }

            List<ItemLCPAInput> results = CatalogueLinePersistenceUtil.getLinesIdsWithValidLcpaInput();

            logger.info("Completed request to get product with LCPA input but not output");
            return ResponseEntity.ok(results);

        } catch (Exception e) {
            throw new NimbleException(NimbleExceptionMessageCode.INTERNAL_SERVER_ERROR_GET_PRODUCTS_WITHOUT_LCPA_PROCESSING.toString(),e);
        }
    }

    @CrossOrigin(origins = {"*"})
    @ApiOperation(value = "", notes = "Adds the provided LCPAOutput data the specified CatalogueLine")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Added the LCPAOutput successfully.", response = CatalogueLineType.class),
            @ApiResponse(code = 400, message = "Could not parse the provided LCPAOutput"),
            @ApiResponse(code = 401, message = "Invalid token."),
            @ApiResponse(code = 404, message = "No CatalogueLine found "),
            @ApiResponse(code = 500, message = "Unexpected error")
    })
    @RequestMapping(value = "/add-lcpa-output",
            consumes = {MediaType.APPLICATION_JSON_VALUE},
            produces = {MediaType.APPLICATION_JSON_VALUE},
            method = RequestMethod.PATCH)
    public ResponseEntity addLCPAOutputData(
            @ApiParam(value = "Hjid of the CatalogueLine to be updated", required = true) @RequestParam("catalogueLineHjid") Long catalogueLineHjid,
            @ApiParam(value = "JSON serialization of the LCPAOutput", required = true) @RequestBody String lcpaOutputJson,
            @ApiParam(value = "The Bearer token provided by the identity service", required = true) @RequestHeader(value = "Authorization") String bearerToken) {
        try {
            // set request log of ExecutionContext
            String requestLog = String.format("Incoming request to update LCPAOutput for catalogue line with hjid: %s", catalogueLineHjid);
            executionContext.setRequestLog(requestLog);

            logger.info(requestLog);
            // validate role
            if(!validationUtil.validateRole(bearerToken, RoleConfig.REQUIRED_ROLES_CATALOGUE)) {
                throw new NimbleException(NimbleExceptionMessageCode.UNAUTHORIZED_INVALID_ROLE.toString());
            }

            GenericJPARepository repo = new JPARepositoryFactory().forCatalogueRepository(true);
            CatalogueLineType catalogueLine = repo.getSingleEntityByHjid(CatalogueLineType.class, catalogueLineHjid);
            if (catalogueLine == null) {
                throw new NimbleException(NimbleExceptionMessageCode.NOT_FOUND_NO_CATALOGUE_LINE_FOR_HJID.toString(), Arrays.asList(catalogueLineHjid.toString()));
            }

            // remove hjid fields of LCPAOutput
            JSONObject object = new JSONObject(lcpaOutputJson);
            JsonSerializationUtility.removeHjidFields(object);
            LCPAOutputType lcpaOutput;
            try {
                lcpaOutput = JsonSerializationUtility.getObjectMapper().readValue(object.toString(), LCPAOutputType.class);
            } catch (IOException e) {
                throw new NimbleException(NimbleExceptionMessageCode.BAD_REQUEST_PARSE_LCPA_OUTPUT.toString(),Arrays.asList(catalogueLineHjid.toString(), lcpaOutputJson),e);
            }
            catalogueLine.getGoodsItem().getItem().getLifeCyclePerformanceAssessmentDetails().setLCPAOutput(lcpaOutput);
            catalogueLine = repo.updateEntity(catalogueLine);

            logger.info("Completed request to update LCPAOutput for catalogue line with hjid: {}", catalogueLineHjid);
            return ResponseEntity.ok(catalogueLine);

        } catch (Exception e) {
            throw new NimbleException(NimbleExceptionMessageCode.INTERNAL_SERVER_ERROR_ADD_LCPA_OUTPUT.toString(),Arrays.asList(catalogueLineHjid.toString(), lcpaOutputJson),e);
        }
    }

    @CrossOrigin(origins = {"*"})
    @ApiOperation(value = "", notes = "Downloads Bill of Material template")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Downloaded BOM template successfully"),
            @ApiResponse(code = 401, message = "Invalid token. No user was found for the provided token"),
            @ApiResponse(code = 500, message = "Unexpected error while downloading BOM template"),
    })
    @RequestMapping(value = "/bom-template",
            method = RequestMethod.GET,
            produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    public void downloadBOMTemplate(
            @ApiParam(value = "The Bearer token provided by the identity service", required = true) @RequestHeader(value = "Authorization", required = true) String bearerToken,
            HttpServletResponse response) {
        // set request log of ExecutionContext
        String requestLog = "Incoming request to download BOM template";
        executionContext.setRequestLog(requestLog);

        logger.info(requestLog);
        // validate role
        if(!validationUtil.validateRole(bearerToken, RoleConfig.REQUIRED_ROLES_CATALOGUE)) {
            throw new NimbleException(NimbleExceptionMessageCode.UNAUTHORIZED_INVALID_ROLE.toString());
        }

        try {
            InputStream file = LCPAController.class.getResourceAsStream("/LCA-BOM-Template.xlsx");
            String fileName = "BillOfMaterialTemplate.xlsx";
            response.setHeader("Content-disposition", "attachment; filename=" + fileName);
            response.addHeader("Access-Control-Expose-Headers", "Content-Disposition");

            byte[] buffer = new byte[10240];

            OutputStream output = response.getOutputStream();
            for (int length = 0; (length = file.read(buffer)) > 0;) {
                output.write(buffer, 0, length);
            }

            response.flushBuffer();
            logger.info("Completed the request to download BOM template");
        } catch (Exception e) {
            throw new NimbleException(NimbleExceptionMessageCode.INTERNAL_SERVER_ERROR_DOWNLOAD_BOM_TEMPLATE.toString(),true);
        }
    }
}
