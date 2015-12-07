package eu.leads.processor.core;

import eu.leads.processor.common.utils.PrintUtilities;
import org.bson.BasicBSONEncoder;
import org.mapdb.BTreeMap;
import org.mapdb.DB;
import org.mapdb.DBMaker;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.*;


/**
 * Created by vagvaz on 10/11/15.
 */
public class MapDBIndex {
  private static Tuple t;
  private DB theDb;

  BTreeMap<String, Integer> keysDB;
  BTreeMap<Object, Object> dataDB;
  private File baseDirFile;
  private File keydbFile;
  private File datadbFile;

  private Iterable<Map.Entry<String, Integer>> keyIterator;
  private MapDBDataIterator valuesIterator;
  //  private int batchSize = ;
  private int batchCount = 0;

  private org.slf4j.Logger log = LoggerFactory.getLogger(MapDBIndex.class);

  public MapDBIndex(String baseDir, String name) {
    baseDirFile = new File(baseDir);
    if (baseDirFile.exists() && baseDirFile.isDirectory()) {
      for (File f : baseDirFile.listFiles()) {
        f.delete();
      }
      baseDirFile.delete();
    } else if (baseDirFile.exists()) {
      baseDirFile.delete();
    }
    baseDirFile.getParentFile().mkdirs();
    keydbFile = new File(baseDirFile.toString() + "/keydb");
    datadbFile = new File(baseDirFile.toString() + "/datadb");


    try {
      theDb = DBMaker.tempFileDB().transactionDisable().closeOnJvmShutdown().deleteFilesAfterClose().asyncWriteEnable()
          .asyncWriteQueueSize(50000).executorEnable().make();
      keysDB = theDb.createTreeMap(keydbFile.getName()).nodeSize(1024)
          .make(); // /dbfactory.open(keydbFile,options.verifyChecksums(true));
      dataDB = theDb.createTreeMap(datadbFile.getName()).nodeSize(1024).make();



    } catch (Exception e) {
      e.printStackTrace();
    }

  }

  public void printKeys() {
    Iterator iterator = keysDB.descendingMap().entrySet().iterator();
    //    iterator.seekToFirst();
    while (iterator.hasNext()) {
      Map.Entry e = (Map.Entry) iterator.next();
      System.out.println(e.getKey() + " -> " + e.getValue());
    }
    System.out.println("keyvalues++++++++++++++++++\n");
    iterator = dataDB.descendingMap().entrySet().iterator();
    while (iterator.hasNext()) {
      Map.Entry e = (Map.Entry) iterator.next();
      System.out.println(e.getKey());
    }
    System.out.println("values-------------\n");
  }

  public Iterable<Map.Entry<String, Integer>> getKeysIterator() {
    keyIterator = keysDB.descendingMap().entrySet();
    return keyIterator;
  }

  public Iterator<Object> getKeyIterator(String key, Integer counter) {
    //        if(valuesIterator != null){
    //            valuesIterator.close();
    //        }
    if (valuesIterator == null)
      valuesIterator = new MapDBDataIterator(dataDB, key.split("\\{\\}")[0], counter);
    //        }

    valuesIterator.initialize(key.split("\\{\\}")[0], counter);
    return valuesIterator;

  }

