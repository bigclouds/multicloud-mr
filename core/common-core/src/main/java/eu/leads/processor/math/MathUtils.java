package eu.leads.processor.math;

import eu.leads.processor.core.Tuple;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Created with IntelliJ IDEA.
 * User: vagvaz
 * Date: 11/7/13
 * Time: 4:42 PM
 * To change this template use File | Settings | File Templates.
 */
//Mathematical Utilities
public class MathUtils {
  //    public static Object add(Object o1, Object o2, String type) {
  //        if (type.equalsIgnoreCase("double") || type.equals("float")) {
  //            return add((Double) o1, (Double) o2);
  //        } else if (type.equalsIgnoreCase("int") || type.equalsIgnoreCase("integer") || type.equalsIgnoreCase("long")) {
  //            return add((Long) o1, (Long) o2);
  //        } else if (type.equalsIgnoreCase("string")) {
  //            return ((String) o1).concat((String) o2);
  //        } else if (type.equalsIgnoreCase("date")) {
  //            return null;
  //        }
  //        return null;
  //    }
  //
  //    private static Long add(Long o1, Long o2) {
  //        return o1 + o2;
  //    }
  //
  //    private static Double add(Double o1, Double o2) {
  //        return o1 + o2;
  //    }
  //
  //
  //    public static Object divide(Object o1, Object o2, String type) {
  //        if (type.equalsIgnoreCase("double") || type.equalsIgnoreCase("float")) {
  //            return add((Double) o1, (Double) o2);
  //        } else if (type.equalsIgnoreCase("int") || type.equalsIgnoreCase("integer") || type.equalsIgnoreCase("long")) {
  //            return add((Long) o1, (Long) o2);
  //        } else if (type.equalsIgnoreCase("string")) {
  //            return "";
  //        } else if (type.equalsIgnoreCase("date")) {
  //            return null;
  //        }
  //        return null;
  //    }
  //
  //    private static Long divide(Long o1, Long o2) {
  //        return o1 / o2;
  //    }
  //
  //    private static Double divide(Double o1, Double o2) {
  //        return o1 / o2;
  //    }


  //    public static Object compare(Object o1, Object o2, String type) {
  //        if (type.equalsIgnoreCase("double") || type.equalsIgnoreCase("float")) {
  //            return compare((Double) o1, (Double) o2);
  //        } else if (type.equalsIgnoreCase("int") || type.equalsIgnoreCase("integer") || type.equalsIgnoreCase("long")) {
  //            return compare((Long) o1, (Long) o2);
  //        } else if (type.equalsIgnoreCase("string")) {
  //            return ((String) o1).compareTo((String) o2);
  //        } else if (type.equalsIgnoreCase("date")) {
  //            return compare((Date) o1, (Date) o2);
  //        }
  //        return 0;
  //    }
  //
  //    public static int compare(Long o1, Long o2) {
  //        return o1.compareTo(o2);
  //    }
  //
  //    public static int compare(Double o1, Double o2) {
  //        return o1.compareTo(o2);
  //    }
  //
  //    public static int compare(Date d1, Date d2) {
  //        return d1.compareTo(d2);
  //    }

  //    public static String handleType(String t1) {
  //        if (t1.equalsIgnoreCase("double") || t1.equalsIgnoreCase("float")) {
  //            return "double";
  //        } else if (t1.equalsIgnoreCase("int") || t1.equalsIgnoreCase("long"))
  //            return "long";
  //        else
  //            return "string";
  //    }

  //    public static String handleTypes(String t1, String t2) {
  //        if (t1.equalsIgnoreCase("double") || t1.equalsIgnoreCase("float")) {
  //            if (t2.equalsIgnoreCase("double") || t2.equalsIgnoreCase("float")) {
  //                return "double";
  //            } else if (t2.equalsIgnoreCase("int") || t2.equalsIgnoreCase("long")) {
  //                return "double";
  //            } else {
  //                return "string";
  //            }
  //        }
  //        return "string";
  //    }
  //
  //    public static boolean isArithmentic(String type) {
  //        return type.equalsIgnoreCase("double") || type.equals("int") || type.equalsIgnoreCase("float") || type.equalsIgnoreCase("long");
  //    }

