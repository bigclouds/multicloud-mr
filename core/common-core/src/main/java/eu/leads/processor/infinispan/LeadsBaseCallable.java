package eu.leads.processor.infinispan;

import eu.leads.processor.common.continuous.ConcurrentDiskQueue;
import eu.leads.processor.common.infinispan.BatchPutListener;
import eu.leads.processor.common.infinispan.ClusterInfinispanManager;
import eu.leads.processor.common.infinispan.InfinispanClusterSingleton;
import eu.leads.processor.common.infinispan.InfinispanManager;
import eu.leads.processor.common.utils.PrintUtilities;
import eu.leads.processor.common.utils.ProfileEvent;
import eu.leads.processor.conf.LQPConfiguration;
import eu.leads.processor.core.EngineUtils;
import eu.leads.processor.core.Tuple;
import eu.leads.processor.math.FilterOpType;
import eu.leads.processor.math.FilterOperatorNode;
import eu.leads.processor.math.FilterOperatorTree;
import eu.leads.processor.math.MathUtils;
import org.infinispan.Cache;
import org.infinispan.commons.marshall.Marshaller;
import org.infinispan.commons.util.CloseableIterable;
import org.infinispan.context.Flag;
import org.infinispan.distexec.DistributedCallable;
import org.infinispan.ensemble.EnsembleCacheManager;
import org.infinispan.ensemble.cache.EnsembleCache;
import org.infinispan.factories.ComponentRegistry;
import org.infinispan.filter.KeyValueFilter;
import org.infinispan.interceptors.locking.ClusteringDependentLogic;
import org.infinispan.manager.EmbeddedCacheManager;
import org.infinispan.marshall.core.MarshalledEntryImpl;
import org.infinispan.persistence.leveldb.LevelDBStore;
import org.infinispan.persistence.manager.PersistenceManager;
import org.infinispan.persistence.manager.PersistenceManagerImpl;
import org.infinispan.persistence.spi.InitializationContext;
import org.infinispan.query.SearchManager;
import org.infinispan.query.dsl.FilterConditionContext;
import org.infinispan.query.dsl.FilterConditionEndContext;
import org.infinispan.query.dsl.QueryFactory;
import org.iq80.leveldb.DB;
import org.iq80.leveldb.DBIterator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vertx.java.core.json.JsonObject;

import java.io.Serializable;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.*;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

/**
 * Created by vagvaz on 2/18/15.
 */
