package eu.leads.processor.math;

import eu.leads.processor.core.Tuple;
import org.infinispan.query.dsl.FilterConditionContext;
import org.infinispan.query.dsl.QueryBuilder;
import org.vertx.java.core.json.JsonElement;
import org.vertx.java.core.json.JsonObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Created by vagvaz on 9/24/14.
 */
public class FilterOperatorNode {
  FilterOpType type;
  FilterOperatorNode left;

  public FilterOperatorNode getRight() {
    return right;
  }

  public void setRight(FilterOperatorNode right) {
    this.right = right;
  }

  public FilterOperatorNode getLeft() {
    return left;
  }

  public void setLeft(FilterOperatorNode left) {
    this.left = left;
  }

  public FilterOpType getType() {
    return type;
  }

  public void setType(FilterOpType type) {
    this.type = type;
  }

  public void setValue(JsonObject value) {
    this.value = value;
  }

  FilterOperatorNode right;
  JsonObject value;

  public FilterOperatorNode(JsonElement node) {
    type = FilterOpType.valueOf(node.asObject().getString("type"));
    if (type.equals(FilterOpType.FIELD) || type.equals(FilterOpType.CONST) || type.equals(FilterOpType.SIGNED) || type
        .equals(FilterOpType.CAST)) {
      value = node.asObject();
      left = null;
      right = null;
    } else if (type.equals(FilterOpType.ROW_CONSTANT)) {
      value = node.asObject();
      left = null;
      right = null;
      value = MathUtils.createValueSet(value);
    } else {
      left = new FilterOperatorNode(node.asObject().getObject("body").getElement("leftExpr"));
      right = new FilterOperatorNode(node.asObject().getObject("body").getElement("rightExpr"));
      value = new JsonObject();
      value.putString("type", "CONST");
      JsonObject body = new JsonObject();
      body.putString("type", "CONST");
      JsonObject datum = new JsonObject();
      datum.putString("type", "BOOLEAN");
      datum.putObject("body", new JsonObject());
      datum.getObject("body").putString("type", "BOOLEAN");
      body.putObject("datum", datum);
      value.putObject("body", body);
    }

  }

  public FilterOperatorNode() {

  }

  //
  public Object CreateQuery(QueryBuilder q) {
    FilterConditionContext result = null;
    FilterConditionContext left = (FilterConditionContext) CreateQuery(q);
    FilterConditionContext right = (FilterConditionContext) CreateQuery(q);
    //      if(left != null)
    //         left.CreateQuery();
    //      if(right != null)
    //         right.CreateQuery();

    switch (type) {
      case NOT:
        //TODO
        break;
      case AND: {
        //            boolean leftValue = left.getValueAsBoolean();
        //            boolean rightValue = right.getValueAsBoolean();
        //            result =  leftValue && rightValue;
      }
      break;
      case OR: {
        //            boolean leftValue = left.getValueAsBoolean();
        //            boolean rightValue = right.getValueAsBoolean();
        //            result =  leftValue || rightValue;


      }
      break;
      case EQUAL:
        result = left.and().having("attributeValue").eq(right);//,right.getValueAsJson());
        break;
      case IS_NULL:
        // result = left.isValueNull();
        break;
      case NOT_EQUAL:
        //  result = !(MathUtils.equals(left.getValueAsJson(), right.getValueAsJson()));
        break;
      case LTH:
        // result = MathUtils.lessThan(left.getValueAsJson(),right.getValueAsJson());
        break;
      case LEQ:
        //  result = MathUtils.lessEqualThan(left.getValueAsJson(),right.getValueAsJson());
        break;
      case GTH:
        // result = MathUtils.greaterThan(left.getValueAsJson(),right.getValueAsJson());
        break;
      case GEQ:
        // result = MathUtils.greaterEqualThan(left.getValueAsJson(),right.getValueAsJson());
        break;
      case AGG_FUNCTION:
        break;
      case FUNCTION:
        break;
      case LIKE:
        // result = MathUtils.like(left.getValueAsJson(),right.getValueAsJson(),value);
        break;
      case IN:
        //TODO
        //            JsonObject val = null;
        //            JsonObject set = null;
        //            if(left.getValueAsJson().getString("type").equals("FIELD")){
        //               val = left.getValueAsJson();
        //               set = right.getValueAsJson();
        //            }
        //            else{
        //               val = right.getValueAsJson();
        //               set= left.getValueAsJson();
        //            }
        //            result =  MathUtils.checkIfIn(val,set);
        // check conditino
        // rerturn field in set
      case ROW_CONSTANT:
        //TODO
        break;
      case FIELD:
        //            JsonObject datum = computeDatum(t);
        //            if(datum != null)
        //               this.value.getObject("body").putObject("datum", datum);
        //result = true;
        return q.having("attributevalue").eq(MathUtils.getTextFrom(value));
      case CONST:
        // result = true;
        return MathUtils.getTextFrom(value);

    }
    // putBooleanDatum(result);
    return result;
  }


