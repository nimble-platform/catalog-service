package eu.nimble.service.catalogue;

import com.fasterxml.jackson.databind.ObjectMapper;
import eu.nimble.service.model.ubl.catalogue.CatalogueType;
import eu.nimble.service.model.ubl.commonbasiccomponents.BinaryObjectType;
import org.apache.commons.io.IOUtils;
import org.junit.Assert;
import org.junit.FixMethodOrder;
import org.junit.Test;
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
public class Test06_BinaryContentTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private Environment environment;
    @Autowired
    private ObjectMapper mapper;

    private final String fileName = "product_image.jpeg";
    private final String secondFileName = "second_image.jpeg";

    private static String firstProductImageUri;
    private static String secondProductImageUri;
    private static String catalogueUuid;

    @Test
    public void test10_postJsonCatalogue() throws Exception {
        String catalogueJson = IOUtils.toString(Test01_CatalogueControllerTest.class.getResourceAsStream("/example_catalogue_binary_content.json"));

        MockHttpServletRequestBuilder request = post("/catalogue/ubl")
                .header("Authorization", environment.getProperty("nimble.test-token"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(catalogueJson);
        MvcResult result = this.mockMvc.perform(request).andDo(print()).andExpect(status().isCreated()).andReturn();

        CatalogueType catalogue = mapper.readValue(result.getResponse().getContentAsString(), CatalogueType.class);
        Assert.assertEquals(1, catalogue.getCatalogueLine().get(0).getGoodsItem().getItem().getProductImage().size());
        Assert.assertNotEquals("", catalogue.getCatalogueLine().get(0).getGoodsItem().getItem().getProductImage().get(0).getUri());

        firstProductImageUri = catalogue.getCatalogueLine().get(0).getGoodsItem().getItem().getProductImage().get(0).getUri();
        catalogueUuid = catalogue.getUUID();
    }

    @Test
    public void test11_retrieveBinaryContent() throws Exception {
        MockHttpServletRequestBuilder request = get("/binary-content")
                .header("Authorization", environment.getProperty("nimble.test-token"))
                .param("uri", firstProductImageUri);
        MvcResult result = this.mockMvc.perform(request).andDo(print()).andExpect(status().isOk()).andReturn();

        BinaryObjectType binaryObject = mapper.readValue(result.getResponse().getContentAsString(), BinaryObjectType.class);
        Assert.assertEquals(fileName, binaryObject.getFileName());
    }

    @Test
    public void test12_updateJsonCatalogue() throws Exception {
        // get the catalogue
        MockHttpServletRequestBuilder request = get("/catalogue/UBL/"+ catalogueUuid)
                .header("Authorization", environment.getProperty("nimble.test-token"));
        MvcResult result = this.mockMvc.perform(request).andDo(print()).andExpect(status().isOk()).andReturn();
        CatalogueType catalogue = mapper.readValue(result.getResponse().getContentAsString(), CatalogueType.class);

        // create a binary object
        BinaryObjectType binaryObject = new BinaryObjectType();
        binaryObject.setValue(catalogue.getCatalogueLine().get(0).getGoodsItem().getItem().getProductImage().get(0).getValue());
        binaryObject.setFileName("second_image.jpeg");
        binaryObject.setMimeCode("image/jpeg");

        // update the catalogue by adding the new binary content and removing the old one
        catalogue.getCatalogueLine().get(0).getGoodsItem().getItem().getProductImage().clear();
        catalogue.getCatalogueLine().get(0).getGoodsItem().getItem().getProductImage().add(binaryObject);

        request = put("/catalogue/UBL")
                .header("Authorization", environment.getProperty("nimble.test-token"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(catalogue));
        result = this.mockMvc.perform(request).andDo(print()).andExpect(status().isOk()).andReturn();
        catalogue = mapper.readValue(result.getResponse().getContentAsString(), CatalogueType.class);

        Assert.assertEquals(1, catalogue.getCatalogueLine().get(0).getGoodsItem().getItem().getProductImage().size());
        Assert.assertEquals(secondFileName, catalogue.getCatalogueLine().get(0).getGoodsItem().getItem().getProductImage().get(0).getFileName());
        secondProductImageUri = catalogue.getCatalogueLine().get(0).getGoodsItem().getItem().getProductImage().get(0).getUri();
    }

    @Test
    public void test13_retrieveBinaryContentNotFound() throws Exception {
        MockHttpServletRequestBuilder request = get("/binary-content")
                .header("Authorization", environment.getProperty("nimble.test-token"))
                .param("uri", firstProductImageUri);
        MvcResult result = this.mockMvc.perform(request).andDo(print()).andExpect(status().isNoContent()).andReturn();
    }

    @Test
    public void test14_retrieveBinaryContent() throws Exception {
        MockHttpServletRequestBuilder request = get("/binary-content")
                .header("Authorization", environment.getProperty("nimble.test-token"))
                .param("uri", secondProductImageUri);
        MvcResult result = this.mockMvc.perform(request).andDo(print()).andExpect(status().isOk()).andReturn();

        BinaryObjectType binaryObject = mapper.readValue(result.getResponse().getContentAsString(), BinaryObjectType.class);
        Assert.assertEquals(secondFileName,binaryObject.getFileName());
    }

    @Test
    public void test15_deleteCatalogue() throws Exception {
        MockHttpServletRequestBuilder request = delete("/catalogue/ubl/" + catalogueUuid)
                .header("Authorization", environment.getProperty("nimble.test-token"));
        this.mockMvc.perform(request).andDo(print()).andExpect(status().isOk()).andReturn();

        request = get("/catalogue/ubl/" + catalogueUuid)
                .header("Authorization", environment.getProperty("nimble.test-token"));
        this.mockMvc.perform(request).andDo(print()).andExpect(status().isNoContent()).andReturn();
    }
}
