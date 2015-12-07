package eu.leads.processor.core;

import eu.leads.processor.common.infinispan.InfinispanClusterSingleton;
import eu.leads.processor.common.utils.PrintUtilities;
import eu.leads.processor.conf.LQPConfiguration;
import eu.leads.processor.infinispan.ExecuteRunnable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Created by vagvaz on 8/22/15.
 */
public class EngineUtils {
  private static Logger log = LoggerFactory.getLogger(EngineUtils.class);
  private static int threadBatch;
  private static ThreadPoolExecutor executor;
  private static ConcurrentLinkedDeque<ExecuteRunnable> runnables;
  private static volatile Object mutex = new Object();
  private static boolean initialized = false;

  public static void initialize() {
    synchronized (mutex) {
      if (initialized) {
        return;
      }

      //            threadBatch = LQPConfiguration.getInstance().getConfiguration().getInt(
      //                "node.engine.threads", 1);
      threadBatch = LQPConfiguration.getInstance().getConfiguration().getInt("node.engine.parallelism", 4);
      System.out.println("Executor threads " + threadBatch + "  ");
      initialized = true;
      executor = new ThreadPoolExecutor((int) threadBatch, (int) (threadBatch), 2000, TimeUnit.MILLISECONDS,
          new LinkedBlockingDeque<Runnable>());
      runnables = new ConcurrentLinkedDeque<>();
      for (int i = 0; i < (threadBatch); i++) {
        runnables.add(new ExecuteRunnable());
      }
    }
  }

  public static ExecuteRunnable getRunnable() {
    ExecuteRunnable result = null;
    //        synchronized (runnableMutex){
    PrintUtilities.printAndLog(log,
        InfinispanClusterSingleton.getInstance().getManager().getMemberName().toString() + ": GETRunnable " + runnables
            .size());
    result = runnables.poll();
    while (result == null) {
      Thread.yield();
      //                            try {
      //            //                    Thread.sleep(1);
      //            Thread.sleep(0,10000);
      //                            } catch (InterruptedException e) {
      //                                e.printStackTrace();
      //                            }
      //            PrintUtilities.printAndLog(log,InfinispanClusterSingleton.getInstance().getManager().getMemberName().toString() + ": whileGETRunnable " + runnables.size());

      result = runnables.poll();
      //            }
    }

    return result;
  }

  public static void addRunnable(ExecuteRunnable runnable) {
    //                synchronized (runnableMutex){
    runnables.add(runnable);
    //            runnableMutex.notify();
    //        }
    PrintUtilities.printAndLog(log,
        InfinispanClusterSingleton.getInstance().getManager().getMemberName().toString() + ": addRunnable " + runnables
            .size());
  }

  public static void waitForAllExecute() {

    while (runnables.size() != threadBatch)
      //            System.out.println("sleeping because run " + runnables.size() + " and " + threadBatch );
      try {
        //            executor.awaitTermination(100,TimeUnit.MILLISECONDS);
        Thread.sleep(100);
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
  }

  public static void submit(ExecuteRunnable runable) {
    executor.submit(runable);
  }
}
