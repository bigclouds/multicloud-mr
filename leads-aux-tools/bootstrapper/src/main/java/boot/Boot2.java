package boot;

import com.jcraft.jsch.*;
import org.apache.commons.configuration.*;
import org.apache.commons.configuration.tree.ConfigurationNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vertx.java.core.json.DecodeException;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Created by vagvaz on 8/21/14.
 */
public class Boot2 {
  static String[] adresses;
  static JsonArray clusters =null;
  static String[] components;
  static String[] configurationFiles;
  static String filename;
  static Logger logger;
  static Map<String, HierarchicalConfiguration> componentsXml;
  static Map<String, Configuration> componentsConf;
  static Map<String, JsonObject> componentsJson;
  static Map<String, Integer> componentsInstances;

  static JsonObject webserviceJson = null;
  static JsonObject log_sinkJson = null;

  static Map<String, String> screensIPmap;
  static Map<String, JsonObject> IPsshmap;
  static Map<String, JsonObject> sshmap;


  static Configuration xmlConfiguration = null;
  static CompositeConfiguration conf = null;
  static JsonObject globalJson;
  static String baseDir = "";
  static int sleepTime = 5;

  static int pagecounter = 0;
  static HashMap<String, JsonArray> allmcAddrs = new HashMap<>();
  static HashSet<String> webservicesAddrs = new HashSet<>();
  static HashSet<String> log_sinkAddrs = new HashSet<>();


  static JsonArray jallAddrs = new JsonArray();
  static JsonArray jwebServiceAddrs = new JsonArray();
  static  JsonArray jcomponentsAddrs = new JsonArray();
  static JsonObject jwebServiceClusterAddrs = new JsonObject();
  static JsonObject jcomponetsClusterAddrs = new JsonObject();

  static String start_date_time;
  static int module_deploy_num=0;
  static String full_ensemble = "";
  private static boolean norun;

  public static void readConfiguration(String[] args) {
    org.apache.log4j.BasicConfigurator.configure();

    logger = LoggerFactory.getLogger(Boot2.class.getCanonicalName());

    componentsXml = new HashMap<>();
    componentsConf = new HashMap<>();
    componentsJson = new HashMap<>();
    componentsInstances = new HashMap<>();
    screensIPmap = new HashMap<>();

    globalJson = new JsonObject();
    conf = new CompositeConfiguration();

    if (checkArguments(args))
      filename = args[0];
    else {
      logger.error("Incorrect arguments  ");
      System.err.println("Incorrect arguments  ");
      System.exit(-1);
    }

    try {
      xmlConfiguration = new XMLConfiguration(filename);
    } catch (ConfigurationException e) {
      e.printStackTrace();
      System.err.println("Unable to load configuration, Please check the file: " + filename);
      System.exit(-1);
    }
    conf.addConfiguration(xmlConfiguration);

    baseDir = getStringValue(conf, "baseDir", null, true); //FIX IT using pwd and configuration file path!?

  }

  public static void get_hdfs_conf() {
    globalJson = checkandget(conf, "hdfs.user", globalJson, true);
    globalJson = checkandget(conf, "hdfs.prefix", globalJson, true);
    globalJson = checkandget(conf, "hdfs.uri", globalJson, true);
    globalJson = checkandget(conf, "scheduler", globalJson, true);
    if (globalJson.getString("hdfs.uri").equals("obtain")) {
      String hdfs_uri = System.getenv("LEADS_QUERY_ENGINE_HADOOP_FS");//LEADS_CURRENT_CLUSTER");
      if (hdfs_uri == null) {
        logger.warn("No HADOOP FS env variable ");

        JsonObject tmpAddrs = getJsonfile(baseDir + "hdfscloud.json", false);
        Set<String> A = tmpAddrs.getFieldNames();

        if (tmpAddrs == null) {
          //Read the env
        } else
          for (String s : A) {
            logger.info("key = " + s + " value = " + tmpAddrs.getObject(s).toString());
            JsonObject ooss = tmpAddrs.getObject(s);
            JsonObject openstacks = ooss.getObject("openstack");
            //          for(int os=0;os<openstacks.size();os++){
            //            JsonObject oos =openstacks.get(os);
            if (openstacks.getObject("leads-yarn-1") != null) {
              JsonObject yarn1 = openstacks.getObject("leads-yarn-1");
              JsonArray yarnpIps = yarn1.getArray("public_ips");
              logger.info("found  " + s + ".openstack.leads-yarn-1");
              globalJson.putString("hdfs.uri", yarnpIps.get(0).toString()); //Fix it hardcode for the moment
            }
          }
      } else {
        globalJson.putString("hdfs.uri", hdfs_uri);
      }
    }

  }

