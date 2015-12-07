package eu.leads.processor.core.comp;

import org.vertx.java.core.json.JsonObject;

/**
 * Created by vagvaz on 7/13/14.
 */
public class DefaultControlHandler implements LeadsMessageHandler {
  Component owner;

  public DefaultControlHandler(Component owner) {
    this.owner = owner;
  }

  @Override public void handle(JsonObject command) {
    if (command.getString("commandType").equals("startUp")) {
      owner.startUp();
    } else if (command.getString("commandType").equals("shutdown")) {
      owner.shutdown();
    } else if (command.getString("commandType").equals("kill")) {
      if (command.getString("mode").equals("testing")) {
        owner.kill();
      }
    } else {
      //      owner.getLogUtil().error("Received Unexpected Command " +  command.toString() );
    }


  }
}
