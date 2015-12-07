package eu.leads.processor.core;

import eu.leads.processor.core.comp.ServiceStatus;
import eu.leads.processor.core.net.MessageUtils;
import org.vertx.java.core.AsyncResult;
import org.vertx.java.core.Handler;
import org.vertx.java.core.json.JsonObject;

/**
 * Created by vagvaz on 7/28/14.
 */
public class LogManageVerticle extends ManageVerticle {

  protected final String serviceType = "logService";
  String logVertcileId = null;
  String logAddress = null;
  JsonObject logConfig = null;
  boolean shouldStop = false;

  @Override public void start() {
    super.start();
    initialize(container.config());
  }

  @Override public void startService() {
    super.startService();
    if (logVertcileId == null) {

      logVertcileId = "";
      container.deployVerticle("eu.leads.processor.core.LogVerticle", logConfig, new Handler<AsyncResult<String>>() {

            @Override public void handle(AsyncResult<String> asyncResult) {
              if (asyncResult.succeeded()) {
                container.logger().info("Log Vertice has been deployed ID " + asyncResult.result());
                logVertcileId = asyncResult.result();
                com.sendTo(parent,
                    MessageUtils.createServiceStatusMessage(ServiceStatus.RUNNING, id + ".manage", serviceType));

              } else {
                container.logger().fatal("Log Verticle failed to deploy");
                fail("Log Verticle failed to deploy");
              }
            }
          });

    }
  }

  @Override public void initialize(JsonObject config) {
    super.initialize(config);
    logAddress = id;//".log";
    logConfig = new JsonObject();
    logConfig.putString("id", logAddress);
    com.sendTo(parent, MessageUtils.createServiceStatusMessage(ServiceStatus.INITIALIZED, id + ".manage", serviceType));
  }

  @Override public void cleanup() {
    super.cleanup();
    logConfig = null;
  }

  @Override public void stopService() {
    super.stopService();
    container.undeployVerticle(logVertcileId, new Handler<AsyncResult<Void>>() {

      @Override public void handle(AsyncResult<Void> asyncResult) {
        if (asyncResult.succeeded()) {
          container.logger().info("Log Vertice has been deployed ID " + asyncResult.result());
          com.sendTo(parent,
              MessageUtils.createServiceStatusMessage(ServiceStatus.STOPPED, id + ".manage", serviceType));
          logVertcileId = null;
          if (shouldStop) {
            stop();
          }

        } else {
          container.logger().fatal("Log Verticle failed to undeploy");
          fail("Log Verticle failed to undeploy");
        }
      }
    });
  }

  @Override public ServiceStatus getStatus() {
    return super.getStatus();
  }

  @Override public void setStatus(ServiceStatus status) {
    super.setStatus(status);
  }

  @Override public String getServiceId() {
    return super.getServiceId();
  }

  @Override public String getServiceType() {
    return serviceType;
  }

  @Override public void exitService() {
    shouldStop = true;
    stopService();
  }

  @Override public void fail(String message) {
    super.fail(message);
    JsonObject failMessage = MessageUtils.createServiceStatusMessage(ServiceStatus.FAILED, id, serviceType);
    failMessage.putString("message", message);
    com.sendTo(parent, failMessage);

  }
}
