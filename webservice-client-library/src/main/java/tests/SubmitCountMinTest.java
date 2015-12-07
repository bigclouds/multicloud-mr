package tests;

import eu.leads.processor.common.utils.PrintUtilities;
import eu.leads.processor.conf.LQPConfiguration;
import eu.leads.processor.core.Tuple;
import eu.leads.processor.web.QueryStatus;
import eu.leads.processor.web.WebServiceClient;
import org.infinispan.client.hotrod.RemoteCache;
import org.infinispan.client.hotrod.RemoteCacheManager;
import org.infinispan.client.hotrod.configuration.ConfigurationBuilder;
import org.infinispan.ensemble.EnsembleCacheManager;
import org.infinispan.ensemble.cache.EnsembleCache;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;

import java.io.*;
import java.net.MalformedURLException;
import java.util.*;

/**
 * Created by Apostolos Nydriotis on 2015/06/22.
 */
public class SubmitCountMinTest {

  private static final String DRESDEN2_IP = "80.156.73.116";
  private static final String DD1A_IP = "80.156.222.4";
  private static final String DD2A_IP = "87.190.238.119";
  private static final String SOFTNET_IP = "147.27.14.38";
  private static final String HAMM5_IP = "5.147.254.161";
  private static final String HAMM6_IP = "5.147.254.199";
  private static final String UNINE_IP = "192.42.43.31";
  private static final String CACHE_NAME = "clustered";
  private static final int PUT_THREADS_COUNT = 100;
  private static String host;
  private static int port;
  private static Map<String, String> microcloudAddresses;
  private static Map<String, String> activeIps;
  private static List<String> activeMicroClouds;
  private static String webserviceAddress;
  private static String ensembleString;
  private static Vector<File> files;
  private static String[] resultWords = {"to", "the", "of", "in", "on"};

