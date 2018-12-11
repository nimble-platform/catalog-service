package eu.nimble.service.catalogue;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
@ActiveProfiles("local_dev")
@RunWith(SpringJUnit4ClassRunner.class)
public class Test02_CatalogueLineControllerTest {

    @Autowired
    private MockMvc mockMvc;
    private static ObjectMapper mapper;
    public static String catalogueId;
    private static String catalogueLineId;

    @BeforeClass
    public static void init() {
        mapper = new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    @Test
    public void test1_addCatalogueLine() throws Exception{
        // create a catalogue first
        String catalogueJson = IOUtils.toString(Test01_CatalogueControllerTest.class.getResourceAsStream("/example_catalogue.json"));

        MockHttpServletRequestBuilder request = post("/catalogue/ubl")
                .contentType(MediaType.APPLICATION_JSON)
                .content(catalogueJson);
        MvcResult result = this.mockMvc.perform(request).andExpect(status().isCreated()).andReturn();
        CatalogueType catalogue = mapper.readValue(result.getResponse().getContentAsString(), CatalogueType.class);
        catalogueId = catalogue.getUUID();

        // add the catalogue line
        String catalogueLineJson = IOUtils.toString(Test02_CatalogueLineControllerTest.class.getResourceAsStream("/example_catalogue_line.json"));
        CatalogueLineType catalogueLine = mapper.readValue(catalogueLineJson, CatalogueLineType.class);
        catalogueLine.getGoodsItem().getItem().getCatalogueDocumentReference().setID(catalogueId);
        catalogueLineJson = mapper.writeValueAsString(catalogueLine);

        request = post("/catalogue/" + catalogue.getUUID() + "/catalogueline")
                .contentType(MediaType.APPLICATION_JSON)
                .content(catalogueLineJson);
        this.mockMvc.perform(request).andDo(print()).andExpect(status().isCreated()).andReturn();
        catalogueLineId = catalogueLine.getID();
    }

    @Test
    public void test2_getCatalogueLine() throws Exception{
        MockHttpServletRequestBuilder request = get("/catalogue/" + catalogueId + "/catalogueline/" + catalogueLineId);
        MvcResult result = this.mockMvc.perform(request).andDo(print()).andExpect(status().isOk()).andReturn();
        CatalogueLineType catalogueLine = mapper.readValue(result.getResponse().getContentAsString(), CatalogueLineType.class);
        Assert.assertEquals(catalogueLineId, catalogueLine.getID());
    }

    @Test
    public void test3_updateCatalogueLine() throws Exception{
        MockHttpServletRequestBuilder request = get("/catalogue/" + catalogueId + "/catalogueline/" + catalogueLineId);
        MvcResult result = this.mockMvc.perform(request).andReturn();
        CatalogueLineType catalogueLine = mapper.readValue(result.getResponse().getContentAsString(), CatalogueLineType.class);

        QuantityType minimumOrderQuantity = new QuantityType();
        minimumOrderQuantity.setValue(new BigDecimal(413));
        catalogueLine.setMinimumOrderQuantity(minimumOrderQuantity);

        String catalogueLineJson = mapper.writeValueAsString(catalogueLine);
        request = put("/catalogue/" + catalogueId + "/catalogueline")
                .contentType(MediaType.APPLICATION_JSON)
                .content(catalogueLineJson);
        this.mockMvc.perform(request).andDo(print()).andExpect(status().isOk()).andReturn();
    }

    @Test
    public void test4_deleteCatalogueLineById() throws Exception {
        MockHttpServletRequestBuilder request = delete("/catalogue/" + catalogueId + "/catalogueline/" + catalogueLineId);
        this.mockMvc.perform(request).andDo(print()).andExpect(status().isOk()).andReturn();
    }

    @Test
    public void test5_getJsonNonExistingCatalogueLine() throws Exception {
        MockHttpServletRequestBuilder request = get("/catalogue/" + catalogueId + "/catalogueline/" + catalogueLineId);
        this.mockMvc.perform(request).andDo(print()).andExpect(status().isNoContent()).andReturn();
    }
}
