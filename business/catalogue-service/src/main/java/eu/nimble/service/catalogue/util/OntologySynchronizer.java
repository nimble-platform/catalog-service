package eu.nimble.service.catalogue.util;

import org.apache.jena.ontology.*;
import org.apache.jena.rdf.model.*;
import org.apache.jena.util.iterator.ExtendedIterator;
import org.apache.jena.vocabulary.RDF;

import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by suat on 26-Jul-17.
 */
public class OntologySynchronizer {
    private OntModel catalogueOntology = null;
    private List<String> ontologyClasses = new ArrayList<>();
    private List<String> ontologyObjectProperties = new ArrayList<>();
    private List<String> ontologyDatatypeProperties = new ArrayList<>();

    private OntModel generatedCatalogueOntology = null;
    private List<String> generatedOntologyClasses = new ArrayList<>();
    private List<String> generatedOntologyObjectProperties = new ArrayList<>();
    private List<String> generatedOntologyDatatypeProperties = new ArrayList<>();

    private OntModel catalogueInstance = null;
    private List<String> instanceTypeCheckedSubjects = new ArrayList();

    private List<String> inconsistentResources = new ArrayList<>();

    public static void main(String[] args) {
        OntologySynchronizer ontologySynchronizer = new OntologySynchronizer();
        ontologySynchronizer.readOntology();
        ontologySynchronizer.parseOntologyTypes();
        ontologySynchronizer.parseInstanceTriples();
        ontologySynchronizer.printInconsistentResources();
    }

    public void readOntology() {
        FileInputStream fis;
        try {
            fis = new FileInputStream("D:\\srdc\\projects\\NIMBLE\\project_starts\\WP2\\T2.2\\quan_ontology_sync\\catalogue_example_rdf.ttl");
            this.catalogueInstance = ModelFactory.createOntologyModel(OntModelSpec.OWL_MEM);
            this.catalogueInstance.read(fis, "a", "TTL");
            fis.close();

            fis = new FileInputStream("D:\\srdc\\projects\\NIMBLE\\project_starts\\WP2\\T2.2\\quan_ontology_sync\\catalogue_ontology.ttl");
            this.generatedCatalogueOntology = ModelFactory.createOntologyModel(OntModelSpec.OWL_MEM);
            this.generatedCatalogueOntology.read(fis, "http://www.nimble-project.org/catalogue#", "TTL");
            fis.close();

            //fis = new FileInputStream("D:\\srdc\\projects\\NIMBLE\\project_starts\\WP2\\T2.2\\ontology_set\\V0.3 20170710\\catalogue v0.8_eva.owl");
            fis = new FileInputStream("D:\\srdc\\projects\\NIMBLE\\project_starts\\codes\\catalog-service-srdc\\business\\ubl-data-model\\src\\main\\schema\\NIMBLE-UBL-2.1-Catalog-Subset\\ontology\\ubl_catalogue_ontology.owl");
            this.catalogueOntology = ModelFactory.createOntologyModel(OntModelSpec.OWL_MEM);
            this.catalogueOntology.read(fis, "http://www.nimble-project.org/catalogue#", "RDF/XML");
            fis.close();

        } catch (java.io.IOException e) {
            throw new RuntimeException("Failed to read ontologies", e);
        }
    }

    public void parseOntologyTypes() {
        ExtendedIterator<OntClass> classIt = catalogueOntology.listClasses();
        while (classIt.hasNext()) {
            Resource ontClass = classIt.next();
            if (!ontClass.isAnon()) {
                //System.out.println("Class: " + ontClass.getLocalName());
                ontologyClasses.add(ontClass.getLocalName());
            }
        }

        ExtendedIterator<ObjectProperty> objectPropertyIt = catalogueOntology.listObjectProperties();
        while (objectPropertyIt.hasNext()) {
            Resource objectProperty = objectPropertyIt.next();
            if (!objectProperty.isAnon()) {
                //System.out.println("Obj prop: " + objectProperty.getLocalName());
                ontologyObjectProperties.add(objectProperty.getLocalName());
            }
        }

        ExtendedIterator<DatatypeProperty> datatypeIt = catalogueOntology.listDatatypeProperties();
        while (datatypeIt.hasNext()) {
            Resource dtProperty = datatypeIt.next();
            if (!dtProperty.isAnon()) {
                //System.out.println("Dt prop: " + dtProperty.getLocalName());
                ontologyDatatypeProperties.add(dtProperty.getLocalName());
            }
        }

        classIt = generatedCatalogueOntology.listClasses();
        while (classIt.hasNext()) {
            Resource ontClass = classIt.next();
            if (!ontClass.isAnon()) {
                //System.out.println("Generated ont class: " + ontClass.getLocalName());
                generatedOntologyClasses.add(ontClass.getLocalName());
            }
        }

        objectPropertyIt = generatedCatalogueOntology.listObjectProperties();
        while (objectPropertyIt.hasNext()) {
            Resource objectProperty = objectPropertyIt.next();
            if (!objectProperty.isAnon()) {
                //System.out.println("Generated ont Obj prop: " + objectProperty.getLocalName());
                generatedOntologyObjectProperties.add(objectProperty.getLocalName());
            }
        }

        datatypeIt = generatedCatalogueOntology.listDatatypeProperties();
        while (datatypeIt.hasNext()) {
            Resource dtProperty = datatypeIt.next();
            if (!dtProperty.isAnon()) {
                //System.out.println("Generated ont Dt prop: " + dtProperty.getLocalName());
                generatedOntologyDatatypeProperties.add(dtProperty.getLocalName());
            }
        }
    }

