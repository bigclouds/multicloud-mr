package eu.leads.processor.infinispan.operators;

import eu.leads.processor.common.infinispan.InfinispanManager;
import eu.leads.processor.common.utils.storage.LeadsStorage;
import eu.leads.processor.core.Action;
import eu.leads.processor.core.comp.LogProxy;
import eu.leads.processor.core.net.Node;
import eu.leads.processor.infinispan.GenericLocalReducerCallable;
import eu.leads.processor.infinispan.GenericMapperCallable;
import eu.leads.processor.infinispan.GenericReducerCallable;
import eu.leads.processor.infinispan.LeadsCollector;
import org.infinispan.Cache;
import org.infinispan.commons.api.BasicCache;
import org.vertx.java.core.json.JsonObject;

import java.io.ByteArrayOutputStream;

/**
 * Created by vagvaz on 3/31/15.
 */
public class GenericMapReduceOperator extends MapReduceOperator {
  public GenericMapReduceOperator(Node com, InfinispanManager persistence, LogProxy log, Action action,
      LeadsStorage storage) {
    super(com, persistence, log, action);
    conf.putString("storageType", storage.getStorageType());
    ByteArrayOutputStream storageConfigurationStream = new ByteArrayOutputStream();
    try {
      storage.getConfiguration().store(storageConfigurationStream, "");
    } catch (Exception e) {
      e.printStackTrace();
    }
    conf.putBinary("storageConfiguration", storageConfigurationStream.toByteArray());
  }

  @Override public void init(JsonObject config) {
    super.init(config);

    //Read Mapper class
    //read mapjar path
    //read reducer class
    //read reducer path
    //read Config path or //read config
    //Storage //storagetype
    //tmpdir prefix

  }

  @Override public String getContinuousListenerClass() {
    return null;
  }


  @Override public void setupMapCallable() {
    conf.putString("output", getOutput());
    inputCache = (Cache) manager.getPersisentCache(inputCacheName);
    //    intermediateCache = (BasicCache) manager.getPersisentCache(intermediateCacheName);
    //create Intermediate cache name for data on the same Sites as outputCache
    //    intermediateDataCache = (BasicCache) manager.getPersisentCache(intermediateCacheName+".data");
    //create Intermediate  keys cache name for data on the same Sites as outputCache;
    //    keysCache = (BasicCache)manager.getPersisentCache(intermediateCacheName+".keys");
    //createIndexCache for getting all the nodes that contain values with the same key! in a mc
    //    indexSiteCache = (BasicCache)manager.getPersisentCache(intermediateCacheName+".indexed");
    //    indexSiteCache = (BasicCache)manager.getIndexedPersistentCache(intermediateCacheName+".indexed");
    //    outputCache = (BasicCache) manager.getPersisentCache(outputCacheName);
    //    reduceInputCache = (Cache) keysCache;
    //vagvaz    collector = new LeadsCollector(0, intermediateCacheName);
    //vagvaz    mapperCallable = new LeadsMapperCallable((Cache) inputCache,collector,mapper,
    //vagvaz                                   LQPConfiguration.getInstance().getMicroClusterName());
    if (reduceLocal) {
      collector = new LeadsCollector(1000, intermediateLocalCacheName);
    } else {
      collector = new LeadsCollector(1000, intermediateCacheName);
    }
    mapperCallable = new GenericMapperCallable(intermediateCacheName, conf.toString());
    ////                                                   ((Cache) inputCache,collector,mapper,
    ////                                                                          LQPConfiguration.getInstance().getMicroClusterName());
  }

  @Override public void setupReduceCallable() {
    conf.putString("output", getOutput());
    //    intermediateCache = (BasicCache) manager.getPersisentCache(intermediateCacheName);
    //    log.error("ReducerIntermediate " + intermediateCache.size());
    //create Intermediate cache name for data on the same Sites as outputCache
    intermediateDataCache = (BasicCache) manager.getPersisentCache(intermediateCacheName + ".data");
    //    log.error("ReducerIntermediateData " + intermediateDataCache.size());
    //create Intermediate  keys cache name for data on the same Sites as outputCache;
    //    keysCache = (BasicCache)manager.getPersisentCache(intermediateCacheName+".keys");
    //    log.error("ReducerIntermediateKeys " + keysCache.size());
    //createIndexCache for getting all the nodes that contain values with the same key! in a mc
    //    indexSiteCache = (BasicCache)manager.getPersisentCache(intermediateCacheName+".indexed");
    //    indexSiteCache = (BasicCache)manager.getIndexedPersistentCache(intermediateCacheName+".indexed");
    //    log.error("ReducerIntermediateSite " + indexSiteCache.size());
    outputCache = (BasicCache) manager.getPersisentCache(outputCacheName);
    //    reduceInputCache = (Cache) keysCache;
    collector = new LeadsCollector(0, outputCache.getName());
    inputCache = (Cache) intermediateDataCache;
    reduceInputCache = inputCache;
    //    inputCache = (Cache) keysCache;
    reducerCallable = new GenericReducerCallable(getOutput(), conf.toString());
    ////                                                         (outputCache.getName(), reducer,
    ////                                                                                 intermediateCacheName);
  }

