package eu.leads.processor.web;

import com.google.common.base.Strings;
import eu.leads.processor.common.StringConstants;
import eu.leads.processor.core.Action;
import eu.leads.processor.core.ActionStatus;
import eu.leads.processor.core.comp.LeadsMessageHandler;
import eu.leads.processor.core.net.MessageUtils;
import eu.leads.processor.core.net.Node;
import eu.leads.processor.imanager.IManagerConstants;
import org.vertx.java.core.Handler;
import org.vertx.java.core.buffer.Buffer;
import org.vertx.java.core.http.HttpServerRequest;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.core.logging.Logger;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static eu.leads.processor.common.StringConstants.IMANAGERQUEUE;

/**
 * Created by vagvaz on 9/23/15.
 */
public class AddListenerHandler implements Handler<HttpServerRequest> {

  Node com;
  Logger log;
  Map<String, AddListenerBodyHandler> bodyHandlers;
  Map<String, AddListenerReplyHandler> replyHandlers;
  private String listener;
  private String cache;


  public AddListenerHandler(final Node com, Logger log) {
    this.com = com;
    this.log = log;
    replyHandlers = new HashMap<>();
    bodyHandlers = new HashMap<>();
  }

  @Override
  public void handle(HttpServerRequest request) {
    request.response().setStatusCode(200);
    request.response().putHeader(WebStrings.CONTENT_TYPE, WebStrings.APP_JSON);

    String reqId = UUID.randomUUID().toString();
    AddListenerReplyHandler replyHandler = new AddListenerReplyHandler(reqId, request);
    AddListenerBodyHandler bodyHanlder = new AddListenerBodyHandler(reqId, replyHandler);
    replyHandlers.put(reqId, replyHandler);
    bodyHandlers.put(reqId, bodyHanlder);
    cache = request.params().get("cache");
    listener = request.params().get("listener");

    request.bodyHandler(bodyHanlder);
  }

  public void cleanup(String id) {
    AddListenerReplyHandler rh = replyHandlers.remove(id);
    AddListenerBodyHandler bh = bodyHandlers.remove(id);
    rh = null;
    bh = null;
  }


  private class AddListenerReplyHandler implements LeadsMessageHandler {
    HttpServerRequest request;
    String requestId;

    public AddListenerReplyHandler(String requestId, HttpServerRequest request) {
      this.request = request;
      this.requestId = requestId;
    }

    @Override
    public void handle(JsonObject message) {
      if (message.containsField("error")) {
        replyForError(message);
        return;
      }
      System.err.println("Replying " + message.toString());
      message.removeField(MessageUtils.FROM);
      message.removeField(MessageUtils.TO);
      message.removeField(MessageUtils.COMTYPE);
      message.removeField(MessageUtils.MSGID);
      message.removeField(MessageUtils.MSGTYPE);
      request.response().end(message.toString());
      cleanup(requestId);
    }

    private void replyForError(JsonObject message) {
      if (message != null) {
        log.error(message.getString("message"));
        request.response().end("{}");
      } else {
        log.error("nothing to add as listener");
        request.response().setStatusCode(400);
      }
      cleanup(requestId);
    }
  }


  private class AddListenerBodyHandler implements Handler<Buffer> {


    private final AddListenerReplyHandler replyHandler;
    private final String requestId;

    public AddListenerBodyHandler(String requestId, AddListenerReplyHandler replyHandler) {
      this.replyHandler = replyHandler;
      this.requestId = requestId;
    }

    @Override
    public void handle(Buffer body) {
      String query = body.getString(0, body.length());
      if (Strings.isNullOrEmpty(query) || query.equals("{}")) {
        replyHandler.replyForError(null);
      }

      System.err.println(" add listener");
      Action action = new Action();
      action.setId(requestId);
      action.setCategory(StringConstants.ACTION);

      JsonObject queryJ = new JsonObject(query);
      cache = queryJ.getString("cache");
      listener = queryJ.getString("listener");
      if (Strings.isNullOrEmpty(cache)) {
        replyHandler.replyForError(null);
        return;
      }

      action.setCategory(StringConstants.ACTION);
      action.setLabel(IManagerConstants.ADD_LISTENER);
      action.setOwnerId(com.getId());
      action.setComponentType("webservice");
      action.setTriggered("");
      action.setTriggers(new JsonArray());
      action.setStatus(ActionStatus.PENDING.toString());
      JsonObject queryRequest = new JsonObject(query);
//      queryRequest.putString("cache", cache);
//      queryRequest.putString("listener", listener);
//      queryRequest.putObject("conf",queryJ);
      System.err.println(query.toString());
      action.setData(queryRequest);
      com.sendRequestToGroup(IMANAGERQUEUE,action.asJsonObject(),replyHandler);
    }
  }
}
