/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package eu.nimble.service.catalogue.impl;

import eu.nimble.data.transformer.ontmalizer.XML2OWLMapper;
import eu.nimble.data.transformer.ontmalizer.XSD2OWLMapper;
import eu.nimble.service.catalogue.CatalogueService;
import eu.nimble.service.catalogue.category.datamodel.Category;
import eu.nimble.service.catalogue.exception.CatalogueServiceException;
import eu.nimble.service.catalogue.exception.TemplateParseException;
import eu.nimble.service.catalogue.impl.template.TemplateGenerator;
import eu.nimble.service.catalogue.impl.template.TemplateParser;
import eu.nimble.service.catalogue.util.ConfigUtil;
import eu.nimble.service.model.modaml.catalogue.TEXCatalogType;
import eu.nimble.service.model.ubl.catalogue.CatalogueType;
import eu.nimble.service.model.ubl.commonaggregatecomponents.*;
import eu.nimble.utility.Configuration;
import eu.nimble.utility.HibernateUtility;
import eu.nimble.utility.JAXBUtility;
import org.apache.commons.io.IOUtils;
import org.apache.poi.ss.formula.functions.T;
import org.apache.poi.ss.usermodel.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sun.reflect.generics.reflectiveObjects.NotImplementedException;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.namespace.QName;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static eu.nimble.service.catalogue.util.ConfigUtil.*;

/**
 * @author yildiray
 */
public class CatalogueServiceImpl implements CatalogueService {

    private static final Logger logger = LoggerFactory.getLogger(CatalogueServiceImpl.class);
    private static CatalogueService instance = null;
    private static CategoryServiceManager csmInstance = CategoryServiceManager.getInstance();

    private CatalogueServiceImpl() {
    }

    public static CatalogueService getInstance() {
        if (instance == null) {
            return new CatalogueServiceImpl();
        } else {
            return instance;
        }
    }

    public static void main(String[] args) throws IOException {
        CatalogueServiceImpl csi = new CatalogueServiceImpl();

        String filePath = "C:\\Users\\suat\\Desktop\\multtemp" + System.currentTimeMillis() + ".xlsx";
        List<String> categoryIds = new ArrayList<>();
        categoryIds.add("0173-1#01-AKJ052#013");
        //categoryIds.add("http://www.semanticweb.org/ontologies/2017/8/FurnitureSectorOntology.owl#Glue");
        categoryIds.add("http://www.semanticweb.org/ontologies/2017/8/FurnitureSectorOntology.owl#MDFBoard");
        //categoryIds.add("0173-1#01-BAC439#012");
        List<String> taxonomyIds = new ArrayList<>();
        taxonomyIds.add("eClass");
        taxonomyIds.add("FurnitureOntology");
        //taxonomyIds.add("eClass");
        Workbook wb = csi.generateTemplateForCategory(categoryIds, taxonomyIds);
        wb.write(new FileOutputStream(filePath));
        wb.close();

//        String filePath = "C:\\Users\\suat\\Desktop\\multtemp.xlsx";
//        InputStream is = new FileInputStream(filePath);
//        PartyType party = new PartyType();
//        CatalogueType catalogue = csi.addCatalogue(is, party);
//        System.out.println(catalogue.getCatalogueLine().size());
    }

    @Override
    public CatalogueType addCatalogue(CatalogueType catalogue) {
        return addCatalogue(catalogue, Configuration.Standard.UBL);
    }

    @Override
    public CatalogueType addCatalogue(String catalogueXml) {
        return addCatalogue(catalogueXml, Configuration.Standard.UBL);
    }

    @Override
    public CatalogueType getCatalogue(String uuid) {
        return getCatalogue(uuid, Configuration.Standard.UBL);
    }

    @Override
    public CatalogueType getCatalogue(String id, String partyId) {
        return getCatalogue(id, partyId, Configuration.Standard.UBL);
    }

