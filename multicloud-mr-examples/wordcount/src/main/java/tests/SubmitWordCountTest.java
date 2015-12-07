package tests;

import eu.leads.processor.common.utils.PrintUtilities;
import eu.leads.processor.conf.LQPConfiguration;
import eu.leads.processor.core.Tuple;
import eu.leads.processor.infinispan.MapReduceJob;
import eu.leads.processor.web.QueryStatus;
import eu.leads.processor.web.WebServiceClient;
import org.infinispan.client.hotrod.RemoteCache;
import org.infinispan.client.hotrod.RemoteCacheManager;
import org.infinispan.client.hotrod.configuration.ConfigurationBuilder;
import org.infinispan.ensemble.EnsembleCacheManager;
import org.infinispan.ensemble.Site;
import org.infinispan.ensemble.cache.EnsembleCache;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;

import java.io.*;
import java.net.MalformedURLException;
import java.util.*;

/**
 * Created by Apostolos Nydriotis on 2015/06/22.
 */
public class SubmitWordCountTest {

  private static String host;
  private static int port;
  private static final String DRESDEN2_IP = "80.156.73.116";
  private static final String DD1A_IP = "80.156.222.4";
  private static final String HAMM5_IP = "5.147.254.161";
  private static final String HAMM6_IP = "5.147.254.199";
  private static final String SOFTNET_IP = "147.27.14.38";
  private static final String UNINE_IP = "192.42.43.31";
  private static final String LOCAL = "127.0.0.1";
  private static final String CACHE_NAME = "clustered";
  private static  int PUT_THREADS_COUNT = 100;
  private static Map<String, String> microcloudAddresses;
  private static Map<String, String> activeIps;
  private static List<String> activeMicroClouds;
  private static String webserviceAddress;
  private static String ensembleString;
  private static Vector<File> files;
  private static String[] resultWords = {"to", "the", "of", "in", "on"};


  private static class Putter implements Runnable {

    String id;
    long putCount;
    private int linesPerTuple = 1000;

    public Putter(int i) {
      id = String.valueOf(i);
      putCount = 0;
    }

    @Override public void run() {
      LQPConfiguration.initialize();
      linesPerTuple = LQPConfiguration.getInstance().getConfiguration()
          .getInt("putter.lines.per.tuple",linesPerTuple);

      File f;

      EnsembleCacheManager ensembleCacheManager = new EnsembleCacheManager((ensembleString));

      EnsembleCache ensembleCache =
          ensembleCacheManager.getCache(CACHE_NAME, new ArrayList<>(ensembleCacheManager.sites()),
              EnsembleCacheManager.Consistency.DIST);

      while (true) {
        synchronized (files) {
          if (files.size() > 0) {
            f = files.get(0);
            files.remove(0);
          } else {
            break;
          }
        }

        System.out.println(id + ": files.get(0).getAbsolutePath() = " + f.getAbsolutePath());

        try {
          BufferedReader bufferedReader =
              new BufferedReader(new InputStreamReader(new FileInputStream(f)));

          JsonObject data = new JsonObject();
          String line;

          int lineCount = 0;
          while ((line = bufferedReader.readLine()) != null) {
            data.putString(String.valueOf(lineCount++), line);
            if (lineCount % linesPerTuple == 0) {
              ensembleCache.put(id + "-" + String.valueOf(putCount++), new Tuple(data.toString()));
              data = new JsonObject();
            }
          }
          if (lineCount % linesPerTuple != 0) {
            // put the remaining lines
            ensembleCache.put(id + "-" + String.valueOf(putCount++), new Tuple(data.toString()));
          }

          bufferedReader.close();
        } catch (FileNotFoundException e) {
          e.printStackTrace();
        } catch (IOException e) {
          e.printStackTrace();
        }
      }
    }
  }

