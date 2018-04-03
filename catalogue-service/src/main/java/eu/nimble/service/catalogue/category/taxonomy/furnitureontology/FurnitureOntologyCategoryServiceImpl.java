package eu.nimble.service.catalogue.category.taxonomy.furnitureontology;

import eu.nimble.service.catalogue.category.ProductCategoryService;
import eu.nimble.service.catalogue.category.datamodel.Category;
import eu.nimble.service.catalogue.category.datamodel.Property;
import eu.nimble.service.catalogue.template.TemplateConfig;
import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.socket.PlainConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.marmotta.client.ClientConfiguration;
import org.apache.marmotta.client.MarmottaClient;
import org.apache.marmotta.client.clients.SPARQLClient;
import org.apache.marmotta.client.exception.MarmottaClientException;
import org.apache.marmotta.client.model.rdf.RDFNode;
import org.apache.marmotta.client.model.sparql.SPARQLResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Created by suat on 07-Jul-17.
 */
public class FurnitureOntologyCategoryServiceImpl implements ProductCategoryService {
    private static final String MARMOTTA_URI = "https://nimble-platform.salzburgresearch.at/marmotta";
    private static final String GRAPH_URI = "http://nimble-platform.salzburgresearch.at/marmotta/context/furnituresectortaxonomy";
    private static final String FURNITURE_NS = "http://www.aidimme.es/FurnitureSectorOntology.owl#";
    private static final String XSD_NS = "http://www.w3.org/2001/XMLSchema#";

    private static final Logger log = LoggerFactory.getLogger(FurnitureOntologyCategoryServiceImpl.class);

    private MarmottaClient client;

    public FurnitureOntologyCategoryServiceImpl() {
        // TODO: take the marmotta base uri form a parameter
        ClientConfiguration config = new ClientConfiguration(MARMOTTA_URI);
        final Registry<ConnectionSocketFactory> registry = RegistryBuilder.<ConnectionSocketFactory>create()
                .register("http", PlainConnectionSocketFactory.getSocketFactory())
                .register("https", SSLConnectionSocketFactory.getSocketFactory())
                .build();

        final PoolingHttpClientConnectionManager cm = new PoolingHttpClientConnectionManager(registry);
        cm.setMaxTotal(100);
        config.setConectionManager(cm);
        config.setConnectionTimeout(30000);
        client = new MarmottaClient(config);
    }

    @Override
    public Category getCategory(String categoryId) {
        // create category from the uri
        Category category = createCategory(categoryId);
        List<Property> properties = new ArrayList<>();
        category.setProperties(properties);

        String datatypeSparql = getDatatypePropertySparql(categoryId);
        SPARQLClient sparqlClient = client.getSPARQLClient();
        try {
            SPARQLResult dataTypes = sparqlClient.select(datatypeSparql);
            if (dataTypes != null) {
                for (Map<String, RDFNode> dataType : dataTypes) {
                    Property property = createProperty(dataType.get("prop").toString(), dataType.get("range").toString());
                    properties.add(property);
                }
            }
        } catch (IOException | MarmottaClientException e) {
            log.warn("Failed to get datatype properties for category: " + categoryId, e);
        }

        return category;
    }

    @Override
    public List<Category> getProductCategories(String categoryName) {
        List<Category> result = new ArrayList<>();
        SPARQLResult sparqlResult;
        try {
            String categoriesSparql = getCategoriesSparqlByName(categoryName.toLowerCase());
            sparqlResult = client.getSPARQLClient().select(categoriesSparql);
        } catch (IOException | MarmottaClientException e) {
            log.error("Failed to retrieve categories for category name: " + categoryName, e);
            return new ArrayList<>();
        }

        if (sparqlResult != null) {
            for (int i = 0; i < sparqlResult.size(); i++) {
                Map<String, RDFNode> record = sparqlResult.get(i);
                String remainder = getRemainder(record.get("uri").toString(), FURNITURE_NS);
                if (remainder.toLowerCase().contains(categoryName.toLowerCase())) {
                    String uri = record.get("uri").toString();
                    Category cat = createCategory(uri);
                    result.add(cat);
                }
            }
        }
        return result;
    }

    private Property createProperty(String uri, String range) {
        Property property = new Property();
        property.setId(uri);
        property.setPreferredName(getRemainder(uri, FURNITURE_NS));
        property.setDataType(getNormalizedDatatype(getRemainder(range, XSD_NS).toUpperCase()));
        property.setUri(uri);
        return property;
    }

    private Category createCategory(String uri) {
        Category cat = new Category();
        cat.setId(uri);
        cat.setCategoryUri(uri);
        cat.setTaxonomyId(getTaxonomyId());
        cat.setPreferredName(getRemainder(uri, FURNITURE_NS));
        cat.setCode(getRemainder(uri, FURNITURE_NS));
        return cat;
    }

    private String getRemainder(String value, String prefix) {
        if (value.startsWith(prefix)) {
            return value.substring(prefix.length());
        }
        return value;
    }

    @Override
    public List<Category> getSubCategories(String categoryUri) {
        throw new IllegalStateException("Not implemented yet");
    }

    @Override
    public String getTaxonomyId() {
        return "FurnitureOntology";
    }

