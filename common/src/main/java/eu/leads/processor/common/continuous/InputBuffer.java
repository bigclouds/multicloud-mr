package eu.leads.processor.common.continuous;

import com.google.common.cache.RemovalListener;

import java.util.Iterator;
import java.util.Map;

/**
 * Created by vagvaz on 10/4/15.
 */
public interface InputBuffer {
  public boolean add(Object key, Object value);
  public boolean remove(Object key);
  public boolean modify(Object key,Object value);
  public boolean isEmpty();
  public int size();
  public boolean isFull();
  public void setRemovalListener(RemovalListener removalListener);
  public RemovalListener getRemovalListener();
  public Map getMapAndReset();
  public void clear();
  public Map reset();
}
