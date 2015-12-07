package eu.leads.processor.common.test;


import eu.leads.processor.common.infinispan.CacheManagerFactory;
import eu.leads.processor.common.infinispan.InfinispanClusterSingleton;
import eu.leads.processor.common.infinispan.InfinispanManager;
import eu.leads.processor.common.utils.PrintUtilities;
import eu.leads.processor.conf.LQPConfiguration;

import java.util.concurrent.ConcurrentMap;

/**
 * Created by vagvaz on 6/2/14.
 */
public class InfinispanTest {
    public static void main(String[] args) {
        LQPConfiguration.initialize();



        //        cluster.initialize();
        InfinispanManager man =InfinispanClusterSingleton.getInstance().getManager();

        ConcurrentMap map = man.getPersisentCache("queriesfoo");
        map.put("1", "11");
        map.put("2", "22");
//        InfinispanCluster cluster2 =
//            new InfinispanCluster(CacheManagerFactory.createCacheManager());

        InfinispanManager man2 = CacheManagerFactory.createCacheManager();
        PrintUtilities.printMap(map);
        ConcurrentMap map2 = man2.getPersisentCache("queriesfoo");
        map2.put("4", "33");
        PrintUtilities.printMap(map2);
        System.out.println("cl");
        PrintUtilities.printList(InfinispanClusterSingleton.getInstance().getManager().getMembers());
        System.out.println("cl2");
        PrintUtilities.printList(man2.getMembers());
        boolean output = false;
        if(map.get("4").equals("33") && map2.get("2").equals("22"))
            output = true;
//        cluster2.shutdown();
//        cluster.shutdown();
        if(output)
            System.err.println("SUCCESS");
        System.exit(0);
    }
}
