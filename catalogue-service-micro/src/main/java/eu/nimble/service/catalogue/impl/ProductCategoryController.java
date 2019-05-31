package eu.nimble.service.catalogue.impl;

import eu.nimble.service.catalogue.category.IndexCategoryService;
import eu.nimble.service.catalogue.category.TaxonomyQueryInterface;
import eu.nimble.service.catalogue.category.eclass.EClassIndexLoader;
import eu.nimble.service.catalogue.model.category.Category;
import eu.nimble.service.catalogue.model.category.CategoryTreeResponse;
import eu.nimble.service.catalogue.index.ClassIndexClient;
import eu.nimble.service.catalogue.util.SpringBridge;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import springfox.documentation.annotations.ApiIgnore;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Product category related REST services
 */
@Controller
public class ProductCategoryController {

    private static Logger log = LoggerFactory
            .getLogger(ProductCategoryController.class);

    @Autowired
    private IndexCategoryService categoryService;
    @Autowired
    private ClassIndexClient classIndexClient;
    @Autowired
    private EClassIndexLoader eClassIndexLoader;

    @CrossOrigin(origins = {"*"})
    @ApiOperation(value = "", notes = "Retrieves a list of Category instances. This operation takes a list of category ids and " +
            "another list containing corresponding taxonomy ids of each category. See the examples in parameter definitions.")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Retrieved the specified categories successfully", responseContainer = "List", response = Category.class),
            @ApiResponse(code = 400, message = "(taxonomyId/categoryId) pairs are not provided; number of elements in taxonomy id and category id lists do not match"),
            @ApiResponse(code = 500, message = "Unexpected error while getting categories")
    })
    @RequestMapping(value = "/categories",
            produces = {"application/json"},
            method = RequestMethod.GET)
    public ResponseEntity getSpecificCategories(@ApiParam(value = "Comma-separated category ids to be retrieved e.g. 0173-1#01-BAC439#012,0173-1#01-AJZ694#013", required = true) @RequestParam(required = false) List<String> categoryIds,
                                                @ApiParam(value = "Comma-separated taxonomy ids corresponding to the specified category ids e.g. eClass, eClass", required = true) @RequestParam(required = false) List<String> taxonomyIds,
                                                @ApiParam(value = "The Bearer token provided by the identity service", required = true) @RequestHeader(value = "Authorization", required = true) String bearerToken) {
        log.info("Incoming request to get categories");
        List<Category> categories = new ArrayList<>();
        if (taxonomyIds != null && taxonomyIds.size() > 0 && categoryIds != null && categoryIds.size() > 0) {
            // ensure that taxonomy id and category id lists have the same size
            if (taxonomyIds.size() != categoryIds.size()) {
                String msg = "Number of elements in taxonomy ids list and  category ids list does not match";
                log.info(msg);
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(msg);
            }

            // validate taxonomy ids
            for (int i = 0; i < taxonomyIds.size(); i++) {
                if(!taxonomyIdExists(taxonomyIds.get(i))){
                    log.error("The given taxonomy id : {} is not valid", taxonomyIds.get(i));
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(String.format("The given taxonomy id %s is not valid", taxonomyIds.get(i)));
                }
            }

            categories = categoryService.getCategories(taxonomyIds, categoryIds);

        } else {
            String msg = "(taxonomy id / category id) pairs should be provided";
            log.info(msg);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(msg);
        }

        log.info("Completed request to get categories. size: {}", categories.size());
        return ResponseEntity.ok(categories);
    }

    @CrossOrigin(origins = {"*"})
    @ApiOperation(value = "", notes = "Retrieves a list of Category instances. This method takes a name and taxonomy id." +
            " If a valid taxonomy id is provided,then category name is looked for in that taxonomy.If no taxonomy id is " +
            " provided,then category name is looked for in all managed taxonomies. See the examples in parameter definitions.")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Retrieved categories for the specified parameters successfully", responseContainer = "List", response = Category.class),
            @ApiResponse(code = 400, message = "Invalid taxonomy id")
    })
    @RequestMapping(value = "/taxonomies/{taxonomyId}/categories",
            produces = {"application/json"},
            method = RequestMethod.GET)
    public ResponseEntity getCategoriesByName(@ApiParam(value = "A name describing the categories to be retrieved. This parameter does not necessarily have to be the exact name of the category.", required = true) @RequestParam String name,
                                              @ApiParam(value = "Taxonomy id from which categories would be retrieved. If no taxonomy id is specified, all available taxonomies are considered. In addition to the taxonomies ids as returned by getAvailableTaxonomyIds method, 'all' value can be specified in order to get categories from all the taxonomies", required = true) @PathVariable String taxonomyId,
                                              @ApiParam(value = "An indicator for retrieving categories for logistics service or regular products. If not specified, no such distinction is considered.", defaultValue = "false") @RequestParam(required = false,defaultValue = "false") Boolean forLogistics,
                                              @ApiParam(value = "The Bearer token provided by the identity service", required = true) @RequestHeader(value = "Authorization", required = true) String bearerToken) {
        log.info("Incoming request to get categories by name");
        // check whether the taxonomy id is valid or not
        if(!(taxonomyId.compareToIgnoreCase("all") == 0 || taxonomyIdExists(taxonomyId))) {
            log.error("The given taxonomy id : {} is not valid", taxonomyId);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(String.format("The given taxonomy id %s is not valid", taxonomyId));
        }
        List<Category> categories = new ArrayList<>();
        if(taxonomyId.compareToIgnoreCase("all") == 0 ){
            log.info("Getting categories for name: {}", name);
            categories.addAll(categoryService.getProductCategories(name,null,forLogistics));
        }
        else {
            log.info("Getting categories for name: {}, taxonomyId: {}", name,taxonomyId);
            categories.addAll(categoryService.getProductCategories(name,taxonomyId,forLogistics));
        }
        log.info("Completed request to get categories by name. size: {}", categories.size());
        return ResponseEntity.ok(categories);
    }

    @CrossOrigin(origins = {"*"})
    @ApiOperation(value = "", notes = "Retrieves the identifiers of the available product category taxonomies")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Retrieved the identifiers of the available taxonomies successfully", response = String.class, responseContainer = "List"),
            @ApiResponse(code = 500, message = "Unexpected error while getting identifiers of the available taxonomies")
    })
    @RequestMapping(value = "/taxonomies/id",
            produces = {"application/json"},
            method = RequestMethod.GET)
    public ResponseEntity getAvailableTaxonomyIds(@ApiParam(value = "The Bearer token provided by the identity service", required = true) @RequestHeader(value = "Authorization", required = true) String bearerToken) {
        List<String> taxonomies = new ArrayList<>();
        try {
            SpringBridge.getInstance().getTaxonomyManager().getTaxonomiesMap().keySet().forEach(id -> taxonomies.add(id));
        } catch (Exception e) {
            String msg = "Failed to get available taxonomies\n" + e.getMessage();
            log.error(msg, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(msg);
        }
        return ResponseEntity.ok(taxonomies);
    }

    @CrossOrigin(origins = {"*"})
    @ApiOperation(value = "", notes = "Retrieves the map of logistics services and corresponding category uris for the given taxonomy id")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Retrieved the logistics related services-category uri map successfully"),
            @ApiResponse(code = 500, message = "Unexpected error while getting the logistics related services-category map")
    })
    @RequestMapping(value = "/taxonomies/{taxonomyId}/logistics-services",
            produces = {"application/json"},
            method = RequestMethod.GET)
    public ResponseEntity getLogisticsRelatedServices(@ApiParam(value = "The taxonomy id which is used to retrieve logistic related services. If 'all' value is specified for taxonomy id, then logistic related services for each available taxonomy id are returned.",required = true) @PathVariable(value = "taxonomyId",required = true) String taxonomyId,
                                                      @ApiParam(value = "The Bearer token provided by the identity service", required = true) @RequestHeader(value = "Authorization", required = true) String bearerToken) {
        log.info("Incoming request to get the logistics related services-category map for taxonomy id: {}",taxonomyId);

        Map<String,Map<String,String>> logisticServicesCategoryUriMap = categoryService.getLogisticsRelatedServices(taxonomyId);

        log.info("Completed request to get the logistics related services-category uri map for taxonomy id: {}", taxonomyId);
        return ResponseEntity.ok(logisticServicesCategoryUriMap);
    }

    @CrossOrigin(origins = {"*"})
    @ApiOperation(value = "", notes = "Retrieves root categories of the specified taxonomy")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Retrieved the root categories successfully", response = Category.class, responseContainer = "List"),
            @ApiResponse(code = 400, message = "Invalid taxonomy id")
    })
    @RequestMapping(value = "/taxonomies/{taxonomyId}/root-categories",
            produces = {"application/json"},
            method = RequestMethod.GET)
    public ResponseEntity getRootCategories(@ApiParam(value = "Taxonomy id from which categories would be retrieved.", required = true) @PathVariable String taxonomyId,
                                            @ApiParam(value = "The Bearer token provided by the identity service", required = true) @RequestHeader(value = "Authorization", required = true) String bearerToken) {
        if (!taxonomyIdExists(taxonomyId)) {
            log.error("The given taxonomy id : {} is not valid", taxonomyId);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(String.format("The given taxonomy id %s is not valid", taxonomyId));
        }
        List<Category> categories = categoryService.getRootCategories(taxonomyId);
        return ResponseEntity.ok(categories);
    }

    @CrossOrigin(origins = {"*"})
    @ApiOperation(value = "", notes = "Retrieves children categories for the specified category")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Retrieved children categories successfully", response = Category.class, responseContainer = "List"),
            @ApiResponse(code = 400, message = "Invalid taxonomy id"),
            @ApiResponse(code = 404, message = "There does not exist a category with the given id")
    })
    @RequestMapping(value = "/taxonomies/{taxonomyId}/categories/children-categories",
            produces = {"application/json"},
            method = RequestMethod.GET)
    public ResponseEntity getChildrenCategories(@ApiParam(value = "Taxonomy id containing the category for which children categories to be retrieved", required = true) @PathVariable("taxonomyId") String taxonomyId,
                                                @ApiParam(value = "Category ifd for which the children categories to be retrieved", required = true) @RequestParam("categoryId") String categoryId,
                                                @ApiParam(value = "The Bearer token provided by the identity service", required = true) @RequestHeader(value = "Authorization", required = true) String bearerToken) {
        if (!taxonomyIdExists(taxonomyId)) {
            log.error("The given taxonomy id : {} is not valid", taxonomyId);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(String.format("The given taxonomy id %s is not valid", taxonomyId));
        }

        if (!categoryExists(taxonomyId, categoryId)) {
            log.error("There does not exist a category with the id : {}", categoryId);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(String.format("There does not exist a category with the id %s", categoryId));
        }

        List<Category> categories = categoryService.getChildrenCategories(taxonomyId, categoryId);
        return ResponseEntity.ok(categories);
    }

    @CrossOrigin(origins = {"*"})
    @ApiOperation(value = "", notes = "Retrieves the parents of the category and siblings of all the way to the root-level parent" +
            " category. For example, considering the MDF Raw category in the eClass taxonomy, the parents list in the response" +
            " contains all the categories including the specified category itself: Construction technology >> Wood, timber material" +
            " >> Wood fiberboard (MDF, HDF, LDF) >> MDF raw. The categories list contains the list of siblings for each category" +
            " specified in the parents list.")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Retrieved the parents of the category and their siblings successfully", response = CategoryTreeResponse.class),
            @ApiResponse(code = 400, message = "Invalid taxonomy id"),
            @ApiResponse(code = 404, message = "There does not exist a category with the given id")
    })
    @RequestMapping(value = "/taxonomies/{taxonomyId}/categories/tree",
            produces = {"application/json"},
            method = RequestMethod.GET)
    public ResponseEntity getCategoryTree(@ApiParam(value = "Taxonomy id containing the category for which children categories to be retrieved", required = true) @PathVariable("taxonomyId") String taxonomyId,
                                          @ApiParam(value = "Category ifd for which the children categories to be retrieved", required = true) @RequestParam("categoryId") String categoryId,
                                          @ApiParam(value = "The Bearer token provided by the identity service", required = true) @RequestHeader(value = "Authorization", required = true) String bearerToken) {
        if (!taxonomyIdExists(taxonomyId)) {
            log.error("The given taxonomy id : {} is not valid", taxonomyId);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(String.format("The given taxonomy id %s is not valid", taxonomyId));
        }

        if (!categoryExists(taxonomyId, categoryId)) {
            log.error("There does not exist a category with the id : {}", categoryId);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(String.format("There does not exist a category with the id %s", categoryId));
        }

        CategoryTreeResponse categories = categoryService.getCategoryTree(taxonomyId, categoryId);
        return ResponseEntity.ok(categories);
    }

    @ApiIgnore
    @CrossOrigin(origins = {"*"})
    @ApiOperation(value = "", notes = "Indexes eClass categories.")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Indexed eClass categories successfully"),
            @ApiResponse(code = 401, message = "No user exists for the given token"),
            @ApiResponse(code = 500, message = "Failed to index eClass resources")
    })
    @RequestMapping(value = "categories/eClass/index",
            produces = {"application/json"},
            method = RequestMethod.GET)
    public ResponseEntity indexEclassResources(@ApiParam(value = "The Bearer token provided by the identity service", required = true) @RequestHeader(value = "Authorization", required = true) String bearerToken) {
        log.info("Incoming request to index eClass resources.");
        // check token
        ResponseEntity tokenCheck = eu.nimble.service.catalogue.util.HttpResponseUtil.checkToken(bearerToken);
        if (tokenCheck != null) {
            return tokenCheck;
        }

        try {
            eClassIndexLoader.indexEClassResources();
        } catch (Exception e) {
            log.error("Failed to index eClass Resources:",e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to index eClass Resources");
        }
        return ResponseEntity.ok(null);
    }

    private boolean taxonomyIdExists(String taxonomyId) {
        for(TaxonomyQueryInterface taxonomyQueryInterface: SpringBridge.getInstance().getTaxonomyManager().getTaxonomiesMap().values()){
            if(taxonomyQueryInterface.getTaxonomy().getId().compareToIgnoreCase(taxonomyId) == 0){
                return true;
            }
        }
        return false;
    }

    private boolean categoryExists(String taxonomyId, String categoryId) {
        try {
            classIndexClient.getIndexCategory(taxonomyId, categoryId);
        } catch (IndexOutOfBoundsException e) {
            log.error("There does not exist a category with the id : {}", categoryId);
            return false;
        }
        return true;
    }
}