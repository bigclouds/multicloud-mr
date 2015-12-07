package eu.leads.processor.web;

import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Created by vagvaz on 3/7/14.
 */
public class QueryResults extends JsonObject {

  public QueryResults() {
    super();
    setId("");
    setMax(-1);
    setMin(-1);
    setResult(new ArrayList<String>());

  }



  public QueryResults(String queryId) {
    setId(queryId);
    setMax(-1);
    setMin(-1);
    setResult(new ArrayList<String>());

  }

  public QueryResults(JsonObject jsonObject) {
    super(jsonObject.toString());
  }

  public String getId() {
    return this.getString("id");
  }

  public void setId(String id) {
    this.putString("id", id);
  }

  public long getMin() {
    return this.getLong("min");
  }

  public void setMin(long min) {
    this.putNumber("min", min);
  }

  public long getMax() {
    return this.getLong("max");
  }

  public void setMax(long max) {
    this.putNumber("max", max);
  }

  public List<String> getResult() {
    List<String> result = new ArrayList<String>();
    JsonArray array = new JsonArray(this.getString("result"));

    Iterator<Object> iterator = array.iterator();
    while (iterator.hasNext()) {
      result.add((String) iterator.next());
    }
    return result;
  }

  public void setResult(List<String> result) {

    JsonArray resultArray = new JsonArray();

    for (String t : result) {
      resultArray.add(t);
    }
    this.putArray("result", resultArray);
  }

  public String getMessage() {
    return this.getString("message");
  }

  public void setMessage(String message) {
    this.putString("message", message);
  }

  @Override public String toString() {
    return this.encodePrettily();
  }

  public long getSize() {
    return this.getLong("size");
  }

  public void setSize(long size) {
    this.putNumber("size", size);
  }
}
