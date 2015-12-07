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

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Created by vagvaz on 8/4/14.
 */
public class PutObjectHandler implements Handler<HttpServerRequest> {



    Node com;
    Logger log;
    Map<String, PutObjectBodyHandler> bodyHandlers;
    Map<String, PutObjectReplyHandler> replyHandlers;


    public PutObjectHandler(final Node com, Logger log) {
        this.com = com;
        this.log = log;
        replyHandlers = new HashMap<>();
        bodyHandlers = new HashMap<>();
    }

    @Override
    public void handle(HttpServerRequest request) {
        request.response().setStatusCode(200);
        request.response().putHeader(WebStrings.CONTENT_TYPE, WebStrings.APP_JSON);
        log.info("Put Object Request");
        String reqId = UUID.randomUUID().toString();
        PutObjectReplyHandler replyHandler = new PutObjectReplyHandler(reqId, request);
        PutObjectBodyHandler bodyHanlder = new PutObjectBodyHandler(reqId, replyHandler);
        replyHandlers.put(reqId, replyHandler);
        bodyHandlers.put(reqId, bodyHanlder);
        request.bodyHandler(bodyHanlder);
    }

    public void cleanup(String id) {
        PutObjectReplyHandler rh = replyHandlers.remove(id);
        PutObjectBodyHandler bh = bodyHandlers.remove(id);
        rh = null;
        bh = null;
    }


    private class PutObjectReplyHandler implements LeadsMessageHandler {
        HttpServerRequest request;
        String requestId;

        public PutObjectReplyHandler(String requestId, HttpServerRequest request) {
            this.request = request;
            this.requestId = requestId;
        }

        @Override
        public void handle(JsonObject message) {
            if (message.containsField("error")) {
                replyForError(message);
            }
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
                log.error("Empty Request");
                request.response().setStatusCode(400);
            }
            cleanup(requestId);
        }
    }


    private class PutObjectBodyHandler implements Handler<Buffer> {


        private final PutObjectReplyHandler replyHandler;
        private final String requestId;

        public PutObjectBodyHandler(String requestId, PutObjectReplyHandler replyHandler) {
            this.replyHandler = replyHandler;
            this.requestId = requestId;
        }

        @Override
        public void handle(Buffer body) {
            String query = body.getString(0, body.length());
            if (Strings.isNullOrEmpty(query) || query.equals("{}")) {
                replyHandler.replyForError(null);
                return;
            }

            //         object.putString("req-id",requestId);
            Action action = new Action();
            action.setId(requestId);
            action.setCategory(StringConstants.ACTION);
            action.setLabel(IManagerConstants.PUT_OBJECT);
            action.setOwnerId(com.getId());
            action.setComponentType("webservice");
            action.setTriggered("");
            action.setTriggers(new JsonArray());
            JsonObject object = new JsonObject(query);
            action.setData(object);
            action.setDestination(StringConstants.IMANAGERQUEUE);
            action.setStatus(ActionStatus.PENDING.toString());
            com.sendRequestTo(StringConstants.IMANAGERQUEUE, action.asJsonObject(), replyHandler);
        }
    }
}
