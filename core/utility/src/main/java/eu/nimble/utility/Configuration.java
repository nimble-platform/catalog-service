/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package eu.nimble.utility;

/**
 *
 * @author yildiray
 */
public class Configuration {

	public static final String UBL_CATALOGUE_PACKAGENAME = "eu.nimble.service.model.ubl.catalogue";
	public static final String UBL_CAC_PACKAGENAME = "eu.nimble.service.model.ubl.commonaggregatecomponents";
	public static final String UBL_CBC_PACKAGENAME = "eu.nimble.service.model.ubl.commonbasiccomponents";
	public static final String UBL_DESPATCHADVICE_PACKAGENAME = "eu.nimble.service.model.ubl.despatchadvice";
	public static final String UBL_ORDER_PACKAGENAME = "eu.nimble.service.model.ubl.order";
	public static final String UBL_ORDERRESPONSESIMPLE_PACKAGENAME = "eu.nimble.service.model.ubl.orderresponsesimple";
	public static final String UBL_QUOTATION_PACKAGENAME = "eu.nimble.service.model.ubl.quotation";
	public static final String UBL_RECEIPTADVICE_PACKAGENAME = "eu.nimble.service.model.ubl.receiptadvice";
	public static final String UBL_REQUESTFORQUOTATION_PACKAGENAME = "eu.nimble.service.model.ubl.requestforquotation";
	public static final String UBL_UDT_PACKAGENAME = "oasis.names.specification.ubl.schema.xsd.unqualifieddatatypes_2";
	public static final String UBL_CCT_PACKAGENAME = "un.unece.uncefact.data.specification.corecomponenttypeschemamodule._2";
	
	public static final String MODAML_CATALOGUE_PACKAGENAME = "eu.nimble.service.model.modaml.catalogue";

	public static final String UBL_CAC_NS = "urn:oasis:names:specification:ubl:schema:xsd:CommonAggregateComponents-2";
	public static final String UBL_CBC_NS = "urn:oasis:names:specification:ubl:schema:xsd:CommonBasicComponents-2";
	public static final String UBL_CATALOGUE_NS = "urn:oasis:names:specification:ubl:schema:xsd:Catalogue-2";
	public static final String UBL_DESPATCHADVICE_NS = "urn:oasis:names:specification:ubl:schema:xsd:DespatchAdvice-2";
	public static final String UBL_ORDER_NS = "urn:oasis:names:specification:ubl:schema:xsd:Order-2";
	public static final String UBL_ORDERRESPONSESIMPLE_NS = "urn:oasis:names:specification:ubl:schema:xsd:OrderResponseSimple-2";
	public static final String UBL_QUOTATION_NS = "urn:oasis:names:specification:ubl:schema:xsd:Quotation-2";
	public static final String UBL_RECEIPTADVICE_NS = "urn:oasis:names:specification:ubl:schema:xsd:ReceiptAdvice-2";
	public static final String UBL_REQUESTFORQUOTATION_NS = "urn:oasis:names:specification:ubl:schema:xsd:RequestForQuotation-2";
	public static final String UBL_UNQUALIFIEDDATATYPES_NS = "urn:oasis:names:specification:ubl:schema:xsd:UnqualifiedDataTypes-2";
	public static final String UBL_CCT_NS = "urn:un:unece:uncefact:data:specification:CoreComponentTypeSchemaModule:2";
	public static final String UBL_CCTS_NS = "urn:un:unece:uncefact:documentation:2";

	public static final String UBL_CATALOGUE_SCHEMA = "/NIMBLE-UBL-2.1/maindoc/UBL-Catalogue-2.1.xsd";
	
	public static final String MODAML_CATALOGUE_NS = "urn:moda-ml:repository:schema:TEXCatalog";
	
	public static final String UBL_PERSISTENCE_UNIT_NAME = "ubl-data-model";
	public static final String MODAML_PERSISTENCE_UNIT_NAME = "modaml-data-model";
	public static final String UBL_PERSISTENCE_PROPERTIES_FILE_NAME = "ubl-persistence.properties";
	public static final String MODAML_PERSISTENCE_PROPERTIES_FILE_NAME = "modaml-persistence.properties";
	
	public static enum Standard { UBL, MODAML };
}
