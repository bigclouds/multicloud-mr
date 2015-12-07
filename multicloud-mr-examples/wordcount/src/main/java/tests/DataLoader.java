package tests;

import eu.leads.processor.common.infinispan.EnsembleCacheUtils;
import eu.leads.processor.common.utils.PrintUtilities;
import eu.leads.processor.conf.LQPConfiguration;
import eu.leads.processor.core.Tuple;

import org.infinispan.ensemble.EnsembleCacheManager;
import org.infinispan.ensemble.cache.EnsembleCache;
import org.vertx.java.core.json.JsonObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;

/**
 * Created by Apostolos Nydriotis on 2015/10/17.
 */
public class DataLoader {

  public static void main(String[] args) {
    String propertiesFile = "client.properties";
    if (args.length != 1) {
      PrintUsage();
    } else {
      propertiesFile = args[0];
    }
    DataLoader dataLoader = new DataLoader(propertiesFile);
    dataLoader.loadData();
  }

  private static Vector<File> documentFiles;
  private static Vector<File> histogramFiles;
  private boolean loadDocuments;
  private boolean loadHistograms;
  private int putThreadsCount;
  private int linesPerTuple;
  private double gigabytesToLoad;  // GB
  private String dataDirectory;
  private String ensembleString;
  private String documentsCacheName;
  private String histogramsCacheName;

  public DataLoader(String propertiesFile) {
    LQPConfiguration.getInstance().initialize();
    LQPConfiguration.getInstance().loadFile(propertiesFile);

    histogramsCacheName = LQPConfiguration.getInstance().getConfiguration()
        .getString("histograms.cache.name", "default.keywords");

    System.out.println("histogramsCacheName = " + histogramsCacheName);

    loadDocuments = LQPConfiguration.getInstance().getConfiguration().getBoolean("load-documents",
                                                                                 true);
    System.out.println("loadDocuments = " + loadDocuments);

    loadHistograms = LQPConfiguration.getInstance().getConfiguration().getBoolean("load-histograms",
                                                                                  true);
    System.out.println("loadHistograms = " + loadHistograms);

    gigabytesToLoad = LQPConfiguration.getInstance().getConfiguration().getDouble("gb-to-load", 1.0);
    System.out.println("gigabytesToLoad = " + gigabytesToLoad);

    dataDirectory = LQPConfiguration.getInstance().getConfiguration().getString("data-path", ".");
    System.out.println("dataDirectory = " + dataDirectory);

    putThreadsCount = LQPConfiguration.getInstance().getConfiguration().getInt("putter.threads",
                                                                               100);
    System.out.println("putThreadsCount = " + putThreadsCount);

    linesPerTuple = LQPConfiguration.getInstance().getConfiguration()
        .getInt("putter.lines.per.tuple");
    System.out.println("linesPerTuple = " + linesPerTuple);

    documentsCacheName = LQPConfiguration.getInstance().getConfiguration()
        .getString("documents.cache.name", "clustered");
    System.out.println("documentsCacheName = " + documentsCacheName);

    List<String> defaultMCs = new ArrayList<>(Arrays.asList("softnet", "dd1a", "dresden2",
                                                            "hamm6"));
    List<String> activeMicroClouds =
        LQPConfiguration.getInstance().getConfiguration().getList("active-microclouds", defaultMCs);
    System.out.println("active mc ");
    PrintUtilities.printList(activeMicroClouds);

    String dresden2Ip = "80.156.73.116";
    String dd1AIp = "80.156.222.4";
    String hamm5Ip = "5.147.254.161";
    String hamm6Ip = "5.147.254.199";
    String softnetIp = "147.27.14.38";
    String unineΙp = "192.42.43.31";
    String localIp = "127.0.0.1";
    Map<String, String> microcloudAddresses;
    microcloudAddresses = new HashMap<>();
    microcloudAddresses.put("dd1a", dd1AIp);
    microcloudAddresses.put("dresden2", dresden2Ip);
    microcloudAddresses.put("hamm6", hamm6Ip);
    microcloudAddresses.put("hamm5", hamm5Ip);
    microcloudAddresses.put("softnet", softnetIp);
    microcloudAddresses.put("unine", unineΙp);
    microcloudAddresses.put("localcluster", localIp);

    Map<String, String> activeIps = new HashMap<>();
    //read the ips from configuration or use the default
    for (String mc : activeMicroClouds) {
      activeIps.put(mc, LQPConfiguration.getInstance().getConfiguration()
          .getString(mc, microcloudAddresses.get(mc)));
    }
    System.out.println("active ips");
    PrintUtilities.printMap(activeIps);

    ensembleString = "";
    for (String mc : activeMicroClouds) {
      ensembleString += activeIps.get(mc) + ":11222|";
    }

    ensembleString = ensembleString.substring(0, ensembleString.length() - 1);
  }

