package eu.leads.processor.infinispan.continuous;

import eu.leads.processor.conf.LQPConfiguration;
import eu.leads.processor.infinispan.ComplexIntermediateKey;
import eu.leads.processor.infinispan.LeadsBaseCallable;
import eu.leads.processor.infinispan.LeadsCollector;
import org.infinispan.Cache;
import org.infinispan.ensemble.EnsembleCacheManager;
import org.infinispan.ensemble.cache.EnsembleCache;
import org.vertx.java.core.json.JsonObject;

import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;

/**
 * Created by vagvaz on 10/6/15.
 */
public abstract class BasicContinuousOperator implements LeadsContinuousOperator {
  protected LeadsCollector collector;
  protected String ensembleString;
  protected EnsembleCacheManager emanager;
  protected EnsembleCache outputCache;
  protected String output;
  protected Map inputData;
  protected boolean isReduce;
  protected boolean islocal;
  protected JsonObject conf;
  protected Iterator<Map.Entry> input;
  protected LeadsBaseCallable callable;
  private OperatorRunCallable operatorCallable;
  protected Cache inputCache;

  @Override public void initializeContinuousOperator(JsonObject conf) {
    this.conf = conf.getObject("configuration");
    isReduce = conf.containsField("isReduce");
    islocal = conf.containsField("isLocal");
    if (conf.containsField("ensembleString")) {
      ensembleString = conf.getString("ensembleString");
    } else {
      ensembleString = LQPConfiguration.getInstance().getConfiguration().getString("node.ip") + ":11222";
    }
    emanager = new EnsembleCacheManager(ensembleString);
    output = conf.getString("output");
//    output = conf.getObject("configuration").getString("output");
    outputCache = emanager.getCache(output);
    inputData = new HashMap();
    operatorCallable = new OperatorRunCallable(this);
    callable = initializeCallableInstance(isReduce,
        islocal);//initialize Callable for the operator parameter is whether to create map or reduce callable
    //    callable.initialize();
  }

  private LeadsBaseCallable initializeCallableInstance(boolean isReduce, boolean islocal) {
    LeadsBaseCallable result = getCallableInstance(isReduce, islocal);
    result.setEnsembleHost(ensembleString);
    result.setConfigString(conf.toString());
    result.setOutput(conf.getString("output"));
    collector =
        new LeadsCollector(LQPConfiguration.getInstance().getConfiguration().getInt("node.combiner.buffersize", 1000),
            output);
    result.setCollector(collector);
    result.setCallableIndex(-2);
    result.setEnvironment(inputCache, null);
    return result;
  }

  protected abstract LeadsBaseCallable getCallableInstance(boolean isReduce, boolean islocal);

  @Override public void executeOn(Object key, Object value, LeadsCollector collector) {
    callable.executeOn(key, value);
  }

  @Override public void onRemove(Object key, Object value) {

  }

  @Override public void setInput(Iterator<Map.Entry> iterator) {
    populateInput(iterator);
  }

  @Override public void setInputCache(Cache cache) {
    this.inputCache = cache;
  }

  private void populateInput(Iterator<Map.Entry> iterator) {
    try {
//      System.out.println("populate input ");
      while (iterator.hasNext()) {
        Map.Entry entry = iterator.next();
        if (isReduce) {
          if (entry.getKey() instanceof ComplexIntermediateKey) {
            ComplexIntermediateKey key = (ComplexIntermediateKey) entry.getKey();
            List values = (List) inputData.get(key.getKey());
            if (values == null) {
              values = new ArrayList();
              inputData.put(key.getKey(), values);
            }
            values.add(entry.getValue());
          } else {
            List values = (List) inputData.get(entry.getKey());
            if (values == null) {
              values = new ArrayList();
              inputData.put(entry.getKey(), values);
            }
            values.add(entry.getValue());
          }
        } else {
          inputData.put(entry.getKey(), entry.getValue());
        }
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  @Override public Future processInput() {
    FutureTask task = new FutureTask(operatorCallable);
    Thread t = new Thread(task);
    t.start();
    return task;
  }

  @Override public Future processInput(List<Map.Entry> input) {
    populateInput(input.iterator());
    return processInput();
  }

  @Override public void finalizeOperator() {
    Future future = processInput();
    try {
      callable.finalizeCallable();
      future.get();
      emanager.stop();
    } catch (InterruptedException e) {
      e.printStackTrace();
    } catch (ExecutionException e) {
      e.printStackTrace();
    } catch (Exception e) {
      e.printStackTrace();
    }

  }

  public LeadsBaseCallable getCallable() {
    return callable;
  }

  public Map getInputData() {
    return inputData;
  }
}
