package eu.leads.processor.core;

import eu.leads.processor.common.utils.PrintUtilities;
import eu.leads.processor.web.WebServiceClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vertx.java.core.json.JsonObject;

import java.io.IOException;

/**
 * Created by vagvaz on 10/1/15.
 */
public class WebUtils {
  static Logger log = LoggerFactory.getLogger(WebUtils.class);

  public static void stopCache(String cache, JsonObject global) {
    JsonObject microClouds = global.getObject("microclouds");
    for (String mc : microClouds.getFieldNames()) {
      String host = getHostFromGlobal(mc, global);
      String port = getPortFromGlobal(mc, global);
      try {
        WebServiceClient.stopCache(host, port, cache);
      } catch (IOException e) {
        e.printStackTrace();
        PrintUtilities.logStackTrace(log, e.getStackTrace());
      }
    }
  }

  private static String getPortFromGlobal(String mc, JsonObject globalConfig) {
    String uri = globalConfig.getObject("microclouds").getArray(mc).get(0);
    int port = -1;
    try {
      String portString = uri.substring(uri.lastIndexOf(":") + 1);
      port = Integer.parseInt(portString);
    } catch (Exception e) {
      port = 8080;
    }

    return Integer.toString(port);
  }

  private static String getHostFromGlobal(String mc, JsonObject globalConfig) {
    String uri = globalConfig.getObject("microclouds").getArray(mc).get(0);


    if (!uri.startsWith("http:")) {
      uri = "http://" + uri;
    }
    try {
      int index = uri.lastIndexOf(":");
      String portString = uri.substring(index + 1);
      int port = Integer.parseInt(portString);
      uri.substring(0, index);
    } catch (Exception e) {
      if (uri.endsWith(":")) {
        uri = uri.substring(0, uri.length() - 1);// + "8080";
      }
    }
    return uri;
  }

  private static String getURIFromGlobal(String coordinator, JsonObject globalConfig) {
    System.err.println(
        "IN NQE getting URI from global for " + coordinator + " while " + globalConfig.getObject("microclouds`"));
    String uri = globalConfig.getObject("microclouds").getArray(coordinator).get(0);


    if (!uri.startsWith("http:")) {
      uri = "http://" + uri;
    }
    try {
      String portString = uri.substring(uri.lastIndexOf(":") + 1);
      int port = Integer.parseInt(portString);
    } catch (Exception e) {
      log.error("Parsing port execption " + e.getMessage());
      System.err.println("Parsing port execption " + e.getMessage());
      PrintUtilities.logStackTrace(log, e.getStackTrace());
      if (uri.endsWith(":")) {
        uri = uri + "8080";
      } else {
        uri = uri + ":8080";
      }

    }
    return uri;
  }

  public static void addListener(JsonObject listenerConfig, JsonObject global) {
    JsonObject microClouds = global.getObject("microclouds");
    for (String mc : microClouds.getFieldNames()) {
      String host = getHostFromGlobal(mc, global);
      String port = getPortFromGlobal(mc, global);
      try {
        WebServiceClient
            .addListener(host, port, listenerConfig.getString("cache"), listenerConfig.getString("listener"),
                listenerConfig);
      } catch (IOException e) {
        e.printStackTrace();
        PrintUtilities.logStackTrace(log, e.getStackTrace());
      }
    }
  }

  public static void addListener(String cache, String listener, JsonObject listenerConfig, JsonObject global) {
    JsonObject microClouds = global.getObject("microclouds");
    for (String mc : microClouds.getFieldNames()) {
      String host = getHostFromGlobal(mc, global);
      String port = getPortFromGlobal(mc, global);
      try {
        WebServiceClient.addListener(host, port, cache, listener, listenerConfig);
      } catch (IOException e) {
        e.printStackTrace();
        PrintUtilities.logStackTrace(log, e.getStackTrace());
      }
    }
  }

  public static void stopListener(String cache, String listener, JsonObject global) {
    JsonObject microClouds = global.getObject("microclouds");
    for (String mc : microClouds.getFieldNames()) {
      String host = getHostFromGlobal(mc, global);
      String port = getPortFromGlobal(mc, global);
      try {
        WebServiceClient.removeListener(host, port, cache, listener);
      } catch (IOException e) {
        e.printStackTrace();
        PrintUtilities.logStackTrace(log, e.getStackTrace());
      }
    }
  }

  public void createCache(String mc, String cacheName, JsonObject global) {

    String host = getHostFromGlobal(mc, global);
    String port = getPortFromGlobal(mc, global);
    try {
      WebServiceClient.putObject(host, port, cacheName, "", new JsonObject());
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  public void createCache(String mc, String cacheName, String listenerName, JsonObject global) {

    String host = getHostFromGlobal(mc, global);
    String port = getPortFromGlobal(mc, global);

    try {
      WebServiceClient.putObject(host, port, cacheName, "", new JsonObject().putString("listener", listenerName));
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  public void createCacheEverywhere(String cacheName, JsonObject global) {

    JsonObject microClouds = global.getObject("microclouds");
    for (String mc : microClouds.getFieldNames()) {
      String host = getHostFromGlobal(mc, global);
      String port = getPortFromGlobal(mc, global);
      try {
        WebServiceClient.putObject(host, port, cacheName, "", new JsonObject());
      } catch (IOException e) {
        e.printStackTrace();
      }
    }
  }

  public void createCacheEverywhere(String cacheName, String listenerName, JsonObject global) {
    JsonObject microClouds = global.getObject("microclouds");
    for (String mc : microClouds.getFieldNames()) {
      String host = getHostFromGlobal(mc, global);
      String port = getPortFromGlobal(mc, global);

      try {
        WebServiceClient.putObject(host, port, cacheName, "", new JsonObject().putString("listener", listenerName));
      } catch (IOException e) {
        e.printStackTrace();
      }
    }
  }

}