  public static boolean lessThan(JsonObject left, JsonObject right) {
    String type = left.getObject("body").getObject("datum").getString("type");
    String rightType = right.getObject("body").getObject("datum").getString("type");
    if (type.startsWith(rightType.substring(0, 3))) {
      if (type.startsWith("TEXT")) {
        String leftValue = getTextFrom(left);

        //left.getObject("body").getObject("datum").getObject("body").getString("val");
        String rightValue = getTextFrom(right);
        //right.getObject("body").getObject("datum").getObject("body").getString("val");
        return leftValue.compareTo(rightValue) < 0;
      } else if (type.startsWith("INT")) {
        Long leftValue = left.getObject("body").getObject("datum").getObject("body").getLong("val");
        Long rightValue = right.getObject("body").getObject("datum").getObject("body").getLong("val");
        return leftValue.compareTo(rightValue) < 0;
      } else if (type.startsWith("FLOAT") || type.startsWith("DOUBLE")) {
        Number leftValue = left.getObject("body").getObject("datum").getObject("body").getNumber("val");
        Number rightValue = right.getObject("body").getObject("datum").getObject("body").getNumber("val");
        return leftValue.doubleValue() < rightValue.doubleValue();
      } else {
        System.out.println("Unknonw type 2" + type);
        Object leftValue = left.getObject("body").getObject("datum").getObject("body").getValue("val");
        Object rightValue = right.getObject("body").getObject("datum").getObject("body").getValue("val");
        return leftValue.toString().compareTo(rightValue.toString()) < 0;
      }
    } else {
      if (type.equals("left type = CAST"))
        System.out.println("Unknonw  CAST");

      if (type.equals("NULL_TYPE")) {
        return false;
      } else if (rightType.equals("NULL_TYPE")) {
        return false;
      } else {
        System.out.println("Unknonw type 3 " + type);
        Object leftValue = left.getObject("body").getObject("datum").getObject("body").getValue("val");
        Object rightValue = right.getObject("body").getObject("datum").getObject("body").getValue("val");
        return leftValue.toString().compareTo(rightValue.toString()) < 0;
      }
    }
  }

  public static Object getValueFrom(JsonObject json) {
    Object result = null;
    JsonObject datum = json.getObject("expr").getObject("body").getObject("datum");
    if (datum.getString("type").equalsIgnoreCase("TEXT")) {
      result = getTextFrom(json.getObject("expr"));
    } else if (datum.getString("type").startsWith("INT")) {
      result = new Long(datum.getObject("body").getNumber("val").longValue());
    } else if (datum.getString("type").startsWith("FLOAT")) {
      result = new Double(datum.getObject("body").getNumber("val").doubleValue());
    } else if (datum.getString("type").startsWith("DATE")) {
      result = getTextFrom(json.getObject("expr"));
    } else if (datum.getString("type").equals("BLOB")) {
      result = getTextFrom(json.getObject("expr"));
    } else if (datum.getString("type").equals("NULL_TYPE")) {
      result = null;
    } else if (datum.getString("type").equals("VERSION")) {

    } else {
      System.err.println("Unknown type 4");
    }
    return result;
  }

  public static String getTextFrom(JsonObject value) {

    String result = null;
    if (value.getString("type").equals("FIELD")) {
      result = value.getObject("body").getObject("datum").getObject("body").getValue("val").toString();
    } else if (value.getString("type").equals("NULL_TYPE")) {
      result = "null";
    } else if (value.getString("type").equals("CONST")) {
      byte[] patternBytes = null;
      JsonObject body = value.getObject("body").getObject("datum").getObject("body");
      org.vertx.java.core.json.JsonArray bytes = body.getArray("bytes");
      int size = body.getInteger("size");
      patternBytes = new byte[size];
      for (int i = 0; i < size; i++) {
        patternBytes[i] = ((Integer) bytes.get(i)).byteValue();
      }
      result = new String(patternBytes);
    } else if (value.getString("type").equals("TEXT")) {
      byte[] patternBytes = null;
      JsonObject body = value.getObject("body");
      org.vertx.java.core.json.JsonArray bytes = body.getArray("bytes");
      int size = body.getInteger("size");
      patternBytes = new byte[size];
      for (int i = 0; i < size; i++) {
        patternBytes[i] = ((Integer) bytes.get(i)).byteValue();
      }
      result = new String(patternBytes);
    }

    return result;
  }

