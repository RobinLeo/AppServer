package com.robin.im.netty.message;

/**
 * Created with IntelliJ IDEA.
 * User: liuhouxiang@1986@gmail.com.
 * Date: 2014/7/3 13:57
 * Project: AppServer
 */

import com.robin.im.redis.service.RedisDAO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.*;

/**
 * 消息处理管理类
 */
public class MessageManager {
    private static final Logger log = LoggerFactory.getLogger(MessageManager.class);

    private static final int RECEIVED_QUEUE_FLAG = 1;//消息接收标记
    private static final int SEND_QUEUE_FLAG = 2;//消息发送标记
    private static final int RETRY_QUEUE_FLAG = 3;//重发消息标记

    //tips：LinkedBlockingQueue实现是线程安全的，实现了先进先出等特性，是作为生产者消费者的首选，另一个常用的是非阻塞队列ConcurrentLinkedQueue
    //LinkedBlockingQueue 发送消息队列（有消息就发送，无消息阻塞） ConcurrentLinkedQueue 读取消息队列（无消息break）
    private static LinkedBlockingQueue<MessagePack> receivedQueue = new LinkedBlockingQueue<MessagePack>(512);//消息接收队列
    private static LinkedBlockingQueue<MessagePack> sendQueue = new LinkedBlockingQueue<MessagePack>(512);//消息发送队列
    private static LinkedBlockingQueue<MessagePack> retryQueue = new LinkedBlockingQueue<MessagePack>(256);//消息充实队列

    private static ExecutorService pool;//线程池

    private static final CountDownLatch latch = new CountDownLatch(1);//同步辅助类,计数器
    //tips:ConcurrentHashMap允许多个修改操作并发进行，其关键在于使用了锁分离技术。它使用了多个锁来控制对hash表的不同部分进行的修改。
    private static final ConcurrentHashMap<Long,MessagePack> wait4AckMsgs = new ConcurrentHashMap<Long, MessagePack>();

    private static int reStartThreadCount = 0;//用于统计线程重启次数，最多重启10次

    public static RedisDAO redisDAO;

    /**
     * 服务启动入口
     */
    public static void start() {
        ThreadFactory factory = new ThreadFactory() {
            @Override
            public Thread newThread(Runnable r) {
                Thread thread = Executors.defaultThreadFactory().newThread(r);
                thread.setName("MesageWorker:" + latch.getCount());
                thread.setDaemon(true);
                return thread;
            }
        };

        pool = Executors.newCachedThreadPool(factory);

        pool.submit(new MsgReceiveThread(latch));//处理接收到的消息的线程
        pool.submit(new MsgSendThread(latch));//处理发送消息的线程
        pool.submit(new MsgRetryThread(latch));//处理重发消息的线程
        latch.countDown();
    }

    /**
     * 终止服务
     */
    public static void shutDown(){
        pool.shutdown();
    }


    //***********处理消息的子线程**********

    private static class MsgReceiveThread implements Runnable{
        private final CountDownLatch latch;

        MsgReceiveThread(CountDownLatch latch){
            this.latch = latch;
        }

        @Override
        public void run() {
            try {
                latch.await();
                while (true){
                    MessagePack messagePack = messagesToHandle(RECEIVED_QUEUE_FLAG);
                    if(messagePack != null){
                        messagePack.onHandler();
                    }
                }
            } catch (InterruptedException e) {
                log.warn("MsgReceiveThread error:",e);
            }
        }


    }

    private static class MsgSendThread implements Runnable {
        private final CountDownLatch latch;
        MsgSendThread(CountDownLatch latch){
            this.latch = latch;
        }

        @Override
        public void run() {
            try {
                latch.await();
                while (true){
                    MessagePack messagePack = messagesToHandle(SEND_QUEUE_FLAG);
                    if(messagePack != null){
                        messagePack.onHandler();
                    }
                }
            } catch (InterruptedException e) {
                log.warn("MsgSendThread error:",e);
            }
        }
    }

    private static class MsgRetryThread implements Runnable {
        private final CountDownLatch latch;
        MsgRetryThread(CountDownLatch latch){
            this.latch = latch;
        }

