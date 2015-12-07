package eu.leads.processor.math;

import eu.leads.processor.core.Tuple;
import org.vertx.java.core.json.JsonObject;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created with IntelliJ IDEA.
 * User: vagvaz
 * Date: 10/29/13
 * Time: 5:17 AM
 * To change this template use File | Settings | File Templates.
 */
//This class has the root node of the a tree produced by a where clause in a sql command
// for example let the where part of an sql query be where webpages.pagerank > 0.2
//the we create a tree with three nodes the root keeps the expression for '>' node and has two children
// the left one will read the value of the tuple for the pagerank and the right keeps the constant 0.2.
//All the job is done into the MathOperatorTreeNode

public class FilterOperatorTree {
  FilterOperatorNode root;

  public FilterOperatorTree(String treeAsDoc) {

  }

  public FilterOperatorTree(JsonObject tree) {
    JsonObject tm = new JsonObject();
    tm.putObject("tree", tree);
    org.vertx.java.core.json.JsonElement test = tm.getElement("tree");
    root = new FilterOperatorNode(tm.getElement("tree"));
  }

  public FilterOperatorTree(org.vertx.java.core.json.JsonElement qual) {
    root = new FilterOperatorNode(qual);
  }

  public FilterOperatorTree() {

  }

  public boolean accept(Tuple tuple) {
    return root.accept(tuple);
  }

  public FilterOperatorNode getRoot() {
    return root;
  }

  public JsonObject getJson() {
    JsonObject result = new JsonObject();
    result = root.toJson(result);
    return result;
  }

  public void loadFromJson(String treeAsJson) {
    JsonObject tree = new JsonObject(treeAsJson);
    root = new FilterOperatorNode();
    root.fromJson(tree);
  }

  public Map<String, List<String>> getJoinColumns() {
    Map<String, List<String>> result = new HashMap<>();
    result = root.getAllFieldsByTable(result);
    return result;
  }

  public void renameTableDatum(String tableName, String toRename) {
    root.renameTableReference(tableName, toRename);
  }
  //    public MathOperatorTree(MathOperatorTreeNode root) {
  //        this.root = root;
  //    }
  //
  //    public MathOperatorTreeNode getRoot() {
  //        return root;
  //    }
  //
  //    public void setRoot(MathOperatorTreeNode root) {
  //        this.root = root;
  //    }
  //
  //    private MathOperatorTreeNode root;
  //
  //    @JsonCreator
  //    public MathOperatorTree(@JsonProperty("root") JsonNode node) {
  //        root = new MathOperatorTreeNode(node);
  //    }
  //
  //    @Override
  //    public String toString() {
  //        String result = root.toString();
  //        return result;
  //    }
  //
  //    @JsonIgnore
  //    public boolean accept(Tuple tuple, QueryContext context) {
  //        return root.accept(tuple, context);
  //    }
}
