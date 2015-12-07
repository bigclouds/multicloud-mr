package eu.leads.processor.infinispan;

import eu.leads.processor.core.DataType;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.XMLConfiguration;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Created by vagvaz on 20/06/15. This is a class representing a MapReduce Job description
 * MapReduceJob extends DataType a class that contians a json object for storing parameters the
 * JsonObject is named data MapReduceJob attributes: name ( application name wordCount etc ) id (
 * this will be set from the engine and returned to the user for polling when the result is ready
 * configuration ( configuration for the job  ) inputs ( name of the input InfinispanCaches ) output
 * ( name of the output caches ) inputMicroClouds ( which micro clouds to use for running Map and
 * possibly ReduceLocal ) outputMicroClouds (which micro clouds to use for output and running the
 * reduce phase ) hasReduceLocal
 */
public class MapReduceJob extends DataType {

  public MapReduceJob(JsonObject other) {
    super(other);
  }

  public MapReduceJob(String jsonString) {
    super(jsonString);
    if (data.containsField("operator")) {
      if (getInternalConf() == null) {
        data.getObject("operator").putObject("configuration", new JsonObject());
      }
    } else {
      createRequiredLeadsData();
    }
//    setBuiltIn(true);
  }

  public MapReduceJob() {
    super();
    createRequiredLeadsData();
//    setBuiltIn(true);
  }

  private JsonObject getInternalConf() {
    return data.getObject("operator").getObject("configuration");
  }

  private void createRequiredLeadsData() {
    data.putObject("operator", new JsonObject());
    data.getObject("operator").putObject("configuration", new JsonObject());
  }

  public String getName() {
    return data.getObject("operator").getString("name");
  }

  public void setName(String name) {
    data.getObject("operator").putString("name", name);
  }

  public String getId() {
    return data.getObject("operator").getString("id");
  }

  public void setId(String id) {
    data.getObject("operator").putString("id", id);
  }


  public JsonObject getConfiguration() {
    JsonObject result = data.getObject("operator").getObject("configuration");
    return result;
  }

  public void setConfiguration(JsonObject configuration) {
    data.getObject("operator").putObject("configuration", configuration);
  }

  public List<String> getInputs() {
    ArrayList<String> result = new ArrayList<String>();
    JsonArray array = data.getObject("operator").getArray("inputs");
    if (array != null && array.size() > 0) {
      Iterator<Object> inputIterator = array.iterator();
      while (inputIterator.hasNext()) {
        result.add((String) inputIterator.next());
      }
    }
    return result;
  }

  public void setInputs(List<String> inputs) {
    JsonArray array = new JsonArray();
    for (String input : inputs) {
      array.add(input);
    }
    data.getObject("operator").putArray("inputs", array);
  }

  public void addInput(String input) {
    JsonArray array = data.getObject("operator").getArray("inputs");
    if (array == null) {
      array = new JsonArray();
    }
    array.add(input);
    data.getObject("operator").putArray("inputs", array);
  }

  public String getOutput() {
    String result = data.getObject("operator").getString("output");
    return result;
  }

  public void setOutput(String output) {
    data.getObject("operator").putString("output", output);
  }

  public JsonObject getInputMicroCloudss() {
   JsonObject result = data.getObject("operator").getObject("scheduling");
    return result;
  }

  public void setInputMicroClouds(List<String> microClouds) {
    JsonObject array = new JsonObject();
    for (String input : microClouds) {
      JsonArray a = new JsonArray();
      a.add("");
      array.putArray(input,a);
    }
    data.getObject("operator").putObject("scheduling", array);
  }

  public void addInputMicroCloud(String microCloud) {
    JsonObject array = data.getObject("operator").getObject("scheduling");
    if (array == null) {
      array = new JsonObject();
    }
    JsonArray a = new JsonArray();
    a.add("");
    array.putArray(microCloud,a);
    data.getObject("operator").putObject("scheduling", array);
  }

  public void setInputMicroClouds(JsonObject scheduling) {
    JsonObject array = data.getObject("operator").getObject("scheduling");
    if(array == null){
      data.getObject("operator").putObject("scheduling",scheduling);
      return;
    }
    for(String field : scheduling.getFieldNames()){
      if(!array.containsField(field)){
        array.putArray(field,scheduling.getArray(field));
      }
    }

  }

  public void setOutputMicroClouds(JsonObject outputMicroClouds) {
    JsonObject array = data.getObject("operator").getObject("targetEndpoints");
    if(array == null){
      data.getObject("operator").putObject("targetEndpoints",outputMicroClouds);
      return;
    }
    for(String field : outputMicroClouds.getFieldNames()){
      if(!array.containsField(field)){
        array.putArray(field,outputMicroClouds.getArray(field));
      }
    }

  }

  public JsonObject getOutputMicroClouds() {
    JsonObject result = data.getObject("operator").getObject("targetEndpoints");
    return result;
  }

  public void setOutputMicroClouds(List<String> microClouds) {
    JsonObject array = new JsonObject();
    for (String input : microClouds) {
      JsonArray a = new JsonArray();
      a.add("");
      array.putArray(input,a);
    }
    data.getObject("operator").putObject("targetEndpoints", array);
  }

  public void addOutputMicroCloud(String microCloud) {
    JsonObject array = data.getObject("operator").getObject("targetEndpoints");
    if (array == null) {
      array = new JsonObject();
    }
    JsonArray a = new JsonArray();
    a.add("");
    array.putArray(microCloud,a);
    data.getObject("operator").putObject("targetEndpoints", array);
  }

  public boolean useCombine(){
    return data.getObject("operator").containsField("combine");
  }

  public void setUseCombine(boolean useCombine){
    if(useCombine){
      data.getObject("operator").putString("combine","1");
    } else{
      data.getObject("operator").removeField("combine");
    }

  }
  public boolean hasReduceLocal() {
    return data.getObject("operator").containsField("reduceLocal");
  }

  public void setReduceLocal(boolean runReduceLocal) {
    if (runReduceLocal) {
      data.getObject("operator").putString("reduceLocal", "1");
    } else {
      data.getObject("operator").removeField("reduceLocal");
    }
  }

  public void setPipelineReduceLocal(boolean isPipeline){
    if(isPipeline){
      data.getObject("operator").putString("recComposableLocalReduce",
          "recComposableLocalReduce");
    }else{
      data.getObject("operator").removeField("recComposableLocalReduce");
    }
  }

  public void setPipelineReduce(boolean isPipeline){
    if(isPipeline){
      data.getObject("operator").putString("recComposableReduce",
          "recComposableReduce");
    }else{
      data.getObject("operator").removeField("recComposableReduce");
    }
  }

  public boolean isReduceLocalPipeline(){
    return data.getObject("operator").containsField("recComposableLocalReduce");
  }

  public boolean isReducePipeline(){
    return data.getObject("operator").containsField("recComposableReduce");
  }
  public boolean isBuiltIn(){
    return  data.getObject("operator").containsField("builtin");
  }
  public void setBuiltIn(boolean isBuiltIn){
    if(isBuiltIn){
      data.getObject("operator").putString("builtin","builtin");
    } else{
      data.getObject("operator").removeField("builtin");
    }
  }
  public void setConfigurationFromFile(String filename){
    try {
      XMLConfiguration configuration = new XMLConfiguration(filename);
      Iterator it = configuration.getKeys();
      JsonObject conf = data.getObject("operator").getObject("configuration");
      while(it.hasNext()){
        String key = (String) it.next();
        conf.putString(key,configuration.getProperty(key).toString());
      }
    } catch (ConfigurationException e) {
      e.printStackTrace();
    }
  }

  public void setMapperClass(String mapperClass){
    data.getObject("operator").putString("mapper",mapperClass);
  }

  public String getMapperClass(){
    return data.getObject("operator").getString("mapper");
  }

  public void setReducerClass(String reducerClass){
    data.getObject("operator").putString("reducer",reducerClass);
  }

  public String getReducerClass(){
    return data.getObject("operator").getString("reducer");
  }

  public void setCombinerClass(String combinerClass){
    data.getObject("operator").putString("combiner",combinerClass);
  }

  public String getCombinerClass(){
    return data.getObject("operator").getString("combiner");
  }

  public void setLocalReducerClass(String localReducerClass){
    data.getObject("operator").putString("localReducer",localReducerClass);
  }

  public String getLocalReducerClass(){
    return data.getObject("operator").getString("localReducer");
  }

  public void setJarPath(String jarPath){
    data.getObject("operator").putString("jar",jarPath);
  }

  public String getJarPath(){
    return data.getObject("operator").getString("jar");
  }

  public void setUseLocalReduce(boolean useLocalReduce) {
    if(useLocalReduce){
      data.getObject("operator").putString("reduceLocal","true");
    } else{
      data.getObject("operator").removeField("reduceLocal");
    }
  }

  public boolean useLocalReduce(){
    return data.getObject("operator").containsField("reduceLocal");
  }



}
