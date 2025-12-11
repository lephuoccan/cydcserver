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
 * Handles Blynk protocol messages for ESP32 devices
 * Compatible with Blynk Library v0.6.1
 */
public class BlynkProtocolHandler extends SimpleChannelInboundHandler<BlynkMessage> {
    private static final Logger log = LoggerFactory.getLogger(BlynkProtocolHandler.class);
    
    private final DeviceInfoService deviceService;
    private final VirtualPinService pinService;
    private final TokenValidator tokenValidator;
    
    // Track authenticated sessions
    private final Map<ChannelHandlerContext, String> authenticatedSessions = new ConcurrentHashMap<>();
    
    // Track active connections by device ID
    private static final Map<Long, ChannelHandlerContext> activeConnections = new ConcurrentHashMap<>();
    
    public BlynkProtocolHandler(DeviceInfoService deviceService, 
                                 VirtualPinService pinService,
                                 TokenValidator tokenValidator) {
        this.deviceService = deviceService;
        this.pinService = pinService;
        this.tokenValidator = tokenValidator;
    }
    
    /**
     * Send hardware command to connected ESP32
     */
    public static void sendHardwareCommand(long deviceId, int pin, String value) {
        ChannelHandlerContext ctx = activeConnections.get(deviceId);
        log.info("[Blynk] sendHardwareCommand called: deviceId={}, pin={}, value={}, ctx={}", 
                 deviceId, pin, value, ctx != null ? "found" : "NULL");
        
        if (ctx != null && ctx.channel().isActive()) {
            // Format: vw\0<pin>\0<value> - same as hardware sends to server
            String body = "vw\0" + pin + "\0" + value;
            BlynkMessage msg = new BlynkMessage(
                BlynkProtocol.BLYNK_CMD_HARDWARE,
                1, // message ID (can be any - hardware will not check)
                body.getBytes()
            );
            
            log.info("[Blynk] Sending HARDWARE command to device {}: body='{}', bytes={}", 
                     deviceId, body.replace("\0", "\\0"), body.getBytes().length);
            ctx.writeAndFlush(msg);
            log.info("[Blynk] Message sent successfully to device {}: V{} = {}", deviceId, pin, value);
        } else {
            log.warn("[Blynk] Device {} not connected or channel inactive", deviceId);
        }
    }
    
    @Override
    protected void channelRead0(ChannelHandlerContext ctx, BlynkMessage msg) throws Exception {
        log.info("[Blynk] Received: {}", msg);
        
        byte command = msg.getCommand();
        int messageId = msg.getMessageId();
        
        try {
            switch (command) {
                case BlynkProtocol.BLYNK_CMD_LOGIN:
                case BlynkProtocol.BLYNK_CMD_LOGIN_2:
                    handleLogin(ctx, msg);
                    break;
                    
                case BlynkProtocol.BLYNK_CMD_PING:
                    handlePing(ctx, msg);
                    break;
                    
                case BlynkProtocol.BLYNK_CMD_HARDWARE:
                    handleHardware(ctx, msg);
                    break;
                    
                case BlynkProtocol.BLYNK_CMD_INTERNAL:
                    handleInternal(ctx, msg);
                    break;
                    
                case BlynkProtocol.BLYNK_CMD_BRIDGE:
                    handleBridge(ctx, msg);
                    break;
                    
                default:
                    log.warn("[Blynk] Unsupported command: {} ({}), msgId: {}, body length: {}", 
                        BlynkProtocol.getCommandName(command), command, messageId, msg.getBody().length);
                    if (msg.getBody().length > 0 && msg.getBody().length < 100) {
                        log.warn("[Blynk] Body content: {}", msg.getBodyAsString());
                    }
                    sendResponse(ctx, messageId, BlynkProtocol.BLYNK_ILLEGAL_COMMAND);
            }
        } catch (Exception e) {
            log.error("[Blynk] Error processing message: {}", msg, e);
            sendResponse(ctx, messageId, BlynkProtocol.BLYNK_SERVER_EXCEPTION);
        }
    }
    
