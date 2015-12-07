package eu.leads.processor.infinispan.operators;

import eu.leads.processor.common.StringConstants;
import eu.leads.processor.common.infinispan.InfinispanManager;
import eu.leads.processor.common.utils.PrintUtilities;
import eu.leads.processor.common.utils.ProfileEvent;
import eu.leads.processor.conf.LQPConfiguration;
import eu.leads.processor.core.Action;
import eu.leads.processor.core.ActionStatus;
import eu.leads.processor.core.EngineUtils;
import eu.leads.processor.core.comp.LeadsMessageHandler;
import eu.leads.processor.core.comp.LogProxy;
import eu.leads.processor.core.net.Node;
import eu.leads.processor.infinispan.FlushContinuousListenerCallable;
import eu.leads.processor.infinispan.LeadsBaseCallable;
import eu.leads.processor.infinispan.LeadsMapperCallable;
import eu.leads.processor.infinispan.LeadsReducerCallable;
import eu.leads.processor.infinispan.continuous.BasicContinuousOperatorListener;
import eu.leads.processor.web.ActionResult;
import eu.leads.processor.web.WebServiceClient;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.infinispan.Cache;
import org.infinispan.commons.api.BasicCache;
import org.infinispan.distexec.DefaultExecutorService;
import org.infinispan.distexec.DistributedExecutorService;
import org.infinispan.distexec.DistributedTask;
import org.infinispan.distexec.DistributedTaskBuilder;
import org.infinispan.remoting.transport.jgroups.SuspectException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vertx.java.core.json.JsonObject;

import java.io.IOException;
import java.net.MalformedURLException;
import java.util.*;
import java.util.concurrent.*;

/**
 * Created by tr on 30/8/2014.
 */
public abstract class BasicOperator extends Thread implements Operator {

  protected JsonObject conf;
  protected Action action;
  protected InfinispanManager manager;
  protected Node com;
  protected Cache statisticsCache;
  protected Logger log;
  protected LogProxy logg;
  protected JsonObject globalConfig;
  protected Set<String> pendingMMC;
  protected Set<String> pendingRMC;
  protected String currentCluster;
  protected LeadsBaseCallable mapperCallable;
  protected LeadsBaseCallable reducerCallable;
  protected LeadsBaseCallable reducerLocalCallable;
  protected Cache inputCache;
  protected BasicCache outputCache;
  protected Cache reduceInputCache;
  protected Cache reduceLocalInputCache;
  protected String finalOperatorName, statInputSizeKey, statOutputSizeKey, statExecTimeKey;
  protected boolean isRemote = false;
  protected boolean executeOnlyMap = false;
  protected boolean executeOnlyReduce = false;
  protected boolean reduceLocal = false;
  protected boolean isRecCompReduceLocal = false;
  protected boolean isRecCompReduce = false;
  long startTime;
  protected boolean failed = false;
  protected volatile Object mmcMutex = new Object();
  private volatile Object rmcMutex = new Object();
  protected LeadsMessageHandler handler = new CompleteExecutionHandler(com, this);
  protected Map<String, String> mcResults;
  protected Logger profilerLog = LoggerFactory.getLogger("###PROF###" + this.getClass().toString());
  protected ProfileEvent profOperator;
  protected JsonObject mapContinuousConf;
  protected JsonObject reduceLocalContinuousConf;
  protected JsonObject reduceContinuousConf;

  protected BasicOperator(Action action) {
    conf = action.getData();
    this.action = action;
    profOperator = new ProfileEvent("Operator- " + this.getClass().toString(), profilerLog);
    reduceLocal = action.getData().getObject("operator").containsField("reduceLocal");
    log = LoggerFactory.getLogger(this.getClass());
  }

  protected BasicOperator(Node com, InfinispanManager manager, LogProxy logProxy, Action action) {
    super(com.getId() + "-" + action.getId() + "-basic-operator-thread-" + UUID.randomUUID().toString());
    log = LoggerFactory.getLogger(this.getClass());
    EngineUtils.initialize();
    System.err.println(this.getClass().getCanonicalName());
    mcResults = new HashMap<>();
    this.com = com;
    this.manager = manager;
    this.logg = logProxy;
    this.action = action;
    this.conf = action.getData().getObject("operator").getObject("configuration");

    isRemote = action.asJsonObject().containsField("remote");
    if (isRemote) {

      executeOnlyMap = action.asJsonObject().containsField("map");
      executeOnlyReduce = action.asJsonObject().containsField("reduce");
      reduceLocal = action.getData().getObject("operator").containsField("reduceLocal");
      isRecCompReduceLocal = action.getData().getObject("operator").containsField("recComposableReduceLocal");
      isRecCompReduce = action.getData().getObject("operator").containsField("recComposableReduce");
      System.err.println(
          "IS REMOTE TRUE map " + executeOnlyMap + " reduce local " + reduceLocal + " reduce " + executeOnlyReduce
              + " recCompRL " + isRecCompReduceLocal + " recCompR " + isRecCompReduce);
      profilerLog.error(
          "IS REMOTE TRUE map " + executeOnlyMap + " reduce local " + reduceLocal + " reduce " + executeOnlyReduce
              + " recCompRL " + isRecCompReduceLocal + " recCompR " + isRecCompReduce);
    } else {
      executeOnlyMap = true;
      executeOnlyReduce = true;

      reduceLocal = action.getData().getObject("operator").containsField("reduceLocal");
      isRecCompReduceLocal = action.getData().getObject("operator").containsField("recComposableLocalReduce");
      isRecCompReduce = action.getData().getObject("operator").containsField("recComposableReduce");
      System.err.println(
          "REMOTE FALSE map " + executeOnlyMap + " reduce local " + reduceLocal + " reduce " + executeOnlyReduce
              + " recCompRL " + isRecCompReduceLocal + " recCompR " + isRecCompReduce);
      profilerLog.error(
          "REMOTE FALSE map " + executeOnlyMap + " reduce local " + reduceLocal + " reduce " + executeOnlyReduce
              + " recCompRL " + isRecCompReduceLocal + " recCompR " + isRecCompReduce);
    }

    this.globalConfig = action.getGlobalConf();
    this.currentCluster = LQPConfiguration.getInstance().getMicroClusterName();
    this.statisticsCache = (Cache) manager.getPersisentCache(StringConstants.STATISTICS_CACHE);
    this.init_statistics(this.getClass().getCanonicalName());
    startTime = System.currentTimeMillis();
    profOperator = new ProfileEvent("Operator " + this.getClass().toString(), profilerLog);
    reduceLocal = action.getData().getObject("operator").containsField("reduceLocal");
    JsonObject targetEndpoints = action.getData().getObject("operator").getObject("targetEndpoints");
    if(targetEndpoints == null)
    {
      action.getData().getObject("operator").putObject("targetEndpoints",globalConfig.getObject("microclouds").copy());
    }
    JsonObject scheduling = action.getData().getObject("operator").getObject("scheduling");
    if(scheduling == null){
      action.getData().getObject("operator").putObject("scheduling",globalConfig.getObject("microclouds").copy());
    }
  }

