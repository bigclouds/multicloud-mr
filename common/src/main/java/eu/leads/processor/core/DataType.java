package eu.leads.processor.core;

import org.vertx.java.core.json.JsonObject;

/**
 * Created by angelos on 22/01/15.
 */
public abstract class DataType {

  protected JsonObject data;

  public DataType(JsonObject other) {
    data = other;
  }

  public DataType(String jsonString) {
    data = new JsonObject(jsonString);
  }

  public DataType() {
    data = new JsonObject();
  }

  public JsonObject asJsonObject() {
    return data;
  }

  public String asString() {
    return data.toString();
  }

  public void copy(JsonObject other) {
    data = other.copy();
  }

  @Override public String toString() {
    return data.toString();
  }
}
