package eu.leads.processor.common.test;

import eu.leads.processor.common.infinispan.InfinispanClusterSingleton;
import eu.leads.processor.common.infinispan.InfinispanManager;
import eu.leads.processor.common.utils.PrintUtilities;
import eu.leads.processor.conf.LQPConfiguration;
import org.infinispan.Cache;

import java.util.Set;

/**
 * Created by vagvaz on 6/3/14.
 */
public class StartInfinispan {
    public static void main(String[] args) throws InterruptedException {

        LQPConfiguration.initialize();


        //        cluster.initialize();

        InfinispanManager man = InfinispanClusterSingleton.getInstance().getManager();
        PrintUtilities.printList(man.getMembers());

        man.getPersisentCache("testCache").put("1", "2");
        PrintUtilities.printList(man.getMembers());
        PrintUtilities
            .printIterable(((Cache) man.getPersisentCache("testCache")).getListeners().iterator());
        for (int i = 0; i < 1; i++) {
            Thread.sleep(1000);
            System.out.println("i " + i);
            PrintUtilities.printIterable(((Cache) man.getPersisentCache("testCache")).getListeners()
                                             .iterator());
            System.out.println("-----");
            PrintUtilities.printList(man.getMembers());
            System.out.println("-----");
            Set<String> set = man.getCacheManager().getCacheNames();
            for (String c : set) {
                System.out.println("s: " + c + "  " + man.getPersisentCache(c).size());
            }
        }

        man.stopManager();
    }
}
