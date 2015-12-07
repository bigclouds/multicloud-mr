package eu.leads.processor.core;

import com.sleepycat.je.*;

import java.nio.charset.Charset;
import java.util.AbstractMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Created by vagvaz on 8/17/15.
 */
public class BerkeleyDBKeyIterable
    implements Iterable<Map.Entry<String, Integer>>, Iterator<Map.Entry<String, Integer>> {

  Cursor cursor;
  Database db;
  DatabaseEntry searchKey;
  DatabaseEntry searchValue;
  OperationStatus status;

  public BerkeleyDBKeyIterable(Database indexDB) {
    this.db = indexDB;
    try {
      cursor = db.openCursor(null, null);
      searchKey = new DatabaseEntry();
      searchValue = new DatabaseEntry();
      status = cursor.getNext(searchKey, searchValue, LockMode.DEFAULT);
    } catch (DatabaseException e) {
      e.printStackTrace();
    }

  }

  @Override public Iterator<Map.Entry<String, Integer>> iterator() {
    return this;
  }

  @Override public boolean hasNext() {
    if (status == OperationStatus.SUCCESS)
      return true;
    return false;
  }

  @Override public Map.Entry<String, Integer> next() {
    if (status == OperationStatus.SUCCESS) {
      String key = new String(searchKey.getData(), Charset.forName("UTF-8"));
      Integer value = 0;
      try {
        status = cursor.getNextNoDup(searchKey, searchValue, LockMode.DEFAULT);
      } catch (DatabaseException e) {
        e.printStackTrace();
      }
      return new AbstractMap.SimpleEntry<String, Integer>(key, value);
    }
    return null;
  }

  @Override public void remove() {

  }

  public void close() {
    cursor.close();
  }
}
