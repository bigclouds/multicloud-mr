package eu.leads.processor.infinispan;

import eu.leads.processor.common.continuous.BasicContinuousListener;
import eu.leads.processor.common.infinispan.InfinispanManager;
import org.vertx.java.core.json.JsonObject;

/**
 * Created by vagvaz on 9/25/15.
 */
public class TopkFirstStageListener extends BasicContinuousListener {
  public TopkFirstStageListener(JsonObject conf) {

  }

  @Override public InfinispanManager getManager() {
    return null;
  }

  @Override public void setManager(InfinispanManager manager) {

  }

  @Override public void initialize(InfinispanManager manager, JsonObject conf) {

  }

  @Override protected void initializeContinuousListener(JsonObject conf) {

  }

  @Override protected void processBuffer() {

  }

  @Override public void finalizeListener() {

  }


  @Override public void initialize(InfinispanManager manager) {

  }

  @Override public String getId() {
    return TopkFirstStageListener.class.toString();
  }

  @Override public void close() {

  }

}