  public void loadData() {

    if (!loadDocuments && !loadHistograms) {
      return;
    }

    File datasetDirectory = new File(dataDirectory);
    File[] allFiles = datasetDirectory.listFiles();

    Vector<Thread> documentThreads = null;
    Vector<Thread> histogramThreads = null;

    long bytesToLoad = (long) gigabytesToLoad * 1024 * 1024 * 1024;
    bytesToLoad /= putThreadsCount;

    if (loadDocuments) {
      documentFiles = new Vector<>();
      for (File f : allFiles) {
        documentFiles.add(f);
      }
      documentThreads = new Vector<>(putThreadsCount);
      for (int i = 0; i < putThreadsCount; i++) {
        documentThreads.add(new Thread(new DocumentLoader(i, bytesToLoad)));
      }
    }

    if (loadHistograms) {
      histogramFiles = new Vector<>();
      for (File f : allFiles) {
        histogramFiles.add(f);
      }

      EnsembleCacheManager ensembleCacheManager = new EnsembleCacheManager((ensembleString));
      EnsembleCacheUtils.initialize(ensembleCacheManager, false);

      histogramThreads = new Vector<>(putThreadsCount);

      for (int i = 0; i < putThreadsCount; i++) {
        histogramThreads.add(new Thread(new HistogramLoader(i, ensembleCacheManager, bytesToLoad)));
      }
    }

    if (loadDocuments) {
      System.out.println("Loading documents to '" + documentsCacheName + "' cache\n ");
      for (Thread t : documentThreads) {
        t.start();
      }
    }

    if (loadHistograms) {
      System.out.println("Loading histograms to '" + histogramsCacheName + "' cache\n ");
      for (Thread t : histogramThreads) {
        t.start();
      }
    }

    if (loadDocuments) {
      System.out.println("Waiting for document loading to finish...");
      for (Thread t : documentThreads) {
        try {
          t.join();
        } catch (InterruptedException e) {
          e.printStackTrace();
        }
      }
      System.out.println("Documents loaded.");
    }

    if (loadHistograms) {
      System.out.println("Waiting for histogram loading to finish...");
      for (Thread t : histogramThreads) {
        try {
          t.join();
        } catch (InterruptedException e) {
          e.printStackTrace();
        }
      }
      System.out.println("Histograms loaded.");
    }
  }

  /**
   * Loads data for WordCount and CountMin experiments
   */
  private class DocumentLoader implements Runnable {

    String id;
    long putCount;
    private long bytesToLoad;  // per threads bytes to load
    private long bytesLoaded;

    public DocumentLoader(int id, long bytesToLoad) {
      this.id = String.valueOf(id);
      putCount = 0;
      this.bytesToLoad = bytesToLoad;
      this.bytesLoaded = 0;
    }

