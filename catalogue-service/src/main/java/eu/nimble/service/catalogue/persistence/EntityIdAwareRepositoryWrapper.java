package eu.nimble.service.catalogue.persistence;

import eu.nimble.utility.JsonSerializationUtility;
import eu.nimble.utility.persistence.GenericJPARepository;
import eu.nimble.utility.persistence.resource.ResourceValidationUtil;
import org.springframework.data.domain.Example;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;

import java.io.Serializable;
import java.util.List;

/**
 * <p>
 * This class is a wrapper on {@link GenericJPARepository} and {@link JpaRepository} interfaces so that resource-id
 * mappings can be managed from a single entry point considering the modifying operations (create, update and delete).
 * The class associates entity ids with party ids and user ids (if available) so that each party can only update entities
 * containing only the identifiers associated to it. Identifiers included in the {@link eu.nimble.service.model.ubl.commonaggregatecomponents.PartyType} and {@link eu.nimble.service.model.ubl.commonaggregatecomponents.QualifyingPartyType}
 * objects are excluded since these entities are managed in a singleton manner and can be reused in documents
 * (e.g. {@link eu.nimble.service.model.ubl.order.OrderType} or {@link eu.nimble.service.model.ubl.iteminformationrequest.ItemInformationRequestType}).
 * All other entities are assumed to be associated with distinct parties.
 * <p>
 * Such a wrapper is required concerning especially the UBLDB since there are modifying updates where complex database
 * entities are passed with several database identifiers. There might be data integrity issues when the identifiers
 * passed in the entities are somehow mixed, are not proper.
 * <p>
 * Created by suat on 17-Dec-18.
 */
public class EntityIdAwareRepositoryWrapper implements GenericJPARepository, JpaRepository{
    private GenericJPARepository genericJPARepository;
    private JpaRepository jpaRepository;
    private String partyId;
    private String userId;
    private String repositoryName;

    public EntityIdAwareRepositoryWrapper(GenericJPARepository genericJPARepository) {
        this(genericJPARepository, null, null, null);
    }

    public EntityIdAwareRepositoryWrapper(GenericJPARepository genericJPARepository, String partyId, String repositoryName) {
        this(genericJPARepository, partyId, null, repositoryName);
    }

    public EntityIdAwareRepositoryWrapper(GenericJPARepository genericJPARepository, String partyId, String userId, String repositoryName) {
        this.genericJPARepository = genericJPARepository;
        this.partyId = partyId;
        this.userId = userId;
        this.repositoryName = repositoryName;
    }

    public EntityIdAwareRepositoryWrapper(JpaRepository jpaRepository) {
        this(jpaRepository, null, null, null);
    }

    public EntityIdAwareRepositoryWrapper(JpaRepository jpaRepository, String partyId, String repositoryName) {
        this(jpaRepository, partyId, null, repositoryName);
    }

    public EntityIdAwareRepositoryWrapper(JpaRepository jpaRepository, String partyId, String userId, String repositoryName) {
        this.jpaRepository = jpaRepository;
        this.partyId = partyId;
        this.userId = userId;
        this.repositoryName = repositoryName;
    }

    @Override
    public <T> T getSingleEntityByHjid(Class<T> klass, long hjid) {
        return genericJPARepository.getSingleEntityByHjid(klass, hjid);
    }

    @Override
    public <T> T getSingleEntity(String query, String[] parameterNames, Object[] parameterValues) {
        return genericJPARepository.getSingleEntity(query, parameterNames, parameterValues);
    }

    @Override
    public <T> List<T> getEntities(String query) {
        return genericJPARepository.getEntities(query);
    }

    @Override
    public <T> List<T> getEntities(String query, String[] parameterNames, Object[] parameterValues) {
        return genericJPARepository.getEntities(query, parameterNames, parameterValues);
    }

    @Override
    public <T> List<T> getEntities(String query, String[] parameterNames, Object[] parameterValues, Integer limit, Integer offset) {
        return genericJPARepository.getEntities(query, parameterNames, parameterValues, limit, offset);
    }

    @Override
    public <T> T updateEntity(T entity) {
        checkHjidAssociation(entity);
        ResourceValidationUtil.removeHjidsForObject(entity, repositoryName);
        entity = genericJPARepository.updateEntity(entity);
        ResourceValidationUtil.insertHjidsForObject(entity, partyId, repositoryName);
        return entity;
    }

    @Override
    public <T> void deleteEntity(T entity) {
        checkHjidAssociation(entity);
        genericJPARepository.deleteEntity(entity);
        ResourceValidationUtil.removeHjidsForObject(entity, repositoryName);
    }

