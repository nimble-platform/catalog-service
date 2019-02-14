package eu.nimble.service.catalogue.category.taxonomy.furnitureontology;

import eu.nimble.service.catalogue.category.ProductCategoryService;
import eu.nimble.service.catalogue.model.category.Category;
import eu.nimble.service.catalogue.model.category.CategoryTreeResponse;
import eu.nimble.service.catalogue.model.category.Property;
import eu.nimble.service.catalogue.template.TemplateConfig;
import eu.nimble.service.catalogue.config.CatalogueServiceConfig;
import eu.nimble.service.model.ubl.commonbasiccomponents.TextType;
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
import java.util.HashMap;
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
        Category category = createCategory(categoryId);
        String datatypeSparql = getDatatypePropertySparql(categoryId);
        SPARQLClient sparqlClient = client.getSPARQLClient();
        try {
            Map<String,Property> propertyMap = new HashMap<>();

            SPARQLResult dataTypes = sparqlClient.select(datatypeSparql);
            if (dataTypes != null) {
                for (Map<String, RDFNode> dataType : dataTypes) {
                    String prop = dataType.get("prop").toString();
                    String range = dataType.get("range").toString();
                    String label = dataType.get("label").toString();
                    String languageId = dataType.get("languageId").toString();

                    // If we have the property in the map, then create a TextType for the label
                    // and add it to the preferred names of the property
                    if(propertyMap.containsKey(prop)){
                        TextType textType = new TextType();
                        textType.setLanguageID(languageId);
                        textType.setValue(label);
                        propertyMap.get(prop).getPreferredName().add(textType);
                    }
                    // Create the property and add the label to its preferred names
                    else {
                        Property property = createProperty(prop,range);
                        TextType textType = new TextType();
                        textType.setValue(label);
                        textType.setLanguageID(languageId);
                        property.getPreferredName().add(textType);

                        propertyMap.put(prop,property);
                    }
                }
                category = createCategory(categoryId);
                category.setProperties(new ArrayList<>(propertyMap.values()));
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
                    Category cat = createCategory(uri);
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

    private Property createProperty(String uri, String range) {
        Property property = new Property();
        property.setId(uri);
        property.setDataType(getNormalizedDatatype(getRemainder(range, XSD_NS,null).toUpperCase()));
        property.setUri(uri);
        return property;
    }

    private Category createCategory(String uri) {
        Category cat = new Category();
        cat.setId(uri);
        cat.setCategoryUri(uri);
        cat.setTaxonomyId(getTaxonomyId());
        // set preferred names and definitions of the category
        setMultilingualFieldsOfCategory(cat.getPreferredName(),cat.getDefinition(),uri);
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

    // This function is used to set multilingual fields of the category
    // There are two multilingual fields in a category: Label and Comments
    // Label corresponds to PreferredName and Comment corresponds to Definition in our Category model
    private void setMultilingualFieldsOfCategory(List<TextType> preferredNames, List<TextType> definitions, String categoryId){
        String preferredNamesSparql = getMultilingualFields(categoryId);
        SPARQLClient sparqlClient = client.getSPARQLClient();
        try {
            SPARQLResult dataTypes = sparqlClient.select(preferredNamesSparql);
            if (dataTypes != null) {
                for (Map<String, RDFNode> dataType : dataTypes) {
                    String label = dataType.get("label") != null ? dataType.get("label").toString() : null;
                    String labelLanguageId = dataType.get("label_languageId") != null ? dataType.get("label_languageId").toString() : null;
                    String definition = dataType.get("definition") != null ? dataType.get("definition").toString() : null;
                    String definitionLanguageId = dataType.get("definition_languageId") != null ? dataType.get("definition_languageId").toString() : null;

                    if(label != null){
                        TextType textType = new TextType();
                        textType.setLanguageID(labelLanguageId);
                        textType.setValue(label);
                        preferredNames.add(textType);
                    }
                    if(definition != null){
                        TextType textType = new TextType();
                        textType.setLanguageID(definitionLanguageId);
                        textType.setValue(definition);
                        definitions.add(textType);
                    }
                }
            }
        }
        catch (IOException | MarmottaClientException e){
            log.warn("Failed to get multilingual fields for category: " + categoryId, e);
        }
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
                Category cat = createCategory(uri);
                parents.add(cat);
            }
        }
        // add categoryURI to parent list
        parents.add(createCategory(categoryURI));
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
                Category cat = createCategory(uri);
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
                    Category cat = createCategory(uri);
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
                Category cat = createCategory(uri);
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
                Category cat = createCategory(uri);
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
                Category cat = createCategory(uri);
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

    // We use this Sparql query to get multilingual fields of the category with the given id
    // There are two multilingual fields in a category: Labels and Comments
    private String getMultilingualFields(String uri){
        StringBuilder sb = new StringBuilder("");
        sb.append("PREFIX owl: <http://www.w3.org/2002/07/owl#>").append(System.lineSeparator())
                .append("PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>").append(System.lineSeparator())
                .append("PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> ").append(System.lineSeparator())
                .append("PREFIX mic: <").append(FURNITURE_NS).append(">").append(System.lineSeparator())
                .append(System.lineSeparator())
                .append("SELECT ?label ?label_languageId ?definition ?definition_languageId WHERE {").append(System.lineSeparator())
                .append("   {GRAPH <").append(GRAPH_URI).append("> {").append(System.lineSeparator())
                .append("       <").append(uri).append("> rdf:type owl:Class .").append(System.lineSeparator())
                .append("       <").append(uri).append("> rdfs:label ?label").append(System.lineSeparator())
                .append("       BIND(lang(?label) AS ?label_languageId)").append(System.lineSeparator())
                .append("       }}").append(System.lineSeparator())
                .append("  UNION").append(System.lineSeparator())
                .append("   {GRAPH <").append(GRAPH_URI).append("> {").append(System.lineSeparator())
                .append("       <").append(uri).append("> rdf:type owl:Class .").append(System.lineSeparator())
                .append("       <").append(uri).append("> rdfs:comment ?definition").append(System.lineSeparator())
                .append("       BIND(lang(?definition) AS ?definition_languageId)").append(System.lineSeparator())
                .append("       }}").append(System.lineSeparator())
                .append("}").append(System.lineSeparator());
        return sb.toString();
    }

    private String getDatatypePropertySparql(String uri) {
        StringBuilder sb = new StringBuilder("");
        sb.append("PREFIX owl: <http://www.w3.org/2002/07/owl#>\n").
                append("PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>\n").
                append("PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>\n").
                append("PREFIX mic:<").append(FURNITURE_NS).append(">\n").
                append("\n").
                append("SELECT DISTINCT ?prop ?range ?label ?languageId WHERE { \n").
                append("  {\n").
                append("    GRAPH <").append(GRAPH_URI).append("> {\n").
                append("      ?cl rdf:type owl:Class .\n").
                append("      FILTER (?cl IN (<").append(uri).append(">)).\n").
                append("      ?cl rdfs:subClassOf*/rdfs:subClassOf ?parents .\n").
                append("      ?prop rdf:type owl:DatatypeProperty . \n").
                append("      ?prop rdfs:label ?label.").append(System.lineSeparator()).
                append("      BIND(lang(?label) AS ?languageId)").
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
                append("      ?prop rdfs:label ?label.").append(System.lineSeparator()).
                append("      BIND(lang(?label) AS ?languageId)").
                append("      ?prop rdfs:domain ?definedIn .\n").
                append("      ?prop rdfs:range ?range .\n").
                append("      FILTER ( isIRI(?definedIn)) .\n").
                append("      FILTER (?definedIn IN (?parents) ) .\n").
                append("    }\n").
                append("  }\n").
                append("  UNION {\n").
                append("    GRAPH <").append(GRAPH_URI).append("> {\n").
                append("      ?prop rdfs:domain <").append(uri).append("> .\n").
                append("      ?prop rdfs:label ?label.").append(System.lineSeparator()).
                append("      BIND(lang(?label) AS ?languageId)").
                append("      ?prop rdfs:range ?range .\n").
                append("    }\n").
                append("  }\n").
                append("  UNION {\n").
                append("    GRAPH <").append(GRAPH_URI).append("> {\n").
                append("      ?prop rdf:type owl:DatatypeProperty . \n").
                append("      ?prop rdfs:label ?label.").append(System.lineSeparator()).
                append("      BIND(lang(?label) AS ?languageId)").
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

    private String getRootCategoriesSparql(){
        StringBuilder sb = new StringBuilder("");
        sb.append("PREFIX owl: <http://www.w3.org/2002/07/owl#>").append(System.lineSeparator())
                .append("PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>").append(System.lineSeparator())
                .append("PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> ").append(System.lineSeparator())
                .append("PREFIX mic: <").append(FURNITURE_NS).append(">").append(System.lineSeparator())
                .append(System.lineSeparator())
                .append("SELECT ?uri WHERE {").append(System.lineSeparator())
                .append("  GRAPH <").append(GRAPH_URI).append("> {").append(System.lineSeparator())
                .append("    ?uri rdf:type owl:Class.").append(System.lineSeparator())
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
                .append("PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> ").append(System.lineSeparator())
                .append("PREFIX mic: <").append(FURNITURE_NS).append(">").append(System.lineSeparator())
                .append(System.lineSeparator())
                .append("SELECT ?children WHERE {").append(System.lineSeparator())
                .append("  GRAPH <").append(GRAPH_URI).append("> {").append(System.lineSeparator())
                .append("    ?children rdfs:subClassOf <").append(categoryURI).append(">. ").append(System.lineSeparator())
                .append("    FILTER (!isBlank(?children)).")
                .append("	}").append(System.lineSeparator())
                .append("}");
        return sb.toString();
    }

    private String getParentCategoriesSparql(String categoryURI){
        StringBuilder sb = new StringBuilder("");
        sb.append("PREFIX owl: <http://www.w3.org/2002/07/owl#>").append(System.lineSeparator())
                .append("PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>").append(System.lineSeparator())
                .append("PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> ").append(System.lineSeparator())
                .append("PREFIX mic: <").append(FURNITURE_NS).append(">").append(System.lineSeparator())
                .append(System.lineSeparator())
                .append("SELECT ?parent WHERE {").append(System.lineSeparator())
                .append("  GRAPH <").append(GRAPH_URI).append("> {").append(System.lineSeparator())
                .append("    <").append(categoryURI).append("> rdfs:subClassOf+ ?parent.").append(System.lineSeparator())
                .append("    FILTER (!isBlank(?parent)).")
                .append("	}").append(System.lineSeparator())
                .append("}");
        return sb.toString();
    }
}