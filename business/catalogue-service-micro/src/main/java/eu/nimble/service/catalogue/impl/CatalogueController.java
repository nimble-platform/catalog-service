package eu.nimble.service.catalogue.impl;

import eu.nimble.service.catalogue.CatalogueService;
import eu.nimble.service.model.modaml.catalogue.TEXCatalogType;
import eu.nimble.service.model.ubl.catalogue.CatalogueType;
import eu.nimble.utility.Configuration;
import org.apache.poi.ss.usermodel.Workbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import java.io.FileOutputStream;
import java.io.IOException;

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

    @RequestMapping(value = "/catalogue/template",
            method = RequestMethod.GET,
            produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    public void downloadTemplate(@RequestParam String categoryId, HttpServletResponse response) throws IOException {
        Workbook template = service.generateTemplateForCategory(categoryId);

        FileOutputStream fileOut = null;
        try {
            fileOut = new FileOutputStream(categoryId + "-catalogue-template.xlsx");
            template.write(fileOut);
            fileOut.close();
        } catch (java.io.IOException e) {
            log.error("Failed to create template for category: " + categoryId, e);
        }

        template.write(response.getOutputStream());
        response.flushBuffer();
    }

    @RequestMapping(value = "/catalogue/template/upload", method = RequestMethod.POST)
    public ResponseEntity uploadTemplate(
            @RequestParam("file") MultipartFile file,
            @RequestParam("companyId") String companyId) {
        try {
            //TODO construct the party instance properly
            service.addCatalogue(null, file.getInputStream());

        } catch (IOException e) {
            e.printStackTrace();
        }
        return ResponseEntity.ok().build();
    }
}
