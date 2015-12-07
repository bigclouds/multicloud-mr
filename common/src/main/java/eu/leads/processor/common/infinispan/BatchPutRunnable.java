package eu.leads.processor.common.infinispan;

import eu.leads.processor.common.utils.PrintUtilities;
import org.infinispan.Cache;
import org.infinispan.commons.util.concurrent.NotifyingFuture;
import org.infinispan.distribution.DistributionManager;
import org.infinispan.ensemble.cache.EnsembleCache;
import org.infinispan.remoting.transport.Address;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * Created by vagvaz on 9/1/15.
 */
public class BatchPutRunnable implements Runnable{
  private EnsembleCacheUtilsSingle owner;
  TupleBuffer buffer = null;
  int retries = 10;
  Logger log = LoggerFactory.getLogger(BatchPutRunnable.class);
  private Map parameters;
  private boolean isLocal;

  public BatchPutRunnable(){

  }
  public BatchPutRunnable(int retries){
    this.retries=retries ;
  }

  public BatchPutRunnable(int i, EnsembleCacheUtilsSingle ensembleCacheUtilsSingle) {
    this.owner = ensembleCacheUtilsSingle;
    this.retries = i;
  }

  public TupleBuffer getBuffer(){
    return buffer;
  }
  public void setBuffer(TupleBuffer buffer){
//    log.error("SET BUF");
    this.buffer = buffer;
  }
  @Override public void run() {
//    log.error("--- START BATCH RUN");
    if(isLocal){
      localPut();
    }else{
      batchPut();
    }
  }

  private void localPut() {
    boolean isok = false;
//    PrintUtilities.printAndLog(log,"BATCHLOCALPUT ");
    Map data = parameters;
    if (data == null || data.size() == 0) {
      if (owner != null) {
        owner.addBatchPutRunnable(this);
      } else {
        EnsembleCacheUtils.addBatchPutRunnable(this);
      }
    }
    DistributionManager distributionManager = (DistributionManager) data.get("distMan");
    Integer localBatch = (Integer) data.get("batchPut");
    Cache cache = (Cache) data.get("cache");
    Map<Object, Object> buffer = (Map) data.get("buffer");
//    PrintUtilities.printAndLog(log,"BATCHLOCALPUT " + buffer.size());
//    Map<NotifyingFuture, Map<Object, Object>> futures = new HashMap<>();
//    Map<Address, Map<Object, Object>> nodeMaps = new HashMap<>();
//    for (Address a : cache.getCacheManager().getMembers()) {
//      nodeMaps.put(a, new HashMap<>());
//    }
//    for (Map.Entry<Object, Object> entry : buffer.entrySet()) {
//      Address a = distributionManager.getPrimaryLocation(entry.getKey());
//      nodeMaps.get(a).put(entry.getKey(), entry.getValue());
//    }
//    for (Map.Entry entry : nodeMaps.entrySet()) {
//      futures.put(cache.putAllAsync((Map) entry.getValue()), (Map<Object, Object>) entry.getValue());
//    }
    try {
//      retries = 2 * futures.size();
      retries =3;
//      List<Map<Object, Object>> toRetry = new ArrayList<>(futures.size());
//      while (retries > 0 && futures.size() > 0) {
//
//        Iterator<NotifyingFuture> futureIterator = futures.keySet().iterator();
//        while (futureIterator.hasNext()) {
//          NotifyingFuture future = futureIterator.next();
          try {
            cache.putAll(buffer);
//            future.get();
//            Map map = futures.get(future);
//            map.clear();
//            futureIterator.remove();
          } catch (Exception e) {
            PrintUtilities.printAndLog(log,"Exception in LOCAL BatchPutRunnbale= " + e.getClass().toString() + " " + e.getMessage());
            e.printStackTrace();
            retries--;
            PrintUtilities.logStackTrace(log, e.getStackTrace());
//            toRetry.add(futures.get(future));
//            futureIterator.remove();
          }
//        }
//        for (Map<Object, Object> map : toRetry) {
//          futures.put(cache.putAllAsync(map), map);
//        }
//        toRetry.clear();
//      }
//      for(Map m : nodeMaps.values()){
//        m.clear();
//      }
//      nodeMaps.clear();

      if (owner != null) {
        owner.addBatchPutRunnable(this);
      } else {
        //          System.err.println("ADDING batchput to static");
        EnsembleCacheUtils.addBatchPutRunnable(this);
      }
      cache = null;
    } catch (Exception e) {
      System.err.println("Exception in LOCAL BatchPutRunnbale= " + e.getClass().toString() + " " + e.getMessage());
      e.printStackTrace();

      PrintUtilities.logStackTrace(log, e.getStackTrace());
    }
  }

  private void batchPut() {
    boolean isok = false;
    Map data = buffer.flushToMC();
    if(data==null || data.size() == 0){
      if(owner != null){
        //          System.err.println("ADDING batchput to single");
        owner.addBatchPutRunnable(this);
      }else {
        //          System.err.println("ADDING batchput to static");
        EnsembleCacheUtils.addBatchPutRunnable(this);
      }
    }
    long counter = (long) data.get("counter");
    EnsembleCache cache = (EnsembleCache) data.get("cache");
    String uuid = (String) data.get("uuid");
    byte[] bytes = (byte[]) data.get("bytes");
    try{

      //            EnsembleCacheUtilsSingle ensembleCacheUtilsSingle = (EnsembleCacheUtilsSingle) data.get("ensemble");

      while(retries > 0 && !isok){
        cache.put(uuid + ":" + Long.toString(counter),bytes);
        isok = true;
//        log.error("++++ END BATCH RUN");
      }
    }
    catch (Exception e){
      System.err.println(
          "Exception in BatchPutRunnbale= " + e.getClass().toString() + " " + e.getMessage());
      e.printStackTrace();
      retries--;
      PrintUtilities.logStackTrace(log,e.getStackTrace());
    }
    if(owner != null){
      //          System.err.println("ADDING batchput to single");
      owner.updateRemoteBytes(bytes.length);
      owner.addBatchPutRunnable(this);
    }else {
      //          System.err.println("ADDING batchput to static");
      EnsembleCacheUtils.addBatchPutRunnable(this);
    }
    bytes = null;
    cache = null;
  }


  public void setParameters(Map parameters) {
    this.parameters = parameters;
    isLocal = true;
  }
}