  public static boolean lessEqualThan(JsonObject left, JsonObject right) {
    String type = left.getObject("body").getObject("datum").getString("type");
    String rightType = right.getObject("body").getObject("datum").getString("type");
    if (type.startsWith(rightType.substring(0, 3))) {
      if (type.startsWith("TEXT")) {
        String leftValue = getTextFrom(left);

        //left.getObject("body").getObject("datum").getObject("body").getString("val");
        String rightValue = getTextFrom(right);
        return leftValue.compareTo(rightValue) <= 0;
      } else if (type.startsWith("INT")) {
        Long leftValue = left.getObject("body").getObject("datum").getObject("body").getLong("val");
        Long rightValue = right.getObject("body").getObject("datum").getObject("body").getLong("val");
        return leftValue.compareTo(rightValue) <= 0;
      } else if (type.startsWith("FLOAT") || type.startsWith("DOUBLE")) {
        Number leftValue = left.getObject("body").getObject("datum").getObject("body").getNumber("val");
        Number rightValue = right.getObject("body").getObject("datum").getObject("body").getNumber("val");
        return leftValue.doubleValue() <= rightValue.doubleValue();
      } else {
        System.out.println("Unknonw type leq " + type);
        Object leftValue = left.getObject("body").getObject("datum").getObject("body").getValue("val");
        Object rightValue = right.getObject("body").getObject("datum").getObject("body").getValue("val");
        return leftValue.toString().compareTo(rightValue.toString()) <= 0;
      }
    } else {
      if (type.equals("NULL_TYPE")) {
        return false;
      } else if (rightType.equals("NULL_TYPE")) {
        return false;
      } else {
        System.out.println("Unknonw type 1 " + type);
        Object leftValue = left.getObject("body").getObject("datum").getObject("body").getValue("val");
        Object rightValue = right.getObject("body").getObject("datum").getObject("body").getValue("val");
        return leftValue.toString().compareTo(rightValue.toString()) < 0;
      }
    }
  }

  public static boolean equals(JsonObject left, JsonObject right) {
    String type = left.getObject("body").getObject("datum").getString("type");
    String rightType = right.getObject("body").getObject("datum").getString("type");
    if (type.startsWith(rightType.substring(0, 3))) {
      if (type.startsWith("TEXT")) {
        String leftValue = getTextFrom(left);

        //left.getObject("body").getObject("datum").getObject("body").getString("val");
        String rightValue = getTextFrom(right);
        return leftValue.compareTo(rightValue) == 0;
      } else if (type.startsWith("INT")) {
        Long leftValue = left.getObject("body").getObject("datum").getObject("body").getLong("val");
        Long rightValue = right.getObject("body").getObject("datum").getObject("body").getLong("val");
        return leftValue.compareTo(rightValue) == 0;
      } else if (type.startsWith("FLOAT") || type.startsWith("DOUBLE")) {
        Number leftValue = left.getObject("body").getObject("datum").getObject("body").getNumber("val");
        Number rightValue = right.getObject("body").getObject("datum").getObject("body").getNumber("val");
        return leftValue.doubleValue() == rightValue.doubleValue();
      } else {
        System.out.println("Unknonw type equals " + type);
        Object leftValue = left.getObject("body").getObject("datum").getObject("body").getValue("val");
        Object rightValue = right.getObject("body").getObject("datum").getObject("body").getValue("val");
        return leftValue.toString().compareTo(rightValue.toString()) == 0;
      }
    } else {
      if (type.equals("NULL_TYPE")) {
        return right.getObject("body").getObject("datum").getObject("body").getValue("val") == null;
      } else if (rightType.equals("NULL_TYPE")) {
        return left.getObject("body").getObject("datum").getObject("body").getValue("val") == null;
      } else {
        return false;
      }
    }
  }