    @Override
    public CatalogueType updateCatalogue(CatalogueType catalogue) {
        logger.info("Catalogue with uuid: {} will be updated", catalogue.getUUID());

        // Assign IDs to lines that are missing it
        for (CatalogueLineType catalogueLine : catalogue.getCatalogueLine()) {
            if (catalogueLine.getID() == null) {
                catalogueLine.setID(UUID.randomUUID().toString());
            }
        }

        // merge the hibernate object
        HibernateUtility.getInstance(Configuration.UBL_PERSISTENCE_UNIT_NAME).update(catalogue);

        // delete the catalgoue from marmotta and submit once again
        deleteCatalogueFromMarmotta(catalogue.getUUID());

        // submit again
        submitCatalogueDataToMarmotta(catalogue);
        logger.info("Catalogue with uuid: {} updated", catalogue.getUUID());
        return catalogue;
    }

    @Override
    public void deleteCatalogue(String uuid) {
        deleteCatalogue(uuid, Configuration.Standard.UBL);
    }

    @Override
    public void deleteCatalogue(String id, String partyId) {
        CatalogueType catalogue = getCatalogue(id, partyId);
        deleteCatalogue(catalogue.getUUID());
    }

    @Override
    public <T> T addCatalogue(String catalogueXml, Configuration.Standard standard) {
        T catalogue = null;
        if (standard == Configuration.Standard.UBL) {
            CatalogueType ublCatalogue = (CatalogueType) JAXBUtility.deserialize(catalogueXml, Configuration.UBL_CATALOGUE_PACKAGENAME);
            catalogue = (T) ublCatalogue;

        } else if (standard == Configuration.Standard.MODAML) {
            catalogue = (T) JAXBUtility.deserialize(catalogueXml, Configuration.MODAML_CATALOGUE_PACKAGENAME);
        }
        addCatalogue(catalogue, standard);

        return catalogue;
    }

    @Override
    public <T> T addCatalogue(T catalogue, Configuration.Standard standard) {
        if (standard == Configuration.Standard.UBL) {
            // create a globally unique identifier
            CatalogueType ublCatalogue = (CatalogueType) catalogue;
            String uuid = UUID.randomUUID().toString();
            ublCatalogue.setUUID(uuid);

            // persist the catalogue in relational DB
            HibernateUtility.getInstance(Configuration.UBL_PERSISTENCE_UNIT_NAME).persist(ublCatalogue);
            logger.info("Catalogue with uuid: {} persisted in DB", uuid.toString());

            // persist the catalogue also in Marmotta
            submitCatalogueDataToMarmotta(ublCatalogue);

        } else if (standard == Configuration.Standard.MODAML) {
            HibernateUtility.getInstance(Configuration.MODAML_PERSISTENCE_UNIT_NAME).persist(catalogue);
        }
        return catalogue;
    }

    @Override
    public <T> T getCatalogue(String uuid, Configuration.Standard standard) {
        T catalogue = null;
        List<T> resultSet = null;

        String query;
        if (standard == Configuration.Standard.UBL) {
            query = "SELECT catalogue FROM CatalogueType catalogue "
                    + " WHERE catalogue.UUID = '" + uuid + "'";

            resultSet = (List<T>) HibernateUtility.getInstance(Configuration.UBL_PERSISTENCE_UNIT_NAME)
                    .loadAll(query);
            if (resultSet.size() > 0) {
                catalogue = (T) resultSet.get(0);
            }

        } else if (standard == Configuration.Standard.MODAML) {
            query = "SELECT catalogue FROM TEXCatalogType catalogue "
                    + " JOIN FETCH catalogue.TCheader catalogue_header "
                    + " WHERE catalogue_header.msgID = '" + uuid + "'";
            resultSet = (List<T>) HibernateUtility.getInstance(Configuration.MODAML_PERSISTENCE_UNIT_NAME)
                    .loadAll(query);

        }
        if (resultSet != null && resultSet.size() > 0) {
            catalogue = resultSet.get(0);
        }

        return catalogue;
    }

