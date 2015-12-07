package eu.leads.processor.core.comp;

import eu.leads.processor.core.net.Node;
import org.vertx.java.core.json.JsonObject;

/**
 * Created by vagvaz on 7/13/14.
 */
public class DefaultFailHandler implements LeadsMessageHandler {

  public DefaultFailHandler(ComponentControlVerticle componentControlVerticle, Node com, LogProxy log) {
  }

  @Override public void handle(JsonObject jsonObject) {

  }
}
