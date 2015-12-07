import eu.leads.processor.common.utils.PrintUtilities;
import eu.leads.processor.math.FilterOperatorTree;
import org.vertx.java.core.json.JsonObject;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by vagvaz on 4/18/15.
 */
public class GetAllTableFieldsTest {

  public static void main(String[] args) {
    String joinq =
        "{\n" + "          \"type\" : \"AND\",\n" + "          \"body\" : {\n" + "            \"leftExpr\" : {\n"
            + "              \"type\" : \"EQUAL\",\n" + "              \"body\" : {\n"
            + "                \"leftExpr\" : {\n" + "                  \"type\" : \"FIELD\",\n"
            + "                  \"body\" : {\n" + "                    \"column\" : {\n"
            + "                      \"name\" : \"default.webpages.url\",\n"
            + "                      \"dataType\" : {\n" + "                        \"type\" : \"TEXT\"\n"
            + "                      },\n" + "                      \"primaryKey\" : false\n"
            + "                    },\n" + "                    \"fieldId\" : -1,\n"
            + "                    \"type\" : \"FIELD\"\n" + "                  }\n" + "                },\n"
            + "                \"rightExpr\" : {\n" + "                  \"type\" : \"FIELD\",\n"
            + "                  \"body\" : {\n" + "                    \"column\" : {\n"
            + "                      \"name\" : \"default.entities.webpageurl\",\n"
            + "                      \"dataType\" : {\n" + "                        \"type\" : \"TEXT\"\n"
            + "                      },\n" + "                      \"primaryKey\" : false\n"
            + "                    },\n" + "                    \"fieldId\" : -1,\n"
            + "                    \"type\" : \"FIELD\"\n" + "                  }\n" + "                },\n"
            + "                \"returnType\" : {\n" + "                  \"type\" : \"BOOLEAN\"\n"
            + "                },\n" + "                \"type\" : \"EQUAL\"\n" + "              }\n"
            + "            },\n" + "            \"rightExpr\" : {\n" + "              \"type\" : \"EQUAL\",\n"
            + "              \"body\" : {\n" + "                \"leftExpr\" : {\n"
            + "                  \"type\" : \"FIELD\",\n" + "                  \"body\" : {\n"
            + "                    \"column\" : {\n"
            + "                      \"name\" : \"default.webpages.sentiment\",\n"
            + "                      \"dataType\" : {\n" + "                        \"type\" : \"FLOAT8\"\n"
            + "                      },\n" + "                      \"primaryKey\" : false\n"
            + "                    },\n" + "                    \"fieldId\" : -1,\n"
            + "                    \"type\" : \"FIELD\"\n" + "                  }\n" + "                },\n"
            + "                \"rightExpr\" : {\n" + "                  \"type\" : \"FIELD\",\n"
            + "                  \"body\" : {\n" + "                    \"column\" : {\n"
            + "                      \"name\" : \"default.entities.sentimentscore\",\n"
            + "                      \"dataType\" : {\n" + "                        \"type\" : \"FLOAT8\"\n"
            + "                      },\n" + "                      \"primaryKey\" : false\n"
            + "                    },\n" + "                    \"fieldId\" : -1,\n"
            + "                    \"type\" : \"FIELD\"\n" + "                  }\n" + "                },\n"
            + "                \"returnType\" : {\n" + "                  \"type\" : \"BOOLEAN\"\n"
            + "                },\n" + "                \"type\" : \"EQUAL\"\n" + "              }\n"
            + "            },\n" + "            \"returnType\" : {\n" + "              \"type\" : \"BOOLEAN\"\n"
            + "            },\n" + "            \"type\" : \"AND\"\n" + "          }\n" + "        }";
    JsonObject joinQual = new JsonObject(joinq);
    FilterOperatorTree tree = new FilterOperatorTree(joinQual);
    Map<String, List<String>> tableCols = new HashMap<>();
    tableCols = tree.getJoinColumns();
    for (Map.Entry<String, List<String>> entry : tableCols.entrySet()) {
      System.out.println(entry.getKey());
      PrintUtilities.printList(entry.getValue());
    }
  }
}
