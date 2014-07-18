package com.robin.im;

import com.alibaba.citrus.util.Assert;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created with IntelliJ IDEA.
 * User: liuhouxiang@1986@gmail.com.
 * Date: 2014/7/15 22:21
 * Project: AppServer
 */
public class AtomicIntegerTest {

    public static void main(String[] args) throws Exception{
        final AtomicInteger value = new AtomicInteger(10);
        Assert.assertTrue(value.compareAndSet(10, 9));
        Assert.assertTrue(value.get() == 9);
        Assert.assertTrue(value.compareAndSet(9, 3));
        Assert.assertTrue(value.get() == 3);
        value.set(0);
        Assert.assertTrue(value.incrementAndGet() == 1);
        System.out.println(value.getAndAdd(2));
//        Assert.assertTrue(value.getAndAdd(2) == 1);
        System.out.println(value.get());
        Assert.assertTrue(value.getAndSet(5) == 3);
        Assert.assertTrue(value.get() == 5);
        final int threadSize = 10;
        Thread[] ts = new Thread[threadSize];
        for (int i = 0; i < threadSize; i++) {
            ts[i] = new Thread() {
                public void run() {
                    value.incrementAndGet();
                }
            };
        }
        for(Thread t:ts) {
            t.start();
        }
        for(Thread t:ts) {
            t.join();
        }
        Assert.assertTrue(value.get() == (5 + threadSize));
    }
}
