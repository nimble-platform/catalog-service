/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package eu.nimble.service.catalogue;

import eu.nimble.utility.Configuration;
import eu.nimble.utility.FileUtility;
import eu.nimble.utility.HibernateUtility;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

/**
 *
 * @author yildiray
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class CatalogueServiceTest {

	@BeforeClass
	public static void startH2DB() {
		HibernateUtility.startH2DB();
	}

	@AfterClass
	public static void stopH2DB() {
		HibernateUtility.stopH2DB();
	}

	@Test
	public void addCatalogueTest_MODAML() {
		String catalogueXML = FileUtility.readFile("../modaml-data-model/src/test/resources/MODAML-CatalogueFullDummy.xml");
		CatalogueServiceImpl.getInstance().addCatalogue(catalogueXML, Configuration.Standard.MODAML);
	}

	@Test
	public void addCatalogueTest_UBL() {
		String catalogueXML = FileUtility.readFile("../ubl-data-model/src/test/resources/UBL-CatalogueFullDummy.xml");
		CatalogueServiceImpl.getInstance().addCatalogue(catalogueXML, Configuration.Standard.UBL);
	}

	//@Test
	public void deleteCatalogueTest_UBL() {
		CatalogueServiceImpl.getInstance().deleteCatalogueByUUID("normalizedString", Configuration.Standard.UBL);
	}
	
	//@Test
	public void deleteCatalogueTest_MODAML() {
		CatalogueServiceImpl.getInstance().deleteCatalogueByUUID("aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa", Configuration.Standard.MODAML);
	}

}
