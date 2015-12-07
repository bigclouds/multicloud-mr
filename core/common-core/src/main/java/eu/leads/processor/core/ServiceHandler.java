package eu.leads.processor.core;

import eu.leads.processor.core.comp.LeadsMessageHandler;
import eu.leads.processor.core.comp.LeadsService;
import eu.leads.processor.core.comp.LogProxy;
import eu.leads.processor.core.comp.ServiceStatus;
import eu.leads.processor.core.net.MessageTypeConstants;
import eu.leads.processor.core.net.Node;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vertx.java.core.json.JsonObject;

/**
 * Created by vagvaz on 7/28/14.
 */
public class ServiceHandler implements LeadsMessageHandler {
  LeadsService owner;
  LogProxy logg;
  Logger log;
  // Calls to this proxy should be avoided in ManageVertice cause will freeze the vertx event loop
  Node com;

  public ServiceHandler(LeadsService owner, Node com, LogProxy logProxy) {
    this.owner = owner;
    this.logg = logProxy;
    log = LoggerFactory.getLogger(ServiceHandler.class);
    this.com = com;
  }

  @Override public void handle(JsonObject message) {
    if (message.getString("type").equals(MessageTypeConstants.SERVICE_CMD)) {
      String cmd = message.getString("command");
      switch (ServiceCommand.valueOf(cmd)) {
        case INITIALIZE:
          if (!((owner.getStatus().equals(ServiceStatus.INITIALIZING)) || (owner.getStatus()
              .equals(ServiceStatus.INITIALIZED)))) {
            owner.initialize(message.getObject("conf"));
          }
          break;
        case START:
          if (!(owner.getStatus().equals(ServiceStatus.RUNNING)))
            owner.startService();
          break;
        case STOP:
          if (!((owner.getStatus().equals(ServiceStatus.STOPPING)) || (owner.getStatus()
              .equals(ServiceStatus.STOPPED))))
            owner.stopService();
          break;
        case GETSTATUS:
          JsonObject statusMessage = new JsonObject();
          statusMessage.putString("id", owner.getServiceId());
          statusMessage.getString("status", owner.getStatus().toString());
          statusMessage.putString("to", message.getString("from"));
          com.sendTo(message.getString("from"), statusMessage);
          break;
        case EXIT:
          owner.exitService();
          break;
        default:
          log.warn("unknown command received by " + owner.getServiceId() + "\n" + message.toString());
      }
    } else {
      log.warn("unknown type of message received by " + owner.getServiceId() + "\n" + message.toString());
    }
  }
}
