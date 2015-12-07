package eu.leads.processor.common.infinispan;

import org.infinispan.Cache;
import org.infinispan.configuration.cache.Configuration;
import org.infinispan.manager.EmbeddedCacheManager;
import org.infinispan.remoting.transport.Address;

import java.util.List;
import java.util.concurrent.ConcurrentMap;

/**
 * Created by vagvaz on 5/18/14. This interface is a wrapper for different operations to an
 * Infinispan cluster Depending on the type of Infinispan cluster different implementaitons might be
 * needed.
 */

public interface InfinispanManager {
    String getUniquePath();

    /**
     * Setter for property 'configurationFile'.
     *
     * @param configurationFile Value to set for property 'configurationFile'.
     */ //Manager related methods
    public void setConfigurationFile(String configurationFile);

    public void startManager(String configurationFile);

    /**
     * Getter for property 'cacheManager'.
     *
     * @return Value for property 'cacheManager'.
     */
    public EmbeddedCacheManager getCacheManager();

    public void stopManager();

    //Cache realted methods
    public ConcurrentMap getPersisentCache(String name);
    public ConcurrentMap getInMemoryCache(String name, int inMemSize);
    public ConcurrentMap getPersisentCache(String name, Configuration configuration);

    public ConcurrentMap getIndexedPersistentCache(String name);
    public ConcurrentMap getIndexedPersistentCache(String name,Configuration configuration);
    //    public ConcurrentMap getVersionedCache(String name);
    //    public ConcurrentMap getVersionedCache(String name, Configuration configuration);
    //    public ConcurrentMap getWeaklyConsistentCache(String name);
    //    public ConcurrentMap getWeaklyConsistentCache(String name, Configuration configuration);
    //    public ConcurrentMap getSWMRCache(String name);
    //    public ConcurrentMap getSWMRCache(String name,Configuration configuration);
    //    public ConcurrentMap getAtomicCache(String name);
    //    public ConcurrentMap getAtomicCache(String name, Configuration configuration);
    public void removePersistentCache(String name);

    //    public void removeVersionedCache(String name);
    //    public void removeVersionedCache(Cache cache);
    //    public void removeWeaklyConsisentCache(String name);
    //    public void removeWeaklyConsistentCache(Cache cache);
    //    public void removeSWMRCache(String name);
    //    public void removeSWMRCache(Cache cache);
    //    public void removeAtomicCache(String name);
    //    public void removeAtomicCache(Cache cache);


    // Listener related methods
    public void addListener(Object listener, Cache cache);

    public void addListener(Object listener, String name);

    //  public void addListener(Object listener, String name, KeyFilter filter);
    //
    //  public void addListener(Object listener, String name, KeyValueFilter filter, Converter converter);
    //
    //  public void addListener(Object listener, Cache cache, KeyFilter filter);
    //
    //  public void addListener(Object listener, Cache cache, KeyValueFilter filter, Converter converter);
    //   public void addRemoteListener(Object listener, Cache cache, KeyValueFilter filter, Converter converter);

    public void removeListener(Object listener, Cache cache);

    public void removeListener(Object listener, String cacheNane);


    /**
     * Getter for property 'members'.
     *
     * @return Value for property 'members'.
     */ //Cluster and Jgroups related methods
    public List<Address> getMembers();

    /**
     * Getter for property 'memberName'.
     *
     * @return Value for property 'memberName'.
     */
    public Address getMemberName();

    /**
     * Getter for property 'started'.
     *
     * @return Value for property 'started'.
     */
    boolean isStarted();

    Cache getLocalCache(String s);
}
