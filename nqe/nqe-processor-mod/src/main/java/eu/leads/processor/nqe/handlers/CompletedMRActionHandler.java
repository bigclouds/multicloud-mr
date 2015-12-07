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
public class CompletedMRActionHandler implements ActionHandler {
   private final Node com;
   private final LogProxy log;
   private final InfinispanManager persistence;
   private final String id;
   public CompletedMRActionHandler(Node com, LogProxy log, InfinispanManager persistence, String id) {
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
      JsonObject resultObject = new JsonObject();
      String deployerId = "";
      try {
         JsonObject data = action.getData();
//         deployerId  = data.getString("coordinator");
         statusResult.putString("status","SUCCESS");
         statusResult.putString("message","");
         actionResult.putObject("status", statusResult);
//         actionResult.putString("deployerID", deployerId);
         actionResult.putObject("result",data);
         actionResult.putString("replyGroup", action.getData().getObject("data").getString("replyGroup"));
         actionResult.putString("microcloud",action.getData().getObject("data").getString("microcloud"));
         actionResult.getObject("result").putString("microcloud",action.getData().getObject("data").getString("microcloud"));
         actionResult.getObject("result").putString("STATUS",action.getData().getObject("data").getString("STATUS"));

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
