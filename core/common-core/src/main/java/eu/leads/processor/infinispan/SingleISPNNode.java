package eu.leads.processor.infinispan;

import eu.leads.processor.common.infinispan.InfinispanClusterSingleton;
import eu.leads.processor.common.infinispan.InfinispanManager;
import eu.leads.processor.conf.LQPConfiguration;

/**
 * Created by vagvaz on 19/06/15.
 */
public class SingleISPNNode {
  public static void main(String[] args) {
    LQPConfiguration.initialize();
    //        rmanager = createRemoteCacheManager();
    if (args.length > 0) {
      LQPConfiguration.getInstance().getConfiguration().setProperty("node.current.component", args[0]);
    } else {
      LQPConfiguration.getInstance().getConfiguration().setProperty("node.current.component", "imanager");
    }
    InfinispanManager manager = InfinispanClusterSingleton.getInstance().getManager();
  }
}
