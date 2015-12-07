package eu.leads.processor.core.comp;

import java.util.concurrent.Callable;

/**
 * Created by vagvaz on 11/27/14.
 */
public class StartCallable implements Callable {
  boolean callStart;
  ComponentControlVerticle owner;

  public StartCallable(boolean callStart, ComponentControlVerticle componentControlVerticle) {
    this.callStart = callStart;
    this.owner = componentControlVerticle;
  }

  @Override public Object call() throws Exception {
    if (callStart)
      owner.startUp();
    return null;
  }
}
