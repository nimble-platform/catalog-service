package eu.nimble.service.catalogue.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import eu.nimble.service.catalogue.util.SpringBridge;
import eu.nimble.service.model.ubl.catalogue.CatalogueType;
import eu.nimble.service.model.ubl.commonbasiccomponents.BinaryObjectType;
import eu.nimble.utility.HttpResponseUtil;
import eu.nimble.utility.JsonSerializationUtility;
import eu.nimble.utility.persistence.binary.BinaryContentService;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.logging.LogLevel;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@Controller
public class BinaryContentController {

    private static Logger logger = LoggerFactory.getLogger(BinaryContentController.class);

    @Autowired
    private BinaryContentService binaryContentService;

    @CrossOrigin(origins = {"*"})
    @ApiOperation(value = "", notes = "Retrieves a specified binary content wrapped inside a BinaryCbjectType instance.")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Retrieved the binary content successfully", response = CatalogueType.class),
            @ApiResponse(code = 204, message = "No binary content exists for the specified uri"),
            @ApiResponse(code = 401, message = "Invalid token. No user was found for the provided token"),
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
            boolean isValid = SpringBridge.getInstance().getIdentityClientTyped().getUserInfo(bearerToken);
            if(!isValid){
                String msg = String.format("No user exists for the given token : %s",bearerToken);
                logger.error(msg);
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(msg);
            }
            BinaryObjectType result = binaryContentService.retrieveContent(uri);
            // check whether the binary content exists or not
            if(result == null){
                String msg = String.format("There does not exist a binary content for uri: %s", uri);
                logger.error(msg);
                return ResponseEntity.status(HttpStatus.NO_CONTENT).body(msg);
            }

            ObjectMapper objectMapper = JsonSerializationUtility.getObjectMapper();
            String response = objectMapper.writeValueAsString(result);

            logger.info("Completed request to retrieve binary content for uri: {}", uri);
            return ResponseEntity.ok().body(response);

        } catch (Exception e) {
            return HttpResponseUtil.createResponseEntityAndLog(String.format("Unexpected error while getting the binary content for uri: %s", uri), e, HttpStatus.INTERNAL_SERVER_ERROR, LogLevel.ERROR);
        }
    }

    @CrossOrigin(origins = {"*"})
    @ApiOperation(value = "", notes = "Retrieves a specified binary content in raw Base64 encoded format.")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Retrieved the binary content successfully", response = CatalogueType.class),
            @ApiResponse(code = 204, message = "No binary content exists for the specified uri"),
            @ApiResponse(code = 401, message = "Invalid token. No user was found for the provided token"),
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
            // check token
            boolean isValid = SpringBridge.getInstance().getIdentityClientTyped().getUserInfo(bearerToken);
            if(!isValid){
                String msg = String.format("No user exists for the given token : %s",bearerToken);
                logger.error(msg);
                response.setStatus(HttpStatus.UNAUTHORIZED.value());
                try {
                    response.getOutputStream().write(msg.getBytes());
                } catch (IOException e1) {
                    logger.error("Failed to write the error message to the output stream", e1);
                }
            }
            BinaryObjectType result = binaryContentService.retrieveContent(uri);
            // check whether the binary content exists or not
            if(result == null){
                String msg = String.format("There does not exist a binary content for uri: %s", uri);
                logger.error(msg);
                response.setStatus(HttpStatus.NO_CONTENT.value());
                try {
                    response.getOutputStream().write(msg.getBytes());
                } catch (IOException e1) {
                    logger.error("Failed to write the error message to the output stream", e1);
                }
            }
            try {
                response.setHeader("Content-disposition", "attachment; filename=" + result.getFileName());
                response.addHeader("Access-Control-Expose-Headers", "Content-Disposition");
                IOUtils.write(result.getValue(), response.getOutputStream());
                response.flushBuffer();

                logger.info("Completed the request to retrieve raw binary content for uri: {}", uri);

            } catch (IOException e) {
                String msg = String.format("Failed to write the raw binary content to the response output stream for uri: %s\n%s", uri, e.getMessage());
                logger.error(msg, e);
                response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
                try {
                    response.getOutputStream().write(msg.getBytes());
                } catch (IOException e1) {
                    logger.error("Failed to write the error message to the output stream", e);
                }
            }

        } catch (Exception e) {
            String msg = String.format("Unexpected error while retrieving the raw content for uri: %s\n%s", uri, e.getMessage());
            logger.error(msg, e);
            response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
            try {
                response.getOutputStream().write(msg.getBytes());
            } catch (IOException e1) {
                logger.error("Failed to write the error message to the output stream", e);
            }
        }
    }
}