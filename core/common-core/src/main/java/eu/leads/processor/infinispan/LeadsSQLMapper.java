package eu.leads.processor.infinispan;

import org.infinispan.distexec.mapreduce.Collector;
import org.vertx.java.core.json.JsonObject;

/**
 * Created by vagvaz on 2/18/15.
 */
public class LeadsSQLMapper extends LeadsMapper<String, String, String, String> {
  public LeadsSQLMapper(JsonObject configuration) {
    super(configuration);
  }

  public LeadsSQLMapper(String configString) {
    super(configString);
  }

  public LeadsSQLMapper() {
  }

  @Override public void map(String key, String value, Collector<String, String> collector) {

  }
}
