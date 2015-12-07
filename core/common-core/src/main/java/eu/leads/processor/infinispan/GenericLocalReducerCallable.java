package eu.leads.processor.infinispan;

import eu.leads.processor.common.LeadsListener;
import eu.leads.processor.common.utils.PrintUtilities;
import eu.leads.processor.common.utils.storage.LeadsStorage;
import eu.leads.processor.common.utils.storage.LeadsStorageFactory;
import eu.leads.processor.conf.LQPConfiguration;
import eu.leads.processor.core.EngineUtils;
import eu.leads.processor.core.LevelDBIndex;
import eu.leads.processor.core.MapDBIndex;
import org.infinispan.Cache;
import org.infinispan.interceptors.locking.ClusteringDependentLogic;
import org.vertx.java.core.json.JsonObject;

import java.io.File;
import java.io.Serializable;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.AbstractMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;

/**
 * Created by vagvaz on 9/6/15.
 */
public class GenericLocalReducerCallable<kOut, vOut> extends LeadsBaseCallable<kOut, Object> implements Serializable {
  private static final long serialVersionUID = 8028728191155715526L;
  private LeadsReducer<kOut, vOut> reducer = null;
  private String tmpdirPrefix = "/tmp/leads/processor/tmp/";
  private transient LevelDBIndex index;
  private transient LeadsListener leadsListener;
  private transient Iterator<Map.Entry<String, Integer>> iterator;
  private transient LeadsStorage storage;
  private String storageType;
  private Properties storageConfiguration;
  private String prefix;
  private String jobConfigString;
  private transient MapReduceJob job;

  public GenericLocalReducerCallable(String intermediateCacheName, JsonObject jsonObject,
      String intermediateLocalCacheName, String microClusterName,String storageType,Properties storageConfig) {
    super("{}",intermediateCacheName);
    this.storageType = storageType;
    this.storageConfiguration = storageConfig;
    collector = new LeadsCollector(1000, intermediateCacheName);
    collector.setOnMap(true);
    collector.setCombiner(null);
    collector.setUseCombiner(false);
    this.prefix = intermediateLocalCacheName;
    this.setLocalSite(microClusterName);
    this.jobConfigString = jsonObject.toString();
  }

  public GenericLocalReducerCallable(String cacheName, String s,String prefix) {

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
    storage = LeadsStorageFactory.getInitializedStorage(storageType,storageConfiguration);
    collector.setOnMap(true);
    collector.setEmanager(emanager);
    collector.setManager(embeddedCacheManager);
    collector.setIsReduceLocal(true);
    collector.setCombiner(null);
    collector.setUseCombiner(false);
    collector.setSite(LQPConfiguration.getInstance().getMicroClusterName());
    collector.initializeCache(callableIndex+":"+inputCache.getName(), imanager);
    job = new MapReduceJob(jobConfigString);
    String localJarPath = tmpdirPrefix + "/mapreduce/" + inputCache.getName() + "_" + job.getLocalReducerClass();
    storage.download("mapreduce/"+job.getName(),localJarPath);
    this.reducer = initializeReducer(job.getLocalReducerClass(),localJarPath,job.getConfiguration());
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

  private LeadsReducer<kOut, vOut> initializeReducer(String localReducerClass, String localJarPath,
      JsonObject configuration) {
    LeadsReducer result = null;
    //Get UrlClassLoader
    File file = new File(localJarPath);
    URLClassLoader classLoader = null;
    try {
      classLoader = new URLClassLoader(new URL[] {file.toURI().toURL()},GenericLocalReducerCallable.class.getClassLoader());
    } catch (MalformedURLException e) {
      e.printStackTrace();
    }
    try {
      Class<?> reducerClass =
          Class.forName(localReducerClass, true, classLoader);
      Constructor<?> con = reducerClass.getConstructor();
      result = (LeadsReducer) con.newInstance();
      result.setConfigString(configuration.toString());
      result.initialize();
    } catch (ClassNotFoundException e) {
      e.printStackTrace();
    } catch (NoSuchMethodException e) {
      e.printStackTrace();
    } catch (IllegalAccessException e) {
      e.printStackTrace();
    } catch (InstantiationException e) {
      e.printStackTrace();
    } catch (InvocationTargetException e) {
      e.printStackTrace();
    }

    return result;
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
