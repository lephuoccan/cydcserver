package cloud.cydc.blynk;

import cloud.cydc.service.DeviceInfoService;
import cloud.cydc.service.VirtualPinService;
import cloud.cydc.util.TokenValidator;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Dedicated Blynk Protocol Server for ESP32 devices
 * Runs on separate port from text-based protocol
 */
public class BlynkServer {
    private static final Logger log = LoggerFactory.getLogger(BlynkServer.class);
    private final int port;
    private final DeviceInfoService deviceService;
    private final VirtualPinService pinService;
    private final TokenValidator tokenValidator;
    private EventLoopGroup bossGroup;
    private EventLoopGroup workerGroup;

    public BlynkServer(int port, 
                       DeviceInfoService deviceService, 
                       VirtualPinService pinService,
                       TokenValidator tokenValidator) {
        this.port = port;
        this.deviceService = deviceService;
        this.pinService = pinService;
        this.tokenValidator = tokenValidator;
    }

    public void start() throws InterruptedException {
        bossGroup = new NioEventLoopGroup(1);
        workerGroup = new NioEventLoopGroup();
        try {
            ServerBootstrap b = new ServerBootstrap();
            b.group(bossGroup, workerGroup)
             .channel(NioServerSocketChannel.class)
             .childHandler(new ChannelInitializer<SocketChannel>() {
                 @Override
                 protected void initChannel(SocketChannel ch) throws Exception {
                     ChannelPipeline p = ch.pipeline();
                     // Binary protocol encoder/decoder
                     p.addLast(new BlynkMessageDecoder());
                     p.addLast(new BlynkMessageEncoder());
                     // Business logic handler
                     p.addLast(new BlynkProtocolHandler(deviceService, pinService, tokenValidator));
                 }
             });

            b.bind(port).sync();
            log.info("Blynk protocol server started on port {}", port);
        } catch (InterruptedException e) {
            throw e;
        }
    }

    public void stop() {
        if (bossGroup != null) bossGroup.shutdownGracefully();
        if (workerGroup != null) workerGroup.shutdownGracefully();
        log.info("Blynk protocol server stopped");
    }
}