    public String getNormalizedDatatype(String dataType) {
        String normalizedType;
        if (dataType.compareToIgnoreCase(TemplateConfig.TEMPLATE_DATA_TYPE_INT) == 0 ||
                dataType.compareToIgnoreCase(TemplateConfig.TEMPLATE_DATA_TYPE_FLOAT) == 0 ||
                dataType.compareToIgnoreCase(TemplateConfig.TEMPLATE_DATA_TYPE_DOUBLE) == 0) {
            normalizedType = TemplateConfig.TEMPLATE_DATA_TYPE_REAL_MEASURE;

        } else if (dataType.compareToIgnoreCase(TemplateConfig.TEMPLATE_DATA_TYPE_STRING) == 0) {
            normalizedType = TemplateConfig.TEMPLATE_DATA_TYPE_STRING;

        } else if (dataType.compareToIgnoreCase(TemplateConfig.TEMPLATE_DATA_TYPE_BOOLEAN) == 0) {
            normalizedType = TemplateConfig.TEMPLATE_DATA_TYPE_BOOLEAN;

        } else {
            log.warn("Unknown data type encountered: {}", dataType);
            normalizedType = TemplateConfig.TEMPLATE_DATA_TYPE_STRING;
        }
        return normalizedType;
    }

    private String getDatatypePropertySparql(String uri) {
        StringBuilder sb = new StringBuilder("");
        sb.append("PREFIX owl: <http://www.w3.org/2002/07/owl#>\n").
                append("PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>\n").
                append("PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>\n").
                append("PREFIX mic:<").append(FURNITURE_NS).append(">\n").
                append("\n").
                append("SELECT DISTINCT ?prop ?range WHERE { \n").
                append("  {\n").
                append("    GRAPH <").append(GRAPH_URI).append("> {\n").
                append("      ?cl rdf:type owl:Class .\n").
                append("      FILTER (?cl IN (<").append(uri).append(">)).\n").
                append("      ?cl rdfs:subClassOf*/rdfs:subClassOf ?parents .\n").
                append("      ?prop rdf:type owl:DatatypeProperty . \n").
                append("      ?prop rdfs:domain ?domain .\n").
                append("      ?prop rdfs:range ?range .\n").
                append("      ?domain owl:unionOf ?list .\n").
                append("      FILTER (! isIRI(?domain)) .\n").
                append("      ?list rdf:rest*/rdf:first ?definedIn .\n").
                append("      FILTER (?definedIn IN (?parents) ) .\n").
                append("    }\n").
                append("  } \n").
                append("  UNION {\n").
                append("    GRAPH <").append(GRAPH_URI).append("> {\n").
                append("      ?cl rdf:type owl:Class .\n").
                append("      ?cl rdfs:subClassOf*/rdfs:subClassOf ?parents .\n").
                append("      FILTER (?cl IN (<").append(uri).append(">)).\n").
                append("      ?prop rdf:type owl:DatatypeProperty . \n").
                append("      ?prop rdfs:domain ?definedIn .\n").
                append("      ?prop rdfs:range ?range .\n").
                append("      FILTER ( isIRI(?definedIn)) .\n").
                append("      FILTER (?definedIn IN (?parents) ) .\n").
                append("    }\n").
                append("  }\n").
                append("  UNION {\n").
                append("    GRAPH <").append(GRAPH_URI).append("> {\n").
                append("      ?prop rdfs:domain <").append(uri).append("> .\n").
                append("      ?prop rdfs:range ?range .\n").
                append("    }\n").
                append("  }\n").
                append("  UNION {\n").
                append("    GRAPH <").append(GRAPH_URI).append("> {\n").
                append("      ?prop rdf:type owl:DatatypeProperty . \n").
                append("      ?prop rdfs:domain ?domain .\n").
                append("      ?prop rdfs:range ?range .\n").
                append("      ?domain owl:unionOf ?list .\n").
                append("      ?list rdf:rest*/rdf:first ?member .\n").
                append("      FILTER (! isIRI(?domain)) .\n").
                append("      FILTER (?member IN (<").append(uri).append(">)) .\n").
                append("    }\n").
                append("  } \n").
                append("}");
        return sb.toString();
    }

    private String getParentClassSparql(String uri) {
        StringBuilder sb = new StringBuilder("");
        sb.append("PREFIX owl: <http://www.w3.org/2002/07/owl#>").append(System.lineSeparator())
                .append("PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>").append(System.lineSeparator())
                .append("PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> ").append(System.lineSeparator())
                .append("PREFIX mic: <").append(FURNITURE_NS).append(">").append(System.lineSeparator())
                .append(System.lineSeparator())
                .append("SELECT ?parent WHERE {").append(System.lineSeparator())
                .append("  GRAPH <").append(GRAPH_URI).append("> {").append(System.lineSeparator())
                .append("    <").append(uri).append("> rdfs:subClassOf ?parent.").append(System.lineSeparator())
                .append("	}").append(System.lineSeparator())
                .append("}");
        return sb.toString();
    }

    private String getCategoriesSparqlByName(String categoryName) {
        StringBuilder sb = new StringBuilder("");
        sb.append("PREFIX owl: <http://www.w3.org/2002/07/owl#>").append(System.lineSeparator())
                .append("PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>").append(System.lineSeparator())
                .append("PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> ").append(System.lineSeparator())
                .append("PREFIX mic: <").append(FURNITURE_NS).append(">").append(System.lineSeparator())
                .append(System.lineSeparator())
                .append("SELECT ?uri WHERE {").append(System.lineSeparator())
                .append("  GRAPH <").append(GRAPH_URI).append("> {").append(System.lineSeparator())
                .append("    ?uri rdf:type owl:Class.").append(System.lineSeparator())
                .append("    FILTER regex (lcase(?uri), \"").append(categoryName).append("\") .").append(System.lineSeparator())
                .append("	}").append(System.lineSeparator())
                .append("}");
        return sb.toString();
    }
}
