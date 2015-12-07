package eu.leads.processor.core;

import java.util.Comparator;

/**
 * Created by vagvaz on 9/24/14.
 */
public class TupleComparator implements Comparator<Tuple> {
  String[] sortColumns;
  Boolean[] ascending;
  Integer[] sign;
  String[] types;

  public TupleComparator(String[] sortColumns, Boolean[] ascending, String[] types) {
    this.sortColumns = sortColumns;
    this.ascending = ascending;
    this.sign = new Integer[ascending.length];
    for (int i = 0; i < ascending.length; i++) {


      if (ascending[i])
        sign[i] = 1;
      else
        sign[i] = -1;
    }

    this.types = types;
  }

  @Override public int compare(Tuple o1, Tuple o2) {
    for (int i = 0; i < sortColumns.length; i++) {

      String column = sortColumns[i];
      //         Object value1 = o1.asJsonObject().getValue(column);
      //         Object value2 = o2.asJsonObject().getValue(column);
      Object value1 = o1.getValue(column);
      Object value2 = o2.getValue(column);
      if (value1 == null || value2 == null) {
        //          System.err.println("Comparing tuple " + o1.toString()+"\ntuple " + o2.toString() +"\ncurrent "
        //                               + "columnt" + column + "has null");
        return 0;
      }
      int comparison = TupleUtils.compareValues(value1, value2, types[i]);
      if (comparison != 0) {
        return comparison * sign[i];
      }
    }
    return 0;
  }
}
