package eu.nimble.catalogue.service;

import eu.nimble.service.catalogue.model.category.Category;
import eu.nimble.service.catalogue.impl.ProductCategoryController;
import eu.nimble.utility.config.CatalogueServiceConfig;
import eu.nimble.utility.config.PersistenceConfig;
import org.junit.*;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.ArrayList;
import java.util.List;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
@RunWith(SpringRunner.class)
@SpringBootTest(classes = {PersistenceConfig.class, CatalogueServiceConfig.class})
public class ProductCategoryControllerTest {
    private ProductCategoryController productCategoryController = new ProductCategoryController();

    @Test
    public void test1_getAvailableTaxonomies() throws Exception {
        ResponseEntity responseEntity = productCategoryController.getAvailableTaxonomies();
        Assert.assertEquals(200,responseEntity.getStatusCodeValue());
    }

    @Test
    public void test2_getCategories() throws Exception {
        List<String> names = new ArrayList<String>();
        names.add("die");
        List<String> taxonomyIds = new ArrayList<String>();
        taxonomyIds.add("eClass");
        List<String> categoryIds = new ArrayList<String>();
        categoryIds.add("0173-1#01-BAA975#013");

        ResponseEntity responseEntity = productCategoryController.getCategories(names,null,null, null);

        Assert.assertEquals(200,responseEntity.getStatusCodeValue());

        responseEntity = productCategoryController.getCategories(null,taxonomyIds,categoryIds, null);

        Assert.assertEquals(200,responseEntity.getStatusCodeValue());
    }
}