    @Override
    public <T> T getCatalogue(String id, String partyId, Configuration.Standard standard) {
        T catalogue = null;
        List<T> resultSet = null;

        String query;
        if (standard == Configuration.Standard.UBL) {
            query = "SELECT catalogue FROM CatalogueType as catalogue "
                    + " JOIN catalogue.providerParty as catalogue_provider_party"
                    + " WHERE catalogue.ID = '" + id + "'"
                    + " AND catalogue_provider_party.ID = '" + partyId + "'";

            resultSet = (List<T>) HibernateUtility.getInstance(Configuration.UBL_PERSISTENCE_UNIT_NAME)
                    .loadAll(query);

        } else if (standard == Configuration.Standard.MODAML) {
            logger.warn("Fetching catalogues with id and party id from MODAML repository is not implemented yet");
            throw new NotImplementedException();
        }

        if (resultSet.size() > 0) {
            catalogue = resultSet.get(0);
        }

        return catalogue;
    }

    @Override
    public void deleteCatalogue(String uuid, Configuration.Standard standard) {
        if (standard == Configuration.Standard.UBL) {
            logger.info("Deleting catalogue with uuid: {}", uuid);
            // delete catalogue from relational db
            CatalogueType catalogue = getCatalogue(uuid);

            if (catalogue != null) {
                Long hjid = catalogue.getHjid();
                HibernateUtility.getInstance(Configuration.UBL_PERSISTENCE_UNIT_NAME).delete(CatalogueType.class, hjid);

                // delete catalogue from marmotta
                deleteCatalogueFromMarmotta(uuid);
                logger.info("Deleted catalogue with uuid: {}", uuid);
            } else {
                logger.info("No catalogue for uuid: {}", uuid);
            }

        } else if (standard == Configuration.Standard.MODAML) {
            TEXCatalogType catalogue = getCatalogue(uuid, Configuration.Standard.MODAML);
            Long hjid = catalogue.getHjid();
            HibernateUtility.getInstance(Configuration.MODAML_PERSISTENCE_UNIT_NAME).delete(TEXCatalogType.class, hjid);
        }
    }

    private void deleteCatalogueFromMarmotta(String uuid) {
        boolean indexToMarmotta = Boolean.valueOf(ConfigUtil.getInstance().getConfig(CONFIG_CATALOGUE_PERSISTENCE_MARMOTTA_INDEX));
        if (indexToMarmotta == false) {
            logger.info("Index to Marmotta is set to false");
            return;
        }

        logger.info("Catalogue with uuid: {} will be deleted from Marmotta", uuid);

        URL marmottaURL;
        try {
            String marmottaBaseUrl = ConfigUtil.getInstance().getConfig(CONFIG_CATALOGUE_PERSISTENCE_MARMOTTA_URL);
            marmottaURL = new URL(marmottaBaseUrl + "/context/" + uuid);
        } catch (IOException e) {
            throw new CatalogueServiceException("Failed to read Marmotta URL from config file", e);
        }

        HttpURLConnection conn;
        try {
            conn = (HttpURLConnection) marmottaURL.openConnection();
            conn.setRequestMethod("DELETE");
            conn.setDoOutput(true);
            conn.setConnectTimeout(18000);
            conn.setReadTimeout(18000);

            OutputStream os = conn.getOutputStream();
            os.flush();

            logger.info("Marmotta response for deleting catalogue with uuid: {}: {}", uuid, conn.getResponseCode());

            conn.disconnect();
        } catch (IOException e) {
            throw new CatalogueServiceException("Failed to submit catalogue to Marmotta", e);
        }
        logger.info("Catalogue with uuid: {} deleted from Marmotta", uuid);
    }

    @Override
    public Workbook generateTemplateForCategory(List<String> categoryIds, List<String> taxonomyIds) {
        List<Category> categories = new ArrayList<>();
        for (int i = 0; i < categoryIds.size(); i++) {
            Category category = csmInstance.getCategory(taxonomyIds.get(i), categoryIds.get(i));
            categories.add(category);
        }

        TemplateGenerator templateGenerator = new TemplateGenerator();
        Workbook template = templateGenerator.generateTemplateForCategory(categories);
        return template;
    }