public abstract class LeadsBaseCallable<K, V> implements LeadsCallable<K, V>,

    DistributedCallable<K, V, String>, Serializable {
  protected String configString;
  protected String output;
  transient protected JsonObject conf = null;
  transient protected boolean isInitialized = false;
  transient protected EmbeddedCacheManager embeddedCacheManager = null;
  transient protected InfinispanManager imanager = null;
  transient protected Set<K> keys = null;
  transient protected Cache<K, V> inputCache = null;
  transient protected EnsembleCache outputCache = null;
  protected String ensembleHost;
  transient protected Object luceneKeys = null;
  transient protected HashMap<String, Cache> indexCaches = null;
  transient protected FilterOperatorTree tree = null;
  transient protected List<LeadsBaseCallable> callables;
  transient protected List<ExecuteRunnable> executeRunnables;
  transient Queue input;
  protected int callableIndex = -1;
  protected int callableParallelism = 1;
  protected boolean continueRunning = true;
  //  transient protected EnsembleCacheUtilsSingle ensembleCacheUtilsSingle;
  long start = 0;
  long end = 0;
  int readCounter = 0;
  int processed = 0;
  int processThreshold = 1000;
  int readThreshold = 1000;
  //  transient protected RemoteCache outputCache;
  //  transient protected RemoteCache ecache;
  //  transient protected RemoteCacheManager emanager;
  transient protected EnsembleCacheManager emanager;
  transient protected EnsembleCache ecache;
  transient Logger profilerLog;
  protected ProfileEvent profCallable;
  protected LeadsCollector collector;
  private int listSize;
  private int sleepTimeMilis;
  private int sleepTimeNanos;

  public LeadsBaseCallable() {
    callableParallelism = LQPConfiguration.getInstance().getConfiguration().getInt("node.engine.parallelism", 4);
    callableIndex = -2;
  }

  public LeadsBaseCallable(String configString, String output) {
    this.configString = configString;
    this.output = output;
    profilerLog = LoggerFactory.getLogger("###PROF###" + this.getClass().toString());
    profCallable = new ProfileEvent("Callable Construct" + this.getClass().toString(), profilerLog);
    callableIndex = -1;
    callableParallelism = LQPConfiguration.getInstance().getConfiguration().getInt("node.engine.parallelism", 4);
  }

  public LeadsBaseCallable copy() {
    LeadsBaseCallable result = null;
    try {
      Constructor<?> constructor = this.getClass().getConstructor();
      result = (LeadsBaseCallable) constructor.newInstance();
      result.setCollector(new LeadsCollector(collector.getMaxCollectorSize(), collector.getCacheName()));
      result.setEnsembleHost(ensembleHost);
      result.setOutput(output);
      result.setConfigString(configString);
      if (result instanceof LeadsMapperCallable) {
        LeadsMapperCallable mapperCallable = (LeadsMapperCallable) result;
        LeadsMapperCallable thisCallable = (LeadsMapperCallable) this;
        mapperCallable.setSite(thisCallable.getSite());
        LeadsCombiner thisCombiner = thisCallable.getCombiner();
        if ( thisCombiner != null) {
          mapperCallable.setCombiner(thisCombiner);
//          thisCallable.getCollector().setUseCombiner(true);
        }
        mapperCallable.setMapper(thisCallable.getMapper());
      } else if (result instanceof LeadsLocalReducerCallable) {
        LeadsLocalReducerCallable leadsLocalReducerCallable = (LeadsLocalReducerCallable) result;
        LeadsLocalReducerCallable thisCallable = (LeadsLocalReducerCallable) this;
        leadsLocalReducerCallable.setPrefix(thisCallable.getPrefix());
        leadsLocalReducerCallable.setReducer(thisCallable.getReducer());
      } else if (result instanceof LeadsReducerCallable) {
        LeadsReducerCallable leadsReducerCallable = (LeadsReducerCallable) result;
        LeadsReducerCallable thisCallable = (LeadsReducerCallable) this;
        leadsReducerCallable.setPrefix(thisCallable.getPrefix());
        leadsReducerCallable.setReducer(thisCallable.getReducer());
      }
    } catch (NoSuchMethodException e) {
      e.printStackTrace();
    } catch (InvocationTargetException e) {
      e.printStackTrace();
    } catch (InstantiationException e) {
      e.printStackTrace();
    } catch (IllegalAccessException e) {
      e.printStackTrace();
    } catch (Exception e) {
      e.printStackTrace();
    }
    return result;
  }

  public String getEnsembleHost() {
    return ensembleHost;
  }

  public void setEnsembleHost(String ensembleHost) {
    this.ensembleHost = ensembleHost;
  }

  public int getCallableIndex() {
    return callableIndex;
  }

  public void setCallableIndex(int index) {
    callableIndex = index;
  }

  public Queue<Map.Entry<K, V>> getInput() {
    return input;
  }

  public boolean isContinueRunning() {
    return continueRunning;
  }

  public void setContinueRunning(boolean continueRunning) {
    this.continueRunning = continueRunning;
    synchronized (input){
      input.notify();
    }

  }

  @Override public void setEnvironment(Cache<K, V> cache, Set<K> inputKeys) {
    profilerLog = LoggerFactory.getLogger("###PROF###" + this.getClass().toString());
    listSize = LQPConfiguration.getInstance().getConfiguration().getInt("node.list.size",500);
    sleepTimeMilis =
        LQPConfiguration.getInstance().getConfiguration().getInt("node.sleep.time.milis", 0);
    sleepTimeNanos =
        LQPConfiguration.getInstance().getConfiguration().getInt("node.sleep.time.nanos", 10000);
    EngineUtils.initialize();
    PrintUtilities.printAndLog(profilerLog,
        InfinispanClusterSingleton.getInstance().getManager().getMemberName().toString() + ": setupEnvironment");
    if (callableIndex == -1) {

      executeRunnables = new ArrayList<>(callableParallelism);

      callables = new ArrayList<>(callableParallelism);
      for (int i = 0; i < callableParallelism; i++) {
        PrintUtilities.printAndLog(profilerLog,
            InfinispanClusterSingleton.getInstance().getManager().getMemberName().toString() + ": setupEnvironment "
                + i);
        if (i == 0) {
          this.setCallableIndex(0);
          callables.add(this);
          ExecuteRunnable runnable = EngineUtils.getRunnable();
          runnable.setCallable(this);
          executeRunnables.add(runnable);
        } else {
          LeadsBaseCallable newCallable = this.copy();
          PrintUtilities.printAndLog(profilerLog,
              InfinispanClusterSingleton.getInstance().getManager().getMemberName().toString() + ": setupEnvironment "
                  + i + ".0");
          newCallable.setCallableIndex(i);
          newCallable.setEnvironment(cache, inputKeys);
          PrintUtilities.printAndLog(profilerLog,
              InfinispanClusterSingleton.getInstance().getManager().getMemberName().toString() + ": setupEnvironment "
                  + i + ".1");
          callables.add(newCallable);
          ExecuteRunnable runnable = EngineUtils.getRunnable();
          PrintUtilities.printAndLog(profilerLog,
              InfinispanClusterSingleton.getInstance().getManager().getMemberName().toString() + ": setupEnvironment "
                  + i + ".3");
          runnable.setCallable(newCallable);
          executeRunnables.add(runnable);
        }
      }
    }

    profCallable = new ProfileEvent("name", profilerLog);
    profCallable.setProfileLogger(profilerLog);
    if (profCallable != null) {
      profCallable.end("setEnv");
      profCallable.start("setEnvironment Callable ");
    } else
      profCallable = new ProfileEvent("setEnvironment Callable " + this.getClass().toString(), profilerLog);
    embeddedCacheManager = InfinispanClusterSingleton.getInstance().getManager().getCacheManager();
    imanager = new ClusterInfinispanManager(embeddedCacheManager);
    //    outputCache = (Cache) imanager.getPersisentCache(output);
    keys = inputKeys;
    this.inputCache = cache;
    ProfileEvent tmpprofCallable =
        new ProfileEvent("setEnvironment manager " + this.getClass().toString(), profilerLog);
    tmpprofCallable.start("Start LQPConfiguration");

    LQPConfiguration.initialize();
    tmpprofCallable.end();
    //    ensembleCacheUtilsSingle.initialize();
    //    ensembleCacheUtilsSingle = new EnsembleCacheUtilsSingle();
    if (ensembleHost != null && !ensembleHost.equals("")) {
      tmpprofCallable.start("Start EnsemlbeCacheManager");
      profilerLog.error("EnsembleHost EXIST " + ensembleHost);
      System.err.println("EnsembleHost EXIST " + ensembleHost);
      emanager = new EnsembleCacheManager(ensembleHost);
    } else {
      profilerLog.error("EnsembleHost NULL");
      System.err.println("EnsembleHost NULL");
      tmpprofCallable.start("Start EnsemlbeCacheManager");
      emanager = new EnsembleCacheManager(LQPConfiguration.getConf().getString("node.ip") + ":11222");
    }
    emanager.start();

    ecache = emanager.getCache(output, new ArrayList<>(emanager.sites()), EnsembleCacheManager.Consistency.DIST);
    outputCache = ecache;
    input = new LinkedList<>();
    initialize();
    start = System.currentTimeMillis();
    PrintUtilities.printAndLog(profilerLog,
        InfinispanClusterSingleton.getInstance().getManager().getMemberName().toString() + ": setupEnvironment "
            + ".end");
  }


  @Override public String call() throws Exception {

    if (!isInitialized) {
      initialize();
    }

    final ClusteringDependentLogic cdl =
        inputCache.getAdvancedCache().getComponentRegistry().getComponent(ClusteringDependentLogic.class);
    String compressedCacheName = inputCache.getName() + ".compressed";
    if (inputCache.getCacheManager().cacheExists(compressedCacheName)) {
      Cache compressedCache = inputCache.getCacheManager().getCache(compressedCacheName);
      for (Object l : compressedCache.getListeners()) {
        if (l instanceof BatchPutListener) {
          BatchPutListener listener = (BatchPutListener) l;
          listener.waitForPendingPuts();
          break;
        }
      }
    }
    int count = 0;
      System.err.println("Iterate Over Local Data");
  Object filter = new LocalDataFilter<K, V>(cdl);
//
//      CloseableIterable iterable = inputCache.getAdvancedCache().withFlags(Flag.CACHE_MODE_LOCAL)
//          .filterEntries((KeyValueFilter<? super K, ? super V>) filter);
    ComponentRegistry registry = inputCache.getAdvancedCache().getComponentRegistry();
    PersistenceManagerImpl persistenceManager =
        (PersistenceManagerImpl) registry.getComponent(PersistenceManager.class);
    LevelDBStore dbStore = (LevelDBStore) persistenceManager.getAllLoaders().get(0);

    try {
      Field db = dbStore.getClass().getDeclaredField("db");
      db.setAccessible(true);
      DB realDb = (DB) db.get(dbStore);
      DBIterator iterable = realDb.iterator();
      iterable.seekToFirst();
      Field ctxField = dbStore.getClass().getDeclaredField("ctx");
      ctxField.setAccessible(true);
      InitializationContext ctx = (InitializationContext) ctxField.get(dbStore);
      Marshaller m = ctx.getMarshaller();
      try {
        for (ExecuteRunnable runnable : executeRunnables) {
          EngineUtils.submit(runnable);
        }
        int i = 0;
        List<Map.Entry> buffer = new LinkedList<>();
        while (iterable.hasNext()) {

          buffer.clear();
          boolean tocontinue =true;
          for(; tocontinue && buffer.size() < callableParallelism*listSize;){

            Map.Entry<byte[], byte[]> entryIspn = iterable.next();
            String key = (String) m.objectFromByteBuffer(entryIspn.getKey());
            org.infinispan.marshall.core.MarshalledEntryImpl value =
                (MarshalledEntryImpl) m.objectFromByteBuffer(entryIspn.getValue());

          Tuple tuple = (Tuple) m.objectFromByteBuffer(value.getValueBytes().getBuf());
          //        profExecute.end();
          readCounter++;
          if (readCounter > readThreshold) {
            profilerLog.error(callableIndex+" Read: " + readCounter );
            readThreshold *= 1.3;
          }
          Map.Entry<K, V> bufferentry = new AbstractMap.SimpleEntry(key, tuple);
              buffer.add(bufferentry);
            if(buffer.size() != listSize){
              tocontinue = iterable.hasNext();
            }else{
              tocontinue = false;
            }
          }
//          profilerLog.error("Read Buffer " + buffer.size());
//          for (int j = 0; j < callableParallelism; j++) {
//            profilerLog.error(j+": " + j + callables.get(j).getInput().size());
//          }
          for(Map.Entry entry : buffer ) {
            int roundRobinWithoutAddition = 0;
            if (entry.getValue() != null) {
              while (true) {
                //              PrintUtilities.printAndLog(profilerLog, i + ": size " + callables.get(i).getInput().size());
                if (callables.get(i).getSize() <= listSize) {
                  //                PrintUtilities.printAndLog(profilerLog, i + ": chosen " + callables.get(i).getInput().size());
                  callables.get(i).addToInput(entry);
                  i = (i + 1) % callableParallelism;
                  break;
                }
                i = (i + 1) % callableParallelism;
                roundRobinWithoutAddition++;
                if (roundRobinWithoutAddition % callableParallelism == 0) {
//                  PrintUtilities.printAndLog(profilerLog, "Sleeping because everyting full " + roundRobinWithoutAddition);
                  Thread.sleep((roundRobinWithoutAddition/callableParallelism)*sleepTimeMilis, (roundRobinWithoutAddition/callableParallelism)*sleepTimeNanos);
//                  roundRobinWithoutAddition = 0;
                }
                //              i = (i+1)%callableParallelism;
              }
            }
          }

        }
        iterable.close();
      } catch (Exception e) {
        iterable.close();
        if (e instanceof InterruptedException) {
          profilerLog.error(this.imanager.getCacheManager().getAddress().toString() + " was interrupted ");
          for (ExecuteRunnable ex : executeRunnables) {
            if (ex.isRunning()) {
              ex.cancel();
            }
          }
        } else {
          profilerLog.error("Exception in LEADSBASEBACALLABE " + e.getClass().toString());
          PrintUtilities.logStackTrace(profilerLog, e.getStackTrace());
        }
      }
    } catch (NoSuchFieldException e) {
      e.printStackTrace();
    } catch (IllegalAccessException e) {
      e.printStackTrace();
    }
    //    profCallable.end();
    for (LeadsBaseCallable callable : callables) {
      callable.setContinueRunning(false);
    }
    System.err.println("----Engine wait ");
    EngineUtils.waitForAllExecute();
    for (LeadsBaseCallable callable : callables) {
      callable.finalizeCallable();
      System.err.println("Callable finalized " + callable.getCallableIndex());
    }
    callables.clear();
    executeRunnables.clear();
    PrintUtilities.printAndLog(profilerLog,"LAST LINE OF " + this.getClass().toString() + " " + embeddedCacheManager.getAddress().toString()
        + " ----------- END");

    return embeddedCacheManager.getAddress().toString();
  }

  private void addToInput(Map.Entry<K, V> entry) {
    //    synchronized (input){
    synchronized (input){
    input.add(entry);
//    while (input.size() >= listSize) {
//      try {
//        Thread.sleep(0,10000);
//      } catch (InterruptedException e) {
//        e.printStackTrace();
//      }
//            Thread.yield();
//    }
//        }

        input.notify();
//=======
//    int listSize = LQPConfiguration.getInstance().getConfiguration().getInt("node.list.size", 500);
//    int sleepTimeMilis =
//        LQPConfiguration.getInstance().getConfiguration().getInt("node.sleep.time.milis", 0);
//    int sleepTimeNanos =
//        LQPConfiguration.getInstance().getConfiguration().getInt("node.sleep.time.nanos", 10000);
//    //    synchronized (input){
//    input.add(entry);
//    while (input.size() >= listSize) {
//      try {
//        Thread.sleep(sleepTimeMilis, sleepTimeNanos);
//      } catch (InterruptedException e) {
//        e.printStackTrace();
//>>>>>>> 639dbb647e138d20d76d0e88b841dc15a15a159c
      }
  }

  public Map.Entry poll() {
//    profilerLog.error(callableIndex+": POLL CALLED ");
    Map.Entry result = null;
        synchronized (input){
    result = (Map.Entry) input.poll();
        }

    if(result != null)
    {
//      profilerLog.error(callableIndex+": POLL CALLED  PROCESSED " + processed);
      processed++;
    }
    if(processed > processThreshold ){
      profilerLog.error(callableIndex + " processed " + processed);
      processThreshold *= 1.3;
    }
    return result;
  }

  public void initialize() {
    if (isInitialized)
      return;
    isInitialized = true;
    if (configString != null || configString.length() > 0)
      conf = new JsonObject(configString);
  }

  @Override public void finalizeCallable() {
    try {
//      profCallable.start("finalizeBaseCallable");
      //      EngineUtils.waitForAllExecute();
      //      if(collector != null) {
      //        collector.finalizeCollector();
      //      }
      emanager.stop();
      ecache = null;
      //
      //      ecache.stop();
      //      outputCache.stop();
    } catch (Exception e) {
      System.err.println("LEADS Base callable " + e.getClass().toString() + " " + e.getMessage() + " cause ");
      profilerLog.error(("LEADS Base callable " + e.getClass().toString() + " " + e.getMessage() + " cause "));
      PrintUtilities.logStackTrace(profilerLog, e.getStackTrace());
    }
//    profCallable.end("finalizeBaseCallable");
    end = System.currentTimeMillis();
    profilerLog.error("LBDISTEXEC: " + this.getClass().toString() + " run for " + (end - start) + " ms");
  }

  //

  public void setConfigString(String configString) {
    this.configString = configString;
  }

  public void setOutput(String output) {
    this.output = output;
  }

  public void setCollector(LeadsCollector collector) {
    this.collector = collector;
  }

  public boolean isEmpty() {
    boolean result = false;
    synchronized (input) {
      result = input.isEmpty();
    }
    return result;
  }

  public void setLocalSite(String s) {
    collector.setLocalSite(s);
  }

  public LeadsCollector getCollector() {
    return collector;
  }

  public int getSize() {
    synchronized (input) {
      return input.size();
    }
  }


}
