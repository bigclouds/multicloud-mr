package eu.leads.processor.core;

import org.iq80.leveldb.DB;
import org.iq80.leveldb.DBIterator;
import org.iq80.leveldb.ReadOptions;

import java.io.IOException;
import java.util.AbstractMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Created by vagvaz on 8/17/15.
 */
public class LevelDBIterator implements Iterable<Map.Entry<String, Integer>>, Iterator<Map.Entry<String, Integer>> {
  DB db;
  DBIterator iterator;
  ReadOptions readOptions;

  public LevelDBIterator(DB keysDB) {
    this.db = keysDB;
    readOptions = new ReadOptions();
    readOptions.fillCache(false);
    //        readOptions.verifyChecksums(true);
    this.iterator = db.iterator(readOptions);
    this.iterator.seekToFirst();
    //        System.out.println( new String(this.iterator.peekPrev().getKey()));
  }

  @Override public synchronized Iterator<Map.Entry<String, Integer>> iterator() {
    return this;
  }

  @Override public synchronized boolean hasNext() {
    return iterator.hasNext();
  }

  @Override public synchronized Map.Entry<String, Integer> next() {
    String key;
    Integer value;
    if (iterator.hasNext()) {
      Map.Entry<byte[], byte[]> entry = iterator.next();
      key = new String(entry.getKey());
      value = Integer.parseInt(new String(entry.getValue()));

      return new AbstractMap.SimpleEntry<String, Integer>(key.substring(0, key.length() - 2), value);
    }
    return null;
  }

  @Override public void remove() {

  }

  public synchronized void close() {
    try {
      iterator.close();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }
}
