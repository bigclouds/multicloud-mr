import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import eu.leads.processor.web.ObjectQuery;
import eu.leads.processor.web.PutAction;
import eu.leads.processor.web.WebServiceQuery;
import eu.leads.processor.web.WebServiceWorkflow;

import java.util.ArrayList;

/**
 * Created by vagvaz on 2/4/15.
 */
public class DataPrint {
  public static void main(String[] args) throws JsonProcessingException {
    ObjectMapper mapper = new ObjectMapper();
    mapper.writerWithDefaultPrettyPrinter();
    ObjectQuery ob = new ObjectQuery();
    ob.setKey("akey");
    ob.setTable("atable");
    ob.setAttributes(new ArrayList<String>());
    String obJson = mapper.writeValueAsString(ob);
    System.out.println("GetObject\n" + obJson);
    PutAction action = new PutAction();
    action.setKey("akey");
    action.setTable("atable");
    action.setObject("{\"jsonkey\":\"jsonvalue\"}");
    obJson = mapper.writeValueAsString(action);
    System.out.println("Putobject\n" + obJson);
    WebServiceQuery ws = new WebServiceQuery();
    ws.setSql("SELECT * FROM table");
    ws.setUser("defaultUser");
    obJson = mapper.writeValueAsString(ws);
    System.out.println("SUBMIT QUERY\n" + obJson);
    WebServiceWorkflow wfs = new WebServiceWorkflow();
    wfs.setUser("defaultUser");
    wfs.setWorkflow("{workflow query}");
    obJson = mapper.writeValueAsString(wfs);
    System.out.println("SUBMIT WORKFLOW QUERY\n" + obJson);
  }
}
