package com.robin.im.netty.protocol;

import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.ChannelPipelineFactory;
import org.jboss.netty.channel.Channels;
import org.jboss.netty.channel.group.DefaultChannelGroup;

/**
 * Created with IntelliJ IDEA.
 * User: liuhouxiang@1986@gmail.com.
 * Date: 2014/7/3 16:40
 * Project: AppServer
 */
public class PushServerPiplelineFactory implements ChannelPipelineFactory {
    private DefaultChannelGroup channelGroup;
    private final PushServerCommandHandler pushServerCommandHandler;

    private final PushServerEncoder pushServerEncoder = new PushServerEncoder();
    private final PushServerCommandDecoder puserServerDecoder = new PushServerCommandDecoder();

    public PushServerPiplelineFactory(DefaultChannelGroup channelGroup){
        this.channelGroup = channelGroup;
        this.pushServerCommandHandler = new PushServerCommandHandler(channelGroup);

    }
    @Override
    public ChannelPipeline getPipeline() throws Exception {
        return Channels.pipeline(puserServerDecoder,pushServerCommandHandler,pushServerEncoder);
    }


}
