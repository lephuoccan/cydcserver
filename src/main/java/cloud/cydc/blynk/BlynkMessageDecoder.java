package cloud.cydc.blynk;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Decodes Blynk binary protocol messages
 * Format: [Command(1)][MessageId(2)][Length(2)][Body(Length)]
 * For RESPONSE: [Command(1)][MessageId(2)][StatusCode(2)]
 */
public class BlynkMessageDecoder extends ByteToMessageDecoder {
    private static final Logger log = LoggerFactory.getLogger(BlynkMessageDecoder.class);
    
    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
        // Need at least header (5 bytes)
        if (in.readableBytes() < BlynkProtocol.BLYNK_HEADER_SIZE) {
            return;
        }
        
        // Mark reader index to reset if we don't have full message
        in.markReaderIndex();
        
        // Read header
        byte command = in.readByte();
        int messageId = in.readUnsignedShort();
        int lengthOrStatus = in.readUnsignedShort();
        
        log.debug("Decoding: cmd={}, msgId={}, len/status={}", command, messageId, lengthOrStatus);
        
        // For RESPONSE messages, length field is actually status code
        if (command == BlynkProtocol.BLYNK_CMD_RESPONSE) {
            BlynkMessage msg = new BlynkMessage(command, messageId, lengthOrStatus);
            log.debug("Decoded RESPONSE: {}", msg);
            out.add(msg);
            return;
        }
        
        int length = lengthOrStatus;
        
        // Validate length
        if (length > BlynkProtocol.BLYNK_MAX_BODY_SIZE) {
            log.error("Message body too large: {} bytes (max {})", length, BlynkProtocol.BLYNK_MAX_BODY_SIZE);
            in.resetReaderIndex();
            ctx.close();
            return;
        }
        
        // Check if we have full body
        if (in.readableBytes() < length) {
            in.resetReaderIndex();
            return;
        }
        
        // Read body
        byte[] body = new byte[length];
        if (length > 0) {
            in.readBytes(body);
        }
        
        BlynkMessage msg = new BlynkMessage(command, messageId, body);
        log.debug("Decoded: {}", msg);
        out.add(msg);
    }
}
