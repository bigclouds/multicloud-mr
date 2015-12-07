package eu.leads.processor.nqe.handlers;

import eu.leads.processor.common.StringConstants;
import eu.leads.processor.common.infinispan.AcceptAllFilter;
import eu.leads.processor.common.infinispan.InfinispanManager;
import eu.leads.processor.core.Action;
import eu.leads.processor.core.ActionHandler;
import eu.leads.processor.core.ActionStatus;
import eu.leads.processor.core.Tuple;
import eu.leads.processor.core.comp.LogProxy;
import eu.leads.processor.core.net.Node;
import org.infinispan.Cache;
import org.infinispan.commons.util.CloseableIterable;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;

import java.util.Map;

/**
 * Created by vagvaz on 8/6/14.
 */
public class GetResultsActionHandler implements ActionHandler {
    private final Node com;
    private final LogProxy log;
    private final InfinispanManager persistence;
    private final String id;
    private Cache<String,String> queriesCache;
    public GetResultsActionHandler(Node com, LogProxy log, InfinispanManager persistence,
                                      String id) {
        this.com = com;
        this.log = log;
        this.persistence = persistence;
        this.id = id;
        queriesCache = (Cache<String, String>) persistence.getPersisentCache(StringConstants.QUERIESCACHE);
    }

    @Override
    public Action process(Action action) {
        Action result = action;
      String queryId = action.getData().getString("queryId");
      log.info("GetResults ");
        try {
            queryId = action.getData().getString("queryId");
            Long min = Long.parseLong(action.getData().getString("min"));
            Long max = Long.parseLong(action.getData().getString("max"));
            JsonObject actionResult = new JsonObject();
            if (min < 0) {
                actionResult.putString("error", "");
                actionResult.putString("message", "negative minimum index parameter given");
            }
            String queryJson = queriesCache.get(queryId);
            if(queryJson == null || queryJson.equals(""))
            {
                actionResult.putString("error", "");
                actionResult
                    .putString("message", "query " + queryId + " has not been completed yet");
            } else {
                JsonObject queryStatus = new JsonObject(queryJson);

                String cacheName = queryStatus.getString("output");
                boolean isSorted = queryStatus.getBoolean("isSorted");
                JsonObject tuples = null;
                if(cacheName == null || cacheName.equals("")){
                    actionResult.putString("id", queryId);
                    actionResult.putString("min", String.valueOf("-1"));
                    actionResult.putString("max", "-1");
                    actionResult.putString("result", new JsonArray().toString());
                    actionResult.putString("size", "0");
                    result.setStatus(ActionStatus.FAILED.toString());
                    actionResult.putString("message","output was either null or the empty string");
                    result.setResult(actionResult);
                    return result;
                }
                if(max==-1 && min==-1){
                    log.info("Not getting any results just clean caches ");
                    actionResult.putString("result", new JsonArray().toString());
	                log.info("removing cache " + cacheName);
	                persistence.removePersistentCache(cacheName);
	                log.info("Cache Removed ");
                }else {
                    log.info("GetResults before batchGet ");
                    if (max < 0) {
                        tuples = batchGet(cacheName, isSorted, min);
                    } else {
                        tuples = batchGet(cacheName, isSorted, min);
                    }
                    actionResult.putString("result", tuples.getArray("result").toString());
                }

//                updateQueryReadStatus(queryId, queryStatus, min, max);
                actionResult.putString("id", queryId);
                actionResult.putString("min", String.valueOf(min));
                actionResult.putString("max", String.valueOf(max));
                actionResult.putString("size", String.valueOf(tuples.size()));

            }
            result.setResult(actionResult);
        } catch (Exception e) {
//            log.error("Problem when reading results action " + action.toString() );
          JsonObject actionResult = new JsonObject();
          actionResult.putString("id", queryId);
          actionResult.putString("min", String.valueOf("-1"));
          actionResult.putString("max", String.valueOf("-1"));
          actionResult.putString("result", new JsonArray().toString());
          actionResult.putString("size", String.valueOf("0"));
          result.setResult(actionResult);
          result.setStatus(ActionStatus.COMPLETED.toString());
          return result;
        }
        result.setStatus(ActionStatus.COMPLETED.toString());
        return result;
    }

   private JsonObject batchGet(String cacheName, boolean isSorted, Long min) {
      JsonObject result = new JsonObject();
      JsonArray listOfValues = new JsonArray();
      log.info("GetResults batch get only min");

      Cache cache = (Cache) persistence.getPersisentCache(cacheName);
        if(isSorted) {
          log.info("GetResults batchget sorted");
            String prefix = cache.getName() + ":";
//            long cacheSize = cache.size();
           long index = 0;
          Tuple value = (Tuple) cache.get(prefix+String.valueOf(index));
//           String value = (String) cache.get(prefix+String.valueOf(index));
            while(value != null){
               listOfValues.add(value.toString());
//               listOfValues.add(value);
               index++;
               value =  (Tuple) cache.get(prefix+String.valueOf(index));
            }
           log.info("End of reading values");
        }
        else{
          log.info("GetResults batchet with iterator");
           CloseableIterable<Map.Entry<String, Tuple>> iterable = null;
            try {
                 iterable =
                    cache.getAdvancedCache().filterEntries(new AcceptAllFilter());
              log.info("GetResults batchget before iterate over values");
                for (Map.Entry<String, Tuple> entry : iterable) {
                    listOfValues.add(entry.getValue().toString());
//                    listOfValues.add(entry.getValue());
                }
               iterable.close();
              log.info("GetResults after iteration");
            } catch (Exception e) {
               if(iterable !=null)
               iterable.close();
                log.error("Iterating over " + cacheName + " for batch resulted in Exception "
                              + e.getMessage() + "\n from  " + cacheName);
                result.putString("status", "failed");
                result.putArray("result", new JsonArray());
                return result;
            }
      }
       log.info("removing cache " + cacheName);
       persistence.removePersistentCache(cacheName);
       log.info("Cache Removed ");
      result.putString("status", "ok");
      result.putArray("result", listOfValues);
      return result;
   }

   private JsonObject batchGet(String cacheName, boolean isSorted, Long min, Long max) {
     log.info("GetResults batchget min max");
      return batchGet(cacheName,isSorted,min,max);
   }


   private void updateQueryReadStatus(String queryId, JsonObject queryStatus, Long min, Long max) {
        JsonObject readStatus = queryStatus.getObject("read");
        if (readStatus.getLong("min") > min) {
            readStatus.putNumber("min", min);
        }
        Long size = readStatus.getLong("size");
        if ((readStatus.getLong("max") < max) && (max < size)) {
            readStatus.putNumber("max", max);
        } else if (max < 0 || max > size) {
            readStatus.putNumber("max", max);
            if (readStatus.getLong("min") == 0)
                readStatus.putBoolean("readFully", true);
        }
        queryStatus.putObject("read", readStatus);
        queriesCache.put(queryId, queryStatus.toString());
    }
}