  public JsonObject getConf() {
    return conf;
  }

  public void setConf(JsonObject conf) {
    this.conf = conf;
  }

  public Action getAction() {
    return action;
  }

  public void setAction(Action action) {
    this.action = action;
  }

  public InfinispanManager getManager() {
    return manager;
  }

  public void setManager(InfinispanManager manager) {
    this.manager = manager;
  }

  public Node getCom() {
    return com;
  }

  public void setCom(Node com) {
    this.com = com;
  }

  public LogProxy getLog() {
    return logg;
  }

  public void setLog(LogProxy log) {
    this.logg = log;
  }

  public JsonObject getGlobalConfig() {
    return globalConfig;
  }

  public void setGlobalConfig(JsonObject globalConfig) {
    this.globalConfig = globalConfig;
  }

  public Set<String> getPendingMMC() {
    return pendingMMC;
  }

  public void setPendingMMC(Set<String> pendingMMC) {
    this.pendingMMC = pendingMMC;
  }

  public Set<String> getPendingRMC() {
    return pendingRMC;
  }

  public void setPendingRMC(Set<String> pendingRMC) {
    this.pendingRMC = pendingRMC;
  }

  public String getCurrentCluster() {
    return currentCluster;
  }

  public void setCurrentCluster(String currentCluster) {
    this.currentCluster = currentCluster;
  }


  public LeadsBaseCallable getMapperCallable() {
    return mapperCallable;
  }

  @Override public void setMapperCallable(LeadsBaseCallable mapperCallable) {
    this.mapperCallable = mapperCallable;
  }

  public LeadsBaseCallable getReducerCallable() {
    return reducerCallable;
  }

  @Override public void setReducerCallable(LeadsBaseCallable reducerCallable) {
    this.reducerCallable = reducerCallable;
  }

  public String getFinalOperatorName() {
    return finalOperatorName;
  }

  public void setFinalOperatorName(String finalOperatorName) {
    this.finalOperatorName = finalOperatorName;
  }

  public String getStatInputSizeKey() {
    return statInputSizeKey;
  }

  public void setStatInputSizeKey(String statInputSizeKey) {
    this.statInputSizeKey = statInputSizeKey;
  }

  public String getStatOutputSizeKey() {
    return statOutputSizeKey;
  }

  public void setStatOutputSizeKey(String statOutputSizeKey) {
    this.statOutputSizeKey = statOutputSizeKey;
  }

  public String getStatExecTimeKey() {
    return statExecTimeKey;
  }

  public void setStatExecTimeKey(String statExecTimeKey) {
    this.statExecTimeKey = statExecTimeKey;
  }

  public long getStartTime() {
    return startTime;
  }

  public void setStartTime(long startTime) {
    this.startTime = startTime;
  }


  protected void init_statistics(String finalOperatorName) {
    this.finalOperatorName = finalOperatorName;
    this.statInputSizeKey = finalOperatorName + "inputSize";
    this.statOutputSizeKey = finalOperatorName + "outputSize";
    this.statExecTimeKey = finalOperatorName + "timeSize";
  }

  @Override public void init(JsonObject config) {
    this.conf = config;
  }

  @Override public void execute() {
    //    profOperator.end("execute");
    startTime = System.currentTimeMillis();
    System.out.println("Execution Start! ");
    //    profOperator.start("Execute()");
    this.start();
  }

  @Override public void cleanup() {

    unsubscribeToMapActions("execution." + getName() + "." + com.getId() + "." + action.getId());
    action.setStatus(ActionStatus.COMPLETED.toString());
    if (com != null) {
      com.sendTo(action.getData().getString("owner"), action.asJsonObject());
    } else {
      System.err.println("PROBLEM Uninitialized com");
    }
    updateStatistics(inputCache, null, outputCache);
  }

  @Override public void failCleanup() {
    //    if(!isRemote)
    //    {
    unsubscribeToMapActions("execution." + getName() + "." + com.getId() + "." + action.getId());
    //    }
    action.setStatus(ActionStatus.COMPLETED.toString());

    //    pendingMMC.clear();
    //    pendingRMC.clear();

    //    mmcMutex.notifyAll();

    //      rmcMutex.notifyAll();
    if (com != null) {
      com.sendTo(action.getData().getString("owner"), action.asJsonObject());
    } else {
      System.err.println("PROBLEM Uninitialized com");
    }
    profOperator.end("failclean");
  }

  public void updateStatistics(BasicCache input1, BasicCache input2, BasicCache output) {
    long endTime = System.currentTimeMillis();

    long inputSize = 1;
    long outputSize = 1;
    //    if(input1 != null)
    //      inputSize += input1.size();
    //    if(input2 != null)
    //      inputSize += input2.size();
    //    if(output != null){
    //      outputSize = output.size();
    //    }
    //    else{
    //      outputCache = (BasicCache) manager.getPersisentCache(getOutput());
    //      outputSize = outputCache.size();
    //    }
    if (outputSize == 0) {
      outputSize = 1;
    }
    System.err.println(
        "In#: " + inputSize + " Out#:" + outputSize + " Execution time: " + (endTime - startTime) / 1000.0 + " s for "
            + this.getClass().toString() + "\n" + getName());
    profilerLog.error(
        "In#: " + inputSize + " Out#:" + outputSize + " Execution time: " + (endTime - startTime) / 1000.0 + " s for "
            + this.getClass().toString() + "\n" + getName());

    //    updateStatisticsCache(inputSize, outputSize, (endTime - startTime));
  }

