package cloud.cydc.http;

import cloud.cydc.model.Dashboard;
import cloud.cydc.model.DeviceInfo;
import cloud.cydc.service.DashboardService;
import cloud.cydc.service.DeviceInfoService;
import cloud.cydc.service.UsersService;
import cloud.cydc.service.VirtualPinService;
import cloud.cydc.util.TokenValidator;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaderValues;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpUtil;

import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static io.netty.handler.codec.http.HttpResponseStatus.*;
import static io.netty.handler.codec.http.HttpVersion.HTTP_1_1;

public class HttpRequestHandler extends SimpleChannelInboundHandler<FullHttpRequest> {
    private final UsersService usersService;
    private final DashboardService dashboardService;
    private final DeviceInfoService deviceService;
    private final VirtualPinService pinService;
    private final TokenValidator tokenValidator;
    private final ObjectMapper mapper = new ObjectMapper();

    public HttpRequestHandler(UsersService usersService, DashboardService dashboardService, 
                              DeviceInfoService deviceService, VirtualPinService pinService) {
        this.usersService = usersService;
        this.dashboardService = dashboardService;
        this.deviceService = deviceService;
        this.pinService = pinService;
        this.tokenValidator = new TokenValidator(deviceService);
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, FullHttpRequest req) throws Exception {
        try {
            String fullUri = req.uri();
            // Strip query string for pattern matching
            String uri = fullUri.contains("?") ? fullUri.substring(0, fullUri.indexOf("?")) : fullUri;
            String method = req.method().name();
            System.out.println("[HttpHandler] " + method + " " + fullUri);

            // User APIs
            if (uri.equals("/api/register") && "POST".equals(method)) {
                byte[] bytes = new byte[req.content().readableBytes()];
                req.content().readBytes(bytes);
                var node = mapper.readTree(bytes);
                String email = node.path("email").asText(null);
                String password = node.path("pass").asText(null);
                String appName = node.path("appName").asText("Blynk");
                
                if (email == null || password == null) {
                    writeJson(ctx, BAD_REQUEST, "{\"error\":\"email and pass are required\"}", req);
                    return;
                }
                
                try {
                    String userId = usersService.register(email, password, appName);
                    writeJson(ctx, CREATED, "{\"status\":\"ok\",\"userId\":\"" + userId + "\"}", req);
                } catch (IllegalArgumentException e) {
                    writeJson(ctx, CONFLICT, "{\"error\":\"" + e.getMessage() + "\"}", req);
                }
                return;
            }

            if (uri.equals("/api/login") && "POST".equals(method)) {
                byte[] bytes = new byte[req.content().readableBytes()];
                req.content().readBytes(bytes);
                var node = mapper.readTree(bytes);
                String id = node.path("id").asText(null);
                String pass = node.path("pass").asText(null);
                if (id == null || pass == null) {
                    writeJson(ctx, BAD_REQUEST, "{\"error\":\"missing id or pass\"}", req);
                    return;
                }
                boolean ok = usersService.checkLogin(id, pass);
                if (ok) {
                    String json = usersService.findJsonById(id);
                    if (json != null) {
                        // Remove password before returning
                        var userNode = mapper.readTree(json);
                        ((com.fasterxml.jackson.databind.node.ObjectNode) userNode).remove("pass");
                        json = mapper.writeValueAsString(userNode);
                    }
                    writeJson(ctx, OK, json == null ? "{}" : json, req);
                } else {
                    writeJson(ctx, UNAUTHORIZED, "{\"error\":\"invalid credentials\"}", req);
                }
                return;
            }

            Pattern deleteUserPattern = Pattern.compile("/api/user/([^/]+)");
            Matcher deleteUserMatcher = deleteUserPattern.matcher(uri);
            if (deleteUserMatcher.matches() && "DELETE".equals(method)) {
                String userId = deleteUserMatcher.group(1);
                boolean deleted = usersService.delete(userId);
                if (deleted) writeJson(ctx, OK, "{\"status\":\"deleted\"}", req);
                else writeJson(ctx, NOT_FOUND, "{\"error\":\"not_found\"}", req);
                return;
            }

            // Dashboard APIs
            Pattern dashCreatePattern = Pattern.compile("/api/dashboard/([^/]+)");
            Matcher dashCreateMatcher = dashCreatePattern.matcher(uri);
            if (dashCreateMatcher.matches() && "POST".equals(method)) {
                String userId = dashCreateMatcher.group(1);
                byte[] bytes = new byte[req.content().readableBytes()];
                req.content().readBytes(bytes);
                Dashboard d = mapper.readValue(bytes, Dashboard.class);
                dashboardService.createOrUpdate(userId, d);
                writeJson(ctx, CREATED, "{\"status\":\"ok\"}", req);
                return;
            }

            Pattern dashGetPattern = Pattern.compile("/api/dashboard/([^/]+)/([0-9]+)");
            Matcher dashGetMatcher = dashGetPattern.matcher(uri);
            if (dashGetMatcher.matches() && "GET".equals(method)) {
                String userId = dashGetMatcher.group(1);
                long dashId = Long.parseLong(dashGetMatcher.group(2));
                String json = dashboardService.findJsonById(userId, dashId);
                if (json != null) writeJson(ctx, OK, json, req);
                else writeJson(ctx, NOT_FOUND, "{\"error\":\"not_found\"}", req);
                return;
            }

            Pattern dashDeletePattern = Pattern.compile("/api/dashboard/([^/]+)/([0-9]+)");
            Matcher dashDeleteMatcher = dashDeletePattern.matcher(uri);
            if (dashDeleteMatcher.matches() && "DELETE".equals(method)) {
                String userId = dashDeleteMatcher.group(1);
                long dashId = Long.parseLong(dashDeleteMatcher.group(2));
                if (dashboardService.delete(userId, dashId)) writeJson(ctx, OK, "{\"status\":\"deleted\"}", req);
                else writeJson(ctx, NOT_FOUND, "{\"error\":\"not_found\"}", req);
                return;
            }

            // Device APIs
            Pattern deviceCreatePattern = Pattern.compile("/api/device/([^/]+)/([0-9]+)");
            Matcher deviceCreateMatcher = deviceCreatePattern.matcher(uri);
            if (deviceCreateMatcher.matches() && "POST".equals(method)) {
                String userId = deviceCreateMatcher.group(1);
                long dashId = Long.parseLong(deviceCreateMatcher.group(2));
                byte[] bytes = new byte[req.content().readableBytes()];
                req.content().readBytes(bytes);
                DeviceInfo d = mapper.readValue(bytes, DeviceInfo.class);
                
                // Generate token if not provided
                if (d.getToken() == null || d.getToken().isEmpty()) {
                    String randomPart = deviceService.generateNewToken();
                    String fullToken = userId + "-" + dashId + "-" + d.getId() + "-" + randomPart;
                    d = new DeviceInfo(d.getId(), d.getName(), d.getBoardType(), fullToken,
                        d.getVendor(), d.getConnectionType(), d.getStatus(), d.getDisconnectTime(),
                        d.getConnectTime(), d.getFirstConnectTime(), d.getDataReceivedAt(),
                        d.getLastLoggedIP(), d.getHardwareInfo(), d.isUserIcon());
                }
                
                deviceService.createOrUpdate(userId, dashId, d);
                String json = deviceService.findJsonById(userId, dashId, d.getId());
                writeJson(ctx, CREATED, json != null ? json : "{\"status\":\"ok\"}", req);
                return;
            }

            Pattern deviceGetPattern = Pattern.compile("/api/device/([^/]+)/([0-9]+)/([0-9]+)");
            Matcher deviceGetMatcher = deviceGetPattern.matcher(uri);
            if (deviceGetMatcher.matches() && "GET".equals(method)) {
                String userId = deviceGetMatcher.group(1);
                long dashId = Long.parseLong(deviceGetMatcher.group(2));
                long devId = Long.parseLong(deviceGetMatcher.group(3));
                String json = deviceService.findJsonById(userId, dashId, devId);
                if (json != null) writeJson(ctx, OK, json, req);
                else writeJson(ctx, NOT_FOUND, "{\"error\":\"not_found\"}", req);
                return;
            }

            Pattern deviceDeletePattern = Pattern.compile("/api/device/([^/]+)/([0-9]+)/([0-9]+)");
            Matcher deviceDeleteMatcher = deviceDeletePattern.matcher(uri);
            if (deviceDeleteMatcher.matches() && "DELETE".equals(method)) {
                String userId = deviceDeleteMatcher.group(1);
                long dashId = Long.parseLong(deviceDeleteMatcher.group(2));
                long devId = Long.parseLong(deviceDeleteMatcher.group(3));
                if (deviceService.delete(userId, dashId, devId)) writeJson(ctx, OK, "{\"status\":\"deleted\"}", req);
                else writeJson(ctx, NOT_FOUND, "{\"error\":\"not_found\"}", req);
                return;
            }

            // Regenerate token for device
            Pattern tokenPattern = Pattern.compile("/api/token/([^/]+)/([0-9]+)/([0-9]+)");
            Matcher tokenMatcher = tokenPattern.matcher(uri);
            if (tokenMatcher.matches() && "POST".equals(method)) {
                String userId = tokenMatcher.group(1);
                long dashId = Long.parseLong(tokenMatcher.group(2));
                long devId = Long.parseLong(tokenMatcher.group(3));
                String newToken = deviceService.generateNewToken();
                String json = deviceService.findJsonById(userId, dashId, devId);
                if (json != null) {
                    var d = mapper.readValue(json, DeviceInfo.class);
                       // Create new DeviceInfo with updated token
                       String updatedToken = userId + "-" + dashId + "-" + devId + "-" + newToken;
                       var updatedD = new DeviceInfo(d.getId(), d.getName(), d.getBoardType(), updatedToken, 
                           d.getVendor(), d.getConnectionType(), d.getStatus(), d.getDisconnectTime(),
                           d.getConnectTime(), d.getFirstConnectTime(), d.getDataReceivedAt(),
                           d.getLastLoggedIP(), d.getHardwareInfo(), d.isUserIcon());
                       deviceService.createOrUpdate(userId, dashId, updatedD);
                       writeJson(ctx, OK, "{\"token\":\"" + updatedToken + "\"}", req);
                    } else {
                       writeJson(ctx, NOT_FOUND, "{\"error\":\"device not found\"}", req);
                }
                return;
            }

            // Virtual Pin APIs - RESTful style
            if (uri.equals("/api/pins") && "PUT".equals(method)) {
                // Extract and validate token from header
                String token = extractToken(req);
                if (token == null || !tokenValidator.validateToken(token)) {
                    writeJson(ctx, UNAUTHORIZED, "{\"error\":\"invalid or missing token\"}", req);
                    return;
                }
                
                byte[] bytes = new byte[req.content().readableBytes()];
                req.content().readBytes(bytes);
                var node = mapper.readTree(bytes);
                String pin = node.path("pin").asText(null);
                String value = node.path("value").asText("0");
                
                if (pin == null || !pin.startsWith("V")) {
                    writeJson(ctx, BAD_REQUEST, "{\"error\":\"invalid pin format, expected V{num}\"}", req);
                    return;
                }
                
                int pinNum = Integer.parseInt(pin.substring(1));
                if (pinNum < 0 || pinNum >= 128) {
                    writeJson(ctx, BAD_REQUEST, "{\"error\":\"pin out of range\"}", req);
                    return;
                }
                
                String[] tokenInfo = tokenValidator.extractTokenInfo(token);
                if (tokenInfo != null) {
                    long devId = Long.parseLong(tokenInfo[2]);
                    String userId = tokenInfo[0];
                    pinService.setPinValueWithBroadcast(userId, String.valueOf(devId), pinNum, value);
                } else {
                    writeJson(ctx, BAD_REQUEST, "{\"error\":\"invalid token format\"}", req);
                    return;
                }
                writeJson(ctx, OK, "{\"status\":\"ok\",\"pin\":\"" + pin + "\",\"value\":\"" + value + "\"}", req);
                return;
            }
            
            // Virtual Pin APIs - Legacy path style
            Pattern pinSetPattern = Pattern.compile("/api/pin/([0-9]+)/V([0-9]+)");
            Matcher pinSetMatcher = pinSetPattern.matcher(uri);
            if (pinSetMatcher.matches() && "PUT".equals(method)) {
                long devId = Long.parseLong(pinSetMatcher.group(1));
                int pinNum = Integer.parseInt(pinSetMatcher.group(2));
                if (pinNum < 0 || pinNum >= 128) {
                    writeJson(ctx, BAD_REQUEST, "{\"error\":\"pin out of range\"}", req);
                    return;
                }
                
                // Extract and validate token from header or query parameter
                String token = extractToken(req);
                if (token == null || !tokenValidator.validateToken(token)) {
                    writeJson(ctx, UNAUTHORIZED, "{\"error\":\"invalid or missing token\"}", req);
                    return;
                }
                
                byte[] bytes = new byte[req.content().readableBytes()];
                req.content().readBytes(bytes);
                var node = mapper.readTree(bytes);
                String value = node.isArray() && node.size() > 0 ? node.get(0).asText("0") : node.path("value").asText("0");
                
                String[] tokenInfo = tokenValidator.extractTokenInfo(token);
                if (tokenInfo != null) {
                    String userId = tokenInfo[0];
                    // Broadcast pin update to WebSocket subscribers
                    pinService.setPinValueWithBroadcast(userId, String.valueOf(devId), pinNum, value);
                } else {
                    pinService.setPinValue(devId, pinNum, value);
                }
                writeJson(ctx, OK, "{\"status\":\"ok\"}", req);
                return;
            }

            Pattern pinGetPattern = Pattern.compile("/api/pin/([0-9]+)/V([0-9]+)");
            Matcher pinGetMatcher = pinGetPattern.matcher(uri);
            if (pinGetMatcher.matches() && "GET".equals(method)) {
                long devId = Long.parseLong(pinGetMatcher.group(1));
                int pinNum = Integer.parseInt(pinGetMatcher.group(2));
                if (pinNum < 0 || pinNum >= 128) {
                    writeJson(ctx, BAD_REQUEST, "{\"error\":\"pin out of range\"}", req);
                    return;
                }
                
                // Extract token from query params or header
                String token = extractToken(req);
                if (token == null || !tokenValidator.validateToken(token)) {
                    writeJson(ctx, UNAUTHORIZED, "{\"error\":\"invalid or missing token\"}", req);
                    return;
                }
                
                String value = pinService.getPinValue(devId, pinNum);
                if (value == null) value = "0";
                writeJson(ctx, OK, "{\"pin\":\"V" + pinNum + "\",\"value\":\"" + value + "\"}", req);
                return;
            }

            Pattern allPinsPattern = Pattern.compile("/api/pin/([0-9]+)");
            Matcher allPinsMatcher = allPinsPattern.matcher(uri);
            if (allPinsMatcher.matches() && "GET".equals(method)) {
                long devId = Long.parseLong(allPinsMatcher.group(1));
                
                // Extract token from query params or header
                String token = extractToken(req);
                if (token == null || !tokenValidator.validateToken(token)) {
                    writeJson(ctx, UNAUTHORIZED, "{\"error\":\"invalid or missing token\"}", req);
                    return;
                }
                
                String json = pinService.getAllPinsJson(devId);
                writeJson(ctx, OK, json, req);
                return;
            }

            if (uri.equals("/api/stat") && "GET".equals(method)) {
                Runtime runtime = Runtime.getRuntime();
                long memUsed = (runtime.totalMemory() - runtime.freeMemory()) / 1024 / 1024; // MB
                long memMax = runtime.maxMemory() / 1024 / 1024; // MB
                
                // Lấy pin service stats
                var pinStats = pinService.getStats();
                
                String stats = "{" +
                    "\"uptime_sec\":" + (System.currentTimeMillis() / 1000) + "," +
                    "\"memory_used_mb\":" + memUsed + "," +
                    "\"memory_max_mb\":" + memMax + "," +
                    "\"java_version\":\"" + System.getProperty("java.version") + "\"," +
                    "\"timestamp\":" + System.currentTimeMillis() + "," +
                    "\"pin_stats\":" + mapper.writeValueAsString(pinStats) +
                    "}";
                writeJson(ctx, OK, stats, req);
                return;
            }

            if (uri.equals("/api/health") && "GET".equals(method)) {
                writeJson(ctx, OK, "{\"status\":\"ok\"}", req);
                return;
            }
            
            // Debug endpoint to check stored token
            Pattern debugTokenPattern = Pattern.compile("/api/debug/token/([^/]+)/([0-9]+)/([0-9]+)");
            Matcher debugTokenMatcher = debugTokenPattern.matcher(uri);
            if (debugTokenMatcher.matches() && "GET".equals(method)) {
                String userId = debugTokenMatcher.group(1);
                long dashId = Long.parseLong(debugTokenMatcher.group(2));
                long devId = Long.parseLong(debugTokenMatcher.group(3));
                
                // Try both methods
                String storedToken = deviceService.findTokenByDeviceId(userId, dashId, devId);
                String deviceJson = deviceService.findJsonById(userId, dashId, devId);
                String directToken = null;
                if (deviceJson != null) {
                    try {
                        var node = mapper.readTree(deviceJson);
                        directToken = node.path("token").asText(null);
                    } catch (Exception e) {
                        directToken = "error: " + e.getMessage();
                    }
                }
                
                writeJson(ctx, OK, "{\"storedToken\":\"" + (storedToken != null ? storedToken : "null") + 
                    "\",\"directToken\":\"" + (directToken != null ? directToken : "null") + 
                    "\",\"jsonLength\":" + (deviceJson != null ? deviceJson.length() : 0) + "}", req);
                return;
            }

            writeJson(ctx, NOT_FOUND, "{\"error\":\"not_found\"}", req);
        } catch (Exception e) {
            System.out.println("[HttpHandler] Exception: " + e.getMessage());
            e.printStackTrace();
            try {
                writeJson(ctx, INTERNAL_SERVER_ERROR, "{\"error\":\"Internal server error\"}", req);
            } catch (Exception ex) {
                System.err.println("[HttpHandler] Failed to write error response: " + ex.getMessage());
            }
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        System.out.println("[HttpHandler] exceptionCaught: " + cause.getMessage());
        cause.printStackTrace();
        ctx.close();
    }

    private void writeJson(ChannelHandlerContext ctx, HttpResponseStatus status, String body, FullHttpRequest req) {
        FullHttpResponse resp = new DefaultFullHttpResponse(HTTP_1_1, status, Unpooled.copiedBuffer(body, StandardCharsets.UTF_8));
        resp.headers().set(HttpHeaderNames.CONTENT_TYPE, "application/json; charset=UTF-8");
        resp.headers().setInt(HttpHeaderNames.CONTENT_LENGTH, resp.content().readableBytes());
        if (!HttpUtil.isKeepAlive(req)) {
            ctx.writeAndFlush(resp).addListener(ChannelFutureListener.CLOSE);
        } else {
            resp.headers().set(HttpHeaderNames.CONNECTION, HttpHeaderValues.KEEP_ALIVE);
            ctx.writeAndFlush(resp);
        }
    }

    private String extractToken(FullHttpRequest req) {
        String query = req.uri();
        if (query.contains("?")) {
            String queryString = query.substring(query.indexOf("?") + 1);
            String[] params = queryString.split("&");
            for (String param : params) {
                if (param.startsWith("token=")) {
                    return param.substring(6);
                }
            }
        }
        // Try Authorization header (support both "Bearer token" and direct token)
        String authHeader = req.headers().get(HttpHeaderNames.AUTHORIZATION);
        if (authHeader != null && !authHeader.isEmpty()) {
            if (authHeader.startsWith("Bearer ")) {
                return authHeader.substring(7);
            }
            // Return as-is if not Bearer format
            return authHeader;
        }
        return null;
    }
}