  @Override public void setupReduceLocalCallable() {
    // TODO(ap0n): conf.putString("output", getOutput());
    intermediateLocalDataCache = (BasicCache) manager.getPersisentCache(intermediateLocalCacheName + ".data");
    log.error("ReducerIntermediateLocalData " + intermediateLocalDataCache.size());
    outputCache = (BasicCache) manager.getPersisentCache(intermediateCacheName);
    reduceLocalInputCache = (Cache) intermediateLocalCache;

    collector = new LeadsCollector(1000, outputCache.getName());

    //         reducerLocalCallable = new LeadsLocalReducerCallable(outputCache.getName(), localReducer,
    //             intermediateLocalCacheName, LQPConfiguration
    //             .getInstance().getMicroClusterName());
    reducerLocalCallable = new GenericLocalReducerCallable(outputCache.getName(), conf.toString(),intermediateCacheName);
    combiner = null;
  }
}
//   @Override
//   public void run() {
//      long startTime = System.nanoTime();
//      if(reducer == null)
//         reducer = new LeadsReducer("");
//      System.out.println("RUN MR on " + inputCache.getName());
//      //       MapReduceTask<String,String,String,String> task = new MapReduceTask(inputCache);
//      //               .reducedWith((org.infinispan.distexec.mapreduce.Reducer<String, String>) reducer);
//      //       task.timeout(1, TimeUnit.HOURS);
//      //       task.execute();
//
//      DistributedExecutorService des = new DefaultExecutorService((Cache<?, ?>) inputCache);
//
//      GenericMapperCallable mapperCallable = new GenericMapperCallable(conf.toString(),intermediateCacheName);
////                                                   ((Cache) inputCache,collector,mapper,
////                                                                          LQPConfiguration.getInstance().getMicroClusterName());
//      DistributedTaskBuilder builder =des.createDistributedTaskBuilder(mapperCallable);
//      builder.timeout(24, TimeUnit.HOURS);
//      DistributedTask task = builder.build();
//      List<Future<?>> res = des.submitEverywhere(task);
//      try {
//         if (res != null) {
//            for (Future<?> result : res) {
//               result.get();
//            }
//            System.out.println("mapper Execution is done");
//         }
//         else
//         {
//            System.out.println("mapper Execution not done");
//         }
//      } catch (InterruptedException e) {
//         e.printStackTrace();
//      } catch (ExecutionException e) {
//         e.printStackTrace();
//      }
//      System.err.println("keysCache " + keysCache.size());
//      System.err.println("dataCache " + intermediateDataCache.size());
//      System.err.println("indexedCache " + indexSiteCache.size());
//      if(reducer != null) {
//
//         GenericReducerCallable reducerCacllable = new GenericReducerCallable(conf.toString(),getOutput());
////                                                         (outputCache.getName(), reducer,
////                                                                                 intermediateCacheName);
//         DistributedExecutorService des_inter = new DefaultExecutorService((Cache<?, ?>) keysCache);
//         DistributedTaskBuilder reduceTaskBuilder = des_inter.createDistributedTaskBuilder(reducerCacllable);
//         reduceTaskBuilder.timeout(1,TimeUnit.HOURS);
//         DistributedTask reduceTask = reduceTaskBuilder.build();
//         List<Future<?>> reducers_res= des_inter
//                                               .submitEverywhere(reduceTask);
//         try {
//            if (reducers_res != null) {
//               for (Future<?> result : reducers_res) {
//                  System.err.println("wait " + System.currentTimeMillis());
//                  System.err.println(result.get());
//                  System.err.println("wait end" + System.currentTimeMillis());
//               }
//               System.out.println("reducer Execution is done");
//            } else {
//               System.out.println("reducer Execution not done");
//            }
//         } catch (InterruptedException e) {
//            e.printStackTrace();
//         } catch (ExecutionException e) {
//            e.printStackTrace();
//         }
//      }
//      //Store Values for statistics
//      updateStatistics(inputCache,null,outputCache);
//      cleanup();
//   }
//}
