package eu.nimble.service.catalogue.impl;

import eu.nimble.service.catalogue.index.ClassIndexClient;
import eu.nimble.service.catalogue.index.PartyIndexClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

/**
 * Created by suat on 28-Jan-19.
 */
@Controller
public class AdminController {

    @Autowired
    private PartyIndexClient partyIndexClient;

    @CrossOrigin(origins = {"*"})
    @RequestMapping(value = "/admin/add-class",
            produces = {"application/json"},
            method = RequestMethod.GET)
    public ResponseEntity getDefaultCatalogue() {
        partyIndexClient.indexParty();
        return null;
    }
}
