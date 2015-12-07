package eu.leads.processor.common.utils.storage;

import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.Properties;

/**
 * Created by vagvaz on 12/17/14.
 */
public class InfinispanLeadsStorage implements LeadsStorage {


  @Override public boolean initializeReader(Properties configuration) {
    return false;
  }

  @Override public byte[] read(String uri) {
    return new byte[0];
  }

  @Override public List<byte[]> batchRead(List<String> uris) {
    return null;
  }

  @Override public byte[] batcheReadMerge(List<String> uris) {
    return new byte[0];
  }

  @Override public long size(String path) {
    return 0;
  }

  @Override public String[] parts(String uri) {
    return new String[0];
  }

  @Override public boolean exists(String path) {
    return false;
  }

  @Override public long download(String source, String destination) {
    return 0;
  }


  @Override public boolean initializeWriter(Properties configuration) {
    return false;
  }

  @Override public boolean write(String uri, InputStream stream) {
    return false;
  }

  @Override public boolean write(Map<String, InputStream> stream) {
    return false;
  }

  @Override public boolean write(String uri, List<InputStream> streams) {
    return false;
  }

  @Override public boolean writeData(String uri, byte[] data) {
    return false;
  }

  @Override public boolean writeData(Map<String, byte[]> data) {
    return false;
  }

  @Override public boolean writeData(String uri, List<byte[]> data) {
    return false;
  }

  @Override public boolean initialize(Properties configuration) {
    return false;
  }

  @Override public Properties getConfiguration() {
    return null;
  }

  @Override public void setConfiguration(Properties configuration) {

  }

  @Override public String getStorageType() {
    return null;
  }

  @Override
  public boolean delete(String s) {
    return false;
  }
}
