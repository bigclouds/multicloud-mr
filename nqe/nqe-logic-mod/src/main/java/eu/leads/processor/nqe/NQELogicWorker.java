package eu.leads.processor.nqe;

import eu.leads.processor.common.StringConstants;
import eu.leads.processor.common.utils.storage.LeadsStorage;
import eu.leads.processor.common.utils.storage.LeadsStorageFactory;
import eu.leads.processor.conf.LQPConfiguration;
import eu.leads.processor.core.Action;
import eu.leads.processor.core.ActionStatus;
import eu.leads.processor.core.comp.LeadsMessageHandler;
import eu.leads.processor.core.comp.LogProxy;
import eu.leads.processor.core.net.DefaultNode;
import eu.leads.processor.core.net.MessageUtils;
import eu.leads.processor.core.net.Node;
import eu.leads.processor.imanager.IManagerConstants;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.platform.Verticle;

import java.util.Properties;
import java.util.UUID;

import static eu.leads.processor.core.ActionStatus.PENDING;

/**
 * Created by vagvaz on 8/4/14.
 */
public class NQELogicWorker extends Verticle implements LeadsMessageHandler {

  private final String componentType = "nqe";
  JsonObject config;
  JsonObject globalConfig;
  String monitor;
  String nqeGroup;
  LogProxy log;
  Node com;
  String id;
  String workQueueAddress;
  String currentCluster;
  LeadsStorage storage;

  @Override public void start() {
    super.start();
    LQPConfiguration.initialize();
    config = container.config();
    monitor = config.getString("monitor");
    nqeGroup = config.getString("nqe");
    workQueueAddress = config.getString("workqueue");
    id = config.getString("id");
    com = new DefaultNode();
    com.initialize(id, nqeGroup, null, this, null, vertx);
    log = new LogProxy(config.getString("log"), com);
    Properties storageConf = new Properties();
    storageConf.setProperty("prefix", "/tmp/leads/");


    if(config.containsField("global")){
      JsonObject global = config.getObject("global");
      globalConfig = global;
      if(global.containsField("hdfs.uri") && global.containsField("hdfs.prefix") && global.containsField("hdfs.user"))
      {
        storageConf.setProperty("hdfs.url", global.getString("hdfs.uri"));
        storageConf.setProperty("fs.defaultFS", global.getString("hdfs.uri"));
        storageConf.setProperty("fs.hdfs.impl", org.apache.hadoop.hdfs.DistributedFileSystem.class.getName());
        storageConf.setProperty("fs.file.impl", org.apache.hadoop.fs.LocalFileSystem.class.getName());
        storageConf.setProperty("prefix", global.getString("hdfs.prefix"));
        storageConf.setProperty("hdfs.user", global.getString("hdfs.user"));
        storageConf.setProperty("postfix", "0");
        System.out.println("USING HDFS yeah!");
        log.info("using hdfs: " + global.getString("hdfs.user")+ " @ "+ global.getString("hdfs.uri") + global.getString("hdfs.prefix") );

        storage = LeadsStorageFactory.getInitializedStorage(LeadsStorageFactory.HDFS,storageConf);
      }else
      {
        log.info("No defined all hdfs parameters using local storage ");
        storage = LeadsStorageFactory.getInitializedStorage(LeadsStorageFactory.LOCAL, storageConf);
      }
    }else {
      storage = LeadsStorageFactory.getInitializedStorage(LeadsStorageFactory.LOCAL, storageConf);
    }
  }

  @Override public void stop() {
    super.stop();
    com.unsubscribeFromAll();
  }