  public boolean accept(Tuple t) {
    boolean result = false;
    if (left != null)
      left.accept(t);
    if (right != null)
      right.accept(t);

    switch (type) {
      case NOT:
        //TODO
        break;
      case AND: {
        boolean leftValue = left.getValueAsBoolean();
        boolean rightValue = right.getValueAsBoolean();
        result = leftValue && rightValue;
      }
      break;
      case OR: {
        boolean leftValue = left.getValueAsBoolean();
        boolean rightValue = right.getValueAsBoolean();

        result = leftValue || rightValue;
      }
      break;
      case EQUAL:
        result = MathUtils.equals(left.getValueAsJson(), right.getValueAsJson());
        break;
      case IS_NULL:
        result = left.isValueNull();
        break;
      case NOT_EQUAL:
        result = !(MathUtils.equals(left.getValueAsJson(), right.getValueAsJson()));
        break;
      case LTH:
        result = MathUtils.lessThan(left.getValueAsJson(), right.getValueAsJson());
        break;
      case LEQ:
        result = MathUtils.lessEqualThan(left.getValueAsJson(), right.getValueAsJson());
        break;
      case GTH:
        result = MathUtils.greaterThan(left.getValueAsJson(), right.getValueAsJson());
        break;
      case GEQ:
        result = MathUtils.greaterEqualThan(left.getValueAsJson(), right.getValueAsJson());
        break;
      case AGG_FUNCTION:
        break;
      case FUNCTION:
        break;
      case LIKE:
        result = MathUtils.like(left.getValueAsJson(), right.getValueAsJson(), value);
        break;
      case IN:
        //TODO
        JsonObject val = null;
        JsonObject set = null;
        if (left.getValueAsJson().getString("type").equals("FIELD")) {
          val = left.getValueAsJson();
          set = right.getValueAsJson();
        } else {
          val = right.getValueAsJson();
          set = left.getValueAsJson();
        }
        result = MathUtils.checkIfIn(val, set);
        // check conditino
        // rerturn field in set
      case ROW_CONSTANT:
        //TODO
        break;
      case FIELD:
        JsonObject datum = computeDatum(t);
        if (datum != null)
          this.value.getObject("body").putObject("datum", datum);
        result = true;
        return result;
      case CONST:
        result = true;
        return result;

    }
    putBooleanDatum(result);
    return result;
  }

  public void updateWith(Tuple t) {

    if (left != null)
      left.updateWith(t);
    if (right != null)
      right.updateWith(t);

    switch (type) {
      case NOT:
        //TODO
        break;
      case AND: {
        //            boolean leftValue = left.getValueAsBoolean();
        //            boolean rightValue = right.getValueAsBoolean();
        //            result =  leftValue && rightValue;
      }
      break;
      case OR: {
        //            boolean leftValue = left.getValueAsBoolean();
        //            boolean rightValue = right.getValueAsBoolean();
        //
        //            result =  leftValue || rightValue;
      }
      break;
      case EQUAL:
        //            result = MathUtils.equals(left.getValueAsJson(),right.getValueAsJson());
        break;
      case IS_NULL:
        //            result = left.isValueNull();
        break;
      case NOT_EQUAL:
        //            result = !(MathUtils.equals(left.getValueAsJson(),right.getValueAsJson()));
        break;
      case LTH:
        //            result = MathUtils.lessThan(left.getValueAsJson(),right.getValueAsJson());
        break;
      case LEQ:
        //            result = MathUtils.lessEqualThan(left.getValueAsJson(),right.getValueAsJson());
        break;
      case GTH:
        //            result = MathUtils.greaterThan(left.getValueAsJson(),right.getValueAsJson());
        break;
      case GEQ:
        //            result = MathUtils.greaterEqualThan(left.getValueAsJson(),right.getValueAsJson());
        break;
      case AGG_FUNCTION:
        break;
      case FUNCTION:
        break;
      case LIKE:
        //            result = MathUtils.like(left.getValueAsJson(),right.getValueAsJson(),value);
        break;
      case IN:
        //            //TODO
        //            JsonObject val = null;
        //            JsonObject set = null;
        //            if(left.getValueAsJson().getString("type").equals("FIELD")){
        //               val = left.getValueAsJson();
        //               set = right.getValueAsJson();
        //            }
        //            else{
        //               val = right.getValueAsJson();
        //               set= left.getValueAsJson();
        //            }
        //            result = MathUtils.checkIfIn(val,set);
        // check conditino
        // rerturn field in set
      case ROW_CONSTANT:
        //TODO
        break;
      case FIELD:
        JsonObject datum = computeDatum(t);
        if (datum != null) {
          this.value.getObject("body").putObject("datum", datum);
        }
        //            result = true;
        //            return result;
      case CONST:
        //            result = true;
        //            return result;

    }
    //      putBooleanDatum(result);
    //      return result;
  }


