package eu.nimble.service.catalogue;

import com.fasterxml.jackson.databind.ObjectMapper;
import eu.nimble.service.model.ubl.catalogue.CatalogueType;
import eu.nimble.service.model.ubl.commonaggregatecomponents.CatalogueLineType;
import eu.nimble.service.model.ubl.commonaggregatecomponents.DimensionType;
import eu.nimble.service.model.ubl.commonaggregatecomponents.ItemPropertyType;
import eu.nimble.utility.JsonSerializationUtility;
import eu.nimble.utility.persistence.JPARepositoryFactory;
import eu.nimble.utility.persistence.resource.Resource;
import eu.nimble.utility.persistence.resource.ResourcePersistenceUtility;
import eu.nimble.utility.persistence.resource.ResourceValidationUtility;
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
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import java.io.InputStream;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
@ActiveProfiles("local_dev")
@RunWith(SpringJUnit4ClassRunner.class)
public class Test03_TemplatePublishingTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private JPARepositoryFactory repoFactory;
    @Autowired
    private ResourceValidationUtility resourceValidationUtil;
    @Autowired
    private Environment environment;
    private ObjectMapper mapper = JsonSerializationUtility.getObjectMapper();

    final private String partyName = "alpCompany";
    final private String partyId = "381";
    final private String uploadMode = "replace";
    final private String uploadMode2 = "append";

    final private String productID1 = "Product_id1";
    final private String productID2 = "Product_id2";
    final private String productID4 = "Product_id4";
    final private String colorProperty = "Color";
    final private String usageProperty = "Usage";
    final private String incoterms = "DAT (Delivered at Terminal)";

    final private String contentType = "application/octet-stream";
    final private String fileName = "MDF_Raw.xlsx";
    final private String fileName2 = "MDF_Raw_MDF_Painted.xlsx";

    // these hjids are used to retrieve this catalogue lines later
    public static Long catalogueLineHjid1;
    public static Long catalogueLineHjid2;
    public static Long catalogueLineHjid3;
    public static Long catalogueLineHjid4;

    // the uuid of the created catalogue
    public static String catalogueUUID;

    /*
        The user publishes three products using the template. Their ids are:
            - Product_id1
            - Product_id2
            - Product_id3
     */
    @Test
    public void test1_uploadTemplate() throws Exception {
        InputStream is = Test03_TemplatePublishingTest.class.getResourceAsStream("/template/MDF_Raw.xlsx");
        MockMultipartFile mutipartFile = new MockMultipartFile("file", fileName, contentType, is);

        MvcResult result = this.mockMvc.perform(MockMvcRequestBuilders
                .fileUpload("/catalogue/template/upload")
                .file(mutipartFile)
                .contentType(MediaType.MULTIPART_FORM_DATA_VALUE)
                .header("Authorization", environment.getProperty("nimble.test-responder-person-id"))
                .param("uploadMode",uploadMode)
                .param("partyId",partyId)
                .param("partyName",partyName))
                .andExpect(status().isCreated()).andReturn();
        CatalogueType catalogue = mapper.readValue(result.getResponse().getContentAsString(), CatalogueType.class);
        // check catalogue line size
        Assert.assertSame(catalogue.getCatalogueLine().size(),3);
        // get catalogue lines
        CatalogueLineType catalogueLineType1 = null;
        CatalogueLineType catalogueLineType2 = null;
        CatalogueLineType catalogueLineType3 = null;
        for(CatalogueLineType catalogueLineType : catalogue.getCatalogueLine()){
            if(catalogueLineType.getID().equals(productID1)){
                catalogueLineType1 = catalogueLineType;
            }
            else if(catalogueLineType.getID().equals(productID2)){
                catalogueLineType2 = catalogueLineType;
            }
            else {
                catalogueLineType3 = catalogueLineType;
            }
        }
        // check extra properties
        boolean colorPropertyExists = false;
        for (ItemPropertyType itemPropertyType :catalogueLineType1.getGoodsItem().getItem().getAdditionalItemProperty()){
            if(itemPropertyType.getName().get(0).getValue().equals(colorProperty)){
                Assert.assertSame(2,itemPropertyType.getValue().size());
                colorPropertyExists = true;
                break;
            }
        }
        Assert.assertSame(colorPropertyExists,true);

        boolean usagePropertyExists = false;
        for (ItemPropertyType itemPropertyType :catalogueLineType2.getGoodsItem().getItem().getAdditionalItemProperty()){
            if(itemPropertyType.getName().get(0).getValue().equals(usageProperty)){
                Assert.assertSame(1,itemPropertyType.getValue().size());
                usagePropertyExists = true;
                break;
            }
        }
        Assert.assertSame(usagePropertyExists,true);
        // check incoterms
        Assert.assertSame(true,catalogueLineType3.getGoodsItem().getDeliveryTerms().getIncoterms().equals(incoterms));

        boolean checkEntityIds = Boolean.valueOf(environment.getProperty("nimble.check-entity-ids"));
        if(checkEntityIds) {
            // check that resources have been managed properly
            List<Resource> allResources = ResourcePersistenceUtility.getAllResources();
            Set<Long> catalogueIds = resourceValidationUtil.extractAllHjidsExcludingPartyRelatedOnes(catalogue);

            Set<Long> managedIds = new HashSet<>();
            for (Resource resource : allResources) {
                managedIds.add(resource.getEntityId());
            }
            Assert.assertTrue("Managed ids do not contain the catalogue ids", managedIds.containsAll(catalogueIds));
        }

        // get hjids of the catalogue lines
        Test03_TemplatePublishingTest.catalogueLineHjid1 = catalogueLineType1.getHjid();
        Test03_TemplatePublishingTest.catalogueLineHjid2 = catalogueLineType2.getHjid();
        Test03_TemplatePublishingTest.catalogueLineHjid3 = catalogueLineType3.getHjid();
    }

    /*
        The user publishes two products using the template. The ids are:
            - Product_id2 : Since we have the product with this id, we will replace it with the new product.
                * The custom properties of this product are removed in the new template.
                * Dimension-Length is increased to 70 mm in the new template
                * Price amount is increased to 20 EUR in the new template.
            - Product_id4 : This product will be added to the catalogue.
                * It has two product names: One for english and the other one for spanish
                * It has one additional item property
                * Price amount is 14 EUR
     */
    @Test
    public void test2_uploadTemplate() throws Exception {
        InputStream is = Test03_TemplatePublishingTest.class.getResourceAsStream("/template/MDF_Raw_new.xlsx");
        MockMultipartFile mutipartFile = new MockMultipartFile("file", fileName, contentType, is);

        MvcResult result = this.mockMvc.perform(MockMvcRequestBuilders
                .fileUpload("/catalogue/template/upload")
                .file(mutipartFile)
                .contentType(MediaType.MULTIPART_FORM_DATA_VALUE)
                .header("Authorization", environment.getProperty("nimble.test-responder-person-id"))
                .param("uploadMode",uploadMode2)
                .param("partyId",partyId)
                .param("partyName",partyName))
                .andExpect(status().isCreated()).andReturn();
        CatalogueType catalogue = mapper.readValue(result.getResponse().getContentAsString(), CatalogueType.class);
        // check catalogue line size
        Assert.assertSame(catalogue.getCatalogueLine().size(),4);
        // get catalogue lines
        // catalogue line with id : Product_id2
        CatalogueLineType existingCatalogueLine = null;
        // catalogue line with id : Product_id4
        CatalogueLineType newCatalogueLine = null;
        for(CatalogueLineType catalogueLineType : catalogue.getCatalogueLine()){
            if(catalogueLineType.getID().equals(productID2)){
                existingCatalogueLine = catalogueLineType;
            }
            else if(catalogueLineType.getID().equals(productID4)){
                newCatalogueLine = catalogueLineType;
            }
        }

        // check whether the existing catalogue line is updated properly or not

        // check extra properties
        Assert.assertEquals(existingCatalogueLine.getGoodsItem().getItem().getAdditionalItemProperty().size(),0);
        // check dimension-length
        DimensionType lengthDimension = null;
        for(DimensionType dimensionType :existingCatalogueLine.getGoodsItem().getItem().getDimension()){
            if(dimensionType.getAttributeID().contentEquals("Length")){
                lengthDimension = dimensionType;
            }
        }
        Assert.assertNotNull(lengthDimension);
        Assert.assertEquals(lengthDimension.getMeasure().getValue().intValue(),70);
        Assert.assertEquals(lengthDimension.getMeasure().getUnitCode(),"mm");
        // check price amount
        Assert.assertEquals(existingCatalogueLine.getRequiredItemLocationQuantity().getPrice().getPriceAmount().getValue().intValue(),20);
        Assert.assertEquals(existingCatalogueLine.getRequiredItemLocationQuantity().getPrice().getPriceAmount().getCurrencyID(),"EUR");

        // check whether the new product is added to the catalogue properly or not

        // check product names
        Assert.assertEquals(newCatalogueLine.getGoodsItem().getItem().getName().size(),2);
        // check extra properties
        Assert.assertEquals(newCatalogueLine.getGoodsItem().getItem().getAdditionalItemProperty().size(),1);
        Assert.assertEquals(newCatalogueLine.getGoodsItem().getItem().getAdditionalItemProperty().get(0).getValue().size(),1);
        // check price amount
        Assert.assertEquals(newCatalogueLine.getRequiredItemLocationQuantity().getPrice().getPriceAmount().getValue().intValue(),14);
        Assert.assertEquals(newCatalogueLine.getRequiredItemLocationQuantity().getPrice().getPriceAmount().getCurrencyID(),"EUR");

        // get hjid of the new catalogue line
        Test03_TemplatePublishingTest.catalogueLineHjid4 = newCatalogueLine.getHjid();
    }

    /*
        The user publishes one products using the template.
            - Product_id5 : It has two different categories :MDF Raw and MDF Painted
                * Since in this catalogue, other products have only MDF Raw, when we try to upload this template with replace mode,
                  we have to end up with 5 products.
     */
    @Test
    public void test3_uploadTemplate() throws Exception {
        InputStream is = Test03_TemplatePublishingTest.class.getResourceAsStream("/template/MDF_Raw_MDF_Painted.xlsx");
        MockMultipartFile mutipartFile = new MockMultipartFile("file", fileName2, contentType, is);

        MvcResult result = this.mockMvc.perform(MockMvcRequestBuilders
                .fileUpload("/catalogue/template/upload")
                .file(mutipartFile)
                .contentType(MediaType.MULTIPART_FORM_DATA_VALUE)
                .header("Authorization", environment.getProperty("nimble.test-responder-person-id"))
                .param("uploadMode",uploadMode)
                .param("partyId",partyId)
                .param("partyName",partyName))
                .andExpect(status().isCreated()).andReturn();
        CatalogueType catalogue = mapper.readValue(result.getResponse().getContentAsString(), CatalogueType.class);
        // check catalogue line size
        Assert.assertSame(catalogue.getCatalogueLine().size(),5);
        // set catalogue uuid
        Test03_TemplatePublishingTest.catalogueUUID = catalogue.getUUID();
    }
}