  public static void main(String[] args) {
    for (int i = 0; i < 1; i++) {
      MapDBIndex index = new MapDBIndex("/tmp/testdb/foo/bar/lakia/ma", "mydb");
      if (t == null)
        initTuple();
      int numberofkeys = 500000;
      int numberofvalues = 2;
      String baseKey = "baseKeyString";

      long start = System.nanoTime();
      ArrayList<String> tuples = generate(numberofkeys, numberofvalues);
      System.out.println("insert");
      for (String k : tuples) {
        //            System.out.println("key " + key);
        //            for(int value =0; value < numberofvalues; value++){
        index.add(k, t);
        //            }
      }
      index.flush();
      long end = System.nanoTime();
      long dur = end - start;
      dur /= 1000000;
      int total = numberofkeys * numberofvalues;
      double avg = total / (double) dur;

      System.out.println("Put " + (total) + " in " + (dur) + " avg " + avg);
      int counter = 0;

      //           index.printKeys();

      start = System.nanoTime();
      //        for(int key = 0; key < numberofkeys; key++) {
      int totalcounter = 0;
      for (Map.Entry<String, Integer> entry : index.getKeysIterator()) {
        counter = 0;
        //            System.out.println("iter key "+entry.getKey());
        Iterator<Object> iterator = index.getKeyIterator(entry.getKey(), entry.getValue());
        while (iterator.hasNext()) {
          try {
            Tuple t = (Tuple) iterator.next();
            //                    String t = (String)iterator.next();
            //                System.out.println(t.getAttribute("key")+" --- " + t.getAttribute("value"));
            counter++;
            totalcounter++;
          } catch (NoSuchElementException e) {
            break;
          }
        }
        //                ((LevelDBDataIterator)iterator).close();
        if (counter != numberofvalues) {
          System.err.println("Iteration failed for key " + entry.getKey() + " c " + counter);
        }
      }
      end = System.nanoTime();
      dur = end - start;
      dur /= 1000000;
      avg = total / (double) dur;
      System.out.println("Iterate " + (totalcounter) + " in " + (dur) + " avg " + avg);
      index.close();
      System.out.println("exit---");
    }
    System.err.println("Bye bye after pressing any button");
    try {
      System.in.read();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  //    80.156.73.113:11222;80.156.73.116:11222;80.156.73.123:11222;80.156.73.128:11222
  //    ;
  public synchronized void flush() {
    //    try {

    //      dataDB.write(batch);
    //      batch.close();
    //      batch = dataDB.createWriteBatch();
    //    } catch (IOException e) {
    //      e.printStackTrace();
    //    }
    //        printKeys();

  }

  private static ArrayList<String> generate(int numberofkeys, int numberofvalues) {
    String baseKey = "baseKeyString";
    System.out.println("generate");
    ArrayList<String> result = new ArrayList<>(numberofkeys * numberofvalues);
    for (int key = 0; key < numberofkeys; key++) {
      //            System.out.println("key " + key);
      for (int value = 0; value < numberofvalues; value++) {
        result.add(baseKey + key);
      }
    }
    Collections.shuffle(result);
    return result;
  }

  private static void initTuple() {
    t = new Tuple();
    int key = 4;
    int value = 5;
    for (int i = 0; i < 4; i++) {
      t.setAttribute("key-" + key + "-" + i, key);
      t.setAttribute("value-" + value + "-" + i, value);
      t.setAttribute("keyvalue-" + key + "." + value + "-" + i, key * value);
    }
  }

  private static Tuple getTuple(int key, int value) {

    //        t = new Tuple();
    //        int key = 4;
    //        int value = 5;
    //        t.setAttribute("key","baseKey"+Integer.toString(key));
    //        t.setAttribute("value",Integer.toString(value));
    //        for(int i = 0 ; i < 4; i++){
    //            t.setAttribute("key-"+key+"-"+i,key);
    //            t.setAttribute("value-"+value+"-"+i,value);
    //            t.setAttribute("keyvalue-"+key+"."+value+"-"+i,key*value);
    //        }
    return t;
  }

  public  void put(Object key, Object value) {
    add(key, value);
  }

  public void add(Object keyObject, Object valueObject) {
    try {
      String key = null;
      Tuple value = null;

      key = keyObject.toString();
      value = (Tuple) valueObject;
      Integer counter = keysDB.get((key + "{}"));

      if (counter == null) {
        counter = 0;
      } else {
        counter += 1;
      }

      keysDB.put(key + "{}", counter);

      BasicBSONEncoder encoder = new BasicBSONEncoder();
      byte[] b = encoder.encode(value.asBsonObject());

      //        System.out.println(b.length);
      //        dataDB.put(bytes(key+"{}"+counter),b,writeOptions);
      dataDB.put((key + "{}" + counter), b);

    } catch (Exception e) {
      e.printStackTrace();
      PrintUtilities.logStackTrace(log, e.getStackTrace());
    }
  }


  //

  public void close() {
    if (keyIterator != null) {
      //      keyIterator.close();

    }
    keyIterator = null;
    if (valuesIterator != null) {
      //      valuesIterator.close();
    }
    valuesIterator = null;
    //    if(keysDB != null){
    //      try {
    //        keysDB.close();
    //      } catch (Exception e) {
    //        e.printStackTrace();
    //      }
    //    }
    //    keysDB = null;
    //    if(dataDB != null){
    //      try {
    //        dataDB.close();
    //      } catch (Exception e) {
    //        e.printStackTrace();
    //      }
    //    }
    dataDB = null;
    theDb.close();
    if (baseDirFile.exists()) {
      baseDirFile.delete();
    }
    //        System.out.println("press");
    //        try {
    //            System.in.read();
    //        } catch (IOException e) {
    //            e.printStackTrace();
    //        }
    //        JniDBFactory.popMemoryPool();
  }

  @Override public void finalize() {
    System.err.println("Finalize leveldb Index " + this.baseDirFile.toString());
  }

}
