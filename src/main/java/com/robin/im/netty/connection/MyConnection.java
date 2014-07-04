package com.robin.im.netty.connection;

import com.robin.im.util.Constants;
import com.robin.im.util.DateUtil;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelFuture;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MyConnection {

    // Constants -----------------------------------------------------

    private static final Logger log = LoggerFactory.getLogger(MyConnection.class);

    // Attributes ----------------------------------------------------

    private final Channel channel;

    private boolean waitHbAck;// 是否正在等待心跳回应，如果true，表示客户端还没有回应HB

    private int hbTimeoutCount; // 心跳超时计数

    private long HBI = Constants.HEARTBEAT_INTERVAL; // 心跳间隔时间

    private boolean closed;

    // 是否是其它地址上线，导致该连接被踢下的。是的话，不要向事件服务器发送下线消息
    private boolean isReplaced = false;

    private String chName = null;

    private boolean valid = false;

    // 开始时间
    private long bornTime = 0;

    private long lastFreshTime = 0;

    private String sid;

    // 2秒内收到的包数
    private int msgPackCount = 0;
    

    // private final AtomicBoolean writeLock = new AtomicBoolean(false);

    public MyConnection(final Channel channel) {
        this.channel = channel;
        lastFreshTime = DateUtil.getCacheNow();
        bornTime = lastFreshTime;
    }

    public synchronized void close() {
        if (closed) {
            return;
        }

        ChannelFuture closeFuture = channel.close();

        if (!closeFuture.awaitUninterruptibly(10000)) {
            log.warn("Timed out waiting for channel to close");
        }
        closed = true;
    }

    public int getID() {
        return channel.getId();
    }

    public Channel getChannel() {
        return channel;
    }

    public String getRemoteAddress() {
        return channel.getRemoteAddress().toString();
    }

    public String getChName() {
        return chName;
    }

    public void setChName(String chName) {
        this.chName = chName;
    }

    public boolean isValid() {
        return valid;
    }

    public void setValid(boolean valid) {
        synchronized (this){
            this.valid = valid;
        }
    }

    public String toString() {
        return super.toString() + "[local= " + channel.getLocalAddress() + ", remote=" + channel.getRemoteAddress()
                + "]";
    }

    public ChannelFuture write(Object msg) {
        if (log.isDebugEnabled()) {
            log.debug("chName=" + chName + "(" + msgPackCount + ")==>" + msg);
        }
        return channel.write(msg);
    }

    public long getLastFreshTime() {
        return lastFreshTime;
    }

    public void reFreshTime() {
        lastFreshTime = DateUtil.getCacheNow();
    }

    public String getSid() {
        return sid;
    }

    public void setSid(String sid) {
        this.sid = sid;
    }

    public boolean isClosed() {
        return closed;
    }

    /**
     * 是否是其它地址上线，导致该连接被踢下的。是的话，不要向事件服务器发送下线消息
     *
     * @return
     */
    public boolean isReplaced() {
        return isReplaced;
    }

    public void setReplaced(boolean isReplaced) {
        this.isReplaced = isReplaced;
    }

    public int incPackCount() {
        return msgPackCount++;
    }

    public int getPackCount() {
        return msgPackCount;
    }

    public void clearPackCount() {
        msgPackCount = 0;
    }

    public long getBornTime() {
        return bornTime;
    }

    /**
     *
     * @return true if born within 60 seconds, else return false
     */
    public boolean isYoung() {
        return ((DateUtil.getCacheNow() - bornTime) < 60000);
    }

    public boolean isWaitHbAck() {
        return waitHbAck;
    }

    public void setWaitHbAck(boolean waitHbAck) {
        this.waitHbAck = waitHbAck;
    }

    public long getHBI() {
        return HBI;
    }

    public void setHBI(long hBI) {
        HBI = hBI;
    }

    public int getHbTimeoutCount() {
        return hbTimeoutCount;
    }

    public void incrHbTimeoutCount() {
        this.hbTimeoutCount++;
    }

}
