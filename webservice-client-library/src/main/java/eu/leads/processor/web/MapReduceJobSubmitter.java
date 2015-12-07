package eu.leads.processor.web;

import data.LineDataLoader;
import eu.leads.processor.ResultSetIterator;
import eu.leads.processor.common.utils.PrintUtilities;
import eu.leads.processor.conf.LQPConfiguration;
import eu.leads.processor.infinispan.MapReduceJob;
import org.infinispan.client.hotrod.RemoteCache;
import org.infinispan.client.hotrod.RemoteCacheManager;
import org.infinispan.client.hotrod.configuration.ConfigurationBuilder;
import org.infinispan.ensemble.EnsembleCacheManager;
import org.infinispan.ensemble.Site;
import org.infinispan.ensemble.cache.EnsembleCache;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;

import java.io.FileNotFoundException;
import java.io.RandomAccessFile;
import java.net.MalformedURLException;
import java.util.*;

/**
 * Created by vagvaz on 12/3/15.
 */
public class MapReduceJobSubmitter {
  static String LOCALHOST_IP = "127.0.0.1";
  private static String inputCache = "clustered";
  private static String[] activeMicroClouds;
  private static Map<String,String> activeIps = new HashMap<>();
  private static String configurationFile = null;
  private static String jarFile = null;
  private static String name = null;

