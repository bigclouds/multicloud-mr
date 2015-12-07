package eu.leads.processor.web;

import com.google.common.base.Strings;

import eu.leads.processor.common.StringConstants;
import eu.leads.processor.core.Action;
import eu.leads.processor.core.ActionStatus;
import eu.leads.processor.core.comp.LeadsMessageHandler;
import eu.leads.processor.core.net.MessageUtils;
import eu.leads.processor.core.net.Node;
import eu.leads.processor.infinispan.MapReduceJob;
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

import static eu.leads.processor.common.StringConstants.IMANAGERQUEUE;

/**
 * Created by Apostolos Nydriotis on 2015/06/22.
 */
public class ExecuteMapReduceJobHandler implements Handler<HttpServerRequest> {

    Node com;
    Logger log;
    Map<String, ExecuteMapReduceJobBodyHandler> bodyHandlers;
    Map<String, ExecuteMapReduceJobReplyHandler> replyHandlers;

    public ExecuteMapReduceJobHandler(final Node com, Logger log) {
        this.com = com;
        this.log = log;
        replyHandlers = new HashMap<>();
        bodyHandlers = new HashMap<>();
    }

    @Override
    public void handle(HttpServerRequest request) {
        request.response().setStatusCode(200);
        request.response().putHeader(WebStrings.CONTENT_TYPE, WebStrings.APP_JSON);
        log.info(this.getClass().getName() + ": Submitting MR Job");
        String reqId = UUID.randomUUID().toString();
        ExecuteMapReduceJobReplyHandler
            replyHandler = new ExecuteMapReduceJobReplyHandler(reqId, request);
        ExecuteMapReduceJobBodyHandler
            bodyHandler = new ExecuteMapReduceJobBodyHandler(reqId, replyHandler);
        replyHandlers.put(reqId, replyHandler);
        bodyHandlers.put(reqId, bodyHandler);
        request.bodyHandler(bodyHandler);
    }

    public void cleanup(String id) {
        ExecuteMapReduceJobReplyHandler rh = replyHandlers.remove(id);
        ExecuteMapReduceJobBodyHandler bh = bodyHandlers.remove(id);
        rh = null;
        bh = null;
    }

    private class ExecuteMapReduceJobReplyHandler implements LeadsMessageHandler {

        HttpServerRequest request;
        String requestId;

        public ExecuteMapReduceJobReplyHandler(String requestId, HttpServerRequest request) {
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

    private class ExecuteMapReduceJobBodyHandler implements Handler<Buffer> {

        private final ExecuteMapReduceJobReplyHandler replyHandler;
        private final String requestId;

        public ExecuteMapReduceJobBodyHandler(String requestId,
            ExecuteMapReduceJobReplyHandler replyHandler) {
            this.replyHandler = replyHandler;
            this.requestId = requestId;
        }

        @Override
        public void handle(Buffer body) {
            String job = body.getString(0, body.length());
            if (Strings.isNullOrEmpty(job) || job.equals("")) {
                replyHandler.replyForError(null);
            }
            JsonObject object = new JsonObject();
            Action action = new Action();
            action.setId(requestId);
            action.setCategory(StringConstants.ACTION);
            action.setLabel(NQEConstants.EXECUTE_MAP_REDUCE_JOB);
            action.setOwnerId(com.getId());
            action.setComponentType("webservice");
            action.setTriggered("");
            action.setTriggers(new JsonArray());
            MapReduceJob jobRequest = new MapReduceJob(job);
            action.setData(jobRequest.asJsonObject());
            action.setDestination(IMANAGERQUEUE);
            action.setStatus(ActionStatus.PENDING.toString());
            com.sendRequestTo(StringConstants.NODEEXECUTORQUEUE, action.asJsonObject(), replyHandler);
        }
    }
}
