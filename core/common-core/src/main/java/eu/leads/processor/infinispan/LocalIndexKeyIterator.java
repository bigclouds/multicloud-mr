package eu.leads.processor.infinispan;

import eu.leads.processor.common.utils.ProfileEvent;
import org.infinispan.Cache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

/**
 * Created by vagvaz on 16/07/15.
 */
public class LocalIndexKeyIterator implements Iterator<Object> {
  String key;
  Integer numberOfValues;
  Integer currentCounter;
  Cache<String, Object> dataCache;
  Future<Object> nextResult = null;
  List<Future<Object>> batch;
  ProfileEvent event;
  Logger logger;

  public LocalIndexKeyIterator(String key, Integer counter, Map<String, Object> dataCache) {
    this.currentCounter = counter;
    this.numberOfValues = counter;
    this.key = key;
    this.dataCache = (Cache<String, Object>) dataCache;
    batch = new LinkedList<>();
    readNextBatch();
    nextResult = batch.remove(0);
    logger = LoggerFactory.getLogger(LocalIndexListener.class);
    event = new ProfileEvent("", logger);
  }

  private void readNextBatch() {
    int counter = 0;
    while (currentCounter >= 0 && counter < 50000) {
      //        while(currentCounter <= numberOfValues && counter < 50000){
      batch.add(dataCache.getAsync(key + currentCounter));
      currentCounter--;
      counter++;
    }
  }


  @Override public boolean hasNext() {
    if (currentCounter >= 0 || batch.size() > 0 || nextResult != null) {
      //        if(currentCounter <= numberOfValues || batch.size() > 0 || nextResult != null) {
      return true;
    }
    return false;
  }

  @Override public Object next() {
    event.start("Next1");
    Object result = null;
    if (currentCounter >= 0 || batch.size() > 0 || nextResult != null) {
      //        if(currentCounter <= numberOfValues || batch.size() > 0 || nextResult != null){
      try {
        result = nextResult.get();
      } catch (InterruptedException e) {
        e.printStackTrace();
      } catch (ExecutionException e) {
        e.printStackTrace();
      }
      if (batch.size() > 0)
        nextResult = batch.remove(0);
      else
        nextResult = null;
      if (batch.size() < 50 && currentCounter >= 0)
        //            if(batch.size() < 50 && currentCounter <= numberOfValues)
        readNextBatch();
      event.end();
      event.start("Next2");

      event.end();
      return result;
      //            }
      //            throw new NoSuchElementException("LocalIndexIterator GOT NULL VALUE for  key " + key + " currentCounter " + currentCounter + " maximum " + numberOfValues);
    }
    throw new NoSuchElementException(
        "LocalIndexIterator key " + key + " currentCounter " + currentCounter + " maximum " + numberOfValues);
  }

  @Override public void remove() {
    System.err.println("SHOULD NOT BE USED");
    //        if(currentCounter > 0) {
    //            dataCache.remove(key + (currentCounter - 1));
    //        }
  }
}
