package eu.leads.processor.nqe;

import eu.leads.processor.common.StringConstants;
import eu.leads.processor.core.comp.ComponentControlVerticle;
import org.vertx.java.core.json.JsonObject;

/**
 * edited by KRambo on 29/8/14.
 */
public class NQEControlVerticle extends ComponentControlVerticle {

  protected final String componentType = "nqe";
  private String deployerQueue;
  private String nqeQueue;

  @Override public void start() {
    deployerQueue = StringConstants.PLANNERQUEUE;
    nqeQueue = StringConstants.NODEEXECUTORQUEUE;
    super.start();
    setup(container.config());
    startUp();
  }

  @Override public void setup(JsonObject conf) {
    super.setup(conf);
    this.logicConfig.putString("nqe", nqeQueue);
    this.processorConfig.putString("planner", deployerQueue);
    //this.logicConfig.putString("nqe",nqeQueue);
  }

  @Override public void startUp() {
    super.startUp();
  }

  @Override public void stopComponent() {
    super.stopComponent();
  }

  @Override public void shutdown() {
    super.shutdown();
  }

  @Override public void undeployAllModules() {
    super.undeployAllModules();
  }

  @Override public void reset(JsonObject conf) {
    super.reset(conf);
  }

  @Override public void cleanup() {
    super.cleanup();
  }

  @Override public void kill() {
    super.kill();
  }

  @Override public String getComponentType() {
    return componentType;
  }
}
