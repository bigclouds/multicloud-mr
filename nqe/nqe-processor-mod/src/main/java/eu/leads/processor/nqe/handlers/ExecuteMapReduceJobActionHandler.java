package eu.leads.processor.nqe.handlers;

import eu.leads.processor.common.StringConstants;
import eu.leads.processor.common.infinispan.InfinispanManager;
import eu.leads.processor.common.utils.storage.LeadsStorage;
import eu.leads.processor.core.Action;
import eu.leads.processor.core.ActionHandler;
import eu.leads.processor.core.ActionStatus;
import eu.leads.processor.core.comp.LogProxy;
import eu.leads.processor.core.net.Node;
import eu.leads.processor.core.plan.QueryState;
import eu.leads.processor.core.plan.QueryStatus;
import eu.leads.processor.infinispan.operators.GenericMapReduceOperator;
import eu.leads.processor.infinispan.operators.Operator;
import eu.leads.processor.nqe.NQEConstants;

import org.infinispan.Cache;
import org.vertx.java.core.json.JsonObject;

import java.util.UUID;

/**
 * Created by Apostolos Nydriotis on 2015/06/22.
 */
public class ExecuteMapReduceJobActionHandler implements ActionHandler {

  private final Node com;
  private final LogProxy log;
  private final InfinispanManager persistence;
  private final String id;
  private Cache jobsCache;
  private LeadsStorage storage;
  public ExecuteMapReduceJobActionHandler(Node com, LogProxy log, InfinispanManager persistence,
                                          String id,LeadsStorage storage) {
    this.com = com;
    this.log = log;
    this.persistence = persistence;
    this.id = id;
    jobsCache = (Cache) persistence.getPersisentCache(StringConstants.QUERIESCACHE);
    this.storage = storage;
  }

  @Override
  public Action process(Action action) {
    Action result = new Action(action);
    result.setLabel(NQEConstants.EXECUTE_MAP_REDUCE_JOB);
    result.getData().putString("owner", id);
    String jobId = UUID.randomUUID().toString();
    result.getData().getObject("operator").putString("id", jobId);
    QueryStatus queryStatus = new QueryStatus();
    queryStatus.setId(jobId);
    queryStatus.setStatus(QueryState.PENDING);
    JsonObject wrapper = new JsonObject();
    wrapper.putObject("status",queryStatus.asJsonObject());
    jobsCache.put(jobId, wrapper.toString());
    result.setResult(queryStatus.asJsonObject());
    result.setStatus(ActionStatus.COMPLETED.toString());

    // Maybe use OperatorFactory (and encompass MapReduceOperatorFactory's functionality there.
    // - Won't proceed with this (at least for now)as OperatorFactory uses "operatorType" as a flag
    //   while MapReduceOperatorFactory uses job's "name"
    Operator operator = MapReduceOperatorFactory.createOperator(com, persistence, log, result);
    if(operator instanceof GenericMapReduceOperator){
      GenericMapReduceOperator gmr = (GenericMapReduceOperator) operator;
      gmr.setLeadsStorage(storage);
    }
    if (operator != null) {
      operator.init(result.getData());
      operator.execute();
    } else {
      log.error("Could not get a valid operator to execute so operator FAILED");
    }
    return result;
  }
}
