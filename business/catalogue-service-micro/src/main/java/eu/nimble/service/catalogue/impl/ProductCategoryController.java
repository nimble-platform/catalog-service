package eu.nimble.service.catalogue.impl;

import eu.nimble.service.catalogue.category.datamodel.Category;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

@Controller
public class ProductCategoryController {

    private static Logger log = LoggerFactory
            .getLogger(ProductCategoryController.class);

    private CategoryServiceManager csm = CategoryServiceManager.getInstance();

    @CrossOrigin(origins = {"*"})
    @RequestMapping(value = "/catalogue/category/{taxonomyId}",
            produces = {"application/json"},
            method = RequestMethod.GET)
    public ResponseEntity<Category> getCategoryById(@PathVariable("taxonomyId") String taxonomyId, @RequestParam("categoryId") String categoryId) {
        Category category = csm.getCategory(taxonomyId, categoryId);
        return ResponseEntity.ok(category);
    }

    @CrossOrigin(origins = {"*"})
    @RequestMapping(value = "/catalogue/category",
            produces = {"application/json"},
            method = RequestMethod.GET)
    public ResponseEntity getCategoriesByName(@RequestParam(required = false) String categoryName) {
        List<Category> categories = csm.getProductCategories(categoryName);
        return ResponseEntity.ok(categories);
    }

    @RequestMapping(value = "/catalogue/category/{taxonomyId}/{parentCategoryId}/subcategories",
            produces = {"application/json"},
            method = RequestMethod.GET)
    public ResponseEntity<List<Category>> getSubCategories(@PathVariable String taxonomyId, @PathVariable String parentCategoryId) {
        List<Category> categories = csm.getSubCategories(taxonomyId, parentCategoryId);
        return ResponseEntity.ok(categories);
    }

    // Usage: GET request to /catalogue/category/multiple/taxonomyId1,categoryId1,taxonomyId2,categoryId2...
    @CrossOrigin(origins = {"*"})
    @RequestMapping(value = "/catalogue/category/multiple/{ids}",
            produces = {"application/json"},
            method = RequestMethod.GET)
    public ResponseEntity<List<Category>> getMultipleCategories(@PathVariable String ids) {

        String[] parsedIds = ids.split(",");
        int numOfCategories = parsedIds.length / 2;

        ArrayList<Category> categories = new ArrayList<>();

        for (int i = 0; i < numOfCategories; i++)
            categories.add(csm.getCategory(parsedIds[i * 2], parsedIds[i * 2 + 1]));

        return ResponseEntity.ok(categories);
    }
}
