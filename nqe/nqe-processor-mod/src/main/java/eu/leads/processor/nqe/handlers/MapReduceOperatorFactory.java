package eu.leads.processor.nqe.handlers;

import eu.leads.processor.common.infinispan.InfinispanManager;
import eu.leads.processor.common.utils.storage.LeadsStorage;
import eu.leads.processor.core.Action;
import eu.leads.processor.core.comp.LogProxy;
import eu.leads.processor.core.net.Node;
import eu.leads.processor.infinispan.MapReduceJob;
import eu.leads.processor.infinispan.operators.*;

import org.vertx.java.core.json.JsonObject;

/**
 * Created by Apostolos Nydriotis on 2015/06/23.
 */
public class MapReduceOperatorFactory {
  public static Operator createOperator(Node com, InfinispanManager persistence, LogProxy log,
                                        Action action) {
    JsonObject object = action.getData();
    MapReduceJob job = new MapReduceJob(object);
    String name = job.getName();
    if(job.isBuiltIn()) {
      if (name == null) {
        System.err.println("name == null!");
      } else {
        if (name.equals("wordCount")) {
          return new WordCountOperator(com, persistence, log, action);
        } else if (name.equals("countMin")) {
          return new CountMinOperator(com, persistence, log, action);
        } else if (name.equals("kMeans")) {
          return new KMeansOperator(com, persistence, log, action);
        } else {
          System.err.println("No operator for application \"" + name + "\" found!");
        }
      }
    } else{
      return new GenericMapReduceOperator(com,persistence,log,action,job);
    }
    return null;
  }
}