  public static boolean greaterThan(JsonObject left, JsonObject right) {
    return lessThan(right, left);
  }

  public static boolean greaterEqualThan(JsonObject left, JsonObject right) {
    return lessEqualThan(right, left);
  }

  public static boolean like(JsonObject leftValue, JsonObject rightValue, JsonObject value) {
    boolean result = false;
    byte[] patternBytes = null;
    if (leftValue.getString("type").equals("NULL_TYPE") || rightValue.getString("type").equals("NULL_TYPE"))
      return true;
    if (leftValue.getString("type").equals("CONST")) {
      JsonObject body = leftValue.getObject("body").getObject("datum").getObject("body");
      org.vertx.java.core.json.JsonArray bytes = body.getArray("bytes");
      int size = body.getInteger("size");
      patternBytes = new byte[size];
      for (int i = 0; i < size; i++) {
        patternBytes[i] = bytes.get(i);
      }


    }
    if (rightValue.getString("type").equals("CONST")) {
      JsonObject body = rightValue.getObject("body").getObject("datum").getObject("body");
      org.vertx.java.core.json.JsonArray bytes = body.getArray("bytes");
      int size = body.getInteger("size");
      patternBytes = new byte[size];
      if (bytes != null) {
        for (int i = 0; i < size; i++) {
          int intByte = bytes.get(i);
          patternBytes[i] = (byte) intByte;
        }

      }
    }
    String pattern = new String(patternBytes);
    //      pattern = pattern.replaceAll("%", "__");
    pattern.trim();
    //      pattern = "[" + pattern + "]";
    //      Pattern regex = Pattern.compile(pattern);
    if (leftValue.getString("type").equals("FIELD")) {
      String testString = leftValue.getObject("body").getObject("datum").getObject("body").getString("val");
      //         result =regex.matcher(testString).matches();
      if (pattern.startsWith("%") && pattern.endsWith("%")) {
        pattern = pattern.replaceAll("%", "");
        result = testString.contains(pattern);
      } else if (pattern.startsWith("%")) {
        pattern = pattern.replaceAll("%", "");
        result = testString.endsWith(pattern);
      } else if (pattern.endsWith("%")) {
        pattern = pattern.replaceAll("%", "");
        result = testString.startsWith(pattern);
      } else {
        result = testString.equals(pattern);
      }


    }
    if (rightValue.getString("type").equals("FIELD")) {
      String testString = rightValue.getObject("body").getObject("datum").getObject("body").getString("val");
      //         result =regex.matcher(testString).matches();
      if (pattern.startsWith("%") && pattern.endsWith("%")) {
        pattern = pattern.replaceAll("%", "");
        result = testString.contains(pattern);
      } else if (pattern.startsWith("%")) {
        pattern = pattern.replaceAll("%", "");
        result = testString.endsWith(pattern);
      } else if (pattern.endsWith("%")) {
        pattern = pattern.replaceAll("%", "");
        result = testString.startsWith(pattern);
      } else {
        result = testString.equals(pattern);
      }

    }
    return result;
  }

  public static Object getInitialValue(String type, String function) {
    Object result = null;
    if (type.startsWith("TEXT")) {
      result = new String();
    } else if (type.startsWith("INT")) {
      if (function.equals("sum") || function.equals("count")) {
        result = new Long(0);
      } else if (function.equals("max")) {
        result = new Long(Long.MIN_VALUE);
      } else if (function.equals("min")) {
        result = new Long(Long.MAX_VALUE);
      } else if (function.equals("avg")) {
        Map<String, Object> tmp = new HashMap<String, Object>();
        tmp.put("sum", new Long(0));
        tmp.put("count", new Long(0));
        result = tmp;
      }
    } else if (type.startsWith("FLOAT") || type.startsWith("DOUBLE")) {
      if (function.equals("sum") || function.equals("count")) {
        result = new Double(0.0);
      } else if (function.equals("max")) {
        result = new Double(Double.MIN_VALUE);
      } else if (function.equals("min")) {
        result = new Double(Double.MAX_VALUE);
      } else if (function.equals("avg")) {
        Map<String, Object> tmp = new HashMap<String, Object>();
        tmp.put("sum", new Double(0.0));
        tmp.put("count", new Long(0));
        result = tmp;
      }
    } else if (type.startsWith("LONG")) {
      if (function.equals("sum") || function.equals("count")) {
        result = new Long(0);
      } else if (function.equals("max")) {
        result = new Long(Long.MIN_VALUE);
      } else if (function.equals("min")) {
        result = new Long(Long.MAX_VALUE);
      } else if (function.equals("avg")) {
        Map<String, Object> tmp = new HashMap<String, Object>();
        tmp.put("sum", new Long(0));
        tmp.put("count", new Long(0));
        result = tmp;
      }
    } else {
      System.out.println("Unknown init type " + type);
      result = new Long(0);
    }

    return result;
  }

