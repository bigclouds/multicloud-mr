package eu.leads.processor.infinispan.operators;

import eu.leads.processor.common.infinispan.InfinispanManager;
import eu.leads.processor.core.Action;
import eu.leads.processor.core.comp.LogProxy;
import eu.leads.processor.core.net.Node;
import eu.leads.processor.infinispan.LeadsCombiner;
import eu.leads.processor.infinispan.continuous.KMeansOperatorContinuous;
import eu.leads.processor.infinispan.operators.mapreduce.KMeansCombiner;
import eu.leads.processor.infinispan.operators.mapreduce.KMeansMapper;
import eu.leads.processor.infinispan.operators.mapreduce.KMeansReducer;
import org.vertx.java.core.json.JsonObject;

/**
 * Created by Apostolos Nydriotis on 2015/07/10.
 */
public class KMeansOperator extends MapReduceOperator {

  //  LeadsReducer<?, ?> kMeansReducer;  // same for local and federation reducer

  public KMeansOperator(Node com, InfinispanManager persistence, LogProxy log, Action action) {
    super(com, persistence, log, action);
  }

  @Override public void init(JsonObject config) {
    super.init(conf);
    setMapper(new KMeansMapper(conf.toString()));
    //    kMeansReducer = new KMeansReducer(conf.toString());
    setLocalReducer(new KMeansCombiner(conf.toString()));
    setFederationReducer(new KMeansReducer(conf.toString()));
    init_statistics(this.getClass().getCanonicalName());
  }

  @Override public String getContinuousListenerClass() {
    return KMeansOperatorContinuous.class.getCanonicalName().toString();
  }

  @Override public void setupMapCallable() {
    LeadsCombiner kMeansCombiner = new KMeansCombiner(conf.toString());
    setCombiner(kMeansCombiner);
    setMapper(new KMeansMapper(conf.toString()));
    super.setupMapCallable();
  }

  @Override public void setupReduceLocalCallable() {
    setLocalReducer(new KMeansCombiner(conf.toString()));
    super.setupReduceLocalCallable();
  }

  @Override public void setupReduceCallable() {
    setFederationReducer(new KMeansReducer(conf.toString()));
    super.setupReduceCallable();
  }

  public LeadsCombiner<?, ?> getCombiner() {
    return combiner;
  }

  public void setCombiner(LeadsCombiner<?, ?> combiner) {
    this.combiner = combiner;
  }
}
