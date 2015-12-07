package eu.leads.processor.infinispan.operators;

import eu.leads.processor.common.infinispan.InfinispanManager;
import eu.leads.processor.core.Action;
import eu.leads.processor.core.comp.LogProxy;
import eu.leads.processor.core.net.Node;
import eu.leads.processor.infinispan.LeadsCombiner;
import eu.leads.processor.infinispan.LeadsReducer;
import eu.leads.processor.infinispan.continuous.WordCountContinuousOperator;
import eu.leads.processor.infinispan.operators.mapreduce.WordCountMapper;
import eu.leads.processor.infinispan.operators.mapreduce.WordCountReducer;
import org.vertx.java.core.json.JsonObject;

/**
 * Created by Apostolos Nydriotis on 2015/06/23.
 */
public class WordCountOperator extends MapReduceOperator {

  LeadsReducer<?, ?> wordCountLocalReducer;
  LeadsReducer<?, ?> wordCountFederationReducer;

  public WordCountOperator(Node com, InfinispanManager persistence, LogProxy log, Action action) {
    super(com, persistence, log, action);
  }

  @Override public void init(JsonObject config) {
    super.init(conf);
    setMapper(new WordCountMapper(conf.toString()));

    //create local and federation reducer
    wordCountFederationReducer = new WordCountReducer(conf.toString());
    JsonObject localReducerConf = conf.copy().putString("local", "1");
    wordCountLocalReducer = new WordCountReducer(localReducerConf.toString());

    init_statistics(this.getClass().getCanonicalName());
  }

  @Override public String getContinuousListenerClass() {
    return WordCountContinuousOperator.class.getCanonicalName().toString();
  }

  @Override public void setupMapCallable() {
    //      init(conf);
    LeadsCombiner wordCountCombiner = new WordCountReducer(conf.toString());
    setCombiner(wordCountCombiner);
    setMapper(new WordCountMapper(conf.toString()));
    super.setupMapCallable();
  }

  @Override public void setupReduceLocalCallable() {
    setLocalReducer(wordCountLocalReducer);
    super.setupReduceLocalCallable();
  }

  @Override public void setupReduceCallable() {
    setFederationReducer(wordCountFederationReducer);
    super.setupReduceCallable();
  }



}
