package eu.nimble.service.catalogue.persistence.util;

import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Created by suat on 25-Mar-19.
 */
@Component
public class LockPool {
    private Map<String, ReadWriteLock> catalogueUuidLocks = new HashMap<>();
    private Map<String, ReadWriteLock> partyIdLocks = new HashMap<>();

    public synchronized ReadWriteLock getLockForCatalogue(String catalogueUuid) {
        ReadWriteLock lock = catalogueUuidLocks.get(catalogueUuid);
        if(lock == null) {
            lock = new ReentrantReadWriteLock();
            catalogueUuidLocks.put(catalogueUuid, lock);
        }
        return lock;
    }

    public synchronized ReadWriteLock getLockForParty(String partyId) {
        ReadWriteLock lock = partyIdLocks.get(partyId);
        if(lock == null) {
            lock = new ReentrantReadWriteLock();
            partyIdLocks.put(partyId, lock);
        }
        return lock;
    }
}
