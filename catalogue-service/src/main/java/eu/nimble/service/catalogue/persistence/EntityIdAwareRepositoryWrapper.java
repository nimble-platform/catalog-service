package eu.nimble.service.catalogue.persistence;

import eu.nimble.utility.Configuration;
import eu.nimble.utility.JsonSerializationUtility;
import eu.nimble.utility.persistence.GenericJPARepository;
import eu.nimble.utility.persistence.resource.ResourceValidationUtil;
import org.apache.poi.ss.formula.functions.T;
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
public class EntityIdAwareRepositoryWrapper<T> implements GenericJPARepository, JpaRepository<T, Long> {
    private GenericJPARepository genericJPARepository;
    private JpaRepository<T, Long> jpaRepository;
    private String partyId;
    private String userId;
    private String repositoryName;

    public EntityIdAwareRepositoryWrapper(GenericJPARepository genericJPARepository) {
        this(genericJPARepository, null, null, Configuration.Standard.UBL.toString());
    }

    public EntityIdAwareRepositoryWrapper(GenericJPARepository genericJPARepository, String partyId) {
        this(genericJPARepository, partyId, null, Configuration.Standard.UBL.toString());
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
        this(jpaRepository, null, null, Configuration.Standard.UBL.toString());
    }

    public EntityIdAwareRepositoryWrapper(JpaRepository jpaRepository, String partyId) {
        this(jpaRepository, partyId, null, Configuration.Standard.UBL.toString());
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
    public List<T> findAll() {
        return jpaRepository.findAll();
    }

    @Override
    public List<T> findAll(Sort sort) {
        return jpaRepository.findAll(sort);
    }

    @Override
    public List<T> findAll(Iterable<Long> longs) {
        return jpaRepository.findAll(longs);
    }

    @Override
    public <S extends T> List<S> save(Iterable<S> entities) {
        throw new RuntimeException("This method has not been implemented");
    }

    @Override
    public void flush() {
        jpaRepository.flush();
    }

    @Override
    public <S extends T> S saveAndFlush(S entity) {
        throw new RuntimeException("This method has not been implemented");
    }

    @Override
    public void deleteInBatch(Iterable<T> entities) {
        throw new RuntimeException("This method has not been implemented");
    }

    @Override
    public void deleteAllInBatch() {
        throw new RuntimeException("This method has not been implemented");
    }

    @Override
    public T getOne(Long aLong) {
        return jpaRepository.getOne(aLong);
    }

    @Override
    public <S extends T> List<S> findAll(Example<S> example) {
        return jpaRepository.findAll(example);
    }

    @Override
    public <S extends T> List<S> findAll(Example<S> example, Sort sort) {
        return jpaRepository.findAll(example, sort);
    }

    @Override
    public Page<T> findAll(Pageable pageable) {
        return jpaRepository.findAll(pageable);
    }

    @Override
    public <S extends T> S save(S entity) {
        checkHjidAssociation(entity);
        ResourceValidationUtil.removeHjidsForObject(entity, repositoryName);
        entity = jpaRepository.save(entity);
        ResourceValidationUtil.insertHjidsForObject(entity, partyId, repositoryName);
        return entity;
    }

    @Override
    public T findOne(Long aLong) {
        return jpaRepository.findOne(aLong);
    }

    @Override
    public boolean exists(Long aLong) {
        return jpaRepository.exists(aLong);
    }

    @Override
    public long count() {
        return jpaRepository.count();
    }

    @Override
    public void delete(Long aLong) {
        T entity = findOne(aLong);
        delete(entity);
    }

    @Override
    public void delete(T entity) {
        checkHjidAssociation(entity);
        jpaRepository.delete(entity);
        ResourceValidationUtil.removeHjidsForObject(entity, repositoryName);
    }

    @Override
    public void delete(Iterable<? extends T> entities) {
        throw new RuntimeException("This method has not been implemented");
    }

    @Override
    public void deleteAll() {
        throw new RuntimeException("This method has not been implemented");
    }

    @Override
    public <S extends T> S findOne(Example<S> example) {
        return jpaRepository.findOne(example);
    }

    @Override
    public <S extends T> Page<S> findAll(Example<S> example, Pageable pageable) {
        return jpaRepository.findAll(example, pageable);
    }

    @Override
    public <S extends T> long count(Example<S> example) {
        return jpaRepository.count(example);
    }

    @Override
    public <S extends T> boolean exists(Example<S> example) {
        return jpaRepository.exists(example);
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
