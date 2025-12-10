package cloud.cydc.netty;

import cloud.cydc.service.DeviceService;
import cloud.cydc.model.Device;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Very small line-based protocol handler.
 * Commands:
 * PING -> PONG
 * REGISTER <id> <name> [meta-json] -> OK
 * GET <id> -> JSON or NOT_FOUND
 */
public class DeviceSessionHandler extends SimpleChannelInboundHandler<String> {
    private static final Logger log = LoggerFactory.getLogger(DeviceSessionHandler.class);
    private final DeviceService deviceService;

    public DeviceSessionHandler(DeviceService service) {
        this.deviceService = service;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, String msg) throws Exception {
        String line = msg.trim();
        if (line.isEmpty()) return;
        log.info("Received: {}", line);
        String[] parts = line.split(" ", 4);
        String cmd = parts[0].toUpperCase();
        switch (cmd) {
            case "PING":
                ctx.writeAndFlush("PONG\n");
                break;
            case "REGISTER": {
                if (parts.length < 3) {
                    ctx.writeAndFlush("ERROR missing args\n");
                    break;
                }
                String id = parts[1];
                String name = parts[2];
                String meta = parts.length >= 4 ? parts[3] : "";
                Device d = new Device(id, name, meta);
                deviceService.register(d);
                ctx.writeAndFlush("OK\n");
                break;
            }
            case "GET": {
                if (parts.length < 2) {
                    ctx.writeAndFlush("ERROR missing args\n");
                    break;
                }
                String id = parts[1];
                Device d = deviceService.get(id);
                if (d == null) ctx.writeAndFlush("NOT_FOUND\n");
                else ctx.writeAndFlush(d.getMeta() + "\n");
                break;
            }
            default:
                ctx.writeAndFlush("ERR unknown\n");
        }
    }
}
