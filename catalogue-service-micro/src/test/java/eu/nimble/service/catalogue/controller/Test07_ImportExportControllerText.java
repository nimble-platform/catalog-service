package eu.nimble.service.catalogue.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import eu.nimble.common.rest.identity.IdentityClientTypedMockConfig;
import eu.nimble.service.catalogue.CatalogueServiceImpl;
import eu.nimble.service.model.ubl.catalogue.CatalogueType;
import eu.nimble.utility.JsonSerializationUtility;
import org.apache.poi.ss.usermodel.Workbook;
import org.junit.Assert;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.lang.reflect.Method;
import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
@ActiveProfiles("test")
@RunWith(SpringJUnit4ClassRunner.class)
public class Test07_ImportExportControllerText {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private CatalogueServiceImpl catalogueService;
    private ObjectMapper mapper = JsonSerializationUtility.getObjectMapper();

    private final String workbookName1 = "MDF raw.xlsx";
    private final String workbookName2 = "MDF raw_MDF, painted.xlsx";
    private final String workbookName3 = "Road transport.xlsx";

    @Test
    public void test1_generateTemplateForCatalogue() throws Exception {
        // get the catalogue
        MockHttpServletRequestBuilder request = get("/catalogue/ubl/" + Test03_TemplatePublishingTest.catalogueUUID)
                .header("Authorization", IdentityClientTypedMockConfig.sellerPersonID);
        MvcResult result = this.mockMvc.perform(request).andDo(print()).andExpect(status().isOk()).andReturn();
        CatalogueType catalogue = mapper.readValue(result.getResponse().getContentAsString(), CatalogueType.class);
        Assert.assertEquals(Test03_TemplatePublishingTest.catalogueUUID, catalogue.getUUID());
        // get workbook - workbook name pairs
        Method method = CatalogueServiceImpl.class.getDeclaredMethod("generateTemplateForCatalogue", CatalogueType.class,String.class);
        method.setAccessible(true);
        Map<Workbook,String> workbookStringMap = (Map<Workbook,String>) method.invoke(catalogueService,catalogue,"en");
        // check the response
        Assert.assertEquals(2,workbookStringMap.size());
        Assert.assertEquals(true,workbookStringMap.values().contains(workbookName1));
        Assert.assertEquals(true,workbookStringMap.values().contains(workbookName2));
    }

    @Test
    public void test2_generateTemplateForCatalogueWithTransportService() throws Exception {
        // get the catalogue
        MockHttpServletRequestBuilder request = get("/catalogue/ubl/" + Test01_CatalogueControllerTest.createdCatalogueWithTransportServiceId)
                .header("Authorization", IdentityClientTypedMockConfig.sellerPersonID);
        MvcResult result = this.mockMvc.perform(request).andDo(print()).andExpect(status().isOk()).andReturn();
        CatalogueType catalogue = mapper.readValue(result.getResponse().getContentAsString(), CatalogueType.class);
        Assert.assertEquals(Test01_CatalogueControllerTest.createdCatalogueWithTransportServiceId, catalogue.getUUID());
        // get workbook - workbook name pairs
        Method method = CatalogueServiceImpl.class.getDeclaredMethod("generateTemplateForCatalogue", CatalogueType.class,String.class);
        method.setAccessible(true);
        Map<Workbook,String> workbookStringMap = (Map<Workbook,String>) method.invoke(catalogueService,catalogue,"en");
        // check the response
        Assert.assertEquals(1,workbookStringMap.size());
        Assert.assertTrue(workbookStringMap.containsValue(workbookName3));
    }
}
