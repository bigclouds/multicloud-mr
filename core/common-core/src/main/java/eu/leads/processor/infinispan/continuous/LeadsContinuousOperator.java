package eu.leads.processor.infinispan.continuous;

import eu.leads.processor.infinispan.LeadsCollector;
import org.infinispan.Cache;
import org.vertx.java.core.json.JsonObject;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Future;

/**
 * Created by vagvaz on 10/5/15.
 */
public interface LeadsContinuousOperator {
  public void initializeContinuousOperator(JsonObject conf);

  public void executeOn(Object key, Object value, LeadsCollector collector);

  public void onRemove(Object key, Object value);

  public void setInput(Iterator<Map.Entry> iterator);

  public void setInputCache(Cache cache);

  public Future processInput();

  public Future processInput(List<Map.Entry> input);

  public void finalizeOperator();
}
