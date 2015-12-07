package eu.leads.processor.imanager;

import eu.leads.processor.common.infinispan.InfinispanManager;
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
public class RemoveListenerActionHandler implements ActionHandler {
  private Logger log;
  private InfinispanManager imanager;
  JsonObject global;

  public RemoveListenerActionHandler(Node com, LogProxy logg, InfinispanManager persistence, String id,
      JsonObject global) {
    log = LoggerFactory.getLogger(RemoveListenerActionHandler.class);
    imanager = persistence;
    this.global = global;
  }

  @Override public Action process(Action action) {
    Action result = action;
    JsonObject actionResult = new JsonObject();
    actionResult.putString("status", "SUCCESS");
    actionResult.putString("message", "");
    JsonObject data = action.getData();
    String cache = data.getString("cache");
    String listener = data.getString("listener");
    try {
      imanager.removeListener(listener, cache);
    } catch (Exception e) {
      actionResult.putString("status", "FAIL");
      actionResult.putString("error", e.getMessage() == null ? "null" : e.getMessage().toString());
    }
    result.setResult(actionResult);
    return result;
  }
}
