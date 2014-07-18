package com.robin.im;

/**
 * Created with IntelliJ IDEA.
 * User: liuhouxiang@1986@gmail.com.
 * Date: 2014/7/15 23:10
 * Project: AppServer
 */
public class JoinThread extends Thread {
    public static int n = 0;

    static synchronized void inc()
    {
        n++;
    }
    public void run()
    {
        for (int i = 0; i < 10; i++)
            try
            {
                inc();
                sleep(3);  // 为了使运行结果更随机，延迟3毫秒

            }
            catch (Exception e)
            {
            }
    }
    public static void main(String[] args) throws Exception
    {

        Thread threads[] = new Thread[100];
        for (int i = 0; i < threads.length; i++)  // 建立100个线程
            threads[i] = new JoinThread();
        for (int i = 0; i < threads.length; i++)   // 运行刚才建立的100个线程
            threads[i].start();
//        if (args.length > 0)
            for (int i = 0; i < threads.length; i++)   // 100个线程都执行完后继续
                threads[i].join();
        System.out.println("n=" + JoinThread.n);
    }
}
