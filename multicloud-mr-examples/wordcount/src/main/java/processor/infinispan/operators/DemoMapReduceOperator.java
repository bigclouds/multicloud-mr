package eu.leads.processor.infinispan.operators;

import eu.leads.processor.common.infinispan.InfinispanManager;
import eu.leads.processor.core.Action;
import eu.leads.processor.core.comp.LogProxy;
import eu.leads.processor.core.net.Node;
import eu.leads.processor.infinispan.operators.mapreduce.TransformMapper;
import eu.leads.processor.infinispan.operators.mapreduce.TransformReducer;
import org.vertx.java.core.json.JsonObject;

/**
 * Created by vagvaz on 11/21/14.
 */
public class DemoMapReduceOperator extends MapReduceOperator {
  public DemoMapReduceOperator(Node com, InfinispanManager persistence, LogProxy log, Action action) {
    super(com, persistence, log, action);
  }

  @Override public void init(JsonObject config) {
    super.init(conf);
    setMapper(new TransformMapper(conf.toString()));
    setReducer(new TransformReducer(conf.toString()));
    init_statistics(this.getClass().getCanonicalName());
  }

  @Override public void execute() {
    super.execute();
  }

  @Override public void cleanup() {
    super.cleanup();
  }

  @Override public String getContinuousListenerClass() {
    return null;
  }
}
