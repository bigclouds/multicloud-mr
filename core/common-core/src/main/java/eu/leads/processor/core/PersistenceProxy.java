package eu.leads.processor.core;

import eu.leads.processor.core.net.DefaultNode;
import eu.leads.processor.core.net.Node;
import org.vertx.java.core.Vertx;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;

/**
 * Created by vagvaz on 7/16/14.
 */
public class PersistenceProxy extends Thread {

  private String id;
  private JsonObject putAction;
  private JsonObject getAction;
  private JsonObject readAction;
  private JsonObject storeAction;
  private JsonObject batchGetAction;
  private JsonObject containsAction;
  private volatile Object mutex = new Object();
  private JsonObject action = null;
  private Node bus;
  private ReplyHandler replyHandler;
  private volatile boolean toContinue = true;
  private String cmpId;
  private Vertx vertx;

  public PersistenceProxy(String id, Node com) {
    this.id = id;
    bus = new DefaultNode();
    cmpId = com.getId();
    replyHandler = new ReplyHandler();
    putAction = new JsonObject();
    putAction.putString("action", "put");
    putAction.putString("cache", "");
    putAction.putString("key", "");
    getAction = new JsonObject();
    getAction.putString("action", "get");
    getAction.putString("cache", "");
    getAction.putString("key", "");
    readAction = new JsonObject();
    readAction.putString("action", "read");
    readAction.putString("key", "");
    storeAction = new JsonObject();
    storeAction.putString("action", "store");
    storeAction.putString("key", "");
    batchGetAction = new JsonObject();
    batchGetAction.putString("action", "batchGet");
    batchGetAction.putString("cache", "");
    batchGetAction.putNumber("min", Long.MAX_VALUE);
    batchGetAction.putNumber("max", Long.MIN_VALUE);
    containsAction = new JsonObject();
    containsAction.putString("action", "contains");
    containsAction.putString("cache", "");
    containsAction.putString("key", "");

  }

  public PersistenceProxy(String id, Node com, Vertx vertx) {
    this.id = id;
    bus = new DefaultNode();
    cmpId = com.getId();
    replyHandler = new ReplyHandler();
    putAction = new JsonObject();
    putAction.putString("action", "put");
    putAction.putString("cache", "");
    putAction.putString("key", "");
    getAction = new JsonObject();
    getAction.putString("action", "get");
    getAction.putString("cache", "");
    getAction.putString("key", "");
    readAction = new JsonObject();
    readAction.putString("action", "read");
    readAction.putString("key", "");
    storeAction = new JsonObject();
    storeAction.putString("action", "store");
    storeAction.putString("key", "");
    batchGetAction = new JsonObject();
    batchGetAction.putString("action", "batchGet");
    batchGetAction.putString("cache", "");
    batchGetAction.putNumber("min", Long.MAX_VALUE);
    batchGetAction.putNumber("max", Long.MIN_VALUE);
    containsAction = new JsonObject();
    containsAction.putString("action", "contains");
    containsAction.putString("cache", "");
    containsAction.putString("key", "");
    this.vertx = vertx;
    bus.initialize(cmpId + ".proxy", "persistgroup", null, replyHandler, null, vertx);
  }

  public JsonObject get(String cacheName, String key) {
    JsonObject result = null;
    synchronized (mutex) {

      getAction.putString("cache", cacheName);
      getAction.putString("key", key);
      action = getAction;
      System.out.println("pnget");
      mutex.notify();
    }
    //         bus.sendWithEventBusReply(id, getAction, replyHandler);

    result = replyHandler.waitForMessage();
    result.putObject("result", new JsonObject(result.getString("result")));

    return result;
  }

  public JsonObject read(String key) {
    JsonObject result = null;
    synchronized (mutex) {
      readAction.putString("key", key);
      action = readAction;
      System.out.println("pnread");
      mutex.notify();
    }

    result = replyHandler.waitForMessage();
    result.putObject("result", new JsonObject(result.getString("result")));

    return result;
  }

  public boolean put(String cacheName, String key, JsonObject value) {
    boolean result = false;
    synchronized (mutex) {
      putAction.putString("cache", cacheName);
      putAction.putString("key", key);
      putAction.putString("value", value.toString());
      action = putAction;
      System.out.println("pnput");
      mutex.notifyAll();
    }
    //      bus.sendWithEventBusReply(id, putAction, replyHandler);
    result = replyHandler.waitForStatus();

    return result;

  }

  public boolean store(String key, JsonObject value) {
    synchronized (mutex) {
      storeAction.putString("key", key);
      storeAction.putString("value", value.toString());
      action = storeAction;
      System.out.println("pn");
      mutex.notify();
    }
    boolean result = replyHandler.waitForStatus();

    return result;
  }


  public JsonArray batchGet(String cacheName, Long min) {
    return batchGet(cacheName, min, -1L);
  }

  public JsonArray batchGet(String cacheName, Long min, Long max) {
    synchronized (mutex) {
      batchGetAction.putString("cache", cacheName);
      if (min >= 0) {
        batchGetAction.putNumber("min", min);
      } else {
        batchGetAction.putNumber("min", Long.MIN_VALUE);
      }
      if (max >= 0) {
        batchGetAction.putNumber("max", max);
      } else {
        batchGetAction.putNumber("max", Long.MAX_VALUE);
      }
      action = batchGetAction;
      mutex.notify();
    }
    JsonArray result = replyHandler.waitForBatch();
    return result;

  }

  public boolean contains(String cacheName, String key) {
    synchronized (mutex) {
      containsAction.putString("cache", cacheName);
      containsAction.putString("key", key);
      action = containsAction;
      System.out.println("pn");
      mutex.notify();
    }
    boolean result = replyHandler.waitForContains();
    action = null;

    return result;

  }

  @Override public void run() {

    while (toContinue) {
      synchronized (mutex) {
        System.out.println("Thread " + Thread.currentThread().toString() + " mutex " + mutex.toString());
        if (action != null) {
          System.out.println("pa");
          bus.sendRequestTo(id, action, replyHandler);
          action = null;
        } else {
          try {
            System.out.println("pw");
            mutex.wait();
          } catch (InterruptedException e) {
            e.printStackTrace();
          }
        }
      }
    }
    bus.unsubscribe("persistgroup");
    bus.unsubscribe(cmpId + ".proxy");
  }

  public void cleanup() {
    synchronized (mutex) {
      toContinue = false;
      this.stop();
      mutex.notify();
    }
  }
}