  public static void init_components_configuration() {
    List<HierarchicalConfiguration> components = ((XMLConfiguration) xmlConfiguration).configurationsAt("processor.component");
    if (components == null) { //Maybe components is jsut empty...
      logger.error("No components found exiting");
      System.exit(-1);
    }
    for (HierarchicalConfiguration c : components) {
      ConfigurationNode node = c.getRootNode();
      logger.info("Loading configuration, Name: " + c.getString("name") + " Processors " + c.getString("numberOfProcessors"));
      componentsXml.put(c.getString("name"), c);
      if (c.containsKey("configurationFile")) {

        String filePathname = baseDir + c.getString("configurationFile");

        XMLConfiguration subConf = null;
        try {
          subConf = new XMLConfiguration(filePathname);
        } catch (ConfigurationException e) {
          System.err.println("File " + filePathname + " not found, fix configuration files. \nExiting stopping boot!!!!!!!");
          System.exit(1);
        }
        componentsConf.put(c.getString("name"), subConf);
        componentsInstances.put(c.getString("name"), c.getInt("instances", 1));

        JsonObject modjson = convertConf2Json(subConf);
        modjson.putString("processors", c.getString("numberOfProcessors"));
        if (c.containsKey("modName")) {
          modjson.putString("id", c.getString("modName") + "-default-" + get_date_unique_id());
          modjson.putString("modName", c.getString("modName"));
        }
        if (c.getString("name").equals("webservice"))
          webserviceJson = modjson;
        else if (c.getString("name").equals("log-sink"))
          log_sinkJson = modjson;
        else
          componentsJson.put(c.getString("name"), modjson);

      }
    }
    components = ((XMLConfiguration) xmlConfiguration).configurationsAt("ssh.credentials");
    if (components == null) { //Maybe components is jsut empty...
      logger.error("No ssh credentials found exiting");
      System.exit(-1);
    }
    sshmap=new HashMap<>();
    for (HierarchicalConfiguration c : components) {
      ConfigurationNode node = c.getRootNode();
      JsonObject ssh = new JsonObject();
      String id = c.getString("id");
      if (id == null) {
        System.out.println("Bad formatted ssh credentials");
        continue;
      }
      ssh.putString("id", id);
      Iterator<String> ks = c.getKeys();
      while (ks.hasNext()) {
        String key = ks.next();
        ssh.putString( key, c.getString(key));
      }
      sshmap.put(id,ssh);
    }

  }

  public static void getClusterAdresses() {
    adresses = conf.getStringArray("adresses");

    JsonObject cluster;
    JsonObject nodeData;

    JsonArray clusterPublicIPs;
    JsonArray clusterPrivateIPs;

    List<HierarchicalConfiguration> cmplXadresses = ((XMLConfiguration) xmlConfiguration).configurationsAt("adresses.MC");
    if (cmplXadresses == null) { //
      logger.error("Clusters Addresses not found");
      return;
    }
    clusters = new JsonArray();
    IPsshmap = new HashMap<>();
    for (HierarchicalConfiguration c : cmplXadresses) {
      JsonArray nodesList = new JsonArray();
      ConfigurationNode node = c.getRoot();
      System.out.println("Found Cluster : " + c.getString("[@name]"));
      System.out.println("Cluster Size : " + node.getChildren().size());
      cluster = new JsonObject();
      cluster.putString("name", c.getString("[@name]"));
      cluster.putString("credentials",c.getString("[@credentials]"));
      List<HierarchicalConfiguration> nodes = c.configurationsAt("node");
      nodeData=new JsonObject();
      clusterPublicIPs  = new JsonArray();
      clusterPrivateIPs  = new JsonArray();
      for( HierarchicalConfiguration n :nodes) {
        String prIP = n.getString("[@privateIp]");
        String puIP = n.getString("");
        String name = n.getString("[@name]");
        if(puIP!=null)
        {
          clusterPublicIPs.addString(puIP);
          nodeData.putString("publicIp", puIP);
          IPsshmap.put(puIP, sshmap.get(c.getString("[@credentials]")));
        }
        if(prIP!=null){
          nodeData.putString("privateIp", prIP);
          clusterPrivateIPs.addString(prIP);
          IPsshmap.put(prIP,sshmap.get(c.getString("[@credentials]")));
        }

        clusterPrivateIPs.addString(prIP);
        nodeData.putString("name", name);

        System.out.println("puIP: " + puIP + " name: " + name + " prIP: " + prIP);
        nodesList.add(nodeData);
      }
      cluster.putArray("nodes", nodesList);
      cluster.putArray("allPublicIps", clusterPublicIPs);
      cluster.putArray("allPrivateIps",clusterPrivateIPs);
      clusters.add(cluster);

    }
  }