    @Override
    public <T> void deleteEntityByHjid(Class<T> klass, long hjid) {
        T entity = getSingleEntityByHjid(klass, hjid);
        checkHjidAssociation(entity);
        genericJPARepository.deleteEntity(entity);
        ResourceValidationUtil.removeHjidsForObject(entity, repositoryName);
    }

    @Override
    public <T> void persistEntity(T entity) {
        checkHjidExistence(entity);
        genericJPARepository.persistEntity(entity);
        ResourceValidationUtil.insertHjidsForObject(entity, partyId, repositoryName);
    }

    @Override
    public List findAll() {
        return jpaRepository.findAll();
    }

    @Override
    public List findAll(Sort sort) {
        throw new RuntimeException("This method has not been implemented");
    }

    @Override
    public List findAll(Iterable iterable) {
        throw new RuntimeException("This method has not been implemented");
    }

    @Override
    public List save(Iterable entities) {
        throw new RuntimeException("This method has not been implemented");
    }

    @Override
    public void flush() {
        throw new RuntimeException("This method has not been implemented");
    }

    @Override
    public Object saveAndFlush(Object entity) {
        throw new RuntimeException("This method has not been implemented");
    }

    @Override
    public void deleteInBatch(Iterable entities) {
        throw new RuntimeException("This method has not been implemented");
    }

    @Override
    public void deleteAllInBatch() {
        throw new RuntimeException("This method has not been implemented");
    }

    @Override
    public Object getOne(Serializable serializable) {
        throw new RuntimeException("This method has not been implemented");
    }

    @Override
    public List findAll(Example example) {
        throw new RuntimeException("This method has not been implemented");
    }

    @Override
    public List findAll(Example example, Sort sort) {
        throw new RuntimeException("This method has not been implemented");
    }

    @Override
    public Page findAll(Pageable pageable) {
        throw new RuntimeException("This method has not been implemented");
    }

    @Override
    public Object save(Object entity) {
        checkHjidAssociation(entity);
        ResourceValidationUtil.removeHjidsForObject(entity, repositoryName);
        entity = jpaRepository.save(entity);
        ResourceValidationUtil.insertHjidsForObject(entity, partyId, repositoryName);
        return entity;
    }

    @Override
    public Object findOne(Serializable serializable) {
        throw new RuntimeException("This method has not been implemented");
    }

    @Override
    public boolean exists(Serializable serializable) {
        throw new RuntimeException("This method has not been implemented");
    }

    @Override
    public long count() {
        throw new RuntimeException("This method has not been implemented");
    }

    @Override
    public void delete(Serializable serializable) {
        throw new RuntimeException("This method has not been implemented");
    }

    @Override
    public void delete(Object entity) {
        checkHjidAssociation(entity);
        genericJPARepository.deleteEntity(entity);
        ResourceValidationUtil.removeHjidsForObject(entity, repositoryName);
    }

    @Override
    public void delete(Iterable entities) {
        throw new RuntimeException("This method has not been implemented");
    }

    @Override
    public void deleteAll() {
        throw new RuntimeException("This method has not been implemented");
    }

    @Override
    public Object findOne(Example example) {
        throw new RuntimeException("This method has not been implemented");
    }

    @Override
    public Page findAll(Example example, Pageable pageable) {
        throw new RuntimeException("This method has not been implemented");
    }

    @Override
    public long count(Example example) {
        throw new RuntimeException("This method has not been implemented");
    }

    @Override
    public boolean exists(Example example) {
        throw new RuntimeException("This method has not been implemented");
    }

    public <T> void checkHjidAssociation(T entity) {
        boolean entityAssociationCheck = ResourceValidationUtil.hjidsBelongsToParty(entity, partyId, repositoryName);
        if(entityAssociationCheck == false) {
            String serializedEntity = JsonSerializationUtility.serializeEntitySilently(entity);
            throw new RepositoryException(String.format("There are entity ids (hjids) belonging to another company in the passed object: %s", serializedEntity));
        }
    }

    public <T> void checkHjidExistence(T entity) {
        boolean hjidsExist = ResourceValidationUtil.hjidsExit(entity);
        if(hjidsExist) {
            String serializedEntity = JsonSerializationUtility.serializeEntitySilently(entity);
            throw new RepositoryException(String.format("There are database ids (hjids) in the entity to be persisted. Make sure there is none. The entity: %s", serializedEntity));
        }
    }
}
