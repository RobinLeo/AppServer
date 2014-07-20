package com.robin.im.timer;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class TimerWorkerManager {
    private static Logger log = LoggerFactory.getLogger(TimerWorkerManager.class);
    ExecutorService pool = null;
    private CountDownLatch latch = new CountDownLatch(1);
    private LinkedBlockingQueue<TimerWorker> timerWorkerQueue = new LinkedBlockingQueue<TimerWorker>();
    private int startedThreadCounter = 0;
    
    public void addWorker(TimerWorker worker){
        boolean success = timerWorkerQueue.add(worker);
        if(success){
            if(startedThreadCounter < 10){//最多创建10个心跳工作线程
                pool.submit(new WorkingThread(latch));
                startedThreadCounter++;
            } else {
                log.info("working thread full");
            }
        }
    }
    
    public TimerWorker getWorker(){
        TimerWorker worker = null;
        try {
            worker = timerWorkerQueue.poll(10, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            log.warn("get timerWorker error: " + e);
        }
        return worker;
    }
    
    public void init(){
        log.info("TimerWorkerManager START");
        pool = Executors.newCachedThreadPool();//对于执行很多短期异步任务的程序而言，这些线程池通常可提高程序性能
        pool.submit(new WorkingThread(latch));
        latch.countDown();
    }
    
    private class WorkingThread implements Runnable{
        private final CountDownLatch latch;
        
        
        public WorkingThread(CountDownLatch latch) {
            this.latch = latch;
        }
        
        @Override
        public void run() {
            TimerWorker worker = null;
            while(true){
                try {
                    worker = getWorker();
                    if(worker != null){
                        worker.onWork();
                    }
                } catch (Exception e) {
                    log.error("worker on work error: " + e);
                }
            }
        }
    }
    
}
