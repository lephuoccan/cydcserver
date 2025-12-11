package cloud.cydc.blynk;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Encodes BlynkMessage to binary format
 */
public class BlynkMessageEncoder extends MessageToByteEncoder<BlynkMessage> {
    private static final Logger log = LoggerFactory.getLogger(BlynkMessageEncoder.class);
    
    @Override
    protected void encode(ChannelHandlerContext ctx, BlynkMessage msg, ByteBuf out) throws Exception {
        log.debug("Encoding response: {}", msg);
        int startIndex = out.writerIndex();
        msg.encode(out);
        int bytesWritten = out.writerIndex() - startIndex;
        log.debug("Encoded {} bytes for msgId={}", bytesWritten, msg.getMessageId());
    }
}
