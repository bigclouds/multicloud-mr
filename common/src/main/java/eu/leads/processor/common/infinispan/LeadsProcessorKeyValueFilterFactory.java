package eu.leads.processor.common.infinispan;

import org.infinispan.manager.EmbeddedCacheManager;
import org.infinispan.notifications.cachelistener.filter.CacheEventFilter;
import org.infinispan.notifications.cachelistener.filter.CacheEventFilterFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vertx.java.core.json.JsonObject;

//import org.infinispan.filter.KeyValueFilterFactory;
//import org.infinispan.filter.NamedFactory;

/**
 * Created by vagvaz on 9/29/14.
 */


public class LeadsProcessorKeyValueFilterFactory implements CacheEventFilterFactory {
//public class LeadsProcessorKeyValueFilterFactory {
  Logger log = LoggerFactory.getLogger(LeadsProcessorKeyValueFilterFactory.class);
  private final EmbeddedCacheManager manager;

    public LeadsProcessorKeyValueFilterFactory(EmbeddedCacheManager cacheManager){
        this.manager = cacheManager;
    }

  @Override public <K, V> CacheEventFilter<K, V> getFilter(Object[] params) {
    log.error("GetFilter called");
    if(params.length != 1){
      throw new IllegalArgumentException();
    }
    JsonObject conf = new JsonObject((String)params[0]);
    log.error("Json conf parametrized");
//    PluginRunnerFilter result = new PluginRunnerFilter(manager,conf.toString());
    log.error("Filter init");
    return null;
  }
}
