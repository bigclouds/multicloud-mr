package eu.leads.processor.common.utils.storage;

import java.util.List;
import java.util.Properties;

/**
 * Created by vagvaz on 12/17/14.
 */
public interface LeadsStorageReader {
  boolean initializeReader(Properties configuration);
  byte[] read(String uri);
  List<byte[]> batchRead(List<String> uris);
  byte[] batcheReadMerge(List<String> uris);
  long size(String path);
  String[] parts(String uri);
  boolean exists(String path);
  long download(String source, String destination);
}