    /**
     * Handle LOGIN command
     * Body: token string
     */
    private void handleLogin(ChannelHandlerContext ctx, BlynkMessage msg) {
        String token = msg.getBodyAsString();
        
        log.info("[Blynk] Login attempt with token length: {}", token != null ? token.length() : 0);
        
        if (token == null || token.isEmpty()) {
            log.warn("[Blynk] Empty token in login");
            sendResponse(ctx, msg.getMessageId(), BlynkProtocol.BLYNK_INVALID_TOKEN);
            return;
        }
        
        // Validate token
        if (tokenValidator.validateToken(token)) {
            authenticatedSessions.put(ctx, token);
            
            // Register active connection by device ID
            String[] tokenInfo = tokenValidator.extractTokenInfo(token);
            if (tokenInfo != null) {
                try {
                    long deviceId = Long.parseLong(tokenInfo[2]);
                    activeConnections.put(deviceId, ctx);
                    log.info("[Blynk] Device {} registered in active connections", deviceId);
                } catch (NumberFormatException e) {
                    log.error("[Blynk] Invalid device ID in token", e);
                }
            }
            
            log.info("[Blynk] Login successful for token: {}", token);
            
            // Send OK response (status 200) like official Blynk server
            // Reference: Peterkn2001/blynk-server HardwareLoginHandler.completeLogin()
            sendResponse(ctx, msg.getMessageId(), BlynkProtocol.BLYNK_SUCCESS);
            log.info("[Blynk] Login OK response sent (status 200)");
        } else {
            log.warn("[Blynk] Invalid token: {}", token);
            sendResponse(ctx, msg.getMessageId(), BlynkProtocol.BLYNK_INVALID_TOKEN);
        }
    }
    
    /**
     * Handle PING command
     */
    private void handlePing(ChannelHandlerContext ctx, BlynkMessage msg) {
        sendResponse(ctx, msg.getMessageId(), BlynkProtocol.BLYNK_SUCCESS);
    }
    
    /**
     * Handle HARDWARE command
     * Body format: "vw\0<pin>\0<value>" or "vr\0<pin>"
     */
    private void handleHardware(ChannelHandlerContext ctx, BlynkMessage msg) {
        String token = authenticatedSessions.get(ctx);
        if (token == null) {
            log.warn("[Blynk] Not authenticated");
            sendResponse(ctx, msg.getMessageId(), BlynkProtocol.BLYNK_NOT_AUTHENTICATED);
            return;
        }
        
        String body = msg.getBodyAsString();
        String[] parts = body.split("\0");
        
        log.debug("[Blynk] Hardware body parts: {} (total: {})", java.util.Arrays.toString(parts), parts.length);
        
        if (parts.length < 2) {
            log.warn("[Blynk] Invalid hardware command body: {}", body);
            sendResponse(ctx, msg.getMessageId(), BlynkProtocol.BLYNK_ILLEGAL_COMMAND_BODY);
            return;
        }
        
        String command = parts[0]; // "vw" or "vr"
        String pin = parts[1];     // Pin number like "5" or "V5"
        
        // Extract token info
        String[] tokenInfo = tokenValidator.extractTokenInfo(token);
        if (tokenInfo == null) {
            log.error("[Blynk] Failed to extract token info");
            sendResponse(ctx, msg.getMessageId(), BlynkProtocol.BLYNK_INVALID_TOKEN);
            return;
        }
        
        String deviceId = tokenInfo[2];
        
        // Parse deviceId to long for VirtualPinService
        long devId;
        try {
            devId = Long.parseLong(deviceId);
        } catch (NumberFormatException e) {
            log.error("[Blynk] Invalid device ID in token: {}", deviceId);
            sendResponse(ctx, msg.getMessageId(), BlynkProtocol.BLYNK_INVALID_TOKEN);
            return;
        }
        
        if ("vw".equals(command)) {
            // Virtual write
            if (parts.length < 3) {
                log.warn("[Blynk] Missing value for vw command");
                sendResponse(ctx, msg.getMessageId(), BlynkProtocol.BLYNK_ILLEGAL_COMMAND_BODY);
                return;
            }
            
            String value = parts[2];
            
            // Parse pin number - could be "5" or "V5"
            int pinNum;
            if (pin.startsWith("V") || pin.startsWith("v")) {
                try {
                    pinNum = Integer.parseInt(pin.substring(1));
                } catch (NumberFormatException e) {
                    log.warn("[Blynk] Invalid virtual pin: {}", pin);
                    sendResponse(ctx, msg.getMessageId(), BlynkProtocol.BLYNK_ILLEGAL_COMMAND_BODY);
                    return;
                }
            } else {
                try {
                    pinNum = Integer.parseInt(pin);
                } catch (NumberFormatException e) {
                    log.warn("[Blynk] Invalid virtual pin: {}", pin);
                    sendResponse(ctx, msg.getMessageId(), BlynkProtocol.BLYNK_ILLEGAL_COMMAND_BODY);
                    return;
                }
            }
            
            try {
                pinService.setPinValue(devId, pinNum, value);
                log.info("[Blynk] Virtual pin write: V{} = {}", pinNum, value);
                sendResponse(ctx, msg.getMessageId(), BlynkProtocol.BLYNK_SUCCESS);
            } catch (Exception e) {
                log.error("[Blynk] Error setting pin value", e);
                sendResponse(ctx, msg.getMessageId(), BlynkProtocol.BLYNK_ILLEGAL_COMMAND_BODY);
            }
            
        } else if ("vr".equals(command)) {
            // Virtual read - send current value back
            // Parse pin number - could be "5" or "V5"
            int pinNum;
            if (pin.startsWith("V") || pin.startsWith("v")) {
                try {
                    pinNum = Integer.parseInt(pin.substring(1));
                } catch (NumberFormatException e) {
                    log.warn("[Blynk] Invalid virtual pin: {}", pin);
                    sendResponse(ctx, msg.getMessageId(), BlynkProtocol.BLYNK_ILLEGAL_COMMAND_BODY);
                    return;
                }
            } else {
                try {
                    pinNum = Integer.parseInt(pin);
                } catch (NumberFormatException e) {
                    log.warn("[Blynk] Invalid virtual pin: {}", pin);
                    sendResponse(ctx, msg.getMessageId(), BlynkProtocol.BLYNK_ILLEGAL_COMMAND_BODY);
                    return;
                }
            }
            
            try {
                String value = pinService.getPinValue(devId, pinNum);
                
                if (value != null) {
                    // Send hardware command back to client with current value
                    String responseBody = "vw\0" + pinNum + "\0" + value;
                    BlynkMessage response = new BlynkMessage(
                        BlynkProtocol.BLYNK_CMD_HARDWARE,
                        msg.getMessageId(),
                        responseBody.getBytes()
                    );
                    ctx.writeAndFlush(response);
                    log.info("[Blynk] Virtual pin read: V{} = {}", pinNum, value);
                } else {
                    // No value stored, send 0
                    String responseBody = "vw\0" + pinNum + "\00";
                    BlynkMessage response = new BlynkMessage(
                        BlynkProtocol.BLYNK_CMD_HARDWARE,
                        msg.getMessageId(),
                        responseBody.getBytes()
                    );
                    ctx.writeAndFlush(response);
                    log.info("[Blynk] Virtual pin read: V{} = 0 (default)", pinNum);
                }
            } catch (NumberFormatException e) {
                log.warn("[Blynk] Invalid pin number: {}", pin);
                sendResponse(ctx, msg.getMessageId(), BlynkProtocol.BLYNK_ILLEGAL_COMMAND_BODY);
            }
            
        } else {
            log.warn("[Blynk] Unknown hardware command: {}", command);
            sendResponse(ctx, msg.getMessageId(), BlynkProtocol.BLYNK_ILLEGAL_COMMAND);
        }
    }
    