    @Override
    public CatalogueType addCatalogue(InputStream catalogueTemplate, PartyType party) {
        TemplateParser templateParser = new TemplateParser(party);
        List<CatalogueLineType> catalogueLines = null;
        try {
            catalogueLines = templateParser.getCatalogueLines(catalogueTemplate);
        } catch (TemplateParseException e) {
            throw new CatalogueServiceException("Failed to parse the template", e);
        }

        // Assign IDs to lines that are missing it
        for (CatalogueLineType catalogueLine : catalogueLines) {
            if (catalogueLine.getID() == null) {
                catalogueLine.setID(UUID.randomUUID().toString());
            }
        }

        CatalogueType catalogue = getCatalogue("default", party.getID());

        if(catalogue == null) {
            catalogue = new CatalogueType();
            catalogue.setID("default");
            catalogue.setProviderParty(party);
            catalogue.setCatalogueLine(catalogueLines);
            return addCatalogue(catalogue);
        } else {
            catalogue.getCatalogueLine().addAll(catalogueLines);
            return updateCatalogue(catalogue);
        }
    }

    private XML2OWLMapper transformCatalogueToRDF(CatalogueType catalogue) {
        // TODO generate the ontology once, once the data model is finalized
        XSD2OWLMapper mapping = getXSDToOWLMapping();

        ByteArrayOutputStream serializedCatalogueBaos = new ByteArrayOutputStream();
        StringWriter serializedCatalogueWriter = new StringWriter();
        try {
            String packageName = catalogue.getClass().getPackage().getName();
            JAXBContext jc = JAXBContext.newInstance(packageName);

            Marshaller marsh = jc.createMarshaller();
            marsh.setProperty("jaxb.formatted.output", true);
            JAXBElement element = new JAXBElement(
                    new QName(Configuration.UBL_CATALOGUE_NS, "Catalogue"), catalogue.getClass(), catalogue);
            marsh.marshal(element, serializedCatalogueBaos);
            marsh.marshal(element, serializedCatalogueWriter);

        } catch (JAXBException e) {
            throw new CatalogueServiceException("Failed to serialize the catalogue instance to XML", e);
        }

        // log the catalogue to be transformed
        logger.debug("Catalogue to be transformed:\n{}", serializedCatalogueWriter.toString());
        serializedCatalogueWriter.flush();

        try {
            ByteArrayInputStream bais = new ByteArrayInputStream(serializedCatalogueBaos.toByteArray());
            serializedCatalogueBaos.flush();
            XML2OWLMapper generator = new XML2OWLMapper(bais, mapping);
            generator.convertXML2OWL();

            serializedCatalogueBaos.close();
            bais.close();

            StringWriter catalogueRDFWriter = new StringWriter();
            generator.writeModel(catalogueRDFWriter, "N3");
            logger.debug("Transformed RDF data:\n{}", catalogueRDFWriter.toString());
            catalogueRDFWriter.flush();

            return generator;

        } catch (IOException e) {
            throw new CatalogueServiceException("Failed to convert catalogue with uuid " + catalogue.getUUID() + " to RDF", e);
        }
    }

    private XSD2OWLMapper getXSDToOWLMapping() {
        URL url = CatalogueServiceImpl.class.getResource(Configuration.UBL_CATALOGUE_SCHEMA);
        XSD2OWLMapper mapping = new XSD2OWLMapper(url);
        mapping.setObjectPropPrefix("");
        mapping.setDataTypePropPrefix("");
        mapping.convertXSD2OWL();

        // log the ontology generated based on the XSD schema
        StringWriter serializedOntology = new StringWriter();
        mapping.writeOntology(serializedOntology, "N3");
        //logger.debug("Serialized ontology:\n{}", serializedOntology.toString());
        //serializedOntology.flush();

        return mapping;
    }

