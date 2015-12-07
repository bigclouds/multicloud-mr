package eu.leads.processor.infinispan;

import eu.leads.processor.common.utils.FSUtilities;
import eu.leads.processor.common.utils.PrintUtilities;
import eu.leads.processor.common.utils.storage.LeadsStorage;
import eu.leads.processor.common.utils.storage.LeadsStorageFactory;
import eu.leads.processor.conf.ConfigurationUtilities;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.XMLConfiguration;
import org.infinispan.Cache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vertx.java.core.json.JsonObject;

import java.io.File;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Properties;

/**
 * Created by vagvaz on 2/18/15.
 */
public class GenericMapperCallable<K, V, kOut, vOut> extends LeadsBaseCallable<K, V> {

  private Properties storageConfiguration;
  private String tmpdirPrefix = "/tmp/leads/processor/tmp/";
  private String jobConfigString;
  private boolean useCombine;
  String site;
  private String storageType;
  transient private LeadsMapper mapper;
  transient private LeadsCombiner combiner;
  transient private MapReduceJob jobConfiguration;
  transient LeadsStorage storageLayer;
  transient Logger log = null;


  public GenericMapperCallable(Cache inputCache, LeadsCollector<?, ?> collector, String microClusterName,
      JsonObject jsonObject, String storageType, Properties configuration) {
    super("{}",collector.getCacheName());
    this.site = microClusterName;
    this.collector = collector;
    this.storageType = storageType;
    this.storageConfiguration = configuration;
    jobConfigString = jsonObject.toString();
  }


//  public GenericMapperCallable(Cache inputCache, LeadsCollector<?, ?> collector, String microClusterName,
//      JsonObject jsonObject) {
//    super("{}",collector.getCacheName());
//    this.site = microClusterName;
//  }

  public LeadsMapper<K, V, kOut, vOut> getMapper() {
    Class<?> mapperClass = mapper.getClass();
    Constructor<?> constructor = null;
    try {
      constructor = mapperClass.getConstructor();
    } catch (NoSuchMethodException e) {
      e.printStackTrace();
    }
    LeadsMapper result = null;
    try {
      result = (LeadsMapper) constructor.newInstance();
    } catch (InstantiationException e) {
      e.printStackTrace();
    } catch (IllegalAccessException e) {
      e.printStackTrace();
    } catch (InvocationTargetException e) {
      e.printStackTrace();
    }
    result.setConfigString(mapper.getConfigString());

    return result;
  }

  public void setMapper(LeadsMapper<K, V, kOut, vOut> mapper) {
    this.mapper = mapper;
  }

  public String getSite() {
    return site;
  }

  public void setSite(String site) {
    this.site = site;
  }

  @Override public void initialize() {
    //Call super initialization
    super.initialize();
    jobConfiguration = new MapReduceJob(jobConfigString);
    storageLayer = LeadsStorageFactory.getInitializedStorage(storageType,storageConfiguration);
    log = LoggerFactory.getLogger("GenericCallable" + jobConfiguration.getMapperClass());

    String localJarPath = tmpdirPrefix + "/mapreduce/" + inputCache.getName() + "_" + jobConfiguration.getMapperClass() +".jar";
    storageLayer.download("mapreduce/"+jobConfiguration.getName(),localJarPath);

    //download combiner from storage layer
    //instatiate and initialize with the given configuration

    collector.setOnMap(true);
    collector.setManager(this.embeddedCacheManager);
    collector.setEmanager(emanager);
    collector.setSite(site);
    collector.initializeCache(callableIndex+":"+inputCache.getName(), imanager);

    if (useCombine) {
      System.out.println("USE COMBINER");
      combiner = initiliazeCombiner(localJarPath, jobConfiguration.getCombinerClass(), jobConfiguration.getConfiguration());
      combiner.initialize();
      collector.setCombiner((LeadsCombiner<kOut, vOut>) combiner);
      collector.setUseCombiner(true);
    } else {
      System.out.println("NO COMBINER");
      collector.setCombiner(null);
      collector.setUseCombiner(false);
    }
    mapper = initializeMapper(localJarPath, jobConfiguration.getMapperClass(), jobConfiguration.getConfiguration());
    mapper.initialize();

  }


  private LeadsCombiner initiliazeCombiner(String localCombinerPath, String combinerClassName, JsonObject combinerConfig) {
    LeadsCombiner result = null;
    //Get UrlClassLoader
    File file = new File(localCombinerPath);
    URLClassLoader classLoader = null;
    try {
      classLoader = new URLClassLoader(new URL[] {file.toURI().toURL()},GenericMapperCallable.class.getClassLoader());
    } catch (MalformedURLException e) {
      e.printStackTrace();
    }
    try {
      Class<?> combinerClass =
          Class.forName(combinerClassName, true, classLoader);
      Constructor<?> con = combinerClass.getConstructor();
      result = (LeadsCombiner) con.newInstance();
      result.setConfigString(combinerConfig.toString());
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
    //Get instance of the Class
    //Store config to tmpidir
    //initialize combiner instance with config
    //return combiner;
    return result;
  }

  private LeadsMapper initializeMapper(String localMapJarPath, String mapperClassName, JsonObject mapperConfig) {
    LeadsMapper result = null;
    //Get UrlClassLoader
    File file = new File(localMapJarPath);
    URLClassLoader classLoader = null;
    try {
      classLoader = new URLClassLoader(new URL[] {file.toURI().toURL()},GenericMapperCallable.class.getClassLoader());
    } catch (MalformedURLException e) {
      e.printStackTrace();
    }
    try {
      Class<?> mapperClass =
          Class.forName(mapperClassName, true, classLoader);
      Constructor<?> con = mapperClass.getConstructor();
      result = (LeadsMapper) con.newInstance();
      result.setConfigString(mapperConfig.toString());
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
    //Get instance of the Class
    //Store config to tmpidir
    //initialize combiner instance with config
    //return combiner;
    return result;
  }

  @Override public void executeOn(K key, V value) {
    try {
      mapper.map(key, value, collector);
    }catch (Exception e){
      e.printStackTrace();
      PrintUtilities.logStackTrace(profilerLog,e.getStackTrace());
      PrintUtilities.printAndLog(profilerLog,"Exception in MApper: " + e.getMessage());
    }
  }

  @Override public void finalizeCallable() {
    mapper.finalizeTask();
    collector.finalizeCollector();
    super.finalizeCallable();
  }

  public void setCombiner(LeadsCombiner<?, ?> combiner) {
    this.combiner = combiner;
  }

  public LeadsCombiner<?, ?> getCombiner() {
    if(combiner == null) {
      return null;
    }
    Class<?> combinerClass = combiner.getClass();
    Constructor<?> constructor = null;
    try {
      constructor = combinerClass.getConstructor();
    } catch (NoSuchMethodException e) {
      e.printStackTrace();
    }
    LeadsCombiner result = null;
    try {
      result = (LeadsCombiner) constructor.newInstance();
    } catch (InstantiationException e) {
      e.printStackTrace();
    } catch (IllegalAccessException e) {
      e.printStackTrace();
    } catch (InvocationTargetException e) {
      e.printStackTrace();
    }
    result.setConfigString(combiner.configString);

    return result;
  }

  public LeadsCombiner getCallableCombiner() {
    return combiner;
  }

  public void setUseCombine(boolean useCombine) {
    this.useCombine = useCombine;
  }
}
