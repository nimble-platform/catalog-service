package eu.nimble.service.catalogue;

import com.fasterxml.jackson.databind.ObjectMapper;
import eu.nimble.service.model.ubl.catalogue.CatalogueType;
import eu.nimble.service.model.ubl.commonbasiccomponents.TextType;
import eu.nimble.service.model.ubl.commonaggregatecomponents.PartyType;
import eu.nimble.utility.JsonSerializationUtility;
import eu.nimble.utility.persistence.JPARepositoryFactory;
import eu.nimble.utility.persistence.resource.Resource;
import eu.nimble.utility.persistence.resource.ResourcePersistenceUtility;
import eu.nimble.utility.persistence.resource.ResourceValidationUtility;
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

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
@ActiveProfiles("local_dev")
@RunWith(SpringJUnit4ClassRunner.class)
public class Test01_CatalogueControllerTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private JPARepositoryFactory repoFactory;
    @Autowired
    private ResourceValidationUtility resourceValidationUtil;
    @Autowired
    private Environment environment;

    private ObjectMapper mapper = JsonSerializationUtility.getObjectMapper();
    private static String createdCatalogueId;

    @Test
    public void test10_postJsonCatalogue() throws Exception {
        String catalogueJson = IOUtils.toString(Test01_CatalogueControllerTest.class.getResourceAsStream("/example_catalogue.json"));

        MockHttpServletRequestBuilder request = post("/catalogue/ubl")
                .header("Authorization", environment.getProperty("nimble.test-token"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(catalogueJson);
        MvcResult result = this.mockMvc.perform(request).andDo(print()).andExpect(status().isCreated()).andReturn();

        CatalogueType catalogue = mapper.readValue(result.getResponse().getContentAsString(), CatalogueType.class);
        createdCatalogueId = catalogue.getUUID();

        boolean checkEntityIds = Boolean.valueOf(environment.getProperty("nimble.check-entity-ids"));
        if(checkEntityIds) {
            // check that resources have been managed properly
            List<Resource> allResources = ResourcePersistenceUtility.getAllResources();
            Set<Long> catalogueIds = resourceValidationUtil.extractAllHjidsExcludingPartyRelatedOnes(catalogue);

            Assert.assertEquals("Resource numbers and managed id sizes do not match", allResources.size(), catalogueIds.size());
            Set<Long> managedIds = new HashSet<>();
            for(Resource resource : allResources) {
                managedIds.add(resource.getEntityId());
            }
            Assert.assertTrue("Managed ids and catalogue ids do not match", managedIds.containsAll(catalogueIds) && catalogueIds.containsAll(managedIds));
        }

        // check that only a single party instance is created
        List<PartyType> parties = repoFactory.forCatalogueRepository().getEntities(PartyType.class);
        Assert.assertEquals(1, parties.size());
    }

    @Test
    public void test11_postExistingCatalogue() throws Exception {
        String catalogueJson = IOUtils.toString(Test01_CatalogueControllerTest.class.getResourceAsStream("/example_catalogue.json"));

        MockHttpServletRequestBuilder request = post("/catalogue/ubl")
                .header("Authorization", environment.getProperty("nimble.test-token"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(catalogueJson);
        this.mockMvc.perform(request).andDo(print()).andExpect(status().isConflict()).andReturn();
    }

    @Test
    public void test2_getJsonCatalogue() throws Exception {
        MockHttpServletRequestBuilder request = get("/catalogue/ubl/" + createdCatalogueId)
                .header("Authorization", environment.getProperty("nimble.test-token"));
        MvcResult result = this.mockMvc.perform(request).andDo(print()).andExpect(status().isOk()).andReturn();
        CatalogueType catalogue = mapper.readValue(result.getResponse().getContentAsString(), CatalogueType.class);
        Assert.assertEquals(createdCatalogueId, catalogue.getUUID());

        // check that only a single party instance is created
        List<PartyType> parties = repoFactory.forCatalogueRepository().getEntities(PartyType.class);
        Assert.assertEquals(1, parties.size());
    }

    @Test
    public void test31_updateJsonCatalogue() throws Exception {
        MockHttpServletRequestBuilder request = get("/catalogue/ubl/" + createdCatalogueId)
                .header("Authorization", environment.getProperty("nimble.test-token"));
        MvcResult result = this.mockMvc.perform(request).andReturn();
        CatalogueType catalogue = mapper.readValue(result.getResponse().getContentAsString(), CatalogueType.class);

        // update party address
        TextType textType = new TextType();
        textType.setValue("Updated product name");
        textType.setLanguageID("en");
        catalogue.getCatalogueLine().get(0).getGoodsItem().getItem().setName(Arrays.asList(textType));

        // get Json version of the updated catalogue
        String catalogueTypeAsString = mapper.writeValueAsString(catalogue);

        // make request
        request = put("/catalogue/ubl")
                .header("Authorization", environment.getProperty("nimble.test-token"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(catalogueTypeAsString);
        result = this.mockMvc.perform(request).andDo(print()).andExpect(status().isOk()).andReturn();
        catalogue = mapper.readValue(result.getResponse().getContentAsString(), CatalogueType.class);

        // check whether it is updated or not
        Assert.assertEquals("Updated product name", catalogue.getCatalogueLine().get(0).getGoodsItem().getItem().getName().get(0).getValue());

        boolean checkEntityIds = Boolean.valueOf(environment.getProperty("nimble.check-entity-ids"));
        if(checkEntityIds) {
            // check that resources have been managed properly
            List<Resource> allResources = ResourcePersistenceUtility.getAllResources();
            Set<Long> catalogueIds = resourceValidationUtil.extractAllHjidsExcludingPartyRelatedOnes(catalogue);

            Assert.assertEquals("Resource numbers and managed id sizes do not match", allResources.size(), catalogueIds.size());
            Set<Long> managedIds = new HashSet<>();
            for (Resource resource : allResources) {
                managedIds.add(resource.getEntityId());
            }
            Assert.assertTrue("Managed ids and catalogue ids do not match", managedIds.containsAll(catalogueIds) && catalogueIds.containsAll(managedIds));
        }

        // check that only a single party instance is created
        List<PartyType> parties = repoFactory.forCatalogueRepository().getEntities(PartyType.class);
        Assert.assertEquals(1, parties.size());
    }

    @Test
    public void test32_updateJsonCatalogue() throws Exception {
        MockHttpServletRequestBuilder request = get("/catalogue/ubl/" + createdCatalogueId)
                .header("Authorization", environment.getProperty("nimble.test-token"));
        MvcResult result = this.mockMvc.perform(request).andReturn();
        CatalogueType catalogue = mapper.readValue(result.getResponse().getContentAsString(), CatalogueType.class);


        // get Json version of the updated catalogue
        String catalogueTypeAsString = mapper.writeValueAsString(catalogue);

        // make request
        request = put("/catalogue/ubl")
                .header("Authorization", environment.getProperty("nimble.test-token"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(catalogueTypeAsString);
        result = this.mockMvc.perform(request).andDo(print()).andExpect(status().isOk()).andReturn();
        catalogue = mapper.readValue(result.getResponse().getContentAsString(), CatalogueType.class);

        // check whether it is updated or not
        Assert.assertEquals("Updated product name", catalogue.getCatalogueLine().get(0).getGoodsItem().getItem().getName().get(0).getValue());

        boolean checkEntityIds = Boolean.valueOf(environment.getProperty("nimble.check-entity-ids"));
        if(checkEntityIds) {
            // check that resources have been managed properly
            List<Resource> allResources = ResourcePersistenceUtility.getAllResources();
            Set<Long> catalogueIds = resourceValidationUtil.extractAllHjidsExcludingPartyRelatedOnes(catalogue);

            Assert.assertEquals("Resource numbers and managed id sizes do not match", allResources.size(), catalogueIds.size());
            Set<Long> managedIds = new HashSet<>();
            for (Resource resource : allResources) {
                managedIds.add(resource.getEntityId());
            }
            Assert.assertTrue("Managed ids and catalogue ids do not match", managedIds.containsAll(catalogueIds) && catalogueIds.containsAll(managedIds));
        }

        // check that only a single party instance is created
        List<PartyType> parties = repoFactory.forCatalogueRepository().getEntities(PartyType.class);
        Assert.assertEquals(1, parties.size());
    }

    @Test
    public void test4_deleteCatalogue() throws Exception {
        MockHttpServletRequestBuilder request = delete("/catalogue/ubl/" + createdCatalogueId)
                .header("Authorization", environment.getProperty("nimble.test-token"));
        this.mockMvc.perform(request).andDo(print()).andExpect(status().isOk()).andReturn();

        request = get("/catalogue/ubl/" + createdCatalogueId)
                .header("Authorization", environment.getProperty("nimble.test-token"));
        this.mockMvc.perform(request).andDo(print()).andExpect(status().isNotFound()).andReturn();
    }

    @Test
    public void test50_getJsonNonExistingCatalogue() throws Exception {
        MockHttpServletRequestBuilder request = get("/catalogue/ubl/" + createdCatalogueId)
                .header("Authorization", environment.getProperty("nimble.test-token"));
        this.mockMvc.perform(request).andDo(print()).andExpect(status().isNotFound()).andReturn();
    }

    /*
    * Tests for catalogue with item properties
    * */
    @Test
    public void test51_postJsonCatalogue() throws Exception {
        String catalogueJson = IOUtils.toString(Test01_CatalogueControllerTest.class.getResourceAsStream("/example_catalogue_item_property.json"));

        MockHttpServletRequestBuilder request = post("/catalogue/ubl")
                .header("Authorization", environment.getProperty("nimble.test-token"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(catalogueJson);
        MvcResult result = this.mockMvc.perform(request).andDo(print()).andExpect(status().isCreated()).andReturn();

        CatalogueType catalogue = mapper.readValue(result.getResponse().getContentAsString(), CatalogueType.class);
        createdCatalogueId = catalogue.getUUID();

        Assert.assertEquals(1,catalogue.getCatalogueLine().get(0).getGoodsItem().getItem().getAdditionalItemProperty().size());
        Assert.assertEquals("Color",catalogue.getCatalogueLine().get(0).getGoodsItem().getItem().getAdditionalItemProperty().get(0).getName().get(0).getValue());
        Assert.assertEquals(2,catalogue.getCatalogueLine().get(0).getGoodsItem().getItem().getAdditionalItemProperty().get(0).getValue().size());
        Assert.assertEquals("Green",catalogue.getCatalogueLine().get(0).getGoodsItem().getItem().getAdditionalItemProperty().get(0).getValue().get(0).getValue());
        Assert.assertEquals("Red",catalogue.getCatalogueLine().get(0).getGoodsItem().getItem().getAdditionalItemProperty().get(0).getValue().get(1).getValue());
    }

    @Test
    public void test52_updateJsonCatalogue() throws Exception {
        MockHttpServletRequestBuilder request = get("/catalogue/ubl/" + createdCatalogueId)
                .header("Authorization", environment.getProperty("nimble.test-token"));
        MvcResult result = this.mockMvc.perform(request).andReturn();
        CatalogueType catalogue = mapper.readValue(result.getResponse().getContentAsString(), CatalogueType.class);

        // update item properties
        catalogue.getCatalogueLine().get(0).getGoodsItem().getItem().getAdditionalItemProperty().get(0).getValue().clear();
        TextType textType = new TextType();
        textType.setValue("Blue");
        textType.setLanguageID("en");
        catalogue.getCatalogueLine().get(0).getGoodsItem().getItem().getAdditionalItemProperty().get(0).getValue().add(textType);

        // get Json version of the updated catalogue
        String catalogueTypeAsString = mapper.writeValueAsString(catalogue);

        // make request
        request = put("/catalogue/ubl")
                .header("Authorization", environment.getProperty("nimble.test-token"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(catalogueTypeAsString);
        result = this.mockMvc.perform(request).andDo(print()).andExpect(status().isOk()).andReturn();

        // get the catalogue
        request = get("/catalogue/ubl/" + createdCatalogueId)
                .header("Authorization", environment.getProperty("nimble.test-token"));
        result = this.mockMvc.perform(request).andDo(print()).andExpect(status().isOk()).andReturn();
        catalogue = mapper.readValue(result.getResponse().getContentAsString(), CatalogueType.class);
        Assert.assertEquals(1,catalogue.getCatalogueLine().get(0).getGoodsItem().getItem().getAdditionalItemProperty().get(0).getValue().size());
        Assert.assertEquals("Blue",catalogue.getCatalogueLine().get(0).getGoodsItem().getItem().getAdditionalItemProperty().get(0).getValue().get(0).getValue());

        // delete the catalogue
        request = delete("/catalogue/ubl/" + createdCatalogueId)
                .header("Authorization", environment.getProperty("nimble.test-token"));
        this.mockMvc.perform(request).andDo(print()).andExpect(status().isOk()).andReturn();
    }

}
