package eu.leads.processor.core.comp;

import eu.leads.processor.core.ServiceCommand;
import eu.leads.processor.core.net.MessageTypeConstants;
import eu.leads.processor.core.net.MessageUtils;
import eu.leads.processor.core.net.Node;
import org.vertx.java.core.json.JsonObject;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Created by vagvaz on 7/29/14.
 */
public class ServiceController implements LeadsMessageHandler {
  Map<String, ServiceStatus> serviceStatus;
  Set<String> services;
  Map<ServiceStatus, Set<String>> serviceSets;
  Component owner;
  Node com;
  LogProxy log;

  public ServiceController(Set<String> services, Component owner, Node com, LogProxy log) {
    this.services = new HashSet<>(services.size());
    this.services.addAll(services);
    this.owner = owner;
    this.com = com;
    this.log = log;

    this.serviceStatus = new HashMap<String, ServiceStatus>(services.size());
    for (String service : services) {
      this.serviceStatus.put(service, ServiceStatus.IDLE);
    }
    this.serviceSets = new HashMap<ServiceStatus, Set<String>>(ServiceStatus.values().length);
    for (ServiceStatus candidateStatus : ServiceStatus.values()) {
      this.serviceSets.put(candidateStatus, new HashSet<String>());
    }
    this.serviceSets.get(ServiceStatus.IDLE).addAll(services);
  }

  @Override public void handle(JsonObject message) {
    if (message.getString("type").equals(MessageTypeConstants.SERVICE_STATUS_REPLY)) {
      String service = message.getString(MessageUtils.FROM);
      ServiceStatus serviceStatus = ServiceStatus.valueOf(message.getString("status"));

      switch (owner.getState()) {
        case INITIALIZED:
        case INITIALIZING:
        case STARTING:
        case RUNNING:
          switch (serviceStatus) {
            case IDLE:
              break;
            case INITIALIZING:
              break;
            case INITIALIZED:
              sendStartCommand(service);
              break;
            case RUNNING:
              break;
            case STOPPING:
              //do notghin wait to stop and then init->start
              break;
            case STOPPED:
              sendInitCommand(service);
              break;
            case FAILED:
              reportFail(service, message.getString("status.message"));
              break;
          }
          break;
        case RESETTING:
          switch (serviceStatus) {
            case IDLE:
            case INITIALIZING:
              //Do Nothing in particular
              break;
            case INITIALIZED:
              sendStartCommand(service);
              break;
            case RUNNING:
              //Its Running
              break;
            case STOPPING:
              // Do nothing we must wait to stop.
              break;
            case STOPPED:
              //Send Initialialize
              sendInitCommand(service);
              break;
            case FAILED:
              reportFail(service, message.getString("status.message"));
              break;
          }
          break;
        case STOPPING:
          switch (serviceStatus) {
            case IDLE:
            case INITIALIZING:
              //Do Nothing wait for init and then stop.
              break;
            case INITIALIZED:
              sendStopCommand(service);
              break;
            case RUNNING:
              sendStopCommand(service);
              break;
            case STOPPING:
              // Do nothing we must wait to stop.
              break;
            case STOPPED:
              //Send Initialialize
              break;
            case FAILED:
              reportFail(service, message.getString("status.message"));
              break;
          }
          break;
        case STOPPED:
        case KILLED:
          switch (serviceStatus) {
            case IDLE:
            case INITIALIZING:
              //Do Nothing wait for init and then stop.
              break;
            case INITIALIZED:
              sendStopCommand(service);
              break;
            case RUNNING:
              sendStopCommand(service);
              break;
            case STOPPING:
              // Do nothing we must wait to stop.
              break;
            case STOPPED:
              //We wnated to stop
              break;
            case FAILED:
              reportFail(service, message.getString("status.message"));
              break;
          }
          break;
        default:
          log.error("Unknown state received by service " + service + " " + message.toString());
          break;
      }

      updateServiceStatus(service, serviceStatus);
      checkComponentStatus();
    } else {
      log.error("Unknown message received from controller in " + owner.getComponentType() + ":" + owner.getId());
    }
  }

  private void checkComponentStatus() {
    if (serviceSets.get(ServiceStatus.INITIALIZED).size() == services.size()) {
      owner.setStatus(ComponentState.INITIALIZED);
    }
    if (serviceSets.get(ServiceStatus.RUNNING).size() == services.size()) {
      owner.setStatus(ComponentState.RUNNING);
    }
    if (serviceSets.get(ServiceStatus.STOPPED).size() == services.size()) {
      owner.setStatus(ComponentState.STOPPED);
    }


  }

  private void updateServiceStatus(String service, ServiceStatus newStatus) {
    ServiceStatus oldStatus = this.serviceStatus.get(service);
    if (this.serviceStatus.get(service) != newStatus) {
      this.serviceSets.get(oldStatus).remove(service);
      this.serviceSets.get(newStatus).add(service);
    }
  }

  private void sendInitCommand(String service) {
    JsonObject command = MessageUtils.createServiceCommand(ServiceCommand.INITIALIZE);
    com.sendTo(service, command);
  }

  private void reportFail(String service, String errorMessage) {
    log.error("Service " + service + " failed");
    if (!serviceStatus.get(service).equals(ServiceStatus.FAILED)) { //if service has not failed another time retry
      if (owner.getState().equals(ComponentState.RUNNING) || owner.getState().equals(ComponentState.INITIALIZING)) {
        sendStartCommand(service);
      }
    } else {
      log.error("Component " + owner.getComponentType() + ":" + owner.getId() + " failed due to service " + service
          + " with error\n" + errorMessage);
      owner.setStatus(ComponentState.FAILED);
    }
  }

  private void sendStopCommand(String service) {
    JsonObject command = MessageUtils.createServiceCommand(ServiceCommand.START);
    com.sendTo(service, command);
  }

  private void sendStartCommand(String service) {
    JsonObject command = MessageUtils.createServiceCommand(ServiceCommand.START);
    com.sendTo(service, command);
  }
}
