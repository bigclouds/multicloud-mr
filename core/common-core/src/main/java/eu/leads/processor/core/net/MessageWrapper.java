package eu.leads.processor.core.net;

import org.vertx.java.core.json.JsonObject;

/**
 * Created by vagvaz on 11/13/14.
 */
public class MessageWrapper {
  private long messageId;
  private JsonObject message;
  private int retries;


  public MessageWrapper(long messageId, JsonObject message, int retries) {
    this.messageId = messageId;
    this.message = message;
    this.retries = retries;
  }

  public long getMessageId() {
    return messageId;
  }

  public void setMessageId(long messageId) {
    this.messageId = messageId;
  }

  public JsonObject getMessage() {
    return message;
  }

  public void setMessage(JsonObject message) {
    this.message = message;
  }

  public int getRetries() {
    return retries;
  }

  public void setRetries(int retries) {
    this.retries = retries;
  }
}
