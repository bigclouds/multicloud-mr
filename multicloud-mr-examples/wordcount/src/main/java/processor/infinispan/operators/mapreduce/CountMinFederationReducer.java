package eu.leads.processor.infinispan.operators.mapreduce;

import eu.leads.processor.core.Tuple;
import eu.leads.processor.infinispan.LeadsCollector;
import eu.leads.processor.infinispan.LeadsReducer;

import org.mapdb.DBMaker;
import org.vertx.java.core.json.JsonObject;

import java.io.Serializable;
import java.util.Iterator;
import java.util.Map;

/**
 * Created by Apostolos Nydriotis on 2015/07/03.
 */
public class CountMinFederationReducer extends LeadsReducer<String, Tuple> {

  transient int w;
  transient private LeadsCollector collector;
  transient private boolean collectorInitialized;
  transient private Map<String, Row> rowStorage;
  transient private Map<String, Integer> sumStorage;
  transient private boolean reduceLocalOn;

  public CountMinFederationReducer() {
    super();
  }

  public CountMinFederationReducer(JsonObject configuration) {
    super(configuration);
  }

  public CountMinFederationReducer(String configString) {
    super(configString);
  }

  @Override
  public void reduce(String reducedKey, Iterator<Tuple> iter, LeadsCollector collector) {

    int[] singleRow = new int[w];
    int element = 0;

    while (iter.hasNext()) {
      Tuple t = iter.next();
      if (t.hasField("coord")) {
        // localReducer is on
        reduceLocalOn = true;
        int sum = t.getNumberAttribute("sum").intValue();
        String coord = t.getAttribute("coord");
        int column = Integer.valueOf(coord.split(",")[1]);
        singleRow[column] += sum;
      } else {
        // localReducer is off
        reduceLocalOn = false;
        element += t.getNumberAttribute("count").intValue();
      }
    }

    if (isComposable) {
      if (!collectorInitialized) {
        this.collector = collector;
        collectorInitialized = true;
      }

      if (reduceLocalOn) {
        Row r = rowStorage.get(reducedKey);
        if (r == null) {
          r = new Row(w);
          for (int i = 0; i < w; i++) {
            r.setElement(i, singleRow[i]);
          }
        } else {
          for (int i = 0; i < w; i++) {
            r.setElement(i, r.getElement(i) + singleRow[i]);
          }
        }
        rowStorage.put(reducedKey, r);
      } else {
        Integer storedSum = sumStorage.get(reducedKey);
        if (storedSum == null) {
          sumStorage.put(reducedKey, element);
        } else {
          sumStorage.put(reducedKey, storedSum + element);
        }
      }

    } else {
      if (reduceLocalOn) {
        String singleRowStr = "";
        for (int i = 0; i < w; i++) {
          singleRowStr += String.valueOf(singleRow[i]);
          if (i < singleRow.length - 1) {
            singleRowStr += ",";
          }
        }
        collector.emit(reducedKey, singleRowStr);
      } else {
        collector.emit(reducedKey, element);
      }
    }
  }

  @Override
  public void initialize() {
    super.initialize();
    w = conf.getInteger("w");
    collectorInitialized = false;
    if (isComposable) {
      rowStorage = DBMaker.tempTreeMap();
      sumStorage = DBMaker.tempTreeMap();
    }
  }

  @Override
  protected void finalizeTask() {
    System.out.println(getClass().getName() + " finished!");

    if (isComposable) {
      if (reduceLocalOn) {
        for (Map.Entry<String, Row> entry : rowStorage.entrySet()) {
          String singleRowStr = "";
          for (int i = 0; i < w; i++) {
            singleRowStr += String.valueOf(entry.getValue().getElement(i));
            if (i < w - 1) {
              singleRowStr += ",";
            }
          }
          collector.emit(entry.getKey(), singleRowStr);
        }
      } else {
        for (Map.Entry<String, Integer> entry : sumStorage.entrySet()) {
          collector.emit(entry.getKey(), entry.getValue());
        }
      }
    }
  }

  private class Row implements Serializable {
    private int[] row;

    public Row(int width) {
      row = new int[width];
    }

    public int getElement(int position) {
      return row[position];
    }

    public void setElement(int position, int element) {
      row[position] = element;
    }
  }
}
