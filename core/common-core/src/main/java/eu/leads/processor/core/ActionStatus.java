package eu.leads.processor.core;

/**
 * Created by vagvaz on 8/6/14.
 */
public enum ActionStatus {
  PENDING, INPROCESS, COMPLETED, FAILED //FAIL is about the processing of the action itself not failuers/errors inside the
  //the execution. For example when creating a new query if the query is not created
  //then the action iscompleted and not failed. But if we discover an action that it
  // it should be processed and is not then the action is failed.
}
