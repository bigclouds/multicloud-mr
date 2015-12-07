package eu.leads.processor.nqe.handlers;

import eu.leads.processor.common.infinispan.InfinispanManager;
import eu.leads.processor.core.Action;
import eu.leads.processor.core.ActionHandler;
import eu.leads.processor.core.comp.LogProxy;
import eu.leads.processor.core.net.Node;

import org.infinispan.Cache;

import java.util.List;

/**
 * Created by vagvaz on 8/6/14.
 */
public class MapReduceActionHandler implements ActionHandler {

  private final Node com;
  private final LogProxy log;
  private final InfinispanManager persistence;
  private final String id;
  private String textFile;
  private transient Cache<?, ?> InCache;
  private transient Cache<?, List<?>> CollectorCache;
  private transient Cache<?, ?> OutCache;

  public MapReduceActionHandler(Node com, LogProxy log, InfinispanManager persistence, String id) {
    this.com = com;
    this.log = log;
    this.persistence = persistence;
    this.id = id;
  }

  @Override
  public Action process(Action action) {
    Action result = action;
//        try {
//            JsonObject q = action.getData();
//            // read monitor q.getString("monitor");
//            if (action.getLabel().equals(NQEConstants.DEPLOY_OPERATOR)) {//SQL Query
//
//            } else if (q.containsField("mapreduce")) {//
//                String user = q.getString("user");
//                //String sql = q.getString("mapreduce");
//                String operation = q.getString("operator");
//
//                JsonObject actionResult = new JsonObject();
////             SQLQuery query = new SQLQuery(user, sql);
////             query.setId(uniqueId);
//                QueryStatus status = new QueryStatus(uniqueId, QueryState.PENDING, "");
//
//                DistributedExecutorService des = new DefaultExecutorService(InCache);
//
//                //Create Mapper
//                //Create Reducer
//                LeadsMapper<?, ?, ?, ?> Mapper;
//                LeadsCollector<?, ?> Collector=new LeadsCollector(0, CollectorCache);
//                LeadsReducer<?, ?> Reducer;
//
//                Properties configuration = new Properties();
//
//                if (operation == NQEConstants.GROUPBY_OP) {
//                    Mapper = new GroupByMapper(configuration);
//                    Reducer = new GroupByReducer(configuration);
//                } else if (operation == NQEConstants.JOIN_OP) {
//                    Mapper = new JoinMapper(configuration);
//                    Reducer = new JoinReducer(configuration);
//                } else if (operation == NQEConstants.JOIN_OP) {
//                    Mapper = new JoinMapper(configuration);
//                    Reducer = new JoinReducer(configuration);
//                } else if (operation == NQEConstants.SORT_OP) {
//                    Mapper = new SortMapper(configuration);
//                    Reducer = new SortReducer(configuration);
//
//                    // else { //custom mapreduce process
//                } else {
//                    actionResult.putString("error", operation + "  Not found");
//                    result.setResult(actionResult);
//                    return result;
//                }
//
//
//
//
//
//                JsonObject mapreduceStatus = status.asJsonObject();
////                if (!persistence.put(StringConstants.QUERIESCACHE, uniqueId, query.asJsonObject())) {
////                    actionResult.putString("error", "");
////                    actionResult.putString("message", "Failed to add query " + sql + " from user " + user + " to the queries cache");
////
////                }
////                actionResult.putObject("status", query.getQueryStatus().asJsonObject());
//
//                //send msg to monitor operator completed
//
//                result.setResult(actionResult);
//                result.setStatus(ActionStatus.COMPLETED.toString());
//            }
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
    return result;
  }


}


