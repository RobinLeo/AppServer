package com.robin.im.netty.protocol;

import org.jboss.netty.channel.*;
import org.jboss.netty.handler.codec.oneone.OneToOneEncoder;
import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.buffer.ChannelBuffer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class PushServerEncoder extends OneToOneEncoder {

    final Logger logger = LoggerFactory.getLogger(PushServerEncoder.class);

    private static final ChannelBuffer HB_BUFFER = ChannelBuffers.wrappedBuffer(new byte[] { 0x0b,0 });
    private static final ChannelBuffer NULLBUFFER = ChannelBuffers.wrappedBuffer(new byte[] { 0 });

    @Override
    protected Object encode(ChannelHandlerContext ctx, Channel channel, Object msg) throws Exception {
        if (msg instanceof String) {
            // return ChannelBuffers.wrappedBuffer(((String)msg).getBytes());
            ChannelBuffer msgBuffer = ChannelBuffers.wrappedBuffer(((String) msg).getBytes());
            int sendLen = msgBuffer.readableBytes();
            if (msg != null) {
                
            }
            if (1 > sendLen) {
                return null;
            }
            if (0 == msgBuffer.getByte(sendLen - 1)) {
                
                return msgBuffer;
            }
            return ChannelBuffers.wrappedBuffer(msgBuffer, NULLBUFFER);
        } else if(msg instanceof Integer){
            Integer intMsg = ((Integer) msg);
            if(intMsg == 0x0b){//发送心跳到客户端
                return HB_BUFFER;
            }
            ChannelBuffer buffer = ChannelBuffers.dynamicBuffer();
            buffer.writeInt(intMsg);
            return buffer;
        } else {
            return msg;
        }
        
    }

}
