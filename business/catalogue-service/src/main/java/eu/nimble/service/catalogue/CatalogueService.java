package eu.nimble.service.catalogue;

import eu.nimble.service.model.modaml.catalogue.TEXCatalogType;
import eu.nimble.service.model.ubl.catalogue.CatalogueType;
import eu.nimble.service.model.ubl.commonaggregatecomponents.PartyType;
import eu.nimble.utility.Configuration;
import org.apache.poi.ss.usermodel.Workbook;

import java.io.InputStream;
import java.io.OutputStream;

public interface CatalogueService {

	public void addCatalogue(PartyType party, String xml, Configuration.Standard standard);
	
	public Object getCatalogueByUUID(String uuid, Configuration.Standard standard);
	
	public void deleteCatalogueByUUID(String uuid, Configuration.Standard standard);

	public void addCatalogue(PartyType party, CatalogueType catalogue);

	public void addCatalogue(PartyType party, TEXCatalogType catalogue);

	public Workbook generateTemplateForCategory(String categoryId);

	public void addCatalogue(PartyType party, InputStream template);
}
