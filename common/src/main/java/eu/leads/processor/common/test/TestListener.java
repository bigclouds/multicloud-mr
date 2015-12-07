package eu.leads.processor.common.test;

import org.infinispan.notifications.Listener;
import org.infinispan.notifications.cachelistener.annotation.CacheEntryCreated;
import org.infinispan.notifications.cachelistener.annotation.CacheEntryModified;
import org.infinispan.notifications.cachelistener.event.CacheEntryEvent;

import java.io.Serializable;

/**
 * Created by vagvaz on 6/3/14.
 */
@Listener
public class TestListener implements Serializable {
    String testSerial = null;

    public TestListener(String draw, ComplexType c) {
        testSerial = draw;

    }


    @CacheEntryCreated
    @CacheEntryModified
    public void entry(CacheEntryEvent event) {
        if (event.isPre()) {
            return;
        }

        System.out
            .println(testSerial + " " + event.getKey().toString() + " -> " + event.getValue());
        //        System.out.println(testSerial +" "+ cmp.getConfig().getProperty((String) cmp.getConfig().getKeys().next())+ " " + event.getKey().toString() + " -> " + event.getValue());

    }
}
