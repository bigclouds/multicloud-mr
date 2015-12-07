package eu.leads.processor.common.infinispan;

import eu.leads.processor.common.utils.PrintUtilities;
import eu.leads.processor.common.utils.ProfileEvent;
import org.infinispan.Cache;
import org.infinispan.commons.api.BasicCache;
import org.infinispan.context.Flag;
import org.infinispan.ensemble.cache.EnsembleCache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by vagvaz on 09/08/15.
 */
public class SyncPutRunnable implements Runnable {
    private BasicCache cache;
    private Object key;
    private Object value;
    private Logger logger;
    private ProfileEvent event;
    private boolean remote = true;
    private int retries = 3;
    private EnsembleCacheUtilsSingle owner;
    public SyncPutRunnable(){
        logger = LoggerFactory.getLogger(SyncPutRunnable.class);
        event = new ProfileEvent("SyncPutInit",logger);
    }
    public SyncPutRunnable(BasicCache cache,Object key,Object value){
        logger = LoggerFactory.getLogger(SyncPutRunnable.class);
        event = new ProfileEvent("SyncPutInit",logger);
        this.cache=cache;
        this.key = key;
        this.value = value;
    }

    public SyncPutRunnable(EnsembleCacheUtilsSingle ensembleCacheUtilsSingle) {
        this.owner = ensembleCacheUtilsSingle;
    }

    @Override public void run() {
//        event.start("SyncPut");
        if(key != null && value != null) {
            boolean done = false;
            while (!done) {
                try {
//                    System.err.println("BEF PUT-----Key: " + key + "--Size:" + value.toString().length());
//                    System.out.println("BEF PUT-----Key: " + key + "--Size:" + value.toString().length());
                    if(remote) {
                        cache.put(key, value);
                    }
                    else{
                        ((Cache)cache).getAdvancedCache().withFlags(Flag.IGNORE_RETURN_VALUES).put(key,value);
//                        cache.put(key, value);
                    }
//                    System.err.println("AFT PUT-----Key: " + key + "--Size:" + value.toString().length());
//                    System.out.println("AFT PUT-----Key: " + key + "--Size:" + value.toString().length());
                    done = true;
                } catch (Exception e) {
                    done = false;
                    System.err.println(
                        "puting key " + key + " into " + cache.getName() + " failed for " + e.getClass().toString()
                            + " " + e.getMessage());
                    logger.error(
                        "puting key " + key + " into " + cache.getName() + " failed for " + e.getClass().toString()
                            + " " + e.getMessage());
                    PrintUtilities.logStackTrace(logger, e.getStackTrace());
                    if(retries ==0)
                    {
                        System.err.println("puting key " + key + " into " + cache.getName() + " FAILED ");
                        logger.error("PUTFAILED: puting key " + key + " into " + cache.getName() + " FAILED ");
                    }
                    retries--;
                    try {
                        Thread.sleep(2);
                    } catch (InterruptedException e1) {
                        e1.printStackTrace();
                    }
                }

            }
        }
        this.cache = null;
        this.key= null;
        this.value = null;
        if(owner != null){
            owner.addRunnable(this);
        }else {
            EnsembleCacheUtils.addRunnable(this);
        }
//        event.end();
    }

    public void setParameters(BasicCache cache,Object key, Object value){
        this.cache=cache;
        this.key = key;
        this.value = value;
        remote = (cache instanceof EnsembleCache);
    }


}
