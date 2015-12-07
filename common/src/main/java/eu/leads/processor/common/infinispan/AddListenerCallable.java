package eu.leads.processor.common.infinispan;

import eu.leads.processor.common.LeadsListener;
import org.infinispan.Cache;
import org.infinispan.distexec.DistributedCallable;

import java.io.Serializable;
import java.util.Set;

/**
 * Created by vagvaz on 6/4/14.
 */
public class AddListenerCallable<K, V> implements DistributedCallable<K, V, String>, Serializable {
//    private static final long serialVersionUID = 8331682008912636780L;
    private  String cacheName;
    private  Object listner;
    //    private final Configuration configuration;
    private transient Cache<K, V> cache;

    public AddListenerCallable(String cacheName, Object listener) {
        this.cacheName = cacheName;
        this.listner = listener;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public String call() throws Exception {
        try {
            boolean toadd = true;
            if (cache.getCacheManager().cacheExists(cacheName)) {
//                if (cache.getAdvancedCache().getRpcManager().getMembers().contains(cache.getCacheManager().getAddress())) {

                    if (listner instanceof LeadsListener) {
                        LeadsListener leadsListener = (LeadsListener) listner;
                        for (Object l : cache.getListeners()) {
                            if (l.getClass().getCanonicalName().equals(leadsListener.getClass().getCanonicalName())) {
                                toadd = false;
                            }
                        }


                    }
                    if (toadd) {
                        if (listner instanceof LeadsListener) {
                            LeadsListener leadsListener = (LeadsListener) listner;
                            leadsListener.initialize(new ClusterInfinispanManager(cache.getCacheManager()), null);
                        }
                        cache.getCacheManager().getCache(cacheName).addListener(listner);
                    }
//                }
            }
            for (Object l : cache.getListeners()) {
                if (l instanceof LeadsListener) {
                    LeadsListener listener = (LeadsListener) l;
                    System.err.println(
                        listner.getClass().toString() + ": " + cache.getCacheManager().getAddress().toString() + " " + cache.getName() + " LEAD ID " + listener.getId());
                }
            }
        }catch (Exception e){
            e.printStackTrace();
        }
        System.err.println("return");
        return cache.getCacheManager().getAddress().toString();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setEnvironment(Cache<K, V> cache, Set<K> inputKeys) {
        this.cache = cache;
    }
}
