package eu.leads.processor.core.comp;

/**
 * Created by vagvaz on 7/28/14.
 * This enum models the possible running status of a service. It is not necessary for each service to cycle through all
 * the possible statuses. The possible values and their meanign follows:
 * IDLE the service waiting, it has never been initialized. The service after leaves IDLE it should never go back.
 * INITIALIZING the service has started the initialization procedure which has not yet completed. The required structures
 * must be created during the initialization of a service.
 * INITIALIZED the service has finished the initialization procedure and can be started/stopped. An initilialized service
 * should only need to deploy the required modules/verticles in order to start working.
 * RUNNING the service has been started and is running, All the required modules have been deployed.
 * STOPPING the service starts undeploying the verticles,modules deployed on start
 * STOPPED The service is inactive it cannot process things. This state should be used when the service should be paused.
 */
public enum ServiceStatus {
  IDLE,
  INITIALIZING,
  INITIALIZED,
  RESETTING,
  RUNNING,
  STOPPING,
  STOPPED,
  FAILED
}

