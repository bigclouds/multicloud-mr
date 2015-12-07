package eu.leads.processor.infinispan;

import eu.leads.processor.common.infinispan.EnsembleCacheUtilsSingle;
import eu.leads.processor.common.infinispan.InfinispanManager;
import eu.leads.processor.common.utils.PrintUtilities;
import eu.leads.processor.conf.LQPConfiguration;
import org.infinispan.Cache;
import org.infinispan.commons.api.BasicCache;
import org.infinispan.distexec.mapreduce.Collector;
import org.infinispan.ensemble.EnsembleCacheManager;
import org.infinispan.manager.EmbeddedCacheManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.concurrent.ExecutionException;

import eu.leads.processor.common.infinispan.ClusterInfinispanManager;

import org.infinispan.ensemble.Site;
import org.infinispan.ensemble.cache.EnsembleCache;

import java.util.*;

public class LeadsCollector<KOut, VOut> implements Collector<KOut, VOut>, Serializable {

  private static final long serialVersionUID = -602082107893975415L;
  private  Integer emitCount;
  private  int maxCollectorSize = 1000;
  private double percent = .75;
  private LeadsCombiner<KOut,VOut> combiner;
  protected transient BasicCache intermediateDataCache;
  private transient BasicCache storeCache;
  private transient InfinispanManager imanager;
  private transient EmbeddedCacheManager manager;
  private transient EnsembleCacheManager emanager;
  private transient Logger log = null;
  private Integer counter = 0;
  private transient Map<KOut,List<VOut>> buffer;
  private boolean onMap = true;
  private boolean isReduceLocal = false;
  private boolean useCombiner = false;
  private int indexSite=-1;
  private String localSite;
  private String site;
  private String node;
  private String cacheName;
  private ComplexIntermediateKey baseIntermKey;
  private transient volatile Object mutex;
  private String ensembleHost;
//  private long localData;
//  private long remoteData;
  protected Map<KOut, List<VOut>> combinedValues;
  private EnsembleCacheUtilsSingle ensembleCacheUtilsSingle;
  private LocalCollector localCollector;

  public LeadsCollector(int maxCollectorSize, String collectorCacheName) {
    super();
    maxCollectorSize = LQPConfiguration.getInstance().getConfiguration().getInt("node.combiner.buffersize",10000);
    percent = LQPConfiguration.getInstance().getConfiguration().getInt("node.combiner.percent",75);
//    this.maxCollectorSize = maxCollectorSize;
    cacheName = collectorCacheName;
    emitCount = 0;
  }

  public LeadsCollector(int maxCollectorSize, String cacheName, InfinispanManager manager) {
//    this.maxCollectorSize = maxCollectorSize;
    maxCollectorSize = LQPConfiguration.getInstance().getConfiguration().getInt("node.combiner.buffersize",10000);
    percent = LQPConfiguration.getInstance().getConfiguration().getInt("node.combiner.percent",75);
    emitCount = 0;
    this.imanager = manager;
    this.cacheName = cacheName;
    storeCache = (BasicCache) emanager.getCache(cacheName, new ArrayList<>(emanager.sites()),
                                                EnsembleCacheManager.Consistency.DIST);
  }

  public LeadsCollector(LeadsCollector other) {
    //    this.counterCache = other.counterCache;
    //    this.counter = other.counter;
    this.imanager = other.imanager;
    this.manager = other.manager;
    this.emanager = other.emanager;
    this.log = other.log;
    this.onMap = other.onMap;
    this.site = other.site;
    this.node = other.node;
    this.cacheName = other.cacheName;
    this.ensembleHost = other.ensembleHost;
    this.counter = 0;
    this.isReduceLocal = other.isReduceLocal;
    //    this.combiner = other.combiner;
    this.baseIntermKey = new ComplexIntermediateKey(other.baseIntermKey);
    this.maxCollectorSize = other.maxCollectorSize;
    //    this.currentKey = other.currentKey;
    this.mutex = new Object();
    //    this.ensembleCacheUtilsSingle = new EnsembleCacheUtilsSingle();
  }

  public LeadsCombiner<KOut, VOut> getCombiner() {
    return combiner;
  }

