package eu.leads.processor.infinispan;

import eu.leads.processor.common.continuous.BasicContinuousListener;
import eu.leads.processor.common.infinispan.InfinispanManager;
import eu.leads.processor.math.FilterOperatorTree;
import org.vertx.java.core.json.JsonObject;

/**
 * Created by vagvaz on 9/25/15.
 */
public class ScanCQLListener extends BasicContinuousListener {
  private transient FilterOperatorTree tree;

  public ScanCQLListener(JsonObject conf) {

  }

  @Override public InfinispanManager getManager() {
    return super.getManager();
  }

  @Override public void setManager(InfinispanManager manager) {
    super.setManager(manager);
  }

  @Override public void initialize(InfinispanManager manager, JsonObject conf) {
    super.initialize(manager, conf);
  }

  @Override protected void initializeContinuousListener(JsonObject conf) {

  }

  @Override protected void processBuffer() {

  }

  @Override public void finalizeListener() {

  }

  @Override public void initialize(InfinispanManager manager) {
    super.setManager(manager);
  }

  @Override public String getId() {
    return ScanCQLListener.class.toString();
  }

  @Override public void close() {
    super.close();
  }

}
