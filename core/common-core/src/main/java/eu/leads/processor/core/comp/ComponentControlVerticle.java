package eu.leads.processor.core.comp;


import eu.leads.processor.common.StringConstants;
import eu.leads.processor.core.Action;
import eu.leads.processor.core.ActionStatus;
import eu.leads.processor.core.ServiceCommand;
import eu.leads.processor.core.net.DefaultNode;
import eu.leads.processor.core.net.MessageUtils;
import eu.leads.processor.core.net.Node;
import eu.leads.processor.imanager.IManagerConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vertx.java.core.AsyncResult;
import org.vertx.java.core.Handler;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.platform.Verticle;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.UUID;

/**
 * Created by vagvaz on 7/28/14.
 */
public class ComponentControlVerticle extends Verticle implements Component {

  protected boolean resetting = false;
  protected boolean shuttingdown = false;
  protected ComponentMode mode = ComponentMode.NORMAL;
  protected ComponentState state;

  protected String workQueueId; //worqueue module deployment id, used for undeployment
  protected String workQueueAddress; //WorkQueue Address, the address where processors register
  protected String logId; // log module deployment id, used for undeployment
  protected String logAddress; //prefix used for all id services
  protected JsonObject logConfig; 
  protected String persistenceId; //persistence module deployment id, used for undeployment
  //    protected String persistenceAddress;
  //The persistence address, that services can read/write state/data
  protected JsonObject persistConfig;
  protected Set<String> processorIds; // processors modules deployment id, used for undeployment
  protected Set<String> processorAddresses; //processors ids
  protected JsonObject processorConfig; // processor config
  protected String logicId; // logic module deployment id, used for undeployment
  protected String logicAddress;
  //logic Component address, in the logic module of each component the actual workflow is implemented
  protected JsonObject logicConfig; //logig module configuration
  protected Node com;
  protected LogProxy logg;
  //   protected PersistenceProxy persistence;
  protected LeadsComponentHandler componentHandler;
  protected JsonObject config; //components deployment configuration
  protected String id; //The unique id for this deployment (uuid)
  protected String componentType;
  // the component type for this deployment possible values n, {webservice,planner,deployer,nqe,monitor,stats}
  protected String group; //component group
  protected String internalGroup;
  // The internal group used to communicate between ComponentControl Verticle and the services
  protected Set<String> secondaryGroups; // Secondary groups that the deployment should listen.
  protected JsonArray services; // The rest non-default services that must be deployed by the
  protected Set<String> servicesIds;
  protected int numberOfprocessors;
  protected JsonObject workQueueConfig;
  protected LeadsMessageHandler failHandler;
  protected ServiceController serviceController;
  protected Logger log;

  /**
   * Verticle start reads the configuration for the deployed component. Including:
   * component id, the uuid
   * component group, the group of the component
   * component type, the type of the component that shyould be run, {webservice,planner,deployer,nqe,monitor,stats}
   * number of processors, the number of processors that should be register in the workQueue module
   * services, the services that should be deployed Apart from the log,persistence, that are automatically deployed
   * for all the components
   */


  @Override public void start() {
    super.start();
    config = container.config();
//    id = config.getString("id");
    id = UUID.randomUUID().toString();
    config.putString("id", id);
    group = config.getString("group");
    componentType = config.getString("componentType");
    numberOfprocessors = Integer.valueOf(config.getString("processors", "1"));
    services = config.getArray("services");
    if (services == null)
      services = new JsonArray();
    com = new DefaultNode();
    secondaryGroups = new HashSet<>();
    if (config.containsField("otherGroups"))
      secondaryGroups.addAll(config.getArray("otherGroups").toList());

    log = LoggerFactory.getLogger(this.getClass());
    this.state = ComponentState.IDLE;
  }

