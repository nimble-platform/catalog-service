package eu.nimble.service.catalogue.category.taxonomy.furnitureontology;

import eu.nimble.service.catalogue.category.ProductCategoryService;
import eu.nimble.service.catalogue.model.category.Category;
import eu.nimble.service.catalogue.model.category.CategoryTreeResponse;
import eu.nimble.service.catalogue.model.category.Property;
import eu.nimble.service.catalogue.template.TemplateConfig;
import eu.nimble.service.catalogue.config.CatalogueServiceConfig;
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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Created by suat on 07-Jul-17.
 */
@Component
public class FurnitureOntologyCategoryServiceImpl implements ProductCategoryService {
    private static String GRAPH_URI;
    private static final String CONTEXT = "/context/furnituresectortaxonomy";
    private static final String FURNITURE_NS = "http://www.aidimme.es/FurnitureSectorOntology.owl#";
    private static final String FURNITURE_NS2 = "http://www.aidima.es/furnitureontology.owl#";
    private static final String XSD_NS = "http://www.w3.org/2001/XMLSchema#";
    private String defaultLanguage = "en";

    private static final Logger log = LoggerFactory.getLogger(FurnitureOntologyCategoryServiceImpl.class);

    private MarmottaClient client;

    @Autowired
    CatalogueServiceConfig catalogueServiceConfig;

    public FurnitureOntologyCategoryServiceImpl() {
    }

    @PostConstruct
    public void init(){
        String marmottaURL = catalogueServiceConfig.getMarmottaUrl();
        ClientConfiguration config = new ClientConfiguration(marmottaURL);
        GRAPH_URI = marmottaURL+CONTEXT;
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
        Category category = createCategory(categoryId, null);
        List<Property> properties = new ArrayList<>();
        category.setProperties(properties);

        String datatypeSparql = getDatatypePropertySparql(categoryId);
        SPARQLClient sparqlClient = client.getSPARQLClient();
        try {
            SPARQLResult dataTypes = sparqlClient.select(datatypeSparql);
            if (dataTypes != null) {
                String translation = null;
                for (Map<String, RDFNode> dataType : dataTypes) {
                    translation = dataType.get("translation") != null ? dataType.get("translation").toString() : null;

                    String propertyTranslation = dataType.get("proptranslation") != null ? dataType.get("proptranslation").toString() : null;
                    Property property = createProperty(dataType.get("prop").toString(), dataType.get("range").toString(), propertyTranslation);
                    properties.add(property);
                }
                category = createCategory(categoryId, translation);
                category.setProperties(properties);
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
                String remainder = getRemainder(record.get("uri").toString(), FURNITURE_NS,FURNITURE_NS2);
                if (remainder.toLowerCase().contains(categoryName.toLowerCase())) {
                    String uri = record.get("uri").toString();
                    String translation = record.get("translation") != null ? record.get("translation").toString() : null;
                    Category cat = createCategory(uri, translation);
                    result.add(cat);
                }
            }
        }
        return result;
    }

    @Override
    public List<Category> getProductCategories(String categoryName, boolean forLogistics) {
        if(!forLogistics) {
            return getProductCategories(categoryName);
        } else {
            return new ArrayList<>();
        }
    }

    private Property createProperty(String uri, String range, String translation) {
        Property property = new Property();
        property.setId(uri);
        property.addPreferredName(getRemainder(uri, FURNITURE_NS,FURNITURE_NS2), defaultLanguage);
        if(translation != null) {
            property.addPreferredName(translation, "es");
        }
        property.setDataType(getNormalizedDatatype(getRemainder(range, XSD_NS,null).toUpperCase()));
        property.setUri(uri);
        return property;
    }

    private Category createCategory(String uri, String translation) {
        Category cat = new Category();
        cat.setId(uri);
        cat.setCategoryUri(uri);
        cat.setTaxonomyId(getTaxonomyId());

        cat.addPreferredName(getRemainder(uri, FURNITURE_NS,FURNITURE_NS2), defaultLanguage);
        if(translation != null) {
            cat.addPreferredName(translation, "es");
        }
        cat.setCode(getRemainder(uri, FURNITURE_NS,FURNITURE_NS2));
        return cat;
    }

