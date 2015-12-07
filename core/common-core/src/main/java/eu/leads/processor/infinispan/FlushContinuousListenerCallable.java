package eu.leads.processor.infinispan;

import eu.leads.processor.common.continuous.BasicContinuousListener;
import org.infinispan.Cache;
import org.slf4j.LoggerFactory;

import java.util.Set;

/**
 * Created by vagvaz on 10/9/15.
 */
public class FlushContinuousListenerCallable extends LeadsBaseCallable {
  public FlushContinuousListenerCallable(String configString, String output) {
    super(configString, output);
  }

  @Override public String call() throws Exception {
    System.out.println("LEADSLOG: Flush leadslistener for " + inputCache.getName());
    profilerLog.error("LEADSLOG: Flush leadslistener for " + inputCache.getName());
    for (Object listener : inputCache.getListeners()) {
      if (listener instanceof BasicContinuousListener) {
        BasicContinuousListener l = (BasicContinuousListener) listener;
        l.flush();
        l.finalizeListener();
        l.close();
      }
    }
    System.out.println("LEADSLOG: Flushed leadslistener for " + inputCache.getName());
    profilerLog.error("LEADSLOG: Flushed leadslistener for " + inputCache.getName());
    //return super.call();
    return inputCache.getCacheManager().getAddress().toString();
  }

  @Override public void setEnvironment(Cache cache, Set inputKeys) {
    inputCache = cache;
    profilerLog = LoggerFactory.getLogger(FlushContinuousListenerCallable.class);
  }

  @Override public void executeOn(Object key, Object value) {

  }
}
