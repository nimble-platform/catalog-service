package eu.nimble.service.catalogue.controller;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;

/**
 * Created by suat on 10-Apr-19.
 */
@RunWith(Suite.class)
@Suite.SuiteClasses({
        Test01_CatalogueControllerTest.class,
        Test02_ProductCategoryControllerTest.class,
        Test03_TemplatePublishingTest.class,
        Test04_BinaryContentTest.class,
        Test05_CatalogueLineControllerTest.class,
        Test06_PriceOptionTest.class,
        Test07_ImportExportControllerText.class,
        Test08_LCPAControllerTest.class
})
public class TestSuite {
}
