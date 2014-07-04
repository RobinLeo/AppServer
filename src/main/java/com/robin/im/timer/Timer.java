package com.robin.im.timer;

import java.util.Comparator;
import java.util.NavigableSet;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.atomic.AtomicInteger;

import com.robin.im.util.DateUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * 定时工作类
 *
 */
public class Timer {
    @Autowired
    private TimerWorkerManager timerWorkerManager;
    private static Logger log = LoggerFactory.getLogger(Timer.class);
    private static AtomicInteger add = new AtomicInteger(0);
    private static ConcurrentSkipListSet<TimerWorker> timerSet = new ConcurrentSkipListSet<TimerWorker>(new TimerWorkerCompare());
    
    /**
     * 
     * @param worker
     */
    public static void addTimer(TimerWorker worker) {
        boolean addRes = false;
        do{
            addRes = timerSet.add(worker);
            if(!addRes){
                worker.timeMillis = worker.timeMillis + add.incrementAndGet();
            }
        }while(!addRes);
    }

    public void work() {
        DateUtil.freshCacheNow();//刷新缓存时间
        add.set(0);//重置增量
        Long now = DateUtil.getCacheNow();
        NavigableSet<TimerWorker> timeoutSet = timerSet.headSet(new DefaultTimerWorker(now));
        if(log.isTraceEnabled()){
            int size = timeoutSet.size();
            if(size > 0) log.trace("get TimerWorker :" + size);
        }
        while(!timeoutSet.isEmpty()) {
            TimerWorker worker = timeoutSet.pollFirst();
            timerWorkerManager.addWorker(worker);
        }
    }    
    
    
    public static void main(String[] args) {
        Timer timer = new Timer();
        long start = System.nanoTime();
        for(int i = 0; i<1000; i++){
            Timer.addTimer(new DefaultTimerWorker(1234L));
        }
        long end = System.nanoTime();
        System.out.println((end-start)*0.000000001);
        timer.work();
        System.out.println();
    }
}

class TimerWorkerCompare implements Comparator<TimerWorker> {

    @Override
    public int compare(TimerWorker worker1, TimerWorker worker2) {
        int i = 0;
        if(worker1.timeMillis - worker2.timeMillis > 0){
            i = 1;
        } else if(worker1.timeMillis - worker2.timeMillis == 0){
            i = 0;
        } else {
            i = -1;
        }
        return i;
    }
    
}
