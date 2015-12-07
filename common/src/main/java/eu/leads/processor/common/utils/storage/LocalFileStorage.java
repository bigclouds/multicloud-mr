package eu.leads.processor.common.utils.storage;

import eu.leads.processor.common.StringConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;

/**
 * Created by vagvaz on 2/11/15.
 */
public class LocalFileStorage implements LeadsStorage {
  private String basePath = null;
  private Properties storageConfiguration;
  private Logger log  = LoggerFactory.getLogger(LocalFileStorage.class);
  @Override public boolean initialize(Properties configuration) {
    basePath = configuration.getProperty("prefix");
    setConfiguration(configuration);
    if(basePath == null){
      basePath = StringConstants.TMPPREFIX;
    }
    if(!basePath.endsWith("/"))
      basePath += "/";
    File file = new File(basePath);
    if(file.exists() && file.isFile()){
      file.delete();
      file.mkdirs();
    }
    else{
      if(!file.exists()){
        file.mkdirs();
      }
    }
    return true;
  }

  @Override public Properties getConfiguration() {
    return storageConfiguration;
  }

  @Override public void setConfiguration(Properties configuration) {
    this.storageConfiguration  = configuration;
  }

  @Override public String getStorageType() {
    return LeadsStorageFactory.LOCAL;
  }

  @Override
  public boolean delete(String s) {
    return false;
  }

  @Override public boolean initializeReader(Properties configuration) {
    return true;
  }

  @Override public byte[] read(String uri) {
    try {
      BufferedInputStream input = new BufferedInputStream(new FileInputStream(basePath+"/"+uri));
      ByteArrayOutputStream array = new ByteArrayOutputStream();
      byte[] buffer = null;
      int size = input.available();
      while( size > 0){

        if(size > 1024*1024*20)
        {
          buffer = new byte[1024*1024*20];
        }
        else{
          buffer = new byte[size];
        }
        input.read(buffer);
        array.write(buffer);
        size = input.available();
      }
      return array.toByteArray();
    } catch (FileNotFoundException e) {
      e.printStackTrace();
    } catch (IOException e) {
      e.printStackTrace();
    }
    return  new byte[0];
  }

  @Override public List<byte[]> batchRead(List<String> uris) {
    List<byte[]> result = new ArrayList<>(uris.size());
    for(String uri : uris){
      result.add(read(uri));
    }
    return result;
  }

  @Override public byte[] batcheReadMerge(List<String> uris) {
    ByteArrayOutputStream resultStream = new ByteArrayOutputStream();
    for(String uri : uris){
      try {
        resultStream.write(read(uri));
      } catch (IOException e) {
        e.printStackTrace();
        return new byte[0];
      }
    }
    return resultStream.toByteArray();
  }

  @Override public long size(String path) {
    File file = new File(basePath  + path);

    return file.length();
  }

  @Override public String[] parts(String uri) {
    String[] results;
    File file = new File(basePath  + uri);
    if(file.isDirectory()){
        File[] parts = file.listFiles();
        results = new String[parts.length];
        for (int index = 0; index < parts.length; index++) {
//          results[index] = parts[index].getPath().toString().replaceFirst(basePath,"");
          results[index] = uri+"/"+index;//parts[index].getPath().toString().replaceFirst(basePath,"");
        }
      }
      else{
        if(file.exists()) {
          results = new String[1];
          results[0] = uri;
        }
      else{
          results = new String[0];
        }
      }
    return results;
  }

  @Override public boolean exists(String path) {
    File file = new File(basePath + path);
    return file.exists();
  }
  @Override public long download(String source, String destination) {
    long readBytes = 0;
    try {
      File file = new File(destination);
      if(!file.exists()){
        File parent = file.getParentFile();
        parent.mkdirs();
        file.createNewFile();
      }
      else{
        file.delete();
        file.createNewFile();
      }
      FileOutputStream fos = new FileOutputStream(destination);
      String[] pieces = parts(source);
      for(String piece : pieces){
        byte[] pieceBytes = read(piece);
        fos.write(pieceBytes);
        readBytes += pieceBytes.length;
      }
      fos.close();
    } catch (FileNotFoundException e) {
      e.printStackTrace();
    } catch (IOException e) {
      e.printStackTrace();
    }
    return readBytes;
  }

  @Override public boolean initializeWriter(Properties configuration) {
    return true;
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
    try {
      File file = new File(basePath+uri);
      if(file.exists()){
        log.error("Attempting to writed data to an existing file " + file.getPath());
      }
      File parent = file.getParentFile();
      if(!parent.exists())
      {
        parent.mkdirs();
      }

      FileOutputStream fos = new FileOutputStream(basePath+uri,false);

      fos.write(data);
      fos.close();
      return true;
    } catch (FileNotFoundException e) {
      e.printStackTrace();
      return false;
    } catch (IOException e) {
      e.printStackTrace();
      return false;
    }
  }

  @Override public boolean writeData(Map<String, byte[]> data) {
    boolean result = true;
    for(Map.Entry<String,byte[]> entry : data.entrySet()){
      result &= writeData(entry.getKey(), entry.getValue());
    }
    return result;
  }

  @Override public boolean writeData(String uri, List<byte[]> data) {
    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    for(byte[] datum : data){
      try {
        outputStream.write(datum);
        writeData(uri,outputStream.toByteArray());
        return true;
      } catch (IOException e) {
        e.printStackTrace();
        return false;
      }
    }
    return true;
  }
}
