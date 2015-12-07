package eu.leads.processor.infinispan.operators.mapreduce;

import eu.leads.processor.core.Tuple;
import eu.leads.processor.infinispan.LeadsCollector;
import eu.leads.processor.infinispan.LeadsCombiner;

import org.bson.BasicBSONObject;
import org.mapdb.DBMaker;
import org.vertx.java.core.json.JsonObject;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Created by Apostolos Nydriotis on 2015/07/10.
 */
public class KMeansReducer extends LeadsCombiner<String, Tuple> {

  transient private Map<String, Tuple> storage;
  transient private LeadsCollector collector;
  transient private boolean collectorInitialized;

  public KMeansReducer() {
    super();
  }

  public KMeansReducer(JsonObject configuration) {
    super(configuration);
  }

  public KMeansReducer(String configString) {
    super(configString);
  }

  @Override
  public void initialize() {
    super.initialize();
    if (isComposable) {
      collectorInitialized = false;
      storage = DBMaker.tempTreeMap();
    }
  }

  @Override
  public void reduce(String reducedKey, Iterator<Tuple> iter, LeadsCollector collector) {
    int documentsCount = 0;
    Map<String, Double> dimensions = new HashMap<>();

    String clusterDocuments = "";  // Space-separated document ids

    while (iter.hasNext()) {
      Tuple t = iter.next();
      Tuple dimensionsTuple = new Tuple((BasicBSONObject) t.getGenericAttribute("dimensions"));
      int count = t.getNumberAttribute("documentsCount").intValue();
      documentsCount += count;

      for (String s : dimensionsTuple.getFieldNames()) {
        Double wordFrequency = dimensionsTuple.getNumberAttribute(s).doubleValue();
        Double currentFrequency = dimensions.get(s);
        if (currentFrequency == null) {
          dimensions.put(s, wordFrequency);
        } else {
          dimensions.put(s, currentFrequency + wordFrequency);
        }
      }
      clusterDocuments += t.getAttribute("clusterDocuments");
    }

    if (isComposable) {

      if (!collectorInitialized) {
        this.collector = collector;
        collectorInitialized = true;
      }

      Tuple t = storage.get(reducedKey);
      if (t != null) {

        int storedDocumentsCount = t.getNumberAttribute("documentsCount").intValue();
        BasicBSONObject storedDimensions = (BasicBSONObject) t.getGenericAttribute("dimensions");

        for (Map.Entry<String, Double> e : dimensions.entrySet()) {
          Double storedFrequency = (Double) storedDimensions.get(e.getKey());
          if (storedFrequency == null) {
            storedDimensions.put(e.getKey(), e.getValue());
          } else {
            storedDimensions.put(e.getKey(), storedFrequency + e.getValue());
          }
        }

        t.setAttribute("dimensions", storedDimensions);
        t.setNumberAttribute("documentsCount", documentsCount + storedDocumentsCount);
        t.setAttribute("clusterDocuments", clusterDocuments + t.getAttribute("clusterDocuments"));

      } else {  // not exists in storage
        t = new Tuple();
        BasicBSONObject dimensionsBison = new BasicBSONObject();
        dimensionsBison.putAll(dimensions);
        t.setAttribute("dimensions", dimensionsBison);
        t.setNumberAttribute("documentsCount", documentsCount);
        t.setAttribute("clusterDocuments", clusterDocuments);
      }

      storage.put(reducedKey, t);

    } else {  // not composable
      double norm = 0d;
      for (Map.Entry<String, Double> entry : dimensions.entrySet()) {
        entry.setValue(entry.getValue() / (double) documentsCount);
        norm += entry.getValue() * entry.getValue();
      }

      BasicBSONObject dimensionsBson = new BasicBSONObject();
      dimensionsBson.putAll(dimensions);

      Tuple r = new Tuple();
      r.setAttribute("newCentroid", dimensionsBson);
      r.setAttribute("cluster" + reducedKey, clusterDocuments);
      r.setAttribute("norm" + reducedKey, norm);
      collector.emit(reducedKey, r);
    }
  }

  @Override
  protected void finalizeTask() {
    System.out.println(getClass().getName() + " finished!");
    if (isComposable) {
      for (Map.Entry<String, Tuple> e : storage.entrySet()) {
        String reducedKey = e.getKey();
        Tuple storedTuple = e.getValue();

        int documentsCount = storedTuple.getNumberAttribute("documentsCount").intValue();
        Tuple storedDimensionsTuple =
            new Tuple((BasicBSONObject) storedTuple.getGenericAttribute("dimensions"));

        Map<String, Double> dimensions = new HashMap<>();
        for (String key : storedDimensionsTuple.getFieldNames()) {
          dimensions.put(key, storedDimensionsTuple.getNumberAttribute(key).doubleValue());
        }

        double norm = 0d;
        for (Map.Entry<String, Double> entry : dimensions.entrySet()) {
          entry.setValue(entry.getValue() / (double) documentsCount);
          norm += entry.getValue() * entry.getValue();
        }

        Tuple newCentroidTuple = new Tuple();
        newCentroidTuple.asBsonObject().putAll(dimensions);

        Tuple r = new Tuple();
        r.asBsonObject().put("newCentroid", newCentroidTuple.asBsonObject());
        r.setAttribute("cluster" + reducedKey, storedTuple.getAttribute("clusterDocuments"));
        r.setAttribute("norm" + reducedKey, norm);
        collector.emit(reducedKey, r);
      }
    }
  }
}
