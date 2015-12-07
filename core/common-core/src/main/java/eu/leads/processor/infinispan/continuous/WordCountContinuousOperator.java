package eu.leads.processor.infinispan.continuous;

import eu.leads.processor.infinispan.LeadsMapper;
import eu.leads.processor.infinispan.LeadsReducer;
import eu.leads.processor.infinispan.operators.mapreduce.WordCountMapper;
import eu.leads.processor.infinispan.operators.mapreduce.WordCountReducer;

import org.vertx.java.core.json.JsonObject;

/**
 * Created by vagvaz on 10/6/15.
 */
public class WordCountContinuousOperator extends MapReduceContinuousOperator {
  @Override protected LeadsReducer getReducer() {
    JsonObject newConf = conf.copy()
        .putString("composable", "1");
    // both (continues or not) and (global or local)

    return new WordCountReducer(newConf.toString());
  }

  @Override protected LeadsReducer getLocalReducer() {
    JsonObject newConf = conf.copy()
        .putString("composable", "1")
        .putString("local", "1");
    return new WordCountReducer(newConf.toString());
  }

  @Override protected LeadsMapper getMapper() {
    return new WordCountMapper(conf.toString());
  }

}
