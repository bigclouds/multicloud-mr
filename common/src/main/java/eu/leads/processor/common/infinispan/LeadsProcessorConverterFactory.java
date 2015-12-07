package eu.leads.processor.common.infinispan;


import org.infinispan.notifications.cachelistener.filter.CacheEventConverter;
import org.infinispan.notifications.cachelistener.filter.CacheEventConverterFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by vagvaz on 9/29/14.
 */
public class LeadsProcessorConverterFactory implements CacheEventConverterFactory{
//public class LeadsProcessorConverterFactory implements ConverterFactory {

    Logger log = LoggerFactory.getLogger(LeadsProcessorConverterFactory.class);

    @Override
     public <K, V, C> CacheEventConverter<K, V, C> getConverter(Object[] objects) {
        log.error("Get Converter Called");
        return new LeadsProcessorConverter();
    }
}
