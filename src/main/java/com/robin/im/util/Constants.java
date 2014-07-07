package com.robin.im.util;

/**
 * Created with IntelliJ IDEA.
 * User: liuhouxiang@1986@gmail.com.
 * Date: 2014/7/3 16:19
 * Project: AppServer
 */
public class Constants {
    public final static int  AUTH_MSG_CODE               = 0x0608;//长链接认证信息编码
    public final static int  NEW_MSG_CODE                = 0x0101;//创建聊天消息编码
    public final static int  CHAT_MSG_CODE               = 0x0106;//消息编码

    public static final int NETTY_PORT = 8989;

    public static final byte HEARTBEAT_ACK = -80;

    public static final long HEARTBEAT_INTERVAL = 10000L; // 心跳间隔初始值millis;

    public static final long HEARTBEAT_MAX_INTERVAL      = 300000L; // 心跳间隔最大值

    public static final int  TIMEOUT_MAX_COUNT           = 5;        // 最大超时时间

    public static final long PACK_ACK_TIMEOUT = 5000L;//应答超时时间

    public static void main(String[] args) {
        System.out.println(0x0101);
    }
}
