package com.robin.im.timer;


import com.robin.im.netty.message.MessageManager;

/**
 * 应答超时时间任务
 */
public class ACKTimeoutWorker extends TimerWorker {

    private long rpid;


    public ACKTimeoutWorker(long timeMillis, long rpid) {
        super.timeMillis = timeMillis;
        this.rpid = rpid;
    }

    @Override
    public void onWork() {
        MessageManager.checkAckMsgTimeOut(rpid);
    }

    
    public long getRpid() {
        return rpid;
    }
    
    
    public void setRpid(long rpid) {
        this.rpid = rpid;
    }
}