  public void setCombiner(LeadsCombiner<KOut, VOut> combiner) {
    this.combiner = combiner;
    maxCollectorSize = LQPConfiguration.getInstance().getConfiguration().getInt("node.combiner.buffersize",10000);
    percent = LQPConfiguration.getInstance().getConfiguration().getInt("node.combiner.percent",75);
    percent /= 100;
  }

  public BasicCache getIntermediateDataCache() {
    return intermediateDataCache;
  }

  public void setIntermediateDataCache(BasicCache intermediateDataCache) {
    this.intermediateDataCache = intermediateDataCache;
  }


  public String getLocalSite() {
    return localSite;
  }

  public void setLocalSite(String localSite) {
    this.localSite = localSite;
  }

  public int getMaxCollectorSize() {
    return maxCollectorSize;
  }

  public void setMaxCollectorSize(int maxCollectorSize) {
    this.maxCollectorSize = maxCollectorSize;
  }

  public BasicCache getCache() {
    return storeCache;
  }

  public EnsembleCacheManager getEmanager() {
    return emanager;
  }

  public void setEmanager(EnsembleCacheManager emanager) {
    this.emanager = emanager;
    if(localSite != null && !localSite.equals("")){
      for(Site s : emanager.sites()){
        indexSite++;
        if(s.getName().equals(localSite)){
          break;
        }
      }
    }

    System.out.println("Index Site is " + localSite + indexSite);
  }

  public EmbeddedCacheManager getManager() {
    return manager;
  }

  public void setManager(EmbeddedCacheManager manager) {
    this.manager = manager;

  }

  public String getSite() {
    return site;
  }

  public void setSite(String site) {
    this.site = site;
  }

  public String getNode() {
    return node;
  }

  public void setNode(String node) {
    this.node = node;
  }
  //  private LeadsCombiner combiner;

  public String getCacheName() {
    return cacheName;
  }

  public void setCacheName(String cacheName) {
    this.cacheName = cacheName;
  }

  public boolean isOnMap() {
    return onMap;
  }

  public void setOnMap(boolean onMap) {
    this.onMap = onMap;
  }

  public boolean isReduceLocal() {
    return isReduceLocal;
  }

  public void setIsReduceLocal(boolean isReduceLocal) {
    this.isReduceLocal = isReduceLocal;
  }

  public boolean isUseCombiner() {
    return useCombiner;
  }

  public void setUseCombiner(boolean useCombiner) {
    this.useCombiner = useCombiner;
  }

  public InfinispanManager getImanager() {
    return imanager;
  }

  public void setImanager(InfinispanManager imanager) {
    this.imanager = imanager;
  }

  public void initializeCache(String inputCacheName, InfinispanManager imanager) {
    localCollector = new LocalCollector(0,"");
    maxCollectorSize = LQPConfiguration.getInstance().getConfiguration().getInt("node.combiner.buffersize",10000);
    percent = LQPConfiguration.getInstance().getConfiguration().getInt("node.combiner.percent",75);
    ensembleCacheUtilsSingle = new EnsembleCacheUtilsSingle();
    ensembleCacheUtilsSingle.initialize(emanager);
    this.imanager = imanager;
    log = LoggerFactory.getLogger(LeadsCollector.class);
    emitCount = 0;
    buffer = new HashMap<>();
//    storeCache = (Cache) imanager.getPersisentCache(cacheName);
    if (site == null) {
      LQPConfiguration.getInstance().getMicroClusterName();
    }
    node =imanager.getMemberName().toString();
    site = LQPConfiguration.getInstance().getMicroClusterName();
    if (onMap) {
      intermediateDataCache = (BasicCache) emanager.getCache(cacheName+ ".data",
                                                             new ArrayList<>(emanager.sites()),
                                                             EnsembleCacheManager.Consistency.DIST);
      baseIntermKey = new ComplexIntermediateKey(site, manager.getAddress().toString(),
                                                 inputCacheName);
      mutex = new Object();
    }else{
      storeCache = emanager.getCache(cacheName, new ArrayList<>(emanager.sites()),
          EnsembleCacheManager.Consistency.DIST);
    }
  }

