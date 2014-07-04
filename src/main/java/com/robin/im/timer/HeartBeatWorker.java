package com.robin.im.timer;

import com.robin.im.netty.connection.MyConnection;
import com.robin.im.util.Constants;
import com.robin.im.util.DateUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HeartBeatWorker extends TimerWorker {

    private static final Logger log            = LoggerFactory.getLogger(HeartBeatWorker.class);
    private static final int    HEART_BEAT_MSG = 0x0b;
    MyConnection myConnection   = null;

    private String              userId;

    public HeartBeatWorker(String userId, MyConnection myConnection, long timeMillis){
        super.workerType = 1;
        super.timeMillis = timeMillis;
        this.userId = userId;
        this.myConnection = myConnection;
    }

    @Override
    public void onWork() {
        long hbTimeout = myConnection.getHBI();
        if (myConnection == null) return;
        else if (myConnection.isValid()) {
            if (hbTimeout == Constants.HEARTBEAT_INTERVAL || myConnection.getHbTimeoutCount() >= 3) {
                myConnection.write(HEART_BEAT_MSG);
            }
            myConnection.write((char) HEART_BEAT_MSG + "" + (char) (hbTimeout / 10000));
            myConnection.setWaitHbAck(true);
        }

        // 在Timer中放置一个心跳回应超时的定时任务
        Timer.addTimer(new HeartBeatTimeoutWorker(userId, myConnection, DateUtil.getCacheNow() + hbTimeout));

    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public void setMyConnection(MyConnection myConnection) {
        this.myConnection = myConnection;
    }

    public MyConnection getMyConnection() {
        return myConnection;
    }

}
