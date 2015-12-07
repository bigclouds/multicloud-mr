package eu.leads.processor.web;

import com.fasterxml.jackson.annotation.JsonAutoDetect;

/**
 * Created by vagvaz on 3/7/14.
 */
@JsonAutoDetect public class QueryStatus {
  private String id;
  private String status;
  private String errorMessage;

  public QueryStatus() {
    status = "NONEXISTENT";
  }

  public String getErrorMessage() {
    return errorMessage;
  }

  public void setErrorMessage(String errorMessage) {
    this.errorMessage = errorMessage;
  }

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public String getStatus() {
    return status;
  }

  public void setStatus(String status) {
    this.status = status;
  }

  @Override public String toString() {
    return id + ":" + status + "\n" + errorMessage;

  }
}

