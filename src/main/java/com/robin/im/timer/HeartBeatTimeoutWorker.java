package com.robin.im.timer;

import com.robin.im.AppServerBeanFactory;
import com.robin.im.netty.connection.MyConnection;
import com.robin.im.redis.KeyGeneration;
import com.robin.im.redis.RedisClientTemplate;
import com.robin.im.util.Constants;
import com.robin.im.util.DateUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HeartBeatTimeoutWorker extends TimerWorker {

    private Logger       log = LoggerFactory.getLogger(HeartBeatTimeoutWorker.class);

    private String       userId;

    private MyConnection myConnection;

    public HeartBeatTimeoutWorker(String userId, MyConnection myConnection, long timeMillis){
        super.workerType = 2;
        this.myConnection = myConnection;
        super.timeMillis = timeMillis;
        this.setUserId(userId);
    }

    @Override
    public void onWork() {
        if (myConnection == null) return;
        else if (myConnection.isValid()) {
            RedisClientTemplate redisClient = (RedisClientTemplate) AppServerBeanFactory.getBean("redisClientTemplate");
            if (!myConnection.isWaitHbAck()) {
                // 心跳回应没有超时
                Long newHBI = myConnection.getHBI();
                if (newHBI < Constants.HEARTBEAT_MAX_INTERVAL) {
                    newHBI = newHBI * 2;
                }
                myConnection.setHBI(newHBI);
                // 刷新sessionId
                String sessionId = myConnection.getSid();
                String userService = redisClient.get(KeyGeneration.sessionKey(sessionId));
                if (userService != null) {
                    redisClient.expire(sessionId, KeyGeneration.SESSION_EXPIRED_TIME);
                }
            } else {
                myConnection.incrHbTimeoutCount();
                int hbTimeoutCount = myConnection.getHbTimeoutCount();
                if (hbTimeoutCount < Constants.TIMEOUT_MAX_COUNT) {
                    // 心跳回应超时，继续默认时间间隔发送心跳
                    if (log.isDebugEnabled()) {
                        log.debug("user: " + myConnection.getChName() + " ACK timeout, HEARTBEAT retry "
                                  + myConnection.getHbTimeoutCount());
                    }
                    myConnection.setHBI(Constants.HEARTBEAT_INTERVAL);
                } else {
                    // 心跳超时次数超过3次，关闭连接
                    if (log.isDebugEnabled()) {
                        log.debug("user: " + myConnection.getChName() + " lost, close connection");
                    }
                    myConnection.close();
                    return;
                }
            }
            Timer.addTimer(new HeartBeatWorker(userId, myConnection, DateUtil.getCacheNow()));
        } else {
            if (log.isDebugEnabled()) {
                log.debug("channel: " + myConnection.getChName() + " has already been closed");
            }
        }
    }

    public MyConnection getMyConnection() {
        return myConnection;
    }

    public void setMyConnection(MyConnection myConnection) {
        this.myConnection = myConnection;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

}
