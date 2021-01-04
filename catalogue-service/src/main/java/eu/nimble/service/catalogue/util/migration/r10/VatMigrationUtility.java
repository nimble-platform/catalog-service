package eu.nimble.service.catalogue.util.migration.r10;

import eu.nimble.service.catalogue.util.migration.r9.MultilingualTextTypeCreator;
import eu.nimble.service.model.ubl.commonaggregatecomponents.CatalogueLineType;
import eu.nimble.service.model.ubl.commonaggregatecomponents.TaxCategoryType;
import eu.nimble.utility.country.CountryUtil;
import eu.nimble.utility.persistence.GenericJPARepository;
import eu.nimble.utility.persistence.JPARepositoryFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by suat on 01-Jun-19.
 */
@Component
public class VatMigrationUtility {
    private static final Logger logger = LoggerFactory.getLogger(VatMigrationUtility.class);

    static Map<String, Integer> defaultVats = new HashMap<>();
    static {
        defaultVats.put("Germany", 19);
        defaultVats.put("Italy", 22);
        defaultVats.put("Spain", 21);
        defaultVats.put("Sweden", 21);
    }

    public void createVatsForExistingPrdocuts() {
        GenericJPARepository repo = new JPARepositoryFactory().forCatalogueRepository(true);
        List<CatalogueLineType> lines = repo.getEntities(CatalogueLineType.class);
        for(CatalogueLineType line : lines) {
            if(line.getRequiredItemLocationQuantity().getApplicableTaxCategory() != null &&
                    line.getRequiredItemLocationQuantity().getApplicableTaxCategory().size() > 0) {
                continue;
            }

            String country = CountryUtil.getCountryNameByISOCode(line.getGoodsItem().getItem().getManufacturerParty().getPostalAddress().getCountry().getIdentificationCode().getValue());
            Integer vatRate = defaultVats.get(country);
            if(vatRate == null) {
                vatRate = 20;
            }

            TaxCategoryType taxCategory = new TaxCategoryType();
            line.getRequiredItemLocationQuantity().getApplicableTaxCategory().add(taxCategory);
            taxCategory.setPercent(new BigDecimal(vatRate));
            repo.updateEntity(line);
        }

        logger.info("VAT migration completed");
    }
}
