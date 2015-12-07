//package eu.leads.processor.system;
//
//import eu.leads.processor.conf.LQPConfiguration;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//import org.vertx.java.core.json.JsonArray;
//import org.vertx.java.core.json.JsonObject;
//
//import java.io.FileNotFoundException;
//import java.io.IOException;
//import java.io.RandomAccessFile;
//import java.util.UUID;
//
///**
// * Created by vagvaz on 8/21/14.
// */
//public class LeadsProcessorBootstrapper {
//    static String[] ips;
//    static String[] components;
//    static String[] configurationFiles;
//    static String filename;
//    static Logger logger =
//        LoggerFactory.getLogger(LeadsProcessorBootstrapper.class.getCanonicalName());
//
//    public static void main(String[] args) {
//        if (checkArguments(args)) {
//            LQPConfiguration.initialize(true);
//            filename = args[0];
//            LQPConfiguration.getInstance().loadFile(filename);
//            ips = LQPConfiguration.getInstance().getConfiguration().getStringArray("processor.ips");
//            components =
//                LQPConfiguration.getInstance().getConf().getStringArray("processor.components");
//            configurationFiles = LQPConfiguration.getInstance().getConf()
//                                     .getStringArray("processor.configurationFiles");
//        }
//
//        for (String configuration : configurationFiles) {
//            LQPConfiguration.getInstance().loadFile(configuration);
//        }
//        int ip = 0;
//        for (int component = 0; component < components.length; component++) {
//            deployComponent(components[component], ips[ip]);
//            ip = (ip + 1) % ips.length;
//        }
//    }
//
//
//    private static void deployComponent(String component, String ip) {
//        JsonObject config = generateConfiguration(component);
//        sendConfigurationTo(config, ip);
//        String prefixCommand =
//            LQPConfiguration.getInstance().getConf().getString("processor.ssh.username") + "@" + ip;
//        String group = LQPConfiguration.getInstance().getConf().getString("processor.group");
//        String version = LQPConfiguration.getInstance().getConf().getString("processor.version");
//        //      String command = "vertx runMod " + group +"~"+ component + "-mod~" + version + " -conf /tmp/"+config.getString("id")+".json";
//        String basedir = LQPConfiguration.getInstance().getConf().getString("processor.baseDir");
//        String vertxComponent = null;
//        if (!component.equals("webservice")) {
//            vertxComponent = group + "~" + component + "-comp-mod~" + version;
//        } else {
//            vertxComponent = group + "~" + "processor-webservice~" + version;
//        }
//
//        String command = " 'source ~/.bashrc; java -cp " + basedir
//                             + "/lib/component-deployer.jar eu.leads.processor.system.LeadsComponentRunner "
//                             + vertxComponent + " " + config.getString("group") + " /tmp/" + config
//                                                                                                 .getString("id")
//                             + ".json '";
//        command = "/home/vagvaz/touchafile.sh ";
//        try {
//            ProcessBuilder builder =
//                new ProcessBuilder("ssh", prefixCommand, command, vertxComponent,
//                                      config.getString("group"),
//                                      "/tmp/" + config.getString("id") + ".json");
//            Process p = builder.start();
//            //         Process p = Runtime.getRuntime().exec(prefixCommand+" "+command);
//            //         p.waitFor();
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//
//    }
//
//    private static void sendConfigurationTo(JsonObject config, String ip) {
//        RandomAccessFile file = null;
//        String tmpFile = "/tmp/" + config.getString("id") + ".json";
//        try {
//            file = new RandomAccessFile(tmpFile, "rw");
//            file.writeBytes(config.toString());
//            file.close();
//        } catch (FileNotFoundException e) {
//            e.printStackTrace();
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//        String command = "scp " + tmpFile + " " + LQPConfiguration.getInstance().getConf()
//                                                      .getString("processor.ssh.username") + "@"
//                             + ip + ":" + tmpFile;
//        try {
//            Process p = Runtime.getRuntime().exec(command);
//            p.waitFor();
//        } catch (IOException e) {
//            e.printStackTrace();
//        } catch (InterruptedException e) {
//            e.printStackTrace();
//        }
//    }
//
//    private static JsonObject generateConfiguration(String component) {
//        JsonObject result = new JsonObject();
//        result.putString("id", UUID.randomUUID().toString());
//        result.putString("group", component);
//        result.putString("componentType", component);
//        result
//            .putString("processors", LQPConfiguration.getConf().getString("processor.processors"));
//        result.putString("groupId", LQPConfiguration.getConf().getString("processor.group"));
//        result.putString("version", LQPConfiguration.getConf().getString("processor.version"));
//        try {
//            result.putArray("otherGroups", getGroupsFor(component));
//            result.putArray("services", getServicesFor(component));
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//        if (component.equals("imanager")) {
//
//        } else if (component.equals("planner")) {
//
//        } else if (component.equals("deployer")) {
//
//        } else if (component.equals("nqe")) {
//
//        } else if (component.equals("webservice")) {
//            result
//                .putNumber("port", LQPConfiguration.getConf().getInt("components.webservice.port"));
//        } else if (component.equals("default")) {
//            JsonObject logic = new JsonObject();
//            logic.putString("listen",
//                               LQPConfiguration.getConf().getString("components.default.listen"));
//            logic.putString("publish",
//                               LQPConfiguration.getConf().getString("components.default.publish"));
//            logic.putString("start",
//                               LQPConfiguration.getConf().getString("components.default.start"));
//            result.putObject("logic", logic);
//        } else {
//            logger
//                .error("Error Cannot Create configuration because unknown component " + component);
//        }
//        return result;
//    }
//
//    private static JsonArray getServicesFor(String component) throws Exception {
//        JsonArray result = new JsonArray();
//        //imanager,planner,deployer,nqe,webservice,default
//        if (component.equals("imanager")) {
//
//        } else if (component.equals("planner")) {
//            JsonObject tmp = new JsonObject();
//            tmp.putString("type", "planner-catalog-service");
//            tmp.putObject("conf", getServiceConfiguration("planner-catalog-service"));
//            result.add(tmp);
//        } else if (component.equals("deployer")) {
//            JsonObject tmp = new JsonObject();
//            tmp.putString("type", "deployer-deploy-service");
//            tmp.putObject("conf", getServiceConfiguration("deployer-deploy-service"));
//            result.add(tmp);
//            tmp.putString("type", "deployer-monitor-service");
//            tmp.putObject("conf", getServiceConfiguration("deployer-monitor-service"));
//            result.add(tmp);
//            tmp.putString("type", "deployer-recovery-service");
//            tmp.putObject("conf", getServiceConfiguration("deployer-recovery-service"));
//            result.add(tmp);
//        } else if (component.equals("nqe")) {
//
//        } else if (component.equals("webservice")) {
//
//        } else if (component.equals("default")) {
//
//        } else {
//            logger.error("Error unknown component " + component);
//            throw new Exception("Error unknown component " + component);
//        }
//        return result;
//    }
//
//    private static JsonObject getServiceConfiguration(String service) throws Exception {
//        JsonObject result = new JsonObject();
//        if (service.equals("planner-catalog-service")) {
//            result.putString("ip", LQPConfiguration.getInstance().getConf()
//                                       .getString("planner.catalog.ip"));
//            result.putNumber("port", LQPConfiguration.getInstance().getConf()
//                                         .getInt("planner.catalog.port"));
//        } else if (service.equals("deployer-deploy-service")) {
//
//        } else if (service.equals("deployer-monitor-service")) {
//
//        } else if (service.equals("deployer-recovery-service")) {
//
//        } else {
//            logger.error("Error unknown service " + service);
//            throw new Exception("Error unknown service " + service);
//        }
//        return result;
//    }
//
//    private static JsonArray getGroupsFor(String component) throws Exception {
//        JsonArray result = new JsonArray();
//        if (component.equals("imanager")) {
//            result.add("imanager.control");
//        } else if (component.equals("planner")) {
//            result.add("planner.control");
//        } else if (component.equals("deployer")) {
//            result.add("deployer.control");
//        } else if (component.equals("nqe")) {
//            result.add("nqe.control");
//        } else if (component.equals("webservice")) {
//            result.add("webservice.control");
//        } else if (component.equals("default")) {
//
//        } else {
//            logger.error("Error unknown component " + component);
//            throw new Exception("Error unknown component " + component);
//        }
//        result.add("leads.processor.control");
//        return result;
//    }
//
//    private static boolean checkArguments(String[] args) {
//        boolean result = false;
//        if (args.length == 1) {
//            if (args[0].endsWith(".xml") || args[0].endsWith(".properties")) {
//                result = true;
//            }
//        }
//        return result;
//    }
//}