  public void updateStatisticsCache(double inputSize, double outputSize, double executionTime) {
    updateSpecificStatistic(statInputSizeKey, inputSize);
    updateSpecificStatistic(statOutputSizeKey, outputSize);
    updateSpecificStatistic(statExecTimeKey, executionTime);
  }

  public void updateSpecificStatistic(String StatNameKey, double NewValue) {
    DescriptiveStatistics stats;
    if (!statisticsCache.containsKey(StatNameKey)) {
      stats = new DescriptiveStatistics();
      //stats.setWindowSize(1000);
    } else {
      stats = (DescriptiveStatistics) statisticsCache.get(StatNameKey);
    }
    stats.addValue(NewValue);
    statisticsCache.put(StatNameKey, stats);
  }

  @Override public JsonObject getConfiguration() {
    return conf;
  }

  @Override public void setConfiguration(JsonObject config) {
    conf = config;
  }

  @Override public String getInput() {
    return action.getData().getObject("operator").getArray("inputs").get(0).toString();
  }

  @Override public void setInput(String input) {
    conf.putString("input", input);
  }

  @Override public String getOutput() {
    return action.getData().getObject("operator").getString("id");
  }


  @Override public void setOutput(String output) {
    conf.putString("output", output);
  }

  @Override public JsonObject getOperatorParameters() {
    return conf;
  }

  @Override public void setOperatorParameters(JsonObject parameters) {
    conf = parameters;
  }

  //   @Override
  //   public  boolean isSingleStage(){return true;}

  @Override public void findPendingMMCFromGlobal() {
    pendingMMC = new HashSet<>();
    if (executeOnlyMap) {
      for (String mc : getMicroCloudsFromOpSched()) {
        pendingMMC.add(mc);
      }
      //      if(pendingMMC.contains(currentCluster)){
      //        pendingMMC.add(currentCluster);
      //      }
    }
  }

  @Override public void findPendingRMCFromGlobal() {
    pendingRMC = new HashSet<>();

    if (isSingleStage()) {
      return;
    }
    if (executeOnlyReduce) {
      if (!isRemote) {
        for (String mc : getMicroCloudsFromOpTarget()) {
          pendingRMC.add(mc);
        }
      } else {
        pendingRMC.add(LQPConfiguration.getInstance().getMicroClusterName());
      }
    }
  }

  @Override public void run() {
    ProfileEvent runProf = new ProfileEvent("findPendingMMCFromGlobal() " + this.getClass().toString(), profilerLog);
    findPendingMMCFromGlobal();
    runProf.end();
    runProf.start("findPendingRMCFromGlobal()");
    findPendingRMCFromGlobal();
    runProf.end();
    runProf.start("createCaches()");
    createCaches(isRemote, executeOnlyMap, executeOnlyReduce);
    runProf.end();

    if (executeOnlyMap) {
      runProf.start("setupMapCallable()");
      setupMapCallable();
      runProf.end();
      runProf.start("executeMap()");
      executeMap();
      runProf.end();
    }
    if (!failed) {
      if (executeOnlyReduce) {
        runProf.start("setupReduceCallable()");
        setupReduceCallable();
        runProf.end();
        runProf.start("executeReduce()");
        executeReduce();
        runProf.end();
      }
      if (!failed) {
        runProf.start("run_cleanup()");
        cleanup();
        runProf.end();
      } else {
        runProf.start("fail_cleanup()");
        failCleanup();
        runProf.end();
      }
    } else {
      runProf.start("fail_cleanup()");
      failCleanup();
      runProf.end();
    }
    pendingMMC.clear();
    pendingRMC.clear();
    //    synchronized(mmcMutex){
    //      mmcMutex.notifyAll();
    //    }
    //    try {
    //      this.join();
    //    } catch (InterruptedException e) {
    //      e.printStackTrace();
    //    }
    System.err.println("END OF RUN FOR " + this.getClass().toString() + " " + getName());
  }

  public void createCache(String microCloud, String cacheName) {

    String uri = getURIForMC(microCloud);
    try {
      WebServiceClient.initialize(uri);
    } catch (MalformedURLException e) {
      e.printStackTrace();
    }
    try {
      WebServiceClient.putObject(cacheName, "", new JsonObject());
    } catch (IOException e) {
      e.printStackTrace();
    }

  }

  public void createCache(String microCloud, String cacheName, String listenerName) {

    String uri = getURIForMC(microCloud);
    try {
      WebServiceClient.initialize(uri);
    } catch (MalformedURLException e) {
      e.printStackTrace();
    }
    try {
      WebServiceClient.putObject(cacheName, "", new JsonObject().putString("listener", listenerName));
    } catch (IOException e) {
      e.printStackTrace();
    }

  }

  @Override public void signal() {
    synchronized (mmcMutex) {
      mmcMutex.notifyAll();
    }
  }

  @Override public synchronized void addResult(String mc, String status) {
    //    synchronized (mmcMutex) {
    System.out.println("Deliver " + mc + " " + status + this.getClass().toString() + " " + getName());
    PrintUtilities.printList(Arrays.asList(pendingMMC));
    mcResults.put(mc, status);
    pendingMMC.remove(mc);
    System.out.println("AFTER\n");
    PrintUtilities.printList(Arrays.asList(pendingMMC));
  }

