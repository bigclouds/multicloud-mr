package eu.leads.processor.infinispan.operators.mapreduce;

import eu.leads.processor.core.Tuple;
import eu.leads.processor.infinispan.LeadsMapper;

import org.infinispan.distexec.mapreduce.Collector;
import org.vertx.java.core.json.JsonObject;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

/**
 * Created by Apostolos Nydriotis on 2015/07/10.
 */
public class KMeansMapper extends LeadsMapper<String, Tuple, String, Tuple> {

  int k;
  Map<String, Double>[] centroids;
  Double[] norms;
  Random random;

  public KMeansMapper() {
    super();
  }

  public KMeansMapper(JsonObject configuration) {
    super(configuration);
  }

  public KMeansMapper(String configString) {
    super(configString);
  }

  @Override
  public void map(String key, Tuple document, Collector<String, Tuple> collector) {
    double maxSimilarity = 0;
    int index = random.nextInt(k);

    for (int i = 0; i < k; i++) {
      double d = calculateCosSimilarity(i, document);
      if (d > maxSimilarity) {
        maxSimilarity = d;
        index = i;
      }
    }
    Tuple res = new Tuple();
    res.setAttribute("dimensions", document.asBsonObject());  // TODO can we avoid (de)serializations?
    res.setAttribute("documentsCount", 1);
    collector.emit(String.valueOf(index), res);
  }

  @Override
  public void initialize() {
    super.initialize();
    k = conf.getInteger("k");

    // Get centroids from configuration
    centroids = new Map[k];
    norms = new Double[k];

    for (int i = 0; i < k; i++) {
      norms[i] = conf.getNumber("norm" + String.valueOf(i)).doubleValue();
      Map<String, Double> map = new HashMap<>();
      JsonObject doc = conf.getField("centroid" + String.valueOf(i));
      for (String word : doc.getFieldNames()) {
        map.put(word, doc.getNumber(word).doubleValue());
      }
      centroids[i] = map;
    }
    random = new Random();
  }

  @Override
  protected void finalizeTask() {
    System.out.println(getClass().getName() + " finished!");
  }

  private double calculateCosSimilarity(int i, Tuple value) {

    // cosine = A B / ||A|| ||B|| (Ignore document's norm)

    Double numerator = 0d;

    for (String s : value.getFieldNames()) {
      if (s.equals("~")) {  // Skip the document id
        continue;
      }

      Double centroidValue = centroids[i].get(s);
      if (centroidValue != null) {
        numerator += value.getNumberAttribute(s).doubleValue() * centroidValue;
      }
    }

    return numerator / Math.sqrt(Math.max(norms[i], 1));
  }
}
