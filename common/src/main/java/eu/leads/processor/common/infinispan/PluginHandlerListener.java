package eu.leads.processor.common.infinispan;

import org.infinispan.client.hotrod.annotation.ClientCacheEntryCreated;
import org.infinispan.client.hotrod.annotation.ClientCacheEntryModified;
import org.infinispan.client.hotrod.annotation.ClientCacheEntryRemoved;
import org.infinispan.client.hotrod.annotation.ClientListener;
import org.infinispan.client.hotrod.event.ClientCacheEntryCustomEvent;

import java.io.Serializable;

/**
 * Created by vagvaz on 9/29/14.
 */
@ClientListener(
                   converterFactoryName = "leads-processor-converter-factory",
                   filterFactoryName = "leads-processor-filter-factory"
)
public class PluginHandlerListener implements Serializable{

    @ClientCacheEntryCreated
    @ClientCacheEntryModified
    @ClientCacheEntryRemoved
    public void handleProcessorEntry(ClientCacheEntryCustomEvent<ProcessorEntry> entry){
        System.err.println("Plugin probably Failed on " + entry.getType() + " " + entry.getEventData().getKey() + " ---> " + entry.getEventData().getValue());
    }
}
