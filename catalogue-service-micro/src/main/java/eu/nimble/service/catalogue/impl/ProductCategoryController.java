package eu.nimble.service.catalogue.impl;

import eu.nimble.service.catalogue.category.IndexCategoryService;
import eu.nimble.service.catalogue.category.TaxonomyManager;
import eu.nimble.service.catalogue.category.TaxonomyQueryInterface;
import eu.nimble.service.catalogue.category.eclass.EClassIndexLoader;
import eu.nimble.service.catalogue.config.RoleConfig;
import eu.nimble.service.catalogue.exception.NimbleExceptionMessageCode;
import eu.nimble.service.catalogue.model.category.Category;
import eu.nimble.service.catalogue.model.category.CategoryTreeResponse;
import eu.nimble.service.catalogue.index.ClassIndexClient;
import eu.nimble.service.catalogue.util.SpringBridge;
import eu.nimble.utility.ExecutionContext;
import eu.nimble.utility.exception.NimbleException;
import eu.nimble.utility.validation.IValidationUtil;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.Arrays;
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
    private TaxonomyManager taxonomyManager;
    @Autowired
    private IndexCategoryService categoryService;
    @Autowired
    private ClassIndexClient classIndexClient;
    @Autowired
    private EClassIndexLoader eClassIndexLoader;
    @Autowired
    private IValidationUtil validationUtil;
    @Autowired
    private ExecutionContext executionContext;

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
        // set request log of ExecutionContext
        String requestLog ="Incoming request to get categories";
        executionContext.setRequestLog(requestLog);

        log.info(requestLog);
        List<Category> categories = new ArrayList<>();
        if (taxonomyIds != null && taxonomyIds.size() > 0 && categoryIds != null && categoryIds.size() > 0) {
            // ensure that taxonomy id and category id lists have the same size
            if (taxonomyIds.size() != categoryIds.size()) {
                throw new NimbleException(NimbleExceptionMessageCode.BAD_REQUEST_INVALID_PARAMETERS_TO_GET_CATEGORIES.toString());
            }

            // validate taxonomy ids
            for (int i = 0; i < taxonomyIds.size(); i++) {
                if(!taxonomyIdExists(taxonomyIds.get(i))){
                    throw new NimbleException(NimbleExceptionMessageCode.BAD_REQUEST_INVALID_TAXONOMY.toString(), Arrays.asList(taxonomyIds.get(i)));
                }
            }

            categories = categoryService.getCategories(taxonomyIds, categoryIds);

        } else {
            throw new NimbleException(NimbleExceptionMessageCode.BAD_REQUEST_MISSING_PARAMETERS_TO_GET_CATEGORIES.toString());
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
        // set request log of ExecutionContext
        String requestLog ="Incoming request to get categories by name";
        executionContext.setRequestLog(requestLog);

        log.info(requestLog);
        // check whether the taxonomy id is valid or not
        if(!(taxonomyId.compareToIgnoreCase("all") == 0 || taxonomyIdExists(taxonomyId))) {
            throw new NimbleException(NimbleExceptionMessageCode.BAD_REQUEST_INVALID_TAXONOMY.toString(),Arrays.asList(taxonomyId));
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
        // set request log of ExecutionContext
        String requestLog ="Incoming request to get available taxonomy ids";
        executionContext.setRequestLog(requestLog);

        List<String> taxonomies = new ArrayList<>();
        try {
            SpringBridge.getInstance().getTaxonomyManager().getTaxonomiesMap().keySet().forEach(id -> taxonomies.add(id));
        } catch (Exception e) {
            throw new NimbleException(NimbleExceptionMessageCode.INTERNAL_SERVER_ERROR_GET_AVAILABLE_TAXONOMIES.toString(),e);
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
        // set request log of ExecutionContext
        String requestLog = String.format("Incoming request to get the logistics related services-category map for taxonomy id: %s",taxonomyId);
        executionContext.setRequestLog(requestLog);

        log.info(requestLog);

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
        // set request log of ExecutionContext
        String requestLog = String.format("Incoming request to get root categories for taxonomy: %s", taxonomyId);
        log.info(requestLog);
        executionContext.setRequestLog(requestLog);

        if (!(taxonomyId.compareToIgnoreCase("all") == 0 || taxonomyIdExists(taxonomyId))) {
            throw new NimbleException(NimbleExceptionMessageCode.BAD_REQUEST_INVALID_TAXONOMY.toString(),Arrays.asList(taxonomyId));
        }
        List<Category> categories = categoryService.getRootCategories(taxonomyId);
        log.info("Completed request to get root categories for taxonomy: %{}", taxonomyId);
        return ResponseEntity.ok(categories);
    }

    @CrossOrigin(origins = {"*"})
    @ApiOperation(value = "", notes = "Retrieves service of the specified taxonomy")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Retrieved the service categories successfully", response = Category.class, responseContainer = "List"),
            @ApiResponse(code = 400, message = "Invalid taxonomy id")
    })
    @RequestMapping(value = "/taxonomies/{taxonomyId}/service-categories",
            produces = {"application/json"},
            method = RequestMethod.GET)
    public ResponseEntity getServiceCategoryUris(@ApiParam(value = "Taxonomy id from which categories would be retrieved.", required = true) @PathVariable String taxonomyId,
                                            @ApiParam(value = "The Bearer token provided by the identity service", required = true) @RequestHeader(value = "Authorization", required = true) String bearerToken) {
        // set request log of ExecutionContext
        String requestLog = String.format("Incoming request to get service categories for taxonomy: %s", taxonomyId);
        log.info(requestLog);
        executionContext.setRequestLog(requestLog);

        if (!taxonomyIdExists(taxonomyId)) {
            throw new NimbleException(NimbleExceptionMessageCode.BAD_REQUEST_INVALID_TAXONOMY.toString(),Arrays.asList(taxonomyId));
        }

        List<String> categoryUris = taxonomyManager.getTaxonomiesMap().get(taxonomyId).getTaxonomy().getServiceRootCategories();
        if (categoryUris == null) {
            categoryUris = new ArrayList<>();
        }
        log.info("Completed request to get service categories for taxonomy: {}", taxonomyId);
        return ResponseEntity.ok(categoryUris);
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
        // set request log of ExecutionContext
        String requestLog = "Incoming request to get children categories";
        executionContext.setRequestLog(requestLog);

        if (!taxonomyIdExists(taxonomyId)) {
            throw new NimbleException(NimbleExceptionMessageCode.BAD_REQUEST_INVALID_TAXONOMY.toString(),Arrays.asList(taxonomyId));
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
        // set request log of ExecutionContext
        String requestLog = "Incoming request to get category tree";
        executionContext.setRequestLog(requestLog);

        if (!taxonomyIdExists(taxonomyId)) {
            throw new NimbleException(NimbleExceptionMessageCode.BAD_REQUEST_INVALID_TAXONOMY.toString(),Arrays.asList(taxonomyId));
        }

        CategoryTreeResponse categories = categoryService.getCategoryTree(taxonomyId, categoryId);
        return ResponseEntity.ok(categories);
    }

    @CrossOrigin(origins = {"*"})
    @ApiOperation(value = "", notes = "Indexes eClass resources,i.e. eClass categories and properties.")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Indexed eClass resources successfully"),
            @ApiResponse(code = 401, message = "No user exists for the given token"),
            @ApiResponse(code = 500, message = "Failed to index eClass resources")
    })
    @RequestMapping(value = "/categories/eClass/index",
            produces = {"application/json"},
            method = RequestMethod.GET)
    public ResponseEntity indexEclassResources(@ApiParam(value = "The Bearer token provided by the identity service", required = true) @RequestHeader(value = "Authorization", required = true) String bearerToken) {
        // set request log of ExecutionContext
        String requestLog = "Incoming request to index eClass resources.";
        executionContext.setRequestLog(requestLog);

        log.info(requestLog);
        // validate role
        if(!validationUtil.validateRole(bearerToken,executionContext.getUserRoles(), RoleConfig.REQUIRED_ROLES_FOR_ADMIN_OPERATIONS)) {
            throw new NimbleException(NimbleExceptionMessageCode.UNAUTHORIZED_INDEX_ECLASS_RESOURCES.toString());
        }

        try {
            eClassIndexLoader.indexEClassResources();
        } catch (Exception e) {
            throw new NimbleException(NimbleExceptionMessageCode.INTERNAL_SERVER_ERROR_INDEX_ECLASS_RESOURCES.toString(),e);
        }
        return ResponseEntity.ok(null);
    }

    @CrossOrigin(origins = {"*"})
    @ApiOperation(value = "", notes = "Indexes the given eClass properties.")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Indexed eClass properties successfully"),
            @ApiResponse(code = 401, message = "No user exists for the given token"),
            @ApiResponse(code = 500, message = "Failed to index eClass properties")
    })
    @RequestMapping(value = "/categories/eClass/index/properties",
            produces = {"application/json"},
            method = RequestMethod.POST)
    public ResponseEntity indexEclassProperties(@ApiParam(value = "The Bearer token provided by the identity service", required = true) @RequestHeader(value = "Authorization", required = true) String bearerToken,
                                                @ApiParam(value = "Identifiers of eClass properties to be indexed") @RequestBody List<String> propertyIds) {
        // set request log of ExecutionContext
        String requestLog = "Incoming request to index eClass properties.";
        executionContext.setRequestLog(requestLog);

        log.info(requestLog);
        // validate role
        if(!validationUtil.validateRole(bearerToken,executionContext.getUserRoles(), RoleConfig.REQUIRED_ROLES_FOR_ADMIN_OPERATIONS)) {
            throw new NimbleException(NimbleExceptionMessageCode.UNAUTHORIZED_INDEX_ECLASS_PROPERTIES.toString());
        }

        try {
            eClassIndexLoader.indexEClassProperties(propertyIds);
        } catch (Exception e) {
            throw new NimbleException(NimbleExceptionMessageCode.INTERNAL_SERVER_ERROR_INDEX_ECLASS_PROPERTIES.toString(),e);
        }
        return ResponseEntity.ok(null);
    }

    @CrossOrigin(origins = {"*"})
    @ApiOperation(value = "", notes = "Indexes the given eClass categories.")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Indexed eClass categories successfully"),
            @ApiResponse(code = 401, message = "No user exists for the given token"),
            @ApiResponse(code = 500, message = "Failed to index eClass categories")
    })
    @RequestMapping(value = "/categories/eClass/index/categories",
            produces = {"application/json"},
            method = RequestMethod.POST)
    public ResponseEntity indexEclassCategories(@ApiParam(value = "The Bearer token provided by the identity service", required = true) @RequestHeader(value = "Authorization", required = true) String bearerToken,
                                                @ApiParam(value = "Identifiers of eClass categories to be indexed") @RequestBody List<String> categoryIds) {
        // set request log of ExecutionContext
        String requestLog = "Incoming request to index eClass categories.";
        executionContext.setRequestLog(requestLog);

        log.info(requestLog);
        // validate role
        if(!validationUtil.validateRole(bearerToken, executionContext.getUserRoles(),RoleConfig.REQUIRED_ROLES_FOR_ADMIN_OPERATIONS)) {
            throw new NimbleException(NimbleExceptionMessageCode.UNAUTHORIZED_INDEX_ECLASS_CATEGORIES.toString());
        }

        try {
            eClassIndexLoader.indexEClassCategories(categoryIds);
        } catch (Exception e) {
            throw new NimbleException(NimbleExceptionMessageCode.INTERNAL_SERVER_ERROR_INDEX_ECLASS_CATEGORIES.toString(),e);
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
}