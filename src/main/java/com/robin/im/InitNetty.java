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
 * User: liuhouxiang1986@gmail.com.
 * Date: 2014/7/3 16:16
 * Project: AppServer
 */
public class InitNetty {
    private Logger logger = LoggerFactory.getLogger(InitNetty.class);

    /**
     * TCP与UDP区别
     TCP---传输控制协议,提供的是面向连接、可靠的字节流服务。当客户和服务器彼此交换数据前，必须先在双方之间建立一个TCP连接，
     之后才能传输数据。TCP提供超时重发，丢弃重复数据，检验数据，流量控制等功能，保证数据能从一端传到另一端。
     UDP---用户数据报协议，是一个简单的面向数据报的运输层协议。UDP不提供可靠性，它只是把应用程序传给IP层的数据报发送出去，
     但是并不能保证它们能到达目的地。由于UDP在传输数据报前不用在客户和服务器之间建立一个连接，且没有超时重发等机制，故而传输速度很快
     可以看出，UDP与TCP的主要区别在于：UDP是无连接的，而这一点便是在使用netty进行开发时最重要的区别点了。

     在ChannelFactory 的选择上，UDP的通信选择 NioDatagramChannelFactory，TCP的通信我们选择的是NioServerSocketChannelFactory；
     在Bootstrap的选择上，UDP选择的是ConnectionlessBootstrap，而TCP选择的是ServerBootstrap。
     */

    public void start(){
        InetSocketAddress address = new InetSocketAddress(Constants.NETTY_PORT);
        /*
        Executors.newCachedThreadPool()的解释：
        缓冲线程执行器，产生一个大小可变的线程池。
        当线程池的线程多于执行任务所需要的线程的时候，对空闲线程（即60s没有任务执行）进行回收；
        当执行任务的线程数不足的时候，自动拓展线程数量。因此线程数量是JVM可创建线程的最大数目。

        Netty提供NIO与BIO两种模式，我们主要关心NIO的模式：
        NIO处理方式：
        1.Netty用一个BOSS线程去处理客户端的接入，创建Channel
        2.从WORK线程池（WORK线程数量默认为cpu cores的2倍）拿出一个WORK线程交给BOSS创建好的Channel实例（Channel实例持有java网络对象）
        3.WORK线程进行数据读入（读到ChannelBuffer）
        4.接着触发相应的事件传递给ChannelPipeline进行业务处理（ChannelPipeline中包含一系列用户自定义的ChannelHandler组成的链）
         */
        ServerSocketChannelFactory channelFactory = new NioServerSocketChannelFactory(
                Executors.newCachedThreadPool(),
                Executors.newCachedThreadPool()
        );

        DefaultChannelGroup allChannels = new DefaultChannelGroup("pushServerChannelGroup");//名称唯一

        /**
         * 一切从ServerBootstrap开始
         * ServerBootstrap 负责初始话netty服务器，并且开始监听端口的socket请求。
         */
        ServerBootstrap bootstrap = new ServerBootstrap(channelFactory);

        // PushServerPipelineFactory作为一个ChannelPipelineFactory产生的工厂类，我们可以把需要执行的Handler进行配置
        ChannelPipelineFactory pipelineFactory = new PushServerPiplelineFactory(allChannels);

        // 服务器新连接建立的时候，新的ChannelPipeline会通过我们定义的ChannelPipelineFactory产生，其实是调用了getPipeline()方法。
        bootstrap.setPipelineFactory(pipelineFactory);
        /**
         * tcp传输基本原理，系统底层有一个缓冲区，这个缓冲区是各家系统根据tcp/ip协议自己实现的，大小不同，且你无法直接控制大小，
         * 一般情况下，这个缓冲区会在被填满时，且网络闲置时发送。这个大小从512KB到4MB不等，当你发送数据超过这个缓冲区大小的时候，
         * 就只能被该缓冲区拆成一段段的发，也因此你的接收端会一段段的收到数据，这就是tcp传输中粘包、断包问题的由来。
         */
        int tcpSendBufferSize = 32768;//32K,netty默认是1024bytes
        int tcpReceiveBufferSize = 32768;

        if (tcpReceiveBufferSize != -1) {
            bootstrap.setOption("child.receiveBufferSize", tcpReceiveBufferSize);//设置接收缓冲区的大小。在一定程度上可以提高吞吐量。
        }
        if (tcpSendBufferSize != -1) {
            bootstrap.setOption("child.sendBufferSize", tcpSendBufferSize);
        }
        //Option更过选项参见http://netty.io/3.6/api/index.html?org/jboss/netty/channel/socket/SocketChannelConfig.html
        bootstrap.setOption("reuseAddress", true);//因系统不同而不同，代表是socket连接是否允许使用上一个连接状态是timewait的连接地址
        bootstrap.setOption("child.reuseAddress", true);
        //tcp定期发送心跳包 比如IM里边定期探测对方是否下线
        //只有tcp长连接下才有意义，默认是false，因为keeplive依赖的是系统机制。我们使用的是服务端主动发起心跳给客户端的策略
        bootstrap.setOption("child.keepAlive", false);
        bootstrap.setOption("child.tcpNoDelay", true);//关闭Nagle算法

        Channel serverChannel = bootstrap.bind(address);
        allChannels.add(serverChannel);
        logger.info("netty start");
    }
}
