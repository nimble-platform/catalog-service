package eu.nimble.service.catalogue.util;

import eu.nimble.common.rest.delegate.IDelegateClient;
import eu.nimble.common.rest.identity.IIdentityClientTyped;
import eu.nimble.common.rest.indexing.IIndexingServiceClient;
import eu.nimble.service.catalogue.cache.CacheHelper;
import eu.nimble.service.catalogue.category.IndexCategoryService;
import eu.nimble.service.catalogue.category.TaxonomyManager;
import eu.nimble.service.catalogue.config.CatalogueServiceConfig;
import eu.nimble.service.catalogue.index.IndexingClientController;
import eu.nimble.service.catalogue.persistence.util.LockPool;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.Locale;

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
    private IIdentityClientTyped iIdentityClientTyped;
    @Autowired
    private IIndexingServiceClient iIndexingServiceClient;
    @Autowired
    private IndexCategoryService indexCategoryService;
    @Autowired
    private IndexingClientController indexingClientController;
    @Autowired
    private LockPool lockPool;
    @Autowired
    private TaxonomyManager taxonomyManager;
    @Autowired
    private IDelegateClient delegateClient;
    @Autowired
    private CacheHelper cacheHelper;
    @Autowired
    private MessageSource messageSource;

    private String federationId = null;

    @Override
    public void setApplicationContext(ApplicationContext applicationContext)
            throws BeansException {
        this.applicationContext = applicationContext;
    }

    public CatalogueServiceConfig getCatalogueServiceConfig() {
        return catalogueServiceConfig;
    }

    public IIdentityClientTyped getiIdentityClientTyped() {
        return iIdentityClientTyped;
    }

    public IndexCategoryService getIndexCategoryService() {
        return indexCategoryService;
    }

    public LockPool getLockPool() {
        return lockPool;
    }

    public TaxonomyManager getTaxonomyManager() {
        return taxonomyManager;
    }

    public IIndexingServiceClient getiIndexingServiceClient() {
        return iIndexingServiceClient;
    }

    public IDelegateClient getDelegateClient() {
        return delegateClient;
    }

    public String getFederationId() {
        return getCatalogueServiceConfig().getFederationInstanceId();
    }

    public String getFederatedIndexPlatformName() {
        return indexingClientController.getFederatedIndexPlatformName();
    }

    public CacheHelper getCacheHelper() {
        return cacheHelper;
    }

    public String getMessage(String code,String languageId) {
        return messageSource.getMessage(code, Collections.emptyList().toArray(), Locale.forLanguageTag(languageId));
    }
}