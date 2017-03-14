package eu.nimble.service.catalogue.impl;

import eu.nimble.service.catalogue.CatalogueService;
import eu.nimble.service.model.modaml.catalogue.TEXCatalogType;
import eu.nimble.service.model.ubl.catalogue.CatalogueType;
import eu.nimble.utility.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

@Controller
public class CatalogueController {

	private static Logger log = LoggerFactory
		.getLogger(CatalogueController.class);

	private CatalogueService service = CatalogueServiceImpl.getInstance();

	@RequestMapping(value = "/catalogue/ubl",
		method = RequestMethod.POST)
	public ResponseEntity<Void> addUBLCatalogue(@RequestBody String catalogueXML) {
		//log.info(" $$$ XML Representation: " + catalogueXML);
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
