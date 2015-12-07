package eu.leads.processor.infinispan;

import eu.leads.processor.common.infinispan.InfinispanClusterSingleton;
import eu.leads.processor.conf.LQPConfiguration;

import java.io.IOException;
import java.util.UUID;

/**
 * Created by vagvaz on 11/07/15.
 */
public class SingleInfinispanNode {
  public static void main(String[] args) {
    LQPConfiguration.getInstance().initialize();
    LQPConfiguration.getInstance().getConfiguration()
        .setProperty("node.current.component", UUID.randomUUID().toString());

    InfinispanClusterSingleton.getInstance().getManager();
    try {
      System.in.read();
    } catch (IOException e) {
      e.printStackTrace();
    }
    InfinispanClusterSingleton.getInstance().getManager();
  }
}
