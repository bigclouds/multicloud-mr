import eu.leads.processor.conf.LQPConfiguration;
import eu.leads.processor.core.Tuple;
import org.infinispan.ensemble.EnsembleCacheManager;
import org.infinispan.ensemble.cache.EnsembleCache;

import java.util.ArrayList;

/**
 * Created by vagvaz on 08/05/15.
 */
public class TupleExternalizerTest {

  public static void main(String[] args) {
    int numTuples = 10;
    LQPConfiguration.initialize();
    //        InfinispanManager manager = InfinispanClusterSingleton.getInstance().getManager();
    //        manager.getPersisentCache("testCache");
    EnsembleCacheManager emanager =
        new EnsembleCacheManager(LQPConfiguration.getInstance().getConfiguration().getString("node.ip") + ":11222");
    EnsembleCache ecache =
        emanager.getCache("clustered", new ArrayList<>(emanager.sites()), EnsembleCacheManager.Consistency.DIST);
    for (int i = 0; i < numTuples; i++) {
      Tuple t = new Tuple();
      t.setAttribute("at" + i, new Integer(i));
      ecache.put(Integer.toString(i), t);
    }

    for (int i = 0; i < numTuples; i++) {
      Tuple t = (Tuple) ecache.get(Integer.toString(i));
      System.out.println(t.asString());
    }
    System.exit(0);
  }
}
