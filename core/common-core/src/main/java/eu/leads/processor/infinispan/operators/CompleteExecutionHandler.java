package eu.leads.processor.infinispan.operators;

import eu.leads.processor.core.Action;
import eu.leads.processor.core.comp.LeadsMessageHandler;
import eu.leads.processor.core.net.Node;
import org.vertx.java.core.json.JsonObject;

/**
 * Created by vagvaz on 4/16/15.
 */
public class CompleteExecutionHandler implements LeadsMessageHandler {

  private Node com;
  private BasicOperator operator;

  public CompleteExecutionHandler(Node com, BasicOperator operator) {
    this.com = com;
    this.operator = operator;
  }

  @Override public void handle(JsonObject message) {
    Action action = new Action(message);
    JsonObject data = action.getData();
    //      if(!data.containsField("microcloud"))
    //      {
    //         if(data.containsField("data")){
    //
    //         }
    //         operator.addResult(action.asJsonObject().getString("microcloud"), action.asJsonObject().getString("STATUS"));
    //      }else {
    operator.addResult(data.getString("microcloud"), data.getString("STATUS"));
    //      }
    //      System.out.println("received result " + data.encodePrettily());
    operator.signal();
  }
}
