package eu.leads.processor.infinispan;

import eu.leads.processor.core.Tuple;
import eu.leads.processor.math.FilterOperatorTree;
import org.infinispan.filter.KeyValueFilter;
import org.infinispan.metadata.Metadata;

import java.io.Serializable;

/**
 * Created by vagvaz on 11/15/14.
 */
public class QualFilter implements KeyValueFilter, Serializable {

  public String treeAsString;
  public transient boolean initialized = false;
  public transient FilterOperatorTree tree;

  public QualFilter(String treeNodeAsString) {
    this.treeAsString = treeNodeAsString;
  }

  @Override public boolean accept(Object key, Object value, Metadata metadata) {
    if (!initialized)
      initialize();
    //      System.out.println("key " + key  + " value " + value);
    if (key == null || value == null) {
      return false;
    } else {
      //         Tuple tuple = new Tuple((String)value);
      Tuple tuple = (Tuple) value;
      //         System.out.println("accept?");
      boolean result = tree.accept(tuple);
      //         System.out.println("="+result);
      return result;
    }

  }

  private void initialize() {
    initialized = true;
    //System.out.println("initiailize with " + treeAsString);
    tree = new FilterOperatorTree();
    tree.loadFromJson(treeAsString);
  }
}
