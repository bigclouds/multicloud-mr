package eu.leads.processor.infinispan.continuous;

import eu.leads.processor.common.utils.storage.LeadsStorage;
import eu.leads.processor.common.utils.storage.LeadsStorageFactory;
import eu.leads.processor.infinispan.LeadsBaseCallable;
import eu.leads.processor.infinispan.LeadsMapper;
import eu.leads.processor.infinispan.LeadsReducer;
import eu.leads.processor.infinispan.MapReduceJob;
import org.vertx.java.core.json.JsonObject;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Properties;

/**
 * Created by vagvaz on 12/5/15.
 */
public class GenericMapReduceContinuous extends MapReduceContinuousOperator {
  private LeadsStorage storage;
  private String tmpdirPrefix = "/tmp/leads/";
  //  @Override protected LeadsBaseCallable getCallableInstance(boolean isReduce, boolean islocal) {
//return null;
//  }

  @Override protected LeadsReducer getReducer() {
    LeadsReducer result = null;
    MapReduceJob job = new MapReduceJob(conf.getString("__jobString"));
    Properties storageConfiguration = new Properties();
    String storageType = this.conf.getString("__storageType");
    byte[] storageConfBytes = this.conf.getBinary("__storageConf");
    ByteArrayInputStream bais = new ByteArrayInputStream(storageConfBytes);

    try {
      storageConfiguration.load(bais);
    } catch (IOException e) {
      e.printStackTrace();
    }
    storage = LeadsStorageFactory.getInitializedStorage(storageType,storageConfiguration);
    String localJarPath = tmpdirPrefix + "/mapreduce/" + inputCache.getName() + "_" + job.getLocalReducerClass();
    storage.download("mapreduce/"+job.getName(),localJarPath);
    result = initializeReducer(job.getReducerClass(),localJarPath,job.getConfiguration());
    return result;
  }

  @Override protected LeadsReducer getLocalReducer() {
    LeadsReducer result = null;
    MapReduceJob job = new MapReduceJob(conf.getString("__jobString"));
    Properties storageConfiguration = new Properties();
    String storageType = this.conf.getString("__storageType");
    byte[] storageConfBytes = this.conf.getBinary("__storageConf");
    ByteArrayInputStream bais = new ByteArrayInputStream(storageConfBytes);

    try {
      storageConfiguration.load(bais);
    } catch (IOException e) {
      e.printStackTrace();
    }
    storage = LeadsStorageFactory.getInitializedStorage(storageType,storageConfiguration);
    String localJarPath = tmpdirPrefix + "/mapreduce/" + inputCache.getName() + "_" + job.getLocalReducerClass();
    storage.download("mapreduce/"+job.getName(),localJarPath);
    result = initializeReducer(job.getReducerClass(),localJarPath,job.getConfiguration());
    return result;
  }

  @Override protected LeadsMapper getMapper() {
    return null;
  }

  private LeadsReducer initializeReducer(String localReducerClass, String localJarPath,
      JsonObject configuration) {
    LeadsReducer result = null;
    //Get UrlClassLoader
    File file = new File(localJarPath);
    URLClassLoader classLoader = null;
    try {
      classLoader = new URLClassLoader(new URL[] {file.toURI().toURL()},GenericMapReduceContinuous.class.getClassLoader());
    } catch (MalformedURLException e) {
      e.printStackTrace();
    }
    try {
      Class<?> reducerClass =
          Class.forName(localReducerClass, true, classLoader);
      Constructor<?> con = reducerClass.getConstructor();
      result = (LeadsReducer) con.newInstance();
      result.setConfigString(configuration.toString());
      result.initialize();
    } catch (ClassNotFoundException e) {
      e.printStackTrace();
    } catch (NoSuchMethodException e) {
      e.printStackTrace();
    } catch (IllegalAccessException e) {
      e.printStackTrace();
    } catch (InstantiationException e) {
      e.printStackTrace();
    } catch (InvocationTargetException e) {
      e.printStackTrace();
    }

    return result;
  }
}
