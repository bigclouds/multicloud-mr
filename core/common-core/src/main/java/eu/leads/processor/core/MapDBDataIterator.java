package eu.leads.processor.core;

import org.bson.BasicBSONDecoder;
import org.mapdb.BTreeMap;

import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;

/**
 * Created by vagvaz on 10/11/15.
 */
public class MapDBDataIterator implements Iterator {

  BTreeMap data;
  String key;
  Integer total;
  int currentCounter;
  Iterator iterator;

  //    private BasicBSONDecoder decoder = new BasicBSONDecoder();
  public MapDBDataIterator(BTreeMap<Object, Object> dataDB, String key, Integer counter) {
    this.data = dataDB;
    this.key = key;
    this.total = counter;
  }

  @Override public boolean hasNext() {
    if (currentCounter <= total) {
      return true;
    }
    return false;
  }

  @Override public Object next() {
    if (currentCounter <= total) {
      Map.Entry<String, byte[]> entry = (Map.Entry<String, byte[]>) iterator.next();
      if (!validateKey(entry.getKey())) {
        System.err.println("SERIOUS ERRPR key " + key + " but entry " + entry.getKey());
      }
      BasicBSONDecoder decoder = new BasicBSONDecoder();
      Tuple result = new Tuple(decoder.readObject(entry.getValue()));
      currentCounter++;
      return result;

    }
    throw new NoSuchElementException("MapDB Iterator no more values");
  }

  private boolean validateKey(String key) {
    String keyString = key;
    if (this.key.equals(keyString.split("\\{\\}")[0])) {
      return true;
    }
    return false;
  }

  @Override public void remove() {

  }

  public void initialize(String key, int tot) {
    this.key = key;
    this.total = tot;
    this.currentCounter = 0;
    //        if(iterator!=null)
    //        reportState(key,tot);
    if (iterator == null) {
      iterator = data.descendingMap().entrySet().iterator();

      //      Map.Entry<String,byte[]> entry = (Map.Entry<String, byte[]>) iterator.next();
      //      if(!validateKey(entry.getKey())){
      //        System.out.println("Unsuccessful for key " + this.key + " was " + new String(entry.getKey()));
      //        String searchKey = key + "{}";
      //data.c(searchKey.getBytes());
    }
    return;
    //    }
    //    Map.Entry<String, byte[]> entry = (Map.Entry<String, byte[]>) iterator.next();
    //    if(!validateKey(entry.getKey())){
    //      System.out.println("Unsuccessful for key " + this.key + " was " + new String(entry.getKey()));
    //      String searchKey = key + "{}";
    //    }


  }

  private void reportState(String key, int tot) {
    //    iterator
  }

  public void close() {
    //    try {
    //      iterator.close();
    //    } catch (IOException e) {
    //      e.printStackTrace();
    //    }
  }
}
