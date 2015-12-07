package eu.leads.processor.nqe.handlers;


import eu.leads.processor.common.LeadsListener;
import eu.leads.processor.common.continuous.BasicContinuousListener;
import eu.leads.processor.common.infinispan.InfinispanManager;
import eu.leads.processor.core.Action;
import eu.leads.processor.core.ActionHandler;
import eu.leads.processor.core.comp.LogProxy;
import eu.leads.processor.core.net.Node;
import eu.leads.processor.infinispan.continuous.BasicContinuousOperatorListener;
import eu.leads.processor.plugins.PluginInterface;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vertx.java.core.json.JsonObject;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

/**
 * Created by vagvaz on 9/25/15.
 */
public class AddListenerActionHandler implements ActionHandler {
  private Logger log;
  private InfinispanManager imanager;
  JsonObject global;
  public AddListenerActionHandler(Node com, LogProxy logg, InfinispanManager persistence, String id,JsonObject global) {
    log = LoggerFactory.getLogger(AddListenerActionHandler.class);
    imanager = persistence;
    this.global = global;
  }

  @Override public Action process(Action action) {
    System.err.println("Add listener action handler");
    Action result = action;
    JsonObject actionResult = new JsonObject();
    actionResult.putString("status","SUCCESS");
    actionResult.putString("message","");
    JsonObject data = action.getData();
    String cache = data.getString("cache");
    String listener = data.getString("listener");
    JsonObject conf = data.getObject("conf");
    System.err.println("ADD Listener " + listener+ " to " + cache );

      LeadsListener leadsListener = null;
    try {
      ClassLoader loader = this.getClass().getClassLoader();
      loader.loadClass(listener);
      Class<?> listenerClass = Class.forName(listener, true, loader);

      Constructor<?> con = listenerClass.getConstructor();
      log.error("get Listener new Instance");
      System.err.println("get Listener new Instance");

      leadsListener = (LeadsListener) con.newInstance();
    }catch (ClassNotFoundException ce) {
      System.err.println("Overriding hack for " +listener );
      if(listener.equals("eu.leads.processor.infinispan.continuous.BasicContinuousOperatorListener")) {
        leadsListener = new BasicContinuousOperatorListener();
      }
      else {
        ce.printStackTrace();
      }
    } catch (InvocationTargetException e1) {
      e1.printStackTrace();
    } catch (NoSuchMethodException e1) {
      e1.printStackTrace();
    } catch (InstantiationException e1) {
      e1.printStackTrace();
    } catch (IllegalAccessException e1) {
      e1.printStackTrace();
    }
    try{
    leadsListener.setConfString(conf.toString());
      if(leadsListener != null) {
        imanager.addListener(leadsListener,cache);
      }
    }catch(Exception e){
      e.printStackTrace();
      actionResult.putString("status","FAIL");
      actionResult.putString("error", e.getMessage() == null ? "null" : e.getMessage().toString());
    }
    result.setResult(actionResult);
    return result;
  }
}