  @Override public void executeMap() {

    subscribeToMapActions(pendingMMC);
    Set<String> remoteRequests = new HashSet<>(pendingMMC);
    if (!isRemote) {
      for (String mc : remoteRequests) {
        if (!mc.equals(currentCluster)) {
          sendRemoteRequest(mc, true);
        }
      }
    }

    if (pendingMMC.contains(currentCluster)) {
      ProfileEvent lem = new ProfileEvent("ActualExecMap", profilerLog);
      localExecuteMap();
      lem.end();
    }

    int size = Integer.MAX_VALUE;
    synchronized (mmcMutex) {
      size = pendingMMC.size();
    }
    while (size > 0) {
      synchronized (mmcMutex) {
        System.out.println(
            "Sleeping to executing " + mapperCallable.getClass().toString() + " pending clusters " + this.getClass()
                .toString() + " " + this.getName());
        PrintUtilities.printList(Arrays.asList(pendingMMC));
        try {
          //            System.err.println("JUST BEF " + this.toString() + " threadname " + getName());
          mmcMutex.wait(120000);
          //            System.err.println("JUST AF " + this.toString() + " threadname " + getName());
          size = pendingMMC.size();
        } catch (InterruptedException e) {
          profilerLog.error("Interrupted " + e.getMessage());
          break;
        }
      }
    }

    for (Map.Entry<String, String> entry : mcResults.entrySet()) {
      System.out.println("Execution on " + entry.getKey() + " was " + entry.getValue());
      profilerLog.error("Execution on " + entry.getKey() + " was " + entry.getValue());
      if (entry.getValue().equals("FAIL"))
        failed = true;
    }

  }

  public void sendRemoteRequest(String mc, boolean b) {
    System.err.println("Send remote request to mc " + mc);
    profilerLog.error("Send remote request to mc " + mc);
    Action copyAction = new Action(new JsonObject(action.toString()));
    Action newAction = new Action(copyAction);
    JsonObject dataAction = copyAction.asJsonObject().copy();
    //    serror("DATA ACTION = "+dataAction.toString());
    JsonObject sched = new JsonObject();
    sched.putArray(mc, globalConfig.getObject("microclouds").getArray(mc));
    //    if(dataAction.containsField("data")){
    //      System.err.println("contains data");
    //      if(dataAction.getObject("data").containsField("operator")){
    //        System.err.println("contains operator");
    //        if(dataAction.getObject("data").getObject("operator").containsField("scheduling")){
    //          System.err.println("contains schedu");
    //        }
    //      }

    //    }
    //    else{
    //      for(String s : dataAction.getFieldNames()){
    //        System.err.println(s);
    //      }
    //    }
    dataAction.getObject("data").getObject("operator").putObject("scheduling", sched);
    newAction.setData(dataAction);
    newAction.getData().putString("remote", "remote");
    if (b) {
      newAction.getData().putString("map", "map");
    } else {
      newAction.getData().putString("reduce", "reduce");

    }
    newAction.getData().putString("replyGroup", "execution." + getName() + "." + com.getId() + "." + action.getId());
    newAction.getData().putString("coordinator", currentCluster);
    String uri = getURIForMC(mc);
    try {

      System.out.println("Sending to " + uri + " new action " + newAction.asJsonObject().encodePrettily());
      ActionResult remoteResult = WebServiceClient.executeMapReduce(newAction.asJsonObject(), uri);
      System.out.println("Reply  " + remoteResult.toString());
      if (remoteResult.getStatus().equals("FAIL")) {
        profilerLog.error("Remote invocation for " + mc + " failed ");
        replyForFailExecution(newAction);
      }
    } catch (MalformedURLException e) {
      profilerLog.error("Problem initializing web service client");
      newAction.getData().putString("microcloud", mc);
      newAction.getData().putString("STATUS", "FAIL");
      replyForFailExecution(newAction);
    } catch (IOException e) {
      profilerLog.error("Problem remote callling remote execution web service client");
      newAction.getData().putString("microcloud", mc);
      newAction.getData().putString("STATUS", "FAIL");
      replyForFailExecution(newAction);
    }

  }

  public void subscribeToMapActions(Set<String> pendingMMC) {
    final boolean[] registered = {false};
    //TODO VAGVAZ
    synchronized (rmcMutex) {
      com.subscribe("execution." + getName() + "." + com.getId() + "." + action.getId(), handler, new Callable() {
        @Override public Object call() throws Exception {
          synchronized (rmcMutex) {
            // Thread.sleep(5000);
            registered[0] = true;
            rmcMutex.notifyAll();
          }
          return null;

        }
      });
      try {
        if (!registered[0]){
          rmcMutex.wait();
        }
      } catch (InterruptedException e) {
        profilerLog.error("Subscription wait interreped " + e.getMessage());
        System.err.println("Subscription wait interreped " + e.getMessage());
      }
    }

  }

  public void unsubscribeToMapActions(String group) {
    com.unsubscribe(group);
  }