  private boolean getValueAsBoolean() {
    Object object = value.getObject("body").getObject("datum").getObject("body").getValue("val");
    if (object instanceof Boolean)
      return (Boolean) object;
    else {
      if (object.toString().equals("") || object.toString().equals("0"))
        return false;
      else
        return true;
    }
  }

  private boolean isValueNull() {
    return value.getObject("body").getObject("datum").getObject("body").containsField("val");
  }

  private String getValue() {
    return value.getObject("body").getObject("datum").getObject("body").getValue("val").toString();
  }



  public JsonObject getValueAsJson() {
    return value;
  }

  private void putBooleanDatum(boolean val) {
    JsonObject result = new JsonObject();
    result.putString("type", "BOOLEAN");
    result.putObject("body", new JsonObject());
    result.getObject("body").putString("type", result.getString("type"));
    result.getObject("body").putBoolean("val", val);
    value.getObject("body").putObject("datum", result);

  }

  private JsonObject computeDatum(Tuple t) {
    if (!t.hasField(value.getObject("body").getObject("column").getString("name")))
      return null;
    JsonObject result = new JsonObject();
    result.putString("type", value.getObject("body").getObject("column").getObject("dataType").getString("type"));
    result.putObject("body", new JsonObject());
    result.getObject("body").putString("type", result.getString("type"));
    result.getObject("body")
        .putValue("val", t.getGenericAttribute(value.getObject("body").getObject("column").getString("name")));
    return result;
  }

  public JsonObject toJson(JsonObject result) {
    //      FilterOpType type;
    //      FilterOperatorNode left;
    //      FilterOperatorNode right;
    //      JsonObject value;
    result.putString("type", type.toString());
    result.putObject("value", value);
    if (left != null) {
      JsonObject leftOb = new JsonObject();
      leftOb = left.toJson(leftOb);
      result.putObject("left", leftOb);
    }
    if (right != null) {
      JsonObject rightOb = new JsonObject();
      rightOb = right.toJson(rightOb);
      result.putObject("right", rightOb);
    }
    return result;
  }

  public void fromJson(JsonObject treeAsJson) {
    type = FilterOpType.valueOf(treeAsJson.getString("type"));
    value = treeAsJson.getObject("value");
    if (treeAsJson.containsField("left")) {
      left = new FilterOperatorNode();
      left.fromJson(treeAsJson.getObject("left"));
    }
    if (treeAsJson.containsField("right")) {
      right = new FilterOperatorNode();
      right.fromJson(treeAsJson.getObject("right"));
    }
  }

  public Map<String, List<String>> getAllFieldsByTable(Map<String, List<String>> map) {
    Map<String, List<String>> result = map;
    if (left != null)
      result = left.getAllFieldsByTable(map);
    if (right != null)
      result = right.getAllFieldsByTable(map);
    switch (type) {
      case FIELD:
        String columnName = value.getObject("body").getObject("column").getString("name");
        String table = columnName.substring(0, columnName.lastIndexOf("."));
        List<String> columns = result.get(table);
        if (columns == null) {
          columns = new ArrayList<>();
        }
        columns.add(columnName);
        result.put(table, columns);
        break;
      default:
        break;
    }
    return result;

  }

  public void renameTableReference(String tableName, String toRename) {
    if (left != null)
      left.renameTableReference(tableName, toRename);
    if (right != null)
      right.renameTableReference(tableName, toRename);
    switch (type) {
      case FIELD:
        String columnName = value.getObject("body").getObject("column").getString("name");
        if (columnName.contains("." + toRename + ".")) {
          columnName = columnName.replace("." + toRename + ".", "." + tableName + ".");
          value.getObject("body").getObject("column").putString("name", columnName);
        }
        break;
      default:
        break;
    }
  }
}
