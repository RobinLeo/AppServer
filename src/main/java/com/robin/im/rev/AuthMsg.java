package com.robin.im.rev;

/**
 * Created with IntelliJ IDEA.
 * User: liuhouxiang@1986@gmail.com.
 * Date: 2014/7/3 21:00
 * Project: AppServer
 */

import com.alibaba.fastjson.JSONObject;
import com.robin.im.AppServerBeanFactory;
import com.robin.im.netty.connection.MyConnection;
import com.robin.im.netty.connection.MyConnectionListener;
import com.robin.im.netty.message.MessageManager;
import com.robin.im.netty.message.MessagePack;
import com.robin.im.redis.service.RedisDAO;
import com.robin.im.send.ChatMsg;
import com.robin.im.timer.HeartBeatWorker;
import com.robin.im.timer.Timer;
import com.robin.im.util.DateUtil;
import com.robin.im.util.SjsonUtil;
import org.apache.commons.lang.StringUtils;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.ChannelFutureListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.Tuple;

import java.net.SocketAddress;
import java.util.Set;

/**
 * 长连接鉴权处理
 *
 * @author liuhouxiang
 * Description: 授权消息，每个新建的channel连接都必须先通过此消息授权， 授权成功后connection.isValid = true，才能进行具体的业务消息的收发。
 */
public class AuthMsg extends MessagePack {

    private static final Logger log               = LoggerFactory.getLogger(AuthMsg.class);

    private static final String AUTH_SUCCESS      = "{\"FID\":1545,\"RC\":0,\"HBI\":10}";

    public  static final String AUTH_FAILED       = "{\"FID\":1545,\"RC\":1}";

    private static final String AUTH_UNKNOW_ERROR = "{\"FID\":1545,\"RC\":-1}";

    public static final String  RELOGIN_OTHER_IP  = "{\"FID\":1546,\"DES\":\"relogin\"}";

    private static final String MESSAGE_NAME      = "AuthMsg";                             // 消息名称

    RedisDAO redisDao;

    private int                 rc                = 1;

    // private String msr = null;//user=长连接,business=业务逻辑,event=事件处理,file=文件处理，null的话默认是长连接

    public AuthMsg(String msg, Channel channel){
        super(msg, channel);
    }

