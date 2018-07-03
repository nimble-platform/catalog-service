package eu.nimble.service.catalogue.impl;

import eu.nimble.service.catalogue.UnitManager;
import eu.nimble.service.catalogue.model.unit.UnitList;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Controller
public class UnitServiceController {
    private static final Logger logger = LoggerFactory.getLogger(UnitServiceController.class);

    @Autowired
    private UnitManager unitManager;

    @CrossOrigin(origins = {"*"})
    @ApiOperation(value = "", notes = "Receive all unit lists")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Received all unit lists successfully", response = UnitList.class, responseContainer = "List")
    })
    @RequestMapping(value = "/unit-lists",
            produces = {"application/json"},
            method = RequestMethod.GET)
    public ResponseEntity<List<UnitList>> getAllUnitList() {
        logger.info("All unit lists will be received");
        List<UnitList> resultSet = unitManager.getAllUnitList();
        logger.info("All unit lists are received");
        return ResponseEntity.ok(resultSet);
    }

    @CrossOrigin(origins = {"*"})
    @ApiOperation(value = "", notes = "Receive all units for the given unit list id")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Received all units for the given unit list id successfully", response = String.class, responseContainer = "List"),
            @ApiResponse(code = 404, message = "No unit list with the given id")
    })
    @RequestMapping(value = "/unit-lists/{unitListId}",
            produces = {"application/json"},
            method = RequestMethod.GET)
    public ResponseEntity getValues(@PathVariable String unitListId) {
        logger.info("All units will be received for unitListId: {}", unitListId);

        if (!unitManager.checkUnitListId(unitListId)) {
            return createErrorResponseEntity("No unit list with id: " + unitListId, HttpStatus.NOT_FOUND);
        }

        List<String> resultSet = unitManager.getValues(unitListId);
        logger.info("All units are received for unitListId: {}", unitListId);
        return ResponseEntity.ok(resultSet);
    }

    @CrossOrigin(origins = {"*"})
    @ApiOperation(value = "", notes = "Add unit to unit list with the given id")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Added unit successfully", response = String.class, responseContainer = "List"),
            @ApiResponse(code = 404, message = "No unit list with the given id"),
            @ApiResponse(code = 400, message = "Unit already exists for unit list with the given id")
    })
    @RequestMapping(value = "/unit-lists/{unitListId}",
            produces = {"application/json"},
            method = RequestMethod.PATCH)
    public ResponseEntity addUnitToList(@RequestParam("unit") String unit, @PathVariable String unitListId) {
        logger.info("Unit '{}' will be added to unit list with id: {}", unit, unitListId);

        if (!unitManager.checkUnitListId(unitListId)) {
            return createErrorResponseEntity("No unit list with id: " + unitListId, HttpStatus.NOT_FOUND);
        }
        if (unitManager.checkUnit(unit, unitListId)) {
            return createErrorResponseEntity("Unit '" + unit + "' already exists for unit list with id: " + unitListId, HttpStatus.BAD_REQUEST);
        }

        List<String> resultSet = unitManager.addUnitToList(unit, unitListId);
        logger.info("Unit '{}' is added to unit list with id: {}", unit, unitListId);
        return ResponseEntity.ok(resultSet);
    }

    @CrossOrigin(origins = {"*"})
    @ApiOperation(value = "", notes = "Delete unit from the unit list with the given id")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Deleted unit successfully", response = String.class, responseContainer = "List"),
            @ApiResponse(code = 404, message = "No unit list with the given id"),
            @ApiResponse(code = 400, message = "Unit does not exist for unit list with the given id")
    })
    @RequestMapping(value = "/unit-lists/{unitListId}/unit/{unit}",
            produces = {"application/json"},
            method = RequestMethod.DELETE)
    public ResponseEntity deleteUnitFromList(@PathVariable String unit, @PathVariable String unitListId) {
        logger.info("Unit '{}' will be deleted from unit list with id: {}", unit, unitListId);

        if (!unitManager.checkUnitListId(unitListId)) {
            return createErrorResponseEntity("No unit list with id: " + unitListId, HttpStatus.NOT_FOUND);
        }
        if (!unitManager.checkUnit(unit, unitListId)) {
            return createErrorResponseEntity("Unit '" + unit + "' does not exist for unit list with id: " + unitListId, HttpStatus.BAD_REQUEST);
        }

        List<String> resultSet = unitManager.deleteUnitFromList(unit, unitListId);
        logger.info("Unit '{}' is deleted from unit list with id: {}", unit, unitListId);
        return ResponseEntity.ok(resultSet);
    }

    @CrossOrigin(origins = {"*"})
    @ApiOperation(value = "", notes = "Create a unit list")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Created unit list successfully", response = String.class, responseContainer = "List"),
            @ApiResponse(code = 400, message = "Unit list with the given id already exists")
    })
    @RequestMapping(value = "/unit-lists",
            produces = {"application/json"},
            method = RequestMethod.POST)
    public ResponseEntity addUnitList(@RequestParam("units") List<String> units, @RequestParam("unitListId") String unitListId) {
        logger.info("Unit list with id: {} will be persisted in DB", unitListId);

        if (unitManager.checkUnitListId(unitListId)) {
            return createErrorResponseEntity(String.format("Unit list with id %s already exists", unitListId), HttpStatus.BAD_REQUEST);
        }

        List<String> resultSet = unitManager.addUnitList(unitListId, units);
        logger.info("Unit list with id: {} is persisted in DB", unitListId);
        return ResponseEntity.ok(resultSet);
    }

    private ResponseEntity createErrorResponseEntity(String msg, HttpStatus status) {
        logger.error(msg);
        return ResponseEntity.status(status).body(msg);
    }
}
