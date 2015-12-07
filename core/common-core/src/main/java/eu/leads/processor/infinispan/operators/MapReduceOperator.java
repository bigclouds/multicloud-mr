package eu.leads.processor.infinispan.operators;

import eu.leads.processor.common.infinispan.InfinispanManager;
import eu.leads.processor.conf.LQPConfiguration;
import eu.leads.processor.core.Action;
import eu.leads.processor.core.WebUtils;
import eu.leads.processor.core.comp.LogProxy;
import eu.leads.processor.core.net.Node;
import eu.leads.processor.infinispan.*;
import org.infinispan.Cache;
import org.infinispan.commons.api.BasicCache;
import org.vertx.java.core.json.JsonObject;

import java.util.Set;
import java.util.UUID;

/**
 * Created by tr on 19/9/2014.
 */
public abstract class MapReduceOperator extends BasicOperator {

  //  protected transient BasicCache inputCache;
  protected transient BasicCache intermediateCache;
  protected transient BasicCache intermediateLocalCache;
  protected transient BasicCache outputCache;
  protected transient BasicCache intermediateDataCache;
  protected transient BasicCache intermediateLocalDataCache;

  protected String inputCacheName;
  protected String outputCacheName;
  protected String intermediateCacheName;
  protected String intermediateLocalCacheName;
  protected LeadsMapper<?, ?, ?, ?> mapper;
  protected LeadsCollector<?, ?> collector;
  protected LeadsCombiner<?, ?> combiner;
  protected LeadsReducer<?, ?> federationReducer;
  protected LeadsReducer<?, ?> localReducer;
  protected String uuid;

  public MapReduceOperator(Node com, InfinispanManager persistence, LogProxy log, Action action) {
    super(com, persistence, log, action);
    inputCacheName = getInput();
    outputCacheName = action.getData().getObject("operator").getString("id");
    intermediateCacheName = action.getData().getObject("operator").getString("id") + ".intermediate";
    intermediateLocalCacheName = intermediateCacheName + ".local";
    uuid = UUID.randomUUID().toString();
  }

  public void setMapper(LeadsMapper<?, ?, ?, ?> mapper) {
    this.mapper = mapper;
  }

  public void setLocalReducer(LeadsReducer<?, ?> localReducer) {
    this.localReducer = localReducer;
  }

  public void setFederationReducer(LeadsReducer<?, ?> federationReducer) {
    this.federationReducer = federationReducer;
  }
  public void setReducer(LeadsReducer<?, ?> federationReducer) {
    this.federationReducer = federationReducer;
  }

  @Override public void init(JsonObject config) {
    conf.putString("output", getOutput());
    inputCache = (Cache) manager.getPersisentCache(inputCacheName);
    if (reduceLocal) {
      collector = new LeadsCollector(1000, intermediateLocalCacheName);  // TODO(ap0n): not sure for this
    } else {
      collector = new LeadsCollector(1000, intermediateCacheName);
    }
  }

  @Override public String computeEnsembleHost(boolean isMap) {
    collector.setLocalSite(
        globalConfig.getObject("componentsAddrs").getArray(LQPConfiguration.getInstance().getMicroClusterName()).get(0)
            .toString());// + ":11222");

    return super.computeEnsembleHost(isMap);
  }

  @Override public void cleanup() {
    inputCache = (Cache) manager.getPersisentCache(inputCacheName);
    super.cleanup();
    if (reduceLocal) {
      if (!isRemote) {
        WebUtils.stopCache(intermediateLocalCacheName + ".data", globalConfig);
      }
      //      manager.removePersistentCache(intermediateLocalCache+".data");
    }

    if (executeOnlyReduce) {
      //      intermediateCache.stop();
      //      indexSiteCache.stop();
      //      intermediateDataCache.stop();
      if (!isRemote) {
        WebUtils.stopCache(intermediateCacheName + ".data", globalConfig);
        //        manager.removePersistentCache(intermediateDataCache.getName());
      }
      //      keysCache.stop();
    }
  }

