package eu.leads.processor.infinispan.operators;

import eu.leads.processor.common.StringConstants;
import eu.leads.processor.core.Tuple;
import eu.leads.processor.infinispan.LeadsBaseCallable;
import eu.leads.processor.math.MathUtils;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;

import java.io.Serializable;
import java.util.*;

/**
 * Created by vagvaz on 2/18/15.
 */
public abstract class LeadsSQLCallable<K, V> extends LeadsBaseCallable<K, V> implements Serializable {
  transient protected JsonObject inputSchema;
  transient protected JsonObject outputSchema;
  transient protected Map<String, String> outputMap;
  transient protected Map<String, List<JsonObject>> targetsMap;
  transient List<String> attributeFunctions;

  public LeadsSQLCallable() {
    super();
  }

  public LeadsSQLCallable(String configString, String output) {
    super(configString, output);
  }


  @Override public void initialize() {
    super.initialize();
    outputSchema = conf.getObject("body").getObject("outputSchema");
    inputSchema = conf.getObject("body").getObject("inputSchema");
    targetsMap = new HashMap();
    outputMap = new HashMap<>();
    attributeFunctions = new ArrayList<>();
    JsonArray targets = conf.getObject("body").getArray("targets");
    if (conf.containsField("body") && conf.getObject("body").containsField("targets")) {
      Iterator<Object> targetIterator = targets.iterator();
      while (targetIterator.hasNext()) {
        JsonObject target = (JsonObject) targetIterator.next();
        if (target.getObject("expr").getString("type").equalsIgnoreCase("field")) {
          List<JsonObject> tars =
              targetsMap.get(target.getObject("expr").getObject("body").getObject("column").getString("name"));
          if (tars == null) {
            tars = new ArrayList<>();
          }
          tars.add(target);
          targetsMap.put(target.getObject("expr").getObject("body").getObject("column").getString("name"), tars);
        } else {
          List<JsonObject> tars = new ArrayList<>();
          tars.add(target);
          targetsMap.put(target.getObject("column").getString("name"), tars);
          attributeFunctions.add(target.getObject("column").getString("name"));
        }
      }
    }
  }

  protected void executeFunctions(Tuple tuple) {
    for (String func : attributeFunctions) {
      JsonObject function = targetsMap.get(func).get(0);
      Object functionResult = MathUtils.executeFunction(tuple, function.getObject("expr").getObject("body"));
      tuple.setAttribute(func, functionResult);
    }
  }

  protected void renameAllTupleAttributes(Tuple tuple) {
    JsonArray fields = inputSchema.getArray("fields");
    Iterator<Object> iterator = fields.iterator();
    String columnName = null;
    String fieldName = tuple.getFieldNames().iterator().next();
    String tableName = fieldName.substring(fieldName.indexOf(".") + 1, fieldName.lastIndexOf("."));
    while (iterator.hasNext()) {
      JsonObject tmp = (JsonObject) iterator.next();
      columnName = tmp.getString("name");
      if (columnName.contains("." + tableName + ".")) {
        return;
      }
      int lastPeriod = columnName.lastIndexOf(".");
      String attributeName = columnName.substring(lastPeriod + 1);
      tuple.renameAttribute(StringConstants.DEFAULT_DATABASE_NAME + "." + tableName + "." + attributeName, columnName);
    }
  }

  protected Tuple prepareOutput(Tuple tupleIn) {
    Tuple tuple = new Tuple(tupleIn);
    renameAllTupleAttributes(tuple);
    if (outputSchema.toString().equals(inputSchema.toString())) {
      return tuple;
    }


    JsonObject result = new JsonObject();
    //WARNING
    //       System.err.println("out: " + tuple.asString());

    if (targetsMap.size() == 0) {
      //          System.err.println("s 0 ");
      return tuple;

    }
    //END OF WANRING
    List<String> toRemoveFields = new ArrayList<String>();
    Map<String, List<String>> toRename = new HashMap<String, List<String>>();
    for (String field : tuple.getFieldNames()) {
      List<JsonObject> ob = targetsMap.get(field);
      if (ob == null)
        toRemoveFields.add(field);
      else {
        for (JsonObject obb : ob) {
          List<String> ren = toRename.get(field);
          if (ren == null) {
            ren = new ArrayList<>();
          }
          //               toRename.put(field, ob.getObject("column").getString("name"));
          ren.add(obb.getObject("column").getString("name"));
          toRename.put(field, ren);
        }
      }
    }
    tuple.removeAtrributes(toRemoveFields);
    tuple.renameAttributes(toRename);
    return tuple;
  }
}
