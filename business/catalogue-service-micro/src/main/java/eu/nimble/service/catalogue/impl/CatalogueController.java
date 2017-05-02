package eu.nimble.service.catalogue.impl;

import eu.nimble.service.catalogue.CatalogueService;
import eu.nimble.service.catalogue.client.IdentityClient;
import eu.nimble.service.model.modaml.catalogue.TEXCatalogType;
import eu.nimble.service.model.ubl.catalogue.CatalogueType;
import eu.nimble.service.model.ubl.commonaggregatecomponents.PartyType;
import eu.nimble.utility.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

@Controller
public class CatalogueController {

	private static Logger log = LoggerFactory
		.getLogger(CatalogueController.class);

	private CatalogueService service = CatalogueServiceImpl.getInstance();

	@Autowired
    private  IdentityClient identityClient;

	@RequestMapping(value = "/catalogue/ubl",
		method = RequestMethod.POST)
	public ResponseEntity<Void> addUBLCatalogue(@RequestBody String catalogueXML, @RequestParam String partyId) {

		PartyType party = identityClient.getParty(partyId);
		if (party == null)
		{
			log.warn("Party with Id {0} not found", partyId);
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null);
		}

		log.debug("Fetched party with Id {0}", party.getHjid());

		// TODO for Suat: use party type in catalague
		service.addCatalogue(catalogueXML, Configuration.Standard.UBL);
		return ResponseEntity.ok(null);
	}

	@RequestMapping(value = "/catalogue/ubl/{uuid}",
		produces = {"application/json"},
		method = RequestMethod.GET)
	public ResponseEntity<CatalogueType> getUBLCatalogueByUUID(@PathVariable String uuid) {
		CatalogueType catalogue = (CatalogueType) service.getCatalogueByUUID(uuid, Configuration.Standard.UBL);
		return ResponseEntity.ok(catalogue);
	}

	@RequestMapping(value = "/catalogue/modaml",
		method = RequestMethod.POST)
	public ResponseEntity<Void> addMODAMLCatalogue(@RequestBody String catalogueXML) {
		service.addCatalogue(catalogueXML, Configuration.Standard.UBL);
		return ResponseEntity.ok(null);
	}

	@RequestMapping(value = "/catalogue/modaml/{uuid}",
		produces = {"application/json"},
		method = RequestMethod.GET)
	public ResponseEntity<TEXCatalogType> getMODAMLCatalogueByUUID(@PathVariable String uuid) {
		TEXCatalogType catalogue = (TEXCatalogType) service.getCatalogueByUUID(uuid, Configuration.Standard.MODAML);
		return ResponseEntity.ok(catalogue);
	}
}
