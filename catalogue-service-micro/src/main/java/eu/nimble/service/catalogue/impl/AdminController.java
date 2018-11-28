package eu.nimble.service.catalogue.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import eu.nimble.data.transformer.ontmalizer.XML2OWLMapper;
import eu.nimble.service.catalogue.CatalogueService;
import eu.nimble.service.catalogue.CatalogueServiceImpl;
import eu.nimble.service.catalogue.sync.MarmottaClient;
import eu.nimble.service.catalogue.sync.MarmottaSynchronizationException;
import eu.nimble.service.model.ubl.catalogue.CatalogueType;
import eu.nimble.service.model.ubl.commonaggregatecomponents.ItemType;
import eu.nimble.service.model.ubl.commonaggregatecomponents.PartyType;
import eu.nimble.service.model.ubl.despatchadvice.DespatchAdviceType;
import eu.nimble.service.model.ubl.iteminformationrequest.ItemInformationRequestType;
import eu.nimble.service.model.ubl.iteminformationresponse.ItemInformationResponseType;
import eu.nimble.service.model.ubl.order.OrderType;
import eu.nimble.service.model.ubl.orderresponsesimple.OrderResponseSimpleType;
import eu.nimble.service.model.ubl.quotation.QuotationType;
import eu.nimble.service.model.ubl.receiptadvice.ReceiptAdviceType;
import eu.nimble.service.model.ubl.requestforquotation.RequestForQuotationType;
import eu.nimble.utility.Configuration;
import eu.nimble.utility.HibernateUtility;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * Created by suat on 02-Mar-18.
 */
@Controller
public class AdminController {
    private static Logger log = LoggerFactory.getLogger(AdminController.class);

    private CatalogueService service = CatalogueServiceImpl.getInstance();

//    @Autowired
//    private CustomRepo repo;
//
//    @Autowired
//    private CatalogueRepository catRep;

    @CrossOrigin(origins = {"*"})
    @RequestMapping(value = "/admin/catalogue/{standard}",
            consumes = {MediaType.APPLICATION_JSON_VALUE},
            produces = {MediaType.APPLICATION_XML_VALUE, "application/rdf+xml"},
            method = RequestMethod.POST)
    public ResponseEntity transformCatalogue(@PathVariable String standard, @RequestBody String serializedCatalogue, HttpServletRequest request, HttpServletResponse response) {
        log.info("Incoming request for transforming catalogue");

        String contentType = request.getContentType();
        CatalogueType catalogue;
        try {
            catalogue = new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false).readValue(serializedCatalogue, CatalogueType.class);
        } catch (IOException e) {
            log.error("Failed to parse the catalogue: {}", serializedCatalogue, e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Failed to parse the catalogue");
        }

        MarmottaClient marmottaClient = new MarmottaClient();
        XML2OWLMapper rdfGenerator;
        try {
            rdfGenerator = marmottaClient.transformCatalogueToRDF(catalogue);
        } catch (MarmottaSynchronizationException e) {
            log.error("Failed to transform the catalogue to RDF: {}", serializedCatalogue, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to transform the catalogue to RDF");
        }

        try {
            rdfGenerator.writeModel(response.getOutputStream(), "RDF/XML");
        } catch (IOException e) {
            log.error("Failed to write the  RDF transformation to response output stream: {}", serializedCatalogue, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to write the  RDF transformation to response output stream");
        }

        log.info("Catalogue transformed to XML");
        return ResponseEntity.status(HttpStatus.OK).build();
    }

    @CrossOrigin(origins = {"*"})
    @RequestMapping(value = "/admin/catalogue/{uuid}",
            consumes = {MediaType.APPLICATION_JSON_VALUE},
            produces = {MediaType.APPLICATION_XML_VALUE, "application/rdf+xml"},
            method = RequestMethod.GET)
    public ResponseEntity getCatalogueRDF(@PathVariable String uuid, HttpServletRequest request, HttpServletResponse response) {
        log.info("Incoming request for transforming catalogue");

        CatalogueType catalogue = service.getCatalogue(uuid);

        MarmottaClient marmottaClient = new MarmottaClient();
        XML2OWLMapper rdfGenerator;
        try {
            rdfGenerator = marmottaClient.transformCatalogueToRDF(catalogue);
        } catch (MarmottaSynchronizationException e) {
            log.error("Failed to transform the catalogue to RDF for uuid: {}", uuid, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to transform the catalogue to RDF");
        }

        try {
            rdfGenerator.writeModel(response.getOutputStream(), "RDF/XML");
        } catch (IOException e) {
            log.error("Failed to write the RDF transformation to response output stream: {}", uuid, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to write the  RDF transformation to response output stream");
        }

        log.info("Catalogue transformed to XML");
        return ResponseEntity.status(HttpStatus.OK).build();
    }

    @ExceptionHandler(Exception.class)
    public void handleError(HttpServletRequest req, Exception ex) {
        log.error("Request: " + req.getRequestURL() + " raised " + ex);
        ex.printStackTrace();
    }

//    @CrossOrigin(origins = {"*"})
//    @RequestMapping(value = "/admin/temp",
//            produces = {MediaType.APPLICATION_JSON_VALUE},
//            method = RequestMethod.GET)
//    @Transactional(transactionManager = "ubldbTransactionManager")
//    public ResponseEntity tempMethod() {
//        ItemType c = repo.getCatalogueByUuid();
////        CatalogueType c2 = catRep.getCatalogue("3f25673f-7fd2-4c34-9079-b9c2ecd515ba").get(0);
//        c = catRep.getCatalogue((long) 147).get(0);
//        ObjectMapper om = new ObjectMapper();
//        String serializedCatalogue;
//        try {
//            serializedCatalogue = om.writeValueAsString(c);
//        } catch (JsonProcessingException e) {
//            e.printStackTrace();
//            return null;
//        }
//        return ResponseEntity.status(HttpStatus.OK).body(serializedCatalogue);
//    }
}