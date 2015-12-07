package eu.leads.processor.infinispan;

import eu.leads.processor.common.LeadsListener;
import eu.leads.processor.common.utils.FSUtilities;
import eu.leads.processor.common.utils.PrintUtilities;
import eu.leads.processor.common.utils.storage.LeadsStorage;
import eu.leads.processor.common.utils.storage.LeadsStorageFactory;
import eu.leads.processor.conf.ConfigurationUtilities;
import eu.leads.processor.core.EngineUtils;
import eu.leads.processor.core.LevelDBIndex;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.XMLConfiguration;
import org.infinispan.Cache;
import org.infinispan.interceptors.locking.ClusteringDependentLogic;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vertx.java.core.json.JsonObject;

import java.io.File;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.AbstractMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;

/**
 * Created by vagvaz on 2/19/15.
 */
public class GenericReducerCallable<K, V> extends LeadsBaseCallable<K, Object> {
  private static final long serialVersionUID = 3724554288677416503L;

  private String tmpdirPrefix = "/tmp/leads/processor/tmp/";
  transient private LeadsReducer reducer;
  transient LeadsStorage storageLayer;
  transient private Logger log = null;
  private String prefix;
  //    private transient LevelDBIndex index;
  private transient LevelDBIndex index;
  private transient LeadsListener leadsListener;
  private transient Iterator<Map.Entry<String, Integer>> iterator;
  private transient MapReduceJob job;
  private String jobConfigString;
  private String storageType;
  private Properties storageConfiguration;
  public GenericReducerCallable() {
    super();
  }

  public GenericReducerCallable(String cacheName, String prefix, JsonObject jsonObject,String storageType,Properties storageConfiguration) {
    super("{}", cacheName);
    this.jobConfigString = jsonObject.toString();
    this.storageType = storageType;
    this.storageConfiguration = storageConfiguration;
    collector = new LeadsCollector(1000, cacheName);
    collector.setOnMap(false);
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

  public LeadsReducer<K, V> getReducer() {
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
    result.setConfigString(reducer.getConfigString());

    return result;
  }

  public void setReducer(LeadsReducer<K, V> reducer) {
    this.reducer = reducer;
  }

  @Override public void executeOn(K key, Object value) {
    //        LeadsIntermediateIterator<vOut> values = new LeadsIntermediateIterator<>((String) key,prefix,imanager);
    try {
      Iterator<V> values = (Iterator<V>) value;//((List)value).iterator();
      reducer.reduce(key, values, collector);
    }catch (Exception e){
      e.printStackTrace();
      PrintUtilities.logStackTrace(profilerLog,e.getStackTrace());
      PrintUtilities.printAndLog(profilerLog, "Exception in Reducer: " + e.getMessage());
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
      System.err.println("---reducer callable finalized " + callable.getCallableIndex());

    }
    profCallable.end();
    callables.clear();
    executeRunnables.clear();
    //        finalizeCallable();
    return embeddedCacheManager.getAddress().toString();
  }

  @Override public void initialize() {
    //Call super initialization
    storageLayer = LeadsStorageFactory.getInitializedStorage(storageType,storageConfiguration);
    super.initialize();
    collector.setOnMap(false);
    collector.setEmanager(emanager);
    collector.setCombiner(null);
    collector.setUseCombiner(false);
    collector.initializeCache(callableIndex+":"+inputCache.getName(), imanager);
    log = LoggerFactory.getLogger(GenericReducerCallable.class);
    job = new MapReduceJob(jobConfigString);
    //download mapper from storage layer
    //instatiate and initialize with the given configuration
    String localJarPath = tmpdirPrefix + "/mapreduce/" + inputCache.getName() + "_" + job.getReducerClass();
    storageLayer.download("mapreduce/"+job.getName(), localJarPath);
    reducer = initializeReducer(job.getJarPath(), job.getReducerClass(), job.getConfiguration());
    reducer.initialize();
    //initialize cllector
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

        System.err.println("getIndex " + callableIndex);
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
    System.err.println("Start processing");
    profCallable.end();
    //    collector.setCombiner(combiner);
  }

  private LeadsReducer initializeReducer(String localReduceJarPath, String reducerClassName, JsonObject reducerConfig) {
    LeadsReducer result = null;
    //Get UrlClassLoader
    File file = new File(localReduceJarPath);
    URLClassLoader classLoader = null;
    try {
      classLoader = new URLClassLoader(new URL[] {file.toURI().toURL()},GenericLocalReducerCallable.class.getClassLoader());
    } catch (MalformedURLException e) {
      e.printStackTrace();
    }
    try {
      Class<?> reducerClass =
          Class.forName(reducerClassName, true, classLoader);
      Constructor<?> con = reducerClass.getConstructor();
      result = (LeadsReducer) con.newInstance();
      result.setConfigString(reducerConfig.toString());
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
    AbstractMap.SimpleEntry result = null;
    //        synchronized (iterator) {
    if (index == null || iterator == null) {
      continueRunning = false;
      return null;
    }
    if (iterator.hasNext()) {
      Map.Entry<String, Integer> entry = iterator.next();
      result = new AbstractMap.SimpleEntry(entry.getKey(), index.getKeyIterator(entry.getKey(), entry.getValue()));
    } else {
      continueRunning = false;
      iterator = null;
      result = null;
    }

    //        }
    return result;
  }

  public boolean isEmpty() {
    return (iterator != null) ? !iterator.hasNext() : true;
  }

  @Override public void finalizeCallable() {
    System.err.println("reduce finalize reducer");
    reducer.finalizeTask();
    if(index != null)
      index.close();
    //        inputCache.removeListener(leadsListener);
    if(leadsListener != null){
      leadsListener.close();
    }

    //    System.err.println("reducer finalizee collector");
    //        collector.finalizeCollector();
    //    System.err.println("finalzie super");
    collector.finalizeCollector();
    super.finalizeCallable();

  }
}
