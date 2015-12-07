package data;

import eu.leads.processor.conf.LQPConfiguration;
import eu.leads.processor.core.Tuple;
import org.infinispan.ensemble.EnsembleCacheManager;
import org.infinispan.ensemble.cache.EnsembleCache;
import org.vertx.java.core.json.JsonObject;

import java.io.*;
import java.util.ArrayList;
import java.util.Vector;

/**
 * Created by vagvaz on 12/6/15.
 */
public class LineDataLoader {
  private static  int PUT_THREADS_COUNT = 100;

  static
  Vector<File> files;
  private static class Putter implements Runnable {
     String cacheName;
     String ensembleString;
    String id;
    long putCount;
    private int linesPerTuple = 1000;

    public Putter(int i) {
      id = String.valueOf(i);
      putCount = 0;
    }

    public Putter(int i, String cacheName, String ensembleString) {
      id = String.valueOf(i);
      putCount = 0;
      this.cacheName = cacheName;
      this.ensembleString = ensembleString;
    }

    @Override public void run() {
      LQPConfiguration.initialize();
      linesPerTuple = LQPConfiguration.getInstance().getConfiguration()
          .getInt("putter.lines.per.tuple",linesPerTuple);

      File f;

      EnsembleCacheManager ensembleCacheManager = new EnsembleCacheManager((ensembleString));

      EnsembleCache ensembleCache =
          ensembleCacheManager.getCache(cacheName, new ArrayList<>(ensembleCacheManager.sites()),
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

  public static void putData(String dataDirectory,String cacheName,String ensembleString) {

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
      threads.add(new Thread(new Putter(i,cacheName,ensembleString)));
    }

    System.out.print("Loading data to '" + cacheName + "' cache\n ");

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
    if(args.length != 3){
      System.err.println("run jar directory cachename  ensemblestring");
      System.exit(-1);
    }
    putData(args[0],args[1],args[2]);
    System.out.println("Data loaded");
    System.exit(0);
  }
}
