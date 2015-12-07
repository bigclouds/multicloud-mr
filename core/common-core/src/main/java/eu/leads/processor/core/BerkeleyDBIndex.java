package eu.leads.processor.core;

import com.sleepycat.je.*;

import java.io.File;
import java.util.Iterator;
import java.util.Map;
import java.util.Random;

/**
 * Created by vagvaz on 8/14/15.
 */
public class BerkeleyDBIndex {
  private EnvironmentConfig environmentConfig;
  private File dbFile;
  //    private EntityStore store;
  Environment env;
  DatabaseConfig dbConfig;
  Database indexDB;

  BerkeleyDBIterator iterator = null;
  private BerkeleyDBKeyIterable keyIterable;

  public BerkeleyDBIndex(String baseDir, String dbName) {
    environmentConfig = new EnvironmentConfig();
    environmentConfig.setAllowCreate(true);
    environmentConfig.setLocking(false);
    environmentConfig.setTransactional(false);
    environmentConfig.setCacheSize(2147483648L);
    environmentConfig.setCacheMode(CacheMode.DYNAMIC);

    dbConfig = new DatabaseConfig();
    dbConfig.setTransactional(false);
    dbConfig.setAllowCreate(true);
    dbConfig.setTemporary(true);
    dbConfig.setCacheMode(CacheMode.DEFAULT);

    dbFile = new File(baseDir);
    dbFile.mkdirs();
    try {
      env = new Environment(dbFile, environmentConfig);
      //            dbConfig.setDeferredWrite(true);
      dbConfig.setTransactional(false);
      dbConfig.setSortedDuplicates(true);
      //            dbConfig.setDuplicateComparator( new DummyComparator());

      //            dbConfig.setOverrideDuplicateComparator(true);
      indexDB = env.openDatabase(null, dbName, dbConfig);

      iterator = new BerkeleyDBIterator(indexDB, "");
    } catch (DatabaseException e) {
      e.printStackTrace();
      return;
    }
  }

  public Iterable<Map.Entry<String, Integer>> getKeysIterator() {
    keyIterable = new BerkeleyDBKeyIterable(indexDB);

    return keyIterable;
  }

  public Iterator<Object> getKeyIterator(String key, Integer counter) {
    if (iterator == null) {
      iterator = new BerkeleyDBIterator(indexDB, key);
    }
    iterator.initialize(key);
    return iterator;
  }

  public static void main(String[] args) {
    BerkeleyDBIndex index = new BerkeleyDBIndex("/tmp/testdb/", "mydb");
    int numberofkeys = 200000;
    int numberofvalues = 2;
    String baseKey = "baseKeyString";
    String Value = "";
    for (int i = 0; i < 65; i++)
      Value = Value + "1234567890";
    System.err.println(Value.getBytes().length);
    Random rn = new Random();
    long start = System.nanoTime();

    for (int key = 0; key < numberofkeys; key++) {
      //            System.out.println("key " + key);
      for (int value = 0; value < numberofvalues; value++) {
        index.add(baseKey + key, Value + rn.nextInt());
      }
    }
    long end = System.nanoTime();
    long dur = end - start;
    dur /= 1000000;
    int total = numberofkeys * numberofvalues;
    double avg = total / (double) dur;

    System.out.println("Put " + (total) + " in " + (dur) + " avg " + avg);
    int counter = 0;
    start = System.nanoTime();
    //        for(int key = 0; key < numberofkeys; key++) {
    for (Map.Entry<String, Integer> entry : index.getKeysIterator()) {
      counter = 0;
      Iterator<Object> iterator = index.getKeyIterator(entry.getKey(), numberofvalues);
      while (iterator.hasNext()) {
        String t = (String) iterator.next();
        counter++;
      }
      if (counter != numberofvalues) {
        System.err.println("Iteration failed for key " + entry.getKey());
      }
    }
    end = System.nanoTime();
    dur = end - start;
    dur /= 1000000;
    avg = total / (double) dur;
    System.out.println("Iterate " + (total) + " in " + (dur) + " avg " + avg);
    index.close();
    System.out.println("exit---");
  }


  public void put(Object key, Object value) {
    add(key, value);
  }

  public void add(Object keyObject, Object valueObject) {
    DatabaseEntry keyEntry = getDBKey(keyObject);
    DatabaseEntry valueEntry = getDBValue(valueObject);
    try {
      indexDB.put(null, keyEntry, valueEntry);
    } catch (DatabaseException e) {
      e.printStackTrace();
    }
  }

