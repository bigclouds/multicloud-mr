package eu.leads.processor.core.net;

import eu.leads.processor.core.ReplyHandler;
import eu.leads.processor.core.comp.LeadsMessageHandler;
import org.vertx.java.core.Vertx;
import org.vertx.java.core.eventbus.EventBus;
import org.vertx.java.core.json.JsonObject;

import java.util.Set;
import java.util.concurrent.Callable;

/**
 * Created by vagvaz on 7/8/14.
 */
public interface Node {
  public void sendTo(String nodeid, JsonObject message);

  public void sendRequestTo(String nodeid, JsonObject message, LeadsMessageHandler handler);

  public void sendToGroup(String groupId, JsonObject message);

  public void sendRequestToGroup(String groupId, JsonObject message, LeadsMessageHandler handler);

  public void sendToAllGroup(String groupId, JsonObject message);

  public void subscribe(String groupId, LeadsMessageHandler handler);

  void subscribe(String groupId, LeadsMessageHandler handler, Callable callable);

  public void unsubscribe(String groupId);

  public void initialize(JsonObject config, LeadsMessageHandler defaultHandler, LeadsMessageHandler failHandler,
      Vertx vertx);

  public void initialize(String id, String group, Set<String> groups, LeadsMessageHandler defaultHandler,
      LeadsMessageHandler failHandler, Vertx vertx);

  public JsonObject getConfig();

  public void setEventBus(EventBus bus);

  public int getRetries();

  public void setRetries(int retries);

  public long getTimeout();

  public void setTimeout(long timeout);

  public void retry(Long messageId, AckHandler handler);

  public void fail(Long messageId);

  public void succeed(Long messageId);

  public long getNextMessageId();

  public String getId();

  public String getGroup();

  void sendWithEventBus(String workQueue, JsonObject msg);

  void sendWithEventBusReply(String id, JsonObject putAction, ReplyHandler replyHandler);

  void unsubscribeFromAll();

  void ack(JsonObject incoming);

  boolean checkIfDelivered(JsonObject message);

  void receive(JsonObject message);
}