  public static Object updateFunctionValue(String function, String type, Object oldValue, Object currentValue) {
    Object result = oldValue;
    if (function.equals("count")) {
      return updateCountValue(type, oldValue, currentValue);
    } else if (currentValue == null || currentValue.toString().equals("null"))
      return oldValue;
    if (function.equals("sum")) {
      result = updateSumValue(type, oldValue, currentValue);
    } else if (function.equals("max")) {
      result = updateMaxValue(type, oldValue, currentValue);
    } else if (function.equals("min")) {
      result = updateMinValue(type, oldValue, currentValue);
    } else if (function.equals("avg")) {
      result = updateAvgValue(type, oldValue, currentValue);
    } else {
      System.out.println("Unknonw function " + type);
      result = updateCountValue(type, oldValue, currentValue);
    }

    return result;
  }

  private static Object updateAvgValue(String type, Object oldValue, Object currentValue) {
    Object result = oldValue;
    if (type.startsWith("TEXT")) {
      result = currentValue;
    } else if (type.startsWith("INT")) {
      long currentValueInt = ((Number) currentValue).longValue();
      Map<String, Object> oldMap = (Map<String, Object>) oldValue;
      oldMap.put("count", ((Number) oldMap.get("count")).longValue() + 1);
      oldMap.put("sum", (Long) oldMap.get("sum") + currentValueInt);
      result = oldMap;
    } else if (type.startsWith("FLOAT") || type.startsWith("DOUBLE")) {
      Map<String, Object> oldMap = (Map<String, Object>) oldValue;
      oldMap.put("count", ((Number) oldMap.get("count")).longValue() + 1);
      oldMap.put("sum", ((Number) oldMap.get("sum")).doubleValue() + ((Number) currentValue).doubleValue());
      result = oldMap;
    } else {
      System.out.println("Unknonw up avg type " + type);
      Map<String, Object> oldMap = (Map<String, Object>) oldValue;
      oldMap.put("count", ((Number) oldMap.get("count")).longValue() + 1);
      oldMap.put("sum", ((Number) oldMap.get("sum")).longValue() + ((Number) currentValue).longValue());
      result = oldMap;
    }
    return result;
  }

  private static Object updateMinValue(String type, Object oldValue, Object currentValue) {
    Object result = oldValue;
    if (type.startsWith("TEXT")) {
      String old = (String) oldValue;
      String current = (String) currentValue;
      if (old.compareTo(current) > 0) {
        result = current;
      }
    } else if (type.startsWith("INT")) {
      Long old = ((Number) oldValue).longValue();
      long current = ((Number) currentValue).longValue();
      if (current < old) {
        result = new Long(current);
      }
    } else if (type.startsWith("FLOAT") || type.startsWith("DOUBLE")) {
      Double old = ((Number) oldValue).doubleValue();
      Double current = ((Number) currentValue).doubleValue();
      if (old.compareTo(current) > 0) {
        result = current;
      }
    } else {
      System.out.println("Unknonw min type " + type);
      Long old = ((Number) oldValue).longValue();
      long current = ((Number) currentValue).longValue();
      if (current < old) {
        result = new Long(current);
      }
    }
    return result;
  }

