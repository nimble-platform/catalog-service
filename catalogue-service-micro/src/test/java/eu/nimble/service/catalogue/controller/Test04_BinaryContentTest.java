package eu.nimble.service.catalogue.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import eu.nimble.service.model.ubl.catalogue.CatalogueType;
import eu.nimble.service.model.ubl.commonaggregatecomponents.CatalogueLineType;
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
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import java.io.InputStream;
import java.util.Arrays;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
@ActiveProfiles("test")
@RunWith(SpringJUnit4ClassRunner.class)
public class Test04_BinaryContentTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper mapper;

    private final String fileName = "product_image.jpeg";
    private final String secondFileName = "second_image.jpeg";
    private final String productImageName = "a117c352-88f4-41a4-a616-1f402d9d02e0.product_image.jpg";

    final private String contentType = "application/octet-stream";
    final private String imagesZipName = "ProductImages.zip";

    private static String firstProductImageUri; // id default id test
    private static String secondProductImageUri; // id default
    private static String catalogueUuid;
    private static String catalogueId;
    private static byte[] contentOfProductImage;

    @Test
    public void test10_postJsonCatalogue() throws Exception {
        String catalogueJson = IOUtils.toString(Test01_CatalogueControllerTest.class.getResourceAsStream("/example_catalogue_binary_content.json"));

        MockHttpServletRequestBuilder request = post("/catalogue/ubl")
                .header("Authorization", TestConfig.buyerId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(catalogueJson);
        MvcResult result = this.mockMvc.perform(request).andDo(print()).andExpect(status().isCreated()).andReturn();

        CatalogueType catalogue = mapper.readValue(result.getResponse().getContentAsString(), CatalogueType.class);
        Assert.assertEquals(1, catalogue.getCatalogueLine().get(0).getGoodsItem().getItem().getProductImage().size());
        Assert.assertNotEquals("", catalogue.getCatalogueLine().get(0).getGoodsItem().getItem().getProductImage().get(0).getUri());

        firstProductImageUri = catalogue.getCatalogueLine().get(0).getGoodsItem().getItem().getProductImage().get(0).getUri();
        catalogueUuid = catalogue.getUUID();
        catalogueId = catalogue.getID();
    }

    /*
        We create a copy of the catalogue which is published previously.
        Its id will be 'test' and it is important to set uri of product image so that there will be two references to the same binary content.
     */
    @Test
    public void test11_postJsonCatalogue() throws Exception {
        String catalogueJson = IOUtils.toString(Test01_CatalogueControllerTest.class.getResourceAsStream("/example_catalogue_binary_content.json"));
        // update the catalogue's id and product image uri
        CatalogueType catalogue = mapper.readValue(catalogueJson, CatalogueType.class);
        catalogue.setID("test");
        catalogue.getCatalogueLine().get(0).getGoodsItem().getItem().getProductImage().get(0).setUri(firstProductImageUri);

        catalogueJson = mapper.writeValueAsString(catalogue);

        MockHttpServletRequestBuilder request = post("/catalogue/ubl")
                .header("Authorization", TestConfig.buyerId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(catalogueJson);
        MvcResult result = this.mockMvc.perform(request).andDo(print()).andExpect(status().isCreated()).andReturn();

        catalogue = mapper.readValue(result.getResponse().getContentAsString(), CatalogueType.class);
        Assert.assertEquals(1, catalogue.getCatalogueLine().get(0).getGoodsItem().getItem().getProductImage().size());
        Assert.assertEquals(firstProductImageUri, catalogue.getCatalogueLine().get(0).getGoodsItem().getItem().getProductImage().get(0).getUri());
    }

    @Test
    public void test12_retrieveBinaryContent() throws Exception {
        MockHttpServletRequestBuilder request = get("/binary-content")
                .header("Authorization", TestConfig.buyerId)
                .param("uri", firstProductImageUri);
        MvcResult result = this.mockMvc.perform(request).andDo(print()).andExpect(status().isOk()).andReturn();

        BinaryObjectType binaryObject = mapper.readValue(result.getResponse().getContentAsString(), BinaryObjectType.class);
        Assert.assertEquals(fileName, binaryObject.getFileName());
    }

    @Test
    public void test13_updateJsonCatalogue() throws Exception {
        // get the catalogue
        MockHttpServletRequestBuilder request = get("/catalogue/UBL/"+ catalogueUuid)
                .header("Authorization", TestConfig.buyerId);
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
                .header("Authorization", TestConfig.buyerId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(catalogue));
        result = this.mockMvc.perform(request).andDo(print()).andExpect(status().isOk()).andReturn();
        catalogue = mapper.readValue(result.getResponse().getContentAsString(), CatalogueType.class);

        Assert.assertEquals(1, catalogue.getCatalogueLine().get(0).getGoodsItem().getItem().getProductImage().size());
        Assert.assertEquals(secondFileName, catalogue.getCatalogueLine().get(0).getGoodsItem().getItem().getProductImage().get(0).getFileName());
        secondProductImageUri = catalogue.getCatalogueLine().get(0).getGoodsItem().getItem().getProductImage().get(0).getUri();
    }

    /*
        In the previous test case, we deleted the product image (whose uri is firstProductImageUri) of original catalogue.
        However, there is still a product which has an image with this uri, we should still access this binary content,
        in other words, deleting product image does not delete the content of binary object since it has more than one reference.
     */
    @Test
    public void test14_retrieveBinaryContent() throws Exception {
        MockHttpServletRequestBuilder request = get("/binary-content")
                .header("Authorization", TestConfig.buyerId)
                .param("uri", firstProductImageUri);
        this.mockMvc.perform(request).andDo(print()).andExpect(status().isOk()).andReturn();
    }

    @Test
    public void test15_retrieveBinaryContent() throws Exception {
        MockHttpServletRequestBuilder request = get("/binary-content")
                .header("Authorization", TestConfig.buyerId)
                .param("uri", secondProductImageUri);
        MvcResult result = this.mockMvc.perform(request).andDo(print()).andExpect(status().isOk()).andReturn();

        BinaryObjectType binaryObject = mapper.readValue(result.getResponse().getContentAsString(), BinaryObjectType.class);
        Assert.assertEquals(secondFileName,binaryObject.getFileName());
    }

    @Test
    public void test16_deleteImagesInsideCatalogue() throws Exception {
        MockHttpServletRequestBuilder request = get("/catalogue/delete-images")
                .param("partyId",TestConfig.buyerId)
                .param("ids",catalogueId)
                .header("Authorization", TestConfig.buyerId);
        MvcResult result = this.mockMvc.perform(request).andDo(print()).andExpect(status().isOk()).andReturn();

        // try to get product image
        request = get("/binary-content")
                .header("Authorization", TestConfig.buyerId)
                .param("uri", secondProductImageUri);
        this.mockMvc.perform(request).andDo(print()).andExpect(status().isNotFound()).andReturn();
    }

    /*
        Upload a zip package consisting of two images.
     */
    @Test
    public void test18_uploadImages() throws Exception {
        InputStream is = Test04_BinaryContentTest.class.getResourceAsStream("/images/ProductImages.zip");
        MockMultipartFile mutipartFile = new MockMultipartFile("package", imagesZipName, contentType, is);
        // upload images
        MvcResult result = this.mockMvc.perform(MockMvcRequestBuilders
                .fileUpload("/catalogue/"+catalogueId+"/image/upload")
                .file(mutipartFile)
                .param("partyId",TestConfig.buyerId)
                .contentType(MediaType.MULTIPART_FORM_DATA_VALUE)
                .header("Authorization", TestConfig.buyerId)).andExpect(status().isOk()).andReturn();

        // check the number of product images
        CatalogueType catalogue = mapper.readValue(result.getResponse().getContentAsString(), CatalogueType.class);
        Assert.assertEquals(1,catalogue.getCatalogueLine().size());
        Assert.assertEquals(2,catalogue.getCatalogueLine().get(0).getGoodsItem().getItem().getProductImage().size());
        // try to get product images
        for(BinaryObjectType binaryObjectType:catalogue.getCatalogueLine().get(0).getGoodsItem().getItem().getProductImage()){
            MockHttpServletRequestBuilder request = get("/binary-content")
                    .header("Authorization", TestConfig.buyerId)
                    .param("uri", binaryObjectType.getUri());
            this.mockMvc.perform(request).andDo(print()).andExpect(status().isOk()).andReturn();
            // save the uri and the content of the first product image
            if(binaryObjectType.getFileName().contentEquals(productImageName)){
                firstProductImageUri = binaryObjectType.getUri();
                contentOfProductImage = binaryObjectType.getValue();
            }
        }

    }

    /*
        Upload a zip package consisting of one image whose name is the same with the one of the existing product images.
        Therefore,the service should replace the old image with the new one.
     */
    @Test
    public void test19_uploadImages() throws Exception {
        InputStream is = Test04_BinaryContentTest.class.getResourceAsStream("/images/ProductImagesNew.zip");
        MockMultipartFile mutipartFile = new MockMultipartFile("package", imagesZipName, contentType, is);
        // upload images
        MvcResult result = this.mockMvc.perform(MockMvcRequestBuilders
                .fileUpload("/catalogue/"+catalogueId+"/image/upload")
                .file(mutipartFile)
                .param("partyId",TestConfig.buyerId)
                .contentType(MediaType.MULTIPART_FORM_DATA_VALUE)
                .header("Authorization", TestConfig.buyerId)).andExpect(status().isOk()).andReturn();

        // check the number of product images
        CatalogueType catalogue = mapper.readValue(result.getResponse().getContentAsString(), CatalogueType.class);
        Assert.assertEquals(1,catalogue.getCatalogueLine().size());
        Assert.assertEquals(2,catalogue.getCatalogueLine().get(0).getGoodsItem().getItem().getProductImage().size());
        // try to get original image
        MockHttpServletRequestBuilder request = get("/binary-content")
                .header("Authorization", TestConfig.buyerId)
                .param("uri", firstProductImageUri);
        this.mockMvc.perform(request).andDo(print()).andExpect(status().isNotFound()).andReturn();

        boolean isImageReplaced = false;
        // retrieve the new image
        for(BinaryObjectType binaryObjectType:catalogue.getCatalogueLine().get(0).getGoodsItem().getItem().getProductImage()){
            if(binaryObjectType.getFileName().contentEquals(productImageName)){
                request = get("/binary-content")
                        .header("Authorization", TestConfig.buyerId)
                        .param("uri", binaryObjectType.getUri());
                this.mockMvc.perform(request).andDo(print()).andExpect(status().isOk()).andReturn();
                // check whether the content is updated or not
                Assert.assertNotEquals(Arrays.toString(contentOfProductImage),Arrays.toString(binaryObjectType.getValue()));
                isImageReplaced = true;
                break;
            }
        }
        Assert.assertEquals(true,isImageReplaced);
    }

    @Test
    public void test20_deleteCatalogue() throws Exception {
        MockHttpServletRequestBuilder request = delete("/catalogue/ubl/" + catalogueUuid)
                .header("Authorization", TestConfig.buyerId);
        this.mockMvc.perform(request).andDo(print()).andExpect(status().isOk()).andReturn();

        request = get("/catalogue/ubl/" + catalogueUuid)
                .header("Authorization", TestConfig.buyerId);
        this.mockMvc.perform(request).andDo(print()).andExpect(status().isNotFound()).andReturn();
    }
}
