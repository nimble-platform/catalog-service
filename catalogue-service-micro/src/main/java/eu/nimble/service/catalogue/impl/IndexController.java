package eu.nimble.service.catalogue.impl;

import eu.nimble.service.catalogue.index.ItemIndexClient;
import eu.nimble.service.model.ubl.catalogue.CatalogueType;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

/**
 * Created by suat on 21-Feb-19.
 */
@Controller
public class IndexController {

    private static Logger log = LoggerFactory.getLogger(IndexController.class);

    @Autowired
    private ItemIndexClient itemIndexClient;

    @CrossOrigin(origins = {"*"})
    @ApiOperation(value = "", notes = "Gets all the catalogue identifiers from the item index and clear all data in the index")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Deleted the content in the index successfully"),
            @ApiResponse(code = 401, message = "Invalid token. No user was found for the provided token"),
            @ApiResponse(code = 500, message = "Failed to delete content in the index")
    })
    @RequestMapping(value = "/catalogue/index/item",
            produces = {"application/json"},
            method = RequestMethod.DELETE)
    public ResponseEntity clearItemIndex(@ApiParam(value = "The Bearer token provided by the identity service", required = true) @RequestHeader(value = "Authorization", required = true) String bearerToken) {
        log.info("Incoming request to clear the item index");

        // check token
        ResponseEntity tokenCheck = eu.nimble.service.catalogue.util.HttpResponseUtil.checkToken(bearerToken);
        if (tokenCheck != null) {
            return tokenCheck;
        }

        // delete the contents
        itemIndexClient.deleteAllContent();

        log.info("Completed request to clear the item index");
        return ResponseEntity.ok().build();
    }

    @CrossOrigin(origins = {"*"})
    @ApiOperation(value = "", notes = "Deletes the specified catalogue from index")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Deleted the specified catalogue from the index successfully"),
            @ApiResponse(code = 401, message = "Invalid token. No user was found for the provided token"),
            @ApiResponse(code = 500, message = "Failed to delete the catalogue from the index")
    })
    @RequestMapping(value = "/catalogue/index/{catalogueUuid}",
            produces = {"application/json"},
            method = RequestMethod.DELETE)
    public ResponseEntity clearItemIndex(@ApiParam(value = "The Bearer token provided by the identity service", required = true) @RequestHeader(value = "Authorization", required = true) String bearerToken,
                                         @ApiParam(value = "Uuid of the catalogue to be deleted from the index") @PathVariable(value = "catalogueUuid",required = true) String catalogueUuid) {
        log.info("Incoming request to delete catalogue uuid from the item index with uuid: {}", catalogueUuid);

        // check token
        ResponseEntity tokenCheck = eu.nimble.service.catalogue.util.HttpResponseUtil.checkToken(bearerToken);
        if (tokenCheck != null) {
            return tokenCheck;
        }

        // delete the contents
        itemIndexClient.deleteCatalogue(catalogueUuid);

        log.info("Completed request to delete catalogue from the item index with uuid: {}", catalogueUuid);
        return ResponseEntity.ok().build();
    }
}
