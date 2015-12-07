package eu.leads.processor.infinispan;

import eu.leads.processor.common.LeadsListener;
import eu.leads.processor.common.infinispan.InfinispanManager;
import org.vertx.java.core.json.JsonObject;

/**
 * Created by vagvaz on 9/25/15.
 */
public class TopkSecondStageListener implements LeadsListener {
  public TopkSecondStageListener(JsonObject conf) {

  }

  @Override public InfinispanManager getManager() {
    return null;
  }

  @Override public void setManager(InfinispanManager manager) {

  }

  @Override public void initialize(InfinispanManager manager, JsonObject conf) {

  }

  @Override public void initialize(InfinispanManager manager) {

  }

  @Override public String getId() {
    return TopkSecondStageListener.class.toString();
  }

  @Override public void close() {

  }

  @Override public void setConfString(String s) {

  }
}
