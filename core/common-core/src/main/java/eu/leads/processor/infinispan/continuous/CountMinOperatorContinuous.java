package eu.leads.processor.infinispan.continuous;

import eu.leads.processor.infinispan.LeadsMapper;
import eu.leads.processor.infinispan.LeadsReducer;
import eu.leads.processor.infinispan.operators.mapreduce.CountMinFederationReducer;
import eu.leads.processor.infinispan.operators.mapreduce.CountMinLocalReducer;
import eu.leads.processor.infinispan.operators.mapreduce.CountMinMapper;

import org.vertx.java.core.json.JsonObject;

/**
 * Created by vagvaz on 10/12/15.
 */
public class CountMinOperatorContinuous extends MapReduceContinuousOperator {
  @Override protected LeadsReducer getReducer() {
    JsonObject newConf = conf.copy()
        .putString("composable", "1");
    return new CountMinFederationReducer(newConf.toString());
  }

  @Override protected LeadsReducer getLocalReducer() {
    JsonObject newConf = conf.copy()
        .putString("composable", "1");
    return new CountMinLocalReducer(newConf.toString());
  }

  @Override protected LeadsMapper getMapper() {
    return new CountMinMapper(conf.toString());
  }
}
