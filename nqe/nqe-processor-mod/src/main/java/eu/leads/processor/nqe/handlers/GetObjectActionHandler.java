package eu.leads.processor.nqe.handlers;

import eu.leads.processor.common.infinispan.InfinispanManager;
import eu.leads.processor.core.Action;
import eu.leads.processor.core.ActionHandler;
import eu.leads.processor.core.comp.LogProxy;
import eu.leads.processor.core.net.Node;
import org.infinispan.Cache;
import org.vertx.java.core.json.JsonObject;

/**
 * Created by vagvaz on 8/6/14.
 */
public class GetObjectActionHandler implements ActionHandler {

    Node com;
    LogProxy log;
    InfinispanManager persistence;
    String id;

    public GetObjectActionHandler(Node com, LogProxy log, InfinispanManager persistence, String id) {
        this.com = com;
        this.log = log;
        this.persistence = persistence;
        this.id = id;
    }

    @Override
    public Action process(Action action) {
        Action result = new Action(action);
        try {
            String cacheName = action.getData().getString("table");
            String key = action.getData().getString("key");
            Cache<String,String> cache = (Cache) persistence.getPersisentCache(cacheName);
            System.err.println(cache.getName() + " sz " + cache.size());
            String objectJson = cache.get(key);
            JsonObject actionResult = new JsonObject();
            if (!(objectJson == null || objectJson.equals(""))) {
                //               com.sendTo(from, result.getObject("result"));
                result.setResult(new JsonObject(objectJson));
            } else {
                actionResult.putString("error", "");
                result.setResult(actionResult);
            }
        } catch (Exception e) {
//            e.printStackTrace();
           JsonObject object = new JsonObject();
           object.putString("error",e.getMessage());
           result.setResult(object);
        }
        return result;
    }
}
