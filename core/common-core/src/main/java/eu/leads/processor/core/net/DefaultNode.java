package eu.leads.processor.core.net;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.RemovalListener;
import com.google.common.cache.RemovalNotification;
import eu.leads.processor.core.ReplyHandler;
import eu.leads.processor.core.comp.LeadsMessageHandler;
import org.slf4j.Logger;
import org.vertx.java.core.AsyncResult;
import org.vertx.java.core.Handler;
import org.vertx.java.core.Vertx;
import org.vertx.java.core.eventbus.EventBus;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;

/**
 * Created by vagvaz on 7/8/14.
 */
public class DefaultNode implements Node, Handler<Long> {
  private EventBus bus;
  private JsonObject config;
  private Logger logger;
  private int retries = ComUtils.DEFAULT_RETRIES;
  private long timeout = ComUtils.DEFAULT_TIMEOUT;
  private static long currentId;
  private ConcurrentMap<Long, MessageWrapper> pending;
  private ConcurrentMap<Long, AckHandler> pendingHandlers;
  private CommunicationHandler comHandler;
  private LeadsMessageHandler failHandler;
  private Set<Long> requests;
  private Cache<String, Long> incomingMessages;
  private RemovalListener<String, Long> removalListener;
  private Vertx vertx;

  public DefaultNode() {
    config = new JsonObject();
    pending = new ConcurrentHashMap<>();
    pendingHandlers = new ConcurrentHashMap<>();
    requests = new HashSet<Long>();
    removalListener = new RemovalListener<String, Long>() {
      @Override public void onRemoval(RemovalNotification<String, Long> removalNotification) {
        logger.info(getId() + " REMOVING RECEIVED " + removalNotification.getKey());
      }
    };
    incomingMessages =
        CacheBuilder.newBuilder().expireAfterWrite((retries / 3) * timeout, TimeUnit.MILLISECONDS).build();
    logger = org.slf4j.LoggerFactory.getLogger(DefaultNode.class);

  }

  /**
   * Getter for property 'comHandler'.
   *
   * @return Value for property 'comHandler'.
   */
  public CommunicationHandler getComHandler() {
    return comHandler;
  }

  /**
   * Setter for property 'comHandler'.
   *
   * @param comHandler Value to set for property 'comHandler'.
   */
  public void setComHandler(CommunicationHandler comHandler) {
    this.comHandler = comHandler;
  }

  /**
   * Getter for property 'failHandler'.
   *
   * @return Value for property 'failHandler'.
   */
  public LeadsMessageHandler getFailHandler() {
    return failHandler;
  }

  /**
   * Setter for property 'failHandler'.
   *
   * @param failHandler Value to set for property 'failHandler'.
   */
  public void setFailHandler(LeadsMessageHandler failHandler) {
    this.failHandler = failHandler;
  }

  @Override public String getId() {
    return config.getString("id");
  }

  @Override public String getGroup() {
    return config.getString("group");
  }

  @Override public void sendTo(String nodeid, JsonObject message) {
    JsonObject leadsMessage =
        MessageUtils.createLeadsMessage(message, getId(), nodeid, ComUtils.P2P, getNextMessageId());
    sendMessageToDestination(nodeid, leadsMessage, null);
  }

  @Override public void sendRequestTo(String nodeid, JsonObject message, LeadsMessageHandler handler) {
    long messageId = this.getNextMessageId();
    String from = getId() + "-requests-" + messageId;
    subscribeForRequest(nodeid, handler, messageId, message, from);
    //        requests.add(messageId);
    //        JsonObject leadsMessage =
    //            MessageUtils.createLeadsMessage(message, from, nodeid, ComUtils.P2P,messageId);
    ////        AckHandler ack = new AckHandler(this, logger, messageId, null);
    //        pending.put(messageId, new MessageWrapper(messageId,leadsMessage,ComUtils.DEFAULT_RETRIES));
    ////        pendingHandlers.put(messageId, ack);
    ////        logger.error(getId() + " send to " + leadsMessage.getString(MessageUtils.TO) +"\nmessage " + leadsMessage.toString());
    //        bus.send(nodeid, leadsMessage);
  }



  @Override public void sendToGroup(String groupId, JsonObject message) {
    JsonObject leadsMessage =
        MessageUtils.createLeadsMessage(message, getId(), groupId, ComUtils.GROUP, getNextMessageId());
    //        logger.error(getId() + " send to " + leadsMessage.getString(MessageUtils.TO) +"\nmessage " + leadsMessage.toString());
    sendMessageToDestination(groupId, leadsMessage, null);
  }

