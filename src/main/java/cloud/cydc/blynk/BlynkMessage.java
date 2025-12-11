package cloud.cydc.blynk;

import io.netty.buffer.ByteBuf;

/**
 * Blynk Protocol Message
 */
public class BlynkMessage {
    private final byte command;
    private final int messageId;
    private final byte[] body;
    private int statusCode; // For RESPONSE messages
    
    public BlynkMessage(byte command, int messageId, byte[] body) {
        this.command = command;
        this.messageId = messageId;
        this.body = body != null ? body : new byte[0];
        this.statusCode = 0;
    }
    
    public BlynkMessage(byte command, int messageId, int statusCode) {
        this.command = command;
        this.messageId = messageId;
        this.body = new byte[0];
        this.statusCode = statusCode;
    }
    
    public byte getCommand() {
        return command;
    }
    
    public int getMessageId() {
        return messageId;
    }
    
    public byte[] getBody() {
        return body;
    }
    
    public String getBodyAsString() {
        if (body == null || body.length == 0) return "";
        return new String(body);
    }
    
    public int getLength() {
        return body != null ? body.length : 0;
    }
    
    public int getStatusCode() {
        return statusCode;
    }
    
    /**
     * Encode message to ByteBuf
     * Format: [Command(1)][MessageId(2)][Length(2)][Body(Length)]
     * For RESPONSE: [Command(1)][MessageId(2)][StatusCode(2)]
     */
    public void encode(ByteBuf out) {
        out.writeByte(command);
        out.writeShort(messageId);
        
        // For RESPONSE messages, encode status code in length field position
        if (command == BlynkProtocol.BLYNK_CMD_RESPONSE && body.length == 0) {
            out.writeShort(statusCode);
        } else {
            // Normal messages: write length then body
            out.writeShort(getLength());
            if (body != null && body.length > 0) {
                out.writeBytes(body);
            }
        }
    }
    
    /**
     * Create response message
     * For hardware protocol, status code is encoded in length field, not body
     */
    public static BlynkMessage response(int messageId, short status) {
        return new BlynkMessage(BlynkProtocol.BLYNK_CMD_RESPONSE, messageId, (int)status);
    }
    
    /**
     * Create response with body
     */
    public static BlynkMessage response(int messageId, short status, String responseBody) {
        if (responseBody == null || responseBody.isEmpty()) {
            return new BlynkMessage(BlynkProtocol.BLYNK_CMD_RESPONSE, messageId, (int)status);
        }
        
        // Response with body: status code in first 2 bytes + body
        byte[] bodyBytes = responseBody.getBytes();
        byte[] combined = new byte[2 + bodyBytes.length];
        combined[0] = (byte) (status >> 8);
        combined[1] = (byte) status;
        System.arraycopy(bodyBytes, 0, combined, 2, bodyBytes.length);
        
        return new BlynkMessage(BlynkProtocol.BLYNK_CMD_RESPONSE, messageId, combined);
    }
    
    @Override
    public String toString() {
        return String.format("BlynkMessage[cmd=%s, msgId=%d, len=%d, body=%s]",
            BlynkProtocol.getCommandName(command), messageId, getLength(),
            getLength() > 0 ? getBodyAsString() : "empty");
    }
}