  @Override public void failCleanup() {
    inputCache = (Cache) manager.getPersisentCache(inputCacheName);
    super.failCleanup();
    if (reduceLocal) {
      WebUtils.stopCache(intermediateLocalCacheName + ".data", globalConfig);
      //      manager.removePersistentCache(intermediateLocalCache+".data");
    }

    if (executeOnlyReduce) {
      //      intermediateCache.stop();
      //      indexSiteCache.stop();
      //      intermediateDataCache.stop();
      WebUtils.stopCache(intermediateCacheName + ".data", globalConfig);
      //      manager.removePersistentCache(intermediateDataCache.getName());
      //      keysCache.stop();
    }
  }
  @Override public boolean isSingleStage() {
    return false;
  }

  @Override public void createCaches(boolean isRemote, boolean executeOnlyMap, boolean executeOnlyReduce) {
    Set<String> targetMC = getTargetMC();
    if (!isRemote) {
      for (String mc : targetMC) {
        createCache(mc, getOutput(), "batchputListener");
        //      createCache(mc, intermediateCacheName);
        //create Intermediate cache name for data on the same Sites as outputCache
//        if (!conf.containsField("skipMap")) {
          //          if(!conf.getBoolean("skipMap")){
          if (!isRecCompReduce) {
            createCache(mc, intermediateCacheName + ".data", "localIndexListener:batchputListener");
          } else {
            createCache(mc, intermediateCacheName + ".data", "batchputListener");
          }
          if (reduceLocal) {
            System.out.println("REDUCE LOCAL DETECTED CREATING CACHE");
            if (!isRecCompReduceLocal) {
              createCache(mc, intermediateLocalCacheName + ".data", "localIndexListener:batchputListener");
            } else {
              createCache(mc, intermediateLocalCacheName + ".data", "batchputListener");
            }
          } else {
            System.out.println("NO REDUCE LOCAL");
          }
        }
        //create Intermediate  keys cache name for data on the same Sites as outputCache;
        //      createCache(mc,intermediateCacheName+".keys");
        //createIndexCache for getting all the nodes that contain values with the same key! in a mc
        //      createCache(mc,intermediateCacheName+".indexed");
        //    indexSiteCache = (BasicCache)manager.getIndexedPersistentCache(intermediateCacheName+".indexed");


    }
  }