  @Override public void handle(JsonObject msg) {
    String type = msg.getString("type");
    String from = msg.getString(MessageUtils.FROM);
    String to = msg.getString(MessageUtils.TO);

    if (type.equals("action")) {
      Action action = new Action(msg);
      String label = action.getLabel();
      Action newAction = null;
      action.setProcessedBy(id);
      //         action.setStatus(ActionStatus.INPROCESS.toString());

      switch (ActionStatus.valueOf(action.getStatus())) {
        case PENDING: //probably received an action from an external source
          if (label.equals(NQEConstants.DEPLOY_OPERATOR)) {
            action.getData().putString("replyTo", action.getData().getString("monitor"));
            action.setStatus(ActionStatus.INPROCESS.toString());
            com.sendWithEventBus(workQueueAddress, action.asJsonObject());
          } else if ((label.equals(NQEConstants.DEPLOY_PLUGIN)) || (label.equals(NQEConstants.UNDEPLOY_PLUGIN))) {
            action.setStatus(ActionStatus.INPROCESS.toString());
            com.sendWithEventBus(workQueueAddress, action.asJsonObject());
          } else if (label.equals(NQEConstants.DEPLOY_REMOTE_OPERATOR)) {
            System.err.println("RECEVEIVED REMOTE DEPLOY!!!!!!!!!!");
            action.setStatus(ActionStatus.INPROCESS.toString());
            com.sendWithEventBus(workQueueAddress, action.asJsonObject());
          } else if (label.equals(NQEConstants.EXECUTE_MAP_REDUCE_JOB)) {
            //            String actionId = UUID.randomUUID().toString();
            //            action.setId(actionId);
            action.getData().putString("replyTo", from);
            com.sendWithEventBus(workQueueAddress, action.asJsonObject());
          } else if (label.equals(IManagerConstants.EXECUTE_MAPREDUCE)) {
            //handle the remote execution mapreduce
            action.getData().putString("replyTo", msg.getString("from"));
            com.sendWithEventBus(workQueueAddress, action.asJsonObject());
          } else if (label.equals(IManagerConstants.COMPLETED_MAPREDUCE)) {
            action.getData().putString("replyTo", msg.getString("from"));
            com.sendWithEventBus(workQueueAddress, action.asJsonObject());
          } else if (label.equals(IManagerConstants.PUT_OBJECT)) {
            action.getData().putString("replyTo", msg.getString("from"));
            com.sendWithEventBus(workQueueAddress, action.asJsonObject());
          } else if (label.equals(IManagerConstants.GET_QUERY_STATUS)) {
            action.getData().putString("replyTo", msg.getString("from"));
            com.sendWithEventBus(workQueueAddress, action.asJsonObject());
          } else if (label.equals(IManagerConstants.STOP_CACHE)) {
            action.getData().putString("replyTo", msg.getString("from"));
            com.sendWithEventBus(workQueueAddress, action.asJsonObject());
          } else if (label.equals(IManagerConstants.ADD_LISTENER)) {
            action.getData().putString("replyTo", msg.getString("from"));
            com.sendWithEventBus(workQueueAddress, action.asJsonObject());
          } else if (label.equals(IManagerConstants.REMOVE_LISTENER)) {
            action.getData().putString("replyTo", msg.getString("from"));
            com.sendWithEventBus(workQueueAddress, action.asJsonObject());
          } else if(label.equals(IManagerConstants.UPLOAD_DATA)){
            String path = action.getData().getString("path");
            byte[] data = action.getData().getBinary("data");
            storage.writeData(path,data);
            action.getData().putString("replyTo", msg.getString("from"));
            JsonObject result = new JsonObject();
            result.putString("status","SUCCESS");
            result.putString("message","");
            com.sendTo(action.getData().getString("replyTo"), result);
          }
          else {
            log.error("Unknown PENDING Action received " + action.toString());
            return;
          }
          action.setStatus(ActionStatus.INPROCESS.toString());
          if (newAction != null) {
            action.addChildAction(newAction.getId());
            logAction(newAction);
          }
          logAction(action);
          break;
        case INPROCESS: //  probably received an action from internal source (processors)
        case COMPLETED: // the action either a part of a multistep workflow (INPROCESSING) or it could be processed.
          if (label.equals(NQEConstants.DEPLOY_OPERATOR)) {
            com.sendTo(action.getData().getString("replyTo"), action.getResult());
          } else if ((label.equals(NQEConstants.DEPLOY_PLUGIN)) || (label.equals(NQEConstants.UNDEPLOY_PLUGIN))) {
            //                      action.setStatus(ActionStatus.INPROCESS.toString());
            //                      com.sendWithEventBus((workQueueAddress,action.asJsonObject());
          } else if (label.equals(NQEConstants.DEPLOY_REMOTE_OPERATOR)) {
            newAction = new Action(action);
            newAction.setData(action.getData());
            newAction.getData().putString("microcloud", currentCluster);
            newAction.getData().putString("STATUS", "SUCCESS");
          } else if (label.equals(NQEConstants.EXECUTE_MAP_REDUCE_JOB)) {
            //here reply to the webservice with the ID of the job
            com.sendTo(action.getData().getString("replyTo"), action.getResult());
          } else if (label.equals(IManagerConstants.EXECUTE_MAPREDUCE)) {
            //send the completed message to the correct nqe
            JsonObject webServiceReply = action.getResult().getObject("status");
            com.sendTo(action.getData().getString("replyTo"), webServiceReply);
            newAction = createNewAction(action);
            newAction.setLabel(NQEConstants.DEPLOY_REMOTE_OPERATOR);
            newAction.setDestination((StringConstants.NODEEXECUTORQUEUE));
            newAction.setData(action.getResult().getObject("result"));
            com.sendWithEventBus(workQueueAddress, newAction.asJsonObject());

          } else if (label.equals(IManagerConstants.COMPLETED_MAPREDUCE)) {
            JsonObject webServiceReply = action.getResult().getObject("status");
            com.sendTo(action.getData().getString("replyTo"), webServiceReply);
            newAction = createNewAction(action);
            newAction.setLabel(NQEConstants.OPERATOR_COMPLETE);
            newAction.setDestination(action.getResult().getString("replyGroup"));
            newAction.setData(action.getResult().getObject("result"));
            newAction.setStatus(ActionStatus.COMPLETED.toString());
            com.sendTo(newAction.getDestination(), newAction.asJsonObject());
          } else if (label.equals(IManagerConstants.PUT_OBJECT)) {
            if (!action.getResult().containsField("message")) {
              action.getResult().putString("message", "");
            }
            com.sendTo(action.getData().getString("replyTo"), action.getResult());
          } else if (label.equals(IManagerConstants.GET_QUERY_STATUS)) {
            com.sendTo(action.getData().getString("replyTo"), action.getResult());
          } else if (label.equals(IManagerConstants.QUIT)) {
            System.out.println(" Imanager logic worker recovery ");
            stop();
          } else if (label.equals(IManagerConstants.STOP_CACHE)) {
            JsonObject webServiceReply = action.getResult();
            com.sendTo(action.getData().getString("replyTo"), webServiceReply);
          } else if (label.equals(IManagerConstants.ADD_LISTENER)) {
            JsonObject webServiceReply = action.getResult();
            com.sendTo(action.getData().getString("replyTo"), webServiceReply);
          } else if (label.equals(IManagerConstants.REMOVE_LISTENER)) {
            JsonObject webServiceReply = action.getResult();
            com.sendTo(action.getData().getString("replyTo"), webServiceReply);
          } else {
            log.error("Unknown COMPLETED OR INPROCESS Action received " + action.toString());
            return;
          }
          finalizeAction(action);
      }
    }
  }

  private void finalizeAction(Action action) {
    //TODO
    //1 inform monitor about completion (if it is completed in this logic each action requires 1 step processing
    //2 remove from processing if necessary
    //3 update action to persistence service
  }


  private void logAction(Action action) {
    //TODO
    //1 inform monitor about action.
    //2 add action to processing set
    //3 update action to persistence service
  }

  private Action createNewAction(Action action) {
    Action result = new Action();
    result.setId(UUID.randomUUID().toString());
    result.setTriggered(action.getId());
    result.setComponentType(componentType);
    result.setStatus(PENDING.toString());
    result.setTriggers(new JsonArray());
    result.setOwnerId(this.id);
    result.setProcessedBy("");
    result.setDestination("");
    result.setData(new JsonObject());
    result.setResult(new JsonObject());
    result.setLabel("");
    result.setCategory("");
    return result;
  }

}
