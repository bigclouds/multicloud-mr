package eu.leads.processor.infinispan.operators.mapreduce;

import eu.leads.processor.common.utils.ProfileEvent;
import eu.leads.processor.core.Tuple;
import eu.leads.processor.infinispan.LeadsCollector;
import eu.leads.processor.infinispan.LeadsCombiner;

import org.mapdb.DB;
import org.mapdb.DBMaker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vertx.java.core.json.JsonObject;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;

/**
 * Created by Apostolos Nydriotis on 2015/06/23.
 */
public class WordCountReducer extends LeadsCombiner<String, Tuple> {

  Logger log;
  transient private LeadsCollector collector;
  transient private boolean collectorInitialized;
  transient private boolean isComposableButNotLocal;
  private Map<String, Integer> sums;

  public WordCountReducer(JsonObject configuration) {
    super(configuration);
  }

  public WordCountReducer(String configString) {
    super(configString);
  }

  public WordCountReducer() {
    super();
  }

  @Override public void initialize() {
    super.initialize();
    log = LoggerFactory.getLogger(WordCountReducer.class);
    collectorInitialized = false;
    isComposableButNotLocal = isComposable && !isLocal;
    if (isComposableButNotLocal) {
//      sums = new HashMap<>();
      sums = DBMaker.tempTreeMap();
    }
  }

  @Override public void reduce(String reducedKey, Iterator<Tuple> iter, LeadsCollector collector) {
    //    System.out.println(getClass().getName() + ".reduce!");
    int sum = 0;
    ProfileEvent event = new ProfileEvent("WCRComputeSum", log);
    while (true) {
      try {
        Tuple input = iter.next();
        int count = Integer.valueOf(input.getAttribute("count"));
        sum += count;
      } catch (Exception e) {
        if (e instanceof NoSuchElementException) {
          break;
        }
        e.printStackTrace();
      }
    }

    if (isComposableButNotLocal) {
      if (sums.containsKey(reducedKey)) {
        sum += sums.get(reducedKey);
      }
      sums.put(reducedKey, sum);

      if (!collectorInitialized) {
        this.collector = collector;
        collectorInitialized = true;
      }
    }

    event.end();
    if (!isComposableButNotLocal) {
      event.start("WRCOutputResult");
      Tuple output = new Tuple();
      output.setAttribute("count", sum);
      collector.emit(reducedKey, output);
      event.end();
    }
  }

  @Override protected void finalizeTask() {
    System.out.println(getClass().getName() + " finished!");
    if (isComposableButNotLocal) {
      for (Map.Entry<String, Integer> entry : sums.entrySet()) {
        Tuple output = new Tuple();
        output.setAttribute("count", entry.getValue());
        collector.emit(entry.getKey(), output);
      }
    }
  }
}
