package eu.nimble.service.catalogue;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import eu.nimble.service.catalogue.model.lcpa.ItemLCPAInput;
import eu.nimble.service.model.ubl.catalogue.CatalogueType;
import eu.nimble.service.model.ubl.commonaggregatecomponents.LCPAOutputType;
import eu.nimble.service.model.ubl.commonbasiccomponents.AmountType;
import eu.nimble.service.model.ubl.commonbasiccomponents.QuantityType;
import eu.nimble.utility.JsonSerializationUtility;
import org.apache.commons.io.IOUtils;
import org.junit.Assert;
import org.junit.FixMethodOrder;
import org.junit.Test;
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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
@ActiveProfiles("test")
@RunWith(SpringJUnit4ClassRunner.class)
public class Test08_LCPAControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private ObjectMapper mapper = JsonSerializationUtility.getObjectMapper();

    private static String catalogueLineHjid;

    @Test
    public void test1_postCatalogue() throws Exception {
        String catalogueJson = IOUtils.toString(Test08_LCPAControllerTest.class.getResourceAsStream("/example_catalogue_LCPA_inputs.json"));

        MockHttpServletRequestBuilder request = post("/catalogue/ubl")
                .header("Authorization", TestConfig.sellerId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(catalogueJson);
        MvcResult result = this.mockMvc.perform(request).andDo(print()).andExpect(status().isCreated()).andReturn();

        CatalogueType catalogue = mapper.readValue(result.getResponse().getContentAsString(), CatalogueType.class);
        catalogueLineHjid = catalogue.getCatalogueLine().get(0).getHjid().toString();
    }

    @Test
    public void test2_getProductsWithoutLCPAProcessing() throws Exception{
        MockHttpServletRequestBuilder request = get("/lcpa/products-with-lcpa-input")
                .header("Authorization", TestConfig.buyerId);

        MvcResult result = this.mockMvc.perform(request).andDo(print()).andExpect(status().isOk()).andReturn();
        List<ItemLCPAInput> inputs = mapper.readValue(result.getResponse().getContentAsString(),new TypeReference<List<ItemLCPAInput>>(){});
        Assert.assertSame(1,inputs.size());
        Assert.assertEquals(catalogueLineHjid,inputs.get(0).getCatalogueLineHjid());
    }

    @Test
    public void test3_addLCPAOutputData() throws Exception{
        // get LCPAOutputType
        LCPAOutputType lcpaOutputType = getLCPAOutputType();

        MockHttpServletRequestBuilder request = patch("/lcpa/add-lcpa-output")
                .header("Authorization", TestConfig.buyerId)
                .param("catalogueLineHjid",catalogueLineHjid)
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(lcpaOutputType));

        MvcResult result = this.mockMvc.perform(request).andDo(print()).andExpect(status().isOk()).andReturn();

        // try to get products without LCPA Processing
        // we should get no results since LCPA Output is added to the product
        request = get("/lcpa/products-with-lcpa-input")
                .header("Authorization", TestConfig.buyerId);

        result = this.mockMvc.perform(request).andDo(print()).andExpect(status().isOk()).andReturn();
        List<ItemLCPAInput> inputs = mapper.readValue(result.getResponse().getContentAsString(),new TypeReference<List<ItemLCPAInput>>(){});
        Assert.assertSame(0,inputs.size());
    }

    private LCPAOutputType getLCPAOutputType(){
        LCPAOutputType lcpaOutput = new LCPAOutputType();

        AmountType lifeCycleCost = new AmountType();
        lifeCycleCost.setCurrencyID("EUR");
        lifeCycleCost.setValue(BigDecimal.valueOf(20));

        AmountType operationCostsPerYear = new AmountType();
        operationCostsPerYear.setCurrencyID("EUR");
        operationCostsPerYear.setValue(BigDecimal.valueOf(55));

        QuantityType capexOpexRelation = new QuantityType();
        capexOpexRelation.setValue(BigDecimal.valueOf(223));
        capexOpexRelation.setUnitCode("EUR");

        lcpaOutput.setCapexOpexRelation(capexOpexRelation);
        lcpaOutput.setLifeCycleCost(lifeCycleCost);
        lcpaOutput.setOperationCostsPerYear(operationCostsPerYear);

        return lcpaOutput;
    }
}
