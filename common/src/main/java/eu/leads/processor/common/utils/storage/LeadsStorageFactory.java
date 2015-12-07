package eu.leads.processor.common.utils.storage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Properties;

/**
 * Created by vagvaz on 2/11/15.
 */
public class LeadsStorageFactory {
  public static final String HDFS ="hdfs";
  public static final String LOCAL = "local";
  private static Logger log = LoggerFactory.getLogger(LeadsStorageFactory.class);
  public static LeadsStorage getStorage(String type){
    LeadsStorage result = null;
    if(type.equalsIgnoreCase(LeadsStorageFactory.HDFS)){
      log.info("Create Storage Layer for HDFS");
      result = new HDFSStorage();
    }
    else if(type.equalsIgnoreCase(LeadsStorageFactory.LOCAL)){
      log.info("Create Stroage Layer for Local Filesystem");
      result  = new LocalFileStorage();
    }
    else{
      //default choice is HDFS
      log.warn("Unknown type " + type);
      result = LeadsStorageFactory.getStorage(LeadsStorageFactory.HDFS);
    }
    return result;
  }

  public static LeadsStorage getInitializedStorage(String type,Properties configuration){
    LeadsStorage result = LeadsStorageFactory.getStorage(type);
    result.initialize(configuration);
    return result;
  }
}
