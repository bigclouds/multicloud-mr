package eu.leads.processor.common.utils.storage;

import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.Properties;


/**
 * Created by vagvaz on 12/17/14.
 */
public interface LeadsStorageWriter {
  boolean initializeWriter(Properties configuration);
  boolean write(String uri, InputStream stream);
  boolean write(Map<String,InputStream> stream);
  boolean write(String uri,List<InputStream> streams);
  boolean writeData(String uri, byte[] data);
  boolean writeData(Map<String,byte[]> data);
  boolean writeData(String uri, List<byte[]> data);

}
