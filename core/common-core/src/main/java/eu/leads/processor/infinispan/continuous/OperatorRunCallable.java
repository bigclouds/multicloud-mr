package eu.leads.processor.infinispan.continuous;

import eu.leads.processor.common.utils.PrintUtilities;
import eu.leads.processor.infinispan.LeadsBaseCallable;
import eu.leads.processor.infinispan.LeadsLocalReducerCallable;
import eu.leads.processor.infinispan.LeadsReducerCallable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

/**
 * Created by vagvaz on 10/6/15.
 */
public class OperatorRunCallable implements Callable {
  BasicContinuousOperator owner;
  Logger log;

  public OperatorRunCallable(BasicContinuousOperator basicContinuousOperator) {
    this.owner = basicContinuousOperator;
    log = LoggerFactory.getLogger(this.getClass());
  }

  @Override public Object call() throws Exception {
    LeadsBaseCallable callable = owner.getCallable();
    try {
      for (Object ob : owner.getInputData().entrySet()) {
        Map.Entry entry = (Map.Entry) ob;
        if (callable instanceof LeadsReducerCallable || callable instanceof LeadsLocalReducerCallable) {
          callable.executeOn(entry.getKey(), ((ArrayList) entry.getValue()).iterator());
        } else {
          List values = (List) entry.getValue();
          for (Object value : values) {
            callable.executeOn(entry.getKey(), value);
          }
        }
      }
    } catch (Exception e) {
      e.printStackTrace();
      PrintUtilities.logStackTrace(log, e.getStackTrace());
    }
    owner.getInputData().clear();
    return null;
  }
}
