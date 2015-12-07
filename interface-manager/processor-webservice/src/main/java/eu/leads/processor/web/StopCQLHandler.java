package eu.leads.processor.web;

import com.google.common.base.Strings;
import eu.leads.processor.common.StringConstants;
import eu.leads.processor.core.Action;
import eu.leads.processor.core.ActionStatus;
import eu.leads.processor.core.comp.LeadsMessageHandler;
import eu.leads.processor.core.net.MessageUtils;
import eu.leads.processor.core.net.Node;
import eu.leads.processor.imanager.IManagerConstants;
import eu.leads.processor.nqe.NQEConstants;
import org.vertx.java.core.Handler;
import org.vertx.java.core.buffer.Buffer;
import org.vertx.java.core.http.HttpServerRequest;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.core.logging.Logger;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Created by vagvaz on 9/23/15.
 */
public class StopCQLHandler implements Handler<HttpServerRequest> {
  Node com;
  Logger log;


  public StopCQLHandler(final Node com, Logger log) {
    this.com = com;
    this.log = log;
  }

  @Override
  public void handle(HttpServerRequest request) {
    request.response().setStatusCode(200);
    request.response().putHeader(WebStrings.CONTENT_TYPE, WebStrings.APP_JSON);
    //        log.info("Get Query Results Request");
    String reqId = UUID.randomUUID().toString();

    String queryId = request.params().get("id");
    if (Strings.isNullOrEmpty(queryId)) {
      replyForError(request,null);
      return;
    }
    Action action = new Action();
    action.setId(reqId);
    action.setCategory(StringConstants.ACTION);
    action.setLabel(NQEConstants.STOP_CQL);
    action.setOwnerId(com.getId());
    action.setComponentType("webservice");
    action.setTriggered("");
    action.setTriggers(new JsonArray());
    action.setStatus(ActionStatus.PENDING.toString());
    JsonObject queryRequest = new JsonObject();
    queryRequest.putString("queryId", queryId);
    action.setData(queryRequest);
    com.sendToAllGroup(StringConstants.NODEEXECUTORQUEUE, action.asJsonObject());
    request.response().end( new JsonObject().putString("status","SUCCESS").putString("message","").toString());
  }

  public void cleanup(String id) {
  }



  private void replyForError(HttpServerRequest request,JsonObject message) {
      if (message != null) {
        log.error(message.getString("message"));
        request.response().end("{}");
      } else {
        log.error("Request for Query Status had empty query Id.");
        request.response().setStatusCode(400);
      }

    }
}
