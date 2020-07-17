package eu.nimble.service.catalogue.controller;

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
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.math.BigDecimal;
import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
@ActiveProfiles("test")
@RunWith(SpringJUnit4ClassRunner.class)
public class Test05_CatalogueLineControllerTest {

    @Autowired
    private MockMvc mockMvc;
    private static ObjectMapper mapper;
    // id of 'default' catalogue
    public static String defaultCatalogueId;
    private static String defaultCatalogueLineId;
    // id of 'test' catalogue
    public static String testCatalogueId;

    @BeforeClass
    public static void init() {
        mapper = JsonSerializationUtility.getObjectMapper();
    }

    @Test
    public void test01_addCatalogueLine() throws Exception{
        // create a catalogue first
        String catalogueJson = IOUtils.toString(Test01_CatalogueControllerTest.class.getResourceAsStream("/example_catalogue.json"));

        MockHttpServletRequestBuilder request = post("/catalogue/ubl")
                .header("Authorization", TestConfig.buyerId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(catalogueJson);
        MvcResult result = this.mockMvc.perform(request).andExpect(status().isCreated()).andReturn();
        CatalogueType catalogue = mapper.readValue(result.getResponse().getContentAsString(), CatalogueType.class);
        defaultCatalogueId = catalogue.getUUID();

        // post the other catalog of party
        catalogueJson = IOUtils.toString(Test01_CatalogueControllerTest.class.getResourceAsStream("/example_catalogue_test.json"));
        request = post("/catalogue/ubl")
                .header("Authorization", TestConfig.buyerId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(catalogueJson);
        result = this.mockMvc.perform(request).andExpect(status().isCreated()).andReturn();
        catalogue = mapper.readValue(result.getResponse().getContentAsString(), CatalogueType.class);
        testCatalogueId = catalogue.getUUID();

        // add the catalogue line
        String catalogueLineJson = IOUtils.toString(Test05_CatalogueLineControllerTest.class.getResourceAsStream("/example_catalogue_line.json"));
        CatalogueLineType catalogueLine = mapper.readValue(catalogueLineJson, CatalogueLineType.class);
        catalogueLine.getGoodsItem().getItem().getCatalogueDocumentReference().setID(defaultCatalogueId);
        catalogueLineJson = mapper.writeValueAsString(catalogueLine);

        request = post("/catalogue/" + defaultCatalogueId + "/catalogueline")
                .header("Authorization", TestConfig.buyerId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(catalogueLineJson);
        this.mockMvc.perform(request).andDo(print()).andExpect(status().isCreated()).andReturn();
        defaultCatalogueLineId = catalogueLine.getID();
    }

    @Test
    public void test02_getCatalogueLine() throws Exception{
        MockHttpServletRequestBuilder request = get("/catalogue/" + defaultCatalogueId + "/catalogueline")
                .param("lineId",defaultCatalogueLineId)
                .header("Authorization", TestConfig.buyerId);
        MvcResult result = this.mockMvc.perform(request).andDo(print()).andExpect(status().isOk()).andReturn();
        CatalogueLineType catalogueLine = mapper.readValue(result.getResponse().getContentAsString(), CatalogueLineType.class);
        Assert.assertEquals(defaultCatalogueLineId, catalogueLine.getID());
    }

    @Test
    public void test03_updateCatalogueLine() throws Exception{
        MockHttpServletRequestBuilder request = get("/catalogue/" + defaultCatalogueId + "/catalogueline")
                .param("lineId",defaultCatalogueLineId)
                .header("Authorization", TestConfig.buyerId);
        MvcResult result = this.mockMvc.perform(request).andReturn();
        CatalogueLineType catalogueLine = mapper.readValue(result.getResponse().getContentAsString(), CatalogueLineType.class);

        QuantityType minimumOrderQuantity = new QuantityType();
        minimumOrderQuantity.setValue(new BigDecimal(413));
        catalogueLine.setMinimumOrderQuantity(minimumOrderQuantity);

        String catalogueLineJson = mapper.writeValueAsString(catalogueLine);
        request = put("/catalogue/" + defaultCatalogueId + "/catalogueline")
                .header("Authorization", TestConfig.buyerId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(catalogueLineJson);
        this.mockMvc.perform(request).andDo(print()).andExpect(status().isOk()).andReturn();
    }

    @Test
    public void test04_getCatalogueLines() throws Exception{

        MockHttpServletRequestBuilder request = get("/cataloguelines")
                .header("Authorization", TestConfig.buyerId)
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
    public void test05_getCatalogueLines() throws Exception{

        MockHttpServletRequestBuilder request = get("/cataloguelines")
                .header("Authorization", TestConfig.buyerId)
                .param("limit","2")
                .param("offset","1")
                .param("ids","")
                .param("sortOption", CatalogueLineSortOptions.PRICE_HIGH_TO_LOW.toString());
        MvcResult result = this.mockMvc.perform(request).andDo(print()).andExpect(status().isOk()).andReturn();
        List<CatalogueLineType> catalogueLines = mapper.readValue(result.getResponse().getContentAsString(),new TypeReference<List<CatalogueLineType>>() {});
        Assert.assertEquals(0,catalogueLines.size());
    }

    @Test
    public void test06_getCatalogueLinesByLineItems() throws Exception{
        // get catalogue line via line items
        MockHttpServletRequestBuilder request = get("/catalogue/cataloguelines")
                .header("Authorization", TestConfig.buyerId)
                .param("catalogueUuids",defaultCatalogueId)
                .param("lineIds",defaultCatalogueLineId);
        MvcResult result = this.mockMvc.perform(request).andDo(print()).andExpect(status().isOk()).andReturn();
        List<CatalogueLineType> catalogueLines = mapper.readValue(result.getResponse().getContentAsString(),new TypeReference<List<CatalogueLineType>>() {});

        // get catalogue line directly
        request = get("/catalogue/"+ defaultCatalogueId +"/catalogueline")
                .param("lineId",defaultCatalogueLineId)
                .header("Authorization", TestConfig.buyerId);
        result = this.mockMvc.perform(request).andDo(print()).andExpect(status().isOk()).andReturn();
        CatalogueLineType catalogueLine = mapper.readValue(result.getResponse().getContentAsString(), CatalogueLineType.class);
        // check whether they are the same or not
        Assert.assertEquals(catalogueLines.get(0).getHjid(), catalogueLine.getHjid());
    }

    // update catalogue line by changing its catalog
    @Test
    public void test07_updateCatalogueLine() throws Exception{
        MockHttpServletRequestBuilder request = get("/catalogue/" + defaultCatalogueId + "/catalogueline")
                .param("lineId",defaultCatalogueLineId)
                .header("Authorization", TestConfig.buyerId);
        MvcResult result = this.mockMvc.perform(request).andReturn();

        request = put("/catalogue/" + testCatalogueId + "/catalogueline")
                .header("Authorization", TestConfig.buyerId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(result.getResponse().getContentAsString());
        this.mockMvc.perform(request).andDo(print()).andExpect(status().isOk()).andReturn();

        // make sure that old catalogue line does not exist
        request = get("/catalogue/" + defaultCatalogueId + "/catalogueline")
                .param("lineId",defaultCatalogueLineId)
                .header("Authorization", TestConfig.buyerId);
        this.mockMvc.perform(request).andExpect(status().isNotFound());
        // make sure that new catalogue line exists
        request = get("/catalogue/" + testCatalogueId + "/catalogueline")
                .param("lineId",defaultCatalogueLineId)
                .header("Authorization", TestConfig.buyerId);
        this.mockMvc.perform(request).andExpect(status().isOk());
    }

    // there are two different catalogs: default and test
    @Test
    public void test08_getAllCatalogueIdsForParty() throws Exception{
        MockHttpServletRequestBuilder request = get("/catalogue/ids/"+TestConfig.sellerId)
                .header("Authorization", TestConfig.buyerId);
        MvcResult result = this.mockMvc.perform(request).andReturn();
        List<String> catalogueIds = mapper.readValue(result.getResponse().getContentAsString(),new TypeReference<List<String>>() {});
        Assert.assertEquals(2,catalogueIds.size());
        Assert.assertEquals(true,catalogueIds.contains("default"));
        Assert.assertEquals(true,catalogueIds.contains("test"));
    }

    @Test
    public void test09_deleteCatalogueLineById() throws Exception {
        MockHttpServletRequestBuilder request = delete("/catalogue/" + testCatalogueId + "/catalogueline")
                .param("lineId",defaultCatalogueLineId)
                .header("Authorization", TestConfig.buyerId);
        this.mockMvc.perform(request).andDo(print()).andExpect(status().isOk()).andReturn();
    }

    @Test
    public void test10_getJsonNonExistingCatalogueLine() throws Exception {
        MockHttpServletRequestBuilder request = get("/catalogue/" + testCatalogueId + "/catalogueline/" + defaultCatalogueLineId)
                .header("Authorization", TestConfig.buyerId);
        this.mockMvc.perform(request).andDo(print()).andExpect(status().isNotFound()).andReturn();
    }

    @Test
    public void test11_postCatalogueLineWithInvalidId() throws Exception {
        // add the catalogue line
        String catalogueLineJson = IOUtils.toString(Test05_CatalogueLineControllerTest.class.getResourceAsStream("/example_catalogue_line_invalid_id.json"));
        CatalogueLineType catalogueLine = mapper.readValue(catalogueLineJson, CatalogueLineType.class);
        catalogueLine.getGoodsItem().getItem().getCatalogueDocumentReference().setID(defaultCatalogueId);
        catalogueLineJson = mapper.writeValueAsString(catalogueLine);

        MockHttpServletRequestBuilder request = post("/catalogue/" + defaultCatalogueId + "/catalogueline")
                .header("Authorization", TestConfig.buyerId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(catalogueLineJson);
        this.mockMvc.perform(request).andDo(print()).andExpect(status().isBadRequest()).andReturn();
    }
}
