package eu.leads.processor.nqe;

import com.fasterxml.jackson.annotation.JsonAutoDetect;

import java.util.List;

/**
 * Created by vagvaz on 8/4/14.
 */
@JsonAutoDetect public class GetObjectQuery {

  private String table;
  private String key;
  private List<String> attributes;

  public String getTable() {
    return table;
  }

  public void setTable(String table) {
    this.table = table;
  }

  public String getKey() {
    return key;
  }

  public void setKey(String key) {
    this.key = key;
  }

  public List<String> getAttributes() {
    return attributes;
  }

  public void setAttributes(List<String> attributes) {
    this.attributes = attributes;
  }

  @Override public String toString() {
    String result = table + ":" + key + " [ ";
    StringBuilder builder = new StringBuilder();
    for (String attribute : attributes) {
      builder.append(attribute + ", ");
    }
    return result + builder.toString() + " ]";
  }
}
