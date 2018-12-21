package eu.nimble.service.catalogue.persistence;

import eu.nimble.service.model.ubl.catalogue.CatalogueType;
import eu.nimble.service.model.ubl.commonaggregatecomponents.CatalogueLineType;
import eu.nimble.service.model.ubl.commonaggregatecomponents.PartyType;
import eu.nimble.service.model.ubl.commonaggregatecomponents.UnitType;
import eu.nimble.service.model.ubl.commonaggregatecomponents.UnitTypeUnitCodeItem;
import eu.nimble.utility.persistence.GenericJPARepository;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

/**
 * Created by suat on 20-Nov-18.
 */
public interface CatalogueRepository extends JpaRepository<CatalogueType, Long>, GenericJPARepository {

    @Query(value = "SELECT catalogue FROM CatalogueType catalogue WHERE catalogue.UUID = :uuid")
    CatalogueType getCatalogueByUuid(@Param("uuid") String uuid);

    @Query(value =
            "SELECT catalogue FROM CatalogueType as catalogue "
            + " JOIN catalogue.providerParty as catalogue_provider_party"
            + " WHERE catalogue.ID = :catalogueId"
            + " AND catalogue_provider_party.ID = :partyId")
    CatalogueType getCatalogueForParty(@Param("catalogueId") String catalogueId, @Param("partyId") String partyId);

    @Query(value = "SELECT COUNT(c) FROM CatalogueType c WHERE c.ID = :id and c.providerParty.ID = :partyId")
    Long checkCatalogueExistenceByID(@Param("id") String id, @Param("partyId") String partyId);

    @Query(value = "SELECT party FROM PartyType party WHERE party.ID = :id")
    PartyType getPartyByID(@Param("id") String id);

    @Query(value =
    "SELECT catalogue.UUID FROM CatalogueType as catalogue" +
            " JOIN catalogue.providerParty as catalogue_provider_party " +
            " WHERE catalogue_provider_party.ID = :partyId")
    List<String> getCatalogueIdsForParty(@Param("partyId") String partyId);

    @Query(value =
            "SELECT COUNT(cl) FROM CatalogueLineType as cl, CatalogueType as c "
                    + " JOIN c.catalogueLine as clj"
                    + " WHERE c.UUID = :catalogueId "
                    + " AND cl.ID = :lineId "
                    + " AND clj.ID = cl.ID ")
    Long checkCatalogueLineExistence(@Param("catalogueId") String catalogueId, @Param("lineId") String lineId);

    @Query(value =
            "SELECT COUNT(clj) FROM CatalogueType as c "
                    + " JOIN c.catalogueLine as clj"
                    + " WHERE c.UUID = :catalogueId"
                    + " AND clj.hjid <> :hjid "
                    + " AND clj.ID = :lineId ")
    Long checkCatalogueLineExistence(@Param("catalogueId") String catalogueId, @Param("lineId") String lineId, @Param("hjid") Long hjid);

    @Query(value =
            "SELECT cl FROM CatalogueLineType as cl, CatalogueType as c "
            + " JOIN c.catalogueLine as clj"
            + " WHERE c.UUID = :catalogueId "
            + " AND cl.ID = :lineId "
            + " AND clj.ID = cl.ID ")
    CatalogueLineType getCatalogueLine(@Param("catalogueId") String catalogueId, @Param("lineId") String lineId);


    /**
     * Units
     */

    @Query(value = "SELECT ut FROM UnitType ut WHERE ut.ID = 'NIMBLE_quantity'")
    List<UnitType> getUnitMarker();

    @Query(value = "SELECT ut FROM UnitType ut WHERE ut.ID = :listId")
    List<UnitType> getUnitsInList(@Param("listId") String listId);

    @Query(value = "SELECT ut FROM UnitType ut WHERE ut.ID <> 'NIMBLE_quantity'")
    List<UnitType> getAllUnits();

    @Query(value = "SELECT ut.unitCodeItems FROM UnitType ut WHERE ut.ID = :listId")
    List<UnitTypeUnitCodeItem> getUnitCodesInList(@Param("listId") String listId);

    @Query(value = "SELECT ut.ID FROM UnitType ut WHERE ut.ID <> 'NIMBLE_quantity'")
    List<String> getAllUnitListIds();

    @Query(value = "SELECT ut.hjid FROM UnitType ut WHERE ut.ID = :listId")
    Long getListUniqueId(@Param("listId") String listId);
}