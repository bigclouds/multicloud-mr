package eu.leads.processor.common.infinispan;

import eu.leads.processor.common.utils.PrintUtilities;
import eu.leads.processor.conf.LQPConfiguration;
import eu.leads.processor.core.Tuple;
import eu.leads.processor.infinispan.ComplexIntermediateKey;
import org.bson.BSONDecoder;
import org.bson.BSONEncoder;
import org.bson.BasicBSONDecoder;
import org.bson.BasicBSONEncoder;
import org.infinispan.Cache;
import org.infinispan.commons.api.BasicCache;
import org.infinispan.commons.util.concurrent.NotifyingFuture;
import org.infinispan.context.Flag;
import org.infinispan.distribution.DistributionManager;
import org.infinispan.ensemble.EnsembleCacheManager;
import org.infinispan.ensemble.cache.EnsembleCache;
import org.infinispan.remoting.transport.Address;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xerial.snappy.Snappy;

import java.io.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeoutException;

/**
 * Created by vagvaz on 8/30/15.
 */
public class TupleBuffer {
  ;
  HashMap<Object, Object> buffer;
  private  transient int threshold;
  private transient EnsembleCacheManager emanager;
  private transient EnsembleCache ensembleCache;
  private transient long localCounter;
  private transient volatile Object mutex = new Object();
  private transient String mc;
  private transient Cache localCache;
  private String cacheName;
  private transient String uuid;
  private int batchThreshold = 10;
  private int size = 0;
  private Logger log = LoggerFactory.getLogger(TupleBuffer.class);
  private transient EnsembleCacheUtilsSingle ensembleCacheUtilsSingle;
  private transient Address localAddress;
  private transient DistributionManager distributionManager;
//  private transient Map<Address,Map<Object,Object>> nodeMaps;

  public TupleBuffer(){
    buffer = new HashMap<>();
    threshold = 500;
    localCounter = 0;
    batchThreshold = LQPConfiguration.getInstance().getConfiguration().getInt("node.ensemble.batchput.batchsize",batchThreshold);
  }
  public TupleBuffer(byte[] bytes){
    BSONDecoder decoder = new BasicBSONDecoder();
    buffer = new HashMap<>();
    //        int compressedSize = in.readInt();
    byte[] compressed = bytes;//new byte[compressedSize];
    try {
      byte[] uncompressed = Snappy.uncompress(compressed);
      ByteArrayInputStream byteStream = new ByteArrayInputStream(uncompressed);
      ObjectInputStream inputStream = new ObjectInputStream(byteStream);
      int size = inputStream.readInt();
      for (int index = 0; index < size; index++) {
        Object key = inputStream.readObject();
        //                int tupleBytesSize = inputStream.readInt();
        //                byte[] tupleBytes = new byte[tupleBytesSize];
        //                inputStream.read(tupleBytes);
        //                Tuple tuple = new Tuple(decoder.readObject(tupleBytes));
        Object tuple =  inputStream.readObject();
        buffer.put(key, tuple);
      }
      inputStream.close();
      byteStream.close();

      inputStream = null;
      byteStream =null;
      //      ensembleCacheUtilsSingle = new EnsembleCacheUtilsSingle();
      this.ensembleCacheUtilsSingle = ensembleCacheUtilsSingle;
//      localAddress = InfinispanClusterSingleton.getInstance().getManager().getMemberName();
    }catch (Exception e){
      e.printStackTrace();
    }
  }
//  public TupleBuffer(int threshold){
//    buffer = new HashMap<>();
//    this.threshold = threshold;
//    localCounter = 0;
//    uuid = UUID.randomUUID().toString();
//    batchThreshold = LQPConfiguration.getInstance().getConfiguration().getInt(
//        "node.ensemble.batchput.batchsize", batchThreshold);
////    nodeMaps = new HashMap<>();
////    localAddress = InfinispanClusterSingleton.getInstance().getManager().getMemberName();
//  }

//  public TupleBuffer(int threshold, BasicCache cache, EnsembleCacheManager ensembleCacheManager,EnsembleCacheUtilsSingle ensembleCacheUtilsSingle) {
//    this.ensembleCacheUtilsSingle = ensembleCacheUtilsSingle;
//    //    this.ensembleCacheUtilsSingle = new EnsembleCacheUtilsSingle();
//    this.threshold = threshold;
//    buffer = new HashMap<>();
//    this.emanager = ensembleCacheManager;
//    this.ensembleCache = emanager.getCache(cache.getName()+".compressed", new ArrayList<>(ensembleCacheManager.sites()),
//        EnsembleCacheManager.Consistency.DIST);
////    nodeMaps = new HashMap<>();
////    localAddress = InfinispanClusterSingleton.getInstance().getManager().getMemberName();
//    if(cache instanceof Cache){
//      localCache = (Cache) cache;
////      distMan = localCache.getAdvancedCache().getDistributionManager();
////      for(Address address :((Cache) cache).getAdvancedCache().getRpcManager().getMembers()){
////        nodeMaps.put(address.toString(),new HashMap<Object, Object>());
////      }
//    }
//    localCounter = 0;
//    this.cacheName = cache.getName();
//    uuid = UUID.randomUUID().toString();
//    batchThreshold = LQPConfiguration.getInstance().getConfiguration().getInt(
//        "node.ensemble.batchput.batchsize", batchThreshold);
//
//  }

