/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package eu.nimble.service.catalogue;

import eu.nimble.service.catalogue.impl.CatalogueServiceImpl;
import eu.nimble.service.model.ubl.catalogue.CatalogueType;
import eu.nimble.utility.Configuration;
import eu.nimble.utility.FileUtility;
import eu.nimble.utility.HibernateUtility;
import org.junit.*;
import org.junit.runners.MethodSorters;

/**
 *
 * @author yildiray
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class CatalogueServiceTest {

	private static String addedCatalogueUUID;

	@BeforeClass
	public static void startH2DB() {
		HibernateUtility.startH2DB();
	}

	@AfterClass
	public static void stopH2DB() {
		HibernateUtility.stopH2DB();
	}

	@Test
	public void test1_addCatalogueTest_MODAML() {
		String catalogueXML = FileUtility.readFile("../modaml-data-model/src/test/resources/MODAML-CatalogueFullDummy.xml");
		CatalogueServiceImpl.getInstance().addCatalogue(catalogueXML, null, Configuration.Standard.MODAML);
	}

	@Test
	public void test2_addCatalogueTest_UBL() {
		String catalogueXML = FileUtility.readFile("../ubl-data-model/src/test/resources/UBL-CatalogueFullDummy.xml");
		CatalogueType catalogueType = CatalogueServiceImpl.getInstance().addCatalogue(catalogueXML, null);
		addedCatalogueUUID = catalogueType.getUUID().getValue();
	}

	@Test
	public void test3_deleteCatalogueTest_UBL() {
		CatalogueServiceImpl.getInstance().deleteCatalogue(addedCatalogueUUID);
	}


	@Test
	public void test4_deleteCatalogueTest_MODAML() {
		CatalogueServiceImpl.getInstance().deleteCatalogue("aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa", Configuration.Standard.MODAML);
	}

}
