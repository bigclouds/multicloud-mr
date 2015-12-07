package boot;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * Created by vagvaz on 8/21/14.
 */
public class LeadsComponentRunner {
  static String component;
  static String configuration;
  static String group;
  static Logger logger = LoggerFactory.getLogger(LeadsComponentRunner.class.getCanonicalName());

  public static void main(String[] args) {
    if (checkArguments(args)) {
      readArguments(args);
      String command = component + " -ha -hagroup " + group + " -conf " + configuration;
      try {
        ProcessBuilder builder =
            new ProcessBuilder("vertx", "runMod", component, "-ha", "-hagroup", group, "-conf", configuration);
        Process p = builder.start();
        //            Process p = Runtime.getRuntime().exec(command);

      } catch (IOException e) {
        e.printStackTrace();
      }
      logger.info(command + " run successfully");
    } else {
      logger.error("Could Not Run vertx component.");
    }

  }

  private static void readArguments(String[] args) {
    component = args[0];
    group = args[1];
    configuration = args[2];
  }

  private static boolean checkArguments(String[] args) {
    boolean result = false;
    if (args.length == 3) {
      String component = args[0];
      if (component.contains("imanager") || component.contains("planner") ||
          component.contains("deployer") || component.contains("nqe") ||
          component.contains("webservice") || component.contains("default")) {
        if (args[2].contains("/tmp/") && args[2].endsWith(".json")) {
          result = true;
        }
      } else {
        logger.error("Wrong arguments in LeadsComponentRunner component " + args.toString());
        System.exit(-1);
      }
    }

    return result;
  }
}
