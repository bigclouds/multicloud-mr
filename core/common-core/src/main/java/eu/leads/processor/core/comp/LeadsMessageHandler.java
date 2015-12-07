package eu.leads.processor.core.comp;

import org.vertx.java.core.json.JsonObject;

/**
 * Created by vagvaz on 7/8/14.
 */
public interface LeadsMessageHandler {

  public void handle(JsonObject message);


}