  public static void singleCloud(){
    System.out.println("Single cloud deployment");

    if (adresses == null) {
      logger.error("No Addrs value in the configuration file and single cloud execution, please add at least <adresses>localhost</adresses> for local execution, Exiting");
      System.exit(-1);
    }

    for (String c : adresses)
      jallAddrs.addString(c);

    jwebServiceAddrs.add(jallAddrs.get(0)); //TODO FIX is run for instances

    //predict where the components should be running

    Set<String> scheduled_components_run = new HashSet<>();
    //Add all possible compondend adresses to a set
    int ip = 0;
    for (Map.Entry<String, JsonObject> e : componentsJson.entrySet()) {
      for (int i = 0; i < componentsInstances.get(e.getKey()); i++) {
        scheduled_components_run.add(adresses[ip]);
        ip = (ip + 1) % adresses.length;
      }
    }
    //extract the unique values from the set
    for (String c : scheduled_components_run)
      jcomponentsAddrs.addString(c);

    //get env LEADS_CURRENT_CLUSTER and put the json arrays under it
    String current_cluster = System.getenv("LEADS_QUERY_ENGINE_UCLOUD_NAME");//LEADS_CURRENT_CLUSTER");
    if (current_cluster == null)
      current_cluster = "LEADS_CURRENT_CLUSTER";

    JsonObject JsonClouds = new JsonObject();
    JsonClouds.putArray(current_cluster, jallAddrs);
    globalJson.putObject("microclouds", JsonClouds);

    jwebServiceClusterAddrs.putArray(current_cluster, jwebServiceAddrs);
    jcomponetsClusterAddrs.putArray(current_cluster, jcomponentsAddrs);

    webserviceJson = set_global_addresses(webserviceJson, jwebServiceClusterAddrs, jcomponetsClusterAddrs);
    log_sinkJson = set_global_addresses(log_sinkJson, jwebServiceClusterAddrs, jcomponetsClusterAddrs);

    //Finally deploy modules !
    ip = 0;
    deployComponent("log-sink-module", log_sinkJson, adresses[ip]);
    System.out.println(" Deployed log-sink to Ip: " + adresses[ip]); //TODO FIX is run for instances

    deployComponent("processor-webservice", webserviceJson, adresses[ip]);
    System.out.println(" Deployed webservice Ip: " + adresses[ip]); //TODO FIX is run for instances
    //Deploy other modules
    for (Map.Entry<String, JsonObject> e : componentsJson.entrySet()) {
      JsonObject modJson = componentsJson.get(e.getKey());
      modJson = set_global_addresses(modJson, jwebServiceClusterAddrs, jcomponetsClusterAddrs);
      for (int i = 0; i < componentsInstances.get(e.getKey()); i++) {
        deployComponent(e.getKey(), modJson, adresses[ip]);
        System.out.println("\r\nStarted : " + e.getKey() + " At: " + adresses[ip] +".");
        ip = (ip + 1) % adresses.length;
      }
    }

  }


