package eu.leads.processor.infinispan;

import eu.leads.processor.common.infinispan.AcceptAllFilter;
import eu.leads.processor.common.infinispan.EnsembleCacheUtils;
import eu.leads.processor.common.infinispan.InfinispanClusterSingleton;
import eu.leads.processor.common.infinispan.InfinispanManager;
import eu.leads.processor.conf.LQPConfiguration;
import eu.leads.processor.core.Tuple;
import org.infinispan.Cache;
import org.infinispan.commons.util.CloseableIterable;
import org.infinispan.context.Flag;

import java.util.*;
import java.util.concurrent.ExecutionException;

/**
 * Created by vagvaz on 16/07/15.
 */
public class IntermediateKeyIndex {
  private static Tuple t;
  private Cache<String, Integer> keysCache;
  private Cache<String, Object> dataCache;
  private int putCounter;

  public IntermediateKeyIndex(Map keysCache, Map dataCache) {
    this.keysCache = (Cache<String, Integer>) keysCache;
    this.dataCache = (Cache<String, Object>) dataCache;
  }

  public void put(String key, Object value) {
    Integer count = null;
    synchronized (this) {
      count = keysCache.get(key);
      if (count == null) {
        count = 0;
      } else {
        count++;
      }
      keysCache.put(key, count);
      //            EnsembleCacheUtils.putToCache(keysCache, key, count);
      //            EnsembleCacheUtils.putToCache(dataCache, key + count.toString(), value);

    }
    dataCache.put(key + count.toString(), value);

    //            dataCache.get(key+count.toString());

  }

  public Iterable<Map.Entry<String, Integer>> getKeysIterator() {
    CloseableIterable iterable =
        keysCache.getAdvancedCache().withFlags(Flag.CACHE_MODE_LOCAL).filterEntries(new AcceptAllFilter());
    return iterable;
  }

  public Iterator<Object> getKeyIterator(String key, Integer counter) {
    return new LocalIndexKeyIterator(key, counter, dataCache);
  }

  public Map<String, Integer> getKeysCache() {
    return keysCache;
  }

  public void setKeysCache(Map<String, Integer> keysCache) {
    this.keysCache = (Cache<String, Integer>) keysCache;
  }

  public Map<String, Object> getDataCache() {
    return dataCache;
  }

  public void setDataCache(Map<String, Object> dataCache) {
    this.dataCache = (Cache<String, Object>) dataCache;
  }

  public void close() {
    keysCache.stop();
    dataCache.stop();
  }

  public static void main(String[] args) {
    LQPConfiguration.initialize();
    EnsembleCacheUtils.initialize();
    InfinispanManager manager = InfinispanClusterSingleton.getInstance().getManager();
    Cache keysCache = manager.getLocalCache("testdb.keys");
    Cache dataCache = manager.getLocalCache("testdb.data");
    IntermediateKeyIndex index = new IntermediateKeyIndex(keysCache, dataCache);
    initTuple();
    int numberofkeys = 200000;
    int numberofvalues = 2;
    String baseKey = "baseKeyString";


    ArrayList<String> tuples = generate(numberofkeys, numberofvalues);
    long start = System.nanoTime();
    System.out.println("insert");
    for (String k : tuples) {
      //            System.out.println("key " + key);
      //            for(int value =0; value < numberofvalues; value++){
      index.put(k, t);
      //            }
    }
    long end = System.nanoTime();
    long dur = end - start;
    dur /= 1000000;
    int total = numberofkeys * numberofvalues;
    double avg = total / (double) dur;
    index.flush();

    System.out.println("Put " + (total) + " in " + (dur) + " avg " + avg);
    int counter = 0;

    //               index.printKeys();
    System.err.println(dataCache.size());
    System.err.println(keysCache.size());
    start = System.nanoTime();
    //        for(int key = 0; key < numberofkeys; key++) {
    int totalcounter = 0;
    for (Map.Entry<String, Integer> entry : index.getKeysIterator()) {
      counter = 0;
      //            System.out.println("iter key "+entry.getKey());
      Iterator<Object> iterator = index.getKeyIterator(entry.getKey(), entry.getValue());
      while (true) {
        try {
          Tuple t = (Tuple) iterator.next();
          //                    String t = (String)iterator.next();
          //                System.out.println(t.getAttribute("key")+" --- " + t.getAttribute("value"));
          counter++;
          totalcounter++;
        } catch (NoSuchElementException e) {
          break;
        }
      }
      if (counter != numberofvalues) {
        System.err.println("Iteration failed for key " + entry.getKey() + " c " + counter);
      }
    }
    end = System.nanoTime();
    dur = end - start;
    dur /= 1000000;
    avg = total / (double) dur;
    System.out.println("Iterate " + (totalcounter) + " in " + (dur) + " avg " + avg);
    index.close();
    System.out.println("exit---");
  }

  private void flush() {
    try {
      EnsembleCacheUtils.waitForAllPuts();
    } catch (InterruptedException e) {
      e.printStackTrace();
    } catch (ExecutionException e) {
      e.printStackTrace();
    }
  }

  private static void initTuple() {
    t = new Tuple();
    int key = 4;
    int value = 5;
    for (int i = 0; i < 10; i++) {
      t.setAttribute("key-" + key + "-" + i, key);
      t.setAttribute("value-" + value + "-" + i, value);
      t.setAttribute("keyvalue-" + key + "." + value + "-" + i, key * value);
    }
  }

  private static ArrayList<String> generate(int numberofkeys, int numberofvalues) {
    String baseKey = "baseKeyString";
    System.out.println("generate");
    ArrayList<String> result = new ArrayList<>(numberofkeys * numberofvalues);
    for (int key = 0; key < numberofkeys; key++) {
      //            System.out.println("key " + key);
      for (int value = 0; value < numberofvalues; value++) {
        result.add(baseKey + key);
      }
    }
    Collections.shuffle(result);
    return result;
  }
}
