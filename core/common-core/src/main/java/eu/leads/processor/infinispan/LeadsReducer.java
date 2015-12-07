package eu.leads.processor.infinispan;

import eu.leads.processor.common.ProgressReport;
import eu.leads.processor.common.infinispan.InfinispanClusterSingleton;
import eu.leads.processor.common.infinispan.InfinispanManager;
import eu.leads.processor.core.Tuple;
import org.apache.commons.configuration.XMLConfiguration;
import org.infinispan.Cache;
import org.infinispan.distexec.mapreduce.Reducer;
import org.infinispan.manager.EmbeddedCacheManager;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;

import java.io.Serializable;
import java.util.*;
import java.util.concurrent.ConcurrentMap;

/**
 * Created with IntelliJ IDEA.
 * User: vagvaz
 * Date: 11/4/13
 * Time: 6:08 AM
 * To change this template use File | Settings | File Templates.
 */
public class LeadsReducer<K, V> implements Reducer<K, V>, Serializable {
  /**
   *
   */
  private static final long serialVersionUID = -402082107893975415L;
  transient protected Cache thecache;
  protected String configString;
  protected String outputCacheName;
  protected long overall;
  transient protected InfinispanManager imanager;
  transient protected JsonObject conf;
  transient protected ConcurrentMap<String, String> output;
  transient protected boolean isInitialized = false;
  transient protected Timer timer;
  transient protected ProgressReport report;
  transient protected JsonObject inputSchema;
  transient protected JsonObject outputSchema;
  transient protected Map<String, String> outputMap;
  transient protected Map<String, List<JsonObject>> targetsMap;
  transient protected EmbeddedCacheManager manager;
  transient protected XMLConfiguration xmlConfiguration;
  transient protected boolean isLocal;
  transient protected boolean isComposable;

  public LeadsReducer() {
  }

  public LeadsReducer(JsonObject configuration) {
    this.conf = configuration;
    this.configString = configuration.toString();
  }

  public LeadsReducer(String configString) {
    this.configString = configString;

  }

  public void setConfigString(String confString) {
    this.configString = confString;
  }

  public String getConfigString() {
    return configString;
  }

  public void setCacheManager(EmbeddedCacheManager manager) {
    this.manager = manager;
  }

  @Override public V reduce(K reducedKey, Iterator<V> iter) {
    return null;
  }

  public void reduce(K key, Iterator<V> iterator, LeadsCollector collector) {
  }

  public void initialize(XMLConfiguration xmlConfiguration) {
    this.xmlConfiguration = xmlConfiguration;
  }

  public void initialize() {  // add local or federation
    conf = new JsonObject(configString);

    if (conf.containsField("local")) {
      isLocal = true;
    } else {
      isLocal = false;
    }

    if (conf.containsField("composable")) {
      isComposable = true;
    } else {
      isComposable = false;
    }

    outputCacheName = conf.getString("output");
    if (conf.containsField("body") && conf.getObject("body").containsField("outputSchema")) {
      outputSchema = conf.getObject("body").getObject("outputSchema");
      inputSchema = conf.getObject("body").getObject("inputSchema");
      targetsMap = new HashMap();
      outputMap = new HashMap<>();
      JsonArray targets = conf.getObject("body").getArray("targets");
      Iterator<Object> targetIterator = targets.iterator();
      while (targetIterator.hasNext()) {
        JsonObject target = (JsonObject) targetIterator.next();
        List<JsonObject> tars =
            targetsMap.get(target.getObject("expr").getObject("body").getObject("column").getString("name"));
        if (tars == null) {
          tars = new ArrayList<>();
        }
        tars.add(target);
        targetsMap.put(target.getObject("expr").getObject("body").getObject("column").getString("name"), tars);
      }
    }
    if (thecache != null)
      System.err.println("$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$  \n\nLLLLLL " + thecache.size());
    overall = this.conf.getLong("workload", 100);
    timer = new Timer();
    report = new ProgressReport(this.getClass().toString(), 0, overall);
    timer.scheduleAtFixedRate(report, 0, 2000);

    imanager = InfinispanClusterSingleton.getInstance().getManager();
    manager = imanager.getCacheManager();
    output = imanager.getPersisentCache("output");
  }



  protected void finalizeTask() {
    //      report.printReport(report.getReport());

    //      timer.cancel();
  }

  protected void progress() {
    report.tick();
  }

  protected void progress(long n) {
    report.tick(n);
  }

  protected Tuple prepareOutput(Tuple tuple) {
    if (outputSchema.toString().equals(inputSchema.toString())) {
      return tuple;
    }

    JsonObject result = new JsonObject();
    //WARNING
    //       System.err.println("out: " + tuple.asString());

    if (targetsMap.size() == 0) {
      //          System.err.println("s 0 ");
      return tuple;

    }
    //       System.err.println("normal");

    //END OF WANRING
    List<String> toRemoveFields = new ArrayList<String>();
    Map<String, List<String>> toRename = new HashMap<String, List<String>>();
    for (String field : tuple.getFieldNames()) {
      List<JsonObject> ob = targetsMap.get(field);
      if (ob == null)
        toRemoveFields.add(field);
      else {
        for (JsonObject obb : ob) {
          List<String> ren = toRename.get(field);
          if (ren == null) {
            ren = new ArrayList<>();
          }
          //               toRename.put(field, ob.getObject("column").getString("name"));
          ren.add(obb.getObject("column").getString("name"));
          toRename.put(field, ren);
        }
      }
    }
    tuple.removeAtrributes(toRemoveFields);
    tuple.renameAttributes(toRename);
    return tuple;
  }

  protected void handlePagerank(Tuple t) {

    if (t.hasField("default.webpages.pagerank")) {
      if (!t.hasField("url"))
        return;
      String pagerankStr = t.getAttribute("pagerank");
      //            Double d = Double.parseDouble(pagerankStr);
      //            if (d < 0.0) {
      //
      //                try {
      ////                    d = LeadsPrGraph.getPageDistr(t.getAttribute("url"));
      //                    d = (double) LeadsPrGraph.getPageVisitCount(t.getAttribute("url"));
      //
      //                } catch (IOException e) {
      //                    e.printStackTrace();
      //                }
      //                t.setAttribute("pagerank", d.toString());
      //        }
    }
  }



}
