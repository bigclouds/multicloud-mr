package eu.leads.processor.common.infinispan.comm;

/**
 * @author vagvaz
 * @author otrack
 *
 * Created by vagvaz on 7/5/14.
 * A simple constrain class with upper and lower bound
 */
public class Constrain {
  private int lowBound;
  private int upperBound;
  public Constrain(int low, int high) {
    lowBound = low;
    upperBound = high;
  }

  public boolean violates(int value){
    if(value < lowBound
               || value > upperBound){
      return true;
    }
    return false;
  }

}
