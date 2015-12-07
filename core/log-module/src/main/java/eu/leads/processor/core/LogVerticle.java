package eu.leads.processor.core;

import eu.leads.processor.core.comp.LeadsMessageHandler;
import eu.leads.processor.core.net.DefaultNode;
import eu.leads.processor.core.net.Node;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.core.logging.Logger;
import org.vertx.java.platform.Verticle;

/**
 * Created by vagvaz on 7/13/14.
 */
public class LogVerticle extends Verticle {

  Logger logger;
  Node bus;
  String id;
  String componentId;

  public LogVerticle() {
  }

  @Override public void start() {
    try {
      super.start();
      logger = container.logger();
      bus = new DefaultNode();
      id = container.config().getString("id");
      componentId = id.substring(0, id.indexOf('.'));
      LeadsMessageHandler logHandler = new LeadsMessageHandler() {

        @Override public void handle(JsonObject msg) {
          String type = msg.getString("type");
          String message = msg.getString("message");
          if (id != null) //return allways for not spamming the output!
            return;
          if (type.equals("info")) {
            logger.info(message);
            msg.putString("component", componentId);
            bus.sendToAllGroup("eu.leads.processor.log.info", msg);
          } else if (type.equals("warn")) {
            logger.warn(message);
            msg.putString("component", componentId);
            bus.sendToAllGroup("eu.leads.processor.log.warn", msg);
          } else if (type.equals("error")) {
            logger.error(message);
            msg.putString("component", componentId);
            bus.sendToAllGroup("eu.leads.processor.log.error", msg);
          } else if (type.equals("fatal")) {
            logger.fatal(message);
            msg.putString("component", componentId);
            bus.sendToAllGroup("eu.leads.processor.log.fatal", msg);
          } else {
            logger.debug(message);
            msg.putString("component", componentId);
            bus.sendToAllGroup("eu.leads.processor.log.debug", msg);
          }
        }
      };
      bus.initialize(id, id + ".group", null, logHandler, logHandler, this.getVertx());
    } catch (Exception e) {
      e.printStackTrace();
    }

  }
}
