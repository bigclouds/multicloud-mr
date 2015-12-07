package eu.leads.processor.common.utils.storage;

import java.util.Properties;

/**
 * Created by vagvaz on 12/17/14.
 */
public interface LeadsStorage extends LeadsStorageWriter,LeadsStorageReader {
  /**
   *
   * @param configuration the configuration required to initialize internal structures for example for HDFS
   *                      the hdfs.uri
   *                      All the Storages take as configuration a prefix parameter that is the base prefix
   *                      used in front of every path. i.e. read("apath"0 would translate to prefix/apath
   * @return true if initialized succeeded false otherwise.
   */
  boolean initialize(Properties configuration);
  Properties getConfiguration();
  void setConfiguration(Properties configuration);
  String getStorageType();

  boolean delete(String s);
}
