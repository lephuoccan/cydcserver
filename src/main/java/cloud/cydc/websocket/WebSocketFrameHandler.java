package cloud.cydc.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.websocketx.*;
import io.netty.handler.timeout.IdleStateEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class WebSocketFrameHandler extends SimpleChannelInboundHandler<WebSocketFrame> {
    private static final Logger log = LoggerFactory.getLogger(WebSocketFrameHandler.class);
    private static final ObjectMapper mapper = new ObjectMapper();

    // Global registry: channel -> (userId -> subscribedDeviceIds)
    private static final Map<Channel, WebSocketSession> sessions = new ConcurrentHashMap<>();

    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        log.debug("WebSocket channel connected: {}", ctx.channel().id());
        sessions.put(ctx.channel(), new WebSocketSession());
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        log.debug("WebSocket channel disconnected: {}", ctx.channel().id());
        sessions.remove(ctx.channel());
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        if (evt instanceof IdleStateEvent) {
            IdleStateEvent event = (IdleStateEvent) evt;
            if (event.state().equals(io.netty.handler.timeout.IdleState.ALL_IDLE)) {
                // Send heartbeat ping
                ctx.writeAndFlush(new PingWebSocketFrame()).addListener(ChannelFutureListener.FIRE_EXCEPTION_ON_FAILURE);
                log.debug("Heartbeat ping sent to {}", ctx.channel().id());
            }
        }
        super.userEventTriggered(ctx, evt);
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, WebSocketFrame frame) {
        if (frame instanceof PingWebSocketFrame) {
            ctx.writeAndFlush(new PongWebSocketFrame(frame.content().retain()));
            log.debug("Ping/Pong received: {}", ctx.channel().id());
        } else if (frame instanceof PongWebSocketFrame) {
            log.debug("Pong received: {}", ctx.channel().id());
        } else if (frame instanceof TextWebSocketFrame) {
            handleTextFrame(ctx, (TextWebSocketFrame) frame);
        } else if (frame instanceof BinaryWebSocketFrame) {
            handleBinaryFrame(ctx, (BinaryWebSocketFrame) frame);
        } else if (frame instanceof CloseWebSocketFrame) {
            ctx.writeAndFlush(new CloseWebSocketFrame()).addListener(ChannelFutureListener.CLOSE);
            log.debug("WebSocket close frame received: {}", ctx.channel().id());
        }
    }

    private void handleTextFrame(ChannelHandlerContext ctx, TextWebSocketFrame frame) {
        String text = frame.text();
        log.debug("Text frame received: {}", text);

        try {
            Map<String, Object> msg = mapper.readValue(text, Map.class);
            String command = (String) msg.get("cmd");

            if ("subscribe".equals(command)) {
                String userId = (String) msg.get("userId");
                String deviceId = (String) msg.get("deviceId");
                WebSocketSession session = sessions.get(ctx.channel());
                if (session != null) {
                    session.subscribe(userId, deviceId);
                    log.info("User {} subscribed to device {} on channel {}", userId, deviceId, ctx.channel().id());
                    sendResponse(ctx, "ok", "subscribed");
                }
            } else if ("unsubscribe".equals(command)) {
                String userId = (String) msg.get("userId");
                String deviceId = (String) msg.get("deviceId");
                WebSocketSession session = sessions.get(ctx.channel());
                if (session != null) {
                    session.unsubscribe(userId, deviceId);
                    log.info("User {} unsubscribed from device {} on channel {}", userId, deviceId, ctx.channel().id());
                    sendResponse(ctx, "ok", "unsubscribed");
                }
            } else {
                log.warn("Unknown command: {}", command);
                sendResponse(ctx, "error", "unknown command");
            }
        } catch (Exception e) {
            log.error("Error handling text frame", e);
            sendResponse(ctx, "error", e.getMessage());
        }
    }

    private void handleBinaryFrame(ChannelHandlerContext ctx, BinaryWebSocketFrame frame) {
        log.debug("Binary frame received, length: {}", frame.content().readableBytes());
        // Could implement Blynk binary protocol here if needed
    }

    private void sendResponse(ChannelHandlerContext ctx, String status, String message) {
        try {
            Map<String, String> response = new HashMap<>();
            response.put("status", status);
            response.put("message", message);
            String json = mapper.writeValueAsString(response);
            ctx.writeAndFlush(new TextWebSocketFrame(json));
        } catch (Exception e) {
            log.error("Error sending response", e);
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        log.error("WebSocket error", cause);
        ctx.close();
    }

    // Static helper to broadcast pin updates to subscribed clients
    public static void broadcastPinUpdate(String userId, String deviceId, String pinName, String value) {
        for (Map.Entry<Channel, WebSocketSession> entry : sessions.entrySet()) {
            Channel channel = entry.getKey();
            WebSocketSession session = entry.getValue();
            if (session.isSubscribed(userId, deviceId)) {
                try {
                    Map<String, Object> update = new HashMap<>();
                    update.put("type", "pin_update");
                    update.put("deviceId", deviceId);
                    update.put("pin", pinName);
                    update.put("value", value);
                    update.put("timestamp", System.currentTimeMillis());
                    String json = mapper.writeValueAsString(update);
                    channel.writeAndFlush(new TextWebSocketFrame(json))
                            .addListener(ChannelFutureListener.FIRE_EXCEPTION_ON_FAILURE);
                    log.debug("Pin update broadcast to {}: {}={}", deviceId, pinName, value);
                } catch (Exception e) {
                    log.error("Error broadcasting pin update", e);
                }
            }
        }
    }

    // Static helper to get all active sessions (for monitoring)
    public static Map<Channel, WebSocketSession> getSessions() {
        return new HashMap<>(sessions);
    }
}
