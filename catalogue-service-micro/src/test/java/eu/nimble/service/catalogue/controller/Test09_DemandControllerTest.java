package eu.nimble.service.catalogue.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import eu.nimble.common.rest.identity.IIdentityClientTyped;
import eu.nimble.common.rest.identity.IdentityClientTypedMockConfig;
import eu.nimble.service.model.ubl.catalogue.CatalogueType;
import eu.nimble.service.model.ubl.commonaggregatecomponents.CatalogueLineType;
import eu.nimble.service.model.ubl.commonaggregatecomponents.DemandType;
import eu.nimble.service.model.ubl.commonaggregatecomponents.MetadataType;
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

import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
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
        String demandJson = IOUtils.toString(Test09_DemandControllerTest.class.getResourceAsStream("/demand/demand.json"));

        MockHttpServletRequestBuilder request = post("/demands")
                .header("Authorization", IdentityClientTypedMockConfig.sellerPersonID)
                .contentType(MediaType.APPLICATION_JSON)
                .content(demandJson);
        MvcResult result = this.mockMvc.perform(request).andDo(print()).andExpect(status().isCreated()).andReturn();

        demandHjid = Long.parseLong(result.getResponse().getContentAsString());
        DemandType demand = repoFactory.forCatalogueRepository(true).getSingleEntityByHjid(DemandType.class, demandHjid);
        Assert.assertEquals("Demand Title", demand.getTitle().get(0).getValue());

        BinaryObjectType binaryObject = binaryContentService.retrieveContent(demand.getAdditionalDocumentReference().getAttachment().getEmbeddedDocumentBinaryObject().getUri());
        Assert.assertEquals("product_image.jpeg", binaryObject.getFileName());

        // check metadata
        Assert.assertEquals(IdentityClientTypedMockConfig.sellerPartyID, demand.getMetadata().getOwnerCompany().get(0));
        Assert.assertNotNull(demand.getMetadata().getCreationDate());
        Assert.assertNotNull(demand.getMetadata().getModificationDate());
    }

    @Test
    public void test2_updateDemand() throws Exception {
        DemandType existingDemand = repoFactory.forCatalogueRepository().getSingleEntityByHjid(DemandType.class, demandHjid);
        String initialBinaryContentUri = existingDemand.getAdditionalDocumentReference().getAttachment().getEmbeddedDocumentBinaryObject().getUri();
        XMLGregorianCalendar initialModificationDate = existingDemand.getMetadata().getModificationDate();

        String updateDemandJson = IOUtils.toString(Test09_DemandControllerTest.class.getResourceAsStream("/demand/demand_update_without_metadata.json"));
        DemandType updateDemand = mapper.readValue(updateDemandJson, DemandType.class);

        // try to update demand without sending any included metadata
        MockHttpServletRequestBuilder request = put("/demands/" + demandHjid)
                .header("Authorization", IdentityClientTypedMockConfig.sellerPersonID)
                .contentType(MediaType.APPLICATION_JSON)
                .content(updateDemandJson);
        this.mockMvc.perform(request).andDo(print()).andExpect(status().isBadRequest()).andReturn();

        // try to update demand with metadata including an incorrect modification date
        GregorianCalendar cal = new GregorianCalendar();
        cal.setTime(new Date(1));
        XMLGregorianCalendar date = DatatypeFactory.newInstance().newXMLGregorianCalendar(cal);
        MetadataType metadata = new MetadataType();
        metadata.setModificationDate(date);
        updateDemand.setMetadata(metadata);
        updateDemandJson = JsonSerializationUtility.getObjectMapper(4).writeValueAsString(updateDemand);
        request = put("/demands/" + demandHjid)
                .header("Authorization", IdentityClientTypedMockConfig.sellerPersonID)
                .contentType(MediaType.APPLICATION_JSON)
                .content(updateDemandJson);
        this.mockMvc.perform(request).andDo(print()).andExpect(status().isBadRequest()).andReturn();

        // update with the correct modification date
        updateDemand.getMetadata().setModificationDate(initialModificationDate);
        updateDemandJson = JsonSerializationUtility.getObjectMapper(4).writeValueAsString(updateDemand);
        request = put("/demands/" + demandHjid)
                .header("Authorization", IdentityClientTypedMockConfig.sellerPersonID)
                .contentType(MediaType.APPLICATION_JSON)
                .content(updateDemandJson);
        this.mockMvc.perform(request).andDo(print()).andExpect(status().isOk()).andReturn();

        existingDemand = repoFactory.forCatalogueRepository(true).getSingleEntityByHjid(DemandType.class, demandHjid);
        String newBinaryContentUri = existingDemand.getAdditionalDocumentReference().getAttachment().getEmbeddedDocumentBinaryObject().getUri();
        String newModificationDate = existingDemand.getMetadata().getModificationDate().toString();

        Assert.assertEquals(1, existingDemand.getTitle().size());
        Assert.assertEquals("Demand Title up", existingDemand.getTitle().get(0).getValue());
        Assert.assertEquals("Demand Description 2", existingDemand.getDescription().get(0).getValue());
        Assert.assertEquals("Spain", existingDemand.getCountry().getName().getValue());
        Assert.assertEquals("ES", existingDemand.getCountry().getIdentificationCode().getValue());
        Assert.assertEquals("2020-12-21", existingDemand.getDueDate().toString());
        Assert.assertEquals("product_image2.jpeg", existingDemand.getAdditionalDocumentReference().getAttachment().getEmbeddedDocumentBinaryObject().getFileName());

        // check that the existing binary content is deleted
        BinaryObjectType binaryObject = binaryContentService.retrieveContent(initialBinaryContentUri);
        Assert.assertEquals(null, binaryObject);

        // check that the new binary content is the updated one
        binaryObject = binaryContentService.retrieveContent(newBinaryContentUri);
        Assert.assertEquals("product_image2.jpeg", binaryObject.getFileName());

        // check modification dates are different
        Assert.assertNotEquals(initialModificationDate.toString(), newModificationDate);
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
        String demandJson = IOUtils.toString(Test09_DemandControllerTest.class.getResourceAsStream("/demand/demand.json"));
        String demandJson2 = IOUtils.toString(Test09_DemandControllerTest.class.getResourceAsStream("/demand/demand2.json"));

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
        Assert.assertEquals("Demand Title 2", demands.get(0).getTitle().get(0).getValue());
    }
}
