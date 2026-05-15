package eu.nimble.service.catalogue.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import eu.nimble.service.catalogue.config.RoleConfig;
import eu.nimble.service.model.ubl.commonaggregatecomponents.LogisticsProviderType;
import eu.nimble.utility.ExecutionContext;
import eu.nimble.utility.validation.IValidationUtil;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * HCDP-05-05: Logistics Provider directory — proxy onto indexing-service party search.
 *
 * <p>Before HCDP-05-05 this endpoint read from a parallel admin-curated table
 * ({@code logistics_provider_type}). The directory has been consolidated with
 * the catalogue: a carrier IS a Nimble Party whose company profile has
 * {@code businessType="Logistics Provider"} (seeded by STEP 7c). This controller
 * proxies {@code POST /party/search?fq=businessType:"Logistics Provider"} so the
 * Replace Provider modal lists the same carriers Find Logistics search returns,
 * and the result carries the real {@code partyId} for UBL replacement.</p>
 */
@Controller
public class LogisticsProvidersController {

    private static final Logger logger = LoggerFactory.getLogger(LogisticsProvidersController.class);

    private static final String LOGISTICS_PROVIDER_BUSINESS_TYPE = "Logistics Provider";
    private static final int MAX_RESULTS = 100;

    @Autowired
    private ExecutionContext executionContext;

    @Autowired
    private IValidationUtil validationUtil;

    @Value("${nimble.indexing.url:http://localhost:8090}")
    private String indexingUrl;

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @CrossOrigin(origins = {"*"})
    @ApiOperation(value = "", notes = "Returns the list of logistics providers known to the platform. " +
            "Backed by indexing-service party search filtered to businessType=\"Logistics Provider\". " +
            "Used to populate the Replace Provider modal's alternatives dropdown.")
    @ApiResponses({
        @ApiResponse(code = 200, message = "Logistics providers retrieved successfully"),
        @ApiResponse(code = 401, message = "Unauthorized request"),
        @ApiResponse(code = 503, message = "Indexing service unreachable")
    })
    @GetMapping(value = "/logistics-providers", produces = "application/json")
    public ResponseEntity<List<LogisticsProviderType>> getActiveProviders(
            @ApiParam(value = "Bearer token") @RequestHeader(value = "Authorization") String bearerToken) {

        if (!validationUtil.validateRole(bearerToken, executionContext.getUserRoles(),
                                         RoleConfig.REQUIRED_ROLES_CATALOGUE_READ)) {
            return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
        }
        try {
            List<LogisticsProviderType> providers = fetchProvidersFromIndexing(bearerToken);
            logger.debug("[LOGISTICS-PROVIDERS] Returning {} provider(s) from indexing-service", providers.size());
            return ResponseEntity.ok(providers);
        } catch (Exception ex) {
            logger.error("[LOGISTICS-PROVIDERS] Failed to fetch from indexing-service at {}", indexingUrl, ex);
            return new ResponseEntity<>(HttpStatus.SERVICE_UNAVAILABLE);
        }
    }

    @CrossOrigin(origins = {"*"})
    @ApiOperation(value = "", notes = "Returns a single logistics provider by its stable slug (vatNumber.toLowerCase()).")
    @ApiResponses({
        @ApiResponse(code = 200, message = "Provider found"),
        @ApiResponse(code = 404, message = "Provider not found"),
        @ApiResponse(code = 401, message = "Unauthorized request"),
        @ApiResponse(code = 503, message = "Indexing service unreachable")
    })
    @GetMapping(value = "/logistics-providers/{slug}", produces = "application/json")
    public ResponseEntity<LogisticsProviderType> getProviderBySlug(
            @ApiParam(value = "Stable provider slug, e.g. 'de-dhl-logi-001'") @PathVariable("slug") String slug,
            @ApiParam(value = "Bearer token") @RequestHeader(value = "Authorization") String bearerToken) {

        if (!validationUtil.validateRole(bearerToken, executionContext.getUserRoles(),
                                         RoleConfig.REQUIRED_ROLES_CATALOGUE_READ)) {
            return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
        }
        try {
            List<LogisticsProviderType> providers = fetchProvidersFromIndexing(bearerToken);
            for (LogisticsProviderType p : providers) {
                if (slug.equals(p.getSlug())) {
                    return ResponseEntity.ok(p);
                }
            }
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        } catch (Exception ex) {
            logger.error("[LOGISTICS-PROVIDERS] Failed to fetch from indexing-service at {}", indexingUrl, ex);
            return new ResponseEntity<>(HttpStatus.SERVICE_UNAVAILABLE);
        }
    }

