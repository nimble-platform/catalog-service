package eu.nimble.service.catalogue;

import eu.nimble.service.model.modaml.catalogue.TEXCatalogType;
import eu.nimble.service.model.ubl.catalogue.CatalogueType;
import eu.nimble.service.model.ubl.commonaggregatecomponents.CatalogueLineType;
import eu.nimble.service.model.ubl.commonaggregatecomponents.GoodsItemType;
import eu.nimble.service.model.ubl.commonaggregatecomponents.PartyType;
import eu.nimble.utility.Configuration;
import org.apache.poi.ss.formula.functions.T;
import org.apache.poi.ss.usermodel.Workbook;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.zip.ZipInputStream;

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
     * Returns the supported standards by this {@link CatalogueService}
     *
     * @return
     */
    public List<Configuration.Standard> getSupportedStandards();

    /**
     * @return
     */
    public Workbook generateTemplateForCategory(List<String> categoryId, List<String> taxonomyIds);

    /**
     * Adds the catalogue given through the NIMBLE-specific, Excel-based template.
     *
     * @param catalogueTemplate
     * @param party
     */
    public CatalogueType parseCatalogue(InputStream catalogueTemplate, String uploadMode, PartyType party);

    /**
     * Adds the provided images to the relevant products in the catalogue.
     * !!! This method does not update the database
     * @param imagePackage
     * @param catalogueUuid
     * @return
     */
    public CatalogueType addImagesToProducts(ZipInputStream imagePackage, String catalogueUuid);

    /*
     * Catalogue-line level endpoints
     */

    public <T> T getCatalogueLine(String catalogueId, String catalogueLineId);

    public <T> List<T> getCatalogueLines(String catalogueId, List<String> catalogueLineIds);

    public CatalogueLineType addLineToCatalogue(CatalogueType catalogue, CatalogueLineType catalogueLine);

    public CatalogueLineType updateCatalogueLine(CatalogueLineType catalogueLine);

    public void deleteCatalogueLineById(String catalogueId, String lineId);
}
