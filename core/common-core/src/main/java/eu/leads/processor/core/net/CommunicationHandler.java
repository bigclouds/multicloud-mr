package eu.leads.processor.core.net;

import eu.leads.processor.core.comp.LeadsMessageHandler;
import org.slf4j.Logger;
import org.vertx.java.core.Handler;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.json.JsonObject;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Created by vagvaz on 7/8/14.
 */
public class CommunicationHandler implements Handler<Message> {
  private Map<String, LeadsMessageHandler> handlers;
  private Node owner;
  private Set<String> requests;
  private Logger logger;

  public CommunicationHandler(LeadsMessageHandler defaultHandler, Node owner) {
    this.owner = owner;
    handlers = new HashMap<String, LeadsMessageHandler>();
    handlers.put("default", defaultHandler);
    requests = new HashSet<String>();
    logger = ((DefaultNode) owner).getLogger();
  }

  @Override public void handle(Message message) {

    JsonObject incoming = (JsonObject) message.body();
    String from = incoming.getString(MessageUtils.FROM);
    String to = incoming.getString(MessageUtils.TO);
    String type = incoming.getString(MessageUtils.COMTYPE);
    //        JsonObject object = new JsonObject();
    //        message.reply(MessageUtils.createAckMessage(object, to, from));
    if (incoming.getString(MessageUtils.MSGTYPE).equals("ack")) {
      //            logger.info(owner.getId() + " Received ack from " + from + " for " + incoming.getLong(MessageUtils.MSGID));
      owner.succeed(incoming.getLong(MessageUtils.MSGID));
      return;
    } else if (incoming.getString(MessageUtils.MSGTYPE).equals("msg")) {
      //            logger.info(owner.getId() + " Received Message from " + from + " with id " + incoming.getLong(MessageUtils.MSGID));
      boolean alreadyDelivered = owner.checkIfDelivered(incoming);
      owner.ack(incoming);
      if (alreadyDelivered)
        return;
    }



    LeadsMessageHandler handler = handlers.get(to);
    if (requests.remove(to)) {
      owner.unsubscribe(to);
    }
    if (handler == null)
      handler = handlers.get("default");
    handler.handle((JsonObject) message.body());
  }

  public void registerRequest(String groupId, LeadsMessageHandler handler) {
    requests.add(groupId);
    register(groupId, handler);
  }

  public void register(String groupId, LeadsMessageHandler handler) {
    LeadsMessageHandler oldHandler = handlers.remove(groupId);
    if (oldHandler != null)
      oldHandler = null;
    handlers.put(groupId, handler);
  }

  public void unregister(String groupId) {
    handlers.remove(groupId);
  }

  public LeadsMessageHandler getDefaultHandler() {
    return getHandler("default");
  }

  public LeadsMessageHandler getHandler(String groupId) {
    LeadsMessageHandler result = handlers.get(groupId);
    if (result == null)
      result = getDefaultHandler();
    return result;
  }

  public void unregisterRequest(String request) {
    if (requests.remove(request)) {
      owner.unsubscribe(request);
    }
  }
}
