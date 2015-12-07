package eu.leads.processor.infinispan;

import eu.leads.processor.common.utils.PrintUtilities;
import eu.leads.processor.core.EngineUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Queue;
import java.util.Map;

/**
 * Created by vagvaz on 8/19/15.
 */
public class ExecuteRunnable implements Runnable {
  Object key;
  Object value;
  private LeadsBaseCallable callable;
  private boolean isRunning = false;
  private static Logger log = LoggerFactory.getLogger(ExecuteRunnable.class);

  public ExecuteRunnable(LeadsBaseCallable callable) {
    this.callable = callable;
  }

  public ExecuteRunnable() {

  }

  public void setKeyValue(Object key, Object value, LeadsBaseCallable callable) {
    this.key = key;
    this.value = value;
    this.callable = callable;
  }

  @Override public void run() {
    isRunning = true;
    int sleep = 0;
    int run = 0;
    try {
      Map.Entry entry = null;
      while (callable.isContinueRunning() || !callable.isEmpty()) {
//        System.err.println(callable.getCallableIndex()+": "+ callable.isContinueRunning() + " " + callable.isEmpty() + " sz " + callable.getSize());
//        System.err.println(callable.getCallableIndex()+" POLLING " + " is " + callable.isContinueRunning() + " " + callable.isEmpty() +" "+ ((Queue)callable.getInput()).size() );
        entry = callable.poll();
        while (entry != null) {
//          System.err.println(callable.getCallableIndex()+" Run " + run++);
//          System.err.println(callable.getCallableIndex()+"INSIDE POLLING " + " is " + callable.isContinueRunning() + " " + callable.isEmpty() +" "+ ((Queue)callable.getInput()).size() );
          key = entry.getKey();
          value = entry.getValue();
//          if(run % 100 == 0)
//            log.error(callable.getCallableIndex()+": EXON  PROCESSED { " + run++ );
          callable.executeOn(key, value);
//          log.error(callable.getCallableIndex()+": EXON  PROCESSED }" );
          entry = callable.poll();
        }
        try {
          if(callable.isContinueRunning() && callable.isEmpty()) {
            synchronized (callable.getInput()) {
//              PrintUtilities.printAndLog(log,
//                  callable.getCallableIndex() + "IN SLEEPING " + " is " + callable.isContinueRunning() + " " + callable
//                      .isEmpty() + " " + ((Queue) callable.getInput()).size());
              callable.getInput().wait();
            }
          }
        } catch (InterruptedException e) {
          e.printStackTrace();
        }

      }
      entry = callable.poll();
      while (entry != null) {
        key = entry.getKey();
        value = entry.getValue();
        callable.executeOn(key, value);
        entry = callable.poll();
//        System.err.println("stuck here ");
//        System.err.println(callable.getCallableIndex()+"INSIDE POLLING " + " is " + callable.isContinueRunning() + " " + callable.isEmpty() +" "+ ((Queue)callable.getInput()).size() );
      }

      callable = null;
      EngineUtils.addRunnable(this);
      isRunning = false;
    }catch (Exception e ){
      e.printStackTrace();
    }
    isRunning = false;
  }

  public <K, V> void setCallable(LeadsBaseCallable<K, V> callable) {
    this.callable = callable;
  }

  public boolean isRunning() {
    return isRunning;
  }

  public void cancel() {
    isRunning = false;
    System.err.println("in cacnel " + callable.getCallableIndex()+": "+ callable.isContinueRunning() + " " + callable.isEmpty() + " sz " + callable.getSize());
    callable.setContinueRunning(false);
    callable.getInput().clear();
  }
}
