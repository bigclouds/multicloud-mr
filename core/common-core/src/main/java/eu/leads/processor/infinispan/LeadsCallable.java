package eu.leads.processor.infinispan;

/**
 * Created by vagvaz on 2/18/15.
 */
public interface LeadsCallable<K, V> {
  void executeOn(K key, V value);

  void initialize();

  void finalizeCallable();

}
