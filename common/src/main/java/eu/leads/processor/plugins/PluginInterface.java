package eu.leads.processor.plugins;

import eu.leads.processor.common.infinispan.InfinispanManager;
import org.apache.commons.configuration.Configuration;
import org.infinispan.Cache;

/**
 * Created by vagvaz on 6/3/14. The interface that a plugin should implement
 */

public interface PluginInterface {
  /**
   * Get the Unique Id of the plugin
   */
  public String getId();

  /**
   * Setter for property 'id'.
   *
   * @param id Value to set for property 'id'.
   */
  public void setId(String id);

  /**
   * return the Class name usually  MyPlugin.class.getCanonicalName()
   */
  public String getClassName();

  /**
   * This function is called by the system once in order to initialize the plugin
   *
   * @param config  The Hierarchical Configuration for the plugin
   * @param manager The InfinispanManager given to the plugin to getCaches and stuff
   */
  public void initialize(Configuration config, InfinispanManager manager);

  /**
   * This function is called when a plugin is undeployed in order to cleanup (close connections
   * etc)
   */
  public void cleanup();

  /**
   * Function called when a modification on a  key in a cache is performed The function is called
   * after the action
   *
   * @param key   the key of the cache that was modified
   * @param value the value associated with the key that was modified
   * @param cache the cache that was modified
   */
  public void modified(Object key, Object value, Cache<Object, Object> cache);

  /**
   * Function called when a new  key in a cache is created The function is called after the action
   *
   * @param key   the key of the cache that was created
   * @param value the value associated with the key that was created
   * @param cache the cache that the new key was inserted
   */
  public void created(Object key, Object value, Cache<Object, Object> cache);

  /**
   * Function called when a new  key in a cache is removed The function is called after the action
   *
   * @param key   the key of the cache that was removed
   * @param value the value associated with the key that was removed
   * @param cache the cache that the new key was removed
   */
  public void removed(Object key, Object value, Cache<Object, Object> cache);

  /**
   * Getter for property 'configuration'.
   *
   * @return Value for property 'configuration'.
   */
  public Configuration getConfiguration();

  /**
   * Setter for property 'configuration'.
   *
   * @param config Value to set for property 'configuration'.
   */
  public void setConfiguration(Configuration config);
}
