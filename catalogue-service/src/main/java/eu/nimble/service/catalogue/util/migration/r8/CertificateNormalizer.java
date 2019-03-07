package eu.nimble.service.catalogue.util.migration.r8;

import eu.nimble.service.catalogue.util.migration.DBConnector;
import eu.nimble.service.model.ubl.commonaggregatecomponents.CertificateType;
import eu.nimble.service.model.ubl.commonbasiccomponents.CodeType;
import eu.nimble.utility.HibernateUtility;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Created by suat on 26-Feb-19.
 */
public class CertificateNormalizer extends DBConnector {
    private static final org.slf4j.Logger logger = LoggerFactory.getLogger(CertificateNormalizer.class);

    List<String> defaultCertificateTypes = new ArrayList<>(Arrays.asList("Appearance Approval Report",
            "Checking Aids",
            "Control Plan",
            "Customer Engineering Approval",
            "Customer Specific Requirements",
            "Design Documentation",
            "Design Failure Mode and Effects Analysis",
            "Dimensional Results",
            "Engineering Change Documentation",
            "Initial Process Studies",
            "Master Sample",
            "Measurement System Analysis Studies",
            "Part Submission Warrant",
            "Process Failure Mode and Effects Analysis",
            "Process Flow Diagram",
            "Qualified Laboratory Documentation",
            "Records of Material / Performance Tests",
            "Sample Production Parts"));

    List<String> fmpCertificates = new ArrayList<>(Arrays.asList(
            "Health and Safety",
            "Innovation",
            "Management",
            "Quality",
            "Sustainability and Environment"));

    public static void main(String[] args) {
        new CertificateNormalizer().normalizeCertificates();
    }

    public void normalizeCertificates() {
        String instance = System.getenv("ENV");
        HibernateUtility hu = getHibernateUtility();
        List<CertificateType> certificates = (List<CertificateType>) hu.loadAll(CertificateType.class);

        for(CertificateType certificate : certificates) {
            String certificateType = certificate.getCertificateType();
            List<String> certificateTypes = defaultCertificateTypes;
            if(instance.contentEquals("fmp")) {
                certificateTypes = fmpCertificates;
            }

            if(!certificateTypes.contains(certificateType)) {
                CodeType certificateTypeCode = certificate.getCertificateTypeCode();
                if(certificateTypeCode == null) {
                    certificateTypeCode = new CodeType();
                    certificate.setCertificateTypeCode(certificateTypeCode);
                }
                certificateTypeCode.setName(certificateType);
                certificate.setCertificateType("Other");
            }
            if(certificateType.contentEquals("Other (Non-PPAP)")) {
                certificate.setCertificateType("Other");
            }
            hu.update(certificate);
        }
        logger.info("Certificates normalized");
    }
}