  public static void multiCloud(){
    //multicloud deployment
    System.out.println("Multi-cloud deployment");
    // System.exit(-1);


    //globalJson.putObject("microclouds", salt_get("leads_query-engine.map", true));

    String JsonCloudsEnv = System.getenv("LEADS_QUERY_ENGINE_CLUSTER_TOPOLOGY");//LEADS_CURRENT_CLUSTER");
    JsonObject JsonClouds=null;
    if (JsonCloudsEnv == null) {
      if(clusters!=null) {
        JsonClouds = new JsonObject();
        for(int c=0; c<clusters.size();c++){
          JsonArray tmp = new JsonArray();
          tmp.add(((JsonObject)clusters.get(c)).getArray("allPublicIps").get(0));
          JsonClouds.putArray(((JsonObject) clusters.get(c)).getString("name"), tmp);
          allmcAddrs.put(((JsonObject)clusters.get(c)).getString("name"),((JsonObject)clusters.get(c)).getArray("allPublicIps"));
        }
      }else {
        logger.info("Check for microclouds.json file.");
        JsonClouds = read_salt(getJsonfile(baseDir + "microclouds.json", false), true);
      }
    }
    else
      JsonClouds = read_salt(new JsonObject(JsonCloudsEnv), true);

    if(JsonClouds==null){
      logger.error(" Unable to retrieve any micro-cloud configuration! ");
      System.exit(-1);
    }

    globalJson.putObject("microclouds", JsonClouds);

    //execute webservice in all clouds

    //TODO
    //Verify that available nodes are accessible

    //predicted adresses of running  components
    //predict-prederermine webservice modules adresses //todo make also use of # of instances variable
    JsonArray componentsInstancesNames = new JsonArray(); //An array that keeps the order of the components executed
    for (Map.Entry<String, JsonObject> e : componentsJson.entrySet())
      for (int i = 0; i < componentsInstances.get(e.getKey()); i++)
        componentsInstancesNames.addString(e.getKey());

    JsonObject compAlladresses = new JsonObject();
    JsonObject webserviceAlladresses = new JsonObject();
    int instancesCnt;
    for (Map.Entry<String, JsonArray> entry : allmcAddrs.entrySet()) {
      instancesCnt = 0;
      JsonArray pAddrs = entry.getValue();
      JsonArray clusterComponentsAddressed = new JsonArray();
      JsonArray clusterWebServiceAddressed = new JsonArray();
      if (pAddrs.size() == 0)
        continue;


      for (int i = 0; i < pAddrs.size() && i < componentsInstances.get("webservice"); i++) {
        clusterWebServiceAddressed.addString((String) pAddrs.get(i));
        webservicesAddrs.add((String) pAddrs.get(i));
      }
      //for all available ips of current microcloud that are used save them to a set
      HashMap<String, Integer> instancesPerNode = new HashMap<>();
      for (int s = 0; /*s < pAddrs.size() &&*/ instancesCnt < componentsInstancesNames.size(); s++) {
        String address = pAddrs.get(instancesCnt % pAddrs.size());
        clusterComponentsAddressed.addString(address);
        logger.info("Cur comp " + componentsInstancesNames.get(instancesCnt) + " instances " + componentsInstances.get(componentsInstancesNames.get(instancesCnt)));
        //

        if(instancesPerNode.containsKey(address))
          instancesPerNode.put(address, instancesPerNode.get(address)+1);
        else
          instancesPerNode.put(address, 1);
        //Also write the cluster name in a text file into home dir ...
        //remoteExecute((String) pAddrs.get(s), "echo " + entry.getKey() + " > ~/micro_cloud.txt");
        instancesCnt++;
      }
      //compAlladresses.putArray(entry.getKey(), clusterComponentsAddressed);
      String ensembleString = new String();
      for (String ip:instancesPerNode.keySet()){
        int in=0;
        do{
          ensembleString+=ip+":"+Integer.toString(11222 + in) + ";";
          in++;
          if(in>=instancesPerNode.get(ip))
            break;
        }while(true);

      }
      //      for(int ip=0; ip<clusterComponentsAddressed.size();ip++){
      //        ensembleString+=clusterComponentsAddressed.get(ip)+":11222;";
      //      }
      JsonArray ensembleStringtmp = new JsonArray();
      ensembleStringtmp.add(ensembleString.substring(0, ensembleString.length() - 1));
      System.out.println("Ensemble String for " + entry.getKey() + " :" + ensembleStringtmp);
      if(full_ensemble.isEmpty())
        full_ensemble=ensembleString.substring(0, ensembleString.length() - 1);
      else
        full_ensemble=full_ensemble+"|"+ensembleString.substring(0, ensembleString.length() - 1);
      compAlladresses.putArray(entry.getKey(), ensembleStringtmp);
      webserviceAlladresses.putArray(entry.getKey(), clusterWebServiceAddressed);
      //      if (instancesCnt >= componentsInstancesNames.size()) {
      //        break;
      //      }
    }

    /////////////////
    //set also global variable adresses to
    webserviceJson = set_global_addresses(webserviceJson, webserviceAlladresses, compAlladresses);
    //    log_sinkJson = set_global_addresses(log_sinkJson, webserviceAlladresses, compAlladresses);

    for (Map.Entry<String, JsonArray> entry : allmcAddrs.entrySet()) {
      instancesCnt=0;
      JsonArray pAddrs = entry.getValue();
      if (pAddrs.size() == 0)
        continue;

      //deploy at least one log one webservice modules to each microcloud
      //      for (int i = 0; i < pAddrs.size() && i < componentsInstances.get("log-sink"); i++) {
      //        System.out.println(" Deploying log-sink to: " + entry.getKey() + " Ip: " + pAddrs.get(i) + " ");
      //        deployComponent("log-sink-module", log_sinkJson, pAddrs.get(i).toString());
      //      }
      for (int i = 0; i < pAddrs.size() && i < componentsInstances.get("webservice"); i++) {

        System.out.println(" Deploying webservice to: " + entry.getKey() + " Ip: " + pAddrs.get(i) + " ");
        deployComponent("processor-webservice", webserviceJson, pAddrs.get(i).toString());
      }
      //TODO Check if successfull deployment ...

      //deploy all other components
      for (int s = 0; /*s < pAddrs.size() &&*/ instancesCnt < componentsInstancesNames.size(); s++) {
        JsonObject modJson = componentsJson.get(componentsInstancesNames.get(instancesCnt));
        //Fixed ?
        modJson.putString("id", componentsInstancesNames.get(instancesCnt) + "-default-" +  get_date_unique_id());
        String address = pAddrs.get(instancesCnt % pAddrs.size()).toString();
        modJson = set_global_addresses(modJson, webserviceAlladresses, compAlladresses);
        deployComponent(componentsInstancesNames.get(instancesCnt).toString(), modJson, address);
        System.out.println("\r\n Started : " + componentsInstancesNames.get(instancesCnt) + " At: " + address );
        instancesCnt++;
      }
    }
  }

