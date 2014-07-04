package com.robin.im.timer;

public abstract class TimerWorker {
    public long timeMillis;
    protected int workerType = 0;/*工作者类型 :
                                  * 0-默认工作者
                                  * 1-心跳超时工作者 
                                  * 2-心跳回应超时检查工作者 
                                  * 3-消息确认超时检查工作者      
                                  */
    
    public abstract void onWork();
}
