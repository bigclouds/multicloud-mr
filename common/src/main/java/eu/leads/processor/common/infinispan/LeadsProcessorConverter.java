package eu.leads.processor.common.infinispan;

import org.infinispan.metadata.Metadata;
import org.infinispan.notifications.cachelistener.filter.CacheEventConverter;
import org.infinispan.notifications.cachelistener.filter.EventType;

import java.io.Serializable;


/**
 * Created by vagvaz on 9/29/14.
 */
public class LeadsProcessorConverter implements CacheEventConverter,Serializable {


  @Override
  public Object convert(Object key, Object oldValue, Metadata oldMetadata, Object newValue,
                         Metadata newMetadata, EventType eventType) {
    return new ProcessorEntry(key,newValue);
  }
}
