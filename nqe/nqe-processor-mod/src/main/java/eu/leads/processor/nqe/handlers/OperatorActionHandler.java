package eu.leads.processor.nqe.handlers;

import eu.leads.processor.common.infinispan.InfinispanManager;
import eu.leads.processor.core.Action;
import eu.leads.processor.core.ActionHandler;
import eu.leads.processor.core.ActionStatus;
import eu.leads.processor.core.comp.LogProxy;
import eu.leads.processor.core.net.Node;
import eu.leads.processor.infinispan.operators.Operator;
import eu.leads.processor.nqe.NQEConstants;

import org.infinispan.Cache;

import java.util.List;

/**
 * Created by vagvaz on 8/6/14.
 */
public class OperatorActionHandler implements ActionHandler {

  private final Node com;
  private final LogProxy log;
  private final InfinispanManager persistence;
  private final String id;


  private String textFile;
  private transient Cache<?, ?> InCache;
  private transient Cache<?, List<?>> CollectorCache;
  private transient Cache<?, ?> OutCache;

  public OperatorActionHandler(Node com, LogProxy log, InfinispanManager persistence, String id) {
    this.com = com;
    this.log = log;
    this.persistence = persistence;
    this.id = id;
  }

  @Override
  public Action process(Action action) {
    Action result = action;
    result.getData().putString("owner", id);
    Action ownerAction = new Action(result.asJsonObject().copy());
    ownerAction.setLabel(NQEConstants.OPERATOR_OWNER);
    ownerAction.setStatus(ActionStatus.INPROCESS.toString());
    com.sendTo(action.getData().getString("monitor"), ownerAction.asJsonObject());
    Operator operator = OperatorFactory.createOperator(com, persistence, log, result);
    if (operator != null) {
      operator.init(result.getData());
      operator.execute();
    } else {
      log.error("Could not get a valid operator to execute so operator FAILED");
      ownerAction.setLabel(NQEConstants.OPERATOR_FAILED);
      com.sendTo(action.getData().getString("monitor"), ownerAction.asJsonObject());
    }

    return result;
  }
}


