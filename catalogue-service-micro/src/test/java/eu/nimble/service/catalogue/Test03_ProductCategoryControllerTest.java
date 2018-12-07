package eu.nimble.service.catalogue;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import eu.nimble.service.catalogue.model.category.Category;
import org.junit.Assert;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
@ActiveProfiles("local_dev")
@RunWith(SpringJUnit4ClassRunner.class)
public class Test03_ProductCategoryControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @Test
    public void test1_getAvailableTaxonomies() throws Exception {
        MockHttpServletRequestBuilder request = get("/catalogue/category/taxonomies");
        MvcResult result = this.mockMvc.perform(request).andDo(print()).andExpect(status().isOk()).andReturn();
        ObjectMapper mapper = new ObjectMapper();
        JsonParser parser = mapper.getFactory().createParser(result.getResponse().getContentAsString());
        ArrayNode taxonomies = mapper.readTree(parser);
        Assert.assertEquals(2, taxonomies.size());
    }

    @Test
    public void test2_getCategoriesByName() throws Exception {
        MockHttpServletRequestBuilder request = get("/catalogue/category").param("categoryNames", "die");
        MvcResult result = this.mockMvc.perform(request).andDo(print()).andExpect(status().isOk()).andReturn();
        ObjectMapper mapper = new ObjectMapper();
        List<Category> categories = mapper.readValue(result.getResponse().getContentAsString(), new TypeReference<List<Category>>() {});

        // check existence duplicate categories
        Set<String> categoryNames = new HashSet<>();
        categories.stream().forEach(category -> categoryNames.add(category.getId()));
        Assert.assertEquals("Duplicates results in the retrieved categories", categoryNames.size(), categories.size());
    }

    @Test
    public void test3_getCategoriesByIds() throws Exception {
        MockHttpServletRequestBuilder request = get("/catalogue/category").param("categoryIds", "0173-1#01-BAA975#013").param("taxonomyIds", "eClass");
        MvcResult result = this.mockMvc.perform(request).andDo(print()).andExpect(status().isOk()).andReturn();
        ObjectMapper mapper = new ObjectMapper();
        List<Category> categories = mapper.readValue(result.getResponse().getContentAsString(), new TypeReference<List<Category>>() {});
        Assert.assertEquals("Die-cutter and stamping machines (post press)", categories.get(0).getPreferredName());
    }
}