  /**
   * Sets up the basic structures that are required in order to deploy the module. LogAddress,workQueueAddress,
   * persistenceAddress,processorAddresses. It also initializes the configuration for Log,Persistence,WorkQueue modules
   *
   * @param conf
   */
  @Override public void setup(JsonObject conf) {
    boolean callStart = false;

    if (this.state == ComponentState.RESETTING)
      callStart = true;

    this.state = ComponentState.INITIALIZING;
    //Set state to initializing.
    String componentPrefix = componentType + "." + id;
    logAddress = componentPrefix + ".log"; //log Address, this will be used by all services for logging
    logg = new LogProxy(logAddress, com);

    //        persistenceAddress = componentPrefix + ".processor-0";
    //        persistence = new PersistenceProxy(persistenceAddress, com);

    workQueueAddress = componentPrefix + ".workQueue";

    logicAddress = componentPrefix + ".logic";

    processorAddresses = new HashSet<String>(); //processor ids
    processorIds = new HashSet<>();
    for (int processor = 0; processor < numberOfprocessors; processor++) {
      processorAddresses.add(componentPrefix + ".processror-" + Integer.toString(processor));
    }

    servicesIds = new HashSet<>();

    internalGroup = componentPrefix + ".servicesGroup";
    failHandler = new DefaultFailHandler(this, com, logg);
    componentHandler = new LeadsComponentHandler(this, com, logg);

    //TODO check that messages will not be lost between controlverticle and logic (cause id)
    com.initialize(id + ".control", group, secondaryGroups, componentHandler, failHandler, vertx);


    logConfig = new JsonObject();
    logConfig.putString("id", logAddress);
    logConfig.putString("group", internalGroup);
    logConfig.putString("log", logAddress);
    logConfig.putString("persistence", "persistence");
    logConfig.putString("parent", id + ".serviceMonitor");
    logConfig.putString("componentType", getComponentType());
    logConfig.putString("componentId", getId());


    //        persistConfig = new JsonObject();
    //        persistConfig.putString("id", persistenceAddress);
    //        persistConfig.putString("group", internalGroup);
    //        persistConfig.putString("log", logAddress);
    //        persistConfig.putString("persistence", persistenceAddress);
    //        persistConfig.putString("parent", id + ".serviceMonitor");
    //        persistConfig.putString("componentType", getComponentType());
    //        persistConfig.putString("componentId", getId());

    workQueueConfig = new JsonObject();
    workQueueConfig.putString("address", workQueueAddress);
    workQueueConfig.putNumber("process_timeout", 35 * 60 * 1000);

    logicConfig = new JsonObject();
    logicConfig.putString("id", logicAddress);
    logicConfig.putString("group", internalGroup);
    logicConfig.putString("log", logAddress);
    logicConfig.putString("persistence", "persistence");
    logicConfig.putString("parent", id + ".serviceMonitor");
    logicConfig.putString("componentType", componentType);
    logicConfig.putString("workqueue", workQueueAddress);
    logicConfig.putString("componentId", getId());
    logicConfig.putObject("global", config.getObject("global"));
    if (config.containsField("logic")) {
      logicConfig.mergeIn(config.getObject("logic"));
    }

    processorConfig = new JsonObject();
    processorConfig.putString("id", ""); //empty because for each processor there will be different id.
    processorConfig.putString("group", internalGroup);
    processorConfig.putString("workqueue", workQueueAddress);
    processorConfig.putString("log", logAddress);
    processorConfig.putString("persistence", "persistence");
    processorConfig.putString("logic", logicAddress);
    processorConfig.putString("parent", id + ".serviceMonitor");
    processorConfig.putString("componentType", getComponentType());
    processorConfig.putString("componentId", getId());
    processorConfig.putObject("global", conf.getObject("global"));
    if (config.containsField("processor")) {
      processorConfig = processorConfig.mergeIn(config.getObject("processor"));
    }


    //Create An array of the services that must be managed by this component at least log,persistence,logic,processors
    Set<String> arrayServices = new HashSet<>();
    arrayServices.add(logAddress + ".manage");
    //        arrayServices.add(persistenceAddress + ".manage");
    arrayServices.add(logicAddress + ".manage");
    for (String proc : processorAddresses)
      arrayServices.add(proc + ".manage");
    Iterator<Object> serviceIterator = services.iterator();
    while (serviceIterator.hasNext()) {
      JsonObject s = (JsonObject) serviceIterator.next();
      arrayServices.add(componentPrefix + "." + s.getString("type") + ".manage");
    }
    serviceController = new ServiceController(arrayServices, this, com, logg);
    com.subscribe(id + ".serviceMonitor", serviceController);//, new StartCallable(callStart,this));
    this.state = ComponentState.INITIALIZED;
    if (callStart)
      startUp();
  }