    public void parseInstanceTriples() {
        StmtIterator stmtIterator = catalogueInstance.listStatements();
        while (stmtIterator.hasNext()) {
            Statement stmt = stmtIterator.next();
            Resource subject = stmt.getSubject();
            if (!instanceTypeCheckedSubjects.contains(subject.toString())) {

                // check the type of subjects are consistent
                if (subject.hasProperty(RDF.type)) {
                    Statement typeStmt = subject.getProperty(RDF.type);
                    Resource object = typeStmt.getObject().asResource();
                    checkType(object, null);
                } else {
                    System.out.println(subject.toString() + " does not have an RDF type");
                }
                instanceTypeCheckedSubjects.add(subject.toString());

                // check the type of the properties
                List<String> checkedProperties = new ArrayList<>();
                StmtIterator stmtIt = subject.listProperties();
                while (stmtIt.hasNext()) {
                    Statement resStmt = stmtIt.next();
                    Resource predicate = resStmt.getPredicate().asResource();
                    if (!checkedProperties.contains(predicate.getLocalName()) && !predicate.equals(RDF.type)) {
                        RDFNode object = resStmt.getObject();
                        if (object.isResource()) {
                            checkType(predicate, ResourceType.OBJECT_PROPERTY);
                        } else if (object.isLiteral()) {
                            checkType(predicate, ResourceType.DATATYPE_PROPERTY);
                        } else {
                            System.out.println("Unknown object for: " + resStmt.getSubject());
                        }
                        checkedProperties.add(resStmt.getPredicate().getLocalName());
                    }
                }
            }
        }
    }

    private void checkType(Resource resource, ResourceType expectedType) {
        //first check the type of the resource is included in catalogue ontology
        String name = resource.getLocalName();
        List<ResourceType> typesInGeneratedOntology = findType(name, true);
        List<ResourceType> typesInOntology = findType(name, false);

        if (checkCommonType(typesInOntology, typesInGeneratedOntology) && typesInGeneratedOntology.size() != 0) {
            /*if (!inconsistentResources.contains(resource.getLocalName())) {
                System.out.println("Resource types are equal for: " + resource.toString());
                System.out.println("Type: " + typesInGeneratedOntology);
            }*/

        } else if (typesInGeneratedOntology != typesInOntology) {
            if (!inconsistentResources.contains(resource.getLocalName())) {
                System.out.println("Resource types are not equal for: " + resource.toString());
                System.out.println("Type in generated ontology: " + typesInGeneratedOntology);
                System.out.println("Type in ontology: " + typesInOntology);
            }
            saveAsInconsistentResource(resource);

        } else if (typesInGeneratedOntology == null) {
            if (!inconsistentResources.contains(resource.getLocalName())) {
                System.out.println("Type in generated ontology is null for: " + resource.toString());
                System.out.println("Type in ontology is " + typesInOntology);
            }
            saveAsInconsistentResource(resource);

        } else if (typesInOntology == null) {
            if (!inconsistentResources.contains(resource.getLocalName())) {
                System.out.println("Type in generated ontology is " + typesInGeneratedOntology + " for: " + resource.toString());
                System.out.println("Type in ontology is null ");
            }
            saveAsInconsistentResource(resource);
        }

        if (expectedType != null) {
            if (!inconsistentResources.contains(resource.getLocalName())) {
                if (!typesInGeneratedOntology.contains(expectedType)) {
                    System.out.println(resource + " has an unexpected type in generated ontology: " + typesInGeneratedOntology);
                }
                if (!typesInOntology.contains(expectedType)) {
                    System.out.println(resource + " has an unexpected type in ontology: " + typesInOntology);
                }
            }
        }
    }

    private void saveAsInconsistentResource(Resource resource) {
        String name = resource.getLocalName();
        if (!inconsistentResources.contains(name)) {
            inconsistentResources.add(name);
        }
    }

    private List<ResourceType> findType(String resourceName, boolean generatedOntology) {
        List<ResourceType> types = new ArrayList<>();
        if (generatedOntology) {
            if (generatedOntologyClasses.contains(resourceName)) {
                types.add(ResourceType.CLASS);
            }
            if (generatedOntologyObjectProperties.contains(resourceName)) {
                types.add(ResourceType.OBJECT_PROPERTY);
            }
            if (generatedOntologyDatatypeProperties.contains(resourceName)) {
                types.add(ResourceType.DATATYPE_PROPERTY);
            }
        } else {
            if (ontologyClasses.contains(resourceName)) {
                types.add(ResourceType.CLASS);
            }
            if (ontologyObjectProperties.contains(resourceName)) {
                types.add(ResourceType.OBJECT_PROPERTY);
            }
            if (ontologyDatatypeProperties.contains(resourceName)) {
                types.add(ResourceType.DATATYPE_PROPERTY);
            }
        }
        return types;
    }

    private boolean checkCommonType(List<ResourceType> typesInOntology, List<ResourceType> typesInGeneratedOntology) {
        for (ResourceType typeInOntology : typesInOntology) {
            for (ResourceType typeInGeneratedOntology : typesInGeneratedOntology) {
                if (typeInOntology == typeInGeneratedOntology) {
                    return true;
                }
            }
        }
        return false;
    }

    private void printInconsistentResources() {
        for (String resourceName : inconsistentResources) {
            System.out.println(resourceName);
        }
    }

    private enum ResourceType {
        CLASS, OBJECT_PROPERTY, DATATYPE_PROPERTY
    }
}
