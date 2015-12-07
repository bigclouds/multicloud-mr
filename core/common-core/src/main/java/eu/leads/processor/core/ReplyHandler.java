package eu.leads.processor.core;

import eu.leads.processor.core.comp.LeadsMessageHandler;
import org.vertx.java.core.Handler;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;

/**
 * Created by vagvaz on 7/16/14.
 */
public class ReplyHandler implements Handler<Message<JsonObject>>, LeadsMessageHandler {

  private JsonObject message = null;
  private volatile Object mutex = new Object();

  public JsonObject waitForMessage() {
    JsonObject result = null;
    synchronized (mutex) {

      while (message == null) {
        try {
          System.out.println("rw");
          mutex.wait();

        } catch (InterruptedException e) {
          e.printStackTrace();
        }
      }
      result = message;
      message = null;

      return result;
    }

  }

  public boolean waitForStatus() {
    boolean result = false;
    synchronized (mutex) {

      while (message == null) {
        try {
          System.out.println("rw");
          mutex.wait();

        } catch (InterruptedException e) {
          e.printStackTrace();
        }
      }
      result = message.getString("status").equals("ok");
      message = null;
      return result;
    }

  }

  //   @Override
  //   public void handle(JsonObject jsonObject) {
  //      message = jsonObject;
  //      mutex.notify();
  //   }

  @Override public void handle(Message<JsonObject> msg) {
    System.err.println("pre rn");
    synchronized (mutex) {
      message = msg.body();
      System.out.println("rn");
      mutex.notify();
    }
  }

  public JsonArray waitForBatch() {
    JsonObject result = null;
    synchronized (mutex) {

      while (message == null) {
        try {
          System.out.println("rw");
          mutex.wait();
        } catch (InterruptedException e) {
          e.printStackTrace();
        }
      }
      result = message;
      message = null;
      return result.getArray("result");
    }

  }

  public boolean waitForContains() {
    boolean result = true;
    synchronized (mutex) {

      while (message == null) {
        try {

          mutex.wait();
        } catch (InterruptedException e) {
          e.printStackTrace();
        }
      }
      result = message.getBoolean("result");
      message = null;
      return result;
    }

  }

  @Override public void handle(JsonObject msg) {
    System.err.println("aapre rn");
    synchronized (mutex) {
      message = msg;
      System.out.println("aarn");
      mutex.notify();
    }
  }
}
