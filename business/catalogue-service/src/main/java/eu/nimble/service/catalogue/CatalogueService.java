package eu.nimble.service.catalogue;

import eu.nimble.service.model.modaml.catalogue.TEXCatalogType;
import eu.nimble.service.model.ubl.catalogue.CatalogueType;
import eu.nimble.utility.Configuration;

public interface CatalogueService {

	public void addCatalogue(String xml, Configuration.Standard standard);
	
	public Object getCatalogueByUUID(String uuid, Configuration.Standard standard);
	
	public void deleteCatalogueByUUID(String uuid, Configuration.Standard standard);

	public void addCatalogue(CatalogueType catalogue);

	public void addCatalogue(TEXCatalogType catalogue);
}
