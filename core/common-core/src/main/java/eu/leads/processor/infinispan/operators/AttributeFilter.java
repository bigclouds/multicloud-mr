package eu.leads.processor.infinispan.operators;

import eu.leads.processor.core.Tuple;
import org.infinispan.filter.KeyValueFilter;
import org.infinispan.metadata.Metadata;

import java.io.Serializable;

/**
 * Created by vagvaz on 9/28/14.
 */
public class AttributeFilter implements KeyValueFilter<String, Tuple>, Serializable {
  String columnValue;
  String columnKey;

  public AttributeFilter(String columnKey, String columnValue) {
    this.columnKey = columnKey;
    this.columnValue = columnValue;
  }


  @Override public boolean accept(String key, Tuple value, Metadata metadata) {
    if (value == null) {
      //          System.err.println("key " + key + " had null " + value);
      return false;
    }
    Tuple tuple = value;
    //       Tuple tuple = new Tuple(value);
    if (tuple.getGenericAttribute(columnKey).toString().equals(columnValue))
      return true;
    return false;
  }

  //    //TODO FIX this horrible filter
  //    @Override public boolean accept(String o, String o2, Metadata metadata) {
  //        String key = (String)o;
  //        String value = (String)o2;
  //        return accept2(key,value,metadata);
  //    }
}