  public static String get_date_unique_id(){
    return start_date_time+Integer.toString(module_deploy_num++);
  }

  public static void main(String[] args) {

    start_date_time = new SimpleDateFormat("HHmmss").format(new Date());
    readConfiguration(args);
    get_hdfs_conf();
    init_components_configuration();
    sleepTime = conf.getInt("startUpDelay", 5);
    getClusterAdresses();
    String deploymentType = getStringValue(conf, "deploymentType", "singlecloud", false);

    if (deploymentType.equals("singlecloud")) {
      singleCloud();
    } else if (deploymentType.equals("multicloud")) {
      multiCloud();
      System.out.println("Ensemble String for loaders: " + full_ensemble);
    }

  }

  private static JsonObject set_global_addresses(JsonObject modJson, JsonObject jwebServiceAddrs, JsonObject jcomponentsAddrs) {
    JsonObject global = modJson.getObject("global");
    global.putObject("webserviceAddrs", jwebServiceAddrs);
    //    modJson.putArray("allAddrs", jallAddrs);
    global.putObject("componentsAddrs", jcomponentsAddrs);
    modJson.putObject("global", global);
    return modJson;
  }

  private static JsonObject getJsonfile(String filename, boolean on_error_exit) {

    try {
      String data = new String(Files.readAllBytes(Paths.get(filename)));
      return new JsonObject(data);
    } catch (IOException e) {
      System.err.print(e.getMessage());
      logger.error(e.getMessage());
      if (on_error_exit) {
        System.err.print(e.getMessage());
        logger.error(e.getMessage());
        System.exit(3);
      } else {
        logger.warn(e.getMessage());
      }
      return null;
    } catch (DecodeException e) {
      if (on_error_exit) {
        System.err.print(e.getMessage());
        logger.error("Unable to parse the json file " + filename + " " + e.getMessage());
        System.exit(13);
      } else {
        logger.warn(e.getMessage());
      }
      return null;
    }
  }

  private static String getStringValue(Configuration conf, String key, String default_value, boolean on_error_exit) {
    String ret = conf.getString(key);
    if (ret == null)
      if (on_error_exit) {
        logger.error("Required value " + key + " undefined, bootStrapper exits");
        System.exit(13);
      } else {
        logger.warn("Required value " + key + " undefined, bootStrapper, continues with value " + default_value);
        ret = default_value;
      }
    return ret;
  }


  private static JsonObject checkandget(Configuration conf, String attribute, JsonObject inputJson, boolean on_error_exit) {
    if (conf.containsKey(attribute))
      inputJson.putString(attribute, getStringValue(conf, attribute, null, on_error_exit));
    return inputJson;
  }

  //Todo check correct existance of values
  private static JsonObject convertConf2Json(XMLConfiguration subconf) {
    //System.out.println(" componentType: " + subconf.getString("componentType"));
    Iterator it = null;
    JsonObject ret = new JsonObject();
    JsonArray otherGroups = null;
    ret.putObject("global", globalJson);
    if (subconf.containsKey("componentType")) {
      String componentType = subconf.getString("componentType");
      ret.putString("id", componentType + "-default-" + get_date_unique_id());
      ret.putString("group", componentType);
      ret.putString("version", conf.getString("processor.version"));
      ret.putString("groupId", conf.getString("processor.groupId"));

      if (componentType.toLowerCase().equals("leads.log.sink")) { //Special case for the log sink module
        if (subconf.containsKey("groups")) {
          otherGroups = new JsonArray();
          for (String group : subconf.getStringArray("groups"))
            otherGroups.addString(group);
          ret.putArray("groups", otherGroups);
        }
        return ret;
      }
      ret.putString("componentType", componentType);
      List<HierarchicalConfiguration> servicenodes = subconf.configurationsAt("service");

      String type = subconf.getString("service.type");
      // System.out.println(" Type: " + type);
      List<HierarchicalConfiguration> nodes = subconf.configurationsAt("service.conf");

      // System.out.println("service  size " + servicenodes.size());

      if (subconf.containsKey("otherGroups")) {
        otherGroups = new JsonArray();
        for (String group : subconf.getStringArray("otherGroups")) {
          //System.out.println("Group: " + group);
          otherGroups.addString(group);
        }
      }

      JsonObject service = null;
      JsonArray serviceA = new JsonArray();
      if (servicenodes.size() > 0) {
        service = new JsonObject();
        service.putString("type", type);
        JsonObject conf_tmp = new JsonObject();

        if (nodes.size() > 0) {
          it = nodes.get(0).getKeys();
          while (it.hasNext()) {
            String key = (String) it.next();
            conf_tmp = checkNumberInsert(conf_tmp, key, nodes.get(0));
          }
        }
        conf_tmp.putString("id", componentType + ".$id." + type);
        conf_tmp.putString("group", "$group");
        service.putObject("conf", conf_tmp);

        serviceA = new JsonArray();
        serviceA.addObject(service);
      }
      if (otherGroups != null)
        ret.putArray("otherGroups", otherGroups);

      ret.putArray("services", serviceA);

    } else {
      it = subconf.getKeys();
      while (it.hasNext()) {
        String key = (String) it.next();
        ret = checkNumberInsert(ret, key, subconf);
      }
    }
    return ret;
  }

