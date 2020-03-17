package eu.nimble.service.catalogue.cache;

import org.ehcache.Cache;
import org.ehcache.CacheManager;
import org.ehcache.config.builders.CacheManagerBuilder;
import org.ehcache.xml.XmlConfiguration;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.net.URL;

@Component
public class CacheHelper {
    private CacheManager cacheManager;

    private final String xmlConfigurationFile = "/ehcache.xml";

    @PostConstruct
    private void initCacheManager(){
        URL url = getClass().getResource(xmlConfigurationFile);
        XmlConfiguration xmlConfiguration = new XmlConfiguration(url);
        cacheManager = CacheManagerBuilder.newCacheManager(xmlConfiguration);
        // initialize cache manager
        cacheManager.init();
    }

    @PreDestroy
    private void closeCacheManager(){
        cacheManager.close();
    }

    public Cache<Object,Object> getCategoryCache() {
        return cacheManager.getCache("category",Object.class, Object.class);
    }
}
