/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package eu.nimble.service.catalogue;

import eu.nimble.service.model.ubl.catalogue.CatalogueType;
import eu.nimble.utility.Configuration;
import eu.nimble.utility.HibernateUtility;
import org.apache.commons.io.IOUtils;
import org.junit.*;

/**
 *
 * @author yildiray
 */
//@FixMethodOrder(MethodSorters.NAME_ASCENDING)
//@RunWith(SpringRunner.class)
//@SpringBootTest(classes = {PersistenceConfig.class, CatalogueServiceConfig.class})
@Ignore
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
	public void test1_addCatalogueTest_MODAML() throws Exception {
		String catalogueXML = IOUtils.toString(CatalogueServiceTest.class.getResourceAsStream("/MODAML-CatalogueFullDummy.xml"));
		CatalogueServiceImpl.getInstance().addCatalogue(catalogueXML, Configuration.Standard.MODAML);
	}

	@Test
	public void test2_addCatalogueTest_UBL() throws Exception {
		String catalogueXML = IOUtils.toString(CatalogueServiceTest.class.getResourceAsStream("/UBL-CatalogueFullDummy.xml"));
		CatalogueType catalogueType = CatalogueServiceImpl.getInstance().addCatalogue(catalogueXML);
		addedCatalogueUUID = catalogueType.getUUID();
	}

	@Test
	public void test3_deleteCatalogueTest_UBL() {
		Assert.assertNotNull(addedCatalogueUUID);
		CatalogueServiceImpl.getInstance().deleteCatalogue(addedCatalogueUUID);
	}


	@Test
	public void test4_deleteCatalogueTest_MODAML() {
		CatalogueServiceImpl.getInstance().deleteCatalogue("aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa", Configuration.Standard.MODAML);
	}
}
