package com.robin.im;

import com.robin.im.netty.protocol.PushServerPiplelineFactory;
import com.robin.im.util.Constants;
import org.jboss.netty.bootstrap.ServerBootstrap;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelPipelineFactory;
import org.jboss.netty.channel.group.DefaultChannelGroup;
import org.jboss.netty.channel.socket.ServerSocketChannelFactory;
import org.jboss.netty.channel.socket.nio.NioServerSocketChannelFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.util.concurrent.Executors;

/**
 * Created with IntelliJ IDEA.
 * User: liuhouxiang@1986@gmail.com.
 * Date: 2014/7/3 16:16
 * Project: AppServer
 */
public class InitNetty {
    private Logger logger = LoggerFactory.getLogger(InitNetty.class);

    public void start(){
        InetSocketAddress address = new InetSocketAddress(Constants.NETTY_PORT);
        //Executors.newCachedThreadPool()的解释：
        //缓冲线程执行器，产生一个大小可变的线程池。
        //当线程池的线程多于执行任务所需要的线程的时候，对空闲线程（即60s没有任务执行）进行回收；
        //当执行任务的线程数不足的时候，自动拓展线程数量。因此线程数量是JVM可创建线程的最大数目。
        ServerSocketChannelFactory channelFactory = new NioServerSocketChannelFactory(
                Executors.newCachedThreadPool(),
                Executors.newCachedThreadPool()
        );

        int tcpSendBufferSize = 32768;//32K
        int tcpReceiveBufferSize = 32768;

        DefaultChannelGroup allChannels = new DefaultChannelGroup("pushServerChannelGroup");
        ServerBootstrap bootstrap = new ServerBootstrap(channelFactory);

        // PushServerPipelineFactory作为一个ChannelPipelineFactory产生的工厂类，我们可以把需要执行的Handler进行配置
        ChannelPipelineFactory pipelineFactory = new PushServerPiplelineFactory(allChannels);

        // 服务器新连接建立的时候，新的ChannelPipeline会通过我们定义的ChannelPipelineFactory产生，其实是调用了getPipeline()方法。
        bootstrap.setPipelineFactory(pipelineFactory);

        if (tcpReceiveBufferSize != -1) {
            bootstrap.setOption("child.receiveBufferSize", tcpReceiveBufferSize);
        }
        if (tcpSendBufferSize != -1) {
            bootstrap.setOption("child.sendBufferSize", tcpSendBufferSize);
        }

        bootstrap.setOption("reuseAddress", true);
        bootstrap.setOption("child.reuseAddress", true);
        bootstrap.setOption("child.keepAlive", false);
        bootstrap.setOption("child.tcpNoDelay", true);

        Channel serverChannel = bootstrap.bind(address);
        allChannels.add(serverChannel);

        logger.info("netty start");
    }
}
