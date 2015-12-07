package eu.leads.processor.infinispan.operators.mapreduce;

import eu.leads.processor.common.infinispan.InfinispanClusterSingleton;
import eu.leads.processor.core.Tuple;
import eu.leads.processor.infinispan.LeadsCollector;
import eu.leads.processor.infinispan.LeadsReducer;
import org.infinispan.Cache;
import org.vertx.java.core.json.JsonObject;

import java.util.Iterator;

//import eu.leads.processor.common.utils.SQLUtils;


/**
 * Created with IntelliJ IDEA.
 * User: vagvaz
 * Date: 11/4/13
 * Time: 9:36 AM
 * To change this template use File | Settings | File Templates.
 */
public class TransformReducer extends LeadsReducer<String, Tuple> {

  transient Cache<String, String> data;
  transient String prefix;


  public TransformReducer() {
  }

  public TransformReducer(JsonObject configuration) {
    super(configuration);
  }

  public TransformReducer(String configString) {
    super(configString);
  }

  @Override public void initialize() {

    isInitialized = true;
    //        super.initialize();
    //        imanager = new ClusterInfinispanManager(manager);
    conf = new JsonObject(configString);
    outputCacheName = conf.getString("output");
    prefix = outputCacheName + ":";
    data = (Cache<String, String>) InfinispanClusterSingleton.getInstance().getManager()
        .getPersisentCache(outputCacheName);

  }

  @Override public void reduce(String key, Iterator<Tuple> iterator, LeadsCollector collector) {
    //Reduce takes all the grouped Typles per key
    //      System.out.println("running for " + key + " .");
    if (key == null || key.equals(""))
      return;

    if (!isInitialized)
      initialize();

    Tuple t = null;
    //Iterate overall values
    while (iterator.hasNext()) {
      //            t = new Tuple(iterator.next()); //Check
      t = iterator.next(); //Check
    }

    key = key.split(":")[1]; // Get the key without the cachename
    //        System.err.println("tout: " + t.toString());
    //        collector.emit(prefix + key, t.asString());
    collector.emit(prefix + key, t);
    return;
  }

}
