package eu.leads.processor.infinispan;

import eu.leads.processor.common.LeadsListener;
import eu.leads.processor.common.utils.PrintUtilities;
import eu.leads.processor.conf.LQPConfiguration;
import eu.leads.processor.core.EngineUtils;
import eu.leads.processor.core.LevelDBIndex;
import org.infinispan.Cache;
import org.infinispan.interceptors.locking.ClusteringDependentLogic;

import java.io.Serializable;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.AbstractMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Created by Apostolos Nydriotis on 2015/06/24.
 */
public class LeadsLocalReducerCallable<kOut, vOut> extends LeadsBaseCallable<kOut, Object> implements Serializable {

  private static final long serialVersionUID = 8028728191155715526L;
  private LeadsReducer<kOut, vOut> reducer = null;
  //    private LeadsCollector collector;
  private String prefix;
  private transient LevelDBIndex index;
  private transient LeadsListener leadsListener;
  private transient Iterator<Map.Entry<String, Integer>> iterator;

  public LeadsLocalReducerCallable() {
    super();
  }

  public LeadsLocalReducerCallable(String cacheName, LeadsReducer<kOut, vOut> reducer, String prefix, String site) {
    super("{}", cacheName);
    this.reducer = reducer;
    collector = new LeadsCollector(1000, cacheName);
    collector.setOnMap(true);
    collector.setCombiner(null);
    collector.setUseCombiner(false);
    this.prefix = prefix;
  }

  public String getPrefix() {
    return prefix;
  }

  public void setPrefix(String prefix) {
    this.prefix = prefix;
  }

  public LeadsReducer<kOut, vOut> getReducer() {
    Class<?> reducerClass = reducer.getClass();
    Constructor<?> constructor = null;
    try {
      constructor = reducerClass.getConstructor();
    } catch (NoSuchMethodException e) {
      e.printStackTrace();
    }
    LeadsReducer result = null;
    try {
      result = (LeadsReducer) constructor.newInstance();
    } catch (InstantiationException e) {
      e.printStackTrace();
    } catch (IllegalAccessException e) {
      e.printStackTrace();
    } catch (InvocationTargetException e) {
      e.printStackTrace();
    }
    result.setConfigString(reducer.configString);

    return result;
  }

  public void setReducer(LeadsReducer<kOut, vOut> reducer) {
    this.reducer = reducer;
  }

  @Override public void executeOn(kOut key, Object value) {
    //    LeadsIntermediateIterator<vOut> values = new LeadsIntermediateIterator<>((String) key, prefix,
    //                                                                             imanager);
    //    Iterator<vOut> values = ((List)value).iterator();
    try{
    Iterator<vOut> values = (Iterator<vOut>) value;
    reducer.reduce(key, values, collector);
    }catch (Exception e){
      e.printStackTrace();
      PrintUtilities.logStackTrace(profilerLog, e.getStackTrace());
      PrintUtilities.printAndLog(profilerLog,"Exception in MApper: " + e.getMessage());
    }
  }


  @Override public String call() throws Exception {
    profCallable.end("call");
    if (!isInitialized) {
      initialize();
    }
    profCallable.start("Call getComponent ()");
    final ClusteringDependentLogic cdl =
        inputCache.getAdvancedCache().getComponentRegistry().getComponent(ClusteringDependentLogic.class);
    profCallable.end();
    profCallable.start("InitIndex");
    try {
      for (ExecuteRunnable runnable : executeRunnables) {
        EngineUtils.submit(runnable);
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
    EngineUtils.waitForAllExecute();
    for (LeadsBaseCallable callable : callables) {
      callable.finalizeCallable();
      System.err.println("--- reducelocal callable finalized " + callable.getCallableIndex());

    }
    profCallable.end();
    //        finalizeCallable();
    callables.clear();
    executeRunnables.clear();
    return embeddedCacheManager.getAddress().toString();
  }


  @Override public void initialize() {
    super.initialize();
    collector.setOnMap(true);
    collector.setEmanager(emanager);
    collector.setManager(embeddedCacheManager);
    collector.setIsReduceLocal(true);
    collector.setCombiner(null);
    collector.setUseCombiner(false);
    collector.setSite(LQPConfiguration.getInstance().getMicroClusterName());
    collector.initializeCache(callableIndex+":"+inputCache.getName(), imanager);
    this.reducer.initialize();
    Cache dataCache = inputCache.getCacheManager().getCache(prefix + ".data");

    index = null;
    //        EnsembleCacheUtils.waitForAllPuts();
    for (Object listener : dataCache.getListeners()) {
      if (listener instanceof LocalIndexListener) {
        System.err.println("listener class is " + listener.getClass().toString());
        LocalIndexListener localIndexListener = (LocalIndexListener) listener;
        leadsListener = localIndexListener;
        System.err.println("WaitForAllData");
        localIndexListener.waitForAllData();

        System.err.println("getIndex");
        index = localIndexListener.getIndex(callableIndex);
        //                index.flush();
        break;
      }
    }
    if (index == null) {
      System.err.println("\n\n\n\n\n\nIndex was not installed serious...\n\n\n\n\n\n");
      profilerLog.error("\n\n\n\n\n\nIndex was not installed serious...\n\n\n\n\n\n");
      setContinueRunning(false);
      return;
    }
    iterator = index.getKeysIterator().iterator();
  }


  @Override public synchronized Map.Entry poll() {
    if (index == null || iterator == null) {
      continueRunning = false;
      return null;
    }
//    System.out.println(" POLL NEXT");
    Map.Entry result = null;
    if (iterator.hasNext()) {
      Map.Entry<String, Integer> entry = iterator.next();
      result = new AbstractMap.SimpleEntry(entry.getKey(), index.getKeyIterator(entry.getKey(), entry.getValue()));
    } else {
      iterator = null;
      result = null;
      continueRunning = false;
    }
    return result;
  }

  public boolean isEmpty() {
    return ((iterator != null) ? !iterator.hasNext() : true);
  }

  @Override public void finalizeCallable() {
    System.err.println("finalize collector in reduce callable");
    System.err.println("finalize reducelocalabe task");
    if(leadsListener != null){
      leadsListener.close();
    }
    if(index != null)
      index.close();
    reducer.finalizeTask();
    collector.finalizeCollector();
    System.err.println("finalize base");
    super.finalizeCallable();
  }
}