    private String getRemainder(String value, String prefix, String optionalPrefix) {
        if(optionalPrefix == null){
            if (value.startsWith(prefix)) {
                return value.substring(prefix.length());
            }
        }
        else {
            if(value.startsWith(prefix)){
                return value.substring(prefix.length());
            }
            else if(value.startsWith(optionalPrefix)){
                return value.substring(optionalPrefix.length());
            }
        }

        return value;
    }

    @Override
    public CategoryTreeResponse getCategoryTree(String categoryURI) {
        CategoryTreeResponse categoryTreeResponse = new CategoryTreeResponse();
        // get parents
        List<Category> parents = new ArrayList<>();
        SPARQLResult sparqlResult;
        try {
            String categoriesSparql = getParentCategoriesSparql(categoryURI);
            sparqlResult = client.getSPARQLClient().select(categoriesSparql);
        } catch (IOException | MarmottaClientException e) {
            log.error("Failed to get parent categories for categoryURI: {}",categoryURI,e);
            return null;
        }

        if (sparqlResult != null) {
            for (int i = sparqlResult.size()-1; i > -1; i--) {
                Map<String, RDFNode> record = sparqlResult.get(i);
                String uri = record.get("parent").toString();
                String translation = record.get("translation") != null ? record.get("translation").toString() : null;
                Category cat = createCategory(uri, translation);
                parents.add(cat);
            }
        }
        // add categoryURI to parent list
        parents.add(createCategory(categoryURI, null));
        int parentSize = parents.size();
        // set parents
        categoryTreeResponse.setParents(parents);

        List<List<Category>> siblings = new ArrayList<>();
        for (int m=0;m<parentSize+1;m++){
            siblings.add(new ArrayList<>());
        }
        // get root categories
        try {
            String categoriesSparql = getRootCategoriesSparql();
            sparqlResult = client.getSPARQLClient().select(categoriesSparql);
        } catch (IOException | MarmottaClientException e) {
            log.error("Failed to get root categories", e);
            return null;
        }

        if (sparqlResult != null) {
            for (int i = 0; i < sparqlResult.size(); i++) {
                Map<String, RDFNode> record = sparqlResult.get(i);
                String uri = record.get("uri").toString();
                String translation = record.get("translation") != null ? record.get("translation").toString() : null;
                Category cat = createCategory(uri, translation);
                siblings.get(0).add(cat);
            }
        }

        int j = 1;
        for(Category category : parents){
            try {
                String categoriesSparql = getChildrenCategoriesSparql(category.getCategoryUri());
                sparqlResult = client.getSPARQLClient().select(categoriesSparql);
            } catch (IOException | MarmottaClientException e) {
                log.error("Failed to get children categories for categoryURI: {}",category.getCategoryUri(), e);
                return null;
            }

            if (sparqlResult != null) {
                for (int i = 0; i < sparqlResult.size(); i++) {
                    Map<String, RDFNode> record = sparqlResult.get(i);
                    String uri = record.get("children").toString();
                    String translation = record.get("translation") != null ? record.get("translation").toString() : null;
                    Category cat = createCategory(uri, translation);
                    siblings.get(j).add(cat);
                }
            }
            j++;
        }
        categoryTreeResponse.setCategories(siblings);

        return categoryTreeResponse;
    }

    @Override
    public List<Category> getParentCategories(String categoryURI) {
        List<Category> result = new ArrayList<>();
        SPARQLResult sparqlResult;
        try {
            String categoriesSparql = getParentCategoriesSparql(categoryURI);
            sparqlResult = client.getSPARQLClient().select(categoriesSparql);
        } catch (IOException | MarmottaClientException e) {
            log.error("Failed to get parent categories for categoryURI: {}",categoryURI, e);
            return new ArrayList<>();
        }

        if (sparqlResult != null) {
            for (int i = 0; i < sparqlResult.size(); i++) {
                Map<String, RDFNode> record = sparqlResult.get(i);
                String uri = record.get("parent").toString();
                String translation = record.get("translation") != null ? record.get("translation").toString() : null;
                Category cat = createCategory(uri, translation);
                result.add(cat);
            }
        }
        return result;
    }

