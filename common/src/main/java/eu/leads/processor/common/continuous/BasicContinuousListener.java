package eu.leads.processor.common.continuous;

import com.google.common.base.Strings;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.RemovalListener;
import eu.leads.processor.common.LeadsListener;
import eu.leads.processor.common.LeadsRemovalListener;
import eu.leads.processor.common.infinispan.InfinispanManager;
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

import java.util.Map;
import java.util.Queue;
import java.util.concurrent.TimeUnit;

/**
 * Created by vagvaz on 10/1/15.
 */

public abstract class BasicContinuousListener implements LeadsListener {

  protected transient InfinispanManager imanager;
  protected transient JsonObject conf;
  protected transient JsonObject operatorConf;
  protected transient InputBuffer buffer;
  protected transient Queue eventQueue;
  protected transient RemovalListener removalListener;
  protected String confString;
  protected transient volatile Object mutex = new Object();
  protected transient volatile Object queueMutex = new Object();
  protected boolean isFlushed = false;
  protected transient ContinuousProcessingThread processingThread;
  protected transient org.infinispan.Cache inputCache;
  @Override public InfinispanManager getManager() {
    return imanager;
  }

  @Override public void setManager(InfinispanManager manager) {
    imanager = manager;
  }

  @Override public void initialize(InfinispanManager manager, JsonObject conf) {
    if(Strings.isNullOrEmpty(confString)) {
      this.conf = conf;
    }else{
      this.conf = new JsonObject(confString);
    }
    imanager = manager;
    if(mutex == null){
      mutex = new Object();
    }
    if(queueMutex == null){
      queueMutex = new Object();
    }

    eventQueue = new ConcurrentDiskQueue(1000);
    if (this.conf.containsField("operator")) {
      operatorConf = this.conf.getObject("operator");
    }
    if(this.conf.containsField("window")){
      String windowType = this.conf.getString("window");
      long windowSize = 1;
      if(this.conf.containsField("windowSize")){
        windowSize = this.conf.getNumber("windowSize").longValue();
      }
      if(windowType.equals("sizeBased")){
        buffer = new SizeBasedInputBuffer((int) windowSize);
      }else if (windowType.equals("timeBased")){
        buffer = new TimeBasedInputBuffer(windowSize);
      }else{
        buffer = new SimpleInputBuffer(1);
      }
    }
    if(this.conf.containsField("input")){
      inputCache = (org.infinispan.Cache) imanager.getPersisentCache(this.conf.getString("input"));
    }else{
      inputCache = imanager.getCacheManager().getCache();
    }
    initializeContinuousListener(this.conf);
    processingThread = new ContinuousProcessingThread(this);
    processingThread.start();
    if(removalListener != null){
      buffer.setRemovalListener(removalListener);
    }
  }

  protected abstract void initializeContinuousListener(JsonObject conf);

  protected abstract void processBuffer() ;

  public abstract void finalizeListener();

  @Override public void initialize(InfinispanManager manager) {
    initialize(manager,null);
  }

  @Override public String getId() {
    return this.getClass().toString();
  }

  @Override public void close() {
    buffer.clear();
    try {
      if(processingThread.isAlive()){
        synchronized (queueMutex){
          queueMutex.notify();
        }
      }
      processingThread.join();
    } catch (InterruptedException e) {
      e.printStackTrace();
    }
  }

  public void flush() {
    isFlushed = true;
    while(!eventQueue.isEmpty() || !buffer.isEmpty()){
      synchronized (mutex){
        try {
          synchronized (queueMutex){
            queueMutex.notifyAll();
          }
          mutex.wait();
          if(!eventQueue.isEmpty() && !buffer.isEmpty()){
            System.err.println("FLUSH woke up and evenqt que " + eventQueue.isEmpty() + " buffer size " + buffer.isEmpty() + " " + buffer.size());
          }
        } catch (InterruptedException e) {
          e.printStackTrace();
        }
      }
    }
    close();
  }

  @Override public void setConfString(String s) {
    this.confString = s;
  }

  @CacheEntryCreated
  public void entryCreated(CacheEntryCreatedEvent event){
    if(event.isPre()){
      return;
    }
//    System.out.println("process key " + event.getKey().toString());
    EventTriplet triplet = new EventTriplet(EventType.CREATED,event.getKey(),event.getValue());
    synchronized (queueMutex) {
      eventQueue.add(triplet);
      queueMutex.notify();
    }
  }

  @CacheEntryModified
  public void entryModified(CacheEntryModifiedEvent event){
    if(event.isPre()){
      return;
    }
    EventTriplet triplet = new EventTriplet(EventType.MODIFIED,event.getKey(),event.getValue());
    synchronized (queueMutex){
      eventQueue.add(triplet);
      queueMutex.notify();
    }
  }

  @CacheEntryRemoved
  public void entryModified(CacheEntryRemovedEvent event){
    if(event.isPre()){
      return;
    }
    EventTriplet triplet = new EventTriplet(EventType.REMOVED,event.getKey(),event.getOldValue());
    synchronized (queueMutex){
      eventQueue.add(triplet);
      queueMutex.notify();
    }
  }

  public Queue getEventQueue() {
    return eventQueue;
  }

  public InputBuffer getBuffer() {
    return buffer;
  }

  public boolean getIsFlushed() {
    return isFlushed;
  }

  public Object getMutex() {
    return queueMutex;
  }

  public void signal() {
    synchronized (mutex){
      mutex.notifyAll();
    }
  }

}
