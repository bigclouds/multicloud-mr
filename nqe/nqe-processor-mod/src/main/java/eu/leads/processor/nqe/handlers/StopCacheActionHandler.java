package eu.leads.processor.nqe.handlers;

import eu.leads.processor.common.LeadsListener;
import eu.leads.processor.common.infinispan.*;
import eu.leads.processor.core.Action;
import eu.leads.processor.core.ActionHandler;
import eu.leads.processor.core.comp.LogProxy;
import eu.leads.processor.core.net.Node;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vertx.java.core.json.JsonObject;

/**
 * Created by vagvaz on 9/25/15.
 */
public class StopCacheActionHandler implements ActionHandler {
  InfinispanManager imanager;
  Logger logger;
  JsonObject global;
  public StopCacheActionHandler(Node com, LogProxy log, InfinispanManager persistence, String id,JsonObject global) {
    logger = LoggerFactory.getLogger(StopCacheActionHandler.class);
    imanager = persistence;
    this.global = global;
  }

  @Override public Action process(Action action){
    Action result = action;
    JsonObject actionResult = new JsonObject();
    actionResult.putString("status","SUCCESS");
    actionResult.putString("message","");
    JsonObject data = action.getData();
    String cache = data.getString("cache");
    try{

      if(cache != null && !cache.isEmpty()) {
        logger.error("STOPPING CACHE FOR CQL " + cache);
        System.err.println("STOPPING CACHE FOR CQL " + cache);
        imanager.removePersistentCache(cache);
      }
    }catch(Exception e){
      actionResult.putString("status","FAIL");
      actionResult.putString("error",e.getMessage() == null ? "null" : e.getMessage().toString());
    }
    result.setResult(actionResult);
    return result;
  }
}