  private void sendMessageToDestination(String destination, JsonObject leadsMessage, LeadsMessageHandler handler) {
    //        long messageId = this.getNextMessageId();
    long messageId = leadsMessage.getLong(MessageUtils.MSGID);
    //        AckHandler ack = new AckHandler(this, logger, messageId, handler);
    pending.put(messageId, new MessageWrapper(messageId, leadsMessage, ComUtils.DEFAULT_RETRIES));
    //        pendingHandlers.put(messageId, ack);
    //        bus.sendWithTimeout(destination, leadsMessage, timeout, ack);
    //        logger.error(getId() + " send to " + leadsMessage.getString(MessageUtils.TO) +"\nmessage " + leadsMessage.toString());
    bus.send(destination, leadsMessage);
  }


  @Override public void sendRequestToGroup(String groupId, JsonObject message, LeadsMessageHandler handler) {
    long messageId = this.getNextMessageId();
    String from = getId() + "-requests-" + messageId;
    subscribeForRequest(groupId, handler, messageId, message, from);
    //        requests.add(messageId);
    //        JsonObject leadsMessage =
    //            MessageUtils.createLeadsMessage(message, from, groupId, ComUtils.GROUP,messageId);
    ////--        AckHandler ack = new AckHandler(this, logger, messageId, null);
    //        pending.put(messageId, new MessageWrapper(messageId,leadsMessage,ComUtils.DEFAULT_RETRIES));
    ////--        pendingHandlers.put(messageId, ack);
    ////--        logger.error(getId() + " send to " + leadsMessage.getString(MessageUtils.TO) +"\nmessage " + leadsMessage.toString());
    //        bus.send(groupId, leadsMessage);

  }

  @Override public void sendToAllGroup(String groupId, JsonObject message) {
    long messageId = getNextMessageId();
    JsonObject leadsMessage = MessageUtils.createLeadsMessage(message, getId(), groupId, ComUtils.ALLGROUP, messageId);
    pending.put(messageId, new MessageWrapper(messageId, leadsMessage, ComUtils.DEFAULT_RETRIES));
    //        logger.error(getId() + " send to " + leadsMessage.getString(MessageUtils.TO) +"\nmessage " + leadsMessage.toString());
    bus.publish(groupId, leadsMessage);
  }

  @Override public void subscribe(final String groupId, final LeadsMessageHandler handler) {
    bus.registerHandler(groupId, comHandler, new Handler<AsyncResult<Void>>() {
      @Override public void handle(AsyncResult<Void> event) {
        if (event.succeeded()) {
          logger.info("subscribing to " + groupId + " succeded");
          config.getArray("groups").add(groupId);
          comHandler.register(groupId, handler);
        } else {
          logger.error("Fail to subscribe to " + groupId);
        }
      }
    });

  }

  @Override public void subscribe(final String groupId, final LeadsMessageHandler handler, final Callable callable) {

    bus.registerHandler(groupId, comHandler, new Handler<AsyncResult<Void>>() {
      @Override public void handle(AsyncResult<Void> event) {
        if (event.succeeded()) {
          logger.info("subscribing to " + groupId + " succeded");
          config.getArray("groups").add(groupId);
          comHandler.register(groupId, handler);
          try {
            callable.call();
          } catch (Exception e) {
            e.printStackTrace();
          }
        } else {
          logger.error("Fail to subscribe to " + groupId);
        }
      }
    });
  }

  private void subscribeForRequest(final String groupId, final LeadsMessageHandler handler, final long messageId,
      final JsonObject message, final String from) {
    bus.registerHandler(from, comHandler, new Handler<AsyncResult<Void>>() {
      @Override public void handle(AsyncResult<Void> event) {
        if (event.succeeded()) {
          logger.info("subscribing to " + from + " succeded");
          //               config.getArray("groups").add(groupId);
          requests.add(messageId);
          JsonObject leadsMessage = MessageUtils.createLeadsMessage(message, from, groupId, ComUtils.GROUP, messageId);
          //--        AckHandler ack = new AckHandler(this, logger, messageId, null);
          pending.put(messageId, new MessageWrapper(messageId, leadsMessage, ComUtils.DEFAULT_RETRIES));
          //--        pendingHandlers.put(messageId, ack);
          //--        logger.error(getId() + " send to " + leadsMessage.getString(MessageUtils.TO) +"\nmessage " + leadsMessage.toString());
          bus.send(groupId, leadsMessage);

          comHandler.registerRequest(from, handler);
        } else {
          logger.error("Fail to subscribe to " + groupId);
        }
      }
    });
  }

