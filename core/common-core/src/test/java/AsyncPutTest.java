import eu.leads.processor.common.infinispan.EnsembleCacheUtils;
import eu.leads.processor.common.infinispan.InfinispanClusterSingleton;
import eu.leads.processor.common.infinispan.InfinispanManager;
import eu.leads.processor.conf.LQPConfiguration;
import eu.leads.processor.infinispan.ComplexIntermediateKey;
import eu.leads.processor.infinispan.IndexedComplexIntermediateKey;
import eu.leads.processor.infinispan.LeadsIntermediateIterator;
import org.infinispan.client.hotrod.RemoteCacheManager;
import org.infinispan.client.hotrod.configuration.ConfigurationBuilder;
import org.infinispan.commons.api.BasicCache;
import org.infinispan.ensemble.EnsembleCacheManager;

import java.util.ArrayList;
import java.util.NoSuchElementException;
import java.util.concurrent.ExecutionException;

/**
 * Created by vagvaz on 06/07/15.
 */
public class AsyncPutTest {
  static BasicCache indexedCache;
  static BasicCache dataCache;
  static BasicCache keysCache;
  static String[] nodes = null;//{"node0","node1","node2","node3","node00"};//,"node11","node22","node33"};
  static String[] microClouds = null;//{"mc0"};//,"mc1"};//,"mc2","mc01","mc11","mc21"};
  static String[] keys;
  static BasicCache[] caches = new BasicCache[4];
  static String cacheName = "acache";
  static int numOfkeys = 20; //10
  static int numOfNodes = 10; //10
  static int numOfMicroClouds = 10; //10
  static int valuesPerKey = 20;
  static int numOfCaches = 4;
  //    static RemoteCacheManager rmanager;
  static EnsembleCacheManager rmanager;

  public static void main(String[] args) throws ExecutionException, InterruptedException {
    LQPConfiguration.initialize();
    LQPConfiguration.getInstance().getConfiguration().setProperty("node.current.component", "testsync");
    EnsembleCacheUtils.initialize();
    //        rmanager = createRemoteCacheManager();

    InfinispanManager manager = InfinispanClusterSingleton.getInstance().getManager();
    //    InfinispanManager manager11 = CacheManagerFactory.createCacheManager();
    //    InfinispanManager manager12 = CacheManagerFactory.createCacheManager();
    //    InfinispanManager manager13 = CacheManagerFactory.createCacheManager();
    //    InfinispanManager manager14 = CacheManagerFactory.createCacheManager();
    // create required caches
    indexedCache = (BasicCache) manager.getPersisentCache("prefix.indexed");
    dataCache = (BasicCache) manager.getPersisentCache("prefix.data");
    keysCache = (BasicCache) manager.getPersisentCache("keysCache");
    for (int i = 0; i < numOfCaches; i++) {
      caches[i] = (BasicCache) manager.getPersisentCache("cache" + i);
    }
    rmanager =
        new EnsembleCacheManager(LQPConfiguration.getInstance().getConfiguration().getString("node.ip") + ":11222");
    //get remote caches in order to emulate ensemble!
    indexedCache =
        rmanager.getCache("prefix.indexed", new ArrayList<>(rmanager.sites()), EnsembleCacheManager.Consistency.DIST);
    dataCache =
        rmanager.getCache("prefix.data", new ArrayList<>(rmanager.sites()), EnsembleCacheManager.Consistency.DIST);
    keysCache =
        rmanager.getCache("keysCache", new ArrayList<>(rmanager.sites()), EnsembleCacheManager.Consistency.DIST);

    //generate keys
    keys = new String[numOfkeys];
    for (int index = 0; index < numOfkeys; index++) {
      keys[index] = "key" + index;
      keysCache.put(keys[index], keys[index]);
    }

    //generate nodes and micro clouds
    nodes = new String[numOfNodes];
    microClouds = new String[numOfMicroClouds];

    for (int i = 0; i < numOfMicroClouds; i++) {
      microClouds[i] = "mc" + i;
    }

    for (int i = 0; i < numOfNodes; i++) {
      nodes[i] = "node" + i;
    }
    //generate intermediate keyValuePairs
    long start = System.currentTimeMillis();
    generateIntermKeyValue(keysCache, dataCache, indexedCache, valuesPerKey, keys, nodes, microClouds);
    EnsembleCacheUtils.waitForAllPuts();
    long end = System.currentTimeMillis();
    System.out.println(
        "Put " + (valuesPerKey * numOfkeys * numOfMicroClouds * numOfNodes) + " in " + ((end - start) / 1000)
            + " secs");
    int counter = 0;

    System.err.println("Start Iteration");
    start = System.currentTimeMillis();
    for (String k : keys) {
      int keyCounter = 0;
      //initialize iterator
      System.out.println("key: " + k);
      LeadsIntermediateIterator iterator = new LeadsIntermediateIterator(k, "prefix",
          InfinispanClusterSingleton.getInstance().getManager());//,numOfMicroClouds*numOfNodes);

      while (true) {
        try {
          //          System.out.println(keyCounter + ": " + iterator.next().toString());
          iterator.next();
          keyCounter++;
          counter++;
          //                    if(keyCounter % 10 == 0)
          //                        System.out.print(".");
          //                    if(keyCounter % 100 == 0)
          //                        System.err.println();
        } catch (Exception e) {
          if (e instanceof NoSuchElementException) {
            //            System.err.println("End of Iteration");
            break;
          }
          e.printStackTrace();
        }
      }
      //check if key iterated correctly
      if (keyCounter != (valuesPerKey * numOfMicroClouds * numOfNodes)) {
        System.err.println("key " + k + " iteration size: " + keyCounter + " instead of "
            + valuesPerKey * numOfMicroClouds * numOfNodes + " number of missed iterations"
            + (valuesPerKey * numOfMicroClouds * numOfNodes - keyCounter) / valuesPerKey);
      }
    }
    System.err.println(
        "\nTotal counted " + counter + " total " + keys.length * microClouds.length * nodes.length * valuesPerKey);
    end = System.currentTimeMillis();
    System.out.println(
        "Iterate " + (valuesPerKey * numOfkeys * numOfMicroClouds * numOfNodes) + " in " + ((end - start) / 1000)
            + " secs");
    InfinispanClusterSingleton.getInstance().getManager().stopManager();
    System.exit(0);
  }

