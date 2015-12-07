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
import org.vertx.java.core.http.HttpServerRequest;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.core.logging.Logger;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Created by vagvaz on 8/4/14.
 */
public class GetQueryStatusHandler implements Handler<HttpServerRequest> {
    Node com;
    Logger log;
    Map<String, GetQueryStatusReplyHandler> replyHandlers;


    public GetQueryStatusHandler(final Node com, Logger log) {
        this.com = com;
        this.log = log;
        replyHandlers = new HashMap<>();

    }

    @Override
    public void handle(HttpServerRequest request) {
        request.response().setStatusCode(200);
        request.response().putHeader(WebStrings.CONTENT_TYPE, WebStrings.APP_JSON);
//        log.info("Get Query Results Request");
        String reqId = UUID.randomUUID().toString();
        GetQueryStatusReplyHandler replyHandler = new GetQueryStatusReplyHandler(reqId, request);

        String queryId = request.params().get("id");
        if (Strings.isNullOrEmpty(queryId)) {
            replyHandler.replyForError(null);
            return;
        }
        Action action = new Action();
        action.setId(reqId);
        action.setCategory(StringConstants.ACTION);
        action.setLabel(IManagerConstants.GET_QUERY_STATUS);
        action.setOwnerId(com.getId());
        action.setComponentType("webservice");
        action.setTriggered("");
        action.setTriggers(new JsonArray());
        action.setStatus(ActionStatus.PENDING.toString());
        JsonObject queryRequest = new JsonObject();
        queryRequest.putString("queryId", queryId);
        action.setData(queryRequest);
        replyHandlers.put(action.getId(), replyHandler);
        com.sendRequestTo(StringConstants.IMANAGERQUEUE, action.asJsonObject(), replyHandler);
    }

    public void cleanup(String id) {
        GetQueryStatusReplyHandler rh = replyHandlers.remove(id);
        rh = null;
    }


    private class GetQueryStatusReplyHandler implements LeadsMessageHandler {
        HttpServerRequest request;
        String requestId;

        public GetQueryStatusReplyHandler(String requestId, HttpServerRequest request) {
            this.request = request;
            this.requestId = requestId;
        }

        @Override
        public void handle(JsonObject message) {
            if (message.containsField("error")) {
                log.error("and errror " + message.toString());
                replyForError(message);
                return;
            }
//            log.info("GetStatus webservice received reply " + message.getString(MessageUtils.TO) + " " + message.getValue(MessageUtils.MSGID).toString());
            message.removeField(MessageUtils.FROM);
            message.removeField(MessageUtils.TO);
            message.removeField(MessageUtils.COMTYPE);
            message.removeField(MessageUtils.MSGID);
            message.removeField(MessageUtils.MSGTYPE);
//            log.info("end requests");
            request.response().end(message.toString());
            cleanup(requestId);
        }

        private void replyForError(JsonObject message) {
            if (message != null) {
                log.error(message.getString("message"));
                request.response().end("{}");
            } else {
                log.error("Request for Query Status had empty query Id.");
                request.response().setStatusCode(400);
            }
            cleanup(requestId);
        }
    }
}
