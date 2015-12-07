package eu.leads.processor.common.continuous;

import eu.leads.processor.common.utils.PrintUtilities;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Queue;

/**
 * Created by vagvaz on 10/5/15.
 */
public class ContinuousProcessingThread extends Thread{
  protected BasicContinuousListener owner;
  protected Queue eventQueue;
  protected InputBuffer buffer;
  protected volatile Object ownerMutex;
  static Logger log = LoggerFactory.getLogger(ContinuousProcessingThread.class);
//  protected volatile Object mutex = new Object();
  public ContinuousProcessingThread(BasicContinuousListener owner){
    this.owner = owner;
    eventQueue = owner.getEventQueue();
    buffer = owner.getBuffer();
    ownerMutex = owner.getMutex();
    log = LoggerFactory.getLogger(ContinuousProcessingThread.class);
  }

  @Override
  public void run(){
    try {
      log.error("Started CONTINUOUSTHREAD");
      while (!owner.getIsFlushed() || !eventQueue.isEmpty() || !buffer.isEmpty()) {
          if(!owner.getIsFlushed()){
            if(!eventQueue.isEmpty()){
              EventTriplet triplet = (EventTriplet)eventQueue.poll();
              if(triplet != null){
                processEvent(triplet);
              }else{
                System.err.println("problem queue says not NULL but reutrned triplet is null");
              }
            }else{
              synchronized (owner.queueMutex){
                owner.queueMutex.wait();
              }
            }
          }else{
            while(!eventQueue.isEmpty()){
              EventTriplet triplet = (EventTriplet)eventQueue.poll();
              if(triplet != null){
                processEvent(triplet);
              }else{
                System.err.println("problem queue says not NULL but reutrned triplet is null");
              }
            }
            if(!buffer.isEmpty()){
              owner.processBuffer();
              buffer.clear();
            }
            owner.signal();
          }
        //        EventTriplet triplet = (EventTriplet) eventQueue.poll();
//        if (triplet != null) {
//          processEvent(triplet);
//        }
//        if (owner.getIsFlushed()) {
//          if (eventQueue.isEmpty()) {
//            owner.processBuffer();
//            buffer.clear();
//            owner.signal();
//            return;
//          }
//        } else {
//          if (eventQueue.isEmpty()) {
//            synchronized (ownerMutex) {
//              try {
//                ownerMutex.wait(100);
//              } catch (InterruptedException e) {
//                e.printStackTrace();
//              }
//            }
//          }
//        }
      }
    }catch (Exception e){
      e.printStackTrace();
      PrintUtilities.logStackTrace(log,e.getStackTrace());
    }
  }

  private void processEvent(EventTriplet triplet) {
//    System.err.println("PROCESSING triplet");
//    log.error("PROCESSING triplet");
    boolean processBuffer = false;
    switch (triplet.getType()) {
      case CREATED:
        processBuffer = buffer.add(triplet.getKey(),triplet.getValue());
        break;
      case MODIFIED:
        processBuffer = buffer.modify(triplet.getKey(),triplet.getValue());
        break;
      case REMOVED:
        processBuffer =buffer.remove(triplet.getKey());
        break;
    }
    if(processBuffer){
//      System.out.println("Processing buffer");
      owner.processBuffer();
      buffer.clear();
    }
  }
}