  private static void generateIntermKeyValue(BasicCache keysCache, BasicCache dataCache, BasicCache indexedCache,
      int valuesPerKey, String[] keys, String[] nodes, String[] microClouds) {
    //    IndexedComplexIntermediateKey indexedKey = new IndexedComplexIntermediateKey();
    //    ComplexIntermediateKey ikey = new ComplexIntermediateKey();
    for (String node : nodes) {
      System.out.println("node " + node);
      for (String site : microClouds) {
        System.out.println("site " + site);
        for (String key : keys) {
          EnsembleCacheUtils.putToCache(keysCache, key, key);
          IndexedComplexIntermediateKey indexedKey = new IndexedComplexIntermediateKey();
          indexedKey.setKey(key);
          indexedKey.setNode(node);
          indexedKey.setSite(site);
          indexedKey.setCache(cacheName);
          for (int counter = 0; counter < valuesPerKey; counter++) {
            ComplexIntermediateKey ikey = new ComplexIntermediateKey();
            ikey.setCounter(counter);
            ikey.setKey(key);
            ikey.setSite(site);
            ikey.setNode(node);
            ikey.setCache(cacheName);
            EnsembleCacheUtils.putToCache(indexedCache, indexedKey, indexedKey);
            EnsembleCacheUtils.putToCache(dataCache, ikey, ikey);
            for (int i = 0; i < numOfCaches; i++) {
              EnsembleCacheUtils.putToCache(caches[i], indexedKey, indexedKey);
              EnsembleCacheUtils.putToCache(caches[i], ikey, ikey);
            }
          }
          //                    System.out.println("Generated " + node + " " + site + " " + key);
        }
      }
    }

  }

  private static RemoteCacheManager createRemoteCacheManager() {
    ConfigurationBuilder builder = new ConfigurationBuilder();
    builder.addServer().host(LQPConfiguration.getConf().getString("node.ip")).port(11222);
    return new RemoteCacheManager(builder.build());
  }

}
