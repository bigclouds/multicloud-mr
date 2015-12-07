package eu.leads.processor.common.continuous;

import com.google.common.cache.RemovalListener;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Created by vagvaz on 10/4/15.
 */
public class SizeBasedInputBuffer implements InputBuffer {
  private Map data;
  private int maximumSize;
  private RemovalListener listener;

  public SizeBasedInputBuffer(int maximumSize){
    this.maximumSize = maximumSize;
    data = new HashMap(maximumSize);
  }
  @Override public  boolean add(Object key, Object value) {
    data.put(key,value);
    return isOverflown();
  }

  private boolean isOverflown() {
    return data.size() >= maximumSize;
  }

  @Override public  boolean remove(Object key) {
    data.remove(key);
    return isOverflown();
  }

  @Override public  boolean modify(Object key, Object value) {
    return this.add(key,value);
  }

  @Override public  boolean isEmpty() {
    return data.isEmpty();
  }

  @Override public  int size() {
    return data.size();
  }

  @Override public  boolean isFull() {
    return isOverflown();
  }

  @Override public void setRemovalListener(RemovalListener removalListener) {
    //do nothing
  }

  @Override public RemovalListener getRemovalListener() {
    return null;
  }

  @Override public  Map getMapAndReset() {
    Map result = data;
    data = new HashMap();
    return result;
  }

  @Override public  void clear() {
    data.clear();
  }

  @Override public  Map reset() {
    Map result = data;
    data  = new HashMap(maximumSize);
    return result;
  }
}