  @Override public void startUp() {
    //Set state to staring and start deploying all the service modules
    this.state = ComponentState.RUNNING;

    //deploy log module
    container.deployModule(StringConstants.LOG_MOD_NAME, logConfig, new Handler<AsyncResult<String>>() {

          @Override public void handle(AsyncResult<String> asyncResult) {
            if (asyncResult.succeeded()) {
              container.logger().info("Log Module has been deployed ID " + asyncResult.result());
              logId = asyncResult.result();
            } else {
              container.logger().fatal("Log Module failed to deploy");
            }
          }
        });

    //deploy persistence module
    //        container.deployModule(StringConstants.PERSIST_MOD_NAME, persistConfig, 1,
    //                                  new Handler<AsyncResult<String>>() {
    //
    //                                      @Override
    //                                      public void handle(AsyncResult<String> asyncResult) {
    //                                          if (asyncResult.succeeded()) {
    //                                              container.logger()
    //                                                  .info("Persistence Module has been deployed ID "
    //                                                            + asyncResult.result());
    //                                              persistenceId = asyncResult.result();
    //                                          } else {
    //                                              container.logger()
    //                                                  .fatal("Persistence Module failed to deploy");
    //                                          }
    //                                      }
    //                                  });

    //deploy workqueue module.
    container.deployModule(StringConstants.WORKQUEUE_MOD_NAME, workQueueConfig, 1, new Handler<AsyncResult<String>>() {

          @Override public void handle(AsyncResult<String> asyncResult) {
            if (asyncResult.succeeded()) {
              container.logger().info("WorkerQueue Module has been deployed ID " + asyncResult.result());
              workQueueId = asyncResult.result();
            } else {
              container.logger().fatal("WorkerQueue Module failed to deploy");
            }
          }
        });

    //Deploy processors module.
    Iterator<String> processorIdIterator = processorAddresses.iterator();
    for (int processor = 0; processor < numberOfprocessors; processor++) {
      processorConfig.putString("id", processorIdIterator.next());
      container.deployModule(
          config.getString("groupId") + "~" + componentType + "-processor-mod~" + config.getString("version"),
          processorConfig, 1, new Handler<AsyncResult<String>>() {

            @Override public void handle(AsyncResult<String> asyncResult) {
              if (asyncResult.succeeded()) {
                container.logger()
                    .info(componentType + " Processor Module  has been deployed ID " + asyncResult.result());
                processorIds.add(asyncResult.result());
              } else {
                container.logger().fatal(componentType + " Processor Module failed to deploy");
              }
            }
          });
    }
    //deploy logic module
    container
        .deployModule(config.getString("groupId") + "~" + componentType + "-logic-mod~" + config.getString("version"),
            logicConfig, 1, new Handler<AsyncResult<String>>() {

              @Override public void handle(AsyncResult<String> asyncResult) {
                if (asyncResult.succeeded()) {
                  container.logger().info(componentType + " Logic Module has been deployed ID " + asyncResult.result());
                  logicId = asyncResult.result();
                } else {
                  container.logger().fatal(componentType + "Logic Module failed to deploy");
                }
              }
            });

    //Deploly the rest services defined in the configuration.
    //processorConfig can work as basis for the services basic configuration

    JsonObject basicConf = processorConfig.copy();
    Iterator<Object> serviceIterator = services.iterator();
    //Iterate over all services, customize configuration and then deploy the respective vertx module.
    while (serviceIterator.hasNext()) {
      JsonObject service = (JsonObject) serviceIterator.next();
      final String serviceType = service.getString("type");
      JsonObject conf = service.getObject("conf");
      for (String field : conf.getFieldNames()) {
        //replace id and group variables in the configuration values.
        if (field.equals("id") || field.equals("group")) {
          if (conf.getString(field).contains("$id")) {
            conf.putString(field, conf.getString(field).replace("$id", id));
          }
          if (conf.getString(field).contains("$group")) {
            conf.putString(field, conf.getString(field).replace("$group", group));
          }
        }
      }
      //merge into the service configuration the basic Configuration
      conf = basicConf.mergeIn(conf);
      if (!conf.containsField("global"))
        conf.putObject("global", processorConfig.getObject("global"));
      container.deployModule(
          config.getString("groupId") + "~" + componentType + "-" + serviceType + "-mod~" + config.getString("version"),
          conf, 1, new Handler<AsyncResult<String>>() {

        @Override public void handle(AsyncResult<String> asyncResult) {
          if (asyncResult.succeeded()) {
            container.logger().info(
                componentType + " Service " + serviceType + " Module has been deployed ID " + asyncResult.result());
            servicesIds.add(asyncResult.result());
          } else {
            container.logger().fatal(componentType + " Service " + serviceType + " Module failed to deploy");
          }
        }
      });

    }
    //subscribe to componentType control
    com.subscribe(componentType + ".control", componentHandler);
    com.subscribe("leads.processor.control", componentHandler);
  }

