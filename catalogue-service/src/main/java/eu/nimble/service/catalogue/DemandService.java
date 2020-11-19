package eu.nimble.service.catalogue;

import eu.nimble.service.model.ubl.commonaggregatecomponents.DemandType;
import eu.nimble.service.model.ubl.commonbasiccomponents.BinaryObjectType;
import eu.nimble.utility.UblUtil;
import eu.nimble.utility.persistence.resource.EntityIdAwareRepositoryWrapper;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Service providing CRUD functionalities for DemandTypes
 */
@Component
public class DemandService {
    public DemandType saveDemand(DemandType demand) {
        EntityIdAwareRepositoryWrapper repositoryWrapper = new EntityIdAwareRepositoryWrapper();
        repositoryWrapper.persistEntity(demand, UblUtil.getBinaryObjectsFrom(demand));
        return demand;
    }

    public DemandType updateDemand(DemandType existingDemand, DemandType updatedDemand) {
        EntityIdAwareRepositoryWrapper repositoryWrapper = new EntityIdAwareRepositoryWrapper();

        // get binary objects to be deleted
        List<String> binaryContentUrisToDelete = UblUtil.getBinaryObjectsFrom(existingDemand).stream().map(BinaryObjectType::getUri).collect(Collectors.toList());

        // replace each field
        UblUtil.copy(updatedDemand, existingDemand);

        // it is important to get the binary objects to be persisted after copying as the binary content service updates the given binary objects
        List<BinaryObjectType> binaryObjectsToPersist = UblUtil.getBinaryObjectsFrom(existingDemand);

        // update the demand
        repositoryWrapper.updateEntity(existingDemand, binaryObjectsToPersist, binaryContentUrisToDelete);
        return existingDemand;
    }

    public void deleteDemand(DemandType demand) {
        // get uris of binary objects in the demand
        List<String> binaryObjectUris = UblUtil.getBinaryObjectsFrom(demand).stream().map(BinaryObjectType::getUri).collect(Collectors.toList());

        // delete the entity
        EntityIdAwareRepositoryWrapper repositoryWrapper = new EntityIdAwareRepositoryWrapper();
        repositoryWrapper.deleteEntityByHjid(DemandType.class, demand.getHjid(), binaryObjectUris);
    }
}
