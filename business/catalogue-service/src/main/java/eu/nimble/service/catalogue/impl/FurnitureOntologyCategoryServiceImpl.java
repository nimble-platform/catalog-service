package eu.nimble.service.catalogue.impl;

import eu.nimble.service.catalogue.ProductCategoryService;
import eu.nimble.service.catalogue.category.datamodel.Category;
import eu.nimble.service.catalogue.category.datamodel.Property;
import eu.nimble.service.catalogue.exception.ProductCategoryServiceException;
import org.apache.log4j.Logger;
import org.apache.marmotta.client.ClientConfiguration;
import org.apache.marmotta.client.MarmottaClient;
import org.apache.marmotta.client.clients.SPARQLClient;
import org.apache.marmotta.client.exception.MarmottaClientException;
import org.apache.marmotta.client.model.rdf.RDFNode;
import org.apache.marmotta.client.model.sparql.SPARQLResult;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Created by suat on 07-Jul-17.
 */
public class FurnitureOntologyCategoryServiceImpl implements ProductCategoryService {
    private static final String MARMOTTA_URI = "http://134.168.33.237:8080/marmotta";
    private static final String GRAPH_URI = "http://134.168.33.237:8080/marmotta/context/micuna";
    private static final String FURNITURE_NS = "http://www.semanticweb.org/ontologies/2013/4/Ontology1367568797694.owl#";
    private static final String XSD_NS = "http://www.w3.org/2001/XMLSchema#";

    private static final Logger log = Logger.getLogger(FurnitureOntologyCategoryServiceImpl.class);

    private MarmottaClient client;

    private List<Category> categories;

    public FurnitureOntologyCategoryServiceImpl() {
        categories = new ArrayList<>();
        Category category = new Category();
        category.setTaxonomyId(getTaxonomyId());
        category.setCode("MDFBoard");
        category.setPreferredName("MDF Board");
        category.setId("MDFBoard");

        Property property = new Property();
        property.setId("Material_Composition");
        property.setPreferredName("Material Composition");
        property.setDataType("STRING");

        List<Property> properties = new ArrayList<>();
        properties.add(property);
        category.setProperties(properties);

        categories.add(category);

        // TODO: take the marmotta base uri form a parameter
        ClientConfiguration config = new ClientConfiguration(MARMOTTA_URI);
        client = new MarmottaClient(config);
    }

    public static void main(String[] args) {
        FurnitureOntologyCategoryServiceImpl f = new FurnitureOntologyCategoryServiceImpl();
        //f.getProductCategories("MDF");
        f.getCategory("http://www.semanticweb.org/ontologies/2013/4/Ontology1367568797694.owl#MDFBoard");
        //f.getParentClassSparql("http://www.semanticweb.org/ontologies/2013/4/Ontology1367568797694.owl#MDFBoard");
        //f.getParentCategories(new ArrayList<>(), "http://www.semanticweb.org/ontologies/2013/4/Ontology1367568797694.owl#MDFBoard");
    }

    @Override
    public Category getCategory(String categoryId) {
        /*if (categoryId.equals(categories.get(0).getId())) {
            return categories.get(0);
        } else {
            return null;
        }*/
        // create category from the uri
        Category category = createCategory(categoryId);
        List<Property> properties = new ArrayList<>();
        category.setProperties(properties);

        // find parent categories
        List<String> parents = new ArrayList<>();
        parents.add(categoryId);
        getParentCategories(parents, categoryId);

        // create properties contained
        for (String parent : parents) {
            String datatypeSparql = getDatatypePropertySparql(parent);
            SPARQLClient sparqlClient = client.getSPARQLClient();
            try {
                SPARQLResult dataTypes = sparqlClient.select(datatypeSparql);
                if (dataTypes != null) {
                    for (Map<String, RDFNode> dataType : dataTypes) {
                        String dtUri = getRemainder(dataType.get("dtp").toString(), FURNITURE_NS);
                        String unit = getRemainder(dataType.get("range").toString(), XSD_NS);
                        Property property = createProperty(dtUri, unit);
                        properties.add(property);
                    }
                }
            } catch (IOException | MarmottaClientException e) {
                log.warn("Failed to get datatype properties for category: " + parent, e);
            }
        }

        return category;
    }

    @Override
    public List<Category> getProductCategories(String categoryName) {
        /*if (name.toLowerCase().contentEquals("mdf")) {
            return categories;
        } else {
            return new ArrayList<>();
        }*/
        List<Category> result = new ArrayList<>();
        SPARQLResult sparqlResult = null;
        try {
            String categoriesSparql = getCategoriesSparqlByName(categoryName.toLowerCase());
            sparqlResult = client.getSPARQLClient().select(categoriesSparql);
        } catch (IOException | MarmottaClientException e) {
            e.printStackTrace();
            throw new ProductCategoryServiceException("Failed to retrieve category results for query: " + categoryName + " from Marmotta", e);
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
        property.setDataType(getRemainder(range, FURNITURE_NS).toUpperCase());
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

    public void getParentCategories(List<String> parents, String categoryUri) {
        String parentSparql = getParentClassSparql(categoryUri);
        SPARQLClient sparqlClient = client.getSPARQLClient();
        try {
            SPARQLResult results = sparqlClient.select(parentSparql);
            if (results != null) {
                for (Map<String, RDFNode> result : results) {
                    String uri = result.get("parent").toString();
                    parents.add(uri);
                    getParentCategories(parents, uri);
                }
            }
        } catch (IOException | MarmottaClientException e) {
            log.warn("Failed to retrieve parent classes for: " + categoryUri, e);
        }
    }

    @Override
    public String getTaxonomyId() {
        return "FurnitureOntology";
    }

    private String getDatatypePropertySparql(String uri) {
        StringBuilder sb = new StringBuilder("");
        sb.append("PREFIX owl: <http://www.w3.org/2002/07/owl#>").append(System.lineSeparator())
                .append("PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>").append(System.lineSeparator())
                .append("PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> ").append(System.lineSeparator())
                .append("PREFIX mic: <http://www.semanticweb.org/ontologies/2013/4/Ontology1367568797694.owl#>").append(System.lineSeparator())
                .append(System.lineSeparator())
                .append("SELECT ?dtp ?range WHERE {").append(System.lineSeparator())
                .append("  GRAPH <").append(GRAPH_URI).append("> {").append(System.lineSeparator())
                .append("    ?dtp rdfs:domain <").append(uri).append(">.").append(System.lineSeparator())
                .append("    ?dtp rdfs:range ?range.").append(System.lineSeparator())
                .append("	}").append(System.lineSeparator())
                .append("}");
        return sb.toString();
    }

    private String getParentClassSparql(String uri) {
        StringBuilder sb = new StringBuilder("");
        sb.append("PREFIX owl: <http://www.w3.org/2002/07/owl#>").append(System.lineSeparator())
                .append("PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>").append(System.lineSeparator())
                .append("PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> ").append(System.lineSeparator())
                .append("PREFIX mic: <http://www.semanticweb.org/ontologies/2013/4/Ontology1367568797694.owl#>").append(System.lineSeparator())
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
                .append("PREFIX mic: <http://www.semanticweb.org/ontologies/2013/4/Ontology1367568797694.owl#>").append(System.lineSeparator())
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