  public static JsonObject checkNumberInsert(JsonObject in, String key, Configuration conf) {
    if (in == null)
      in = new JsonObject();

    if (conf.containsKey(key))
      try {
        //System.out.println(" key found  " + key + " value Int " + conf.getInt(key));
        in.putNumber(key, conf.getInt(key));
      } catch (ConversionException eInt) {
        try {
          //System.out.println(" key found  " + key + " value Double " + conf.getDouble(key));
          in.putNumber(key, conf.getDouble(key));
        } catch (ConversionException eDouble) {
          try {
            //System.out.println(" key found  " + key + " value String " + conf.getString(key));
            in.putBoolean(key, conf.getBoolean(key));
          } catch (ConversionException eBoolean) {
            try {
              //System.out.println(" key found  " + key + " value String " + conf.getString(key));
              in.putString(key, conf.getString(key));
            } catch (ConversionException eString) {
              System.err.print("Cannot parse Value");
            }
          }
        }
      }
    return in;
  }

  private static void deployComponent(String component, JsonObject modJson, String ip) {
    //JsonObject modJson = componentsJson.get(component); //generateConfiguration(component);
    if (modJson == null)
      return;

    sendConfigurationTo(modJson, ip);

    String group = getStringValue(conf, "processor.groupId", null, true);
    String version = getStringValue(conf, "processor.version", null, true);
    ;
    //    String command = "vertx runMod " + group +"~"+ component + "-mod~" + version + " -conf /tmp/"+config.getString("id")+".json";
    String remotedir = getStringValue(conf, "remoteDir", null, true);
    String vertxComponent;
    if (modJson.containsField("modName"))
      vertxComponent = group + "~" + modJson.getString("modName") + "~" + version;
    else
      vertxComponent = group + "~" + component + "-comp-mod~" + version;

    String command;
    String vertx_executable;
    if(vertxComponent.contains("web"))
      vertx_executable="vertx";
    else if(vertxComponent.contains("log"))//run vertx with lower memory usage (vertxb)
      vertx_executable="vertx";
    else if(vertxComponent.contains("iman"))
      vertx_executable="vertx";
    else if(vertxComponent.contains("dep"))
      vertx_executable="vertx";
    else if(vertxComponent.contains("nqe"))
      vertx_executable="vertx";
    else if(vertxComponent.contains("plan"))
      vertx_executable="vertx";
    else
      vertx_executable="vertx";

    command = vertx_executable + " runMod " + vertxComponent + " -conf " + remotedir + "R" + modJson.getString("id") + ".json";
    if (conf.containsKey("processor.vertxArg"))
      command += " -" + conf.getString("processor.vertxArg");

    runRemotely(modJson.getString("id"), ip, command);
    for (int t = 0; t < sleepTime * 2; t++) {
      System.out.printf("\rPlease wait "+ sleepTime+ "s Deploying: "+ component + " elapsed:" + Integer.toString(1+ t * 500/1000));
      try {
        Thread.sleep(500);
      } catch (InterruptedException e1) {
        e1.printStackTrace();
      }
    }
    System.out.println("");
  }

  public static void runRemotely(String id, String ip, String command) {
    runRemotely(id, ip, command, false);
  }

