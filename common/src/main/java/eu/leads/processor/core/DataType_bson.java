package eu.leads.processor.core;

import com.mongodb.util.JSON;
import org.bson.BSONObject;
import org.bson.BasicBSONObject;
import org.vertx.java.core.json.JsonObject;

/**
 * Created by vagvaz on 8/4/14.
 */
public abstract class DataType_bson {
  protected BSONObject data;

  public DataType_bson(BSONObject other) {
    data = other;
  }

  public DataType_bson(String jsonString) {
    data = new BasicBSONObject();
    data = (BSONObject) JSON.parse(jsonString);
  }

  public DataType_bson() {
    data = new BasicBSONObject();
  }

  public JsonObject asJsonObject() {
    String bsonString = data.toString();
    JsonObject objectJSON = new JsonObject(bsonString);
    return objectJSON;
  }


  public String asString() {
    return data.toString();
  }

  public void copy(BSONObject other) {
    data = new BasicBSONObject();
    data.putAll(other);
  }

  @Override public String toString() {
    return data.toString();
  }

  public Object getValue(String key) {
    return data.get(key);
  }

  public BSONObject asBsonObject() {
    return data;
  }
}
