package eu.leads.processor;

import org.infinispan.client.hotrod.RemoteCache;
import org.infinispan.client.hotrod.RemoteCacheManager;
import org.infinispan.client.hotrod.configuration.ConfigurationBuilder;
import org.infinispan.ensemble.EnsembleCacheManager;
import org.infinispan.ensemble.cache.EnsembleCache;

import java.util.*;

/**
 * Created by vagvaz on 12/4/15.
 */
public class ResultSetIterator implements Iterator {

  List<RemoteCacheManager> remoteCacheManagers;
  List<RemoteCache> remoteCaches;
  Iterator<RemoteCache> iterator;
  Iterator currentIterator;
  RemoteCache currentCache;
  String cacheName;
  public ResultSetIterator(Collection<String> values, String id) {
    remoteCacheManagers = new ArrayList<>();
    remoteCaches = new ArrayList<>();
    for(String value : values){
      if(value.endsWith(":11222")){
        remoteCacheManagers.add(createRemoteCacheManager(value.substring(0,value.lastIndexOf(":"))));
      } else{
        remoteCacheManagers.add(createRemoteCacheManager(value));
      }
    }
    this.cacheName = id;

    for(RemoteCacheManager manager : remoteCacheManagers){
      remoteCaches.add(manager.getCache(this.cacheName));
    }
    iterator = remoteCaches.iterator();
    if(iterator.hasNext()){
      currentCache = iterator.next();
      currentIterator = currentCache.keySet().iterator();
    }
  }
  private  RemoteCacheManager createRemoteCacheManager(String host) {
   return createRemoteCacheManager(host,11222);
  }

  private  RemoteCacheManager createRemoteCacheManager(String host,int port) {
    ConfigurationBuilder builder = new ConfigurationBuilder();
    builder.addServer().host(host).port(11222);
    return new RemoteCacheManager(builder.build());
  }

  @Override public boolean hasNext() {
    if(currentIterator == null){
      return false;
    }
    if(currentIterator.hasNext()){
      return true;
    }
    while(iterator.hasNext() && !currentIterator.hasNext()) {
      if (iterator.hasNext()) {
        currentCache = iterator.next();
        currentIterator = currentCache.keySet().iterator();
        if(currentIterator.hasNext()){
          return true;
        }
      }
    }

    return false;
  }

  @Override public Map.Entry<Object,Object> next() {
    Object key = currentIterator.next();
    Object value = currentCache.get(key);
    return new AbstractMap.SimpleEntry(key,value);
  }

  @Override public void remove() {
    throw new UnsupportedOperationException("Remote is not implemented for the ResultSet Operator");
  }
}