    @Override
    public List<Category> getChildrenCategories(String categoryURI) {
        List<Category> result = new ArrayList<>();
        SPARQLResult sparqlResult;
        try {
            String categoriesSparql = getChildrenCategoriesSparql(categoryURI);
            sparqlResult = client.getSPARQLClient().select(categoriesSparql);
        } catch (IOException | MarmottaClientException e) {
            log.error("Failed to get children categories for categoryURI: {}",categoryURI, e);
            return new ArrayList<>();
        }

        if (sparqlResult != null) {
            for (int i = 0; i < sparqlResult.size(); i++) {
                Map<String, RDFNode> record = sparqlResult.get(i);
                String uri = record.get("children").toString();
                String translation = record.get("translation") != null ? record.get("translation").toString() : null;
                Category cat = createCategory(uri, translation);
                result.add(cat);
            }
        }
        return result;
    }

    @Override
    public List<Category> getRootCategories() {
        List<Category> result = new ArrayList<>();
        SPARQLResult sparqlResult;
        try {
            String categoriesSparql = getRootCategoriesSparql();
            sparqlResult = client.getSPARQLClient().select(categoriesSparql);
        } catch (IOException | MarmottaClientException e) {
            log.error("Failed to get root categories", e);
            return new ArrayList<>();
        }

        if (sparqlResult != null) {
            for (int i = 0; i < sparqlResult.size(); i++) {
                Map<String, RDFNode> record = sparqlResult.get(i);
                String uri = record.get("uri").toString();
                String translation = record.get("translation") != null ? record.get("translation").toString() : null;
                Category cat = createCategory(uri, translation);
                result.add(cat);
            }
        }
        return result;
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
                append("PREFIX FurnitureSectorOntology1: <http://www.aidimme.es/FurnitureSectorOntology.owl#>").append(System.lineSeparator()).
                append("PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>\n").
                append("PREFIX mic:<").append(FURNITURE_NS).append(">\n").
                append("\n").
                append("SELECT DISTINCT ?prop ?range ?translation ?proptranslation WHERE { \n").
                append("  {\n").
                append("    GRAPH <").append(GRAPH_URI).append("> {\n").
                append("      ?cl rdf:type owl:Class .\n").
                append("      ?cl FurnitureSectorOntology1:translation ?translation.").append(System.lineSeparator()).
                append("      FILTER (?cl IN (<").append(uri).append(">)).\n").
                append("      ?cl rdfs:subClassOf*/rdfs:subClassOf ?parents .\n").
                append("      ?prop rdf:type owl:DatatypeProperty . \n").
                append("      ?prop FurnitureSectorOntology1:translation ?proptranslation.").append(System.lineSeparator()).
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
                append("      ?cl FurnitureSectorOntology1:translation ?translation.").append(System.lineSeparator()).
                append("      ?cl rdfs:subClassOf*/rdfs:subClassOf ?parents .\n").
                append("      FILTER (?cl IN (<").append(uri).append(">)).\n").
                append("      ?prop rdf:type owl:DatatypeProperty . \n").
                append("      ?prop FurnitureSectorOntology1:translation ?proptranslation.").append(System.lineSeparator()).
                append("      ?prop rdfs:domain ?definedIn .\n").
                append("      ?prop rdfs:range ?range .\n").
                append("      FILTER ( isIRI(?definedIn)) .\n").
                append("      FILTER (?definedIn IN (?parents) ) .\n").
                append("    }\n").
                append("  }\n").
                append("  UNION {\n").
                append("    GRAPH <").append(GRAPH_URI).append("> {\n").
                append("      ?prop rdfs:domain <").append(uri).append("> .\n").
                append("      <").append(uri).append("> FurnitureSectorOntology1:translation ?translation.").append(System.lineSeparator()).
                append("      ?prop FurnitureSectorOntology1:translation ?proptranslation.").append(System.lineSeparator()).
                append("      ?prop rdfs:range ?range .\n").
                append("    }\n").
                append("  }\n").
                append("  UNION {\n").
                append("    GRAPH <").append(GRAPH_URI).append("> {\n").
                append("      ?prop rdf:type owl:DatatypeProperty . \n").
                append("      ?prop FurnitureSectorOntology1:translation ?proptranslation.").append(System.lineSeparator()).
                append("      ?prop rdfs:domain ?domain .\n").
                append("      ?domain FurnitureSectorOntology1:translation ?translation.").append(System.lineSeparator()).
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
                .append("PREFIX FurnitureSectorOntology1: <http://www.aidimme.es/FurnitureSectorOntology.owl#>").append(System.lineSeparator())
                .append("PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> ").append(System.lineSeparator())
                .append("PREFIX mic: <").append(FURNITURE_NS).append(">").append(System.lineSeparator())
                .append(System.lineSeparator())
                .append("SELECT ?uri ?translation WHERE {").append(System.lineSeparator())
                .append("  GRAPH <").append(GRAPH_URI).append("> {").append(System.lineSeparator())
                .append("    ?uri rdf:type owl:Class.").append(System.lineSeparator())
                .append("    ?uri FurnitureSectorOntology1:translation ?translation.").append(System.lineSeparator())
                .append("    FILTER regex (lcase(?uri), \"").append(categoryName).append("\") .").append(System.lineSeparator())
                .append("	}").append(System.lineSeparator())
                .append("}");
        return sb.toString();
    }

