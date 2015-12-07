package eu.leads.processor.core;

import eu.leads.processor.common.utils.PrintUtilities;
import org.bson.BasicBSONEncoder;
import org.infinispan.commons.util.Util;
import org.iq80.leveldb.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.*;

import static org.fusesource.leveldbjni.JniDBFactory.asString;
import static org.fusesource.leveldbjni.JniDBFactory.bytes;

/**
 * Created by vagvaz on 8/17/15.
 */
public class LevelDBIndex {
  private static final String JNI_DB_FACTORY_CLASS_NAME = "org.fusesource.leveldbjni.JniDBFactory";
  private static final String JAVA_DB_FACTORY_CLASS_NAME = "org.iq80.leveldb.impl.Iq80DBFactory";
  private static Tuple t;
  private WriteOptions writeOptions;
  private DB keysDB;
  private DB dataDB;
  private File baseDirFile;
  private File keydbFile;
  private File datadbFile;
  private Options options;
  private LevelDBIterator keyIterator;
  private LevelDBDataIterator valuesIterator;
  private int batchSize = 50000;
  private int batchCount = 0;
  private WriteBatch batch;
  private WriteBatch keyBatch;
  private DBFactory dbfactory;
  private Logger log = LoggerFactory.getLogger(LevelDBIndex.class);
  private boolean isclosed = false;
  //    private BasicBSONEncoder encoder = new BasicBSONEncoder();

  public LevelDBIndex(String baseDir, String name) {
    baseDirFile = new File(baseDir);
    if (baseDirFile.exists() && baseDirFile.isDirectory()) {
      for (File f : baseDirFile.listFiles()) {
        f.delete();
      }
      baseDirFile.delete();
    } else if (baseDirFile.exists()) {
      baseDirFile.delete();
    }
    baseDirFile.mkdirs();
    keydbFile = new File(baseDirFile.toString() + "/keydb");
    datadbFile = new File(baseDirFile.toString() + "/datadb");
    options = new Options();
    options.writeBufferSize(50 * 1024 * 1024);
    options.createIfMissing(true);
    //        options.blockSize(LQPConfiguration.getInstance().getConfiguration()
    //            .getInt("leads.processor.infinispan.leveldb.blocksize", 16)*1024*1024);
    //        options.cacheSize(LQPConfiguration.getInstance().getConfiguration()
    //            .getInt("leads.processor.infinispan.leveldb.cachesize", 256)*1024*1024);
    options.blockSize(4 * 1024);

    options.compressionType(CompressionType.SNAPPY);
    options.cacheSize(64 * 1024 * 1024);
    dbfactory = Util.getInstance(JNI_DB_FACTORY_CLASS_NAME, LevelDBIndex.class.getClassLoader());
    //        JniDBFactory.pushMemoryPool(128*1024*1024);
    try {
      keysDB = dbfactory.open(keydbFile, options.verifyChecksums(true));
      dataDB = dbfactory.open(datadbFile, options);
      writeOptions = new WriteOptions();
      writeOptions.sync(false);

      batch = dataDB.createWriteBatch();
    } catch (IOException e) {
      e.printStackTrace();
    }

  }

  public void printKeys() {
    DBIterator iterator = keysDB.iterator();
    iterator.seekToFirst();
    while (iterator.hasNext()) {
      System.out.println(asString(iterator.next().getKey()));
    }

    iterator = dataDB.iterator();
    iterator.seekToFirst();
    while (iterator.hasNext()) {
      System.out.println(asString(iterator.next().getKey()));
    }
    System.out.println("values-------------\n");
  }

  public Iterable<Map.Entry<String, Integer>> getKeysIterator() {
    keyIterator = new LevelDBIterator(keysDB);
    return keyIterator;
  }

  public Iterator<Object> getKeyIterator(String key, Integer counter) {
    //        if(valuesIterator != null){
    //            valuesIterator.close();
    //        }
    if (valuesIterator == null)
      valuesIterator = new LevelDBDataIterator(dataDB, key, counter);
    //        }

    valuesIterator.initialize(key, counter);
    return valuesIterator;

  }

  public static void main(String[] args) {
    for (int i = 0; i < 100; i++) {
      LevelDBIndex index = new LevelDBIndex("/tmp/testdb/", "mydb");
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

      //               index.printKeys();

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
    try {
      if (dataDB != null) {
        if (batch != null) {
          dataDB.write(batch);
          batch.close();
          batch = dataDB.createWriteBatch();
        }
      }
    } catch (IOException e) {
      e.printStackTrace();
    }
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

  public synchronized void put(Object key, Object value) {
    add(key, value);
  }

  public void add(Object keyObject, Object valueObject) {
    try {
      String key = null;
      Tuple value = null;

      key = keyObject.toString();
      value = (Tuple) valueObject;
      byte[] count = keysDB.get(bytes(key + "{}"));
      Integer counter = -1;
      if (count == null) {
        counter = 0;
      } else {
        //            ByteBuffer bytebuf = ByteBuffer.wrap(count);
        counter = Integer.parseInt(new String(count));
        counter += 1;
      }
      byte[] keyvalue = bytes(counter.toString());
      keysDB.put(bytes(key + "{}"), keyvalue, writeOptions);
      //        encoder = new BasicBSONEncoder();
      BasicBSONEncoder encoder = new BasicBSONEncoder();
      byte[] b = encoder.encode(value.asBsonObject());

      //        System.out.println(b.length);
      //        dataDB.put(bytes(key+"{}"+counter),b,writeOptions);
      batch.put(bytes(key + "{}" + counter), b);
      batchCount++;
      if (batchCount >= batchSize) {
        try {
          dataDB.write(batch, writeOptions);
          batch.close();
        } catch (IOException e) {
          e.printStackTrace();
        }
        batch = dataDB.createWriteBatch();
        batchCount = 0;
      }

    } catch (Exception e) {
      e.printStackTrace();
      PrintUtilities.logStackTrace(log, e.getStackTrace());
    }
  }


  //

  public synchronized void close() {

    if(isclosed)
      return;
    isclosed = true;
    if (keyIterator != null) {
      keyIterator.close();
    }
    keyIterator = null;
    if (valuesIterator != null) {
      valuesIterator.close();

    }
    valuesIterator = null;
    if (keysDB != null) {
      try {
        keysDB.close();
        dbfactory.destroy(keydbFile, new Options());
      } catch (IOException e) {
        e.printStackTrace();
      }
    }
    keysDB = null;
    if (dataDB != null) {
      try {
        if (batch != null) {
          batch.close();
        }

        dataDB.close();
        dbfactory.destroy(datadbFile, new Options());
      } catch (IOException e) {
        e.printStackTrace();
      }
    }
    dataDB = null;
    //        for(File f : keydbFile.listFiles())
    //        {
    //            f.delete();
    //        }
    //        keydbFile.delete();
    //
    //        for(File f : datadbFile.listFiles())
    //        {
    //            f.delete();
    //        }
    //        datadbFile.delete();
    //
    //        baseDirFile = new File(baseDirFile.toString()+"/");
    //        for(File f : baseDirFile.listFiles())
    //        {
    //            f.delete();
    //        }
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
