package eu.nimble.service.catalogue.cache;

import eu.nimble.service.model.ubl.catalogue.CatalogueType;
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
    private Cache catalogCache;

    private final String xmlConfigurationFile = "/ehcache.xml";

    @PostConstruct
    private void initCacheManager(){
        URL url = getClass().getResource(xmlConfigurationFile);
        XmlConfiguration xmlConfiguration = new XmlConfiguration(url);
        cacheManager = CacheManagerBuilder.newCacheManager(xmlConfiguration);
        // initialize cache manager
        cacheManager.init();
        // set catalog cache
        catalogCache = cacheManager.getCache("catalog",Object.class, Object.class);
    }

    @PreDestroy
    private void closeCacheManager(){
        cacheManager.close();
    }

    // method for Category cache
    public Cache<Object,Object> getCategoryCache() {
        return cacheManager.getCache("category",Object.class, Object.class);
    }

    // methods for Catalog cache
    public Object getCatalog(String uuid) {
        if(catalogCache.containsKey(uuid)){
            return catalogCache.get(uuid);
        }
        return null;
    }

    public void putCatalog(CatalogueType catalog) {
        if(catalog != null){
            catalogCache.put(catalog.getUUID(),catalog);
        }
    }

    public void removeCatalog(String uuid) {
        if(catalogCache.containsKey(uuid)){
            catalogCache.remove(uuid);
        }
    }
}
