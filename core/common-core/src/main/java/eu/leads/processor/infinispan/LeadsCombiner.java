package eu.leads.processor.infinispan;

import org.vertx.java.core.json.JsonObject;

/**
 * Created by vagvaz on 2/7/15.
 */
public class LeadsCombiner<K, V> extends LeadsReducer<K, V> {

  public LeadsCombiner(){ super();}

  public LeadsCombiner(JsonObject configuration) {
    super(configuration);
  }

  public LeadsCombiner(String configString) {
    super(configString);
  }
}
