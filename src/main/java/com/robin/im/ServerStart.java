package com.robin.im;

import com.robin.im.netty.message.MessageManager;

/**
 * Created with IntelliJ IDEA.
 * User: liuhouxiang1986@gmail.com.
 * Date: 2014/7/3 13:14
 * Project: AppServer
 */
public class ServerStart {
    public static void main(String[] args) {
        //init log system
        InitLog logSystem = new InitLog();
        logSystem.init();
        //init log system end

        //init netty
        InitNetty netty = new InitNetty();
        netty.start();
        //init netty end

        //init redis
        MessageManager.redisDAO = AppServerBeanFactory.getRedisDAO();
        //init redis end

        //启动服务
        MessageManager.start();

    }
}
