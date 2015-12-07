package eu.leads.processor.common.infinispan;

import eu.leads.processor.common.utils.PrintUtilities;
import eu.leads.processor.common.utils.ProfileEvent;
import org.infinispan.commons.api.BasicCache;
import org.infinispan.commons.util.concurrent.NotifyingFuture;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * Created by vagvaz on 20/05/15.
 */
public class BatchPutAllAsyncThread implements Runnable {

    private final Map<String, Map<Object, Object>> objects;
    private final Map<String, BasicCache> caches;
    private Map<NotifyingFuture, String> backup;
    private List<NotifyingFuture> futures;
    private Logger log;
    private ProfileEvent batchPut;
    public BatchPutAllAsyncThread(Map<String, BasicCache> caches,
        Map<String, Map<Object, Object>> objects) {
//        super("Thread-" + UUID.randomUUID().toString());
        this.caches = caches;
        this.objects = objects;
        futures = new ArrayList<>();
        backup = new HashMap<>();
        log = LoggerFactory.getLogger(BatchPutAllAsyncThread.class);

    }


    @Override public void run() {
//        super.run();
        batchPut = new ProfileEvent("batchPut",log);
        for(Map.Entry<String,Map<Object,Object>> entry : objects.entrySet()){
            BasicCache cache = caches.get(entry.getKey());
            NotifyingFuture nextFuture = cache.putAllAsync(entry.getValue());
            futures.add(nextFuture);
            backup.put(nextFuture, cache.getName());
        }
        List<NotifyingFuture> failed = null;
        while (!backup.isEmpty()) {
            failed = new ArrayList<>();
            //            Iterator<Map.Entry<NotifyingFuture,String>> iterator = backup.entrySet().iterator();
            for (NotifyingFuture future : futures) {
                try {
                    future.get();
                    backup.remove(future);
                } catch (InterruptedException e) {
                    //                    e.printStackTrace();
                    log.error(e.getClass().toString());
                    PrintUtilities.logStackTrace(log, e.getStackTrace());
                    failed.add(future);
                } catch (Exception e) {
                    //                    e.printStackTrace();
                    log.error(e.getClass().toString());
                    PrintUtilities.logStackTrace(log, e.getStackTrace());
                    e.printStackTrace();
                    PrintUtilities.logStackTrace(log, e.getStackTrace());
                    failed.add(future);
                }

            }
            futures.clear();
            for (NotifyingFuture failedFuture : failed) {
                //for the failed redo the action
                //Get Cache for that future
                System.err.println("EnsembleRetrying putting data to " + backup.get(failedFuture));
                BasicCache cache = caches.get(backup.get(failedFuture));

                //Get Map that we need to put
                Object ob = objects.get(cache.getName());
                System.err.println("Size of retrying map = " + ((Map)ob).size());
                //Remove old Future from Future backup
                backup.remove(failedFuture);

                //                    BasicCache cache = caches.get(backup.get(future));
                //Reddo operation
                NotifyingFuture nextFuture = cache.putAllAsync((Map) ob);
                futures.add(nextFuture);
                backup.put(nextFuture, cache.getName());
            }
        }
        caches.clear();
        objects.clear();
        batchPut.end();
    }
}