    /**
     * Handle INTERNAL command (property set, etc)
     */
    private void handleInternal(ChannelHandlerContext ctx, BlynkMessage msg) {
        // For now, just acknowledge
        sendResponse(ctx, msg.getMessageId(), BlynkProtocol.BLYNK_SUCCESS);
    }
    
    /**
     * Handle BRIDGE command (device-to-device communication)
     */
    private void handleBridge(ChannelHandlerContext ctx, BlynkMessage msg) {
        // Not implemented yet
        sendResponse(ctx, msg.getMessageId(), BlynkProtocol.BLYNK_NOT_ALLOWED);
    }
    
    /**
     * Send response message
     */
    private void sendResponse(ChannelHandlerContext ctx, int messageId, short status) {
        BlynkMessage response = BlynkMessage.response(messageId, status);
        ctx.writeAndFlush(response);
    }
    
    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        String token = authenticatedSessions.remove(ctx);
        
        // Remove from active connections
        if (token != null) {
            String[] tokenInfo = tokenValidator.extractTokenInfo(token);
            if (tokenInfo != null) {
                try {
                    long deviceId = Long.parseLong(tokenInfo[2]);
                    activeConnections.remove(deviceId);
                    log.info("[Blynk] Device {} removed from active connections", deviceId);
                } catch (NumberFormatException e) {
                    // ignore
                }
            }
        }
        
        log.warn("[Blynk] Channel INACTIVE (client disconnected), was authenticated: {}, remote: {}", 
            token != null, ctx.channel().remoteAddress());
        super.channelInactive(ctx);
    }
    
    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
        // DO NOT call ctx.flush() here - writeAndFlush already handles it
        // Just pass through to parent
        super.channelReadComplete(ctx);
    }
    
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        log.error("[Blynk] Exception caught, closing channel", cause);
        ctx.close();
    }
}
