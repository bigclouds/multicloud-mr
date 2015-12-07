package eu.leads.processor.web;

import com.fasterxml.jackson.annotation.JsonAutoDetect;

/**
 * Created by vagvaz on 3/10/14.
 */
@JsonAutoDetect public class WebServiceWorkflow {
  private String user;
  private String workflow; //json format

  public String getUser() {
    return user;
  }

  public void setUser(String user) {
    this.user = user;
  }

  public String getWorkflow() {
    return workflow;
  }

  public void setWorkflow(String workflow) {
    this.workflow = workflow;
  }
}