    @Override
    public void onHandler() {

        MyConnection myConnection = null;
        rc = 1;
        uid = null;
        // 从MyConnectionListener中的connections属性中查询该socketId是否已经存在长连接，如果存在则说明该用户已经在某个客户端登录
        myConnection = MyConnectionListener.getMyConnectionBySocketId(channel.getId());// 根据socketId获取当前用户是否已经在一台机器上登录
        if (myConnection != null) {
            uid = myConnection.getChName();
        }
        if (myConnection != null) {
            //客户端鉴权时需要将sid放到msg中
            String sid = SjsonUtil.getSIDFromMsg(msg);// 获取sessionId
            if (StringUtils.isNotBlank(sid)) {
                redisDao = AppServerBeanFactory.getRedisDAO();
                // 根据sid去redis里查询该sid是否有效
                String userInfo = redisDao.getUserInfoBySessionId(sid);
                // {"S_K:5613435423564354":"{\"id\":20130401,\"email\":\"liuhouxiang1986@163.com\",\"password\":\"123456\",\"status\":1}"}
                if (userInfo != null) {
                    JSONObject userJson = JSONObject.parseObject(userInfo);
                    uid = userJson.getLong("id").toString();

                    if (StringUtils.isNotBlank(uid)) {
                        // 把connection 设置成有效
                        myConnection.setValid(true);
                        myConnection.setChName(uid);
                        // oldConnection不为空说明同一个用户多处登录，将前一个用户踢下线。
                        MyConnection oldConnection = MyConnectionListener.addNamedConnection(myConnection);

                        if (oldConnection != null) {
                            boolean isSameUser = myConnection.getChannel().getId().equals(oldConnection.getChannel().getId());

                            if(!isSameUser){
                                if (log.isDebugEnabled()) {
                                    log.debug(uid + " login in other IP.old socked id=" + oldConnection.getID()
                                            + ",now socket id=" + channel.getId());
                                }
                                MyConnectionListener.connectionDestroyed(oldConnection.getID());
                                oldConnection.setReplaced(true);
                                if(oldConnection.getChannel() != null ){
                                    Channel channel1 = oldConnection.getChannel();
                                    SocketAddress ip = channel1.getRemoteAddress();
                                    Channel channel2 = myConnection.getChannel();
                                    SocketAddress ip2 = channel2.getRemoteAddress();
                                    if(!ip.toString().equals(ip2.toString())){
                                        oldConnection.write(RELOGIN_OTHER_IP);
                                    }
                                }
                            }else{
                                log.warn("same user auth, uid=" + uid);
                            }

                        } else {
                            // 此时已经将当前长连接设置为有效，不用重复设置
                        }
                        rc = 0;

                    } else {
                        if (log.isDebugEnabled()) {
                            log.debug("get userinfo failed， sid=" + sid + ",client=" + myConnection.getRemoteAddress());
                        }
                    }
                } else {
                       log.info("sid invalid, sid=" + sid);
                }
            }
        } else if (0 != rc) {
            rc = -1;
        }

        try {
            ChannelFuture writeFuture = null;
            if (!channel.isConnected()) {
                // 验证过程中网络已经断开
                if (0 == rc && myConnection != null) {
                    myConnection.setValid(false);
                }
                log.info("network broken when send result， socketId=" + channel.getId());
            }

            if (0 == rc) {
                writeFuture = channel.write(AUTH_SUCCESS);
                //鉴权成功，开始心跳线程用来维持长链接
                Timer.addTimer(new HeartBeatWorker(uid, myConnection, DateUtil.getCacheNow()));

                // 验证成功了，需要推送离线消息
                pushOfflineMsg(uid);
                log.info("auth success,ph=" + uid + ",socketId=" + channel.getId() + ",addr=" + channel.getRemoteAddress());
            } else if (1 == rc) {
                writeFuture = channel.write(AUTH_FAILED);
                log.info("auth failed,msg=" + msg + ",client=" + channel.getRemoteAddress());
            } else {
                writeFuture = channel.write(AUTH_UNKNOW_ERROR);
                log.info("auth error,msg=" + msg);
            }
            writeFuture.addListener(new ChannelFutureListener() {

                public void operationComplete(ChannelFuture future) throws Exception {
                    if (!future.isSuccess()) {
                        channel.close();
                        log.debug("operate channel failed,channel=" + future.getChannel() + ",cause=" + future.getCause());
                    } else {
                        if (0 != rc) {
                            channel.close();
                        }
                    }
                }
            });
        } catch (Exception e) {
            log.error("auth exception， e:" + e);
        }
    }

    /**
     * 客户端上线了，需要推送离线消息
     */
    private void pushOfflineMsg(String uid) {

        Set<Tuple> msgSet = redisDao.getOfflineMsgIds(uid);


        for (Tuple t : msgSet) {
            String msgId = t.getElement();
            String content = redisDao.getMessageByMsgId(msgId);
            if (content != null) {
                try {
                    MessageManager.addSendMessage(new ChatMsg(JSONObject.parseObject(content), msgId));
                } catch (Exception e) {
                    log.error("push offline msg error" + e.getCause() + "," + content);
                }
            } else {
                redisDao.removeOfflineMsg(uid, msgId);
                redisDao.removeMessageByMsgId(msgId);
                if (log.isDebugEnabled()) {
                    log.debug("msgId: " + msgId + " content is null");
                }
            }
        }
    }

    public void onWriteSuccess() {
    }

    public void onWriteFailed() {
    }

    public String getName() {
        return MESSAGE_NAME;
    }

    public boolean isWriteTimeOut(long now) {
        return false;
    }

    public Long getRPID() {
        return null;
    }

}