  @Override public void localExecuteMap() {
    long start = System.currentTimeMillis();
    if (mapperCallable != null) {
      //      if (inputCache.size() == 0) {
      //        replyForSuccessfulExecution(action);
      //        return;
      //      }
      setMapperCallableEnsembleHost();
      if (mapperCallable instanceof LeadsMapperCallable) {
        //        LeadsMapperCallable lm = (LeadsMapperCallable) mapperCallable;
        MapReduceOperator mr = (MapReduceOperator) this;
        System.err.println(
            "EXECUTE " + mr.mapper.getClass().toString() + " ON " + currentCluster + Thread.currentThread().toString());
        profilerLog.error(
            "EXECUTE " + mr.mapper.getClass().toString() + " ON " + currentCluster + Thread.currentThread().toString());
        profilerLog.error("LEM: " + mr.mapper.getClass().toString() + " " + mapperCallable.getEnsembleHost());
      } else {
        System.err.println(
            "EXECUTE " + mapperCallable.getClass().toString() + " ON " + currentCluster + Thread.currentThread()
                .toString());
        profilerLog.error(
            "EXECUTE " + mapperCallable.getClass().toString() + " ON " + currentCluster + Thread.currentThread()
                .toString());
        profilerLog.error("LEM: " + mapperCallable.getClass().toString() + " " + mapperCallable.getEnsembleHost());
      }
      //      ProfileEvent distTask = new ProfileEvent("setup map taks " + mapperCallable.getClass().toString(),profilerLog);
      DistributedExecutorService des = new DefaultExecutorService(inputCache);
      System.err.println("serrbuilding dist task");
      profilerLog.error("logbuilding dist task");
      DistributedTaskBuilder builder = des.createDistributedTaskBuilder(mapperCallable);
      builder.timeout(24, TimeUnit.HOURS);
      DistributedTask task = builder.build();
      System.err.println("serrsubmitting to local cluster task");
      profilerLog.error("log submitting to local cluster task");

      List<Future<String>> res = des.submitEverywhere(task);//new Arr?ayList<>();
      //      List<Address> taskNodes =  inputCache.getAdvancedCache().getRpcManager().getMembers();
      //      taskNodes.add(inputCache.getCacheManager().getAddress());
      //      for(Address node : taskNodes){
      //        Future<String> ft = des.submit(node,task);
      //        res.add(ft);
      //      }
      //      distTask.end();
      //      Future<String> res = des.submit(callable);
      //      List<String> addresses = new ArrayList<String>();
      try {
        if (res != null) {
          double perc = handleCallables(res, mapperCallable);
          //          for (Future<?> result : res) {
          //            System.out.println(result.get());
          //            addresses.add((String) result.get());
          //          }
          System.out.println("map " + mapperCallable.getClass().toString() +
              " Execution is done " + perc);
          profilerLog.info("map " + mapperCallable.getClass().toString() +
              " Execution is done " + perc);
        } else {
          System.out.println("map " + mapperCallable.getClass().toString() +
              " Execution not done");
          profilerLog.info("map " + mapperCallable.getClass().toString() +
              " Execution not done");
          failed = true;
          replyForFailExecution(action);
        }
      } catch (InterruptedException e) {
        profilerLog
            .error("Interrupted Exception in Map Excuettion " + "map " + mapperCallable.getClass().toString() + "\n" +
                e.getClass().toString());
        profilerLog.error(e.getMessage());
        System.err.println("Exception in Map Excuettion " + "map " + mapperCallable.getClass().toString() + "\n" +
                e.getClass().toString());
        System.err.println(e.getMessage());
        failed = true;
        profilerLog.error(e.getStackTrace().toString());
        e.printStackTrace();
        replyForFailExecution(action);
      } catch (Exception e) {
        profilerLog
            .error("Execution Exception in Map Excuettion " + "map " + mapperCallable.getClass().toString() + "\n" +
                e.getClass().toString());
        profilerLog.error(e.getMessage());
        System.err.println("Exception in Map Excuettion " + "map " + mapperCallable.getClass().toString() + "\n" +
            e.getClass().toString());
        System.err.println(e.getMessage());
        profilerLog.error(e.getStackTrace().toString());
        failed = true;
        e.printStackTrace();
        replyForFailExecution(action);
      }
    }
    long end = System.currentTimeMillis();
    PrintUtilities.printAndLog(log,"TIME FOR MAP = " + (end - start) / 1000f);
    if (reduceLocal) {
      setupReduceLocalCallable();
      executeReduceLocal();
    }
    replyForSuccessfulExecution(action);
  }

  public void setMapperCallableEnsembleHost() {
    mapperCallable.setEnsembleHost(computeEnsembleHost(true));
  }

  public void setReducerCallableEnsembleHost() {

    reducerCallable.setEnsembleHost(computeEnsembleHost(false,true));
  }

  public void setReducerLocaleEnsembleHost() {
    reducerLocalCallable.setEnsembleHost(computeEnsembleHost(false));
  }

  public String computeEnsembleHost(boolean isMap) {
    String result = "";
    JsonObject targetEndpoints = action.getData().getObject("operator").getObject("targetEndpoints");
    List<String> sites = new ArrayList<>();

    for (String targetMC : targetEndpoints.getFieldNames()) {
      //         JsonObject mc = targetEndpoints.getObject(targetMC);
      sites.add(targetMC);
      //
    }

    Collections.sort(sites);
    if (isMap && reduceLocal) { // compute is run for map and there reducelocal should run
      result += globalConfig.getObject("componentsAddrs").getArray(LQPConfiguration.getInstance().getMicroClusterName())
          .get(0).toString() + "|";
    } else {
      for (String site : sites) {
        // If reduceLocal, we only need the local micro-cloud
        result += globalConfig.getObject("componentsAddrs").getArray(site).get(0).toString() + "|";

      }
    }
    result = result.substring(0, result.length() - 1);
    profilerLog.error("EnsembleHost: " + result);

    return result;
  }

  public String computeEnsembleHost(boolean isMap,boolean islocal) {
    String result = "";
    JsonObject targetEndpoints = action.getData().getObject("operator").getObject("targetEndpoints");
    List<String> sites = new ArrayList<>();

    for (String targetMC : targetEndpoints.getFieldNames()) {
      //         JsonObject mc = targetEndpoints.getObject(targetMC);
      sites.add(targetMC);
      //
    }

    Collections.sort(sites);
    if (islocal) { // compute is run for map and there reducelocal should run
      result += globalConfig.getObject("componentsAddrs").getArray(LQPConfiguration.getInstance().getMicroClusterName())
          .get(0).toString() + "|";
    } else {
      for (String site : sites) {
        // If reduceLocal, we only need the local micro-cloud
        result += globalConfig.getObject("componentsAddrs").getArray(site).get(0).toString() + "|";

      }
    }
    result = result.substring(0, result.length() - 1);
    profilerLog.error("EnsembleHost: " + result);

    return result;
  }

  String computeEnsembleHost() {
    String result = "";
    JsonObject targetEndpoints = null;
    if (!conf.containsField("next")) {
      targetEndpoints = action.getData().getObject("operator").getObject("targetEndpoints");
    } else {
      targetEndpoints = conf.getObject("next").getObject("targetEndpoints");
    }
    if (targetEndpoints == null)
      return null;
    List<String> sites = new ArrayList<>();
    for (String targetMC : targetEndpoints.getFieldNames()) {
      //         JsonObject mc = targetEndpoints.getObject(targetMC);
      sites.add(targetMC);
      //
    }
    Collections.sort(sites);
    for (String site : sites) {
      result += globalConfig.getObject("componentsAddrs").getArray(site).get(0).toString() + "|";//+":11222|";
    }
    result = result.substring(0, result.length() - 1);
    profilerLog.error("EnsembleHost: " + result);
    return result;
  }

