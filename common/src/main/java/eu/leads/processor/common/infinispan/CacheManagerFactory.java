package eu.leads.processor.common.infinispan;

import eu.leads.processor.conf.LQPConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by vagvaz on 5/17/14.
 */
public class CacheManagerFactory {

    private static Logger logger = LoggerFactory.getLogger(CacheManagerFactory.class);

    //    @Produces
    //    public  static  InfinispanManager instantiateCacheManager(InjectionPoint injectionPoint){
    //        InfinispanManager result = null;
    //        System.out.println("Creatting ISPN manager");
    //        switch (KVSType.stringToKVSType(LQPConfiguration.getConf().getString("processor.infinispan.mode"))) {
    //            case LOCAL:
    //                logger.info("Starting local Infinispan Manager");
    //                result = new LocalInfinispanManager();
    //                break;
    //            case CLUSTER:
    //                logger.info("Starting Clustered Infinispan Manager");
    //                result = new ClusterInfinispanManager();
    //                break;
    //            case ENSEMBLE:
    //                logger.info("Creatting ISPN manager multi");
    ////                result = new MuliClusterInfinispanWrapper();
    //
    //                break;
    //            default:
    ////                result = new LocalInfinsipanWrapper();
    //               logger.info("Creatting ISPN manager def");
    //                break;
    //        }
    //        result.startManager("conf/"+LQPConfiguration.getConf().getString("processor.infinispan.file"));
    //
    //        return result;
    //    }

    /*This function creates a cache InfinispanManaager instance depending on the mode from the configuration Right now there are two
    * types of InifnispanManager Local and Clustered
    * User probably should not use this function apart from Testing its plugins...*/
    public static InfinispanManager createCacheManager() {
        InfinispanManager result = null;
        System.out.println("Creatting ISPN manager");
        switch (KVSType.stringToKVSType(LQPConfiguration.getConf()
                                            .getString("processor.infinispan.mode"))) {
            case LOCAL:
                System.out.println("Creatting ISPN manager loc");
                logger.info("Starting local Infinispan Manager");
//                result = new LocalInfinispanManager();
                result = new ClusterInfinispanManager();
                break;
            case CLUSTER:

                logger.info("Starting Clustered Infinispan Manager");
                result = new ClusterInfinispanManager();
                break;
            case ENSEMBLE:
                logger.info("Creatting ISPN manager multi");
                break;
            default:
                logger.info("Creatting ISPN manager def");
                break;
        }
        result.startManager("conf/" + LQPConfiguration.getConf()
                                          .getString("processor.infinispan.file"));

        return result;
    }

   public static InfinispanManager createCacheManager(String type, String config) {
      InfinispanManager result = null;
      System.out.println("Creatting ISPN manager");
      switch (KVSType.stringToKVSType(type)) {
         case LOCAL:
            System.out.println("Creatting ISPN manager loc");
            logger.info("Starting local Infinispan Manager");
//                result = new LocalInfinispanManager();
            result = new LocalInfinispanManager();
            break;
         case CLUSTER:

            logger.info("Starting Clustered Infinispan Manager");
            result = new ClusterInfinispanManager();
            break;
         case ENSEMBLE:
            logger.info("Creatting ISPN manager for Ensemble");
           result = new EnsembleInfinispanManager();
            break;
         default:
            logger.info("Creatting ISPN manager def");
            break;
      }
      result.startManager(config);

      return result;

   }
}