  public static void runRemotely(String id, String ip, String command, boolean existingWindow) {
    String command1;
    if (existingWindow) {
      String session_name;
      if (screensIPmap.containsKey(ip)) {
        session_name = screensIPmap.get(ip);
        // run top within that bash session
        String command0 = "cd ~/.vertx_mods && screen -S " + session_name;
        // run top within that bash session
        command1 = command0 + " screen -S  " + session_name + " -p " + (pagecounter++) + " -X stuff $\'" + " bash -l &&" + command+ "\r\'";//ping 147.27.18.1";
      } else {
        session_name = "shell_" + ip;
        screensIPmap.put(ip, session_name);
        String command0 = "cd ~/.vertx_mods && screen -AmdS " + session_name + " bash -l";
        // run top within that bash session
        command1 = command0 + " && " + "screen -S  " + session_name + " -p \" + (pagecounter++) +\" -X stuff $\'" + command + "\r\'";//ping 147.27.18.1";
      }

    } else {
      String command0 = "cd ~/.vertx_mods &&  screen -AmdS shell_" + id + " bash -l";
      // run top within that bash session
      command1 = command0 + " && " + "screen -S shell_" + id + " -p 0 -X stuff $\'" + command + "\r\'";//ping 147.27.18.1";
    }

    //System.out.print("Cmd: " + command1);
    logger.info("Execution command: " + command1);
    //command1 =command;
    remoteExecute(ip, command1);

  }

  private static String remoteExecute(String ip, String command_) {
    JsonObject sshconf = IPsshmap.get(ip);
    String ret = "";
    if (norun && !command_.contains("salt")) {
      System.out.print("Just local test, no remote execution");
      return ret;
    }
    try {
      JSch jsch = new JSch();

      Session session = createSession(jsch, ip);
      Channel channel = session.openChannel("exec");

      if (command_.contains("sudo") && sshconf.containsField("password"))
        command_ = command_.replace("sudo", "sudo -S -p '' "); //check if works always

      ((ChannelExec) channel).setCommand(command_);
      OutputStream out = channel.getOutputStream();
      channel.setInputStream(null);
      ((ChannelExec) channel).setErrStream(System.err);
      //((ChannelExec) channel).setPty(true);

      InputStream in = channel.getInputStream();
      channel.connect();
      if (command_.contains("sudo") && sshconf.containsField("password")) {
        out.write((sshconf.getString("password") + "\n").getBytes());
        out.flush();
      }
      byte[] tmp = new byte[1024];
      while (true) {
        while (in.available() > 0) {
          int i = in.read(tmp, 0, 1024);
          if (i < 0) break;
          // System.out.print(new String(tmp, 0, i));
          ret += new String(tmp, 0, i);
        }

        if (channel.isClosed()) {
          logger.info("exit-status: " + channel.getExitStatus());
          break;
        }
        try {
          Thread.sleep(200);
        } catch (Exception ee) {
        }
      }
      if (!ret.isEmpty())
        System.out.println(ret);

      channel.disconnect();
      session.disconnect();

      if (channel.getExitStatus() == 0)
        logger.info("Remote execution DONE");
      else
        logger.error("Unsuccessful execution");

    } catch (Exception e) {
      logger.error("Remote execution error: " + e.getMessage());
    }
    return ret;
  }


  private static boolean sendConfigurationTo(JsonObject config, String ip) {
    String remoteDir = getStringValue(conf, "remoteDir", null, true);
    String tmpFile = remoteDir + config.getString("id") + ".json";
    try {
      RandomAccessFile file = new RandomAccessFile(tmpFile, "rw");
      file.writeBytes(config.encodePrettily().toString());
      file.close();

    } catch (FileNotFoundException e) {
      e.printStackTrace();
      logger.error("FileNotFoundException error: " + e.getMessage());
      return false;
    } catch (IOException e) {
      e.printStackTrace();
      logger.error("IOException error: " + e.getMessage());
      return false;
    }

    if (norun) {
      System.out.println("No configuration upload, just testing, file created  " + tmpFile);
      return true;
    }
    JSch jsch = new JSch();


    Session session;
    try {
      session = createSession(jsch, ip);
      ChannelSftp channel = (ChannelSftp) session.openChannel("sftp");
      channel.connect();
      File localFile = new File(tmpFile);
      //If you want you can change the directory using the following line.
      channel.cd(remoteDir);
      channel.put(new FileInputStream(localFile), "R" + localFile.getName());
      channel.disconnect();
      session.disconnect();
      logger.info("File successful uploaded: " + localFile);
      return true;

    } catch (JSchException e) {
      e.printStackTrace();
      logger.error("Ssh error: " + e.getMessage());
    } catch (FileNotFoundException e) {
      e.printStackTrace();
      logger.error("File not found : " + e.getMessage());
    } catch (SftpException e) {
      logger.error("Sftp error: " + e.getMessage());
      e.printStackTrace();
    }
    return false;

  }

