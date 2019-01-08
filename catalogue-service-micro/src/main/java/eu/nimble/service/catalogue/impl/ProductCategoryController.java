package eu.nimble.service.catalogue.impl;

import eu.nimble.service.catalogue.category.CategoryServiceManager;
import eu.nimble.service.catalogue.model.category.Category;
import eu.nimble.service.catalogue.model.category.CategoryTreeResponse;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
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
     * Retrieves a list of {@link Category}. This method takes a list (category id/taxonomy id)
     * pairs.See the examples in parameter definitions.
     * @param taxonomyIds   List of taxonomy ids. IDs must indicate the taxonomies containing the corresponding category
     *                      in the categoryIds parameter.
     *                      Example: eClass, eClass
     * @param categoryIds   List of category ids.
     *                      Example: 0173-1#01-BAC439#012,0173-1#01-AJZ694#013
     * @return <li>200 along with the list of categories retrieved for the given parameters</li>
     * <li>400 if (taxonomyId/categoryId) pairs are not provided; number of elements in taxonomy id and category id lists do not match</li>
     */
    @CrossOrigin(origins = {"*"})
    @ApiOperation(value = "", notes = "Retrieve a list of categories")
    @RequestMapping(value = "/catalogue/category",
            produces = {"application/json"},
            method = RequestMethod.GET)
    public ResponseEntity getCategories(@RequestParam(required = false) List<String> taxonomyIds,
                                        @RequestParam(required = false) List<String> categoryIds) {
        log.info("Incoming request to get categories");
        List<Category> categories = new ArrayList<>();
        if (taxonomyIds != null && taxonomyIds.size() > 0 && categoryIds != null && categoryIds.size() > 0) {
            if (taxonomyIds.size() != categoryIds.size()) {
                String msg = "Number of elements in taxonomy ids list and  category ids list does not match";
                log.info(msg);
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(msg);
            }

            log.info("Getting categories for taxonomyIds: {}, categoryIds: {}", taxonomyIds, categoryIds);
            for (int i = 0; i < taxonomyIds.size(); i++) {
                if(!taxonomyIdExists(taxonomyIds.get(i))){
                    log.error("The given taxonomy id : {} is not valid", taxonomyIds.get(i));
                    return ResponseEntity.status(HttpStatus.NOT_FOUND).body(String.format("The given taxonomy id %s is not valid", taxonomyIds.get(i)));
                }
                categories.add(csm.getCategory(taxonomyIds.get(i), categoryIds.get(i)));
            }

        } else {
            String msg = "(taxonomy id / category id) pairs should be provided";
            log.info(msg);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(msg);
        }

        log.info("Completed request to get categories. size: {}", categories.size());
        return ResponseEntity.ok(categories);
    }

    /**
     * Retrieves a list of {@link Category}. This method takes a name and taxonomy id.If a valid taxonomy id is provided,then category name
     * is looked for in that taxonomy.If no taxonomy id is provided,then category name is looked for in all managed taxonomies.
     * See the examples in parameter definitions.
     * @param name          name of the category
     *                      Example: "wood, mdf"
     * @param taxonomyId   taxonomy id.
     *                      Example: eClass, FurnitureOntology
     * @param forLogistics  Optional parameter to indicate to restrict the results specific to logistics services or
     *                      regular products. If not specified, all matched categories are returned
     * @return <li>200 along with the list of categories retrieved for the given parameters</li>
     * <li>404 if the given taxonomy id is not valid</li>
     */
    @CrossOrigin(origins = {"*"})
    @ApiOperation(value = "", notes = "Retrieve a list of categories by name")
    @RequestMapping(value = "catalogue/taxonomies/{taxonomyId}",
            produces = {"application/json"},
            method = RequestMethod.GET)
    public ResponseEntity getCategoriesByName(@RequestParam String name,
                                              @PathVariable String taxonomyId,
                                              @RequestParam(required = false) Boolean forLogistics) {
        log.info("Incoming request to get categories by name");
        // check whether the taxonomy id is valid or not
        if(!taxonomyId.contentEquals("All") && !taxonomyIdExists(taxonomyId)){
            log.error("The given taxonomy id : {} is not valid", taxonomyId);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(String.format("The given taxonomy id %s is not valid", taxonomyId));
        }
        List<Category> categories = new ArrayList<>();
        if(taxonomyId.contentEquals("All")){
            log.info("Getting categories for name: {}", name);
            categories.addAll(csm.getProductCategories(name,null,forLogistics));
        }
        else {
            log.info("Getting categories for name: {}, taxonomyId: {}", name,taxonomyId);
            categories.addAll(csm.getProductCategories(name,taxonomyId,forLogistics));
        }
        log.info("Completed request to get categories by name. size: {}", categories.size());
        return ResponseEntity.ok(categories);
    }

    /**
     * Retrieves the identifiers of the available product category taxonomies
     *
     * @return
     */
    @CrossOrigin(origins = {"*"})
    @ApiOperation(value = "", notes = "Retrieve the identifiers of the available product category taxonomies")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Retrieved the identifiers of the available product category taxonomies successfully", response = String.class, responseContainer = "List"),
            @ApiResponse(code = 404, message = "Failed to get available taxonomies")
    })
    @RequestMapping(value = "/catalogue/category/taxonomies",
            produces = {"application/json"},
            method = RequestMethod.GET)
    public ResponseEntity getAvailableTaxonomies() {
        List<String> taxonomies;
        try {
            taxonomies = csm.getAvailableTaxonomies();
        } catch (Exception e) {
            String msg = "Failed to get available taxonomies\n" + e.getMessage();
            log.error(msg, e);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(msg);
        }
        return ResponseEntity.ok(taxonomies);
    }

    @CrossOrigin(origins = {"*"})
    @ApiOperation(value = "", notes = "Retrieve root categories")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Retrieved root categories successfully", response = Category.class, responseContainer = "List"),
            @ApiResponse(code = 404, message = "The given taxonomy id is not valid")
    })
    @RequestMapping(value = "/catalogue/category/{taxonomyId}/root-categories",
            produces = {"application/json"},
            method = RequestMethod.GET)
    public ResponseEntity getRootCategories(@PathVariable String taxonomyId) {
        if (!taxonomyIdExists(taxonomyId)) {
            log.error("The given taxonomy id : {} is not valid", taxonomyId);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(String.format("The given taxonomy id %s is not valid", taxonomyId));
        }
        List<Category> categories = csm.getRootCategories(taxonomyId);
        return ResponseEntity.ok(categories);
    }

    @CrossOrigin(origins = {"*"})
    @ApiOperation(value = "", notes = "Retrieve children categories")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Retrieved children categories successfully", response = Category.class, responseContainer = "List"),
            @ApiResponse(code = 404, message = "The given taxonomy id is not valid"),
            @ApiResponse(code = 400, message = "There does not exist a category with the given id")
    })
    @RequestMapping(value = "/catalogue/category/children-categories",
            produces = {"application/json"},
            method = RequestMethod.GET)
    public ResponseEntity getChildrenCategories(@RequestParam("taxonomyId") String taxonomyId, @RequestParam("categoryId") String categoryId) {
        if (!taxonomyIdExists(taxonomyId)) {
            log.error("The given taxonomy id : {} is not valid", taxonomyId);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(String.format("The given taxonomy id %s is not valid", taxonomyId));
        }

        if (!categoryExists(taxonomyId, categoryId)) {
            log.error("There does not exist a category with the id : {}", categoryId);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(String.format("There does not exist a category with the id %s", categoryId));
        }

        List<Category> categories = csm.getChildrenCategories(taxonomyId, categoryId);
        return ResponseEntity.ok(categories);
    }

    @CrossOrigin(origins = {"*"})
    @ApiOperation(value = "", notes = "Retrieve the parents of the category and their siblings")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Retrieved the parents of the category and their siblings successfully", response = CategoryTreeResponse.class),
            @ApiResponse(code = 404, message = "The given taxonomy id is not valid"),
            @ApiResponse(code = 400, message = "There does not exist a category with the given id")
    })
    @RequestMapping(value = "/catalogue/category/tree",
            produces = {"application/json"},
            method = RequestMethod.GET)
    public ResponseEntity getCategoryTree(@RequestParam("taxonomyId") String taxonomyId, @RequestParam("categoryId") String categoryId) {
        if (!taxonomyIdExists(taxonomyId)) {
            log.error("The given taxonomy id : {} is not valid", taxonomyId);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(String.format("The given taxonomy id %s is not valid", taxonomyId));
        }

        if (!categoryExists(taxonomyId, categoryId)) {
            log.error("There does not exist a category with the id : {}", categoryId);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(String.format("There does not exist a category with the id %s", categoryId));
        }

        CategoryTreeResponse categories = csm.getCategoryTree(taxonomyId, categoryId);
        return ResponseEntity.ok(categories);
    }

    private boolean taxonomyIdExists(String taxonomyId) {
        if (!csm.getAvailableTaxonomies().contains(taxonomyId)) {
            return false;
        }
        return true;
    }

    private boolean categoryExists(String taxonomyId, String categoryId) {
        try {
            csm.getCategory(taxonomyId, categoryId);
        } catch (IndexOutOfBoundsException e) {
            log.error("There does not exist a category with the id : {}", categoryId);
            return false;
        }
        return true;
    }

}