package eu.leads.processor.common;

import com.google.common.cache.RemovalListener;
import com.google.common.cache.RemovalNotification;

/**
 * Created by vagvaz on 10/1/15.
 */
public class LeadsRemovalListener implements RemovalListener {


  @Override public void onRemoval(RemovalNotification notification) {
    process(notification.getKey(), notification.getValue());
  }

  protected void process(Object key, Object value) {
    System.err.println("key: " + key.toString() + " -> " + value.toString());
  }
}