  private DatabaseEntry getDBValue(Object valueObject) {
    DatabaseEntry result = new DatabaseEntry(((String) valueObject).getBytes());
    return result;
  }

  private DatabaseEntry getDBKey(Object keyObject) {
    DatabaseEntry result = new DatabaseEntry(((String) keyObject).getBytes());
    return result;
  }


  public void close() {
    if (iterator != null) {
      iterator.close();
    }
    if (keyIterable != null) {
      keyIterable.close();
    }
    if (indexDB != null) {
      try {
        indexDB.close();
      } catch (DatabaseException dbe) {
        System.err.println("Error closing store: " + dbe.toString());
        System.exit(-1);
      }
    }

    if (env != null) {
      try {
        // Finally, close environment.
        env.close();
      } catch (DatabaseException dbe) {
        System.err.println("Error closing MyDbEnv: " + dbe.toString());
        System.exit(-1);
      }
    }
    dbFile = new File(dbFile.toString() + "/");
    for (File f : dbFile.listFiles()) {
      f.delete();
    }
    dbFile.delete();

  }


}

//
//import com.sleepycat.je.*;
//import eu.leads.processor.infinispan.ComplexIntermediateKey;
//
//import java.io.File;
//import java.nio.charset.Charset;
//import java.util.Iterator;
//import java.util.Map;
//
///**
// * Created by vagvaz on 8/14/15.
// */
//public class BerkeleyDBIndex {
//    private static Tuple t;
//    private EnvironmentConfig environmentConfig;
//    private File dbFile;
//    //    private EntityStore store;
//    Environment env;
//    DatabaseConfig dbConfig;
//    Database indexDB;
//    TupleWrapperBinding tupleBinding;
//
//    BerkeleyDBIterator iterator = null;
//    private BerkeleyDBKeyIterable keyIterable;
//
//    public BerkeleyDBIndex(String baseDir, String dbName) {
//        environmentConfig = new EnvironmentConfig();
//        environmentConfig.setAllowCreate(true);
//        environmentConfig.setLocking(false);
//        environmentConfig.setTransactional(false);
//        environmentConfig.setCacheSize(64 * 1024 * 1024);
//
//        dbConfig = new DatabaseConfig();
//        dbConfig.setTransactional(false);
//        dbConfig.setAllowCreate(true);
//        dbConfig.setTemporary(true);
//        dbConfig.setCacheMode(CacheMode.DEFAULT);
//
//        dbFile = new File(baseDir);
//        dbFile.mkdirs();
//        try {
//            env = new Environment(dbFile, environmentConfig);
//            tupleBinding = new TupleWrapperBinding();
//            //            primaryIndex = store.getPrimaryIndex(String.class, TupleWrapper.class);
//            //            secondaryIndex = store.getSecondaryIndex(primaryIndex, String.class, "key");
////            dbConfig.setDeferredWrite(true);
//            dbConfig.setTransactional(false);
//            dbConfig.setSortedDuplicates(true);
////            dbConfig.setDuplicateComparator( new DummyComparator());
//
////            dbConfig.setOverrideDuplicateComparator(true);
//            indexDB = env.openDatabase(null,dbName,dbConfig);
//
//            iterator = new BerkeleyDBIterator(indexDB,"");
//        } catch (DatabaseException e) {
//            e.printStackTrace();
//            return;
//        }
//    }
//
//    public Iterable<Map.Entry<String,Integer>> getKeysIterator(){
//                 keyIterable = new BerkeleyDBKeyIterable(indexDB);
//
//        return keyIterable;
//    }
//    public Iterator<Object> getKeyIterator(String key , Integer counter){
//        if(iterator == null){
//            iterator = new BerkeleyDBIterator(indexDB,key);
//        }
//        iterator.initialize(key);
//        return iterator;
//    }
//
//    public static void main(String[] args) {
//        BerkeleyDBIndex index = new BerkeleyDBIndex("/tmp/testdb/","mydb");
//        initTuple();
//        int numberofkeys = 200000;
//        int numberofvalues = 2;
//        String baseKey= "baseKeyString";
//
//        long start = System.nanoTime();
//
//        for(int key = 0; key < numberofkeys; key++){
////            System.out.println("key " + key);
//            for(int value =0; value < numberofvalues; value++){
//                index.add(baseKey+key,getTuple(key,value));
//            }
//        }
//        long end = System.nanoTime();
//        long dur = end - start;
//        dur /= 1000000;
//        int total = numberofkeys*numberofvalues;
//        double avg = total/(double)dur;
//
//        System.out.println("Put " + (total) + " in " + (dur) + " avg " + avg);
//        int counter  =0;
//        start = System.nanoTime();
////        for(int key = 0; key < numberofkeys; key++) {
//        for(Map.Entry<String,Integer> entry : index.getKeysIterator()){
//            counter = 0;
//            Iterator<Object> iterator = index.getKeyIterator(entry.getKey(),numberofvalues);
//            while(iterator.hasNext()){
//                Tuple t = (Tuple) iterator.next();
//                counter++;
//            }
//            if(counter != numberofvalues){
//                System.err.println("Iteration failed for key " + entry.getKey());
//            }
//        }
//        end = System.nanoTime();
//        dur = end - start;
//        dur /= 1000000;
//        avg = total/(double)dur;
//        System.out.println("Iterate " + (total) + " in " + (dur) + " avg " + avg);
//        index.close();
//        System.out.println("exit---");
//    }
//
//    private static void initTuple(){
//        t = new Tuple();
//        int key = 4;
//        int value = 5;
//        for(int i = 0 ; i < 10; i++){
//            t.setAttribute("key-"+key+"-"+i,key);
//            t.setAttribute("value-"+value+"-"+i,value);
//            t.setAttribute("keyvalue-"+key+"."+value+"-"+i,key*value);
//        }
//    }
//    private static Tuple getTuple(int key, int value) {
//
//        t = new Tuple();
////        int key = 4;
////        int value = 5;
//        for(int i = 0 ; i < 10; i++){
//            t.setAttribute("key-"+key+"-"+i,key);
//            t.setAttribute("value-"+value+"-"+i,value);
//            t.setAttribute("keyvalue-"+key+"."+value+"-"+i,key*value);
//        }
//        System.out.println(t.asString().length());
//        return t;
//    }
//
//    public void put(Object key,Object value){
//        add(key,value);
//    }
//    public void add(Object keyObject , Object valueObject){
//        String key = null;
//        Tuple value = null;
//
//
//
//        DatabaseEntry keyEntry = getDBKey(keyObject);
//        DatabaseEntry valueEntry = getDBValue(valueObject);
//        try {
//            indexDB.put(null,keyEntry,valueEntry);
//        } catch (DatabaseException e) {
//            e.printStackTrace();
//        }
//    }
//
//    private DatabaseEntry getDBValue(Object valueObject) {
//        DatabaseEntry result = new DatabaseEntry();
//        Object value = null;
//        if(valueObject instanceof TupleWrapper){
//            value = (TupleWrapper)valueObject;
//        }
//        else if (valueObject instanceof Tuple){
//            value = valueObject;
//        }
//        else {
//
//            System.err.println("value class of " + valueObject.getClass().toString());
//        }
//        tupleBinding.objectToEntry(value,result);
//        return result;
//    }
//
//    private DatabaseEntry getDBKey(Object keyObject) {
//
//        String key = null;
//        if(keyObject instanceof ComplexIntermediateKey){
//            key = ((ComplexIntermediateKey)keyObject).asString();
//        }
//        else{
//            //                System.err.println("key class of " + keyObject.getClass().toString());
//            key = keyObject.toString();
//        }
//        DatabaseEntry result = new DatabaseEntry(key.getBytes(Charset.forName("UTF-8")));
//        return result;
//    }
//
//    public void close() {
//        if(iterator != null)
//        {
//            iterator.close();
//        }
//        if(keyIterable != null){
//            keyIterable.close();
//        }
//        if (indexDB != null) {
//            try {
//                indexDB.close();
//            } catch (DatabaseException dbe) {
//                System.err.println("Error closing store: " + dbe.toString());
//                System.exit(-1);
//            }
//        }
//
//        if (env != null) {
//            try {
//                // Finally, close environment.
//                env.close();
//            } catch (DatabaseException dbe) {
//                System.err.println("Error closing MyDbEnv: " + dbe.toString());
//                System.exit(-1);
//            }
//        }
//        dbFile = new File(dbFile.toString()+"/");
//        for(File f : dbFile.listFiles())
//        {
//            f.delete();
//        }
//        dbFile.delete();
//
//    }
//
//
//}
