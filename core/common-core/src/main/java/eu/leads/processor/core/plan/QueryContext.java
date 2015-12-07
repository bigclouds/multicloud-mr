package eu.leads.processor.core.plan;

import eu.leads.processor.core.DataType;
import org.vertx.java.core.json.JsonObject;

/**
 * Created by vagvaz on 8/4/14.
 */
public class QueryContext extends DataType {
  public QueryContext(JsonObject context) {

  }

  public QueryContext(String queryId) {
    setQuery(queryId);
  }

  public String getQuery() {
    return data.getString("queryId");
  }

  public void setQuery(String id) {
    data.putString("queryId", id);
  }
}
