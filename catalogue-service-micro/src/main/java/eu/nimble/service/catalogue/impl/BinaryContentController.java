package eu.nimble.service.catalogue.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import eu.nimble.service.catalogue.util.HttpResponseUtil;
import eu.nimble.service.model.ubl.commonbasiccomponents.BinaryObjectType;
import eu.nimble.utility.persistence.binary.BinaryContentService;
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
    @RequestMapping(value = "/binary-content",
            method = RequestMethod.GET,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity getContent(@RequestParam(value = "uri") String uri,
                                     @RequestHeader(value = "Authorization") String bearerToken) {
        try {
            logger.info("Request to retrieve binary content for uri: {}", uri);
            BinaryObjectType result = binaryContentService.retrieveContent(uri);

            ObjectMapper objectMapper = new ObjectMapper();
            String response = objectMapper.writeValueAsString(result);

            logger.info("Completed request to retrieve binary content for uri: {}", uri);
            return ResponseEntity.ok().body(response);

        } catch (Exception e) {
            return HttpResponseUtil.createResponseEntityAndLog(String.format("Unexpected error while getting the binary content for uri: %s", uri), e, HttpStatus.INTERNAL_SERVER_ERROR, LogLevel.ERROR);
        }
    }

    @CrossOrigin(origins = {"*"})
    @RequestMapping(value = "/binary-content/raw",
            method = RequestMethod.GET,
            produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    public void getContent(@RequestParam(value = "uri") String uri,
                           @RequestHeader(value = "Authorization") String bearerToken,
                           HttpServletResponse response) {
        try {
            logger.info("Request to retrieve raw binary content for uri: {}", uri);
            BinaryObjectType result = binaryContentService.retrieveContent(uri);

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
//
//    @CrossOrigin(origins = {"*"})
//    @RequestMapping(value = "/binary-content",
//            method = RequestMethod.GET)
//    public void createContent() {
//        try {
//            BinaryObjectType bi = new BinaryObjectType();
//            File f = new File("C:\\Users\\suat\\Desktop\\08.docx");
//            Path path = f.toPath();
//            bi.setValue(FileUtils.readFileToByteArray(f));
//            bi.setFileName(f.getName());
//            bi.setMimeCode(Files.probeContentType(path));
//            bi.setUri("uri");
//
//            binaryContentService.createContent(bi);
//
//        } catch (Exception e) {
//
//        }
//    }
//
//    @CrossOrigin(origins = {"*"})
//    @RequestMapping(value = "/binary-content",
//            method = RequestMethod.DELETE)
//    public void delete() {
//        try {
//
//            binaryContentService.deleteContent("uri");
//
//        } catch (Exception e) {
//
//        }
//    }
}