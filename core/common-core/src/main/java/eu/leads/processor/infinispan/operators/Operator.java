package eu.leads.processor.infinispan.operators;


import eu.leads.processor.infinispan.LeadsBaseCallable;
import org.vertx.java.core.json.JsonObject;

public interface Operator {
  void failCleanup();

  public JsonObject getConfiguration();

  public void init(JsonObject config);

  public void execute();

  public void cleanup();

  public void setConfiguration(JsonObject config);

  public String getInput();

  public void setInput(String input);

  public String getOutput();

  public void setOutput(String output);

  public void findPendingMMCFromGlobal();

  public void findPendingRMCFromGlobal();

  public void createCache(String microCloud, String cacheName);

  public void createCaches(boolean isRemote, boolean executeOnlyMap, boolean executeOnlyReduce);

  public void setMapperCallable(LeadsBaseCallable mapperCacllable);

  public void setReducerCallable(LeadsBaseCallable reducerCallable);

  public JsonObject getContinuousMap();

  public JsonObject getContinuousReduceLocal();

  public JsonObject getContinuousReduce();

  public String getContinuousListenerClass();

  //    public void setupExecution();
  public void setupMapCallable();

  public void setupReduceCallable();

  public void executeMap();

  public void executeReduce();

  public void localExecuteMap();

  public void localExecuteReduce();

  //    public void executeAll();
  public boolean isSingleStage();

  public JsonObject getOperatorParameters();

  public void setOperatorParameters(JsonObject parameters);

  public void addResult(String microcloud, String status);

  public void signal();
}
