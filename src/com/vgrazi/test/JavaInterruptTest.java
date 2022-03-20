package com.vgrazi.test;

import org.junit.BeforeClass;
import org.junit.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;

public class JavaInterruptTest{

    @BeforeClass
    public static void setLoggingProperties(){
        System.setProperty("java.util.logging.SimpleFormatter.format", "%1$tY-%1$tm-%1$td %1$tH:%1$tM:%1$tS %5$s%6$s%n");
    }
    @Test
    public void testInterruptReceivedOnThreadWaitingOnSynchronizedLock() throws InterruptedException{
        // Thread 1 - grab a lock and wait.
        // Thread 2 - grab the lock and spin
        // main thread - interrupt thread 1
        // main thread - wait 50 MS and check interrupt flag
        Logger logger = Logger.getLogger("Synchronized");
        CountDownLatch latch = new CountDownLatch(1);
        Object mutex = new Object();
        Thread thread1 = new Thread(()->{
            synchronized(mutex){
                try{
                    logger.info("First thread Waiting");
                    mutex.wait();
                }catch(InterruptedException e){
                    Thread.currentThread().interrupt();
                    logger.log(Level.WARNING, e, () -> "First thread caught InterruptedException.  re-raised interrupt flag");
                    boolean interrupted = Thread.currentThread().isInterrupted();
                    logger.info("First thread Interrupted:" + interrupted);
                }finally{
                    logger.info("First thread Unlocking");
                }
            }});
        thread1.start();
        Thread.sleep(50);
        Thread thread2 = new Thread(() -> {
            synchronized(mutex){
                try{
                    logger.info("Second thread grabbing lock");
                    latch.await();
                }catch(InterruptedException e){
                    logger.info("Should never happen");
                }finally{
                    logger.info("Second thread Unlocking");
                }
            }
        });
        thread2.start();
        Thread.sleep(50);
        thread1.interrupt();
        logger.info("Interrupted first thread");
        Thread.sleep(50);
        boolean interrupted;
        interrupted = thread1.isInterrupted();
        logger.info("First thread Interrupted:" + interrupted);
        // as expected the thread is interrupted
        logger.info("Releasing lock on second thread");
        latch.countDown();
        Thread.sleep(50);
    }

    @Test
    public void testInterruptReceivedOnThreadWaitingOnReenrantLock() throws InterruptedException{
        // Thread 1 - grab a lock, create a condition, and await that condition.
        // Thread 2 - grab the lock and spin
        // main thread - interrupt thread 1
        // main thread - wait 50 MS and check interrupt flag
        Logger logger = Logger.getLogger("ReentrantLock");

        CountDownLatch latch = new CountDownLatch(1);
        ReentrantLock lock = new ReentrantLock();
        Thread thread1 = new Thread(()->{
            lock.lock();
            Condition condition = lock.newCondition();
            try{
                logger.info("First thread Waiting");
                condition.await();
                boolean interrupted = Thread.currentThread().isInterrupted();
                // huh? _Now_ the thread is interrupted??
                logger.info("First thread Interrupted:.? " + interrupted);
            }catch(InterruptedException e){
                Thread.currentThread().interrupt();
                logger.log(Level.WARNING, e, () -> "First thread caught InterruptedException.  re-raised interrupt flag");
                boolean interrupted = Thread.currentThread().isInterrupted();
                logger.info("First thread Interrupted:.." + interrupted);
            }finally{
                logger.info("First thread Unlocking");
                lock.unlock();
            }
        });
        thread1.start();
        Thread.sleep(50);
        Thread thread2 = new Thread(()->{
        lock.lock();
            try{
                logger.info("Second thread grabbing lock");
                latch.await();
            }catch(InterruptedException e){
                logger.info("Should never happen");
            }
            finally{
                logger.info("Second thread Unlocking");
                lock.unlock();

            }
        });
        thread2.start();
        Thread.sleep(50);
        thread1.interrupt();
        logger.info("Interrupted first thread");
        Thread.sleep(50);
        boolean interrupted;
        interrupted = thread1.isInterrupted();
        logger.info("First thread Interrupted:" + interrupted + ". HUH????");
        // huh? The thread was interrupted but the interrupt flag is not set
        logger.info("Releasing lock on second thread");
        latch.countDown();
        Thread.sleep(50);
    }

}
