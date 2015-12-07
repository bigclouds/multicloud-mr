package eu.leads.processor.nqe;

import eu.leads.processor.core.ManageVerticle;
import eu.leads.processor.core.net.MessageUtils;
import org.vertx.java.core.AsyncResult;
import org.vertx.java.core.Handler;
import org.vertx.java.core.json.JsonObject;

/**
 * Created by vagvaz on 8/3/14.
 */
public class NQELogicManage extends ManageVerticle {

  protected final String serviceType = "nqe-logic";
  protected String plannerAddress;
  protected String nqeAddress;
  protected String workerId;
  protected JsonObject workerConfig;

  @Override public void start() {
    super.start();
    initialize(config);
  }

  @Override public void initialize(JsonObject config) {
    super.initialize(config);
    plannerAddress = config.getString("planner");
    nqeAddress = config.getString("nqe");
    workerConfig = new JsonObject();
    workerConfig.putString("planner", plannerAddress);
    workerConfig.putString("nqe", nqeAddress);
    workerConfig.putString("log", config.getString("log"));
    workerConfig.putString("persistence", config.getString("persistence"));
    workerConfig.putString("id", id);
    workerConfig.putString("workqueue", config.getString("workqueue"));
    workerConfig.putObject("global",config.getObject("global"));
    com.sendTo(parent, MessageUtils.createServiceStatusMessage(status, id, serviceType));

  }

  @Override public void startService() {
    super.startService();
    if (workerId == null) {
      workerId = "";
      container.deployWorkerVerticle(NQELogicWorker.class.getCanonicalName(), workerConfig, 1, false,
          new Handler<AsyncResult<String>>() {
            @Override public void handle(AsyncResult<String> event) {
              if (event.succeeded()) {
                workerId = event.result();
                logProxy.info("NQELogicWorker has been deployed.");
                JsonObject statusMessage = MessageUtils.createServiceStatusMessage(status, id + ".manage", serviceType);
                com.sendTo(parent, statusMessage);
              } else {
                String msg = "NQELogWorker could not be deployed";

                fail(msg);
              }
            }
          });
      //         com.sendTo(parent, MessageUtils.createServiceStatusMessage(status, id, serviceType));
    }
  }

  @Override public void cleanup() {
    super.cleanup();
  }

  @Override public void stopService() {
    super.stopService();
    if (workerId != null) {
      container.undeployModule(workerId);
      workerId = null;
    }
    com.sendTo(parent, MessageUtils.createServiceStatusMessage(status, id, serviceType));

  }

  @Override public void fail(String message) {
    super.fail(message);
    JsonObject msg = MessageUtils.createServiceStatusMessage(status, id, serviceType);
    msg.putString("message", message);
    com.sendTo(parent, msg);
  }

  @Override public String getServiceType() {
    return serviceType;
  }

  @Override public void exitService() {
    System.exit(-1);
  }


}
