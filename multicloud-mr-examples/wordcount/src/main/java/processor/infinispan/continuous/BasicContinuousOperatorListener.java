package eu.leads.processor.infinispan.continuous;

import eu.leads.processor.common.continuous.BasicContinuousListener;
import eu.leads.processor.common.continuous.EventTriplet;
import eu.leads.processor.common.infinispan.EnsembleCacheUtilsSingle;
import eu.leads.processor.common.utils.PrintUtilities;
import eu.leads.processor.conf.LQPConfiguration;
import eu.leads.processor.plugins.EventType;
import org.infinispan.ensemble.EnsembleCacheManager;
import org.infinispan.ensemble.cache.EnsembleCache;
import org.infinispan.notifications.Listener;
import org.infinispan.notifications.cachelistener.annotation.CacheEntryCreated;
import org.infinispan.notifications.cachelistener.annotation.CacheEntryModified;
import org.infinispan.notifications.cachelistener.annotation.CacheEntryRemoved;
import org.infinispan.notifications.cachelistener.event.CacheEntryCreatedEvent;
import org.infinispan.notifications.cachelistener.event.CacheEntryModifiedEvent;
import org.infinispan.notifications.cachelistener.event.CacheEntryRemovedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vertx.java.core.json.JsonObject;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

/**
 * Created by vagvaz on 10/5/15.
 */
