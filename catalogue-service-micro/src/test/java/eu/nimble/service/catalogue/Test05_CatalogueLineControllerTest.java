package eu.nimble.service.catalogue;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import eu.nimble.service.catalogue.model.catalogue.CatalogueLineSortOptions;
import eu.nimble.service.model.ubl.catalogue.CatalogueType;
import eu.nimble.service.model.ubl.commonaggregatecomponents.CatalogueLineType;
import eu.nimble.service.model.ubl.commonbasiccomponents.QuantityType;
import eu.nimble.utility.JsonSerializationUtility;
import org.apache.commons.io.IOUtils;
import org.junit.*;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
@ActiveProfiles("local_dev")
@RunWith(SpringJUnit4ClassRunner.class)
public class Test05_CatalogueLineControllerTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private Environment environment;
    private static ObjectMapper mapper;
    public static String catalogueId;
    private static String catalogueLineId;

    @BeforeClass
    public static void init() {
        mapper = JsonSerializationUtility.getObjectMapper();
    }

    @Test
    public void test1_addCatalogueLine() throws Exception{
        // create a catalogue first
        String catalogueJson = IOUtils.toString(Test01_CatalogueControllerTest.class.getResourceAsStream("/example_catalogue.json"));

        MockHttpServletRequestBuilder request = post("/catalogue/ubl")
                .header("Authorization", environment.getProperty("nimble.test-responder-person-id"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(catalogueJson);
        MvcResult result = this.mockMvc.perform(request).andExpect(status().isCreated()).andReturn();
        CatalogueType catalogue = mapper.readValue(result.getResponse().getContentAsString(), CatalogueType.class);
        catalogueId = catalogue.getUUID();

        // add the catalogue line
        String catalogueLineJson = IOUtils.toString(Test05_CatalogueLineControllerTest.class.getResourceAsStream("/example_catalogue_line.json"));
        CatalogueLineType catalogueLine = mapper.readValue(catalogueLineJson, CatalogueLineType.class);
        catalogueLine.getGoodsItem().getItem().getCatalogueDocumentReference().setID(catalogueId);
        catalogueLineJson = mapper.writeValueAsString(catalogueLine);

        request = post("/catalogue/" + catalogue.getUUID() + "/catalogueline")
                .header("Authorization", environment.getProperty("nimble.test-responder-person-id"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(catalogueLineJson);
        this.mockMvc.perform(request).andDo(print()).andExpect(status().isCreated()).andReturn();
        catalogueLineId = catalogueLine.getID();
    }

    @Test
    public void test2_getCatalogueLine() throws Exception{
        MockHttpServletRequestBuilder request = get("/catalogue/" + catalogueId + "/catalogueline/" + catalogueLineId)
                .header("Authorization", environment.getProperty("nimble.test-responder-person-id"));
        MvcResult result = this.mockMvc.perform(request).andDo(print()).andExpect(status().isOk()).andReturn();
        CatalogueLineType catalogueLine = mapper.readValue(result.getResponse().getContentAsString(), CatalogueLineType.class);
        Assert.assertEquals(catalogueLineId, catalogueLine.getID());
    }

    @Test
    public void test3_updateCatalogueLine() throws Exception{
        MockHttpServletRequestBuilder request = get("/catalogue/" + catalogueId + "/catalogueline/" + catalogueLineId)
                .header("Authorization", environment.getProperty("nimble.test-responder-person-id"));
        MvcResult result = this.mockMvc.perform(request).andReturn();
        CatalogueLineType catalogueLine = mapper.readValue(result.getResponse().getContentAsString(), CatalogueLineType.class);

        QuantityType minimumOrderQuantity = new QuantityType();
        minimumOrderQuantity.setValue(new BigDecimal(413));
        catalogueLine.setMinimumOrderQuantity(minimumOrderQuantity);

        String catalogueLineJson = mapper.writeValueAsString(catalogueLine);
        request = put("/catalogue/" + catalogueId + "/catalogueline")
                .header("Authorization", environment.getProperty("nimble.test-responder-person-id"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(catalogueLineJson);
        this.mockMvc.perform(request).andDo(print()).andExpect(status().isOk()).andReturn();
    }

    @Test
    public void test4_deleteCatalogueLineById() throws Exception {
        MockHttpServletRequestBuilder request = delete("/catalogue/" + catalogueId + "/catalogueline/" + catalogueLineId)
                .header("Authorization", environment.getProperty("nimble.test-responder-person-id"));
        this.mockMvc.perform(request).andDo(print()).andExpect(status().isOk()).andReturn();
    }

    @Test
    public void test5_getJsonNonExistingCatalogueLine() throws Exception {
        MockHttpServletRequestBuilder request = get("/catalogue/" + catalogueId + "/catalogueline/" + catalogueLineId)
                .header("Authorization", environment.getProperty("nimble.test-responder-person-id"));
        this.mockMvc.perform(request).andDo(print()).andExpect(status().isNotFound()).andReturn();
    }

    @Test
    public void test6_getCatalogueLines() throws Exception{

        MockHttpServletRequestBuilder request = get("/cataloguelines")
                .header("Authorization", environment.getProperty("nimble.test-responder-person-id"))
                .param("limit","2")
                .param("offset","1")
                .param("ids" , Test03_TemplatePublishingTest.catalogueLineHjid1.toString(),Test03_TemplatePublishingTest.catalogueLineHjid2.toString(),
                        Test03_TemplatePublishingTest.catalogueLineHjid3.toString(),Test03_TemplatePublishingTest.catalogueLineHjid4.toString())
                .param("sortOption", CatalogueLineSortOptions.PRICE_HIGH_TO_LOW.toString());
        MvcResult result = this.mockMvc.perform(request).andDo(print()).andExpect(status().isOk()).andReturn();
        List<CatalogueLineType> catalogueLines = mapper.readValue(result.getResponse().getContentAsString(),new TypeReference<List<CatalogueLineType>>() {});
        Assert.assertEquals(2,catalogueLines.size());
    }

    // since no hjids are provided, we should get an empty list of catalogue lines
    @Test
    public void test7_getCatalogueLines() throws Exception{

        MockHttpServletRequestBuilder request = get("/cataloguelines")
                .header("Authorization", environment.getProperty("nimble.test-responder-person-id"))
                .param("limit","2")
                .param("offset","1")
                .param("ids","")
                .param("sortOption", CatalogueLineSortOptions.PRICE_HIGH_TO_LOW.toString());
        MvcResult result = this.mockMvc.perform(request).andDo(print()).andExpect(status().isOk()).andReturn();
        List<CatalogueLineType> catalogueLines = mapper.readValue(result.getResponse().getContentAsString(),new TypeReference<List<CatalogueLineType>>() {});
        Assert.assertEquals(0,catalogueLines.size());
    }
}
