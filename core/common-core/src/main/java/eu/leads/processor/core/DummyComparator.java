package eu.leads.processor.core;

import java.io.Serializable;
import java.util.Comparator;

/**
 * Created by vagvaz on 8/17/15.
 */
public class DummyComparator implements Comparator, Serializable {
  @Override public int compare(Object o1, Object o2) {
    return 1;
  }
}
