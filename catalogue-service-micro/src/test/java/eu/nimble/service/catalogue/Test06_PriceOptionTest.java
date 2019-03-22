package eu.nimble.service.catalogue;

import com.fasterxml.jackson.databind.ObjectMapper;
import eu.nimble.service.model.ubl.commonaggregatecomponents.CatalogueLineType;
import eu.nimble.service.model.ubl.commonaggregatecomponents.PriceOptionType;
import eu.nimble.service.model.ubl.commonbasiccomponents.TextType;
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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
@ActiveProfiles("local_dev")
@RunWith(SpringJUnit4ClassRunner.class)
public class Test06_PriceOptionTest {
    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private Environment environment;
    private static ObjectMapper mapper;
    private static CatalogueLineType catalogueLine;
    private static PriceOptionType priceOption;

    @BeforeClass
    public static void init() {
        mapper = JsonSerializationUtility.getObjectMapper();
    }

    @Test
    public void test1_createPriceOption() throws Exception {
        // create a catalogue line first
        String catalogueLineJson = IOUtils.toString(Test05_CatalogueLineControllerTest.class.getResourceAsStream("/example_catalogue_line.json"));
        CatalogueLineType line = mapper.readValue(catalogueLineJson, CatalogueLineType.class);
        line.getGoodsItem().getItem().getCatalogueDocumentReference().setID(Test05_CatalogueLineControllerTest.catalogueId);
        catalogueLineJson = mapper.writeValueAsString(line);

        MockHttpServletRequestBuilder request = post("/catalogue/" + Test05_CatalogueLineControllerTest.catalogueId + "/catalogueline")
                .header("Authorization", environment.getProperty("nimble.test-token"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(catalogueLineJson);
        MvcResult result = this.mockMvc.perform(request).andReturn();

        catalogueLine = mapper.readValue(result.getResponse().getContentAsString(), CatalogueLineType.class);

        // get test price option content
        String priceOptionJson = IOUtils.toString(Test06_PriceOptionTest.class.getResourceAsStream("/example_price_option.json"));

        // post pricing option
        request = post("/catalogue/" + Test05_CatalogueLineControllerTest.catalogueId + "/catalogueline/" + line.getID() + "/price-options")
                .header("Authorization", environment.getProperty("nimble.test-token"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(priceOptionJson);
        result = this.mockMvc.perform(request).andDo(print()).andExpect(status().isCreated()).andReturn();
        priceOption = mapper.readValue(result.getResponse().getContentAsString(), PriceOptionType.class);
    }

    @Test
    public void test2_updatePriceOption() throws Exception {
        String updatedIncoterm = "DDP";
        TextType textType = new TextType();
        textType.setValue("p1v1");
        textType.setLanguageID("en");
        priceOption.getIncoterms().set(1, updatedIncoterm);
        priceOption.getAdditionalItemProperty().get(0).getValue().set(1, textType);

        MockHttpServletRequestBuilder request = put("/catalogue/" + Test05_CatalogueLineControllerTest.catalogueId + "/catalogueline/" + catalogueLine.getID() + "/price-options")
                .header("Authorization", environment.getProperty("nimble.test-token"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(priceOption));
        MvcResult result = this.mockMvc.perform(request).andDo(print()).andExpect(status().isOk()).andReturn();
        PriceOptionType updatedOption = mapper.readValue(result.getResponse().getContentAsString(), PriceOptionType.class);

        Assert.assertEquals(updatedIncoterm, updatedOption.getIncoterms().get(1));
        Assert.assertEquals(2, updatedOption.getIncoterms().size());
        Assert.assertEquals(textType.getValue(), updatedOption.getAdditionalItemProperty().get(0).getValue().get(1).getValue());
        Assert.assertEquals(2, updatedOption.getAdditionalItemProperty().get(0).getValue().size());
    }

    @Test
    public void test3_deletePriceOption() throws Exception {
        // delete the option
        MockHttpServletRequestBuilder request = delete("/catalogue/" + Test05_CatalogueLineControllerTest.catalogueId + "/catalogueline/" + catalogueLine.getID() + "/price-options/" + priceOption.getHjid())
                .header("Authorization", environment.getProperty("nimble.test-token"));
        this.mockMvc.perform(request).andDo(print()).andExpect(status().isOk()).andReturn();

        // get catalogue line
        request = get("/catalogue/" + Test05_CatalogueLineControllerTest.catalogueId + "/catalogueline/" + catalogueLine.getID())
                .header("Authorization", environment.getProperty("nimble.test-token"));
        MvcResult result = this.mockMvc.perform(request).andReturn();
        CatalogueLineType catalogueLine = mapper.readValue(result.getResponse().getContentAsString(), CatalogueLineType.class);

        Assert.assertEquals(0, catalogueLine.getPriceOption().size());
    }
}
