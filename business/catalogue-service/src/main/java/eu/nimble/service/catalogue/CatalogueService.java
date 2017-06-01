package eu.nimble.service.catalogue;

import eu.nimble.service.model.modaml.catalogue.TEXCatalogType;
import eu.nimble.service.model.ubl.catalogue.CatalogueType;
import eu.nimble.service.model.ubl.commonaggregatecomponents.GoodsItemType;
import eu.nimble.service.model.ubl.commonaggregatecomponents.PartyType;
import eu.nimble.utility.Configuration;
import org.apache.poi.ss.formula.functions.T;
import org.apache.poi.ss.usermodel.Workbook;

import java.io.InputStream;
import java.io.OutputStream;

public interface CatalogueService {

    public CatalogueType addCatalogue(CatalogueType catalogue);

    public CatalogueType addCatalogue(String catalogueXml);

    public CatalogueType getCatalogue(String uuid);

    public CatalogueType getCatalogue(String id, String partyId);

    public CatalogueType updateCatalogue(CatalogueType catalogue);

    public void deleteCatalogue(String uuid);

    public void deleteCatalogue(String id, String partyId);

    public <T> T addCatalogue(String catalogueXML, Configuration.Standard standard);

    public <T> T addCatalogue(T catalogue, Configuration.Standard standard);

    public <T> T getCatalogue(String uuid, Configuration.Standard standard);

    public <T> T getCatalogue(String id, String partyId, Configuration.Standard standard);

    public void deleteCatalogue(String uuid, Configuration.Standard standard);

    /**
     * Generates the template for the given {@code categoryId}. The template includes the details about the
     * properties
     *
     * @param categoryId
     * @return
     */
    public Workbook generateTemplateForCategory(String categoryId);

    /**
     * Adds the catalogue given through the NIMBLE-specific, Excel-based template.
     *
     * @param catalogueTemplate
     * @param party
     */
    public void addCatalogue(InputStream catalogueTemplate, PartyType party);
}