  @Override public void localExecuteReduce() {
    long start = System.currentTimeMillis();
    if (reducerCallable != null) {
      if (reducerCallable instanceof LeadsReducerCallable) {
        //        LeadsMapperCallable lm = (LeadsMapperCallable) mapperCallable;
        MapReduceOperator mr = (MapReduceOperator) this;
        System.err.println(
            "EXECUTE REDUCER" + mr.federationReducer.getClass().toString() + " ON " + currentCluster + Thread.currentThread()
                .toString());
        profilerLog.error(
            "EXECUTE REDCUER" + mr.federationReducer.getClass().toString() + " ON " + currentCluster + Thread.currentThread()
                .toString());
        profilerLog.error("LEM: " + mr.federationReducer.getClass().toString() + " " + reducerCallable.getEnsembleHost());
      } else {
        System.err.println(
            "EXECUTE " + reducerCallable.getClass().toString() + " ON " + currentCluster + Thread.currentThread()
                .toString());
        profilerLog.error(
            "EXECUTE " + reducerCallable.getClass().toString() + " ON " + currentCluster + Thread.currentThread()
                .toString());
        profilerLog.error("LEM: " + reducerCallable.getClass().toString() + " " + reducerCallable.getEnsembleHost());
      }

      DistributedExecutorService des = new DefaultExecutorService(reduceInputCache);
      setReducerCallableEnsembleHost();

      DistributedTaskBuilder builder = null;
      if (!isRecCompReduce) {
        builder = des.createDistributedTaskBuilder(reducerCallable);
      } else {
        builder = des.createDistributedTaskBuilder(new FlushContinuousListenerCallable("{}", ""));
      }
      builder.timeout(24, TimeUnit.HOURS);
      DistributedTask task = builder.build();
      List<Future<String>> res = des.submitEverywhere(task);
      //      Future<String> res = des.submit(callable);
      //      List<String> addresses = new ArrayList<String>();
      try {
        if (res != null) {
          //          for (Future<?> result : res) {
          double percentage = handleCallables(res, reducerCallable);
          System.out.println("reduce " + reducerCallable.getClass().toString() +
              " Execution is done " + percentage);
          profilerLog.info("reduce " + reducerCallable.getClass().toString() +
              " Execution is done " + percentage);
        } else {
          System.out.println("reduce " + reducerCallable.getClass().toString() +
              " Execution not done");
          profilerLog.info("reduce " + reducerCallable.getClass().toString() +
              " Execution not done");
          failed = true;
          replyForFailExecution(action);
        }
      } catch (InterruptedException e) {
        profilerLog.error("Exception in reduce Excuettion " + "reduce " + reducerCallable.getClass().toString() + "\n" +
            e.getClass().toString());
        profilerLog.error(e.getMessage());
        System.err
            .println("Exception in reduce Excuettion " + "reduce " + reducerCallable.getClass().toString() + "\n" +
                e.getClass().toString());
        System.err.println(e.getMessage());
        failed = true;
        replyForFailExecution(action);
        e.printStackTrace();
      } catch (Exception e) {
        profilerLog.error("Exception in reduce Excuettion " + "reduce " + reducerCallable.getClass().toString() + "\n" +
            e.getClass().toString());
        profilerLog.error(e.getMessage());
        System.err.println("Exception in reduce Excuettion " + "map " + reducerCallable.getClass().toString() + "\n" +
            e.getClass().toString());
        System.err.println(e.getMessage());
        failed = true;
        replyForFailExecution(action);
        e.printStackTrace();
      }
    }
    long end = System.currentTimeMillis();

    PrintUtilities.printAndLog(log, "TIME FOR FEDREDUCE = " + (end - start) / 1000f);
    replyForSuccessfulExecution(action);
  }

  public void replyForSuccessfulExecution(Action action) {
    action.getData().putString("microcloud", currentCluster);
    action.getData().putString("STATUS", "SUCCESS");
    com.sendTo("execution." + getName() + "." + com.getId() + "." + action.getId(), action.asJsonObject());
  }

  public void replyForFailExecution(Action action) {
    if (!action.getData().containsField("microcloud")) {
      action.getData().putString("microcloud", currentCluster);
    }
    action.getData().putString("STATUS", "FAIL");
    com.sendTo("execution." + getName() + "." + com.getId() + "." + action.getId(), action.asJsonObject());
  }

  @Override public void executeReduce() {
    System.err.println(
        "RUNNING REDUCE ON CLUSTER " + this.getClass().toString() + " " + currentCluster + Thread.currentThread()
            .toString());
    pendingMMC = new HashSet<>();
    mcResults = new HashMap<>();
    pendingMMC.addAll(pendingRMC);
    if (!executeOnlyMap) {
      subscribeToMapActions(pendingMMC);
    }
    if (!isRemote) {
      HashSet<String> remoteRequests = new HashSet<>(pendingMMC);
      for (String mc : remoteRequests) {
        if (!mc.equals(currentCluster)) {
          sendRemoteRequest(mc, false);
        }
      }
    }

    if (pendingMMC.contains(currentCluster)) {
      System.err.println("RUNNING FREDUCE ON CLUSTER " + currentCluster);
      localExecuteReduce();
      System.err.println("END FREDUCE ON CLUSTER " + currentCluster);
    }

    synchronized (mmcMutex) {
      while (pendingMMC.size() > 0) {
        System.out.println("Sleeping to executing " + reducerCallable.getClass().toString() + " pending clusters ");
        PrintUtilities.printList(Arrays.asList(pendingMMC));
        try {
          mmcMutex.wait(240000);
        } catch (InterruptedException e) {
          profilerLog.error("REduce Interrupted " + e.getMessage());
          break;
        }
      }
    }
    for (Map.Entry<String, String> entry : mcResults.entrySet()) {
      System.out.println("Reduce Execution on " + entry.getKey() + " was " + entry.getValue());
      profilerLog.error("Reduce Execution on " + entry.getKey() + " was " + entry.getValue());
      if (entry.getValue().equals("FAIL"))
        failed = true;
    }
  }

