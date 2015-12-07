package eu.leads.processor.infinispan.operators.mapreduce;

import eu.leads.processor.core.Tuple;
import eu.leads.processor.infinispan.LeadsMapper;
import org.infinispan.distexec.mapreduce.Collector;
import org.vertx.java.core.json.JsonObject;

/**
 * Created by Apostolos Nydriotis on 2015/06/23.
 */
public class WordCountMapper extends LeadsMapper<String, Tuple, String, Tuple> {

  int w = 0;
  public WordCountMapper(JsonObject configuration) {
    super(configuration);
  }

  public WordCountMapper(String configString) {
    super(configString);
  }

  public WordCountMapper() {
  }

  @Override public void map(String key, Tuple value, Collector<String, Tuple> collector) {
    //    System.out.println(getClass().getName() + ".map!");
    for (String attribute : value.getFieldNames()) {
      for (String word : value.getAttribute(attribute).split(" ")) {
        if (word != null && word.length() > 0) {
          Tuple outputTuple = new Tuple();
          outputTuple.setAttribute("count", 1);
          collector.emit(word, outputTuple);
        }
      }
    }
  }

  @Override protected void finalizeTask() {
    System.out.println(getClass().getName() + " finished!");
  }
}