  @Override public void setupMapCallable() {
    //    conf.putString("output",getOutput());
    inputCache = (Cache) manager.getPersisentCache(inputCacheName);

    if (reduceLocal) {
      collector = new LeadsCollector(1000, intermediateLocalCacheName);
      if(!isRemote) {
        if (isRecCompReduceLocal) {
          JsonObject reduceLocalConf = getContinuousReduceLocal();
          String ensembleString = computeEnsembleHost(false);
          String inputListener = intermediateLocalCacheName + ".data";
          String outputListener = intermediateCacheName;
          String window = "sizeBased";
          int windowSize = LQPConfiguration.getInstance().getConfiguration().getInt("node.continuous.windowSize", 1000);
          int parallelism = LQPConfiguration.getInstance().getConfiguration().getInt("node.engine.parallelism", 4);
          reduceLocalConf.putString("cache", inputListener);
          reduceLocalConf.getObject("conf").putString("window", window);
          reduceLocalConf.getObject("conf").putNumber("windowSize", windowSize);
          reduceLocalConf.getObject("conf").putNumber("parallelism", parallelism);
          reduceLocalConf.getObject("conf").putString("input", inputListener);
          reduceLocalConf.getObject("conf").putString("ensembleHost", ensembleString);
          reduceLocalConf.getObject("conf").getObject("operator").putString("ensembleString", ensembleString);
          reduceLocalConf.getObject("conf").getObject("operator").putString("output", outputListener);
          reduceLocalConf.getObject("conf").getObject("operator").putNumber("parallelism", parallelism);
          WebUtils.addListener(inputListener, getContinuousListenerClass(), reduceLocalConf, globalConfig);
        }
      }
    } else {
      collector = new LeadsCollector(1000, intermediateCacheName);
    }

    if (isRecCompReduce) {
      JsonObject reduceConf = getContinuousReduce();
      String ensembleString = computeEnsembleHost(false);
      String inputListener = intermediateCacheName + ".data";
      String outputListener = getOutput();
      String window = "sizeBased";
      int windowSize = LQPConfiguration.getInstance().getConfiguration().getInt("node.continuous.windowSize", 1000);
      int parallelism = LQPConfiguration.getInstance().getConfiguration().getInt("node.engine.parallelism", 4);
      reduceConf.putString("cache", inputListener);
      reduceConf.getObject("conf").putString("window", window);
      reduceConf.getObject("conf").putNumber("windowSize", windowSize);
      reduceConf.getObject("conf").putNumber("parallelism", parallelism);
      reduceConf.getObject("conf").putString("input", inputListener);
      reduceConf.getObject("conf").putString("ensembleHost", ensembleString);
      reduceConf.getObject("conf").getObject("operator").putString("output", outputListener);
      reduceConf.getObject("conf").getObject("operator").putString("ensembleString", ensembleString);
      reduceConf.getObject("conf").getObject("operator").putNumber("parallelism", parallelism);
      WebUtils.addListener(inputListener, getContinuousListenerClass(), reduceConf, globalConfig);
    }
    mapperCallable = new LeadsMapperCallable((Cache) inputCache, collector, mapper,
        LQPConfiguration.getInstance().getMicroClusterName());
    if (combiner != null && action.getData().getObject("operator").containsField("combine")) {
      ((LeadsMapperCallable) mapperCallable).setCombiner(combiner);
    }
  }

  @Override public void setupReduceCallable() {
    conf.putString("output", getOutput());
    intermediateDataCache = (BasicCache) manager.getPersisentCache(intermediateCacheName + ".data");

    outputCache = (BasicCache) manager.getPersisentCache(outputCacheName);
    collector = new LeadsCollector(0, outputCache.getName());
    inputCache = (Cache) intermediateDataCache;
    reduceInputCache = inputCache;
    reducerCallable = new LeadsReducerCallable(outputCache.getName(), federationReducer, intermediateCacheName);

    ((LeadsReducerCallable) reducerCallable).setLocalSite(
        globalConfig.getObject("componentsAddrs").getArray(LQPConfiguration.getInstance().getMicroClusterName()).get(0)
            .toString());// + ":11222");
  }

  @Override public void setupReduceLocalCallable() {
    // TODO(ap0n): conf.putString("output", getOutput());
    intermediateLocalDataCache = (BasicCache) manager.getPersisentCache(intermediateLocalCacheName + ".data");
//    log.error("ReducerIntermediateLocalData " + intermediateLocalDataCache.size());
    //    outputCache = (BasicCache) manager.getPersisentCache(intermediateCacheName);
    reduceLocalInputCache = (Cache) intermediateLocalDataCache;

    collector = new LeadsCollector(1000, intermediateCacheName);

    reducerLocalCallable =
        new LeadsLocalReducerCallable(intermediateCacheName, localReducer, intermediateLocalCacheName,
            LQPConfiguration.getInstance().getMicroClusterName());

    combiner = null;
    String localSite =
        globalConfig.getObject("componentsAddrs").getArray(LQPConfiguration.getInstance().getMicroClusterName()).get(0)
            .toString();
    ((LeadsLocalReducerCallable) reducerLocalCallable).setLocalSite(localSite + ":11222");
  }
  public LeadsCombiner<?, ?> getCombiner() {
    return combiner;
  }

  public void setCombiner(LeadsCombiner<?, ?> combiner) {
    this.combiner = combiner;
  }
}
