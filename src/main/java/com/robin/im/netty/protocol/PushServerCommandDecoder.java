package com.robin.im.netty.protocol;

import com.robin.im.util.Constants;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBufferIndexFinder;
import org.jboss.netty.channel.*;
import org.jboss.netty.handler.codec.frame.FrameDecoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.nio.charset.Charset;

/**
 * The MemcachedCommandDecoder is responsible for taking lines from the MemcachedFrameDecoder and parsing them
 * into CommandMessage instances for handling by the MemcachedCommandHandler
 * <p/>
 * Protocol status is held in the SessionStatus instance which is shared between each of the decoders in the pipeline.
 */
public final class PushServerCommandDecoder extends FrameDecoder {
    
    private static final Logger log = LoggerFactory.getLogger(PushServerCommandDecoder.class);
    private static final int MIN_BYTES_LINE = 2;

    public static final Charset UTF8 = Charset.forName("UTF-8");

    public PushServerCommandDecoder() {
    }

    /**
     * Index finder which locates a byte which is not 0x00
     */
    static ChannelBufferIndexFinder NULL_DELIMITER = new ChannelBufferIndexFinder() {

        public final boolean find(ChannelBuffer buffer, int guessedIndex) {
            byte b = buffer.getByte(guessedIndex);
            return b == 0;
        }
    };

    @Override
    protected Object decode(ChannelHandlerContext ctx, Channel channel, ChannelBuffer buffer) throws Exception {
        ChannelBuffer in = buffer.slice();
        if (in.readableBytes() < MIN_BYTES_LINE)
            return null;
        int pos = in.bytesBefore(NULL_DELIMITER);
        if (-1 == pos) {
            return null;
        } else if (0 == pos) {
            buffer.skipBytes(1);
            return null;
        }
        ChannelBuffer frame;
        frame = buffer.readBytes(pos);
        log.info("receive byte:" + frame.getByte(0));
        if (Constants.HEARTBEAT_ACK == frame.getByte(0)) {//"0xB0"
            return CommandMessage.command(0);
        }
        CommandMessage cmd = CommandMessage.command(1);
        cmd.message = frame.toString(UTF8);
        log.info("recv message:" + cmd.message);
        return cmd;
    }
}
