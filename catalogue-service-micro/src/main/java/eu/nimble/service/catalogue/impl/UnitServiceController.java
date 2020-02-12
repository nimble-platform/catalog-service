package eu.nimble.service.catalogue.impl;

import eu.nimble.service.catalogue.UnitManager;
import eu.nimble.service.catalogue.config.RoleConfig;
import eu.nimble.service.catalogue.model.unit.UnitList;
import eu.nimble.utility.exception.NimbleException;
import eu.nimble.utility.exception.NimbleExceptionMessageCode;
import eu.nimble.utility.validation.IValidationUtil;
import io.swagger.annotations.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;

@Controller
public class UnitServiceController {
    private static final Logger logger = LoggerFactory.getLogger(UnitServiceController.class);

    @Autowired
    private UnitManager unitManager;
    @Autowired
    private IValidationUtil validationUtil;

    @CrossOrigin(origins = {"*"})
    @ApiOperation(value = "", notes = "Gets all unit lists")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Retrieved all unit lists successfully", response = UnitList.class, responseContainer = "List")
    })
    @RequestMapping(value = "/unit-lists",
            produces = {"application/json"},
            method = RequestMethod.GET)
    public ResponseEntity<List<UnitList>> getAllUnitLists(@ApiParam(value = "The Bearer token provided by the identity service", required = true) @RequestHeader(value = "Authorization", required = true) String bearerToken) {
        logger.info("All unit lists will be received");
        List<UnitList> resultSet = unitManager.getAllUnitList();
        logger.info("All unit lists are received");
        return ResponseEntity.ok(resultSet);
    }

    @CrossOrigin(origins = {"*"})
    @ApiOperation(value = "", notes = "Gets units of the specified list")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Retrieved all units for the given unit list id successfully", response = String.class, responseContainer = "List"),
            @ApiResponse(code = 404, message = "No unit list with the given id")
    })
    @RequestMapping(value = "/unit-lists/{unitListId}",
            produces = {"application/json"},
            method = RequestMethod.GET)
    public ResponseEntity getValues(@ApiParam(value = "List id for which the contained units to be retrieved", required = true) @PathVariable String unitListId,
                                    @ApiParam(value = "The Bearer token provided by the identity service", required = true) @RequestHeader(value = "Authorization", required = true) String bearerToken) {
        logger.info("All units will be received for unitListId: {}", unitListId);

        if (!unitManager.checkUnitListId(unitListId)) {
            throw new NimbleException(NimbleExceptionMessageCode.NOT_FOUND_NO_UNIT_LIST.toString(), Arrays.asList(unitListId));
        }

        List<String> resultSet = unitManager.getValues(unitListId);
        logger.info("All units are received for unitListId: {}", unitListId);
        return ResponseEntity.ok(resultSet);
    }

    @CrossOrigin(origins = {"*"})
    @ApiOperation(value = "", notes = "Deletes the specified unit list")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Deleted unit list successfully", response = String.class, responseContainer = "List"),
            @ApiResponse(code = 401, message = "Invalid token. No user was found for the provided token"),
            @ApiResponse(code = 404, message = "No unit list with the given id"),
    })
    @RequestMapping(value = "/unit-lists/{unitListId}",
            produces = {"application/json"},
            method = RequestMethod.DELETE)
    public ResponseEntity addUnitToList(@ApiParam(value = "Id of the list to be deleted", required = true) @PathVariable String unitListId,
                                        @ApiParam(value = "The Bearer token provided by the identity service", required = true) @RequestHeader(value = "Authorization", required = true) String bearerToken) {
        logger.info("Unit list '{}' will be deleted", unitListId);
        // validate role
        if(!validationUtil.validateRole(bearerToken, RoleConfig.REQUIRED_ROLES_CATALOGUE)) {
            throw new NimbleException(NimbleExceptionMessageCode.UNAUTHORIZED_INVALID_ROLE.toString());
        }

        if (!unitManager.checkUnitListId(unitListId)) {
            throw new NimbleException(NimbleExceptionMessageCode.NOT_FOUND_NO_UNIT_LIST.toString(),Arrays.asList(unitListId));
        }
        unitManager.deleteUnitList(unitListId);

        logger.info("Unit '{}' is deleted", unitListId);
        return ResponseEntity.ok().build();
    }

    @CrossOrigin(origins = {"*"})
    @ApiOperation(value = "", notes = "Adds a unit to a specified list")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Added unit successfully", response = String.class, responseContainer = "List"),
            @ApiResponse(code = 404, message = "No unit list with the given id"),
            @ApiResponse(code = 409, message = "Unit already exists for unit list with the given id")
    })
    @RequestMapping(value = "/unit-lists/{unitListId}",
            produces = {"application/json"},
            method = RequestMethod.PATCH)
    public ResponseEntity addUnitToList(@ApiParam(value = "List id to which the specified unit to be added", required = true) @PathVariable String unitListId,
                                        @ApiParam(value = "Unit to be added", required = true) @RequestParam("unit") String unit,
                                        @ApiParam(value = "The Bearer token provided by the identity service", required = true) @RequestHeader(value = "Authorization", required = true) String bearerToken) {
        logger.info("Unit '{}' will be added to unit list with id: {}", unit, unitListId);
        // validate role
        if(!validationUtil.validateRole(bearerToken, RoleConfig.REQUIRED_ROLES_CATALOGUE)) {
            throw new NimbleException(NimbleExceptionMessageCode.UNAUTHORIZED_INVALID_ROLE.toString());
        }

        if (!unitManager.checkUnitListId(unitListId)) {
            throw new NimbleException(NimbleExceptionMessageCode.NOT_FOUND_NO_UNIT_LIST.toString(),Arrays.asList(unitListId));
        }
        if (unitManager.checkUnit(unit, unitListId)) {
            throw new NimbleException(NimbleExceptionMessageCode.CONFLICT_UNIT_EXISTS.toString(),Arrays.asList(unitListId));
        }

        List<String> resultSet = unitManager.addUnitToList(unit, unitListId);
        logger.info("Unit '{}' is added to unit list with id: {}", unit, unitListId);
        return ResponseEntity.ok(resultSet);
    }

    @CrossOrigin(origins = {"*"})
    @ApiOperation(value = "", notes = "Deletes a unit from a specified list")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Deleted unit successfully", response = String.class, responseContainer = "List"),
            @ApiResponse(code = 404, message = "No unit list with the given id or no such unis available in the specified list")
    })
    @RequestMapping(value = "/unit-lists/{unitListId}/unit/{unit}",
            produces = {"application/json"},
            method = RequestMethod.DELETE)
    public ResponseEntity deleteUnitFromList(@ApiParam(value = "List id from which the specified unit to be deleted", required = true) @PathVariable String unitListId,
                                             @ApiParam(value = "Unit to be added", required = true) @PathVariable String unit,
                                             @ApiParam(value = "The Bearer token provided by the identity service", required = true) @RequestHeader(value = "Authorization", required = true) String bearerToken) {
        logger.info("Unit '{}' will be deleted from unit list with id: {}", unit, unitListId);
        // validate role
        if(!validationUtil.validateRole(bearerToken, RoleConfig.REQUIRED_ROLES_CATALOGUE)) {
            throw new NimbleException(NimbleExceptionMessageCode.UNAUTHORIZED_INVALID_ROLE.toString());
        }

        if (!unitManager.checkUnitListId(unitListId)) {
            throw new NimbleException(NimbleExceptionMessageCode.NOT_FOUND_NO_UNIT_LIST.toString(),Arrays.asList(unitListId));
        }
        if (!unitManager.checkUnit(unit, unitListId)) {
            throw new NimbleException(NimbleExceptionMessageCode.NOT_FOUND_NO_UNIT.toString(),Arrays.asList(unit,unitListId));
        }

        List<String> resultSet = unitManager.deleteUnitFromList(unit, unitListId);
        logger.info("Unit '{}' is deleted from unit list with id: {}", unit, unitListId);
        return ResponseEntity.ok(resultSet);
    }

    @CrossOrigin(origins = {"*"})
    @ApiOperation(value = "", notes = "Creates a unit list with the given id and units")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Created the unit list successfully", response = String.class, responseContainer = "List"),
            @ApiResponse(code = 409, message = "Unit list with the given id already exists")
    })
    @RequestMapping(value = "/unit-lists",
            produces = {"application/json"},
            method = RequestMethod.POST)
    public ResponseEntity addUnitList(@ApiParam(value = "Id for the unit list to be created", required = true) @RequestParam("unitListId") String unitListId,
                                      @ApiParam(value = "Comma-separated units to be included in the unit list", required = true) @RequestParam("units") List<String> units,
                                      @ApiParam(value = "The Bearer token provided by the identity service", required = true) @RequestHeader(value = "Authorization", required = true) String bearerToken) {
        logger.info("Unit list with id: {} will be persisted in DB", unitListId);
        // validate role
        if(!validationUtil.validateRole(bearerToken, RoleConfig.REQUIRED_ROLES_CATALOGUE)) {
            throw new NimbleException(NimbleExceptionMessageCode.UNAUTHORIZED_INVALID_ROLE.toString());
        }

        if (unitManager.checkUnitListId(unitListId)) {
            throw new NimbleException(NimbleExceptionMessageCode.CONFLICT_UNIT_LIST_EXISTS.toString(),Arrays.asList(unitListId));
        }

        List<String> resultSet = unitManager.addUnitList(unitListId, units);
        logger.info("Unit list with id: {} is persisted in DB", unitListId);
        return ResponseEntity.ok(resultSet);
    }
}
