package eu.leads.processor.core.comp;

import eu.leads.processor.core.net.MessageUtils;
import eu.leads.processor.core.net.Node;
import eu.leads.processor.imanager.IManagerConstants;
import org.vertx.java.core.json.JsonObject;

/**
 * Created by vagvaz on 7/28/14.
 */
public class LeadsComponentHandler implements LeadsMessageHandler {

  Component owner;
  LogProxy log;
  Node com;

  public LeadsComponentHandler(Component owner, Node com, LogProxy log) {
    this.owner = owner;
    this.com = com;
    this.log = log;
  }

  @Override public void handle(JsonObject message) {
    System.err.println(" Leads Components Handler " + message.toString());
    if (message.getString("type").equals("command")) {
      String cmd = message.getString("command");
      ComponentCommand command = ComponentCommand.valueOf(cmd);
      JsonObject conf = null;
      JsonObject reply = null;
      String replyTo = null;
      switch (command) {
        case SETUP:
          conf = message.getObject("conf");
          owner.setup(conf);
          break;
        case START:
          owner.startUp();
          break;
        case STOP:
          owner.shutdown();
          break;
        case KILL:
          owner.kill();
          break;
        case GETSTATUS:
          ComponentState state = owner.getState();
          reply = MessageUtils.createComponentStateMessage(state, owner.getId(), owner.getComponentType());
          replyTo = message.getString("from");
          com.sendTo(replyTo, reply);
          break;
        case GETMODE:
          ComponentMode mode = owner.getRunningMode();
          reply = MessageUtils.createComponentModeMessage(mode, owner.getId(), owner.getComponentType());
          replyTo = message.getString("from");
          com.sendTo(replyTo, reply);
          break;
        case SETMODE:
          String newmodeString = message.getString("mode");
          ComponentMode newmode = ComponentMode.valueOf(newmodeString);
          owner.setRunningMode(newmode);
          break;
        case CLEANUP:
          owner.cleanup();
          break;
        case RESET:
          conf = message.getObject("conf");
          owner.reset(conf);
          break;
        case UNKNOWN:
          log.error(
              owner.getComponentType() + ":" + owner.getId() + " received an unknown command \n" + message.toString());
          break;
      }
    } else {

      if (message.getString("type").equals("action")) {
        System.out.println(" Quit leads Component ");
        if (message.getString("label").equals(IManagerConstants.QUIT)) {

          System.out.println(" Shutdown " + owner.getId());
          owner.shutdown();
          owner.kill();
        }
      } else {
        log.error(owner.getComponentType() + ":" + owner.getId() + " received other\n" + message.toString());
        owner.kill();
      }
    }
  }
}
