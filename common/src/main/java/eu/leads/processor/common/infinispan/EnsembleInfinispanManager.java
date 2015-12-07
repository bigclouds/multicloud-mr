package eu.leads.processor.common.infinispan;

import eu.leads.processor.conf.LQPConfiguration;
import org.infinispan.Cache;
import org.infinispan.client.hotrod.RemoteCache;
import org.infinispan.client.hotrod.RemoteCacheManager;
import org.infinispan.client.hotrod.configuration.ConfigurationBuilder;
import org.infinispan.configuration.cache.Configuration;
import org.infinispan.ensemble.EnsembleCacheManager;
import org.infinispan.ensemble.cache.EnsembleCache;
import org.infinispan.manager.EmbeddedCacheManager;
import org.infinispan.remoting.transport.Address;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentMap;

/**
 * Created by vagvaz on 2/8/15.
 */
public class EnsembleInfinispanManager implements InfinispanManager {
  private Logger log = LoggerFactory.getLogger(this.getClass());
  private EnsembleCacheManager manager;
  private RemoteCacheManager rmanager;
  private String configurationFile;
  private Configuration defaultConfig = null;
  private String connectionStrings;

  @Override public String getUniquePath() {
    return null;
  }

  @Override public void setConfigurationFile(String configurationFile) {
    this.configurationFile  = configurationFile;
  }

  @Override public void startManager(String configurationFile) {

    //manager = new EnsembleCacheManager()
    //    manager = new EnsembleCacheManager(connectionStrings);
    manager = new EnsembleCacheManager(configurationFile);
    rmanager = createRemoteCacheManager();
    RemoteCache clustered = null;//rmanager.getCache("clustered");
    boolean started = false;
    while(!started) {
      try {
        //        manager = new EnsembleCacheManager(configurationFile);
        //        EnsembleCache clustered = manager.getCache("clustered");
        rmanager = null;
        clustered = null;
        rmanager = createRemoteCacheManager();
        clustered = rmanager.getCache("clustered");
        clustered.put("1", "1");
        clustered.remove("1");
        started = true;
      } catch (Exception e) {
        started = false;
        System.out.println("Not started yet");
        try {
          Thread.sleep(2500);
        } catch (InterruptedException e1) {
          //          e1.printStackTrace();

        }
      }
    }
    //    manager = new EnsembleCacheManager(configurationFile);
    rmanager = null;
    rmanager = createRemoteCacheManager();
  }
  private  RemoteCacheManager createRemoteCacheManager() {
    ConfigurationBuilder builder = new ConfigurationBuilder();
    builder.addServer().host(LQPConfiguration.getConf().getString("node.ip")).port(11222);
    return new RemoteCacheManager(builder.build());
  }

  @Override public EmbeddedCacheManager getCacheManager() {
    return null;
  }

  @Override public void stopManager() {
    //    manager.stop();
  }

  @Override public ConcurrentMap getPersisentCache(String name) {
    return rmanager.getCache(name);
  }

  @Override public ConcurrentMap getInMemoryCache(String name, int inMemSize) {
    return null;
  }

  @Override public ConcurrentMap getPersisentCache(String name, Configuration configuration) {
    return rmanager.getCache(name);
  }

  @Override
  public ConcurrentMap getIndexedPersistentCache(String name) {
    return null;
  }

  @Override
  public ConcurrentMap getIndexedPersistentCache(String name, Configuration configuration) {
    return null;
  }

  @Override public void removePersistentCache(String name) {

  }

  @Override public void addListener(Object listener, Cache cache) {

  }

  @Override public void addListener(Object listener, String name) {

  }

  @Override public void removeListener(Object listener, Cache cache) {

  }

  @Override public void removeListener(Object listener, String cacheNane) {

  }

  @Override public List<Address> getMembers() {
    return new ArrayList<>();
  }

  @Override public Address getMemberName() {
    return  null;
  }

  @Override public boolean isStarted() {
    return true;
  }

  @Override public Cache getLocalCache(String s) {
    return null;
  }
}
