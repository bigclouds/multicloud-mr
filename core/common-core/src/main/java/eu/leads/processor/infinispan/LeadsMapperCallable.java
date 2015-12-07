package eu.leads.processor.infinispan;

import eu.leads.processor.common.utils.PrintUtilities;
import org.infinispan.Cache;

import java.io.Serializable;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Set;

public class LeadsMapperCallable<K, V, kOut, vOut> extends LeadsBaseCallable<K, V> implements

    Serializable {

  /**
   * tr
   */
  private static final long serialVersionUID = 1242145345234214L;

//  private LeadsCollector<kOut, vOut> collector = null;

  private Set<K> keys;
  private LeadsMapper<K, V, kOut, vOut> mapper = null;
  String site;
  private LeadsCombiner<?, ?> combiner;

  public LeadsMapperCallable() {
    super();
  }

  public LeadsMapperCallable(Cache<K, V> cache, LeadsCollector<kOut, vOut> collector,
      LeadsMapper<K, V, kOut, vOut> mapper, String site) {
    super("{}", collector.getCacheName());
    this.site = site;
    this.collector = collector;
    this.mapper = mapper;
  }
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
    //    collector.initializeCache(inputCache.getCacheManager());
    super.initialize();
    collector.setOnMap(true);
    collector.setManager(this.embeddedCacheManager);
    collector.setEmanager(emanager);
    collector.setSite(site);
    collector.initializeCache(callableIndex+":"+inputCache.getName(), imanager);
    if (combiner != null) {
      System.out.println("USE COMBINER");
      combiner.initialize();
      collector.setCombiner((LeadsCombiner<kOut, vOut>) combiner);
      collector.setUseCombiner(true);
    } else {
      System.out.println("NO COMBINER");
      collector.setCombiner(null);
      collector.setUseCombiner(false);
    }
    mapper.initialize();
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
}
