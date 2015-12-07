package eu.leads.processor.core;

import java.io.Serializable;

/**
 * Created by vagvaz on 8/14/15.
 */
public class TupleWrapper implements Serializable {

  String key;
  private Tuple tuple;

  public TupleWrapper() {
  }

  public TupleWrapper(String key, int counter, Tuple tuple) {
    this.key = key;
    this.tuple = tuple;
  }

  public String getKey() {
    return key;
  }

  public void setKey(String key) {
    this.key = key;
  }

  public Tuple getTuple() {
    return tuple;
  }

  public void setTuple(Tuple tuple) {
    this.tuple = tuple;
  }

  @Override public String toString() {
    return "TupleWrapper{" +
        "key='" + key + '\'' +
        ", tuple=" + tuple +
        '}';
  }
}
