import eu.leads.processor.common.utils.storage.LeadsStorage;
import eu.leads.processor.common.utils.storage.LeadsStorageFactory;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

/**
 * Created by vagvaz on 3/23/15.
 */
public class HDFSStorageTest {

  public static void main(String[] args) throws IOException {
    Properties conf = new Properties();
    conf.setProperty("hdfs.url", "hdfs://snf-618466.vm.okeanos.grnet.gr:8020");
    conf.setProperty("prefix", "/user/vagvaz/");

    LeadsStorage hdfsStorage = LeadsStorageFactory.getInitializedStorage(LeadsStorageFactory.HDFS, conf);
    String[] containers = hdfsStorage.parts("/a");
    for (String c : containers) {
      System.out.println(c);
    }

    FileInputStream fis = new FileInputStream("/home/vagvaz/example.log");
    int size = fis.available();
    byte[] bytes = new byte[size];
    fis.read(bytes);
    hdfsStorage.writeData("logs/example.log", bytes);
    System.out.println("hello world");
  }
}
