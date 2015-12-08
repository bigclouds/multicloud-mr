package eu.leads.processor.conf;

import eu.leads.processor.common.StringConstants;
import eu.leads.processor.common.infinispan.EnsembleCacheUtils;
import eu.leads.processor.core.TupleUtils;
import org.apache.commons.configuration.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by vagvaz on 5/25/14.
 */
public class LQPConfiguration {
  private static volatile Object mutex = new Object();
  private static final LQPConfiguration instance = new LQPConfiguration();
  private static Logger log = LoggerFactory.getLogger(LQPConfiguration.class);
  private String baseDir;
  private CompositeConfiguration configuration;
  private Map<String, Configuration> configurations;
  private static boolean initialized = false;

  /**
   * Do not instantiate LQPConfiguration.
   */
  private LQPConfiguration() {
    mutex = new Object();
    synchronized (mutex) {
      configuration = new CompositeConfiguration();
      configurations = new HashMap<String, Configuration>();
      initialized = false;
    }
  }

  /**
   * Getter for property 'conf'.
   *
   * @return Value for property 'conf'.
   */
  public static Configuration getConf() {
    return LQPConfiguration.getInstance().getConfiguration();
  }

  /**
   * Getter for property 'instance'.
   *
   * @return Value for property 'instance'.
   */
  public static LQPConfiguration getInstance() {
    return instance;
  }

  /*Default function that initializes the configuration of the current node
  * The fucntion parametrizes specific files such as the jgroups file used by infinsipan
  * This function can take as argument the path of the configuration folder and
  * whether a full or a lite (without the parametrization of files) is needed
  * One can safely use the version with no parameters.*/
  public static void initialize() {
    initialize("conf/", false);
  }

  public static void initialize(String base_dir, boolean lite) {
    synchronized (mutex) {
      if (initialized)
        return;
      initialized = true;
      ConfigurationUtilities.addToClassPath(base_dir);

      if (!base_dir.endsWith("/"))
        base_dir += "/";
      instance.setBaseDir(base_dir);
      //Get All important initialValues
      generateDefaultValues();
      resolveDyanmicParameters();
      loadSystemPropertiesFile();
      readPublicIp();
      if (!lite) {
        updateConfigurationFiles();
      }
      System.out.println("Initializing EnsembleCacheUtils");
      EnsembleCacheUtils.initialize();
      TupleUtils.initialize();
    }
  }

  private static void readPublicIp() {
    try {
      PropertiesConfiguration publicProperties =
          new PropertiesConfiguration(instance.getBaseDir() + "public_ip.properties");
      instance.getConfiguration().setProperty(StringConstants.PUBLIC_IP,
          publicProperties.getString(StringConstants.PUBLIC_IP, instance.getConfiguration().getString("node.ip")));
    } catch (ConfigurationException e) {
      System.out.println(e.getMessage());
      log.error(e.getMessage());
    }
  }

  private static void generateDefaultValues() {
    instance.getConfiguration().setProperty("node.cluster", StringConstants.DEFAULT_CLUSTER_NAME);
    instance.getConfiguration().setProperty("node.name", StringConstants.DEFAULT_NODE_NAME);
    instance.getConfiguration().setProperty("processor.InfinispanFile", StringConstants.ISPN_DEFAULT_FILE);
    instance.getConfiguration().setProperty("processor.JgroupsFile", StringConstants.JGROUPS_DEFAULT_TMPL);
    instance.getConfiguration().setProperty("processor.debug", "INFO");
    instance.getConfiguration().setProperty("processor.infinispan.mode", "cluster");
  }

  private static void resolveDyanmicParameters() {
    String hostname = ConfigurationUtilities.resolveHostname();
    instance.getConfiguration().setProperty("node.hostname", hostname);
    String ip = ConfigurationUtilities.resolveIp();
    instance.getConfiguration().setProperty("node.ip", ip);
    String broadcast = ConfigurationUtilities.resolveBroadCast(ip);
    instance.getConfiguration().setProperty("node.broadcast", broadcast);
    log.info("HOST:  " + hostname + " IP " + ip + " BRCD " + broadcast);
  }

  private static void loadSystemPropertiesFile() {
    try {
      PropertiesConfiguration config = new PropertiesConfiguration(instance.getBaseDir() + "processor.properties");
      if (config.containsKey("node.interface"))
        instance.getConfiguration()
            .setProperty("node.ip", ConfigurationUtilities.resolveIp(config.getString("node.interface")));
      instance.getConfiguration().addConfiguration(config);
      instance.getConfigurations().put(instance.getBaseDir() + "processor.properties", config);
    } catch (ConfigurationException e) {
      e.printStackTrace();
    }
  }

