package com.robin.im.send;

import com.alibaba.fastjson.JSONObject;
import com.robin.im.AppServerBeanFactory;
import com.robin.im.listener.RpidACKListener;
import com.robin.im.netty.connection.MyConnection;
import com.robin.im.netty.connection.MyConnectionListener;
import com.robin.im.netty.message.MessageManager;
import com.robin.im.netty.message.MessagePack;
import com.robin.im.redis.service.RedisDAO;
import com.robin.im.util.Constants;
import com.robin.im.util.DateUtil;
import com.robin.im.util.SjsonUtil;
import org.apache.commons.lang.StringUtils;
import org.jboss.netty.channel.ChannelFuture;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.Tuple;

import java.util.Set;

/**
 * 2013-4-10
 */
public class ChatMsg extends MessagePack {

    private static final Logger log                = LoggerFactory.getLogger(ChatMsg.class);

    private static final String MESSAGE_NAME       = "ChatMsg";

    private static final int    DEFAULT_MSG        = -1;
    public static final int     TEXT_MSG           = 0;
    public static final int     IMG_MSG            = 1;
    public static final int     VOICE_MSG          = 2;

    public static final String  IMG_MSG_NOTICE     = "您有一条图片信息";
    public static final String  VOICE_MSG_NOTICE   = "您有一段语音信息";
    public static final String  DEFAULT_MSG_NOTICE = "您有一条新信息";
    public static final String  SYS_NOTIFY_NOTICE  = "您有一条系统通知";
    public static final String  SAME_FLIGHT_NOTICE = "您有一条同航班通知";
    public static final String  SAME_TRAIN_NOTICE  = "您有一条同列车通知";

    private RedisDAO            redisDao;

    private Long                srcUid             = 0L;

    private Long                desUid             = 0L;

    private String              msgId;

    private JSONObject writeJson ;


    public ChatMsg(JSONObject msg, String msgId){
        super(msg.toJSONString());
        writeJson =msg;
        this.msgId = msgId;
    }

    public void onHandler() {
        try {
            redisDao = AppServerBeanFactory.getRedisDAO();
            Integer FID = writeJson.getIntValue("FID");
            if(FID== Constants.NEW_MSG_CODE){
                writeJson.put("FID", Constants.CHAT_MSG_CODE);
            }
            
            JSONObject dataObject = writeJson.getJSONObject("Data");

            if (msgId == null) {
                msgId = dataObject.getString("MsgId");
            }
            dataObject.put("MsgId", msgId);
            DateUtil.freshCacheNow();
            dataObject.put("serverSendTime", DateUtil.getCacheNow());
            // 已完成处理的消息，直接发送
            srcUid = dataObject.getLong("SrcUid");
            desUid = dataObject.getLong("DesUid");
            uid = desUid.toString();
            Set<Tuple> redisResult = redisDao.getOfflineMsgIds(uid);
            for (Tuple t : redisResult) {
                String offMsgId = t.getElement();
                if(StringUtils.isNotBlank(msgId) && msgId.equalsIgnoreCase(offMsgId)){
                    Long timemillis = new Double(t.getScore()).longValue();
                    dataObject.put("serverRecieveTime", timemillis);
                    break;
                }
            }

            MyConnection destConnection = MyConnectionListener.getMyConnectionByName(String.valueOf(desUid));

            if (destConnection != null && destConnection.isValid()) {// 对方在线

                try {
                    ChannelFuture writeFuture = null;
                    if (writeJson != null) {
                        writeJson.remove("TMS");
                        writeJson.remove("PID");
                        if (rpid <= 0) {
                            rpid = Long.parseLong(msgId);
                        }
                        //rpid使用msgId作为响应编号，用于接收客户端发送接收响应
                        writeFuture = destConnection.write(SjsonUtil.addSendSequece(writeJson.toJSONString(), rpid));
                        RpidACKListener rpidACKListener = new RpidACKListener(this);
                        writeFuture.addListener(rpidACKListener);
                    }

                } catch (Exception e) {
                    log.error("send exception， e:" + e);
                }

            }
        } catch (Exception e) {
            // 去除有问题的消息
            if (uid != null && msgId != null) {
                redisDao.removeOfflineMsg(uid, msgId);
                log.error("msg with empty:msgid:" + msg);
            }
        }

    }

    @Override
    public void onWriteSuccess() {
        // 发送成功后，去掉离线消息
        if (log.isDebugEnabled()) {
            log.debug("chatMsg: " + msgId + " send succeed, rm offlineMsg");
        }
        redisDao.removeOfflineMsg(desUid.toString(), msgId);
        redisDao.removeMessageByMsgId(msgId);
    }

    @Override
    public void onWriteFailed() {
        // 发送失败后重发相同的包
        packRetryCount++;
        if (packRetryCount < 3) {
            if (log.isDebugEnabled()) {
                log.debug("ChatMsg: " + msgId + " send failed, retry :" + packRetryCount + "!");
            }
            MessageManager.addRetryMessage(this);
        }
    }

    public String getName() {
        return MESSAGE_NAME;
    }

    public long getRpid() {
        return rpid;
    }

    public void setRpid(long rpid) {
        this.rpid = rpid;
    }

    public Long getDesUid() {
        return desUid;
    }  

    @Override
    public Long getRPID() {
        return rpid;
    }
}