  private static Session createSession(JSch jsch, String ip) throws JSchException {
    JsonObject sshconf = IPsshmap.get(ip);
    if(sshconf==null){
      System.err.println("No ssh configuration for ip: "+ ip);
      System.exit(-1);
    }
    String username = sshconf.getString("username");//getStringValue(conf, "ssh.username", null, true);
    Session session = jsch.getSession(username, ip, 22);

    if (sshconf.containsField("rsa")/*xmlConfiguration.containsKey("ssh.rsa")*/) {
      String privateKey = sshconf.getString("rsa"); //conf.getString("ssh.rsa");
      logger.info("ssh identity added: " + privateKey);
      jsch.addIdentity(privateKey);
      session = jsch.getSession(username, ip, 22);
    } else if (sshconf.containsField("password")/*xmlConfiguration.containsKey("ssh.password")*/)
      session.setPassword(sshconf.getString("password")/*conf.getString("ssh.password")*/);
    else {
      logger.error("No ssh credentials, no password either key ");
      System.out.println("No ssh credentials, no password either key ");
      System.exit(-1);
    }
    session.setConfig("StrictHostKeyChecking", "no");
    jsch.setKnownHosts("~/.ssh/known_hosts");
    session.connect();
    logger.info("Securely connected to " + ip);
    //System.out.println("Connected");
    return session;

  }


  private static boolean checkArguments(String[] args) {
    boolean result = false;
    if (args.length >= 1) {
      if (args[0].toLowerCase().endsWith(".xml"))
        result = true;
      else {
        logger.error("Argument 1 must be the boot-configuraion.xml [Xml] file!");
        System.exit(-1);
      }
      if (args.length >= 2)
        if (args[1].contains("norun"))
          norun = true;
    }
    return result;
  }

  private static JsonObject read_salt(JsonObject json, boolean addtomap) {
    HashMap<String, JsonArray> mcAddrs = new HashMap<>();

    JsonObject microclouds = new JsonObject();
    if (json != null) {
      Set<String> MicroClouds = json.getFieldNames();
      for (String mc : MicroClouds) {
        System.out.print("Found microcloud " + mc + " ");
        JsonObject stacj = json.getObject(mc);
        Set<String> open_stacks = stacj.getFieldNames();
        for (String stack : open_stacks) {
          System.out.print(" Found openstack " + stack);
          JsonObject machinesJ = stacj.getObject(stack);
          Set<String> McMachines = machinesJ.getFieldNames();
          for (String machine : McMachines) {
            JsonObject machineDesc = machinesJ.getObject(machine);
            if (!machineDesc.containsField("public_ips"))
              continue;
            System.out.print("machine " + machine + " Ip " + machineDesc.getArray("public_ips").toString());
            //to do fix
            mcAddrs.put(mc, machineDesc.getArray("public_ips"));
          }
        }
        microclouds.putArray(mc, mcAddrs.get(mc));
      }
    }
    if (addtomap)
      allmcAddrs = mcAddrs;
    //    globalJson.putObject("microclouds", newmc);
    return microclouds;
  }

  private static JsonObject salt_get(String salt_file, boolean addtomap) {
    //System.out.println(remoteExecute("localhost", "ls /tmp/boot-conf/"));
    //fix it
    HashMap<String, JsonArray> mcAddrs = new HashMap<>();
    JsonObject microclouds = new JsonObject();
    String remoteJson = remoteExecute("localhost", "sudo salt-cloud -l 'quiet' --out='json' -c " + baseDir + "salt -m " + baseDir + "salt/" + salt_file + " --query\n");
    if (remoteJson.isEmpty())
      return null;

    if (remoteJson != null) {
      JsonObject remoteJ = read_salt(new JsonObject(remoteJson), addtomap);
      Set<String> MicroClouds = remoteJ.getFieldNames();

      for (String mc : MicroClouds) {
        System.out.print("Found microcloud " + mc + " ");
        JsonObject stacj = remoteJ.getObject(mc);
        Set<String> open_stacks = stacj.getFieldNames();
        for (String stack : open_stacks) {
          System.out.print(" Found openstack " + stack);
          JsonObject machinesJ = stacj.getObject(stack);
          Set<String> McMachines = machinesJ.getFieldNames();
          for (String machine : McMachines) {
            JsonObject machineDesc = machinesJ.getObject(machine);
            if (!machineDesc.containsField("public_ips"))
              continue;
            System.out.print("machine " + machine + " Ip " + machineDesc.getArray("public_ips").toString());
            //to do fix
            mcAddrs.put(mc, machineDesc.getArray("public_ips"));
          }
        }
        microclouds.putArray(mc, mcAddrs.get(mc));
      }
    }
    if (addtomap)
      allmcAddrs = mcAddrs;
    //    globalJson.putObject("microclouds", newmc);
    return microclouds;
  }
}
