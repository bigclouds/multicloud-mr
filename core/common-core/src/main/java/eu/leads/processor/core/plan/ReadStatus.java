package eu.leads.processor.core.plan;

import eu.leads.processor.core.DataType;
import org.vertx.java.core.json.JsonObject;

/**
 * Created by vagvaz on 8/4/14.
 */
public class ReadStatus extends DataType {
  public ReadStatus() {
    super();
    data.putNumber("min", Long.MAX_VALUE);
    data.putNumber("max", Long.MIN_VALUE);
    data.putNumber("size", 0);
    data.putBoolean("readFully", false);
  }

  public ReadStatus(JsonObject object) {
    data = object.copy();
  }

  public ReadStatus(String jsonString) {
    data = new JsonObject(jsonString);
  }

  public ReadStatus(ReadStatus other) {
    data = other.asJsonObject().copy();
  }

  public long getMin() {
    return data.getLong("min");
  }

  public void setMin(long min) {
    data.putNumber("min", min);
  }

  public long getMax() {
    return data.getLong("max");
  }

  public void setMax(long max) {
    data.putNumber("max", max);
  }

  public boolean isReadFully() {
    return data.getBoolean("readFully");
  }

  public void setReadFully(boolean readFully) {
    data.putBoolean("readFully", readFully);
  }

  public long size() {
    return data.getLong("size");
  }

  public void setSize(long size) {
    data.putNumber("size", size);
  }
}
