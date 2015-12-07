package eu.leads.processor.nqe.handlers;

import eu.leads.processor.common.infinispan.BatchPutListener;
import eu.leads.processor.common.infinispan.InfinispanManager;
import eu.leads.processor.core.Action;
import eu.leads.processor.core.ActionHandler;
import eu.leads.processor.core.ActionStatus;
import eu.leads.processor.core.comp.LogProxy;
import eu.leads.processor.core.net.Node;
import eu.leads.processor.infinispan.LocalIndexListener;
import org.infinispan.Cache;
import org.vertx.java.core.json.JsonObject;

/**
 * Created by vagvaz on 8/6/14.
 */
public class PutObjectActionHandler implements ActionHandler {

   Node com;
   LogProxy log;
   InfinispanManager persistence;
   String id;

   public PutObjectActionHandler(Node com, LogProxy log, InfinispanManager persistence, String id) {
      this.com = com;
      this.log = log;
      this.persistence = persistence;
      this.id = id;
   }

   @Override
   public Action process(Action action) {
      Action result = action;
      JsonObject actionResult = new JsonObject();
      try {
         String cacheName = action.getData().getString("table");
         String key = action.getData().getString("key");
         JsonObject value = new JsonObject(action.getData().getString("object"));
         Cache<String, String> cache = (Cache<String, String>) persistence.getPersisentCache(cacheName);
         if(!key.equals("") && !value.equals("{}")) {
            cache.put(key, value.toString());
         }
         else{
            log.error("put object used for creating cache");
            if(value.containsField("listener")){
               if(!value.containsField("cqlListener")) {
                  String listeners = value.getString("listener");
                  if (listeners.contains("localIndexListener")) {

                     //                  Cache thecache = (Cache) persistence.getPersisentCache(cacheName);//, 1000);
                     Cache thecache = (Cache) persistence.getInMemoryCache(cacheName, 1000);
                     boolean toadd = true;
                     for (Object l : thecache.getListeners()) {
                        if (l instanceof LocalIndexListener) {
                           toadd = false;
                           break;
                        }
                     }
                     if (toadd) {
                        LocalIndexListener listener = new LocalIndexListener(persistence, cacheName);

                        persistence.addListener(listener, cacheName);
                     }
                     if (listeners.contains("batchputListener")) {
                        Cache compressedCache = (Cache) persistence.getInMemoryCache(cacheName + ".compressed", 4000);
                        //                     Cache compressedCache = (Cache) persistence.getPersisentCache(cacheName+".compressed");
                        BatchPutListener listener = new BatchPutListener(compressedCache.getName(), cacheName);
                        persistence.addListener(listener, compressedCache.getName());
                     }
                  } else if (listeners.contains("batchputListener")) {
                     //                  Cache compressedCache = (Cache) persistence.getPersisentCache(cacheName+".compressed");//,4000);
                     Cache compressedCache = (Cache) persistence.getInMemoryCache(cacheName + ".compressed", 4000);
                     BatchPutListener listener = new BatchPutListener(compressedCache.getName(), cacheName);
                     persistence.addListener(listener, compressedCache.getName());
                     Cache thecache = (Cache) persistence.getPersisentCache(cacheName);
                  }
               }else { //cql create cache
                  String listener = value.getString("cqlListener");
                  if(listener.equals("scan")){
                     //create scan listener
                  }
                  else if (listener.equals("topk-1")){

                  }
                  else if(listener.equals("topk-2")){

                  }else{

                  }
               }
            }
            else{
               Cache thecache = (Cache) persistence.getPersisentCache(cacheName);
            }
         }
         actionResult.putString("status", "SUCCESS");
      } catch (Exception e) {
         actionResult.putString("status", "FAIL");
         actionResult.putString("error", "");
         actionResult.putString("message",
                                       "Could not store object " + action.getData().toString());
         System.err.println(e.getMessage());
      }
      result.setResult(actionResult);
      result.setStatus(ActionStatus.COMPLETED.toString());
      return result;
   }
}
