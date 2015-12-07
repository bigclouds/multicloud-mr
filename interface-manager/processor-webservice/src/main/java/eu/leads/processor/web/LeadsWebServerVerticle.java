package eu.leads.processor.web;

import eu.leads.processor.conf.LQPConfiguration;
import eu.leads.processor.core.Action;
import eu.leads.processor.core.comp.LeadsMessageHandler;
import eu.leads.processor.core.net.DefaultNode;
import eu.leads.processor.core.net.Node;
import eu.leads.processor.imanager.IManagerConstants;
import org.vertx.java.core.Handler;
import org.vertx.java.core.eventbus.EventBus;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.http.HttpServerRequest;
import org.vertx.java.core.http.RouteMatcher;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.core.logging.Logger;
import org.vertx.java.platform.Verticle;
/**
 * Created by vagvaz on 8/3/14.
 */
public class LeadsWebServerVerticle extends Verticle implements LeadsMessageHandler {

  Handler<HttpServerRequest> failHandler;
  Node com;
  Logger log;
  String id;
  static int shutdown_messages =0;
  JsonObject globalConfig;

  public void start() {

    LQPConfiguration.initialize(true);
    id = "webservice-" + LQPConfiguration.getInstance().getHostname() + container.config()
        .getNumber("port",
            8080)
        .toString();

    container.logger().info("Leads Processor REST service is starting..");
    com = new DefaultNode();

    log = container.logger();



    com.initialize(id, "webservice", null, this, null, getVertx());
    JsonObject config = container.config();
    RouteMatcher matcher = new RouteMatcher();
    System.out.println("local cluster: "+ LQPConfiguration.getInstance().getMicroClusterName());
    globalConfig = config.getObject("global");
    if(globalConfig!=null)
      if(globalConfig.containsField("webserviceAddrs")){
        JsonArray webserviceAddrs = new JsonArray();
        JsonObject webserviceAddrsClusters = globalConfig.getObject("webserviceAddrs");
        for(String key:webserviceAddrsClusters.getFieldNames()){
          System.out.println("key :" + key + " value " + webserviceAddrsClusters.getArray(key).encodePrettily());
          if(!key.equals(LQPConfiguration.getInstance().getMicroClusterName()))
            webserviceAddrs.addString((String) webserviceAddrsClusters.getArray(key).get(0));
        }
        System.out.println("webserviceAddrs:"+webserviceAddrs.encodePrettily());
        com.getConfig().putArray("webserviceAddrs", webserviceAddrs);
      }
    //
    //      startEmbeddedCacheManager(configFile);
    //      startLeadsWebModule("propertiesFile");
    GetObjectHandler getObjectHandler = new GetObjectHandler(com, log);
    PutObjectHandler putObjectHandler = new PutObjectHandler(com, log);
    GetQueryStatusHandler getQueryStatusHandler = new GetQueryStatusHandler(com, log);
    GetResultsHandler getResultsHandler = new GetResultsHandler(com, log);
    Handler<HttpServerRequest> submitWorkflowHandler = new SubmitWorkflowHandler(com, log);
    Handler<HttpServerRequest> submitDataHandler = new SubmitPluginHandler(com, log);

    Handler<HttpServerRequest> deployPluginHandler  = new DeployPluginHandler(com, log);
    Handler<HttpServerRequest> undeployPluginHandler = new UndeployPluginHandler(com, log);
    Handler<HttpServerRequest> uploadDataHandler = new UploadDataHandler(com, log);
    Handler<HttpServerRequest> submitPluginHandler = new SubmitPluginHandler(com, log);

    SubmitQueryHandler submitQueryHandler = new SubmitQueryHandler(com, log);
    SubmitSpecialCallHandler submitSpecialCallHandler = new SubmitSpecialCallHandler(com, log);
    Handler<HttpServerRequest> uploadEncryptedData = new UploadEncryptedDataHandler(com,log);
    Handler<HttpServerRequest> privacyPointQueryHandler = new PrivacyPointQueryHandler(com,log);
    Handler<HttpServerRequest> executeMRHandler = new ExecuteMRHandler(com,log);
    Handler<HttpServerRequest> completedMRHandler = new CompletedMRHandler(com,log);
    Handler<HttpServerRequest> executeMapReduceJobHandler = new ExecuteMapReduceJobHandler(com,
        log);
    Handler<HttpServerRequest> stopCacheHandler = new StopCacheHandler(com,log);
    Handler<HttpServerRequest> stopCQLHandler = new StopCQLHandler(com,log);
    Handler<HttpServerRequest> removeListenerHandler = new RemoveListenerHandler(com,log);
    Handler<HttpServerRequest> addListenerHandler = new AddListenerHandler(com,log);

    //object
    failHandler = new Handler<HttpServerRequest>() {
      @Override
      public void handle(HttpServerRequest request) {
        JsonObject object = new JsonObject();
        request.response().setStatusCode(400);
        request.response().putHeader(WebStrings.CONTENT_TYPE, WebStrings.APP_JSON);
        System.out.println("Could not match " +request.uri());
        object.putString("status", "failed");
        object.putString("message", "Invalid requst path");
        request.response().end(object.toString());
      }
    };

    matcher.noMatch(failHandler);
    matcher.post("/rest/object/get/", getObjectHandler);
    matcher.post("/rest/object/put/", putObjectHandler);
    matcher.post("/rest/data/upload/",uploadDataHandler);
    matcher.post("/rest/data/upload",uploadDataHandler);
    matcher.post("/rest/data/submit/plugin",submitPluginHandler);
    matcher.post("/rest/internal/executemr",executeMRHandler);
    matcher.post("//rest/internal/executemr",executeMRHandler);
    matcher.post("//rest/internal/executemr/",executeMRHandler);
    matcher.post("/rest/internal/executemr/",executeMRHandler);
    matcher.post("/rest/internal/completedmr",completedMRHandler);
    matcher.post("//rest/internal/completedmr",completedMRHandler);
    matcher.post("//rest/internal/completedmr/",completedMRHandler);
    matcher.post("/rest/internal/completedmr/",completedMRHandler);
    matcher.post("/rest/internal/stopCache/:cache",stopCacheHandler);
    matcher.post("/rest/internal/removeListener/:cache/:listener",removeListenerHandler);
    matcher.post("/rest/internal/addListener",addListenerHandler);
    matcher.post("/rest/query/stopcql/:id",stopCQLHandler);
    matcher.post("/rest/mrjob/submit/", executeMapReduceJobHandler);
    //
    //      //query   [a-zA-Z0-9]+\-[a-zA-Z0-9]+\-[a-zA-Z0-9]+\-[a-zA-Z0-9]+\-[a-zA-Z0-9]+
    matcher.get("/rest/query/status/:id", getQueryStatusHandler);
    //      //id:[a-zA-Z0-9]+@[a-zA-Z0-9]+}/{min:[0-9]+}/{max:[0-9]+}
    matcher.get("/rest/query/results/:id/min/:min/max/:max", getResultsHandler);
    matcher.post("/rest/query/submit", submitQueryHandler);
    matcher.post("/rest/workflow/submit", submitWorkflowHandler);
    matcher.post("/rest/data/submit", submitDataHandler);
    matcher.post("/rest/query/wgs/:type", submitSpecialCallHandler);
    //

    //        matcher.post("/rest/deploy/plugin/:pluginname/:cachename", deployPluginHandler);
    //        matcher.post("/rest/deploy/plugin/:pluginname/:cachename", deployPluginHandler);
    matcher.post("/rest/undeploy/plugin/", undeployPluginHandler);
    matcher.post("/rest/deploy/plugin/", deployPluginHandler);


    matcher.get("/rest/checkOnline", new Handler<HttpServerRequest>() {
      @Override
      public void handle(HttpServerRequest httpServerRequest) {
        httpServerRequest.response().setStatusCode(200);
        httpServerRequest.response()
            .putHeader(WebStrings.CONTENT_TYPE, WebStrings.TEXT_HTML);
        httpServerRequest.response()
            .end("<html><h1>Leads Query Processor REST Service</h1> <p>Yes I am online</p></html>");

      }
    });
    matcher.get("/", new Handler<HttpServerRequest>() {
      @Override
      public void handle(HttpServerRequest httpServerRequest) {
        httpServerRequest.response().setStatusCode(200);
        httpServerRequest.response()
            .putHeader(WebStrings.CONTENT_TYPE, WebStrings.TEXT_HTML);
        httpServerRequest.response()
            .end("<html><h1>Leads Query Processor REST Service</h1></html>");

      }
    });
    matcher.post("/rest/upload/encData", uploadEncryptedData);
    matcher.post("/rest/query/encrypted/ppq",privacyPointQueryHandler);
    vertx.createHttpServer().requestHandler(matcher)
        .listen((Integer) config.getNumber("port", 8080));

    EventBus bus = vertx.eventBus();

    bus.registerHandler("leads.processor.control", new Handler<Message>() {
      @Override
      public void handle(Message message) {
        //System.err.println("  " + message.toString());

        JsonObject body = (JsonObject) message.body();
        if (body.containsField("type")) {
          if (body.getString("type").equals("action")) {
            Action action = new Action(body);
            if (!action.getLabel().equals(IManagerConstants.QUIT)) {

              System.err.println("Continue");
            } else {
              System.err.println("Exit webprocessor try: "+shutdown_messages);

              shutdown_messages++;
              vertx.setTimer(10000, new Handler<Long>() {
                @Override
                public void handle(Long aLong) {
                  System.err.println("Exit webprocessor at last");
                  System.exit(0);
                }
              });

            }
          }

        }
      }
    });
    container.logger().info("Webserver started");
  }

  @Override
  public void handle(JsonObject message) {
    System.out.println(id + " received message " + message.toString());
    log.warn(id + " received message " + message.toString());
  }
}
