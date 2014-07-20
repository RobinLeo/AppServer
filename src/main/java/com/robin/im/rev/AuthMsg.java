package com.robin.im.rev;

/**
 * Created with IntelliJ IDEA.
 * User: liuhouxiang1986@gmail.com.
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

    public static final String  AUTH_RELOGIN  = "{\"FID\":1546,\"DES\":\"relogin\"}";

    private static final String MESSAGE_NAME      = "AuthMsg";// 消息名称

    RedisDAO redisDao;

    private int rc = 1;//0 表示鉴权成功，1表示鉴权失败，-1表示未知错误，2表示需要重新登陆

    // private String msr = null;//user=长连接,business=业务逻辑,event=事件处理,file=文件处理，null的话默认是长连接

    public AuthMsg(String msg, Channel channel){
        super(msg, channel);
    }

    @Override
    public void onHandler() {

        MyConnection myConnection = null;
        uid = null;
        // 从MyConnectionListener中的connections属性中查询该socketId是否已经存在长连接，如果存在则说明该用户已经在某个客户端登录
        myConnection = MyConnectionListener.getMyConnectionBySocketId(channel.getId());// 根据socketId获取当前用户是否已经在一台机器上登录

        if (myConnection != null) {
            uid = myConnection.getChName();
            //客户端鉴权时需要将sid放到msg中，客户端通过短链接进行登陆，服务端会将sid作为响应结果的一部分返回，并且将sid保存在redis
            String sid = SjsonUtil.getSIDFromMsg(msg);//从参数中截取sessionId
            if (StringUtils.isNotBlank(sid)) {
                redisDao = AppServerBeanFactory.getRedisDAO();
                // 根据sid去redis里查询该sid是否有效
                String userInfo = redisDao.getUserInfoBySessionId(sid);
                // {"S_K:5613435423564354":"{\"id\":20130401,\"email\":\"liuhouxiang1986@163.com\",\"password\":\"123456\",\"status\":1}"}
                if (userInfo != null) {
                    JSONObject userJson = JSONObject.parseObject(userInfo);
                    uid = userJson.getLong("id").toString();

                    if (StringUtils.isNotBlank(uid)) {
                        // oldConnection不为空说明同一个用户多处登录，将前一个用户踢下线。
                        MyConnection oldConnection = MyConnectionListener.addNamedConnection(myConnection);

                        if (oldConnection != null) {
                            boolean isSameUser = myConnection.getChannel().getId().equals(oldConnection.getChannel().getId());
                            //同一个账号，更换设备登陆，将前一个设备上的账号踢下线
                            if(!isSameUser){
                                    log.debug(uid + " login in other IP.old socked id=" + oldConnection.getID()
                                            + ",now socket id=" + channel.getId());
                                //将前一个长链接从长链接池子中删掉
                                MyConnectionListener.connectionDestroyed(oldConnection.getID());
                                oldConnection.setReplaced(true);
                                //如果两个长链接来自不同的两个ip，则将前一个踢下线
                                if(oldConnection.getChannel() != null ){
                                    Channel channel1 = oldConnection.getChannel();
                                    SocketAddress ip = channel1.getRemoteAddress();
                                    Channel channel2 = myConnection.getChannel();
                                    SocketAddress ip2 = channel2.getRemoteAddress();
                                    if(!ip.toString().equals(ip2.toString())){
                                        oldConnection.write(AUTH_RELOGIN);//将之前设备上的账号踢下线
                                    }
                                }
                            }else{
                                //同一账号两个长链接，在此仅作日志处理，客户端应该会控制长链接数量
                                log.warn("same user auth, uid=" + uid);
                            }
                            // 把最新connection设置成有效
                            myConnection.setValid(true);
                            myConnection.setChName(uid);
                        } else {
                            // 把最新connection设置成有效
                            myConnection.setValid(true);
                            myConnection.setChName(uid);
                        }
                        rc = 0;

                    } else {
                        log.info("get userinfo failed， sid=" + sid + ",client=" + myConnection.getRemoteAddress());
                        rc = 2;
                    }
                } else {
                       log.info("sid invalid, sid=" + sid);
                        rc = 2;
                }
            }else{
                log.info("user not login,relogin");
                rc = 2;
            }
        } else if (0 != rc) {
            log.info("can not find valid connection,channelId:" + channel.getId());
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
                //鉴权成功，开始心跳线程用来维持长链接，自此开始服务端和客户端的心跳检测
                Timer.addTimer(new HeartBeatWorker(uid, myConnection, DateUtil.getCacheNow()));
                // 验证成功了，需要推送离线消息
                pushOfflineMsg(uid);
                log.info("auth success,user=" + uid + ",socketId=" + channel.getId() + ",addr=" + channel.getRemoteAddress());
            } else if (1 == rc) {
                writeFuture = channel.write(AUTH_FAILED);
                log.info("auth failed,msg=" + msg + ",client=" + channel.getRemoteAddress());
            } else if (2 == rc) {
                writeFuture = channel.write(AUTH_RELOGIN);
                log.info("user relogin,msg=" + msg + ",client=" + channel.getRemoteAddress());
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
            log.error("auth exception， e:",e);
        }
    }

    /**
     * 客户端长链接鉴权充公，需要推送离线消息
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
                    log.warn("push offline msg error" + e.getCause() + "," + content);
                }
            } else {//一般离线消息保存七天，如果消息过期，则删除离线队列中过期消息
                redisDao.removeOfflineMsg(uid, msgId);
                redisDao.removeMessageByMsgId(msgId);
                log.debug("msgId: " + msgId + " content is null");
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