  public TupleBuffer(int threshold, String cacheName, EnsembleCacheManager ensembleCacheManager,String mc,EnsembleCacheUtilsSingle ensembleCacheUtilsSingle) {
    this.ensembleCacheUtilsSingle = ensembleCacheUtilsSingle;
    //    ensembleCacheUtilsSingle = new EnsembleCacheUtilsSingle();
    this.threshold = threshold;
    buffer = new HashMap<>();
    this.emanager = ensembleCacheManager;
    this.ensembleCache = emanager.getCache(cacheName+".compressed", new ArrayList<>(ensembleCacheManager.sites()),
        EnsembleCacheManager.Consistency.DIST);
    this.mc = mc;
    localCounter = 0;
    this.cacheName = cacheName;
    uuid = UUID.randomUUID().toString();
    batchThreshold = LQPConfiguration.getInstance().getConfiguration().getInt(
        "node.ensemble.batchput.batchsize", batchThreshold);
  }

  public TupleBuffer(int localBatchSize, Cache localCache, EnsembleCacheUtilsSingle ensembleCacheUtilsSingle) {
    this.localCache = localCache;
    this.localAddress = localCache.getCacheManager().getAddress();
    distributionManager = localCache.getAdvancedCache().getDistributionManager();
//    nodeMaps = new HashMap<Address,Map<Object,Object>>();
//    for(Address address : localCache.getAdvancedCache().getRpcManager().getMembers()){
//      nodeMaps.put(address,new LinkedHashMap<>());
//    }
    this.threshold = localBatchSize;
    buffer = new LinkedHashMap<>(threshold);
    ensembleCache = null;

  }

  public Cache getLocalCache() {
    return localCache;
  }

  public void setLocalCache(Cache localCache) {
    this.localCache = localCache;
  }

  public String getMC(){return mc;}
  public Map<Object,Object> getBuffer(){
    return buffer;
  }

  public boolean add(Object key, Object value){
    synchronized (mutex) {
//      if(localCache == null) {
        buffer.put(key, value);
        size++;
        return (size >= threshold);
      }
//    else{
        //        ensembleCacheUtilsSingle.addLocalFuture(localCache.getAdvancedCache().withFlags(Flag.IGNORE_RETURN_VALUES).putAsync(key,value));
//        ensembleCacheUtilsSingle.putToCacheDirect(localCache,key,value);
//        return (size >= threshold);
//      }
//    }
  }

  public Map flushToMC() {
    byte[] bytes = null;
    if(buffer.size() == 0){
      return null;
    }
    Map result = new HashMap();
    synchronized (mutex){
      //      if (ensembleCache == null) {
      //                this.ensembleCache = emanager.getCache(cacheName + ".compressed", new ArrayList<>(emanager.sites()),
      //                    EnsembleCacheManager.Consistency.DIST);
      //
      //      }
      result.put("cache",ensembleCache);
      synchronized (mutex){
        if (buffer.size() == 0) {
          return null;
        }
        localCounter = (localCounter + 1) % Long.MAX_VALUE;
        result.put("counter",localCounter);
        bytes = this.serialize();
        buffer.clear();
        size = 0;
        result.put("bytes",bytes);
        //        result.put("ensemble",ensembleCacheUtilsSingle);
        result.put("uuid",uuid);
      }
      return result;
    }

    //    byte[] bytes = null;
    //    synchronized (mutex) {
    //      if (ensembleCache == null) {
    //        this.ensembleCache = emanager.getCache(cacheName + ".compressed", new ArrayList<>(emanager.sites()),
    //            EnsembleCacheManager.Consistency.DIST);
    //
    //      }
    //      //            System.out.println("FLusht to mc " + ensembleCache.getName() + " " + buffer.size());
    //
    //      if (buffer.size() == 0)
    //        return;
    //      localCounter = (localCounter + 1) % Long.MAX_VALUE;
    //
    //      bytes = this.serialize();
    //      buffer.clear();
    //      size = 0;
    //    }
    //    boolean isok = false;
    //    try {
    //      while (!isok) {
    //        ensembleCache.put(uuid + ":" + Long.toString(localCounter), bytes);
    //        isok = true;
    //
    //      }
    //    } catch (Exception e) {
    //      if (e instanceof TimeoutException) {
    //        try {
    //          Thread.sleep(10);
    //        } catch (InterruptedException e1) {
    //          e1.printStackTrace();
    //        }
    //        System.err.println("Timeout Exxcception in slushToMC " + e.getMessage());
    //        PrintUtilities.logStackTrace(log,e.getStackTrace());
    //      }
    //      e.printStackTrace();
    //
    //    }

  }
  public void flushEndToMC() {
    System.out.println("FLush END to mc " + buffer.size() + " " + (ensembleCache == null ? "null" : ensembleCache.getName()));
    synchronized (mutex) {
      if (buffer.size() == 0) {
        //                ensembleCache = null;
        //                cacheName = null;
        return;
      }
      if (ensembleCache == null) {
        this.ensembleCache = emanager.getCache(cacheName + ".compressed", new ArrayList<>(emanager.sites()),
            EnsembleCacheManager.Consistency.DIST);

      }

      if (buffer.size() > 0) {
        System.err.println("FLUSH END called but more tuples were added for " + cacheName + " missing in the end " + buffer.size());
        //        BatchPutRunnable bpr = ensembleCacheUtilsSingle.getBatchPutRunnable();
        //        bpr.setBuffer(this);
        //        ensembleCacheUtilsSingle.submit(bpr);
      }
      localCounter = (localCounter + 1) % Long.MAX_VALUE;
      byte[] bytes = new byte[1];
      bytes[0] = -1;
      ensembleCache.put(uuid+":"+Long.toString(localCounter), bytes);
      //            ensembleCache = null;
      //            cacheName = null;
      buffer.clear();
      System.out.println("FLush ENDED to mc " + buffer.size() + " " + (ensembleCache == null ? "null" : ensembleCache.getName()));
    }
  }

