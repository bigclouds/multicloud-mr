package eu.leads.processor.web;

import eu.leads.processor.common.utils.storage.LeadsStorage;
import eu.leads.processor.common.utils.storage.LeadsStorageFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Arrays;
import java.util.Properties;

/**
 * Created by vagvaz on 3/18/15.
 */
public class TestClassForName {
  static Logger log = LoggerFactory.getLogger(TestClassForName.class);
  static LeadsStorage storage = null;

  public static void main(String[] args) {
    Properties conf = new Properties();
    conf.setProperty("prefix", "/tmp/leads-testing/");
    String filePath =
        "/home/vagvaz/Projects/idea/transform-plugin/target/transform-plugin-1.0-SNAPSHOT-jar-with-dependencies.jar";
    String targetPath = " /tmp/leads-result/";
    storage = LeadsStorageFactory.getInitializedStorage(LeadsStorageFactory.LOCAL, conf);
    uploadJar(filePath, "testing/myjar");
    storage.download("testing/myjar", targetPath + "/my.jar");
    String path =
        "/tmp/leads/processor/tmp/pluginstmp/plugins/eu.leads.processor.plugins.transform.TransformPlugin.jar";
    path = targetPath + "/my.jar";
    String className = "eu.leads.processor.plugins.transform.TransformPlugin";
    if (checkPluginNameFromFile(className, path)) {
      System.out.println("Successful");
    } else {
      System.out.println("failed");
    }

  }

  public static void uploadJar(String jarPath, String prefix) {
    try {
      BufferedInputStream input = new BufferedInputStream(new FileInputStream(jarPath));
      ByteArrayOutputStream array = new ByteArrayOutputStream();
      byte[] buffer = new byte[2 * 1024 * 1024];
      byte[] toWrite = null;
      int size = input.available();
      int counter = -1;
      while (size > 0) {
        counter++;

        int readSize = input.read(buffer);
        toWrite = Arrays.copyOfRange(buffer, 0, readSize);
        if (!uploadData(toWrite, prefix + "/" + counter)) {
          return;
        }
        size = input.available();
      }
      return;
    } catch (FileNotFoundException e) {
      e.printStackTrace();
    } catch (IOException e) {
      e.printStackTrace();
    }
    return;
  }

  public static boolean uploadData(byte[] data, String target) {
    boolean result = true;
    storage.writeData(target, data);
    return result;
  }


  private static boolean checkPluginNameFromFile(String className, String fileName) {
    boolean result = true;

    File file = new File(fileName);
    ClassLoader cl = null;
    try {
      cl = new URLClassLoader(new URL[] {file.toURI().toURL()});

      Class<?> plugClass = null;

      plugClass = Class.forName(className, true, cl);
    } catch (ClassNotFoundException e) {
      log.error(e.getMessage());
      result = false;
    } catch (MalformedURLException e) {
      result = false;
      log.error(e.getMessage());
    }
    return result;
  }

}
