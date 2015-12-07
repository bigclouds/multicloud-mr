package eu.leads.processor.infinispan.continuous;

import eu.leads.processor.infinispan.LeadsMapper;
import eu.leads.processor.infinispan.LeadsReducer;
import eu.leads.processor.infinispan.operators.mapreduce.KMeansCombiner;
import eu.leads.processor.infinispan.operators.mapreduce.KMeansMapper;
import eu.leads.processor.infinispan.operators.mapreduce.KMeansReducer;

import org.vertx.java.core.json.JsonObject;

/**
 * Created by vagvaz on 10/12/15.
 */
public class KMeansOperatorContinuous extends MapReduceContinuousOperator {
  @Override protected LeadsReducer getReducer() {
    JsonObject newConf = conf.copy().putString("composable", "1");
    return new KMeansReducer(newConf.toString());
  }

  @Override protected LeadsReducer getLocalReducer() {
    return new KMeansCombiner(conf.toString());
  }

  @Override protected LeadsMapper getMapper() {
    return new KMeansMapper(conf.toString());
  }
}