  public byte[] serialize()  {
    synchronized (mutex) {
      try {
        ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
        ObjectOutputStream outputStream = new ObjectOutputStream(byteStream);
        BSONEncoder encoder = new BasicBSONEncoder();
        outputStream.writeInt(buffer.size());
        for (Map.Entry<Object, Object> entry : buffer.entrySet()) {
          if(entry.getKey() instanceof String || entry.getKey() instanceof ComplexIntermediateKey) {
            outputStream.writeObject(entry.getKey());
          }
          else{
            outputStream.writeObject(entry.getKey().toString());
          }
          //                byte[] tupleBytes = encoder.encode(entry.getValue().asBsonObject());
          //                outputStream.writeInt(tupleBytes.length);
          //                outputStream.write(tupleBytes);
          outputStream.writeObject(entry.getValue());
        }
        buffer.clear();
        outputStream.flush();
        outputStream.close();
        byteStream.close();
        byte[] uncompressed = byteStream.toByteArray();
        byte[] compressed = Snappy.compress(uncompressed);
        //        out.writeInt(compressed.length);
        //        out.write(compressed);
        return compressed;
      } catch (IOException ioe) {
        ioe.printStackTrace();
      }
    }
    return null;
  }

  public String getCacheName() {
    return cacheName;
  }

  public void flushToCache(Cache localCache) {
    NotifyingFuture result =null;
//    if(nodeMaps.size() == 0){
//      for(Address address :(localCache).getAdvancedCache().getRpcManager().getMembers()){
//        nodeMaps.put(address.toString(),new HashMap<Object, Object>());
//      }
//    }
//    distMan = localCache.getAdvancedCache().getDistributionManager();
    synchronized (mutex){
      if(buffer == null || buffer.size() == 0)
        return;
      //        return null;
      Map<Object,Object> tmp = buffer;
      buffer = new HashMap<>();
      //      Map<Object,Object> tmpb = new HashMap<>();

      //      Address a;
      //      ensembleCacheUtilsSingle.removeCompleted();
      for(Map.Entry<Object,Object> entry : tmp.entrySet()) {
        //
        //        ensembleCacheUtilsSingle.putToCacheDirect(localCache,entry.getKey(),entry.getValue());
        //        a = distMan.getPrimaryLocation(entry.getKey());
        //        nodeMaps.get(a.toString()).put(entry.getKey(),entry.getValue());
        if(ensembleCacheUtilsSingle !=null) {
          ensembleCacheUtilsSingle.putToCacheDirect(localCache, entry.getKey(), entry.getValue());
        }else{
          EnsembleCacheUtils.putToCacheDirect(localCache, entry.getKey(), entry.getValue());
        }
        //        ensembleCacheUtilsSingle.addLocalFuture(localCache.getAdvancedCache().withFlags(Flag.IGNORE_RETURN_VALUES).putAsync(entry.getKey(),entry.getValue()));
      }

      //      for (Map.Entry<String, Map<Object, Object>> entry : nodeMaps.entrySet()) {
      //          if (entry.getValue().size() > 0){
      //            ensembleCacheUtilsSingle.addLocalFuture(localCache.putAllAsync(entry.getValue()));
      //          }
      //        }

    }
    //    return result;
  }

  public void release() {
    buffer.clear();
    //    ensembleCache =  null;
    //    cacheName = null;
  }

  public void setCacheName(String cacheName) {
    this.cacheName = cacheName;
  }

  public Map flushToLocalCache() {
    if(size == 0)
      return null;
    Map result = new HashMap();


      result.put("cache",localCache);
      result.put("distMan",distributionManager);
      result.put("batchPut",batchThreshold);
      synchronized (mutex){
        if (buffer.size() == 0) {
          return null;
        }
        result.put("buffer",buffer);

        buffer = new HashMap<>();
        size = 0;
      }
      return result;

  }
}
