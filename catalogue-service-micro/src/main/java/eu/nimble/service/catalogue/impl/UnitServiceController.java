package eu.nimble.service.catalogue.impl;

import eu.nimble.service.catalogue.UnitManager;
import eu.nimble.service.catalogue.model.unit.UnitList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Controller
public class UnitServiceController {
    private static final Logger logger = LoggerFactory.getLogger(UnitServiceController.class);
    private UnitManager unitManager = UnitManager.getInstance();

    @CrossOrigin(origins = {"*"})
    @RequestMapping(value = "/unit-lists",
            produces = {"application/json"},
            method = RequestMethod.GET)
    public ResponseEntity<List<UnitList>> getAllUnitList(){
        logger.info("All unit lists will be received");
        List<UnitList> resultSet = unitManager.getAllUnitList();
        logger.info("All unit lists are received");
        return ResponseEntity.ok(resultSet);
    }

    @CrossOrigin(origins = {"*"})
    @RequestMapping(value = "/unit-lists/{unitListId}",
            produces = {"application/json"},
            method = RequestMethod.GET)
    public ResponseEntity getValues(@PathVariable String unitListId){
        logger.info("All units will be received for unitListId: {}",unitListId);

        if(!unitManager.checkUnitListId(unitListId)){
            return createErrorResponseEntity("No unit list with id: "+unitListId, HttpStatus.BAD_REQUEST);
        }

        List<String> resultSet = unitManager.getValues(unitListId);
        logger.info("All units are received for unitListId: {}",unitListId);
        return ResponseEntity.ok(resultSet);
    }

    @CrossOrigin(origins = {"*"})
    @RequestMapping(value = "/unit-lists/{unitListId}",
            produces = {"application/json"},
            method = RequestMethod.PATCH)
    public ResponseEntity addUnitToList(@RequestParam("unit") String unit,@PathVariable String unitListId){
        logger.info("Unit '{}' will be added to unit list with id: {}",unit,unitListId);

        if(!unitManager.checkUnitListId(unitListId)){
            return createErrorResponseEntity("No unit list with id: "+unitListId, HttpStatus.BAD_REQUEST);
        }
        if(unitManager.checkUnit(unit,unitListId)){
            return createErrorResponseEntity("Unit '"+unit+"' already exists for unit list with id: "+unitListId, HttpStatus.BAD_REQUEST);
        }

        List<String> resultSet = unitManager.addUnitToList(unit,unitListId);
        logger.info("Unit '{}' is added to unit list with id: {}",unit,unitListId);
        return ResponseEntity.ok(resultSet);
    }

    @CrossOrigin(origins = {"*"})
    @RequestMapping(value = "/unit-lists/{unitListId}/unit/{unit}",
            produces = {"application/json"},
            method = RequestMethod.DELETE)
    public ResponseEntity deleteUnitFromList(@PathVariable String unit,@PathVariable String unitListId){
        logger.info("Unit '{}' will be deleted from unit list with id: {}",unit,unitListId);

        if(!unitManager.checkUnitListId(unitListId)){
            return createErrorResponseEntity("No unit list with id: "+unitListId, HttpStatus.BAD_REQUEST);
        }
        if(!unitManager.checkUnit(unit,unitListId)){
            return createErrorResponseEntity("Unit '"+unit+"' does not exist for unit list with id: "+unitListId, HttpStatus.BAD_REQUEST);
        }

        List<String> resultSet = unitManager.deleteUnitFromList(unit,unitListId);
        logger.info("Unit '{}' is deleted from unit list with id: {}",unit,unitListId);
        return ResponseEntity.ok(resultSet);
    }

    @CrossOrigin(origins = {"*"})
    @RequestMapping(value = "/unit-lists",
            produces = {"application/json"},
            method = RequestMethod.POST)
    public ResponseEntity addUnitList(@RequestParam("units") List<String> units,@RequestParam("unitListId") String unitListId){
        logger.info("Unit list with id: {} will be persisted in DB",unitListId);

        if(unitManager.checkUnitListId(unitListId)){
            return createErrorResponseEntity("There is already a unit list with id: "+unitListId, HttpStatus.BAD_REQUEST);
        }

        List<String> resultSet = unitManager.addUnitList(unitListId,units);
        logger.info("Unit list with id: {} is persisted in DB",unitListId);
        return ResponseEntity.ok(resultSet);
    }

    private ResponseEntity createErrorResponseEntity(String msg, HttpStatus status) {
        logger.error(msg);
        return ResponseEntity.status(status).body(msg);
    }
}
