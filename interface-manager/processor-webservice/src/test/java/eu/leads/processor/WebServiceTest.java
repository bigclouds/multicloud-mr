package eu.leads.processor;

import org.vertx.java.core.json.JsonObject;

import java.io.IOException;
import java.io.RandomAccessFile;

/**
 * Created by vagvaz on 8/22/14.
 */
public class WebServiceTest {
  public static void main(String[] args) throws IOException, InterruptedException {
    JsonObject config = new JsonObject();
    config.putNumber("port", 8080);
    String configFileName = "/tmp/webservice.json";
    RandomAccessFile file = new RandomAccessFile(configFileName, "rw");
    file.writeBytes(config.toString());
    file.close();
    String cmd = "vertx runMod gr.tuc.softnet~processor-webservice~1.0-SNAPSHOT " + configFileName + "-ha ";
    Process p = Runtime.getRuntime().exec(cmd);
    p.waitFor();
  }
}
