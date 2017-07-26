package eu.nimble.service.catalogue.impl;

import eu.nimble.service.catalogue.category.datamodel.Category;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Controller
public class ProductCategoryController {

    private static Logger log = LoggerFactory
            .getLogger(ProductCategoryController.class);

    private CategoryServiceManager csm = CategoryServiceManager.getInstance();

    @CrossOrigin(origins = {"*"})
    @RequestMapping(value = "/catalogue/category/{taxonomyId}/{categoryId}",
            produces = {"application/json"},
            method = RequestMethod.GET)
    public ResponseEntity<Category> getCategoryById(@PathVariable String taxonomyId, @PathVariable String categoryId) {
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
}
