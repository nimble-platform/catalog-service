package eu.nimble.service.catalogue.impl;

import eu.nimble.service.catalogue.index.ClassIndexClient;
import eu.nimble.service.catalogue.index.PartyIndexClient;
import eu.nimble.service.catalogue.util.migration.r8.CatalogueIndexLoader;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import springfox.documentation.annotations.ApiIgnore;

/**
 * Created by suat on 28-Jan-19.
 */
@ApiIgnore
@Controller
public class AdminController {

    @Autowired
    private PartyIndexClient partyIndexClient;
    @Autowired
    private CatalogueIndexLoader catalogueIndexLoader;

    @CrossOrigin(origins = {"*"})
    @RequestMapping(value = "/admin/add-class",
            produces = {"application/json"},
            method = RequestMethod.GET)
    public ResponseEntity getDefaultCatalogue() {
//        partyIndexClient.indexParty();
        return null;
    }

    @CrossOrigin(origins = {"*"})
    @RequestMapping(value = "/admin/index-catalogues",
            produces = {"application/json"},
            method = RequestMethod.POST)
    public ResponseEntity indexAllCatalogues() {
        catalogueIndexLoader.indexCatalogues();
        return null;
    }
}
