package eu.nimble.utility;

import java.util.Iterator;
import javax.xml.XMLConstants;
import javax.xml.namespace.NamespaceContext;

public class NamespaceResolver implements NamespaceContext {
	/**
	 * This method returns the uri for all prefixes needed. Wherever
	 * possible it uses XMLConstants.
	 *
	 * @param prefix
	 * @return uri
	 */
	public String getNamespaceURI(String prefix) {
		if (prefix == null) {
			throw new IllegalArgumentException("No prefix provided!");
		//} else if (prefix.equals(XMLConstants.DEFAULT_NS_PREFIX)) {
		//	return "http://www.opengis.net/om/2.0";
		} else if (prefix.equals("ublcac")) {
			return Configuration.UBL_CAC_NS;
		} else if (prefix.equals("ublcbc")) {
			return Configuration.UBL_CBC_NS;
		} else if (prefix.equals("ublcatalogue")) {
			return Configuration.UBL_CATALOGUE_NS;
		} else if (prefix.equals("ubldespatchadvice")) {
			return Configuration.UBL_DESPATCHADVICE_NS;
		} else if (prefix.equals("ublorderresponsesimple")) {
			return Configuration.UBL_ORDERRESPONSESIMPLE_NS;
		} else if (prefix.equals("ublquotation")) {
			return Configuration.UBL_QUOTATION_NS;
		} else if (prefix.equals("ublreceiptadvice")) {
			return Configuration.UBL_RECEIPTADVICE_NS;
		} else if (prefix.equals("ublrequestforquotation")) {
			return Configuration.UBL_REQUESTFORQUOTATION_NS;
		} else if (prefix.equals("udt")) {
			return Configuration.UBL_UNQUALIFIEDDATATYPES_NS;
		} else if (prefix.equals("ccts-cct")) {
			return Configuration.UBL_CCT_NS;
		} else if (prefix.equals("ccts")) {
			return Configuration.UBL_CCTS_NS;
		} else if (prefix.equals("modamlcatalogue")) {
			return Configuration.MODAML_CATALOGUE_NS;
		} else {
			return XMLConstants.NULL_NS_URI;
		}
	}

	public String getPrefix(String namespaceURI) {
		// Not needed in this context.
		throw new UnsupportedOperationException();
	}

	public Iterator getPrefixes(String namespaceURI) {
		// Not needed in this context.
		throw new UnsupportedOperationException();
	}
}
