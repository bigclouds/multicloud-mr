package eu.leads.processor.infinispan;

import org.infinispan.filter.KeyValueFilter;
import org.infinispan.metadata.Metadata;

import java.io.Serializable;

/**
 * Created by vagvaz on 4/7/15.
 */
public class IndexedComplexIntermKeyFilter implements KeyValueFilter<Object, Object>, Serializable {
  private String filterKey;

  public IndexedComplexIntermKeyFilter(String key) {
    this.filterKey = key;
  }

  @Override public boolean accept(Object key, Object value, Metadata metadata) {
    IndexedComplexIntermediateKey val = (IndexedComplexIntermediateKey) key;
    if (val.getKey().equals(filterKey)) {
      return true;
    }
    return false;
  }



}
