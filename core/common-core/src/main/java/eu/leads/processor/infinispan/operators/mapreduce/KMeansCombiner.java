package eu.leads.processor.infinispan.operators.mapreduce;

import eu.leads.processor.core.Tuple;
import eu.leads.processor.infinispan.LeadsCollector;
import eu.leads.processor.infinispan.LeadsCombiner;

import org.bson.BasicBSONObject;
import org.vertx.java.core.json.JsonObject;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Created by Apostolos Nydriotis on 2015/10/09.
 */
public class KMeansCombiner extends LeadsCombiner<String, Tuple> {

  public KMeansCombiner() {
    super();
  }

  public KMeansCombiner(JsonObject configuration) {
    super(configuration);
  }

  public KMeansCombiner(String configString) {
    super(configString);
  }

  @Override
  public void reduce(String reducedKey, Iterator<Tuple> iter, LeadsCollector collector) {
    Integer documentsCount = 0;
    Map<String, Double> dimensions = new HashMap<>();
    String clusterDocuments = "";

    while (iter.hasNext()) {
      Tuple t = iter.next();
      BasicBSONObject document = (BasicBSONObject) t.getGenericAttribute("dimensions");
      for (String s : document.keySet()) {  // For each word
        if (s.equals("~")) {  // Skip document id (when used as combiner)
          clusterDocuments += String.valueOf(document.get(s)) + " ";
          continue;
        }

        Double wordFrequency = (Double) document.get(s);
        Double currentFrequency = dimensions.get(s);
        if (currentFrequency == null) {
          dimensions.put(s, wordFrequency);
        } else {
          dimensions.put(s, currentFrequency + wordFrequency);
        }
      }
      documentsCount += t.getNumberAttribute("documentsCount").intValue();

      if (t.hasField("clusterDocuments")) {
        // Carry the clusterDocuments (when user as localReducer)
        clusterDocuments += t.getAttribute("clusterDocuments") + " ";
      }
    }

    BasicBSONObject dimensionsBson = new BasicBSONObject();
    dimensionsBson.putAll(dimensions);

    Tuple toEmit = new Tuple();
    toEmit.setAttribute("dimensions", dimensionsBson);
    toEmit.setNumberAttribute("documentsCount", documentsCount);
    toEmit.setAttribute("clusterDocuments", clusterDocuments);
    collector.emit(reducedKey, toEmit);
  }
}
