package eu.leads.processor.infinispan;

import eu.leads.processor.common.infinispan.InfinispanManager;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

/**
 * Created by vagvaz on 04/07/15.
 */
public class LocalCollector<K, V> extends LeadsCollector<K, V> {
  public LocalCollector(int maxCollectorSize, String collectorCacheName) {
    super(maxCollectorSize, collectorCacheName);
    combinedValues = new HashMap();
  }

  public LocalCollector(int maxCollectorSize, String cacheName, InfinispanManager manager) {
    super(maxCollectorSize, cacheName, manager);
    combinedValues = new HashMap();
  }

  @Override public void emit(K key, V value) {
    List<V> values = combinedValues.get(key);
    if (values == null) {
      values = new LinkedList<>();
      combinedValues.put(key, values);
    }
    values.add(value);

  }

  @Override
  public void reset(){
    combinedValues.clear();
  }
}
