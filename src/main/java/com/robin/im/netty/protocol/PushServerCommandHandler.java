package com.robin.im.netty.protocol;

import com.alibaba.fastjson.JSONObject;
import com.robin.im.AppServerBeanFactory;
import com.robin.im.netty.connection.MyConnection;
import com.robin.im.netty.connection.MyConnectionListener;
import com.robin.im.netty.message.CreateMsg;
import com.robin.im.netty.message.DuplicateMsg;
import com.robin.im.netty.message.MessageManager;
import com.robin.im.netty.message.MessagePack;
import com.robin.im.rev.AuthMsg;
import com.robin.im.util.Constants;
import com.robin.im.util.SjsonUtil;
import org.jboss.netty.channel.*;
import org.jboss.netty.channel.group.DefaultChannelGroup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.atomic.AtomicInteger;
/**
 * Created with IntelliJ IDEA.
 * User: liuhouxiang@1986@gmail.com.
 * Date: 2014/7/3 16:40
 * Project: AppServer
 */
@ChannelHandler.Sharable
public class PushServerCommandHandler extends SimpleChannelUpstreamHandler {

    final static Logger logger = LoggerFactory.getLogger(PushServerCommandHandler.class);


    // 为了监控添加的监控变量
    public static final AtomicInteger curr_conns = new AtomicInteger(0);

    // 为了监控添加的监控变量
    public static final AtomicInteger receiveCount = new AtomicInteger(0);

    // 为了监控添加的监控变量
    public static final AtomicInteger authCount = new AtomicInteger(0);

    // private final ConnectionLifeCycleListener listener;
    /**
     * The channel group for the entire daemon, used for handling global cleanup on shutdown.
     */
    private final DefaultChannelGroup channelGroup;


    public PushServerCommandHandler(DefaultChannelGroup channelGroup) {
        this.channelGroup = channelGroup;
    }


    /**
     * On open we manage some statistics, and add this connection to the channel group.
     *
     * @param channelHandlerContext
     * @param channelStateEvent
     * @throws Exception
     */
    @Override
    public void channelOpen(ChannelHandlerContext channelHandlerContext, ChannelStateEvent channelStateEvent)
            throws Exception {
        // total_conns.incrementAndGet();
        curr_conns.incrementAndGet();
        channelGroup.add(channelHandlerContext.getChannel());
        channelHandlerContext.sendUpstream(channelStateEvent);
        if (logger.isTraceEnabled()) {
            logger.trace("channelOpen client=" + channelStateEvent.getChannel().getRemoteAddress() + ",curr_conns="
                    + curr_conns);
        }
    }

    /**
     * On close we manage some statistics, and remove this connection from the channel group.
     *
     * @param channelHandlerContext
     * @param channelStateEvent
     * @throws Exception
     */
    @Override
    public void channelClosed(ChannelHandlerContext channelHandlerContext, ChannelStateEvent channelStateEvent)
            throws Exception {
        if (logger.isTraceEnabled()) {
            logger.trace("channelClosed socketId=" + channelStateEvent.getChannel().getId() + ",curr_conns="
                    + curr_conns);
        }
        MyConnection myConnection = MyConnectionListener.getMyConnectionBySocketId(channelStateEvent.getChannel().getId());
        if (myConnection != null && myConnection.isValid()) {
            if (false == myConnection.isReplaced()) {
                if(logger.isDebugEnabled()){
                    logger.debug("user: " + myConnection.getChName() + " offline");
                }
            }
            myConnection.setValid(false);
        }
        MyConnectionListener.connectionDestroyed(channelStateEvent.getChannel().getId());
        curr_conns.decrementAndGet();
        channelGroup.remove(channelHandlerContext.getChannel());
    }

    public void channelDisconnected(final ChannelHandlerContext ctx, final ChannelStateEvent e) throws Exception {
        int socketId = e.getChannel().getId();
        if (logger.isTraceEnabled()) {
            logger.trace("channelDisconnected socketId=" + socketId);
        }
        MyConnection myConnection = MyConnectionListener.getMyConnectionBySocketId(socketId);
        if (myConnection != null && myConnection.isValid()) {
            if (false == myConnection.isReplaced()) {
                if(logger.isDebugEnabled()){
                    logger.debug("user: " + myConnection.getChName() + " offline");
                }
            }
            myConnection.setValid(false);
        }
        MyConnectionListener.connectionDestroyed(socketId);
        channelGroup.remove(e.getChannel());
    }

    public void exceptionCaught(final ChannelHandlerContext ctx, final ExceptionEvent e) throws Exception {
        int socketId = e.getChannel().getId();

        if (logger.isDebugEnabled()) {
            logger.error("socketId=" + socketId + ",exceptionCaught e=" + e.toString());
        }
        try {
            MyConnection myConnection = MyConnectionListener.getMyConnectionBySocketId(socketId);
            if (myConnection != null && myConnection.isValid()) {
                if (false == myConnection.isReplaced()) {
                    if(logger.isDebugEnabled()){
                        logger.debug("user: " + myConnection.getChName() + " offline");
                    }
                }
            }
            if (myConnection != null) {
                myConnection.setValid(false);
            }
            e.getChannel().close();
        } catch (Exception ex) {
            logger.error("failed to notify the listener:", ex);
        }
    }

