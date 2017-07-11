package eu.nimble.service.catalogue.impl.marmotta;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.marmotta.client.ClientConfiguration;
import org.apache.marmotta.client.MarmottaClient;
import org.apache.marmotta.client.exception.MarmottaClientException;
import org.apache.marmotta.client.model.rdf.RDFNode;
import org.apache.marmotta.client.model.sparql.SPARQLResult;

import eu.nimble.service.catalogue.category.datamodel.Category;

public class MarmottaAdapter {
	private static final String MARMOTTA_URI = "http://134.168.33.237:8080/marmotta";
	private static final String GRAPH_URI = "http://134.168.33.237:8080/marmotta/context/micuna";

	private MarmottaClient client;
	public MarmottaAdapter() {
		// TODO: take the marmotta base uri form a parameter
		ClientConfiguration config = new ClientConfiguration(MARMOTTA_URI);
		client = new MarmottaClient(config);
		
	}
	
	public static void main(String[]args) throws Exception {
		List<Category> cats = new MarmottaAdapter().getCategoryByName("Tro");
		for ( Category c : cats ) {
			System.out.println(c.getPreferredName());
		}
	}

	
	List<Category> getCategoryByName(String name) throws IOException, MarmottaClientException {
		List<Category> result = new ArrayList<>();
		SPARQLResult sparqlResult = client.getSPARQLClient().select(
				"PREFIX owl: <http://www.w3.org/2002/07/owl#>" + System.lineSeparator() +  
				"PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>" + System.lineSeparator() +  
				"PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> " + System.lineSeparator() +  
				"PREFIX mic: <http://www.semanticweb.org/ontologies/2013/4/Ontology1367568797694.>" + System.lineSeparator() +   
				"" + System.lineSeparator() +   
				"SELECT ?uri ?name WHERE {" + System.lineSeparator() +  
				"  GRAPH <http://134.168.33.237:8080/marmotta/context/micuna> {" + System.lineSeparator() +  
				"    ?uri rdf:type owl:Class . " + System.lineSeparator() +  
				"    ?uri mic:owltranslation ?name ." + 
				"    FILTER regex (?name, \"" + name + "\") ." + System.lineSeparator() +  
				"	}" + System.lineSeparator() +   
				"}");
		if (sparqlResult != null ) {
			for ( int i = 0; i<  sparqlResult.size(); i++ ) {
				Map<String, RDFNode> record = sparqlResult.get(i);
				Category cat = new Category();
				cat.setCategoryUri(record.get("uri").toString());
				cat.setPreferredName(record.get("name").toString());
				result.add(cat);
			}
		}
		return result;
	}
}