  public static void main(String[] args) {
    String propertiesFile = "mapreduce.properties";
    if (args.length != 1) {
      PrintUsage();
    } else {
      propertiesFile = args[0];
    }
    LQPConfiguration.getInstance().initialize();
    LQPConfiguration.getInstance().loadFile(propertiesFile);
    String host = LQPConfiguration.getInstance().getConfiguration()
        .getString("webservice-address", "http://" + LOCALHOST_IP);
    System.out.println("webservice host: " + host);
    int port = 8080;
    String dataPath = LQPConfiguration.getInstance().getConfiguration()
        .getString("data-path", ".");  // "/home/ap0n/Desktop/tmp-dataset"
    boolean loadData = LQPConfiguration.getInstance().getConfiguration().getBoolean("load-data",
        false);
    configurationFile = LQPConfiguration.getInstance().getConfiguration().getString("configuration-path",null);
    System.out.println("data path " + dataPath);

    inputCache = LQPConfiguration.getInstance().getConfiguration().getString("input-cache",null);
    jarFile    = LQPConfiguration.getInstance().getConfiguration().getString("jar-path",null);
    name  =  LQPConfiguration.getInstance().getConfiguration().getString("job-name",null);
    String mapperClass = LQPConfiguration.getInstance().getConfiguration().getString("mapper",null);
    String combinerClass = LQPConfiguration.getInstance().getConfiguration().getString("combiner",null);
    String reducerClass = LQPConfiguration.getInstance().getConfiguration().getString("reducer",null);
    String localReducerClass = LQPConfiguration.getInstance().getConfiguration().getString("local-reducer",null);
    boolean reduceLocal = LQPConfiguration.getInstance().getConfiguration()
        .getBoolean("use-reduce-local", false);
    System.out.println("use reduce local " + reduceLocal);
    boolean combine = LQPConfiguration.getInstance().getConfiguration().getBoolean("use-combine",
        true);
    boolean recComposableReduce = LQPConfiguration.getInstance().getConfiguration()
        .getBoolean("pipelineReduce", false);
    System.out.println("pipelineReduce " + recComposableReduce);

    boolean recComposableLocalReduce = LQPConfiguration.getInstance().getConfiguration()
        .getBoolean("pipelineLocalReduce", false);
    System.out.println("pipelineLocalReduce " + recComposableLocalReduce);

    System.out.println("use combine " + combine);
    //set the default microclouds
    List<String> defaultMCs = new ArrayList<>(Arrays.asList("localhost"));
    //read the microcloud to run the job
    List<String> activeMicroClouds = LQPConfiguration.getInstance().getConfiguration()
        .getList("active-microclouds", defaultMCs);
    System.out.println("active mc ");
    PrintUtilities.printList(activeMicroClouds);
    //initialize default values
    Map<String,String> microcloudAddresses = new HashMap<>();

    microcloudAddresses.put("localcluster",LOCALHOST_IP);

    Map<String,String> activeIps = new HashMap<>();
    //read the ips from configuration or use the default
    for (String mc : activeMicroClouds) {
      activeIps.put(mc, LQPConfiguration.getInstance().getConfiguration()
          .getString(mc, microcloudAddresses.get(mc)));
    }
    System.out.println("active ips");
    PrintUtilities.printMap(activeIps);

    try {
      if (WebServiceClient.initialize(host, port)) {
        System.out.println("Client is up");
      }
    } catch (MalformedURLException e) {
      e.printStackTrace();
    }

    LQPConfiguration.initialize();
    MapReduceJob job = new MapReduceJob();
    job.setName("wordCount");
    job.setBuiltIn(false);
    job.addInput(inputCache);
    job.setOutput("testOuputCache");
    if(jarFile != null){
      job.setJarPath(jarFile);
    }
    if(name != null){
      job.setName(name);
    }
    if(configurationFile != null){
      job.setConfigurationFromFile(configurationFile);
    }
    if(mapperClass != null){
      job.setMapperClass(mapperClass);
    }
    if(combinerClass != null){
      job.setCombinerClass(combinerClass);
    }
    if(reducerClass != null){
      job.setReducerClass(reducerClass);
    }
    if(localReducerClass != null){
      job.setLocalReducerClass(localReducerClass);
    }

//    JsonObject jsonObject = new JsonObject();
//    jsonObject.putObject("operator", new JsonObject());
//    jsonObject.getObject("operator").putObject("configuration", new JsonObject());
//    jsonObject.getObject("operator").putString("name", "wordCount");
//    jsonObject.getObject("operator").putArray("inputs", new JsonArray().add(CACHE_NAME));
//    jsonObject.getObject("operator").putString("output", "testOutputCache");
    //    jsonObject.getObject("operator").putArray("inputMicroClouds",
    //                                              new JsonArray().add("localcluster"));
    //    jsonObject.getObject("operator").pqutArray("outputMicroClouds",
    //                                              new JsonArray().add("localcluster"));
    JsonObject scheduling = getScheduling(activeMicroClouds, activeIps);
//    jsonObject.getObject("operator").putObject("scheduling", scheduling);
    job.setInputMicroClouds(scheduling);
    if(recComposableReduce) {
//      jsonObject.getObject("operator").putString("recComposableReduce", "recComposableReduce");
      job.setPipelineReduce(true);
    }

    if (combine) {
//      jsonObject.getObject("operator").putString("combine", "1");
      job.setUseCombine(true);
    }
    if (reduceLocal) {
      job.setUseLocalReduce(true);
//      jsonObject.getObject("operator").putString("reduceLocal", "true");
      if(recComposableLocalReduce) {
//        jsonObject.getObject("operator").putString("recComposableLocalReduce",
//            "recComposableLocalReduce");
        job.setPipelineReduceLocal(true);
      }
    }


    JsonObject targetEndpoints = scheduling;
//    jsonObject.getObject("operator").putObject("targetEndpoints", targetEndpoints);
    job.setOutputMicroClouds(targetEndpoints);

    try {
     String ensembleString = "";
      for (String mc : activeMicroClouds) {
        ensembleString += activeIps.get(mc) + ":11222|";
      }

      ensembleString = ensembleString.substring(0, ensembleString.length() - 1);

      if (loadData) {
        LineDataLoader.putData(dataPath,inputCache,ensembleString);
//        putData(dataPath);
      }

      QueryStatus res = WebServiceClient.executeMapReduceJob(job.asJsonObject(), host + ":" + port);
      String id = res.getId();
      System.out.println("Submitted job. id: " + id);
      System.out.println("Executing...");

      int secs = 0;
      long start = System.currentTimeMillis();
      while (true) {
        QueryStatus status = WebServiceClient.getQueryStatus(id);
        if (status.getStatus().equals("COMPLETED")) {
          break;
        } else if (status.getErrorMessage() != null && status.getErrorMessage().length() > 0) {
          System.out.println(status.getErrorMessage());
          break;
        } else {
          System.out.print("\r" + secs++);
          Thread.sleep(1000);
        }
      }
      long end = System.currentTimeMillis();
      printResults(activeIps,id, 10);
//      verifyResults(id, resultWords, ensembleString);
      System.out.println("\nDONE IN: " + ((end-start)/1000f) + " sec");

    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  private static JsonObject getScheduling(List<String> activeMicroClouds,
      Map<String, String> activeIps) {
    JsonObject result = new JsonObject();
    for (String mc : activeMicroClouds) {
      result.putArray(mc, new JsonArray().add(activeIps.get(mc)));
    }
    return result;
  }

//  private static Object getKeyFrom(EnsembleCache ensembleCache, Object key) {
//    Object result = null;
//    for (Object s : ensembleCache.sites()) {
//      EnsembleCache siteCache = ((Site) s).getCache(ensembleCache.getName());
//      result = siteCache.get(key);
//      if (result != null) {
//        return result;
//      }
//    }
//    return result;
//  }
//
//  private static void verifyResults(String id, String[] resultWords, String ensembleString) {
//    EnsembleCacheManager ensembleCacheManager = new EnsembleCacheManager(ensembleString);
//    EnsembleCache cache = ensembleCacheManager
//        .getCache(id, new ArrayList<>(ensembleCacheManager.sites()),
//            EnsembleCacheManager.Consistency.DIST);
//    for (String word : resultWords) {
//      Object result = getKeyFrom(cache,word);
//      if (result != null) {
//        System.out.println(word + "--->" + result.toString());
//      } else {
//        System.out.println(word + " NULL");
//      }
//    }
//  }


  private static void printResults(String id) {
    printResults(id, -1);
  }

  private static void printResults(String id, int numOfItes){

    //    for (String mc : activeMicroClouds) {
    //      System.out.println(mc);
    //      RemoteCacheManager remoteCacheManager = createRemoteCacheManager(activeIps.get(mc));
    //      RemoteCache results = remoteCacheManager.getCache(id);
    //      if (numOfItems > 0) {
    //        PrintUtilities.printMap(results, numOfItems);
    //      } else {
    //        PrintUtilities.printMap(results);
    //      }
    //    }

  }

  private static void printResults(Map<String,String> microcloudIPs,String id, int numOfItems) {

    ResultSetIterator iterator = new ResultSetIterator(microcloudIPs.values(),id);
    int counter = numOfItems;
    while(counter > 0 && iterator.hasNext()){
      Map.Entry entry = iterator.next();
      System.out.println("key: " + entry.getKey().toString() + " --> " + entry.getValue().toString());
      counter--;
    }
  }

  private static void PrintUsage() {
    System.out
        .println("java -cp jar className configurationFile");
  }
}