  @Override public void unsubscribe(final String groupId) {

    bus.unregisterHandler(groupId, comHandler, new Handler<AsyncResult<Void>>() {
      @Override public void handle(AsyncResult<Void> event) {
        if (event.succeeded()) {
          logger.info("unsubscribing to " + groupId + " succeded");
          //TODO remove from config the groupId.config.getArray("groups").(groupId);
          comHandler.unregister(groupId);
        } else {
          logger.error("Fail to unsubscribe to " + groupId);
        }
      }
    });
  }

  public Logger getLogger() {
    return logger;
  }

  @Override
  public void initialize(JsonObject config, LeadsMessageHandler defaultHandler, LeadsMessageHandler failHandler,
      final Vertx vertx) {
    this.config = config.copy();
    logger = org.slf4j.LoggerFactory.getLogger(this.getId());
    comHandler = new CommunicationHandler(defaultHandler, this);
    this.failHandler = failHandler;
    bus = vertx.eventBus();

    registerToEventBusAddresses(this.config);
    this.vertx = vertx;

    this.vertx.setPeriodic(timeout, this);
  }

  @Override public void initialize(String id, String group, Set<String> groups, LeadsMessageHandler defaultHandler,
      LeadsMessageHandler failHandler, Vertx vertx) {
    JsonObject conf = new JsonObject();
    conf.putString("id", id);
    conf.putString("group", group);
    JsonArray array = new JsonArray();
    if (groups != null) {
      for (String g : groups) {
        array.add(g);
      }
    }
    conf.putArray("groups", array);
    //System.out.println("id: "+ id + "Groups: "+array.toString());
    initialize(conf, defaultHandler, failHandler, vertx);
  }

  private void registerToEventBusAddresses(JsonObject config) {
    bus.registerHandler(getId(), comHandler);
    bus.registerHandler(getGroup(), comHandler);
    Iterator<Object> it = this.config.getArray("groups").iterator();
    //System.out.println("registerToEventBusAddresses " +this.config.getArray("groups") );
    while (it.hasNext()) {
      String id = (String) it.next();
      //System.out.println("Subscribed " +id +" " + comHandler.toString() );
      bus.registerHandler((String) id, comHandler);
    }
  }

  @Override public JsonObject getConfig() {
    return config;
  }

  @Override public void setEventBus(EventBus bus) {
    this.bus = bus;
  }

  @Override public int getRetries() {
    return retries;
  }

  @Override public void setRetries(int retries) {
    this.retries = retries;
  }

  @Override public long getTimeout() {
    return timeout;
  }

  @Override public void setTimeout(long timeout) {
    this.timeout = timeout;
  }

  @Override public void retry(Long messageId, AckHandler handler) {
    //        JsonObject msg = pending.get(messageId);

    MessageWrapper wrapper = pending.get(messageId);
    JsonObject msg = wrapper.getMessage();
    logger.error(getId() + " Retrying... " + messageId + " to " + msg.getString(MessageUtils.TO));
    if (msg.getString(MessageUtils.COMTYPE).equals(ComUtils.P2P)) {
      //resend message through event bus to the nodeid
      //            bus.sendWithTimeout(msg.getString(MessageUtils.TO), msg, timeout, handler);
      if (msg == null || msg.getString(MessageUtils.TO) == null) {
        logger.error(
            "PROBLEWITH MESSAGE RETRYING msg " + (msg == null) + " to " + (msg.getString(MessageUtils.TO) == null));
      } else {
        try {
          bus.send(msg.getString(MessageUtils.TO), msg);
        } catch (Exception e) {
          e.printStackTrace();
          logger.error(e.getClass().toString() + " msg: " + (msg == null) + " bus " + (bus == null));
          logger.error(e.getClass().toString() + " TO " + msg.getString(MessageUtils.TO));
        }
      }
    } else if (msg.getString(MessageUtils.COMTYPE).equals(ComUtils.GROUP)) {
      //resend message through event bus to the groupId, it is essentially the same as
      //sending to one node since the event bus address is just an id.
      //            bus.sendWithTimeout(msg.getString(MessageUtils.TO), msg, timeout, handler);
      if (msg == null || msg.getString(MessageUtils.TO) == null) {
        logger.error(
            "PROBLEWITH MESSAGE RETRYING msg " + (msg == null) + " to " + (msg.getString(MessageUtils.TO) == null));
      } else {
        try {
          bus.send(msg.getString(MessageUtils.TO), msg);
        } catch (Exception e) {
          e.printStackTrace();
          logger.error(e.getClass().toString() + " msg: " + (msg == null) + " bus " + (bus == null));
          logger.error(e.getClass().toString() + " TO " + msg.getString(MessageUtils.TO));
        }
      }
    } else {
      if (msg == null || msg.getString(MessageUtils.TO) == null) {
        logger.error(
            "PROBLEWITH MESSAGE RETRYING msg " + (msg == null) + " to " + (msg.getString(MessageUtils.TO) == null));
      } else {
        try {
          bus.send(msg.getString(MessageUtils.TO), msg);
        } catch (Exception e) {
          e.printStackTrace();
          logger.error(e.getClass().toString() + " msg: " + (msg == null) + " bus " + (bus == null));
          logger.error(e.getClass().toString() + " TO " + msg.getString(MessageUtils.TO));
        }
      }
    }
  }

