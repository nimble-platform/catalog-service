package eu.nimble.service.catalogue;

import com.fasterxml.jackson.databind.ObjectMapper;
import eu.nimble.service.model.ubl.catalogue.CatalogueType;
import eu.nimble.service.model.ubl.commonaggregatecomponents.CatalogueLineType;
import eu.nimble.service.model.ubl.commonaggregatecomponents.ItemPropertyType;
import eu.nimble.service.model.ubl.commonaggregatecomponents.ResourceType;
import eu.nimble.utility.JsonSerializationUtility;
import eu.nimble.utility.persistence.JPARepositoryFactory;
import eu.nimble.utility.persistence.resource.ResourceValidationUtil;
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
public class Test05_TemplatePublishingTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private JPARepositoryFactory repoFactory;
    @Autowired
    private ResourceValidationUtil resourceValidationUtil;
    @Autowired
    private Environment environment;
    private ObjectMapper mapper = JsonSerializationUtility.getObjectMapper();

    final private String partyName = "alpCompany";
    final private String partyId = "381";
    final private String uploadMode = "replace";

    final private String productID1 = "Product_id1";
    final private String productID2 = "Product_id2";
    final private String colorProperty = "Color";
    final private String usageProperty = "Usage";
    final private String incoterms = "DAT (Delivered at Terminal)";

    final private String contentType = "application/octet-stream";
    final private String fileName = "product_data_template.xlsx";

    @Test
    public void test1_uploadTemplate() throws Exception {
        InputStream is = Test05_TemplatePublishingTest.class.getResourceAsStream("/template/product_data_template.xlsx");
        MockMultipartFile mutipartFile = new MockMultipartFile("file", fileName, contentType, is);

        MvcResult result = this.mockMvc.perform(MockMvcRequestBuilders
                .fileUpload("/catalogue/template/upload")
                .file(mutipartFile)
                .contentType(MediaType.MULTIPART_FORM_DATA_VALUE)
                .header("Authorization", environment.getProperty("nimble.test-token"))
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
            if(itemPropertyType.getName().equals(colorProperty)){
                Assert.assertSame(2,itemPropertyType.getValue().size());
                colorPropertyExists = true;
                break;
            }
        }
        Assert.assertSame(colorPropertyExists,true);

        boolean usagePropertyExists = false;
        for (ItemPropertyType itemPropertyType :catalogueLineType2.getGoodsItem().getItem().getAdditionalItemProperty()){
            if(itemPropertyType.getName().equals(usageProperty)){
                Assert.assertSame(1,itemPropertyType.getValue().size());
                usagePropertyExists = true;
                break;
            }
        }
        Assert.assertSame(usagePropertyExists,true);
        // check incoterms
        Assert.assertSame(true,catalogueLineType3.getGoodsItem().getDeliveryTerms().getIncoterms().equals(incoterms));

        // check that resources have been managed properly
        List<ResourceType> allResources = repoFactory.forCatalogueRepository().getEntities(ResourceType.class);
        Set<Long> catalogueIds = resourceValidationUtil.extractAllHjidsExcludingPartyRelatedOnes(catalogue);

        Set<Long> managedIds = new HashSet<>();
        for(ResourceType resource : allResources) {
            managedIds.add(resource.getEntityID());
        }
        Assert.assertTrue("Managed ids do not contain the catalogue ids", managedIds.containsAll(catalogueIds));
    }

}