  private static void putData(String dataDirectory) {

    PUT_THREADS_COUNT = LQPConfiguration.getInstance().getConfiguration().getInt("putter.threads",
                                                                                 PUT_THREADS_COUNT);
    File datasetDirectory = new File(dataDirectory);
    File[] allFiles = datasetDirectory.listFiles();
    files = new Vector<File>();

    for (File f : allFiles) {
      files.add(f);
    }

    Vector<Thread> threads = new Vector<>(PUT_THREADS_COUNT);

    for (int i = 0; i < PUT_THREADS_COUNT; i++) {
      threads.add(new Thread(new Putter(i)));
    }

    System.out.print("Loading data to '" + CACHE_NAME + "' cache\n ");

    for (Thread t : threads) {
      t.start();
    }

    for (Thread t : threads) {
      try {
        t.join();
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
    }
  }

  public static void main(String[] args) {

    host = "http://" + SOFTNET_IP;  // softnet

    String propertiesFile = "client.properties";
    if (args.length != 1) {
      PrintUsage();
    } else {
      propertiesFile = args[0];
    }
    LQPConfiguration.getInstance().initialize();
    LQPConfiguration.getInstance().loadFile(propertiesFile);
    host = LQPConfiguration.getInstance().getConfiguration()
        .getString("webservice-address", "http://" + DD1A_IP);
    System.out.println("webservice host: " + host);
    port = 8080;
    String dataPath = LQPConfiguration.getInstance().getConfiguration()
        .getString("data-path", ".");  // "/home/ap0n/Desktop/tmp-dataset"
    System.out.println("data path " + dataPath);
    boolean loadData = LQPConfiguration.getInstance().getConfiguration().getBoolean("load-data",
                                                                                    false);
    System.out.println("load data " + loadData);
    boolean reduceLocal = LQPConfiguration.getInstance().getConfiguration()
        .getBoolean("use-reduce-local", false);
    System.out.println("use reduce local " + reduceLocal);
    boolean combine = LQPConfiguration.getInstance().getConfiguration().getBoolean("use-combine",
                                                                                   true);
    boolean recComposableReduce = LQPConfiguration.getInstance().getConfiguration()
        .getBoolean("recComposableReduce", false);
    System.out.println("isRecComposableReduce " + recComposableReduce);

    boolean recComposableLocalReduce = LQPConfiguration.getInstance().getConfiguration()
        .getBoolean("recComposableLocalReduce", false);
    System.out.println("isRecComposableLocalReduce " + recComposableLocalReduce);

    System.out.println("use combine " + combine);
    //set the default microclouds
    List<String> defaultMCs = new ArrayList<>(Arrays.asList("softnet", "dd1a", "dresden2",
                                                            "hamm6"));
    //read the microcloud to run the job
    activeMicroClouds = LQPConfiguration.getInstance().getConfiguration()
        .getList("active-microclouds", defaultMCs);
    System.out.println("active mc ");
    PrintUtilities.printList(activeMicroClouds);
    //initialize default values
    microcloudAddresses = new HashMap<>();
    microcloudAddresses.put("dd1a", DD1A_IP);
    microcloudAddresses.put("dresden2", DRESDEN2_IP);
    microcloudAddresses.put("hamm6", HAMM6_IP);
    microcloudAddresses.put("hamm5", HAMM5_IP);
    microcloudAddresses.put("softnet", SOFTNET_IP);
    microcloudAddresses.put("localcluster",LOCAL);


    activeIps = new HashMap<>();
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
    job.setBuiltIn(true);
    job.addInput(CACHE_NAME);
    job.setOutput("testOuputCache");

    JsonObject jsonObject = new JsonObject();
    jsonObject.putObject("operator", new JsonObject());
    jsonObject.getObject("operator").putObject("configuration", new JsonObject());
    jsonObject.getObject("operator").putString("name", "wordCount");
    jsonObject.getObject("operator").putArray("inputs", new JsonArray().add(CACHE_NAME));
    jsonObject.getObject("operator").putString("output", "testOutputCache");
    //    jsonObject.getObject("operator").putArray("inputMicroClouds",
    //                                              new JsonArray().add("localcluster"));
    //    jsonObject.getObject("operator").pqutArray("outputMicroClouds",
    //                                              new JsonArray().add("localcluster"));
    JsonObject scheduling = getScheduling(activeMicroClouds, activeIps);
    jsonObject.getObject("operator").putObject("scheduling", scheduling);

    if(recComposableReduce) {
      jsonObject.getObject("operator").putString("recComposableReduce", "recComposableReduce");
      job.setPipelineReduce(true);
    }



    if (combine) {
      jsonObject.getObject("operator").putString("combine", "1");
      job.setUseCombine(true);
    }
    if (reduceLocal) {
      job.setUseLocalReduce(true);
      jsonObject.getObject("operator").putString("reduceLocal", "true");
      if(recComposableLocalReduce) {
        jsonObject.getObject("operator").putString("recComposableLocalReduce",
                                                   "recComposableLocalReduce");
        job.setPipelineReduceLocal(true);
      }
    }


    JsonObject targetEndpoints = scheduling;
    jsonObject.getObject("operator").putObject("targetEndpoints", targetEndpoints);


    try {
      ensembleString = "";
      for (String mc : activeMicroClouds) {
        ensembleString += activeIps.get(mc) + ":11222|";
      }

      ensembleString = ensembleString.substring(0, ensembleString.length() - 1);

      if (loadData) {
        putData(dataPath);
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
      printResults(id, 5);
      verifyResults(id, resultWords, ensembleString);
      flushToFile("metrics");
      clearCache("metrics");

      System.out.println("\nDONE IN: " + ((end-start)/1000f) + " sec");

    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  private static void flushToFile(String id) throws FileNotFoundException {
    String name = SubmitWordCountTest.class.getSimpleName();
    Date date = new Date();
    String filename = name+"-"+date.toString()+".txt";
    flushToFile(id,filename);
  }

  private static void flushToFile(String id, String filename) throws FileNotFoundException {
    RandomAccessFile ram = new RandomAccessFile(filename,"rw");
    for (String mc : activeMicroClouds) {
      System.out.println(mc);
      RemoteCacheManager remoteCacheManager = createRemoteCacheManager(activeIps.get(mc));
      RemoteCache results = remoteCacheManager.getCache(id);
      PrintUtilities.saveMapToFile(results,filename);
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

  private static Object getKeyFrom(EnsembleCache ensembleCache, Object key) {
    Object result = null;
    for (Object s : ensembleCache.sites()) {
      EnsembleCache siteCache = ((Site) s).getCache(ensembleCache.getName());
      result = siteCache.get(key);
      if (result != null) {
        return result;
      }
    }
    return result;
  }

  private static void verifyResults(String id, String[] resultWords, String ensembleString) {
    EnsembleCacheManager ensembleCacheManager = new EnsembleCacheManager(ensembleString);
    EnsembleCache cache = ensembleCacheManager
        .getCache(id, new ArrayList<>(ensembleCacheManager.sites()),
                  EnsembleCacheManager.Consistency.DIST);
    for (String word : resultWords) {
      Object result = getKeyFrom(cache,word);
      if (result != null) {
        System.out.println(word + "--->" + result.toString());
      } else {
        System.out.println(word + " NULL");
      }
    }
  }

  private static RemoteCacheManager createRemoteCacheManager(String host) {
    ConfigurationBuilder builder = new ConfigurationBuilder();
    builder.addServer().host(host).port(11222);
    return new RemoteCacheManager(builder.build());
  }

  private static void printProgress(int i) {
    if (i % 6 == 0 || i % 6 == 3) {
      System.out.print("\r" + i + " |");
    } else if (i % 6 == 1) {
      System.out.print("\r" + i + " \\");
    } else if (i % 6 == 2 || i % 6 == 5) {
      System.out.print("\r" + i + " \u2014");
    } else if (i % 6 == 4) {
      System.out.print("\r" + i + " /");
    }
  }

  private static void printResults(String id) {
    printResults(id, -1);
  }

  private static void printResults(String id, int numOfItems) {
    for (String mc : activeMicroClouds) {
      System.out.println(mc);
      RemoteCacheManager remoteCacheManager = createRemoteCacheManager(activeIps.get(mc));
      RemoteCache results = remoteCacheManager.getCache(id);
      if (numOfItems > 0) {
        PrintUtilities.printMap(results, numOfItems);
      } else {
        PrintUtilities.printMap(results);
      }
    }

  }
  private static void clearCache(String id) {
    for (String mc : activeMicroClouds) {
      System.out.println(mc);
      RemoteCacheManager remoteCacheManager = createRemoteCacheManager(activeIps.get(mc));
      RemoteCache results = remoteCacheManager.getCache(id);
      results.clear();
    }
  }
  private static void PrintUsage() {
    System.out
        .println("java -cp tests.SubmitWordCountTest http://<IP> <PORT> <DATA_DIR>"
                 + " <LOAD_DATA> <REDUCE_LOCAL>");
    System.out.println("Defaults:");
    System.out.println("java -cp tests.SubmitWordCountTest http://80.156.222.4 8080 . false false");
  }
}
