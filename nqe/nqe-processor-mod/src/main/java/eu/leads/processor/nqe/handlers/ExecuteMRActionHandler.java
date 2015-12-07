package eu.leads.processor.nqe.handlers;

import eu.leads.processor.common.infinispan.InfinispanManager;
import eu.leads.processor.core.Action;
import eu.leads.processor.core.ActionHandler;
import eu.leads.processor.core.comp.LogProxy;
import eu.leads.processor.core.net.Node;
import org.vertx.java.core.json.JsonObject;

/**
 * Created by vagvaz on 4/2/15.
 */
public class ExecuteMRActionHandler implements ActionHandler {
   private final Node com;
   private final LogProxy log;
   private final InfinispanManager persistence;
   private final String id;

   public ExecuteMRActionHandler(Node com, LogProxy log, InfinispanManager persistence, String id) {
      this.com = com;
      this.log = log;
      this.persistence = persistence;
      this.id = id;
   }

   @Override
   public Action process(Action action) {
      Action result = new Action(action);
      JsonObject actionResult = new JsonObject();
      JsonObject statusResult = new JsonObject();

      try {
         JsonObject data = action.getData();

         statusResult.putString("status","SUCCESS");
         statusResult.putString("message","");
         actionResult.putObject("status", statusResult);
         actionResult.putObject("result",data);


         if (!(actionResult == null || actionResult.equals(""))) {
            //               com.sendTo(from, result.getObject("result"));
            result.setResult(actionResult);
         } else {
            actionResult.putString("error", "");
            result.setResult(actionResult);
         }
      } catch (Exception e) {
//            e.printStackTrace();
         JsonObject object = new JsonObject();
         object.putString("error",e.getMessage());
         result.setResult(object);
      }
      return result;
   }
}
