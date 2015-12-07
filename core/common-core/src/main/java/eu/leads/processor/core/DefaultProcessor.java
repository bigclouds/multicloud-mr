package eu.leads.processor.core;

import org.vertx.java.core.Handler;
import org.vertx.java.core.eventbus.EventBus;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.platform.Verticle;

/**
 * Created by vagvaz on 7/21/14.
 */
public class DefaultProcessor extends Verticle implements Handler<Message<JsonObject>> {

  JsonObject config;
  EventBus bus;
  String completed;

  public DefaultProcessor() {
  }

  @Override public void start() {
    super.start();
    bus.registerHandler(config.getString("workQueueAddress"), this);
  }

  @Override public void handle(Message<JsonObject> jsonObjectMessage) {
    System.out.println("Working on " + jsonObjectMessage.toString());

    try {
      Thread.sleep(1000); //emulating serious work
    } catch (InterruptedException e) {
      e.printStackTrace();
    }
    JsonObject reply = new JsonObject();
    reply.putString("action", jsonObjectMessage.toString());
    reply.putString("result", new JsonObject().putString("completed", "ok").toString());
    bus.publish(completed, reply);

  }
}