    @Override
    public void run() {
      File f;

      EnsembleCacheManager ensembleCacheManager = new EnsembleCacheManager((ensembleString));

      EnsembleCache ensembleCache =
          ensembleCacheManager.getCache(documentsCacheName,
                                        new ArrayList<>(ensembleCacheManager.sites()),
                                        EnsembleCacheManager.Consistency.DIST);

      while (true) {
        synchronized (documentFiles) {
          if (documentFiles.size() > 0) {
            f = documentFiles.remove(0);
          } else {
            break;
          }
        }

        System.out.println("[D]" + id + ": f.getName() = " + f.getName());

        try {
          BufferedReader bufferedReader =
              new BufferedReader(new InputStreamReader(new FileInputStream(f)));

          JsonObject data = new JsonObject();
          String line;

          int lineCount = 0;
          while ((line = bufferedReader.readLine()) != null) {
            bytesLoaded += line.length();

            data.putString(String.valueOf(lineCount++), line);
            if (lineCount % linesPerTuple == 0) {

              ensembleCache.put(id + "-" + String.valueOf(putCount++), new Tuple(data.toString()));
              data = new JsonObject();
            }

            if (bytesToLoad >= bytesLoaded) {
              break;
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

  /**
   * Loads data for kMeans experiments
   */
  private class HistogramLoader implements Runnable {

    int id;
    long putCount;
    private EnsembleCacheManager ensembleCacheManager;
    private long bytesToLoad;  // per threads bytes to load
    private long bytesLoaded;

    public HistogramLoader(int id, EnsembleCacheManager ensembleCacheManager, long bytesToLoad) {
      this.id = id;
      putCount = 0;
      this.ensembleCacheManager = ensembleCacheManager;
      this.bytesToLoad = bytesToLoad;
      this.bytesLoaded = 0;
    }

    @Override
    public void run() {
      File f;

      EnsembleCache ensembleCache =
          ensembleCacheManager.getCache(histogramsCacheName,
                                        new ArrayList<>(ensembleCacheManager.sites()),
                                        EnsembleCacheManager.Consistency.DIST);
      boolean documentStarted = false;
      boolean isWiki = false;
      int documentsCount = 0;  // documents added by this thread

      while (true) {
        synchronized (histogramFiles) {
          if (histogramFiles.size() > 0) {
            f = histogramFiles.remove(0);
          } else {
            break;
          }
        }

        System.out.println("[H]" + id + ": f.getName() = " + f.getName());

        if (f.getName().endsWith(".wiki")) {
          isWiki = true;
        }

        try {
          BufferedReader bufferedReader =
              new BufferedReader(new InputStreamReader(new FileInputStream(f)));

          String line;
          Map<String, Double> frequencies = null;

          if (!isWiki) {
            frequencies = new HashMap<>();
          }

          while ((line = bufferedReader.readLine()) != null) {

            if (!documentStarted && isWiki) {
              if (line.startsWith(" doc id")) {
                documentStarted = true;
                frequencies = new HashMap<>();
              } else {
                continue;
              }
            }

            if (line.equals(" doc") && isWiki) {
              documentStarted = false;
              try {
                frequencies.put("~", Double.valueOf(String.valueOf(id)
                                                    + String.valueOf(documentsCount++)));
              } catch (Exception e) {
                e.printStackTrace();
              }

              Tuple data = new Tuple();
              data.asBsonObject().putAll(frequencies);
              EnsembleCacheUtils.putToCache(ensembleCache, String.valueOf(id) + "-"
                                                           + String.valueOf(putCount++), data);
              System.out.println("putting WIKI document " + String.valueOf(id)
                                 + String.valueOf(documentsCount));
              continue;
            }

            if (bytesLoaded >= bytesToLoad) {
              continue;
            }

            String[] words = line.split(" ");
            bytesLoaded += line.length();

            for (String word : words) {
              if (word.length() == 0) {
                continue;
              }
              Double wordFrequency = frequencies.get(word);
              if (wordFrequency == null) {
                frequencies.put(word, 1d);
              } else {
                frequencies.put(word, wordFrequency + 1);
              }
            }
          }

          if (!isWiki) {
            try {
              frequencies.put("~", Double.valueOf(String.valueOf(id)
                                                  + String.valueOf(documentsCount++)));
            } catch (Exception e) {
              e.printStackTrace();
            }

            Tuple data = new Tuple();
            data.asBsonObject().putAll(frequencies);
            EnsembleCacheUtils.putToCache(ensembleCache, String.valueOf(id) + "-"
                                                         + String.valueOf(putCount++), data);
            System.out.println("putting NON-WIKI document " + String.valueOf(id)
                               + String.valueOf(documentsCount));
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

  private static void PrintUsage() {
    System.out.println("java -cp tests.DataLoader <client.properties>");
  }
}
