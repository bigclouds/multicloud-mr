package eu.leads.processor.core.net;

import eu.leads.processor.core.comp.LeadsMessageHandler;
import org.vertx.java.core.AsyncResult;
import org.vertx.java.core.Handler;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.core.logging.Logger;

/**
 * Created by vagvaz on 7/8/14.
 */
public class AckHandler implements Handler<AsyncResult<Message<JsonObject>>> {

  Node owner;
  Logger logger;
  int retries;
  long msgId;
  LeadsMessageHandler handler;

  public AckHandler(Node owner, Logger logger, long msgId) {
    this.owner = owner;
    this.logger = logger;
    retries = owner.getRetries();
    this.msgId = msgId;
    handler = null;
  }

  public AckHandler(Node owner, Logger logger, long msgId, LeadsMessageHandler handler) {
    this.owner = owner;
    this.logger = logger;
    retries = owner.getRetries();
    this.msgId = msgId;
    this.handler = handler;
  }


  @Override public void handle(AsyncResult<Message<JsonObject>> result) {
    logger.fatal("Ack RIN");
    if (result.succeeded()) {
      logger.fatal("Ack RIN");
      //            owner.succeed(msgId);
      //         if (handler != null)
      //            handler.handle(result.result().body());
    } else {
      //IF maximum number of retries reached then fail the message
      //            if (retries == 0) {
      //                owner.fail(msgId);
      //            } else {
      //                //RETRY Sending
      //                retries--;
      //                owner.retry(msgId, this);
      //            }

    }
  }
}