  private static void putData(String dataDirectory) {

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

//    host = "http://" + DD1A_IP;  // dd1a
    host = "http://" + SOFTNET_IP;

    String propertiesFile = "client.properties";
    if (args.length != 1) {
      PrintUsage();
    } else {
      propertiesFile = args[0];
    }
    LQPConfiguration.getInstance().initialize();
    LQPConfiguration.getInstance().loadFile(propertiesFile);

    host = LQPConfiguration.getInstance().getConfiguration().getString("webservice-address",
                                                                       "http://" + DD1A_IP);
    System.out.println("webservice host: " + host);

    port = 8080;

    String dataPath = LQPConfiguration.getInstance().getConfiguration().getString("data-path", ".");
    System.out.println("data path " + dataPath);

    boolean loadData = LQPConfiguration.getInstance().getConfiguration().getBoolean("load-data",
                                                                                    false);
    System.out.println("load data " + loadData);

    boolean reduceLocal = LQPConfiguration.getInstance().getConfiguration()
        .getBoolean("use-reduce-local", false);
    System.out.println("use reduce local " + reduceLocal);

    boolean combine = LQPConfiguration.getInstance().getConfiguration().getBoolean("use-combine",
                                                                                   true);
    System.out.println("use combine " + combine);

    boolean recComposableReduce = LQPConfiguration.getInstance().getConfiguration()
        .getBoolean("recComposableReduce",false);
    System.out.println("isRecComposableReduce " + recComposableReduce);

    boolean recComposableLocalReduce = LQPConfiguration.getInstance().getConfiguration()
        .getBoolean("recComposableLocalReduce",false);
    System.out.println("isRecComposableLocalReduce " + recComposableLocalReduce);

    double delta = LQPConfiguration.getInstance().getConfiguration().getDouble("delta", 0.02);
    System.out.println("delta " + delta);

    double epsilon = LQPConfiguration.getInstance().getConfiguration().getDouble("epsilon", 0.9);
    System.out.println("epsilon " + epsilon);

    //set the default microclouds
    List<String> defaultMCs = new ArrayList<>(Arrays.asList("softnet", "dd1a", "dd2a", "dresden2"));

    //read the microcloud to run the job
    activeMicroClouds = LQPConfiguration.getInstance().getConfiguration()
        .getList("active-microclouds", defaultMCs);
    System.out.println("active mc ");
    PrintUtilities.printList(activeMicroClouds);
    //initialize default values
    microcloudAddresses = new HashMap<>();
    microcloudAddresses.put("softnet", SOFTNET_IP);
    microcloudAddresses.put("dd1a", DD1A_IP);
    microcloudAddresses.put("dd2a", DD2A_IP);
    microcloudAddresses.put("dresden2", DRESDEN2_IP);
    microcloudAddresses.put("hamm6", HAMM6_IP);
    microcloudAddresses.put("hamm5", HAMM5_IP);


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

    JsonObject jsonObject = new JsonObject();
    jsonObject.putObject("operator", new JsonObject());
    jsonObject.getObject("operator").putObject("configuration", new JsonObject());
    jsonObject.getObject("operator").putString("name", "countMin");
    jsonObject.getObject("operator").putArray("inputs", new JsonArray().add(CACHE_NAME));
    jsonObject.getObject("operator").putString("output", "testOutputCache");
    JsonObject scheduling = getScheduling(activeMicroClouds, activeIps);
    jsonObject.getObject("operator").putObject("scheduling", scheduling);

    if(recComposableReduce) {
      jsonObject.getObject("operator").putString("recComposableReduce", "recComposableReduce");
    }

    if(recComposableLocalReduce) {
      jsonObject.getObject("operator").putString("recComposableLocalReduce",
                                                 "recComposableLocalReduce");
    }

    if (combine) {
      jsonObject.getObject("operator").putString("combine", "1");
    }
    if (reduceLocal) {
      jsonObject.getObject("operator").putString("reduceLocal", "true");
    }

    int[] wd = calculateSketchDimentions(delta, epsilon);

    jsonObject.getObject("operator").getObject("configuration")
        .putNumber("w", wd[0]).putNumber("d", wd[1]);

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

      QueryStatus res = WebServiceClient.executeMapReduceJob(jsonObject, host + ":" + port);
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
      printResults(id, 0);
      //      verifyResults(id, resultWords, ensembleString);
      flushToFile("metrics");
      clearCache("metrics");

      System.out.println("\nDONE IN: " + ((end-start)/1000f) + " sec");
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  private static void flushToFile(String id) throws FileNotFoundException {
    String name = SubmitCountMinTest.class.getSimpleName();
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

  private static void verifyResults(String id, String[] resultWords, String ensembleString) {
    EnsembleCacheManager ensembleCacheManager = new EnsembleCacheManager(ensembleString);
    EnsembleCache cache = ensembleCacheManager
        .getCache(id, new ArrayList<>(ensembleCacheManager.sites()),
                  EnsembleCacheManager.Consistency.DIST);
    for (String word : resultWords) {
      Object result = cache.get(word);
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

  private static int[] calculateSketchDimentions(double delta, double epsilon) {
    int[] wd = new int[2];
    wd[0] = (int) Math.ceil(Math.E / epsilon);
    wd[1] = (int) Math.ceil(Math.log(1d / delta));
    return wd;
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
    System.out.println("java -cp tests.SubmitWordCountTest http://<IP> <PORT> <DATA_DIR>"
                       + " <LOAD_DATA> <REDUCE_LOCAL>");
    System.out.println("Defaults:");
    System.out.println("java -cp tests.SubmitWordCountTest http://80.156.222.4 8080 . false false");
  }

  private static class Putter implements Runnable {

    String id;
    long putCount;

    public Putter(int i) {
      id = String.valueOf(i);
      putCount = 0;
    }

    @Override public void run() {
      int linesPerTupe = 100;
      File f = null;

      EnsembleCacheManager ensembleCacheManager = new EnsembleCacheManager((ensembleString));

      EnsembleCache ensembleCache = ensembleCacheManager
          .getCache(CACHE_NAME, new ArrayList<>(ensembleCacheManager.sites()),
                    EnsembleCacheManager.Consistency.DIST);

      while (true) {
        try {
          f = files.remove(0);
        } catch (Exception e) {
          if (e instanceof ArrayIndexOutOfBoundsException) {
            break;
          }
          e.printStackTrace();
        }

        System.out.println(id + ": files.get(0).getAbsolutePath() = " + f.getAbsolutePath());

        try {
          BufferedReader bufferedReader
              = new BufferedReader(new InputStreamReader(new FileInputStream(f)));

          JsonObject data = new JsonObject();
          String line;

          int lineCount = 0;
          while ((line = bufferedReader.readLine()) != null) {
            data.putString(String.valueOf(lineCount++), line);
            if (lineCount % linesPerTupe == 0) {
              ensembleCache.put(id + "-" + String.valueOf(putCount++), new Tuple(data.toString()));
              data = new JsonObject();
            }
          }
          if (lineCount % linesPerTupe != 0) {
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
}
