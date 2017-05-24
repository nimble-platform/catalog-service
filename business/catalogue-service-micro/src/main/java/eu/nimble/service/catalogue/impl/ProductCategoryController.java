package eu.nimble.service.catalogue.impl;

import eu.nimble.service.catalogue.ProductCategoryService;
import eu.nimble.service.catalogue.category.datamodel.Category;
import eu.nimble.service.model.modaml.catalogue.TEXCatalogType;
import eu.nimble.service.model.ubl.catalogue.CatalogueType;
import eu.nimble.utility.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Controller
public class ProductCategoryController {

    private static Logger log = LoggerFactory
            .getLogger(ProductCategoryController.class);

    private ProductCategoryService pcs = ProductCategoryServiceImpl.getInstance();

    @CrossOrigin(origins = {"http://localhost:9093", "http://localhost:9092"})
    @RequestMapping(value = "/catalogue/category/{categoryId}",
            produces = {"application/json"},
            method = RequestMethod.GET)
    public ResponseEntity<Category> getCategoryById(@PathVariable String categoryId) {
        Category category = pcs.getCategory(categoryId);
        return ResponseEntity.ok(category);
    }

    @CrossOrigin(origins = {"http://localhost:9093", "http://localhost:9092"})
    @RequestMapping(value = "/catalogue/category",
            produces = {"application/json"},
            method = RequestMethod.GET)
    public ResponseEntity getCategoriesByName(@RequestParam(required = false) String categoryName) {
        List<Category> categories = pcs.getProductCategories(categoryName);
        return ResponseEntity.ok(categories);
    }

    @RequestMapping(value = "/catalogue/category/{parentCategoryId}/subcategories",
            produces = {"application/json"},
            method = RequestMethod.GET)
    public ResponseEntity<List<Category>> getSubCategories(@PathVariable String parentCategoryId) {
        List<Category> categories = pcs.getSubCategories(parentCategoryId);
        return ResponseEntity.ok(categories);
    }
}