    public void channelConnected(final ChannelHandlerContext ctx, final ChannelStateEvent e) throws Exception {
        MyConnection myconnection = new MyConnection(e.getChannel());
        MyConnectionListener.connectionCreated(myconnection);
    }

    public void writeRequested(ChannelHandlerContext ctx, MessageEvent e) throws Exception {
        ctx.sendDownstream(e);
        if (logger.isDebugEnabled()) {
            logger.debug("==>" + e.getMessage());
        }
    }

    public void writeComplete(ChannelHandlerContext ctx, WriteCompletionEvent e) throws Exception {

    }

    /**
     * The actual meat of the matter.  Turn CommandMessages into executions against the physical cache, and then
     * pass on the downstream messages.
     *
     * @param channelHandlerContext
     * @param messageEvent
     * @throws Exception
     */

    public void messageReceived(ChannelHandlerContext channelHandlerContext, MessageEvent messageEvent)
            throws Exception {
        CommandMessage command = (CommandMessage) messageEvent.getMessage();
        if (0 == command.type) {//心跳信令
            MyConnection myConnection =MyConnectionListener.getMyConnectionBySocketId(
                    messageEvent.getChannel().getId());
            if (myConnection != null) {
                myConnection.incPackCount();
                logger.info("<==HB response from " + myConnection.getChName());
                if (myConnection.isValid()) {
                    //心跳确认信令的逻辑
                    myConnection.reFreshTime();
                    myConnection.setWaitHbAck(false);
                } else {
                    myConnection.close();
                    logger.info("Find a connection is not valid,but send HB!");
                }
            } else {
                logger.info("HB Can not find myconnection ,socket id=" + messageEvent.getChannel().getId());
            }
        } else {
            logger.info("socketId=" + messageEvent.getChannel().getId() + "<==" + command.message);
            if (command.message.length() > 3) {
                logger.info("receive msg ,content = " + command.message);
                MessageManager.addReceivedMessage(handleMessage(command.message, messageEvent));
            } else {
                logger.warn("too short message.");
            }
        }
    }

    public static MessagePack handleMessage(String msg, Channel channel){

        MessagePack messagePack = null;
        long pid = -1;// package id

        int fid = SjsonUtil.getFIDFromMsg(msg);
        logger.info("handle this msg:" + msg);
        MyConnection myConnection = MyConnectionListener.getMyConnectionBySocketId(channel.getId());
        if (myConnection != null) {
            //如果消息不是认证消息，并且长链接已经失效，重新进行验证，客户端使用长链接时需要将sid放置到消息体中
            if (fid != Constants.AUTH_MSG_CODE && false == myConnection.isValid()) {
                String sid = SjsonUtil.getSIDFromMsg(msg);
                if(sid!=null && !"".equals(sid)){
                    AuthMsg authMsg = new AuthMsg(msg,myConnection.getChannel());
                    authMsg.onHandler();
                }
                if(!myConnection.isValid()){
                    myConnection.close();
                    logger.warn("Find a connection is not valid,but send fid=" + fid);

                    if (-1 == fid) {
                        long rpid = SjsonUtil.getRPIDFromMsg(msg);
                        if (rpid > 0) {
                            // 是应答消息
                            MessageManager.removeWaitAckMsg(rpid);
                        }
                    }
                    return null;
                }
            }
            if (fid != Constants.AUTH_MSG_CODE) {
                // 认证不需要包号
                pid = SjsonUtil.getPIDFromMsg(msg);
            }
            if (pid > 0) {
                // 回复客户端包号
                myConnection.write(new StringBuffer("{\"PID\":").append(pid).append('}').toString());
                if (AppServerBeanFactory.getRedisDAO().isPidInCache(myConnection.getChName(), pid)) {
                    // 收到重复的包
                    JSONObject jsonObj = JSONObject.parseObject(msg);
                    JSONObject data = jsonObj.getJSONObject("Data");
                    int type = data.getIntValue("Type");
                    if(Constants.NEW_MSG_CODE == fid && type == 0){//防止文本消息重发，直接告诉客户端消息发送成功
                        messagePack = new DuplicateMsg(msg, channel);
                        return messagePack;
                    }
                }
            }
        }
        logger.info("connection is null,",fid);
        switch (fid) {
            case Constants.AUTH_MSG_CODE: // 长连接鉴权
                messagePack = new AuthMsg(msg, channel);
                break;
            case Constants.NEW_MSG_CODE: //新建消息
                messagePack = new CreateMsg(msg,channel);
                break;
            case -1: // 可能是应答消息
                long rpid = SjsonUtil.getRPIDFromMsg(msg);
                if (rpid > 0) {
                    // 是应答消息
                    MessageManager.removeWaitAckMsg(rpid);
                }
                break;
            default:
                logger.warn("unknow FID=" + fid + ",raw msg=" + msg + ",client=" + channel.getRemoteAddress());
        }

        return messagePack;

    }

    public MessagePack handleMessage(String msg, MessageEvent e) {
        return handleMessage(msg,e.getChannel());
    }

}
