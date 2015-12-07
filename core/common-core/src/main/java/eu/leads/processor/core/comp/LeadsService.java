package eu.leads.processor.core.comp;

import org.vertx.java.core.json.JsonObject;

/**
 * Created by vagvaz on 7/17/14.
 */
public interface LeadsService {

  /**
   * Initialize the service with the given configuration. The initialize creates the required structures
   * for the service. It does not start the service. It is good practice to send a message of INITIALIZED to the group
   * in order for parent component process or depenedent services to be informed.
   *
   * @param config The configuration of the Service
   */
  public void initialize(JsonObject config);

  /**
   * The startService deploys the verticles modules used by this service
   */
  public void startService();

  /**
   * The cleanup cleans all the structures created in initialize
   */
  public void cleanup();

  /**
   * Undeploy verticles,modules deployed  during start
   */
  public void stopService();

  /**
   * The fail should call cleanup and stopService. This method is used in order to stop the execution of the service
   * in case of an error.
   *
   * @param message Informative message why the service failed.
   */
  public void fail(String message);

  /**
   * Get the Running status of the service
   *
   * @return the status of the service possible values are   IDLE,INITIALIZING,INITIALIZED,RUNNING,STOPPING,STOPPED,FAILED
   */
  public ServiceStatus getStatus();

  /**
   * Set the Running Status of the service possible values
   * possible values are   IDLE,INITIALIZING,INITIALIZED,RUNNING,STOPPING,STOPPED,FAILED
   * for more information look at {@link ServiceStatus}
   *
   * @param status
   */
  public void setStatus(ServiceStatus status);

  public String getServiceId();

  public String getServiceType();

  public void exitService();
}
