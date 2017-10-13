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

                // check the remaining properties associated to the subject
                List<String> checkedProperties = new ArrayList<>();
                StmtIterator stmtIt = subject.listProperties();
                while (stmtIt.hasNext()) {
                    Statement resStmt = stmtIt.next();
                    Resource predicate = resStmt.getPredicate().asResource();
                    if (!checkedProperties.contains(predicate.getLocalName()) && !predicate.equals(RDF.type)) {

                        // check the type of the property
                        RDFNode object = resStmt.getObject();
                        if (object.isResource()) {
                            checkType(predicate, ResourceType.OBJECT_PROPERTY);
                        } else if (object.isLiteral()) {
                            checkType(predicate, ResourceType.DATATYPE_PROPERTY);
                        } else {
                            System.out.println("Unknown object for: " + resStmt.getSubject());
                        }

                        // check the linkage of the property by checking the types of domain and range
                        checkPropertyLinkage(resStmt);

                        checkedProperties.add(resStmt.getPredicate().getLocalName());
                    }
                }
            }
        }
    }

    private void checkPropertyLinkage(Statement statement) {
        Resource subject = statement.getSubject();
        Property predicate = statement.getPredicate();
        RDFNode object = statement.getObject();

        // get domain and range of the predicate from the ontology
        OntProperty predicateInOntology = catalogueOntology.getOntProperty(catalogueOntology.getNsPrefixURI("") + predicate.getLocalName());
        if (predicateInOntology == null) {
            System.out.println("No property in ontology for: " + catalogueOntology.getNsPrefixURI("") + predicate.getLocalName());
            return;
        }

        List<String> domainClasses = new ArrayList<>();
        OntClass propDomain = predicateInOntology.getDomain().asClass();
        if (propDomain.isUnionClass()) {
            ExtendedIterator<? extends OntClass> operands = propDomain.asUnionClass().listOperands();
            while (operands.hasNext()) {
                domainClasses.add(operands.next().getURI());
            }
        } else {
            domainClasses.add(propDomain.getURI());
        }

        // get range of the predicate
        OntResource range = predicateInOntology.getRange();

        // get the type of the subject
        Resource subjectType = subject.getPropertyResourceValue(RDF.type);

        // get type of the object
        String objectType = null;
        if (object.isResource()) {
            objectType = object.asResource().getPropertyResourceValue(RDF.type).getURI();
        } else if (object.isLiteral()) {
            objectType = object.asLiteral().getDatatype().getURI();
        }

        // compare the types of the subject and object with the domain and range of the property

        // check the domain
        boolean domainsEqual = false;
        for (String domainClass : domainClasses) {
            if(getLocalName(domainClass).contentEquals(subjectType.getLocalName())) {
                domainsEqual = true;
                break;
            }
        }
        if(!domainsEqual) {
            System.out.println("Domain of " + predicate + " is inconsistent");
            System.out.println("Subject: " + subject);
            System.out.println("Domains: " + domainClasses);
            System.out.println("Subject type: " + subjectType);
            System.out.println();
        }

        // check range
        if(!getLocalName(range.getURI()).contentEquals(getLocalName(objectType))) {
            System.out.println("Range of " + predicate + " is inconsistent");
            System.out.println("Subject: " + subject);
            System.out.println("Range: " + range);
            System.out.println("Object type: " + objectType);
            System.out.println();
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

    private String getLocalName(String uri) {
        return uri.substring(uri.indexOf("#") + 1);
    }

    private enum ResourceType {
        CLASS, OBJECT_PROPERTY, DATATYPE_PROPERTY
    }
}
