package eu.leads.processor.common.continuous;

import java.util.*;
import java.util.concurrent.ConcurrentLinkedDeque;

/**
 * Created by anydriotis on 10/5/15.
 */

import org.mapdb.DB;
import org.mapdb.DBMaker;

import java.util.Collection;
import java.util.Iterator;
import java.util.concurrent.BlockingQueue;

/**
 * Created by vagvaz on 10/5/15.
 */
public class ConcurrentDiskQueue implements java.util.Queue {

  BlockingQueue queue;
  Deque inMemory;
  int threshold = 0;
  DB db;

  public ConcurrentDiskQueue(int blockSize) {
    this.threshold = blockSize;
    db = DBMaker.tempFileDB()
        .fileMmapEnable()
        .fileMmapEnableIfSupported()
            //        .cacheSize(blockSize)
        .deleteFilesAfterClose()
        .closeOnJvmShutdown()
        .transactionDisable()
        .asyncWriteEnable()
            //        .asyncWriteQueueSize(blockSize)
        .make();
    queue = db.getQueue("fifo");
    inMemory = new LinkedList();
  }

  @Override
  public int size() {
    return inMemory.size();
  }

  @Override
  public synchronized boolean isEmpty() {
    return ( queue.isEmpty() && inMemory.isEmpty());
  }

  @Override
  public boolean contains(Object o) {
    return queue.contains(o);
  }

  @Override
  public Iterator iterator() {
    return queue.iterator();
  }

  @Override
  public Object[] toArray() {
    return queue.toArray();
  }

  @Override
  public Object[] toArray(Object[] a) {
    return queue.toArray(a);
  }

  @Override
  public synchronized boolean  add(Object o) {
    if(inMemory.size() > threshold)
      return queue.add(o);
    else
      return inMemory.add(o);
  }

  @Override
  public boolean remove(Object o) {
    return queue.remove(o);
  }

  @Override
  public boolean addAll(Collection c) {
    return queue.addAll(c);
  }

  @Override
  public void clear() {
    queue.clear();
    inMemory.clear();
  }

  @Override
  public boolean retainAll(Collection c) {
    return queue.retainAll(c);
  }

  @Override
  public boolean removeAll(Collection c) {
    return queue.removeAll(c);
  }

  @Override
  public boolean containsAll(Collection c) {
    return queue.containsAll(c);
  }

  @Override
  public boolean offer(Object o) {
    return queue.offer(o);
  }

  @Override
  public Object remove() {
    return queue.remove();
  }

  @Override
  public synchronized Object poll() {
    if(inMemory.size() > 0)
      return inMemory.poll();

    return queue.poll();
  }

  @Override
  public Object element() {
    return queue.element();
  }

  @Override
  public Object peek() {
    return queue.peek();
  }
}