  @Override public void succeed(Long messageId) {
    try {
      //If succeded remove message and ackHandler
      MessageWrapper wrapper = pending.remove(messageId);
      //        JsonObject msg = pending.remove(messageId);
      //            logger.error(getId() + " Try to succeed " + messageId + " currentId " + currentId);
      if (wrapper == null) {
        //                logger.error(getId() + " Message " + messageId + " Was ALREADY Succeeded");
        return;
      }
      JsonObject msg = wrapper.getMessage();
      //        AckHandler handler = pendingHandlers.remove(messageId);
      //        handler = null;
      //            logger.info("Succeded Message: " + msg.toString());
      msg = null;
      wrapper = null;
    } catch (Exception e) {
      logger.error(getId() + " Exception in succeed " + messageId);
      e.printStackTrace();
    }
  }

  @Override public void fail(Long messageId) {
    //Remove message andhandler from pending handlers
    //        JsonObject msg = pending.remove(messageId);
    MessageWrapper wrapper = pending.remove(messageId);
    JsonObject msg = wrapper.getMessage();
    AckHandler handler = pendingHandlers.remove(messageId);
    handler = null;
    if (!getId().endsWith(".log")) {
      logger.error(getId() + " Failed Message: " + messageId + " to " + msg.getString(MessageUtils.TO));
    }
    if (requests.remove(messageId)) {
      comHandler.unregisterRequest(getId() + "-request-" + messageId);
    }
    if (failHandler != null) {
      failHandler.handle(msg);
    }
    msg = null;
    wrapper = null;
  }

  @Override public long getNextMessageId() {
    currentId = (currentId + 1) % Long.MAX_VALUE;
    return currentId;
  }

  @Override public void sendWithEventBus(String groupId, JsonObject message) {
    //        long messageId = getNextMessageId();
    //        JsonObject leadsMessage =
    //            MessageUtils.createLeadsMessage(message, getId(), groupId, ComUtils.GROUP,messageId);
    bus.send(groupId, message);
  }

  @Override public void sendWithEventBusReply(String groupId, JsonObject message, ReplyHandler replyHandler) {
    //        JsonObject leadsMessage =
    //            MessageUtils.createLeadsMessage(message, getId(), groupId, ComUtils.GROUP,getNextMessageId());
    bus.send(groupId, message, replyHandler);
  }

  @Override public void unsubscribeFromAll() {

  }

  @Override public void ack(JsonObject incoming) {
    //        logger.error(getId() + " ack " + incoming.getLong(MessageUtils.MSGID) + " from " + incoming.getString(MessageUtils.FROM));
    receive(incoming);
    JsonObject ackMessage = MessageUtils.createAckMessage(incoming);
    bus.send(ackMessage.getString(MessageUtils.TO), ackMessage);
  }

  @Override public boolean checkIfDelivered(JsonObject message) {
    String from = message.getString(MessageUtils.FROM);
    Long messageId = message.getNumber(MessageUtils.MSGID).longValue();
    Long longMessage = incomingMessages.getIfPresent(from + ":" + messageId);
    if (longMessage == null) {
      //            logger.error(getId() + " Not Delivered " + from + " "+ messageId);
    } else {
      logger.error(getId() + " Already Delivered " + from + " " + messageId);
    }
    return longMessage != null;
  }

  @Override public void receive(JsonObject message) {
    String from = message.getString(MessageUtils.FROM);
    Long messageId = message.getNumber(MessageUtils.MSGID).longValue();
    incomingMessages.put(from + ":" + messageId, messageId);
  }


  @Override public void handle(Long event) {
    Iterator<Map.Entry<Long, MessageWrapper>> entryIterator = pending.entrySet().iterator();
    while (entryIterator.hasNext()) {
      Map.Entry<Long, MessageWrapper> entry = entryIterator.next();
      int retries = entry.getValue().getRetries();
      retries--;
      if (retries < 0 || getId().endsWith(".log")) {
        fail(entry.getKey());
        return;
      }
      entry.getValue().setRetries(retries);
      retry(entry.getKey(), null);
    }
  }
}