  public void emit(KOut key, VOut value) {

    if (onMap) {
      if(isReduceLocal){
        output(key,value);
      }
      else{
        if(useCombiner){
          List<VOut> values = buffer.get(key);
          if(values == null) {
            values = new LinkedList<>();
            buffer.put(key, values);
          }
          values.add(value);
          emitCount++;
          if(isOverflown()){
            combine(false);
          }
        }
        else{
          output(key,value);
        }
      }

    } else {
//      if(key.hashCode() % emanager.sites().size() == indexSite){
//        localData += key.toString().length() + value.toString().length();
//      }
//      else{
//        remoteData += key.toString().length() + value.toString().length();
//      }
      ensembleCacheUtilsSingle.putToCache(storeCache, key, value);
    }
  }

  private void combine(boolean force) { //force the output of values
//    log.error("Run combine " + maxCollectorSize + " " + buffer.size());
    localCollector = new LocalCollector(0,"");
    int lastSize = emitCount;
    for(Map.Entry<KOut,List<VOut>> entry : buffer.entrySet()){
      combiner.reduce(entry.getKey(),entry.getValue().iterator(),localCollector);
    }

    Map<KOut,List<VOut>> combinedValues = localCollector.getCombinedValues();
    if( force  || (combinedValues.size() >= maxCollectorSize*percent ) || (lastSize *percent <= combinedValues.size()) ) {
//      PrintUtilities.printAndLog(log,"Flush " + combinedValues.size() + " " + lastSize);
      for (Map.Entry<KOut, List<VOut>> entry : combinedValues.entrySet()) {
        for (VOut v : entry.getValue()) {
          output(entry.getKey(), v);
        }
      }
      buffer.clear();
      emitCount = 0; // the size is 0 since we have written everything
    }
    else{
      buffer.clear();
      buffer = combinedValues;
      emitCount = buffer.size(); // the size is only one per each key
    }
    localCollector =null;
  }

  private void output(KOut key, VOut value) {
//    if(key.hashCode() % emanager.sites().size() == indexSite){
//      localData += key.toString().length() +
//          site.length() +
//          node.length() + 4;  //cost for complex Intermediate key
//      localData += value.toString().length();
//    }
//    else{
//      remoteData += key.toString().length() +
//          site.length() +
//          node.length() + 4;  //cost for complex Intermediate key
//      remoteData += value.toString().length();
//    }

    synchronized (mutex){
      counter++;
    }
    baseIntermKey.setKey(key.toString());
    baseIntermKey.setCounter(counter);
    ComplexIntermediateKey
        newKey =
        new ComplexIntermediateKey(baseIntermKey.getSite(), baseIntermKey.getNode(),
            key.toString(), baseIntermKey.getCache(), counter);
    ensembleCacheUtilsSingle.putToCache(intermediateDataCache, newKey, value);
  }

  public void finalizeCollector() {
    try {
      if (useCombiner) {
        combine(true);
      }
     if (combiner != null) {
      combiner.finalizeTask();
    }
  }catch(Exception e) {
      e.printStackTrace();
  }
    try {
      ensembleCacheUtilsSingle.waitForAllPuts();
    } catch (InterruptedException e) {
      e.printStackTrace();
    } catch (ExecutionException e) {
      e.printStackTrace();
    } catch (Exception e) {
      e.printStackTrace();
    }
  }
//
//  public void initializeCache(EmbeddedCacheManager manager) {
//    imanager = new ClusterInfinispanManager(manager);
//    storeCache = (Cache) imanager.getPersisentCache(cacheName);
//  }


  public void reset() {
//    storeCache.clear();
    emitCount = 0;
  }



  public boolean isOverflown() {
    return emitCount.intValue() >=   maxCollectorSize;
  }

  public Map<KOut, List<VOut>> getCombinedValues() {
    return combinedValues;
  }

  public void setCombinedValues(Map<KOut, List<VOut>> combinedValues) {
    this.combinedValues = combinedValues;
  }

  public void setEnsembleHost(String ensembleHost) {
    this.ensembleHost = ensembleHost;
  }

  public String getEnsembleHost() {
    return ensembleHost;
  }

  public void setReduceLocal(boolean isReduceLocal) {
    this.isReduceLocal = isReduceLocal;
  }
}
