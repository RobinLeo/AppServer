package com.robin.im.send;

import com.robin.im.AppServerBeanFactory;
import com.robin.im.listener.RpidACKListener;
import com.robin.im.netty.connection.MyConnection;
import com.robin.im.netty.connection.MyConnectionListener;
import com.robin.im.netty.message.MessageManager;
import com.robin.im.netty.message.MessagePack;
import com.robin.im.util.SjsonUtil;
import org.jboss.netty.channel.ChannelFuture;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;



public class ResponseMsg extends MessagePack {
    private Logger log = LoggerFactory.getLogger("ResponseMsg");
    
    public ResponseMsg(String msg, String uid) {
        this.msg = msg;
        this.uid = uid;
    }
    
    @Override
    public void onHandler() {
        MyConnection myConnection = MyConnectionListener.getMyConnectionByName(uid);
        if(myConnection != null && myConnection.isValid()) {
            try {
                if (msg != null) {
                    if(rpid <= 0){
                        rpid = AppServerBeanFactory.getRedisDAO().getMsgId();
                    }
                    //add listener
                    ChannelFuture writeFuture = myConnection.write(SjsonUtil.addSendSequece(msg, rpid));
                    RpidACKListener rpidACKListener = new RpidACKListener(this);
                    writeFuture.addListener(rpidACKListener);
                }
            } catch (Exception e) {
                log.error("send exception， e:" + e);
            }

        } else {
            
        }
    }
    
    @Override
    public String getName() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void onWriteSuccess() {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void onWriteFailed() {
        // 发送失败后重发相同的包
        packRetryCount++;
        if (packRetryCount <= 1) {
            if (log.isDebugEnabled()) {
                log.debug("ChangeProfileMsg, user: " + uid + " send failed, retry :" + packRetryCount + "!");
            }
            MessageManager.addRetryMessage(this);
        }
    }

    @Override
    public Long getRPID() {
        // TODO Auto-generated method stub
        return rpid;
    }

}
