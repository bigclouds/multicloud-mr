package eu.leads.processor.infinispan.operators.mapreduce;

import eu.leads.processor.core.Tuple;
import eu.leads.processor.infinispan.LeadsCollector;
import eu.leads.processor.infinispan.LeadsCombiner;

import org.vertx.java.core.json.JsonObject;

import java.util.Iterator;

/**
 * Created by Apostolos Nydriotis on 2015/10/16.
 */
public class CountMinCombiner extends LeadsCombiner<String, Tuple> {

  public CountMinCombiner() {
    super();
  }

  public CountMinCombiner(JsonObject configuration) {
    super(configuration);
  }

  public CountMinCombiner(String configString) {
    super(configString);
  }

  @Override
  public void reduce(String reducedKey, Iterator<Tuple> iter, LeadsCollector collector) {
    int sum = 0;
    while (iter.hasNext()) {
      Tuple t = iter.next();
      sum += t.getNumberAttribute("count").intValue();
    }
    Tuple output = new Tuple();
    output.setAttribute("count", sum);
    collector.emit(reducedKey, output);
  }

  @Override
  protected void finalizeTask() {
    System.out.println(getClass().getName() + " finished!");

  }
}
