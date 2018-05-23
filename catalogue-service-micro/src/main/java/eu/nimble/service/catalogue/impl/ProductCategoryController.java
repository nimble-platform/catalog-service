package eu.nimble.service.catalogue.impl;

import eu.nimble.service.catalogue.category.CategoryServiceManager;
import eu.nimble.service.catalogue.category.datamodel.Category;
import eu.nimble.service.catalogue.category.datamodel.CategoryTreeResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

/**
 * Product category related REST services
 */
@Controller
public class ProductCategoryController {

    private static Logger log = LoggerFactory
            .getLogger(ProductCategoryController.class);

    private CategoryServiceManager csm = CategoryServiceManager.getInstance();

    /**
     * Retrieves a list of {@link Category}. This method takes either a list of names or a list (category id/taxonomy id)
     * pairs. When the category names are provided, they are looked for in all managed taxonomies. For the other option,
     * (i.e. get by id option) the taxonomy id is already provided along with the category id. See the examples in
     * parameter definitions.
     *
     * @param categoryNames List of names.
     *                      Example: "wood, mdf"
     * @param taxonomyIds   List of taxonomy ids. IDs must indicate the taxonomies containing the corresponding category
     *                      in the categoryIds parameter.
     *                      Example: eClass, eClass
     * @param categoryIds   List of category ids.
     *                      Example: 0173-1#01-BAC439#012,0173-1#01-AJZ694#013
     * @return <li>200 along with the list of categories retrieved for the given parameters</li>
     * <li>400 if none of category names or (taxonomyId/categoryId) pairs are provided; number of elements in taxonomy id and category id lists do not match</li>
     */
    @CrossOrigin(origins = {"*"})
    @RequestMapping(value = "/catalogue/category",
            produces = {"application/json"},
            method = RequestMethod.GET)
    public ResponseEntity getCategories(@RequestParam(required = false) List<String> categoryNames,
                                        @RequestParam(required = false) List<String> taxonomyIds,
                                        @RequestParam(required = false) List<String> categoryIds) {
        log.info("Incoming request to get categories category names");
        List<Category> categories = new ArrayList<>();
        if (categoryNames != null && categoryNames.size() > 0) {
            log.info("Getting categories for name: {}", categoryNames);
            for (String name : categoryNames) {
                categories.addAll(csm.getProductCategories(name));
            }

        } else if (taxonomyIds != null && taxonomyIds.size() > 0 && categoryIds != null && categoryIds.size() > 0) {
            if (taxonomyIds.size() != categoryIds.size()) {
                String msg = "Number of elements in taxonomy ids list and  category ids list does not match";
                log.info(msg);
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(msg);
            }

            log.info("Getting categories for taxonomyIds: {}, categoryIds: {}", taxonomyIds, categoryIds);
            for (int i = 0; i < taxonomyIds.size(); i++) {
                categories.add(csm.getCategory(taxonomyIds.get(i), categoryIds.get(i)));
            }

        } else {
            String msg = "Neither category names nor (taxonomy id / category id) pairs provided";
            log.info(msg);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(msg);
        }

        log.info("Completed request to get categories. size: {}", categories.size());
        return ResponseEntity.ok(categories);
    }

    /**
     * Retrieves the identifiers of the available product category taxonomies
     *
     * @return
     */
    @RequestMapping(value = "/catalogue/category/taxonomies",
            produces = {"application/json"},
            method = RequestMethod.GET)
    public ResponseEntity getAvailableTaxonomies() {
        List<String> taxonomies;
        try {
            taxonomies = csm.getAvailableTaxonomies();
        } catch(Exception e) {
            String msg = "Failed to get available taxonomies\n" + e.getMessage();
            log.error(msg, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(msg);
        }
        return ResponseEntity.ok(taxonomies);
    }


    @RequestMapping(value = "/catalogue/category/{taxonomyId}/{parentCategoryId}/subcategories",
            produces = {"application/json"},
            method = RequestMethod.GET)
    public ResponseEntity<List<Category>> getSubCategories(@PathVariable String taxonomyId, @PathVariable String parentCategoryId) {
        List<Category> categories = csm.getSubCategories(taxonomyId, parentCategoryId);
        return ResponseEntity.ok(categories);
    }

    @CrossOrigin(origins = {"*"})
    @RequestMapping(value = "/catalogue/category/{taxonomyId}/{categoryId}/tree",
            produces = {"application/json"},
            method = RequestMethod.GET)
    public ResponseEntity<CategoryTreeResponse> getCategoryTree(@PathVariable String taxonomyId, @PathVariable String categoryId) {
        CategoryTreeResponse categories = csm.getCategoryTree(taxonomyId, categoryId);
        return ResponseEntity.ok(categories);
    }
}