    private String getRootCategoriesSparql(){
        StringBuilder sb = new StringBuilder("");
        sb.append("PREFIX owl: <http://www.w3.org/2002/07/owl#>").append(System.lineSeparator())
                .append("PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>").append(System.lineSeparator())
                .append("PREFIX FurnitureSectorOntology1: <http://www.aidimme.es/FurnitureSectorOntology.owl#>").append(System.lineSeparator())
                .append("PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> ").append(System.lineSeparator())
                .append("PREFIX mic: <").append(FURNITURE_NS).append(">").append(System.lineSeparator())
                .append(System.lineSeparator())
                .append("SELECT ?uri ?translation WHERE {").append(System.lineSeparator())
                .append("  GRAPH <").append(GRAPH_URI).append("> {").append(System.lineSeparator())
                .append("    ?uri rdf:type owl:Class.").append(System.lineSeparator())
                .append("    ?uri FurnitureSectorOntology1:translation ?translation.").append(System.lineSeparator())
                .append("    FILTER (!isBlank(?uri)).").append(System.lineSeparator())
                .append("    MINUS { ?uri rdfs:subClassOf ?parent.").append(System.lineSeparator())
                .append("            FILTER (!isBlank(?parent)).}").append(System.lineSeparator())
                .append("	}").append(System.lineSeparator())
                .append("}");
        return sb.toString();
    }

    private String getChildrenCategoriesSparql(String categoryURI){
        StringBuilder sb = new StringBuilder("");
        sb.append("PREFIX owl: <http://www.w3.org/2002/07/owl#>").append(System.lineSeparator())
                .append("PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>").append(System.lineSeparator())
                .append("PREFIX FurnitureSectorOntology1: <http://www.aidimme.es/FurnitureSectorOntology.owl#>").append(System.lineSeparator())
                .append("PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> ").append(System.lineSeparator())
                .append("PREFIX mic: <").append(FURNITURE_NS).append(">").append(System.lineSeparator())
                .append(System.lineSeparator())
                .append("SELECT ?children ?translation WHERE {").append(System.lineSeparator())
                .append("  GRAPH <").append(GRAPH_URI).append("> {").append(System.lineSeparator())
                .append("    ?children rdfs:subClassOf <").append(categoryURI).append(">. ").append(System.lineSeparator())
                .append("    ?children FurnitureSectorOntology1:translation ?translation.").append(System.lineSeparator())
                .append("    FILTER (!isBlank(?children)).")
                .append("	}").append(System.lineSeparator())
                .append("}");
        return sb.toString();
    }

    private String getParentCategoriesSparql(String categoryURI){
        StringBuilder sb = new StringBuilder("");
        sb.append("PREFIX owl: <http://www.w3.org/2002/07/owl#>").append(System.lineSeparator())
                .append("PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>").append(System.lineSeparator())
                .append("PREFIX FurnitureSectorOntology1: <http://www.aidimme.es/FurnitureSectorOntology.owl#>").append(System.lineSeparator())
                .append("PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> ").append(System.lineSeparator())
                .append("PREFIX mic: <").append(FURNITURE_NS).append(">").append(System.lineSeparator())
                .append(System.lineSeparator())
                .append("SELECT ?parent ?translation WHERE {").append(System.lineSeparator())
                .append("  GRAPH <").append(GRAPH_URI).append("> {").append(System.lineSeparator())
                .append("    <").append(categoryURI).append("> rdfs:subClassOf+ ?parent.").append(System.lineSeparator())
                .append("    ?parent FurnitureSectorOntology1:translation ?translation.").append(System.lineSeparator())
                .append("    FILTER (!isBlank(?parent)).")
                .append("	}").append(System.lineSeparator())
                .append("}");
        return sb.toString();
    }
}