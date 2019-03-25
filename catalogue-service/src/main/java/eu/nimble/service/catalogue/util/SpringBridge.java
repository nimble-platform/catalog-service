package eu.nimble.service.catalogue.util;

import eu.nimble.common.rest.identity.IdentityClientTyped;
import eu.nimble.common.rest.trust.TrustClient;
import eu.nimble.service.catalogue.category.IndexCategoryService;
import eu.nimble.service.catalogue.config.CatalogueServiceConfig;
import eu.nimble.service.catalogue.index.PartyIndexClient;
import eu.nimble.service.catalogue.persistence.util.LockPool;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

/**
 * Created by suat on 24-Jul-18.
 */
@Component
public class SpringBridge implements ApplicationContextAware {

    private static ApplicationContext applicationContext;

    public static SpringBridge getInstance() {
        return applicationContext.getBean(SpringBridge.class);
    }

    @Autowired
    private CatalogueServiceConfig catalogueServiceConfig;
    @Autowired
    private IdentityClientTyped identityClientTyped;
    @Autowired
    private TrustClient trustClient;
    @Autowired
    private IndexCategoryService indexCategoryService;
    @Autowired
    private PartyIndexClient partyIndexClient;
    @Autowired
    private LockPool lockPool;

    @Override
    public void setApplicationContext(ApplicationContext applicationContext)
            throws BeansException {
        this.applicationContext = applicationContext;
    }

    public CatalogueServiceConfig getCatalogueServiceConfig() {
        return catalogueServiceConfig;
    }

    public IdentityClientTyped getIdentityClientTyped() {
        return identityClientTyped;
    }

    public TrustClient getTrustClient() {
        return trustClient;
    }

    public IndexCategoryService getIndexCategoryService() {
        return indexCategoryService;
    }

    public PartyIndexClient getPartyIndexClient() {
        return partyIndexClient;
    }

    public LockPool getLockPool() {
        return lockPool;
    }
}