package cloud.cydc.blynk;

import cloud.cydc.service.DeviceInfoService;
import cloud.cydc.service.VirtualPinService;
import cloud.cydc.util.TokenValidator;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Alternative handler: NO RESPONSE for LOGIN
 * Some Blynk library versions don't expect response
 */
public class BlynkProtocolHandlerAlt extends SimpleChannelInboundHandler<BlynkMessage> {
    private static final Logger log = LoggerFactory.getLogger(BlynkProtocolHandlerAlt.class);
    
    private final DeviceInfoService deviceService;
    private final VirtualPinService pinService;
    private final TokenValidator tokenValidator;
    private final Map<ChannelHandlerContext, String> authenticatedSessions = new ConcurrentHashMap<>();
    
    public BlynkProtocolHandlerAlt(DeviceInfoService deviceService, 
                                    VirtualPinService pinService,
                                    TokenValidator tokenValidator) {
        this.deviceService = deviceService;
        this.pinService = pinService;
        this.tokenValidator = tokenValidator;
    }
    
    @Override
    protected void channelRead0(ChannelHandlerContext ctx, BlynkMessage msg) throws Exception {
        log.info("[BlynkAlt] Received: {}", msg);
        
        byte command = msg.getCommand();
        
        try {
            switch (command) {
                case BlynkProtocol.BLYNK_CMD_LOGIN:
                case BlynkProtocol.BLYNK_CMD_LOGIN_2:
                    handleLoginNoResponse(ctx, msg);
                    break;
                    
                case BlynkProtocol.BLYNK_CMD_PING:
                    sendResponse(ctx, msg.getMessageId(), BlynkProtocol.BLYNK_SUCCESS);
                    break;
                    
                case BlynkProtocol.BLYNK_CMD_HARDWARE:
                    log.info("[BlynkAlt] HARDWARE command received!");
                    sendResponse(ctx, msg.getMessageId(), BlynkProtocol.BLYNK_SUCCESS);
                    break;
                    
                default:
                    log.warn("[BlynkAlt] Unsupported: {}", BlynkProtocol.getCommandName(command));
                    sendResponse(ctx, msg.getMessageId(), BlynkProtocol.BLYNK_ILLEGAL_COMMAND);
            }
        } catch (Exception e) {
            log.error("[BlynkAlt] Error: {}", e.getMessage());
        }
    }
    
    private void handleLoginNoResponse(ChannelHandlerContext ctx, BlynkMessage msg) {
        String token = msg.getBodyAsString();
        
        if (tokenValidator.validateToken(token)) {
            authenticatedSessions.put(ctx, token);
            log.info("[BlynkAlt] ✓ Login OK - NO RESPONSE sent, just waiting...");
            // NO RESPONSE - just keep connection alive
        } else {
            log.warn("[BlynkAlt] Invalid token");
            sendResponse(ctx, msg.getMessageId(), BlynkProtocol.BLYNK_INVALID_TOKEN);
        }
    }
    
    private void sendResponse(ChannelHandlerContext ctx, int messageId, short status) {
        BlynkMessage response = BlynkMessage.response(messageId, status);
        ctx.writeAndFlush(response);
    }
    
    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        String token = authenticatedSessions.remove(ctx);
        log.warn("[BlynkAlt] Disconnected, was auth: {}", token != null);
        super.channelInactive(ctx);
    }
    
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        log.error("[BlynkAlt] Exception", cause);
        ctx.close();
    }
}
