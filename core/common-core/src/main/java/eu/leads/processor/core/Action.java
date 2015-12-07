package eu.leads.processor.core;

import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;

/**
 * Created by vagvaz on 8/5/14.
 */
public class Action extends DataType {

  public Action() {
    super();
    data.putString("type", "action");
  }

  public Action(JsonObject msg) {
    super(msg);
    data.putString("type", "action");
  }

  public Action(Action action) {
    super(action.asJsonObject());
    data.putString("type", "action");
  }

  public String getId() {
    return data.getString("id");
  }

  public void setId(String id) {
    data.putString("id", id);
  }

  public String getOwnerId() {
    return data.getString("ownerId");
  }

  public void setOwnerId(String ownerId) {
    data.putString("ownerId", ownerId);
  }

  public String getComponentType() {
    return data.getString("componentType");
  }

  public void setComponentType(String componentType) {
    data.putString("componentType", componentType);
  }

  public String getCategory() {
    return data.getString("category");
  }

  public void setCategory(String category) {
    data.putString("category", category);
  }

  public String getLabel() {
    return data.getString("label");
  }

  public void setLabel(String label) {
    data.putString("label", label);
  }

  public String getTriggered() {
    return data.getString("triggered");
  }

  public void setTriggered(String triggered) {
    data.putString("triggered", triggered);
  }

  public JsonArray getTriggers() {
    return data.getArray("triggers");
  }

  public void setTriggers(JsonArray array) {
    data.putArray("triggers", array);
  }

  public void addChildAction(String id) {
    data.getElement("triggers").asArray().add(id);
  }


  public String getStatus() {
    return data.getString("status");
  }

  public void setStatus(String status) {
    data.putString("status", status);
  }

  public JsonObject getData() {
    if (!data.containsField("data")) {
      data.putObject("data", new JsonObject());
    }
    return data.getObject("data");
  }

  public void setData(JsonObject data) {
    this.data.putObject("data", data);
  }

  public String getDestination() {
    return data.getString("destination");
  }

  public void setDestination(String destination) {
    data.putString("destination", destination);
  }

  public String getProcessedBy() {
    return data.getString("processed");
  }

  public void setProcessedBy(String processedBy) {
    data.putString("processed", processedBy);
  }

  public JsonObject getResult() {
    return data.getObject("result");
  }

  public void setResult(JsonObject result) {
    data.putObject("result", result);
  }

  public void setGlobalConf(JsonObject global) {
    data.putObject("globalConfig", global);
  }

  public JsonObject getGlobalConf() {
    return data.getObject("globalConfig");
  }

  @Override public int hashCode() {
    return getId().hashCode();
  }

  @Override public boolean equals(Object obj) {
    if (obj instanceof Action) {
      Action other = (Action) obj;
      return other.getId().equals(this.getId());
    }
    return false;

  }

  public String getCurrentCluster() {
    return data.getString("currentCluster");
  }

  public void setCurrentCluster(String currentCluster) {
    data.putString("currentCluster", currentCluster);
  }
}

