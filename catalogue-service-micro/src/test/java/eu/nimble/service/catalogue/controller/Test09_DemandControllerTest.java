package eu.nimble.service.catalogue.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import eu.nimble.common.rest.identity.IIdentityClientTyped;
import eu.nimble.common.rest.identity.IdentityClientTypedMockConfig;
import eu.nimble.service.model.ubl.commonaggregatecomponents.CatalogueLineType;
import eu.nimble.service.model.ubl.commonaggregatecomponents.DemandType;
import eu.nimble.service.model.ubl.commonbasiccomponents.BinaryObjectType;
import eu.nimble.utility.JsonSerializationUtility;
import eu.nimble.utility.persistence.JPARepositoryFactory;
import eu.nimble.utility.persistence.binary.BinaryContentService;
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

import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
@ActiveProfiles("test")
@RunWith(SpringJUnit4ClassRunner.class)
public class Test09_DemandControllerTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private JPARepositoryFactory repoFactory;
    private BinaryContentService binaryContentService = new BinaryContentService();

    private ObjectMapper mapper = JsonSerializationUtility.getObjectMapper();

    private static Long demandHjid;

    @Test
    public void test1_createDemand() throws Exception {
        String demandJson = IOUtils.toString(Test09_DemandControllerTest.class.getResourceAsStream("/demand/example_demand.json"));

        MockHttpServletRequestBuilder request = post("/demands")
                .header("Authorization", IdentityClientTypedMockConfig.sellerPersonID)
                .contentType(MediaType.APPLICATION_JSON)
                .content(demandJson);
        MvcResult result = this.mockMvc.perform(request).andDo(print()).andExpect(status().isCreated()).andReturn();

        demandHjid = Long.parseLong(result.getResponse().getContentAsString());
        DemandType demand = repoFactory.forCatalogueRepository(true).getSingleEntityByHjid(DemandType.class, demandHjid);
        Assert.assertEquals("Demand Title", demand.getTitle().getValue());

        BinaryObjectType binaryObject = binaryContentService.retrieveContent(demand.getAdditionalDocumentReference().getAttachment().getEmbeddedDocumentBinaryObject().getUri());
        Assert.assertEquals("product_image.jpeg", binaryObject.getFileName());

        // check metadata
        Assert.assertEquals(IdentityClientTypedMockConfig.sellerPartyID, demand.getMetadata().getOwnerCompany().get(0));
        Assert.assertNotNull(demand.getMetadata().getCreationDate());
        Assert.assertNotNull(demand.getMetadata().getModificationDate());
    }

    @Test
    public void test2_updateDemand() throws Exception {
        DemandType demand = repoFactory.forCatalogueRepository().getSingleEntityByHjid(DemandType.class, demandHjid);
        String initialBinaryContentUri = demand.getAdditionalDocumentReference().getAttachment().getEmbeddedDocumentBinaryObject().getUri();
        String initialModificationDate = demand.getMetadata().getModificationDate().toString();

        String demandJson = IOUtils.toString(Test09_DemandControllerTest.class.getResourceAsStream("/demand/example_demand_update.json"));

        MockHttpServletRequestBuilder request = put("/demands/" + demandHjid)
                .header("Authorization", IdentityClientTypedMockConfig.sellerPersonID)
                .contentType(MediaType.APPLICATION_JSON)
                .content(demandJson);
        this.mockMvc.perform(request).andDo(print()).andExpect(status().isOk()).andReturn();

        demand = repoFactory.forCatalogueRepository().getSingleEntityByHjid(DemandType.class, demandHjid);
        String newBinaryContentUri = demand.getAdditionalDocumentReference().getAttachment().getEmbeddedDocumentBinaryObject().getUri();
        String newModificationDate = demand.getMetadata().getModificationDate().toString();

        Assert.assertEquals("Demand Title 2", demand.getTitle().getValue());
        Assert.assertEquals("Demand Description 2", demand.getDescription().getValue());
        Assert.assertEquals("Spain", demand.getCountry().getName().getValue());
        Assert.assertEquals("ES", demand.getCountry().getIdentificationCode().getValue());
        Assert.assertEquals("2020-12-21", demand.getDueDate().toString());
        Assert.assertEquals("product_image2.jpeg", demand.getAdditionalDocumentReference().getAttachment().getEmbeddedDocumentBinaryObject().getFileName());

        // check that the existing binary content is deleted
        BinaryObjectType binaryObject = binaryContentService.retrieveContent(initialBinaryContentUri);
        Assert.assertEquals(null, binaryObject);

        // check that the new binary content is the updated one
        binaryObject = binaryContentService.retrieveContent(newBinaryContentUri);
        Assert.assertEquals("product_image2.jpeg", binaryObject.getFileName());

        // check modification dates are different
        Assert.assertNotEquals(initialModificationDate, newModificationDate);
    }

    @Test
    public void test3_deleteDemand() throws Exception {
        MockHttpServletRequestBuilder request = delete("/demands/" + demandHjid)
                .header("Authorization", IdentityClientTypedMockConfig.sellerPersonID)
                .contentType(MediaType.APPLICATION_JSON);
        this.mockMvc.perform(request).andDo(print()).andExpect(status().isOk()).andReturn();

        DemandType demand = repoFactory.forCatalogueRepository().getSingleEntityByHjid(DemandType.class, demandHjid);
        Assert.assertEquals(null, demand);
    }

    @Test
    public void test4_getDemandsForParty() throws Exception {
        String demandJson = IOUtils.toString(Test09_DemandControllerTest.class.getResourceAsStream("/demand/example_demand.json"));
        String demandJson2 = IOUtils.toString(Test09_DemandControllerTest.class.getResourceAsStream("/demand/example_demand2.json"));

        // add first demand
        MockHttpServletRequestBuilder request = post("/demands")
                .header("Authorization", IdentityClientTypedMockConfig.sellerPersonID)
                .contentType(MediaType.APPLICATION_JSON)
                .content(demandJson);
        this.mockMvc.perform(request).andDo(print()).andExpect(status().isCreated()).andReturn();

        // add second demand
        request = post("/demands")
                .header("Authorization", IdentityClientTypedMockConfig.sellerPersonID)
                .contentType(MediaType.APPLICATION_JSON)
                .content(demandJson2);
        this.mockMvc.perform(request).andDo(print()).andExpect(status().isCreated()).andReturn();

        // get demands
        request = get("/demands")
                .param("companyId", IdentityClientTypedMockConfig.sellerPartyID)
                .header("Authorization", IdentityClientTypedMockConfig.sellerPersonID)
                .contentType(MediaType.APPLICATION_JSON);
        MvcResult result = this.mockMvc.perform(request).andDo(print()).andExpect(status().isOk()).andReturn();
        List<DemandType> demands = mapper.readValue(result.getResponse().getContentAsString(),new TypeReference<List<DemandType>>() {});

        Assert.assertEquals(2, demands.size());
        Assert.assertEquals("Demand Title 2", demands.get(0).getTitle().getValue());
    }
}