    // ---- Internals ----------------------------------------------------------

    private List<LogisticsProviderType> fetchProvidersFromIndexing(String bearerToken) throws Exception {
        ObjectNode body = objectMapper.createObjectNode();
        body.put("rows", MAX_RESULTS);
        body.put("q", "*:*");
        ArrayNode fq = body.putArray("fq");
        fq.add("businessType:\"" + LOGISTICS_PROVIDER_BUSINESS_TYPE + "\"");

        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.AUTHORIZATION, bearerToken);
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(java.util.Collections.singletonList(MediaType.APPLICATION_JSON));

        HttpEntity<String> req = new HttpEntity<>(objectMapper.writeValueAsString(body), headers);
        ResponseEntity<String> resp = restTemplate.exchange(
                indexingUrl + "/party/search", HttpMethod.POST, req, String.class);

        JsonNode root = objectMapper.readTree(resp.getBody());
        JsonNode results = root.get("result");
        List<LogisticsProviderType> providers = new ArrayList<>();
        if (results != null && results.isArray()) {
            for (Iterator<JsonNode> it = results.elements(); it.hasNext(); ) {
                LogisticsProviderType p = mapPartyNodeToProvider(it.next());
                if (p != null) {
                    providers.add(p);
                }
            }
        }
        providers.sort((a, b) -> a.getName() == null ? 0 :
                                  b.getName() == null ? 0 :
                                  a.getName().compareToIgnoreCase(b.getName()));
        return providers;
    }

    /**
     * Maps an indexing-service party-search result node to a LogisticsProviderType DTO.
     * Returns null when required fields (id, legalName) are missing — defensive against
     * malformed records.
     */
    private LogisticsProviderType mapPartyNodeToProvider(JsonNode node) {
        if (node == null || node.isNull()) return null;
        String id = textOrNull(node.get("id"));
        String legalName = textOrNull(node.get("legalName"));
        if (id == null || legalName == null) {
            return null;
        }
        String vat = textOrNull(node.get("vatNumber"));
        LogisticsProviderType p = new LogisticsProviderType();
        p.setPartyId(id);
        p.setName(legalName);
        // Slug = vatNumber.toLowerCase() per HCDP-05-05 plan; fall back to "party-<id>"
        // when vat is missing so the slug is always present and unique.
        p.setSlug(vat != null ? vat.toLowerCase() : "party-" + id);
        // Country = vatNumber prefix (e.g. "DE-DHL-LOGI-001" → "DE"); empty otherwise.
        p.setCountry(extractCountryFromVat(vat));
        // federationInstanceID is not surfaced by indexing-service party-search; null
        // here is correct in federation-OFF deployments (the demo).
        p.setFederationInstanceID(null);
        return p;
    }

    private static String textOrNull(JsonNode n) {
        return (n == null || n.isNull()) ? null : n.asText();
    }

    private static String extractCountryFromVat(String vat) {
        if (vat == null || vat.length() < 2) return "";
        int dash = vat.indexOf('-');
        if (dash == 2) return vat.substring(0, 2).toUpperCase();
        // No dash — assume first 2 chars are the ISO-2 prefix.
        return vat.substring(0, 2).toUpperCase();
    }
}
