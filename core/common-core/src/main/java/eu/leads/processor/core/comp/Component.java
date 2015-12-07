package eu.leads.processor.core.comp;

import org.vertx.java.core.json.JsonObject;

/**
 * Created by vagvaz on 7/13/14.
 */
public interface Component {

  public void setup(JsonObject conf);

  public void startUp();

  public void stopComponent();

  public void shutdown();

  public void reset(JsonObject conf);

  public void cleanup();

  public void kill();

  public ComponentMode getRunningMode();

  public void setRunningMode(ComponentMode mode);

  public ComponentState getState();

  public String getId();

  public void setId(String id);

  public String getComponentType();

  void setStatus(ComponentState initialized);
}
