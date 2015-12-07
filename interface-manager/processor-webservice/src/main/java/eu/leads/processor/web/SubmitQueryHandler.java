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
 * Created by vagvaz on 8/4/14.
 */
public class SubmitQueryHandler implements Handler<HttpServerRequest> {


    Node com;
    Logger log;
    Map<String, SubmitQueryBodyHandler> bodyHandlers;
    Map<String, SubmitQueryReplyHandler> replyHandlers;


    public SubmitQueryHandler(final Node com, Logger log) {
        this.com = com;
        this.log = log;
        replyHandlers = new HashMap<>();
        bodyHandlers = new HashMap<>();
    }

    @Override
    public void handle(HttpServerRequest request) {
        request.response().setStatusCode(200);
        request.response().putHeader(WebStrings.CONTENT_TYPE, WebStrings.APP_JSON);
        log.info("Submit Query ");
        String reqId = UUID.randomUUID().toString();
        SubmitQueryReplyHandler replyHandler = new SubmitQueryReplyHandler(reqId, request);
        SubmitQueryBodyHandler bodyHanlder = new SubmitQueryBodyHandler(reqId, replyHandler);
        replyHandlers.put(reqId, replyHandler);
        bodyHandlers.put(reqId, bodyHanlder);
        request.bodyHandler(bodyHanlder);
    }

    public void cleanup(String id) {
        SubmitQueryReplyHandler rh = replyHandlers.remove(id);
        SubmitQueryBodyHandler bh = bodyHandlers.remove(id);
        rh = null;
        bh = null;
    }


    private class SubmitQueryReplyHandler implements LeadsMessageHandler {
        HttpServerRequest request;
        String requestId;

        public SubmitQueryReplyHandler(String requestId, HttpServerRequest request) {
            this.request = request;
            this.requestId = requestId;
        }

        @Override
        public void handle(JsonObject message) {
            if (message.containsField("error")) {
                replyForError(message);
                return;
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
                log.error("No Query to submit");
                request.response().setStatusCode(400);
            }
            cleanup(requestId);
        }
    }


    private class SubmitQueryBodyHandler implements Handler<Buffer> {


        private final SubmitQueryReplyHandler replyHandler;
        private final String requestId;

        public SubmitQueryBodyHandler(String requestId, SubmitQueryReplyHandler replyHandler) {
            this.replyHandler = replyHandler;
            this.requestId = requestId;
        }

        @Override
        public void handle(Buffer body) {
            String query = body.getString(0, body.length());
            if (Strings.isNullOrEmpty(query) || query.equals("{}")) {
                replyHandler.replyForError(null);
            }

            Action action = new Action();
            action.setId(requestId);
            action.setCategory(StringConstants.ACTION);

            JsonObject queryJ = new JsonObject(query);
            if(queryJ.getString("sql").toLowerCase().startsWith("quit")) {
                action.setLabel(IManagerConstants.QUIT);
                action.setComponentType("webservice");
                com.sendToAllGroup("leads.processor.control", action.asJsonObject());
                    if(com.getConfig().containsField("webserviceAddrs")) {
                        JsonArray webAddress = com.getConfig().getArray("webserviceAddrs"); //contains other webservice address
                        for (Object addrs : webAddress.toList()) {
                            try {
                                System.out.println("WebService connect to " + addrs);
                                WebServiceClient.initialize((String) addrs);
                                System.out.println("Send quit to " + addrs);
                                WebServiceClient.submitQuery("leads", queryJ.getString("sql"));
                            } catch (IOException e) {
                                System.err.println("Unable to send quit to" + addrs);
                                e.printStackTrace();
                            }
                        }
                    }

             }
            else {
                action.setLabel(IManagerConstants.SUBMIT_QUERY);
                action.setOwnerId(com.getId());
                action.setComponentType("webservice");
                action.setTriggered("");
                action.setTriggers(new JsonArray());
                JsonObject queryRequest = new JsonObject(query);
                action.setData(queryRequest);
                action.setDestination(IMANAGERQUEUE);
                action.setStatus(ActionStatus.PENDING.toString());
                com.sendRequestTo(IMANAGERQUEUE, action.asJsonObject(), replyHandler);
            }
        }
    }
}