        @Override
        public void run() {
            try {
                latch.await();
                while (true){
                    MessagePack messagePack = messagesToHandle(RETRY_QUEUE_FLAG);
                    if(messagePack != null){
                        messagePack.onHandler();
                    }
                }
            } catch (InterruptedException e) {
                log.warn("MsgRetryThread error:",e);
            }
        }
    }
    //**********处理消息子线程结束***********

    /**
     * 从相应的消息队列中获取需要处理的消息
     * @param queueFlag
     * @return
     */
    public static MessagePack messagesToHandle(int queueFlag) {
        MessagePack messagePack = null;
        while (messagePack == null){
            try {
                switch (queueFlag) {
                    case RECEIVED_QUEUE_FLAG:
                        messagePack = receivedQueue.poll(10, TimeUnit.SECONDS);//立刻或者等待指定时间后,获取并且移除队列的头
                        break;
                    case SEND_QUEUE_FLAG:
                        messagePack = sendQueue.poll(10, TimeUnit.SECONDS);
                        break;
                    case RETRY_QUEUE_FLAG:
                        messagePack = retryQueue.poll(20, TimeUnit.SECONDS);
                }
            }catch (InterruptedException e){
                log.error("poll message from queue failed,",e);
            }
        }
        return messagePack;
    }

    /**
     * 删除客户端应答消息
     * @param key
     */
    public static void removeWaitAckMsg(Long key) {
        if (key != null) {
            MessagePack msg = wait4AckMsgs.remove(key);
            if (msg != null) {
                msg.onWriteSuccess();
            } else {
                log.warn("msg is null, rpid = " + key);
            }
        }
    }
    //将接收到的消息放入队列
    public static void addReceivedMessage(MessagePack message) {
        if (message != null) {
            try {
                boolean success = receivedQueue.offer(message, 5, TimeUnit.SECONDS);
                if (false == success) {
                    log.error("insert into receivedQueen failed,msg=" + message.getMsg());
                    // maybe PushRecvThread is break,restart the thread again
                    if (reStartThreadCount < 10) {
                        pool.submit(new MsgReceiveThread(latch));
                        reStartThreadCount++;
                        log.debug("reStartThreadCount=" + reStartThreadCount);
                    }
                }
            } catch (InterruptedException e) {
                log.error("error call receivedQueen offer,ERROR:", e);
            }
        }
        return;
    }

    /**
     * 将发送的消息放入队列
     * @param message
     */
    public static void addSendMessage(MessagePack message) {

        if (message != null) {

            try {
                boolean success = sendQueue.offer(message, 5, TimeUnit.SECONDS);
                if (false == success) {
                    log.error("insert into sendQueen failed,msg=" + message.getMsg());
                    // maybe PushRecvThread is break,restart the thread again
                    if (reStartThreadCount < 10) {
                        pool.submit(new MsgSendThread(latch));
                        reStartThreadCount++;
                        log.debug("reStartThreadCount=" + reStartThreadCount);
                    }
                }
            } catch (InterruptedException e) {
                log.error("error call sendQueen offer,ERROR:", e);
            }
        }
        return;
    }

    /**
     * 将重发消息放入队列
     * @param message
     */
    public static void addRetryMessage(MessagePack message) {
        if (message != null) {
            try {
                boolean success = retryQueue.offer(message, 5, TimeUnit.SECONDS);
                if (false == success) {
                    log.error("insert into retryQueen failed,msg=" + message.getMsg());
                    // maybe PushRecvThread is break,restart the thread again
                    if (reStartThreadCount < 10) {
                        pool.submit(new MsgRetryThread(latch));
                        reStartThreadCount++;
                        log.debug("reStartThreadCount=" + reStartThreadCount);
                    }
                }
            } catch (InterruptedException e) {
                log.error("error call retryQueen offer,ERROR:", e);
            }
        }
        return;
    }

    /**
     * 将消息放入等待应答map
     * @param msg
     */
    public static void addWaitAckMsg(MessagePack msg) {
        if (msg != null) {
            wait4AckMsgs.putIfAbsent(msg.getRPID(), msg);
        }
    }

    /**
     * 检查消息确认是否有超时
     */
    public static void checkAckMsgTimeOut(Long rpid) {

        MessagePack msg = wait4AckMsgs.remove(rpid);
        if (msg != null) {
            if (log.isDebugEnabled()) {
                log.debug("ACK timeout rpid=" + msg.getRPID());
            }
            msg.onWriteFailed();
        }
    }
}
