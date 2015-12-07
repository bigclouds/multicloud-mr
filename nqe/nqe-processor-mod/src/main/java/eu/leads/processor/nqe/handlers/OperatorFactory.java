package eu.leads.processor.nqe.handlers;

import eu.leads.processor.common.infinispan.InfinispanManager;
import eu.leads.processor.core.Action;
import eu.leads.processor.core.comp.LogProxy;
import eu.leads.processor.core.net.Node;
import eu.leads.processor.infinispan.operators.Operator;

import org.vertx.java.core.json.JsonObject;

/**
 * Created by vagvaz on 9/23/14.
 */
public class OperatorFactory {

  public static Operator createOperator(Node com, InfinispanManager persistence, LogProxy log,
                                        Action action) {
    Operator result = null;
    try {
      JsonObject actionData = action.getData();
      // read monitor q.getString("monitor");
      String operatorType = actionData.getString("operatorType");
//      if (operatorType.equals(LeadsNodeType.WGS_URL.toString())) {//SQL Query
//        result = new WGSOperator(com, persistence, log, action);

//      } else if (operatorType.equals(LeadsNodeType.EPQ.toString())) {
//            result = new SSEPointQueryOperator(com,persistence,log,action);
//      } else {
        //SQL Operators
//        result = SQLOperatorFactory.getOperator(com, persistence, log, action);
//      }

    } catch (Exception e) {
      log.error(e.getMessage());
    }
    return result;
  }
}
