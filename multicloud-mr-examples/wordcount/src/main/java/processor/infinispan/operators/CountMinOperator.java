package eu.leads.processor.infinispan.operators;

import eu.leads.processor.common.infinispan.InfinispanManager;
import eu.leads.processor.core.Action;
import eu.leads.processor.core.comp.LogProxy;
import eu.leads.processor.core.net.Node;
import eu.leads.processor.infinispan.continuous.CountMinOperatorContinuous;
import eu.leads.processor.infinispan.operators.mapreduce.CountMinCombiner;
import eu.leads.processor.infinispan.operators.mapreduce.CountMinFederationReducer;
import eu.leads.processor.infinispan.operators.mapreduce.CountMinLocalReducer;
import eu.leads.processor.infinispan.operators.mapreduce.CountMinMapper;
import org.vertx.java.core.json.JsonObject;

/**
 * Created by Apostolos Nydriotis on 2015/07/05.
 */
public class CountMinOperator extends MapReduceOperator {

  public CountMinOperator(Node com, InfinispanManager persistence, LogProxy log, Action action) {
    super(com, persistence, log, action);
  }

  @Override public void init(JsonObject config) {
    super.init(conf);
    setMapper(new CountMinMapper(conf.toString()));
    setCombiner(new CountMinCombiner(conf.toString()));
    setFederationReducer(new CountMinFederationReducer(conf.toString()));
    init_statistics(this.getClass().getCanonicalName());
  }

  @Override public String getContinuousListenerClass() {
    return CountMinOperatorContinuous.class.getCanonicalName().toString();
  }

  @Override public void setupMapCallable() {
    //      init(conf);
    setMapper(new CountMinMapper(conf.toString()));
    super.setupMapCallable();
  }

  @Override public void setupReduceLocalCallable() {
    setLocalReducer(new CountMinLocalReducer(conf.toString()));
    super.setupReduceLocalCallable();
  }

  @Override public void setupReduceCallable() {
    setFederationReducer(new CountMinFederationReducer(conf.toString()));
    super.setupReduceCallable();
  }
}
