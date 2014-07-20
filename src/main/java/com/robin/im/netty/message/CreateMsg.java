package com.robin.im.netty.message;

import com.alibaba.fastjson.JSONObject;
import com.robin.im.AppServerBeanFactory;
import com.robin.im.netty.connection.MyConnection;
import com.robin.im.netty.connection.MyConnectionListener;
import com.robin.im.redis.KeyGeneration;
import com.robin.im.redis.RedisClientTemplate;
import com.robin.im.redis.service.RedisDAO;
import com.robin.im.rev.AuthMsg;
import com.robin.im.send.ResponseMsg;
import com.robin.im.util.DateUtil;
import org.apache.commons.lang.StringUtils;
import org.jboss.netty.channel.Channel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 长链接创建消息处理
 * 
 * @author liuhouxiang
 */
public class CreateMsg extends MessagePack {

    private static final Logger log          = LoggerFactory.getLogger(CreateMsg.class);
    private static final Logger msgLog = LoggerFactory.getLogger("monitor");
    private RedisDAO redisDao;
    private RedisClientTemplate redisClientTemplate;

    private int FID = 0;
    private int rc = 1;

    private int type = 0;// 0：文本，1：图片，2：音频
    private Long tms = null;

    private JSONObject resultJson   = new JSONObject();
	private Long desUid = 0L;
    private Long srcUid = 0L;

    private static final String MESSAGE_NAME = "CreateMsg";// 消息名称

    public CreateMsg(String msg, Channel channel){
        super(msg, channel);
    }

    public void onHandler() {

        rc = 1;//1表示失败 0表示成功
        // 首先去根据用户accountId去查询长连接
        MyConnection myConnection = null;
        redisDao = (RedisDAO) AppServerBeanFactory.getRedisDAO();
        redisClientTemplate = (RedisClientTemplate) AppServerBeanFactory.getBean("redisClientTemplate");
        uid = null;
        // 从MyConnectionListener中的connections属性中查询该socketId是否已经存在长连接，如果存在则说明该用户已经在某个客户端登录
        myConnection = MyConnectionListener.getMyConnectionBySocketId(channel.getId());// 根据socketId获取当前用户是否已经在一台机器上登录
        JSONObject data = null;
        if (myConnection != null) {
            if (false == myConnection.isValid()) {// 该长连接已经失效，执行下面的创建新连接
                rc = 2;//需要客户端重新登陆
            } else {// 该长连接没有失效，验证成功
                uid = myConnection.getChName();
                log.info("createMsg receive msg:" + msg);
                try {
                    JSONObject jsonObj = JSONObject.parseObject(msg);
                    FID = jsonObj.getIntValue("FID");
                    tms = jsonObj.getLong("TMS");
                    data = jsonObj.getJSONObject("Data");
                    type = data.getIntValue("Type");
                    desUid = data.getLong("DesUid");
                    srcUid = data.getLong("SrcUid");
               
                    JSONObject resultDataJson = new JSONObject();
                    String msgId = redisDao.getMsgId().toString();
                    data.put("MsgId", msgId);
                    redisDao.saveMessage(msgId, jsonObj.toJSONString());
                    switch (type) {
                        case 0:// 文本消息存在redis里面
                            try {

                                DateUtil.freshCacheNow();
                                redisDao.putOfflineMsg(String.valueOf(desUid), msgId, DateUtil.getCacheNow());
                                // 如果是纯文本消息，则直接丢给redisPubClient去
                                rc = 0;
                            } catch (Exception e) {
                                // 不管有没有错，此处只记录，不会抛出异常阻碍流程
                                log.debug("add text msg to recievedqueue failed, e:" + e);
                            }
                            break;
                        case 1:
                            data.put("MsgId", msgId);
                            redisDao.saveMessage(msgId, jsonObj.toJSONString());
                            rc = 0;
                            break;
                        case 2:
                            data.put("MsgId", msgId);
                            redisDao.saveMessage(msgId, jsonObj.toJSONString());
                            rc = 0;
                            break;
                    }
                    resultJson.put("RC", rc);
                    resultJson.put("TMS", tms);
                    resultDataJson.put("MsgId", msgId);
                    resultJson.put("FID", FID);
                    resultDataJson.put("Type", type);
                    resultJson.put("Data", resultDataJson);
                } catch (Exception e) {
                    log.error("parse msg error", e);
                }
            }
        } else {
            rc = 2;
            msg = "用户还没有登录";
            log.debug("user not login,cann't get connection by socketid");
        }

        try {
            if (0 == rc) {
                log.info("create success,uid=" + uid + ",socketId=" + channel.getId() + ",addr="
                          + channel.getRemoteAddress());
            } else if (1 == rc) {
                log.debug("create failed,msg=" + msg + ",client=" + channel.getRemoteAddress());
            } else if (2 == rc) {
                channel.write(AuthMsg.AUTH_RELOGIN);//客户端重登录
                log.debug("create failed,msg=" + msg + ",client=" + channel.getRemoteAddress());
            } else {
                log.debug("create error,msg=" + msg);
            }
            MessageManager.addSendMessage(new ResponseMsg(resultJson.toJSONString(), uid));

        } catch (Exception e) {
            log.error("create message exception， e:" , e);
        }
    }

    public String getName() {
        return MESSAGE_NAME;
    }

    @Override
    public void onWriteSuccess() {

    }

    @Override
    public void onWriteFailed() {

    }

    @Override
    public Long getRPID() {
        return null;
    }

}
