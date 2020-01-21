package eu.nimble.service.catalogue.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import eu.nimble.service.model.ubl.catalogue.CatalogueType;
import eu.nimble.service.model.ubl.commonbasiccomponents.BinaryObjectType;
import eu.nimble.utility.JsonSerializationUtility;
import eu.nimble.utility.exception.NimbleException;
import eu.nimble.utility.exception.NimbleExceptionMessageCode;
import eu.nimble.utility.persistence.binary.BinaryContentService;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Controller
public class BinaryContentController {

    private static Logger logger = LoggerFactory.getLogger(BinaryContentController.class);

    @CrossOrigin(origins = {"*"})
    @ApiOperation(value = "", notes = "Retrieves a specified binary content wrapped inside a BinaryCbjectType instance.")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Retrieved the binary content successfully", response = CatalogueType.class),
            @ApiResponse(code = 401, message = "Invalid token. No user was found for the provided token"),
            @ApiResponse(code = 404, message = "No binary content exists for the specified uri"),
            @ApiResponse(code = 500, message = "Unexpected error while getting binary content"),
    })
    @RequestMapping(value = "/binary-content",
            method = RequestMethod.GET,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity getBinaryContent(@ApiParam(value = "Uri of the binary content to be retrieved", required = true) @RequestParam(value = "uri") String uri,
                                           @ApiParam(value = "The Bearer token provided by the identity service", required = true) @RequestHeader(value = "Authorization") String bearerToken) {
        try {
            logger.info("Request to retrieve binary content for uri: {}", uri);
            // check token
            eu.nimble.service.catalogue.util.HttpResponseUtil.checkToken(bearerToken);

            BinaryObjectType result = new BinaryContentService().retrieveContent(uri);
            // check whether the binary content exists or not
            if(result == null){
                throw new NimbleException(NimbleExceptionMessageCode.NOT_FOUND_NO_BINARY_CONTENT.toString(), Arrays.asList(uri));
            }

            ObjectMapper objectMapper = JsonSerializationUtility.getObjectMapper();
            String response = objectMapper.writeValueAsString(result);

            logger.info("Completed request to retrieve binary content for uri: {}", uri);
            return ResponseEntity.ok().body(response);

        } catch (Exception e) {
            throw new NimbleException(NimbleExceptionMessageCode.INTERNAL_SERVER_ERROR_GET_BINARY_CONTENT.toString(),Arrays.asList(uri),e);
        }
    }

    @CrossOrigin(origins = {"*"})
    @ApiOperation(value = "", notes = "Retrieves a specified binary content wrapped inside a BinaryCbjectType instance.")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Retrieved the binary content successfully", response = CatalogueType.class),
            @ApiResponse(code = 401, message = "Invalid token. No user was found for the provided token"),
            @ApiResponse(code = 404, message = "No binary content exists for the specified uri"),
            @ApiResponse(code = 500, message = "Unexpected error while getting binary content"),
    })
    @RequestMapping(value = "/binary-contents",
            method = RequestMethod.GET,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity getBinaryContents(@ApiParam(value = "Uri of the binary content to be retrieved", required = true) @RequestParam(value = "uris") List<String> uris,
                                           @ApiParam(value = "The Bearer token provided by the identity service", required = true) @RequestHeader(value = "Authorization") String bearerToken) {
        try {
            logger.info("Request to retrieve binary contents for uris: {}", uris.toString());
            // check token
            eu.nimble.service.catalogue.util.HttpResponseUtil.checkToken(bearerToken);

            // eliminate empty uris
            uris = uris.stream()
                    .filter(uri -> StringUtils.isNotEmpty(uri))
                    .collect(Collectors.toList());

            List<BinaryObjectType> results = new BinaryContentService().retrieveContents(uris);
            ObjectMapper objectMapper = JsonSerializationUtility.getObjectMapper();
            String response = objectMapper.writeValueAsString(results);

            logger.info("Completed request to retrieve binary content for uris: {}", uris.toString());
            return ResponseEntity.ok().body(response);

        } catch (Exception e) {
            throw new NimbleException(NimbleExceptionMessageCode.INTERNAL_SERVER_ERROR_GET_BINARY_CONTENTS.toString(),Arrays.asList(uris.toString()),e);
        }
    }

    @CrossOrigin(origins = {"*"})
    @ApiOperation(value = "", notes = "Retrieves a specified binary content in raw Base64 encoded format.")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Retrieved the binary content successfully", response = CatalogueType.class),
            @ApiResponse(code = 401, message = "Invalid token. No user was found for the provided token"),
            @ApiResponse(code = 404, message = "No binary content exists for the specified uri"),
            @ApiResponse(code = 500, message = "Unexpected error while getting binary content"),
    })
    @RequestMapping(value = "/binary-content/raw",
            method = RequestMethod.GET,
            produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    public void getBase64BinaryContent(@ApiParam(value = "Uri of the binary content to be retrieved", required = true) @RequestParam(value = "uri") String uri,
                                       @ApiParam(value = "The Bearer token provided by the identity service", required = true) @RequestHeader(value = "Authorization") String bearerToken,
                                       HttpServletResponse response) {
        try {
            logger.info("Request to retrieve raw binary content for uri: {}", uri);
            BinaryObjectType result = new BinaryContentService().retrieveContent(uri);
            // check whether the binary content exists or not
            if(result == null){
                throw new NimbleException(NimbleExceptionMessageCode.NOT_FOUND_NO_BINARY_CONTENT.toString(),Arrays.asList(uri),true);
            }
            try {
                response.setHeader("Content-disposition", "attachment; filename=" + result.getFileName());
                response.addHeader("Access-Control-Expose-Headers", "Content-Disposition");
                IOUtils.write(result.getValue(), response.getOutputStream());
                response.flushBuffer();

                logger.info("Completed the request to retrieve raw binary content for uri: {}", uri);

            } catch (IOException e) {
                throw new NimbleException(NimbleExceptionMessageCode.INTERNAL_SERVER_ERROR_WRITE_BINARY_CONTENT_TO_OUTPUT_STREAM.toString(),Arrays.asList(uri),e,true);
            }

        } catch (Exception e) {
            throw new NimbleException(NimbleExceptionMessageCode.INTERNAL_SERVER_ERROR_GET_BASE_64_BINARY_CONTENT.toString(),Arrays.asList(uri),e,true);
        }
    }
}