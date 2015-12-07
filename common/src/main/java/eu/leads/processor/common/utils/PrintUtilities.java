package eu.leads.processor.common.utils;

import eu.leads.processor.core.Tuple;
import org.infinispan.commons.api.BasicCache;
import org.infinispan.context.Flag;
import org.infinispan.manager.EmbeddedCacheManager;
import org.slf4j.Logger;
import org.vertx.java.core.json.JsonObject;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Created by vagvaz on 6/1/14.
 */
public class PrintUtilities {

    public static void printMap(Map<?, ?> map) {
        System.out.println("Map{ Size " + map.keySet().size() + "\n");
        for (Object  e : map.keySet()) {
            System.out.println("\t " + e.toString() + "--->" + map.get(e).toString());
        }
        System.out.println("end of map }");
    }

  public static void printMap(Map<?, ?> map, int numOfItems) {
    System.out.println("Map{\n");
    int counter = 0;
    for (Object e : map.keySet()) {
      System.out.println("\t " + e.toString() + "--->" + map.get(e).toString());
      counter++;
      if(counter > numOfItems)
        break;
    }
    System.out.println("end of map }");
  }
    public static void saveMapToFile(Map<?, ?> map,String filename) {
       RandomAccessFile raf = null;
       try {

          raf = new RandomAccessFile(filename,"rw");
       } catch (FileNotFoundException e) {
          e.printStackTrace();
       }
       System.out.println("Map{\n");
      for (Object e : map.keySet()) {
         try {
           Object val = map.get(e);//.toString();
            if(val instanceof Tuple || val instanceof JsonObject )
            {

               raf.writeBytes(val.toString() + "\n");
            }
            else{
               raf.writeBytes("\t " + e.toString() + "--->" + val.toString() + "\n");
            }
         } catch (IOException e1) {
            e1.printStackTrace();
         }
      }
       if (raf != null) {
          try {
             raf.close();
          } catch (IOException e) {
             e.printStackTrace();
          }
       }
       System.out.println("end of map }");
   }

    public static void printList(Collection<?> list) {
        System.err.println("List{");
        Iterator<?> it = list.iterator();
        while (it.hasNext()) {
            System.err.println("\t" + it.next().toString());
        }

        System.err.println("end of list}");
    }

    public static void printIterable(Iterator testCache) {
        System.out.println("Iterable{");
        Iterator<?> it = testCache;
        while (it.hasNext()) {
            System.out.println("\t" + it.next().toString());
        }
        System.out.println("end of iterable");
    }

    public static void logStackTrace(Logger profilerLog, StackTraceElement[] stackTrace) {
        for(StackTraceElement s : stackTrace){
            profilerLog.error(s.toString());
        }
    }

    public static void logMapCache(Logger log, Map<String, BasicCache> caches) {
        log.error("LOGMAPCACHE");
        for(Map.Entry<String,BasicCache> entry : caches.entrySet()){
            log.error(entry.getKey() + " --> " + entry.getValue()) ;
        }
    }

    public static void logMapKeys(Logger log, Map<String, Map<Object, Object>> objects) {
        log.error("LOGMAPKEYS");
        for(Map.Entry<String,Map<Object, Object>> entry : objects.entrySet()){
            log.error(entry.getKey() + " --> " + entry.getValue().size()) ;
        }
    }

        public static void printCaches(EmbeddedCacheManager manager) {
        String s = ("\n\n--- Remaining ----\n");

        for(String c : manager.getCacheNames()) {
          if(manager.cacheExists(c))
           s+=("Exist name: " + c + "\n");
          if(manager.isRunning(c)){
            s+="Running name: " + c + "\n";
          }
        }
        System.err.println(s + "\n\n---END Remaining ---- " + manager.getCacheNames().size());
    }

  public static void printAndLog(Logger profilerLog, String msg) {
    System.err.println(msg);
    profilerLog.error(msg);
  }
}
