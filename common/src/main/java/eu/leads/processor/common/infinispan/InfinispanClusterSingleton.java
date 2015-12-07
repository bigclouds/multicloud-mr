package eu.leads.processor.common.infinispan;


/**
 * Created by vagvaz on 6/3/14.
 */


/**
 * A simple utility class for Singleton in order to simplify the bootstrapping and use of infinispan
 * throughout the project
 */
public class InfinispanClusterSingleton {
    private static final InfinispanClusterSingleton instance = new InfinispanClusterSingleton();
    protected  InfinispanManager cluster;

    /**
     * Do not instantiate InfinispanClusterSingleton.
     */
    private InfinispanClusterSingleton() {
//        cluster = new InfinispanCluster(CacheManagerFactory.createCacheManager());
        cluster = CacheManagerFactory.createCacheManager();
    }

    /**
     * Getter for property 'instance'.
     *
     * @return Value for property 'instance'.
     */
    public static InfinispanClusterSingleton getInstance() {
        return instance;
    }

    /**
     * Getter for property 'manager'.
     *
     * @return Value for property 'manager'.
     */
    public InfinispanManager getManager() {
        return instance.cluster;
    }

    /**
     * Getter for property 'cluster'.
     *
     * @return Value for property 'cluster'.
     */
//    public InfinispanCluster getCluster() {
//        return this.cluster;
//    }
}
