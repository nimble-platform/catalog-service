package eu.nimble.service.catalogue.persistence;

import eu.nimble.service.model.ubl.commonaggregatecomponents.CatalogueLineType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

/**
 * Created by suat on 20-Nov-18.
 */
@Transactional(transactionManager = "ubldbTransactionManager")
public interface CatalogueLineRepository extends JpaRepository<CatalogueLineType, Long> {

}