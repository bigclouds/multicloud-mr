package eu.leads.processor.core.comp;

import org.vertx.java.core.eventbus.EventBus;
import org.vertx.java.core.json.JsonObject;

/**
 * Created by vagvaz on 7/13/14.
 */
public class DefaultActionHandler implements LeadsMessageHandler {
  private EventBus bus;
  private String workQueueAddress;
  private Component owner;

  public DefaultActionHandler(Component owner, EventBus bus, String workQueueAddress) {
    this.owner = owner;
    this.bus = bus;
    this.workQueueAddress = workQueueAddress;
  }

  @Override public void handle(JsonObject jsonObject) {
    //    owner.getLogUtil().info("Processing\n"+jsonObject.toString());
    bus.send(workQueueAddress, jsonObject);
  }
}
