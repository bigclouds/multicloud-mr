package eu.leads.processor.infinispan.continuous;

import eu.leads.processor.conf.LQPConfiguration;
import eu.leads.processor.infinispan.*;

/**
 * Created by vagvaz on 10/6/15.
 */
public abstract class MapReduceContinuousOperator extends BasicContinuousOperator {
  LeadsMapper mapper;
  LeadsReducer reducer;
  LeadsReducer localReducer;
  LeadsCombiner combiner;

  @Override protected LeadsBaseCallable getCallableInstance(boolean isReduce, boolean islocal) {
    LeadsBaseCallable result = null;
    if (isReduce) {
      if (islocal) {
        result = new LeadsLocalReducerCallable(inputCache.getName(), getLocalReducer(), "",
            LQPConfiguration.getInstance().getMicroClusterName());
      } else {
        result = new LeadsReducerCallable(inputCache.getName(), getReducer(), "");
      }
    } else {
      result = new LeadsMapperCallable(inputCache, collector, getMapper(),
          LQPConfiguration.getInstance().getMicroClusterName());
    }
    return result;
  }

  protected abstract LeadsReducer getReducer();

  protected abstract LeadsReducer getLocalReducer();

  protected abstract LeadsMapper getMapper();
}
