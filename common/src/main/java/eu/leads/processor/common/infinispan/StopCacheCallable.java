package eu.leads.processor.common.infinispan;

/**
 * Created by vagvaz on 5/23/14.
 */

import eu.leads.processor.common.LeadsListener;
import org.hibernate.annotations.common.util.impl.LoggerFactory;
import org.infinispan.Cache;
import org.infinispan.distexec.DistributedCallable;
import org.slf4j.Logger;

import java.io.Serializable;
import java.util.Set;

public class StopCacheCallable<K, V> implements DistributedCallable<K, V, Void>, Serializable {
    private static final long serialVersionUID = 8331682008912636781L;
    private final String cacheName;
    private transient Cache<K, V> cache;
    public StopCacheCallable(String cacheName) {
        this.cacheName = cacheName;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Void call() throws Exception {
        Logger log = org.slf4j.LoggerFactory.getLogger(StopCacheCallable.class);
        log.error("Try to clear stop " + cacheName + " from " +cache.getCacheManager().getAddress().toString());
        EnsembleCacheUtils.removeCache(cacheName);
        if(cache.getCacheManager().cacheExists(cacheName))
        {
            try{
                for(Object l : cache.getListeners()){
                    if(l instanceof LeadsListener){
                        ((LeadsListener)l).close();
                        cache.removeListener(l);
                    }
                }
            }catch (Exception e){
                e.printStackTrace();
            }

            if(cache.getCacheManager().cacheExists(cache.getName()+".compressed")){
                Cache compressed = cache.getCacheManager().getCache(cache.getName()+".compressed");
                for(Object l : compressed.getListeners()){
                    if(l instanceof LeadsListener){
                        ((LeadsListener)l).close();
                        compressed.removeListener(l);
                        l = null;
                    }
                }
            }
        }
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setEnvironment(Cache<K, V> cache, Set<K> inputKeys) {
        this.cache = cache.getCacheManager().getCache(cacheName);
    }

}
