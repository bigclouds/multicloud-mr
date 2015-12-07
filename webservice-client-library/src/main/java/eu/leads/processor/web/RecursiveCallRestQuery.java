package eu.leads.processor.web;

import com.fasterxml.jackson.annotation.JsonAutoDetect;

/**
 * Created by vagvaz on 3/10/14.
 */
@JsonAutoDetect public class RecursiveCallRestQuery {
  String url;
  String depth;
  String user;

  public String getUrl() {
    return url;
  }

  public void setUrl(String url) {
    this.url = url;
  }

  public String getDepth() {
    return depth;
  }

  public void setDepth(String depth) {
    this.depth = depth;
  }

  public String getUser() {
    return user;
  }

  public void setUser(String user) {
    this.user = user;
  }
}
