package eu.leads.processor.common;

import eu.leads.processor.common.infinispan.InfinispanManager;
import org.vertx.java.core.json.JsonObject;

import java.io.Serializable;

/**
 * Created by vagvaz on 6/7/14.
 */
public interface LeadsListener extends Serializable {


  /**
   * Getter for property 'manager'.
   *
   * @return Value for property 'manager'.
   */
  public InfinispanManager getManager();

  /**
   * Setter for property 'manager'.
   *
   * @param manager Value to set for property 'manager'.
   */
  public void setManager(InfinispanManager manager);

  public void initialize(InfinispanManager manager, JsonObject conf);

  public void initialize(InfinispanManager manager);

  /**
   * Getter for property 'id'.
   *
   * @return Value for property 'id'.
   */
  public String getId();

  public void close();


  void setConfString(String s);
}