  @Override public void stopComponent() {
    //In order to stop the component we must stop all the services
    com.sendToAllGroup(internalGroup, MessageUtils.createServiceCommand(ServiceCommand.STOP));
  }

  @Override public void shutdown() {
    //GRACEFULLY shutting down
    this.state = ComponentState.STOPPING;
    com.sendToAllGroup(internalGroup, MessageUtils.createServiceCommand(ServiceCommand.EXIT));

  }


  public void undeployAllModules() {
    container.undeployModule(logicId);
    for (String processor : processorIds) {
      container.undeployModule(processor);
    }
    for (String service : servicesIds) {
      container.undeployModule(service);
    }
    container.undeployModule(workQueueId);
    container.undeployModule(persistenceId);
    container.undeployModule(logId);
  }

  @Override public void reset(JsonObject conf) {
    this.state = ComponentState.RESETTING;
    config = conf;

    id = config.getString("id");
    group = config.getString("group");
    componentType = config.getString("componentType");
    numberOfprocessors = Integer.valueOf(config.getString("number_processors", "1"));
    services = config.getArray("services");
    stopComponent();
  }

  @Override public void cleanup() {


  }

  @Override public void kill() {
    Action action = new Action();
    action.setLabel(IManagerConstants.QUIT);
    action.setId("-3");
    action.setStatus(ActionStatus.PENDING.toString());
    com.sendRequestTo(workQueueAddress, action.asJsonObject(), failHandler);
    if (mode != null) {
      if (mode.equals(ComponentMode.TESTING)) {
        log.info(componentType + "." + id + " is being killed ");
        vertx.setTimer(4000, new Handler<Long>() {
          @Override public void handle(Long aLong) {
            System.out.println(" Control component stopping ");
            vertx.stop();// System.exit(0);
          }
        });
      } else {
        log.error(componentType + "." + id + " received kill command but mode is  " + mode.toString());
      }
      com.sendToGroup(workQueueAddress, action.asJsonObject());
      com.unsubscribeFromAll();
      vertx.setTimer(4000, new Handler<Long>() {
        @Override public void handle(Long aLong) {
          System.out.println(" Control component Stopping ");
          vertx.stop();
          //System.exit(0);
        }
      });
    } else {
      log.error(componentType + "." + id + " received kill command but mode is null, stopping anyway ");
      com.sendToGroup(workQueueAddress, action.asJsonObject());
      com.unsubscribeFromAll();
      vertx.setTimer(4000, new Handler<Long>() {
        @Override public void handle(Long aLong) {
          System.out.println(" Control component Stopping ");
          vertx.stop();
          //System.exit(0);
        }
      });
    }

  }

  @Override public ComponentMode getRunningMode() {
    return mode;
  }

  @Override public void setRunningMode(ComponentMode mode) {
    this.mode = mode;
  }

  @Override public ComponentState getState() {
    return state;
  }

  @Override public String getId() {
    return id;
  }

  @Override public void setId(String id) {
    this.id = id;
  }

  @Override public String getComponentType() {
    return componentType;
  }

  @Override public void setStatus(ComponentState state) {
    ComponentState oldState = this.state;
    ComponentState newState = state;
    if (state == this.state)
      return;
    switch (this.state) {
      case FAILED:
        log.error("Component " + componentType + "." + id + " has failed");
        break;
      case IDLE:
        break;
      case INITIALIZING:
        if (!(state == ComponentState.INITIALIZED || state == ComponentState.RUNNING)) {
          log.error(getId() + " Cannot Go from external resource from " + this.state + "->" + state);
        }
        break;
      case STARTING:
      case INITIALIZED:
        if (state != ComponentState.RUNNING)
          log.error(getId() + " Cannot Go from external resource from " + this.state + "->" + state);
        break;
      case RESETTING:
        if (state != ComponentState.STOPPED) {
          log.error(getId() + " Cannot Go from external resource from " + this.state + "->" + state);
        } else {
          undeployAllModules();
          setup(config);
        }
        break;
      case RUNNING:
      case STOPPING:
        if (!(state == ComponentState.STOPPED || state == ComponentState.KILLED)) {
          log.error(getId() + " Cannot Go from external resource from " + this.state + "->" + state);
        }
        break;
      case STOPPED:
        log.error(getId() + " Cannot Go from external resource from " + this.state + "->" + state);
        break;
      case KILLED:
        log.error("Cannot Go from external resource from " + this.state + "->" + state);
        break;
    }
    this.state = newState;
  }


}
