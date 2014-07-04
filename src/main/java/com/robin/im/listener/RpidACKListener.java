package com.robin.im.listener;

import com.robin.im.netty.message.MessageManager;
import com.robin.im.netty.message.MessagePack;
import com.robin.im.timer.ACKTimeoutWorker;
import com.robin.im.timer.Timer;
import com.robin.im.util.Constants;
import com.robin.im.util.DateUtil;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.ChannelFutureListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RpidACKListener implements ChannelFutureListener {

    private Logger log = LoggerFactory.getLogger("RpidACKListener");

    private MessagePack msg;
    
    
    public RpidACKListener(MessagePack msg) {
        this.msg = msg;
    }
    
    @Override
    public void operationComplete(ChannelFuture future) throws Exception {
        if (future != null) {
            Channel channel = future.getChannel();
            if (!future.isSuccess()) {
                channel.close();
                if (log.isDebugEnabled()) {
                    log.debug("send failed,channel=" + channel + ",cause=" + future.getCause());
                }
            } else {
                // 是需要回复确认信令的消息，加入等待确认队列中
               
                    MessageManager.addWaitAckMsg(msg);
                    // 消息发送成功，设置确认消息超时的定时器
                    Timer.addTimer(new ACKTimeoutWorker(DateUtil.getCacheNow() + Constants.PACK_ACK_TIMEOUT, msg
                            .getRPID()));
                
            }
        }
    }

    public MessagePack getMsg() {
        return msg;
    }

    public void setMsg(MessagePack msg) {
        this.msg = msg;
    }

}
