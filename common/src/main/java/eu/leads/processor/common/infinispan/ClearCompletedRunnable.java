package eu.leads.processor.common.infinispan;

import org.infinispan.commons.util.concurrent.NotifyingFuture;

import java.util.Iterator;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ExecutionException;

/**
 * Created by vagvaz on 19/05/15.
 */
public class ClearCompletedRunnable extends Thread {


    private final Set<Thread> threads;
    private final Queue<NotifyingFuture<Void>> concurrentQueue;
    private  volatile Object mutex;

    public ClearCompletedRunnable(Queue<NotifyingFuture<Void>> concurrentQuue, Object mutex,
        Set<Thread> threads) {
        this.concurrentQueue = concurrentQuue;
        this.mutex = mutex;
        this.threads = threads;
    }

    @Override public void run() {
        super.run();
        Iterator<NotifyingFuture<Void>> iterator = concurrentQueue.iterator();
        NotifyingFuture current = concurrentQueue.poll();
        while(current != null){
            try {
//                iterator.next().get();
                current.get();
            } catch (InterruptedException e) {
                e.printStackTrace();
            } catch (ExecutionException e) {
                e.printStackTrace();
            }
//            iterator.remove();
//            if(iterator.next().isDone()){
//                iterator.remove();
//            }
            current = concurrentQueue.poll();
        }
//        synchronized (mutex){
//            threads.remove(this);
//        }
    }
}