  private static Object updateMaxValue(String type, Object oldValue, Object currentValue) {
    Object result = oldValue;
    if (type.startsWith("TEXT")) {
      String old = (String) oldValue;
      String current = (String) currentValue;
      if (old.compareTo(current) < 0) {
        result = current;
      }
    } else if (type.startsWith("INT")) {
      Long old = ((Number) oldValue).longValue();
      long current = ((Number) currentValue).longValue();
      if (current > old) {
        result = new Long(current);
      }
    } else if (type.startsWith("FLOAT") || type.startsWith("DOUBLE")) {
      Double old = ((Number) oldValue).doubleValue();
      Double current = ((Number) currentValue).doubleValue();
      if (old.compareTo(current) < 0) {
        result = current;
      }
    } else {
      System.out.println("Unknonw max type " + type);
      Long old = ((Number) oldValue).longValue();
      long current = ((Number) currentValue).longValue();
      if (current > old) {
        result = new Long(current);
      }
    }
    return result;

  }

  private static Object updateCountValue(String type, Object oldValue, Object currentValue) {
    Long result = (Long) oldValue;
    result += 1;
    return result;
  }

  private static Object updateSumValue(String type, Object oldValue, Object currentValue) {
    Object result = oldValue;
    if (type.startsWith("TEXT")) {
      result = oldValue.toString().concat(currentValue.toString());
    } else if (type.startsWith("INT")) {
      Long old = ((Number) oldValue).longValue();
      long current = ((Number) currentValue).longValue();
      result = current + old;

    } else if (type.startsWith("FLOAT") || type.startsWith("DOUBLE")) {
      Double old = ((Number) oldValue).doubleValue();
      Double current = ((Number) currentValue).doubleValue();
      result = current + old;
    } else {
      System.out.println("Unknonw type " + type);
      Long old = ((Number) oldValue).longValue();
      long current = ((Number) currentValue).longValue();
      result = current + old;
    }
    return result;
  }

  public static Double computeAvg(Map<String, Object> avgMap) {
    long count = (long) avgMap.get("count");
    double sum = 0;
    if (avgMap.get("sum") instanceof Long) {
      sum += ((Long) avgMap.get("sum")).doubleValue();
    } else if (avgMap.get("sum") instanceof Double) {
      sum += ((Double) avgMap.get("sum")).doubleValue();
    } else {
      sum += (Double) avgMap.get("sum");
    }
    return sum / count;
  }

  public static boolean checkIfIn(JsonObject val, JsonObject set) {
    boolean result = false;
    Object value = getTextFrom(val);
    result = set.getObject("valueSet").containsField(value.toString());
    return result;
  }

  public static JsonObject createValueSet(JsonObject value) {
    JsonObject result = value;
    JsonObject valueSet = new JsonObject();
    JsonArray values = result.getObject("body").getArray("values");
    Iterator<Object> iterator = values.iterator();
    while (iterator.hasNext()) {
      JsonObject val = (JsonObject) iterator.next();
      if (val.getString("type").equals("TEXT")) {
        String textValue = getTextFrom(val);
        valueSet.putString(textValue, "");
      }
    }
    result.putObject("valueSet", valueSet);
    return result;
  }

  public static Object executeFunction(Tuple tuple, JsonObject function) {
    String type = function.getObject("instance").getString("class");
    type = type.substring(type.lastIndexOf(".") + 1);
    if (type.startsWith("Substr")) {
      JsonArray arguments = function.getArray("argEvals");
      JsonObject attributeNameObject = arguments.get(0);
      JsonObject minIndexObject = arguments.get(1);
      JsonObject maxIndexObject = arguments.get(2);
      String attributeName = attributeNameObject.getObject("body").getObject("column").getString("name");
      String attribute = tuple.getGenericAttribute(attributeName).toString();
      int minIndex =
          Math.min(minIndexObject.getObject("body").getObject("datum").getObject("body").getInteger("val"), 0);
      int maxIndex = Math.min(maxIndexObject.getObject("body").getObject("datum").getObject("body").getInteger("val"),
          attribute.length());
      return attribute.substring(minIndex, maxIndex);
    }
    return "";
  }
}