@Listener(sync = true, primaryOnly = true) public class BasicContinuousOperatorListener
    extends BasicContinuousListener {

  protected String ensembleHost;
  protected transient EnsembleCacheManager ensembleCacheManager;
  protected transient EnsembleCache outputCache;
  protected transient ArrayList<LeadsContinuousOperator> operators;
  protected transient ArrayList<LinkedList<Map.Entry>> inputEntry;
  protected transient Logger log;
  protected transient EnsembleCacheUtilsSingle ensembleCacheUtilsSingle;
  protected int parallelism = 1;
  protected transient Class<?> operatorClass = null;

  @Override protected void initializeContinuousListener(JsonObject conf) {
    log = LoggerFactory.getLogger(this.getClass());
    ensembleCacheUtilsSingle = new EnsembleCacheUtilsSingle();
    if (this.conf.containsField("ensembleHost")) {
      ensembleHost = this.conf.getString("ensembleHost");
    }
    if (ensembleHost != null && !ensembleHost.equals("")) {
      log.error("EnsembleHost EXIST " + ensembleHost);
      System.err.println("EnsembleHost EXIST " + ensembleHost);
      ensembleCacheManager = new EnsembleCacheManager(ensembleHost);
      ensembleCacheUtilsSingle.initialize(ensembleCacheManager);
      //      emanager.start();
      //      emanager = createRemoteCacheManager();
      //      ecache = emanager.getCache(output,new ArrayList<>(emanager.sites()),
      //          EnsembleCacheManager.Consistency.DIST);
    } else {
      log.error("EnsembleHost NULL");
      System.err.println("EnsembleHost NULL");
      ensembleCacheManager = new EnsembleCacheManager(LQPConfiguration.getConf().getString("node.ip") + ":11222");
      //      ensembleCacheUtilsSingle.initialize(ensembleCacheManager);
    }
    int parallelism = this.conf.getInteger("parallelism");
    inputEntry = new ArrayList<>();
    operators = new ArrayList<>();
    for (int i = 0; i < parallelism; i++) {
      LeadsContinuousOperator operator = initializeOperator(this.conf);
      operator.setInputCache(inputCache);
      operators.add(operator);
      inputEntry.add(new LinkedList<Map.Entry>());
    }
  }

  public LeadsContinuousOperator initializeOperator(JsonObject conf) {
    String operatorClassName = conf.getString("operatorClass");
    BasicContinuousOperator operator = null;


    if (operatorClassName.equals(WordCountContinuousOperator.class.getCanonicalName().toString())) {
      operator = new WordCountContinuousOperator();
    }else if(operatorClassName.equals(CountMinOperatorContinuous.class.getCanonicalName().toString())){
      operator = new CountMinOperatorContinuous();
    }else if(operatorClassName.equals(KMeansOperatorContinuous.class.getCanonicalName().toString())){
      operator = new KMeansOperatorContinuous();
    }else{
      PrintUtilities.printAndLog(log,"SERIOIS ERROR Continuous class is not KNOWND " + operatorClassName);
    }
    operator.setInputCache(inputCache);
    operator.initializeContinuousOperator(operatorConf);
    return operator;
  }

  @Override protected  void processBuffer() {
    try {
//      System.out.println("Processing buffer BasicContinuousOperatorListner");
      Map map = buffer.getMapAndReset();
      if(map.size() == 0){
        return;
      }
      Iterator iterator = map.entrySet().iterator();
      while (iterator.hasNext()) {
        Map.Entry entry = (Map.Entry) iterator.next();
        iterator.remove();
        int index = (Math.abs(entry.getKey().hashCode()) % parallelism);
        inputEntry.get(index).add(entry);
      }
//      map.clear();
      ArrayList<Future> futures = new ArrayList<>();
      for (int i = 0; i < parallelism; i++) {
        operators.get(i).setInput(inputEntry.get(i).iterator());
        futures.add(operators.get(i).processInput());
      }
      for (Future f : futures) {
        try {
          f.get();
        } catch (InterruptedException e) {
          e.printStackTrace();
        } catch (ExecutionException e) {
          e.printStackTrace();
        }
      }
      for (LinkedList list : inputEntry) {
        list.clear();
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  @Override public void finalizeListener() {
    processBuffer();
    for (LeadsContinuousOperator operator : operators) {
      operator.finalizeOperator();
    }
    operators.clear();
    inputEntry.clear();
    try {
      ensembleCacheManager.stop();
    } catch (Exception e) {
      e.printStackTrace();
    }
  }


//  @CacheEntryCreated @Override public void entryCreated(CacheEntryCreatedEvent event) {
//    try {
//      if (event.isPre()) {
//        return;
//      }
//      if (event.isCommandRetried())
//        return;
//      if (!event.isOriginLocal())
//        return;
//      //      log.error("process key " + event.getKey().toString());
//      //      System.out.println("process key " + event.getKey().toString());
//
//      EventTriplet triplet = new EventTriplet(EventType.CREATED, event.getKey(), event.getValue());
//      synchronized (queueMutex) {
//        eventQueue.add(triplet);
//        queueMutex.notify();
//      }
//    } catch (Exception e) {
//      e.printStackTrace();
//    }
//  }
//
//  @CacheEntryModified @Override public void entryModified(CacheEntryModifiedEvent event) {
//    try {
//      //      log.error("process key " + event.getKey().toString());
//      if (event.isPre()) {
//        return;
//      }
//      if (event.isCreated())
//        return;
//      if (event.isCommandRetried())
//        return;
//      EventTriplet triplet = new EventTriplet(EventType.MODIFIED, event.getKey(), event.getValue());
//      synchronized (queueMutex) {
//        eventQueue.add(triplet);
//        queueMutex.notify();
//      }
//    } catch (Exception e) {
//      e.printStackTrace();
//    }
//  }
//
//  @CacheEntryRemoved @Override public void entryModified(CacheEntryRemovedEvent event) {
//    try {
//      if (event.isCommandRetried())
//        return;
//      //      System.out.println("process key " + event.getKey().toString());
//      //      log.error("process key " + event.getKey().toString());
//      if (event.isPre()) {
//        return;
//      }
//      EventTriplet triplet = new EventTriplet(EventType.REMOVED, event.getKey(), event.getOldValue());
//      synchronized (queueMutex) {
//        eventQueue.add(triplet);
//        queueMutex.notify();
//      }
//    } catch (Exception e) {
//      e.printStackTrace();
//    }
//  }
}
