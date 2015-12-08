package eu.leads.processor.conf;

import eu.leads.processor.common.StringConstants;
import org.apache.commons.configuration.Configuration;
import org.vertx.java.core.json.JsonObject;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.*;
import java.util.*;

/**
 * Created by vagvaz on 6/1/14.
 */
public class ConfigurationUtilities {

  //    static Logger log = LoggerFactory.getLogger(ConfigurationUtilities.class);
  public static File[] getConfigurationFiles(String directory) {
    return ConfigurationUtilities.getConfigurationFiles(directory, null);
  }

  public static File[] getConfigurationFiles(String directory, String pattern) {
    File[] result = null;
    File folder = new File(directory);
    if (pattern == null) {
      result = folder.listFiles();
    } else {
      PatternFileNameFilter filter = new PatternFileNameFilter(pattern);
      result = folder.listFiles(filter);
    }
    return result;
  }


  public static String resolveHostname() {
    String hostname = "localhost";
    try {
      hostname = InetAddress.getLocalHost().getHostName();
    } catch (UnknownHostException e) {
      e.printStackTrace();
    }
    return hostname;
  }

  public static String resolveIp() {

    String ip = "127.0.0.1";
    try {
      Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
      while (interfaces.hasMoreElements()) {
        NetworkInterface iface = interfaces.nextElement();
        // filters out 127.0.0.1 and inactive interfaces
        if (iface.isLoopback() || !iface.isUp())
          continue;

        Enumeration<InetAddress> addresses = iface.getInetAddresses();
        while (addresses.hasMoreElements()) {
          InetAddress addr = addresses.nextElement();
          ip = addr.getHostAddress();
          System.out.println(iface.getDisplayName() + " " + ip);
        }
      }
    } catch (SocketException e) {
      throw new RuntimeException(e);
    }
    return ip;
  }

  public static String resolveIp(String interfaceFilter) {


    String ip = "127.0.0.1";
    try {
      Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
      while (interfaces.hasMoreElements()) {
        NetworkInterface iface = interfaces.nextElement();
        // filters out 127.0.0.1 and inactive interfaces
        if (iface.isLoopback() || !iface.isUp())
          continue;
        if (!iface.getDisplayName().equals(interfaceFilter))
          continue;
        Enumeration<InetAddress> addresses = iface.getInetAddresses();
        while (addresses.hasMoreElements()) {
          InetAddress addr = addresses.nextElement();
          ip = addr.getHostAddress();

          System.out.println(iface.getDisplayName() + " " + ip);
        }
      }
    } catch (SocketException e) {
      throw new RuntimeException(e);
    }
    return ip;
  }

  public static String getString(Configuration conf) {
    Iterator<String> iterator = conf.getKeys();
    StringBuilder builder = new StringBuilder();
    builder.append("Config{");
    while (iterator.hasNext()) {
      String key = iterator.next();
      builder.append("\t" + key + "--> " + conf.getProperty(key).toString() + "\n");
    }
    builder.append("}");
    return builder.toString();

  }

  public static void addToClassPath(String base_dir) {
    URLClassLoader urlClassLoader = (URLClassLoader) ClassLoader.getSystemClassLoader();
    File f = new File(base_dir);
    URL u = null;
    try {
      u = f.toURI().toURL();
    } catch (MalformedURLException e) {
      e.printStackTrace();
    }
    Class urlClass = URLClassLoader.class;
    Method method = null;
    try {
      method = urlClass.getDeclaredMethod("addURL", new Class[] {URL.class});
    } catch (NoSuchMethodException e) {
      e.printStackTrace();
    }
    method.setAccessible(true);
    try {
      method.invoke(urlClassLoader, new Object[] {u});
    } catch (IllegalAccessException e) {
      e.printStackTrace();
    } catch (InvocationTargetException e) {
      e.printStackTrace();
    }
  }

  public static ClassLoader getClassLoaderFor(String filename) throws URISyntaxException {
    ClassLoader result = null;
    URL[] urls = null;
    try {
      File file = new File(filename);
      urls = new URL[] {file.toURI().toURL()};
    } catch (MalformedURLException e) {

    }
    result = new URLClassLoader(urls, ClassLoader.getSystemClassLoader());
    return result;
  }

  public static String resolveBroadCast(String ip) {
    String result = ip.substring(0, ip.lastIndexOf(".")) + ".255";
    Enumeration<NetworkInterface> interfaces = null;
    try {
      interfaces = NetworkInterface.getNetworkInterfaces();
    } catch (SocketException e) {
      e.printStackTrace();
    }
    while (interfaces.hasMoreElements()) {
      NetworkInterface networkInterface = interfaces.nextElement();
      try {
        if (networkInterface.isLoopback())
          continue;    // Don't want to broadcast to the loopback interface
      } catch (SocketException e) {
        e.printStackTrace();
      }
      for (InterfaceAddress interfaceAddress : networkInterface.getInterfaceAddresses()) {
        if (interfaceAddress.getAddress().toString().endsWith(ip)) {
          InetAddress broadcast = interfaceAddress.getBroadcast();

          if (broadcast == null)
            continue;
          else
            result = broadcast.toString().substring(1);
          // Use the address
        }
      }
    }
    return result;
  }

  public static String getPublicIPFromGlobal(String microClusterName, JsonObject globalConfig) {
    //        String result = globalConfig.getObject("componentsAddrs").getArray(microClusterName).get(0).toString();
    //        String[] ips = result.split(";");
    //        Random random = new Random();
    //        int index = random.nextInt(ips.length);
    ////        if(ips[index].contains(":")){
    ////            String[] hostAndPort = ips[index].split(":");
    ////            System.out.println("\n\nEXTERNAL IP " + hostAndPort[0]);
    ////            log.error("\n\nEXTERNAL IP " + hostAndPort[0]);
    ////            return hostAndPort[0];
    ////        }
    String result = LQPConfiguration.getInstance().getConfiguration().getString(StringConstants.PUBLIC_IP);
    System.out.println("\n\nEXTERNAL IP " + result);
    ////        log.error("EXTERNAL IP " + ips[index]);
    //            return ips[index];
    return result;
  }

  public static String getEnsembleString(JsonObject globalConfig) {
    String result = "";
    List<String> sites = new ArrayList<>();

    for (String targetMC : globalConfig.getObject("microclouds").getFieldNames()) {
      //         JsonObject mc = targetEndpoints.getObject(targetMC);
      sites.add(targetMC);
      //
    }
    Collections.sort(sites);
    for (String site : sites) {
      result += globalConfig.getObject("componentsAddrs").getArray(site).get(0).toString() + "|";//:11222|";
    }
    result = result.substring(0, result.length() - 1);
    return result;
  }
}