    private void submitCatalogueDataToMarmotta(CatalogueType catalogue) {


        logger.info("Catalogue with uuid: {} will be submitted to Marmotta.", catalogue.getUUID());
        XML2OWLMapper rdfGenerator = transformCatalogueToRDF(catalogue);
        logger.info("Transformed catalogue with uuid: {} to RDF", catalogue.getUUID());

        boolean indexToMarmotta = Boolean.valueOf(ConfigUtil.getInstance().getConfig(CONFIG_CATALOGUE_PERSISTENCE_MARMOTTA_INDEX));
        if (indexToMarmotta == false) {
            logger.info("Index to Marmotta is set to false");
            return;
        }

        URL marmottaURL;
        try {
            String marmottaBaseUrl = ConfigUtil.getInstance().getConfig(CONFIG_CATALOGUE_PERSISTENCE_MARMOTTA_URL);
            marmottaURL = new URL(marmottaBaseUrl + "/import/upload?context=" + catalogue.getUUID());
        } catch (MalformedURLException e) {
            throw new CatalogueServiceException("Invalid format for the submitted template", e);
        } catch (IOException e) {
            throw new CatalogueServiceException("Failed to read Marmotta URL from config file", e);
        }

        HttpURLConnection conn = null;
        try {
            conn = (HttpURLConnection) marmottaURL.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "text/n3");
            conn.setDoOutput(true);
            conn.setConnectTimeout(18000);
            conn.setReadTimeout(18000);

            OutputStream os = conn.getOutputStream();
            rdfGenerator.writeModel(os, "N3");
            os.flush();

            logger.info("Catalogue with uuid: {} submitted to Marmotta. Received HTTP response: {}", catalogue.getUUID(), conn.getResponseCode());
            if(conn.getResponseCode() == 500) {
                InputStream error = conn.getErrorStream();
                logger.error("Error from Marmotta: " + IOUtils.toString(error));
            }

            conn.disconnect();
        } catch (IOException e) {
            throw new CatalogueServiceException("Failed to submit catalogue to Marmotta", e);
        }
    }

    /*
     * Catalogue-line level endpoints
     */

    @Override
    public <T> T getCatalogueLine(String id) {
        T catalogueLine = null;
        List<T> resultSet = null;

        String query;
        query = "Select catalogue_line FROM CatalogueLineType as catalogue_line "
                + " WHERE catalogue_line.ID = '" + id + "'";

        resultSet = (List<T>) HibernateUtility.getInstance(Configuration.UBL_PERSISTENCE_UNIT_NAME)
                .loadAll(query);
        if (resultSet.size() > 0) {
            catalogueLine = (T) resultSet.get(0);
        }

        return catalogueLine;
    }

    // TODO test
    @Override
    public CatalogueLineType addLineToCatalogue(CatalogueType catalogue, CatalogueLineType catalogueLine) {
        catalogueLine.setID(UUID.randomUUID().toString());
        catalogue.getCatalogueLine().add(catalogueLine);

        HibernateUtility.getInstance(Configuration.UBL_PERSISTENCE_UNIT_NAME).update(catalogue);

        return catalogueLine;
    }

    @Override
    public CatalogueLineType updateCatalogueLine(CatalogueLineType catalogueLine) {
        HibernateUtility.getInstance(Configuration.UBL_PERSISTENCE_UNIT_NAME).update(catalogueLine);
/*
        // delete the catalgoue from marmotta and submit once again
        deleteCatalogueFromMarmotta(catalogue.getUUID());

        // submit again
        submitCatalogueDataToMarmotta(catalogue);*/

        return catalogueLine;
    }

    @Override
    public void deleteCatalogueLineById(String catalogueId, String id) {
        // delete catalogue from relational db
        CatalogueLineType catalogueLine = getCatalogueLine(id);

        if (catalogueLine != null) {
            Long hjid = catalogueLine.getHjid();
            HibernateUtility.getInstance(Configuration.UBL_PERSISTENCE_UNIT_NAME).delete(CatalogueLineType.class, hjid);

            CatalogueType catalogue = getCatalogue(catalogueId);
            // delete catalogue from marmotta
            deleteCatalogueFromMarmotta(catalogueId);
            // submit again
            submitCatalogueDataToMarmotta(catalogue);
        }
    }
}
