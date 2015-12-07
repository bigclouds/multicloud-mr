package eu.leads.processor.core.comp;

import eu.leads.processor.core.net.Node;
import org.vertx.java.core.json.JsonObject;

/**
 * Created by vagvaz on 7/13/14.
 */
public class LogProxy {
  JsonObject logMessage = new JsonObject();
  Node bus;
  String logId;

  public LogProxy() {
  }


  public LogProxy(String log, Node com) {
    logId = log;
    bus = com;
  }

  public void info(String message) {
    //        logMessage.putString("type", "info");
    //        logMessage.putString("message", message);
    //        bus.sendToAllGroup(logId, logMessage);
  }

  public void warn(String message) {
    //        logMessage.putString("type", "warn");
    //        logMessage.putString("message", message);
    //        bus.sendToAllGroup(logId, logMessage);
  }

  public void error(String message) {
    //        logMessage.putString("type", "error");
    //        logMessage.putString("message", message);
    //        bus.sendToAllGroup(logId, logMessage);
  }

  public void debug(String message) {
    //        logMessage.putString("type", "debug");
    //        logMessage.putString("message", message);
    //        bus.sendToAllGroup(logId, logMessage);
  }

  public void fatal(String message) {
    //        logMessage.putString("type", "fatal");
    //        logMessage.putString("message", message);
    //        bus.sendToAllGroup(logId, logMessage);
  }
}
