package eu.leads.processor.common.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.channels.OverlappingFileLockException;

/**
 * Created by vagvaz on 6/7/14.
 */
public class FileLockWrapper {
    File file;
    FileLock lock;
    FileChannel channel;
    Logger log = LoggerFactory.getLogger(FileLockWrapper.class);

    public FileLockWrapper(String filename) {
        file = new File(filename + ".lock");
        if (!file.getParentFile().mkdirs()) {
            log.error("Could not create the directories for the lock of " + filename + ".lock");
        }
        try {
            channel = new RandomAccessFile(file, "rw").getChannel();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    public synchronized void tryLock() {
        try {
            lock = channel.tryLock();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    public synchronized void lock() {
        try {
            int counter = 1;
            while (lock == null || !lock.acquiredBy().equals(channel)) {
                try {
                    lock = channel.lock(0, Long.MAX_VALUE, false);
                } catch (OverlappingFileLockException e) {
                    log.info("Lock File Found " + file.getCanonicalPath());
                    try {
                        Thread.sleep(counter * 10);
                        counter++;
                    } catch (InterruptedException e1) {
                        e1.printStackTrace();
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    public synchronized void release() {
        try {
            lock.release();
            channel.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


}