  public String getURIForMC(String microCloud) {
    profilerLog.error("Getting uriformc " + microCloud + " " + globalConfig.getObject("microclouds").toString());
    String uri = globalConfig.getObject("microclouds").getArray(microCloud).get(0);

    if (!uri.startsWith("http:")) {
      uri = "http://" + uri;
    }
    try {
      String portString = uri.substring(uri.lastIndexOf(":") + 1);
      int port = Integer.parseInt(portString);
    } catch (Exception e) {
      profilerLog.error("Parsing port execption " + e.getMessage());
      System.err.println("Parsing port execption " + e.getMessage());
      if (uri.endsWith(":")) {
        uri = uri + "8080";
      } else {
        uri = uri + ":8080";
      }

    }
    return uri;
  }

  public Set<String> getMicroCloudsFromOpSched() {
    Set<String> result = new HashSet<>();
    JsonObject operator = action.getData().getObject("operator");
    JsonObject scheduling = operator.getObject("scheduling");
    for (String mc : scheduling.getFieldNames()) {
      result.add(mc);
    }

    return result;
  }

  public String getEnsembleHost(Set<String> runningMC) {
    String result = "";
    JsonObject targetEndpoints = action.getData().getObject("operator").getObject("targetEndpoints");
    List<String> sites = new ArrayList<>();
    for (String targetMC : runningMC) {
      //         JsonObject mc = targetEndpoints.getObject(targetMC);
      sites.add(targetMC);
      //
    }
    Collections.sort(sites);
    for (String site : sites) {
      result += globalConfig.getObject("componentsAddrs").getArray(site).get(0).toString() + "|";//:11222|";
    }
    result = result.substring(0, result.length() - 1);
    return result;
  }

  public Set<String> getMicroCloudsFromOpTarget() {
    Set<String> result = new HashSet<>();
    JsonObject operator = action.getData().getObject("operator");
    JsonObject scheduling = operator.getObject("targetEndpoints");
    for (String mc : scheduling.getFieldNames()) {
      result.add(mc);
    }

    return result;
  }

  public Set<String> getTargetMC() {
    Set<String> result = new HashSet<>();
    JsonObject targetEndpoints = action.getData().getObject("operator").getObject("targetEndpoints");

    for (String targetMC : targetEndpoints.getFieldNames()) {
      //         JsonObject mc = targetEndpoints.getObject(targetMC);
      result.add(targetMC);
      //
    }
    return result;
  }

  public Set<String> getRunningMC() {
    Set<String> result = new HashSet<>();
    JsonObject targetEndpoints = action.getData().getObject("operator").getObject("scheduling");

    for (String runningMC : targetEndpoints.getFieldNames()) {
      //         JsonObject mc = targetEndpoints.getObject(targetMC);
      result.add(runningMC);
      //
    }
    return result;
  }

  public void executeReduceLocal() {
    long start = System.currentTimeMillis();
    if (reducerLocalCallable != null) {
      DistributedExecutorService des = new DefaultExecutorService(reduceLocalInputCache);
      setReducerLocaleEnsembleHost();
      DistributedTaskBuilder builder = null;
      if (!isRecCompReduceLocal) {
        builder = des.createDistributedTaskBuilder(reducerLocalCallable);
      } else {
        builder = des.createDistributedTaskBuilder(new FlushContinuousListenerCallable("{}", ""));
      }
      builder.timeout(24, TimeUnit.HOURS);
      DistributedTask task = builder.build();
      System.out.println(
          "EXECUTE reduceLocal " + reduceLocalInputCache.getName() + " " + reducerLocalCallable.getClass().toString() +
              " ON " + currentCluster + " with " + inputCache.size() + " keys");
      profilerLog.error("EXECUTE reduceLocal " + reducerLocalCallable.getClass().toString() +
          " ON " + currentCluster + " with " + inputCache.size() + " keys");

      profilerLog.info("reduceLocal " + reducerLocalCallable.getClass().toString() +
          " Execution is done");
      List<Future<String>> res = des.submitEverywhere(task);  // TODO(ap0n) Is this wrong?
      try {
        if (res != null) {
          double perc = handleCallables(res, reducerLocalCallable);
          System.out.println("reduceLocal " + reducerLocalCallable.getClass().toString() +
              " Execution is done " + perc);
          profilerLog.info("reduceLocal " + reducerLocalCallable.getClass().toString() +
              " Execution is done " + perc);
        } else {
          System.out.println("reduceLocal " + reducerLocalCallable.getClass().toString() +
              " Execution not done");
          profilerLog.info("reduceLocal " + reducerLocalCallable.getClass().toString() +
              " Execution not done");
          failed = true;
          replyForFailExecution(action);
        }
      } catch (InterruptedException e) {
        profilerLog.error(
            "Exception in reduceLocal Execution " + "reduceLocal " + reducerCallable.getClass().toString() + "\n" + e
                .getClass().toString());
        profilerLog.error(e.getMessage());
        System.err.println(
            "Exception in reduceLocal Execution " + "reduceLocal " + reducerLocalCallable.getClass().toString() + "\n"
                + e.getClass().toString());
        System.err.println(e.getMessage());
        failed = true;
        replyForFailExecution(action);
        e.printStackTrace();
      } catch (Exception e) {
        profilerLog.error(
            "Exception in reduceLocal Execution " + "reduceLocal " + reducerLocalCallable.getClass().toString() + "\n"
                + e.getClass().toString());
        profilerLog.error(e.getMessage());
        System.err.println(
            "Exception in reduceLocal Execution " + "map " + reducerLocalCallable.getClass().toString() + "\n" + e
                .getClass().toString());
        System.err.println(e.getMessage());
        failed = true;
        replyForFailExecution(action);
        e.printStackTrace();
      }
    }
    long end = System.currentTimeMillis();
    PrintUtilities.printAndLog(log,"TIME FOR REDUCELOCAL = " + (end - start) / 1000f);
    //    replyForSuccessfulExecution(action);
  }

