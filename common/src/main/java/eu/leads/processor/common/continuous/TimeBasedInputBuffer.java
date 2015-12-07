package eu.leads.processor.common.continuous;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.RemovalListener;

import java.util.Iterator;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;

/**
 * Created by vagvaz on 10/4/15.
 */
public class TimeBasedInputBuffer implements InputBuffer {
  private RemovalListener removalListener;
  private Cache data;
  private long timeWindow;
  private boolean actionResult;
  private Timer timer;
  public TimeBasedInputBuffer(long timeWindow){
    this.timeWindow = timeWindow;
    timer = new Timer("TimeBasedInputBuffer-"+timeWindow);
    timer.schedule(new TimerTask() {
      @Override public void run() {
        actionResult = true;
      }
    },0,timeWindow);
    data = CacheBuilder.newBuilder().expireAfterWrite(timeWindow, TimeUnit.MILLISECONDS).build();
  }

  @Override public  boolean add(Object key, Object value) {
    boolean result = actionResult;
    data.put(key,value);
    if(result){
      actionResult = false;
    }
    return result;
  }

  @Override public  boolean remove(Object key) {
    boolean result = actionResult;
    data.invalidate(key);
    if(result){
      actionResult = false;
    }
    return result;
  }

  @Override public  boolean modify(Object key, Object value) {
    boolean result = actionResult;
    data.put(key,value);
    if(result){
      actionResult = false;
    }
    return result;
  }

  @Override public  boolean isEmpty() {
    return (data.size() == 0);
  }

  @Override public  int size() {
    return (int) data.size();
  }

  @Override public  boolean isFull() {
    return false;
  }

  @Override public  void setRemovalListener(RemovalListener removalListener) {
    this.removalListener = removalListener;
    resetDataCache();
  }

  @Override public RemovalListener getRemovalListener() {
    return removalListener;
  }

  @Override public  Map getMapAndReset() {
    Map result = data.asMap();
    resetDataCache();
    return result;
  }

  @Override public  void clear() {
    data.invalidateAll();
  }

  @Override public  Map reset() {
    Map result = data.asMap();
    resetDataCache();
    return result;

  }

  private void resetDataCache() {
    if(removalListener != null){
      data = CacheBuilder.newBuilder().expireAfterWrite(timeWindow,TimeUnit.MILLISECONDS).removalListener(removalListener).build();
    }else{
      data = CacheBuilder.newBuilder().expireAfterWrite(timeWindow,TimeUnit.MILLISECONDS).build();
    }
  }
}