  private static void updateConfigurationFiles() {
    try {
      XMLConfiguration jgroups = new XMLConfiguration("conf/jgroups.tmpl");
      String ip = instance.getConfiguration().getString("node.ip");
      jgroups.setProperty("TCP[@bind_addr]", "${jgroups.tcp.address:" + ip + "}");
      //jgroups.setProperty("MPING[@bind_addr]", ip);
      String broadcast = instance.getConfiguration().getString("node.broadcast");
      //                instance.getConfiguration().getString("node.ip").substring(0, ip.lastIndexOf("."))
      //                    + ".255";
      jgroups.setProperty("BPING[@dest]", broadcast);
      jgroups.save(System.getProperty("user.dir") + "/" + instance.getBaseDir() + "jgroups-tcp.xml");
      //            jgroups.save(baseDir+"jgroups-tcp.xml");
    } catch (ConfigurationException e) {
      e.printStackTrace();
    }

  }

  public static void initialize(boolean lite) {
    initialize("conf/", lite);
  }

  /**
   * Getter for property 'configuration'.
   *
   * @return Value for property 'configuration'.
   */ //Returns the Combined configuration.
  public CompositeConfiguration getConfiguration() {
    return configuration;
  }

  /**
   * Setter for property 'configuration'.
   *
   * @param configuration Value to set for property 'configuration'.
   */
  public void setConfiguration(CompositeConfiguration configuration) {
    this.configuration = configuration;
  }

  /**
   * Getter for property 'baseDir'.
   *
   * @return Value for property 'baseDir'.
   */
  public String getBaseDir() {
    return baseDir;
  }

  /**
   * Setter for property 'baseDir'.
   *
   * @param baseDir Value to set for property 'baseDir'.
   */
  public void setBaseDir(String baseDir) {
    this.baseDir = baseDir;
  }

  /**
   * Getter for property 'configurations'.
   *
   * @return Value for property 'configurations'.
   */
  public Map<String, Configuration> getConfigurations() {
    return configurations;
  }

  /**
   * Setter for property 'configurations'.
   *
   * @param configurations Value to set for property 'configurations'.
   */
  public void setConfigurations(Map<String, Configuration> configurations) {
    this.configurations = configurations;
  }

  private void loadPropertiesConfig() {
    File[] files = ConfigurationUtilities.getConfigurationFiles("processor*.properties");
    for (File f : files) {
      try {
        PropertiesConfiguration config = new PropertiesConfiguration(f);
        log.info(f.toString() + " was found and loaded");
        configuration.addConfiguration(config);
      } catch (ConfigurationException e) {
        e.printStackTrace();
      }
    }
  }

  /**
   * Getter for property 'microClusterName'.
   *
   * @return Value for property 'microClusterName'.
   */
  public String getMicroClusterName() {
    return configuration.getString("node.cluster");
  }

  /**
   * Getter for property 'nodeName'.
   *
   * @return Value for property 'nodeName'.
   */
  public String getNodeName() {
    return configuration.getString("node.name");
  }

  /**
   * Getter for property 'hostname'.
   *
   * @return Value for property 'hostname'.
   */
  public String getHostname() {
    return configuration.getString("node.hostname");
  }

  public void loadFile(String filename) {
    Configuration tmp = null;
    File confFile = new File(filename);
    if (!confFile.exists()) {
      confFile = new File(baseDir + filename);
      if (!confFile.exists()) {
        log.error(
            "File " + filename + "Could not be loaded because it does not exist neither in the working dir nor in "
                + baseDir);
        return;
      }
    }
    if (filename.endsWith(".properties")) {

      try {
        tmp = new PropertiesConfiguration(filename);
      } catch (ConfigurationException e) {
        e.printStackTrace();
      }
    } else if (filename.endsWith(".xml")) {
      try {
        tmp = new XMLConfiguration(filename);
      } catch (ConfigurationException e) {
        e.printStackTrace();
      }

    }
    if (tmp != null) {
      instance.getConfigurations().put(filename, tmp);
      instance.getConfiguration().addConfiguration(tmp);
    }

  }

  public void storeConfiguration(String configName, String target) {
    Configuration tmp = instance.getConfigurations().get(configName);
    if (tmp != null) {
      if (configName.endsWith(".properties")) {
        PropertiesConfiguration prop = (PropertiesConfiguration) tmp;
        try {
          prop.save(target);
        } catch (ConfigurationException e) {
          e.printStackTrace();
        }
        log.info("Configuration " + configName + " stored to " + target);
      } else if (configName.endsWith(".xml")) {
        XMLConfiguration xml = (XMLConfiguration) tmp;
        try {
          xml.save(target);
        } catch (ConfigurationException e) {
          e.printStackTrace();
        }
        log.info("Configuration " + configName + " stored to " + target);
      } else {
        log.warn("Unknown configuration type");
      }
    }
  }


}