  public void setupReduceLocalCallable() {
    try {
      throw new Exception("Not implemented method!");
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  @Override public void finalize() {
    System.err.println("Finalize " + this.getClass().toString() + "  " + getName());
    if (this.isAlive())
      this.interrupt();
  }

  public double handleCallables(List<Future<String>> futures, LeadsBaseCallable callable)
      throws ExecutionException, InterruptedException {
    int size = futures.size();
    int threshold = 3;
    long timeout = 240;
    long minimumTime = 60000;
    long start = -1;
    long dur = 0;
    int counter = 0;
    long lastEnd = 0;
    long begin = System.currentTimeMillis();
    try {
      System.err.println("TOTAL_FUTURES: " + size + " for " + callable.getClass().toString() + "\n" + getName());
      while (futures.size() > 0) {
        Iterator<Future<String>> resultIterator = futures.iterator();
        counter = 0;
        while (resultIterator.hasNext()) {
          Future<String> future = resultIterator.next();
          try {
            //          System.err.println("Checking if done "+ counter++ +":" + future.toString() + " for " + callable.getClass().toString() +"in op " + getName() );
            profilerLog.error(
                "Checking if done " + counter++ + ":" + future.toString() + " for " + callable.getClass().toString()
                    + "in op " + getName());
            String result = null;
            if (counter > 0) {
              result = future.get(timeout, TimeUnit.SECONDS);
            } else {
              result = future.get(2 * timeout, TimeUnit.SECONDS);
            }
            System.err.println(callable.getClass().toString() + " Completed on " + result + " " + getName());
            profilerLog.error(callable.getClass().toString() + " Completed on " + result + " " + getName());
            resultIterator.remove();
            lastEnd = System.currentTimeMillis();
          } catch (TimeoutException e) {
            //            System.err.println("Future is not done yet " + ((start < 0) ? "next cancel in inf " : (" next cancel at "+ Long.toString(
            //                (long) (1.3 * Math.max(minimumTime,lastEnd-begin)))) ) );
            if (start < 0) {
              if (counter == 0) {
                profilerLog.error("Future is not done yet " + "next cancel in inf ");
              }
            } else {
              profilerLog
                  .error(" next cancel at " + Long.toString((long) (1.3 * Math.max(minimumTime, lastEnd - begin))));
              System.err
                  .println(" next cancel at " + Long.toString((long) (1.3 * Math.max(minimumTime, lastEnd - begin))));
            }
          } catch (SuspectException se) {
            System.err.println("Cancelling possible SupsectException");
            profilerLog.error("Cancelling possible SupsectException");
            future.cancel(true);
            resultIterator.remove();
          }
        }
        if (futures.size() < threshold && start < 0) {
          start = System.currentTimeMillis();
        } else if (futures.size() < threshold) {
          dur = System.currentTimeMillis() - begin;
          if (dur > 1.3 * Math.max(minimumTime, lastEnd - begin))
            ;
          {
            try {
              for (Future f : futures) {
                f.cancel(true);
                System.err.println(callable.getClass().toString() + " Cancelled on  " + getName());
                profilerLog.error(callable.getClass().toString() + " Cancelled on " + getName());
              }
              futures.clear();
            } catch (Exception e) {
              System.err.println(
                  "Exception while cancelling " + callable.getClass().toString() + " Cancelled on  " + getName());
              profilerLog
                  .error("Exception while cancelling " + callable.getClass().toString() + " Completed on " + getName());
            }
          }
        }
      }
    } catch (Exception e) {
      profilerLog
          .error("EXCEPTION WHILE Handling Callables for " + this.getClass().toString() + " \nthread: " + getName());
      PrintUtilities.logStackTrace(profilerLog, e.getStackTrace());
      e.printStackTrace();
      throw e;
    }
    int endSize = futures.size();
    return (double) (size - endSize) / (double) size;
  }


  @Override public JsonObject getContinuousMap() {
    JsonObject result = new JsonObject();
    result.putString("listener", BasicContinuousOperatorListener.class.getCanonicalName().toString());
    result.putString("operatorClass", getContinuousListenerClass());
    JsonObject listenerConf = new JsonObject();
    listenerConf.putObject("operator", new JsonObject());
    listenerConf.getObject("operator").putObject("configuration", conf);
    listenerConf.getObject("operator").putString("isMap", "true");
    listenerConf.putString("operatorClass", getContinuousListenerClass());
    result.putObject("conf", listenerConf);
    return result;
  }

  @Override public JsonObject getContinuousReduceLocal() {
    JsonObject result = new JsonObject();
    result.putString("listener", BasicContinuousOperatorListener.class.getCanonicalName().toString());
    result.putString("operatorClass", getContinuousListenerClass());
    JsonObject listenerConf = new JsonObject();
    listenerConf.putObject("operator", new JsonObject());
    listenerConf.getObject("operator").putObject("configuration", conf);
    listenerConf.getObject("operator").putString("isReduce", "true");
    listenerConf.getObject("operator").putString("isLocal", "true");
    listenerConf.putString("operatorClass", getContinuousListenerClass());
    result.putObject("conf", listenerConf);
    return result;
  }

  @Override public JsonObject getContinuousReduce() {
    JsonObject result = new JsonObject();
    result.putString("listener", BasicContinuousOperatorListener.class.getCanonicalName().toString());
    result.putString("operatorClass", getContinuousListenerClass());
    JsonObject listenerConf = new JsonObject();
    listenerConf.putObject("operator", new JsonObject());
    listenerConf.getObject("operator").putObject("configuration", conf);
    listenerConf.getObject("operator").putString("isReduce", "true");
    listenerConf.putString("operatorClass", getContinuousListenerClass());
    result.putObject("conf", listenerConf);
    return result;
  }
}
