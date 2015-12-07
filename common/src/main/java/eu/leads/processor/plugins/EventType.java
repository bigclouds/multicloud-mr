package eu.leads.processor.plugins;

/**
 * Created by vagvaz on 6/5/14.
 */
public enum EventType {
  CREATED(1), MODIFIED(2), REMOVED(3);


  public static final EventType[] CREATEANDMODIFY = {CREATED, MODIFIED};
  public static final EventType[] ALL = {CREATED, MODIFIED, REMOVED};
  private int value;

  private EventType(int num) {
    value = num;
  }

  /**
   * {@inheritDoc}
   */
  //    @Override
  //    public String toString() {
  //        return super.toString() + "(" + getValue() + ")";
  //    }

  /**
   * Getter for property 'value'.
   *
   * @return Value for property 'value'.
   */
  public int getValue() {
    return value;
  }
}
