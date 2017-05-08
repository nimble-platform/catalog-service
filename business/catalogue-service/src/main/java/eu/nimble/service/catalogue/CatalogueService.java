package eu.nimble.service.catalogue;

import eu.nimble.service.model.modaml.catalogue.TEXCatalogType;
import eu.nimble.service.model.ubl.catalogue.CatalogueType;
import eu.nimble.service.model.ubl.commonaggregatecomponents.GoodsItemType;
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
     * @param party
     * @param catalgoueTemplate
     */
    public void addCatalogue(PartyType party, InputStream catalgoueTemplate);

    /**
     * Registers the {@code item} to the {@code party}
     *
     * @param party
     * @param item
     */
    public void addProduct(PartyType party, GoodsItemType item);

    /**
     * Adds the new property specified by the dereferencable {@code propertyURI} with the {@code value} to the
     * item.
     *
     * @param itemId
     * @param value
     * @param propertyURI dereferencable URI of the property. The referenced property should be compatible with the
     *                    NIMBLE product category schema so that other details about the property could be retrieved.
     */
    public void addPropertyToProduct(Long itemId, String value, String propertyURI);

    /**
     * Adds the new property with the given details to the item
     *
     * @param itemId
     * @param propertyName   name of the property
     * @param value
     * @param minValue minimum value of the range, the value is specified as a range
     * @param maxValue maximum value of the range, the value is specified as a range
     * @param valueQualifier used for specifying the data type of the property.
     *                       Permitted qualifier types are STRING, NUMBER, BOOLEAN
     * @param unit           if there is a unit for the property, the short name of (e.g. m2, kg) is provided via this
     *                       parameter
     */
    public void addPropertyToProduct(Long itemId, String propertyName, String value, String minValue, String maxValue, String valueQualifier, String unit);

    /**
     * Adds the new property with a binary value to the item
     *
     * @param itemId
     * @param binaryValue string serialization of the binary object if the value of the property is obtained from a
     *                    non-dereferencable location e.g. local file system.
     * @param mimeCode mime code of related to the provided value
     * @param contentURI dereferencable URI to obtain the value for the property
     */
    public void addPropertyToProduct(Long itemId, String propertyName, String binaryValue, String mimeCode, String contentURI);
}
