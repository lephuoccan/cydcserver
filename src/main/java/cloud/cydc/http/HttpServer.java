package cloud.cydc.http;

import cloud.cydc.service.DashboardService;
import cloud.cydc.service.DeviceInfoService;
import cloud.cydc.service.UsersService;
import cloud.cydc.service.VirtualPinService;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HttpServer {
    private static final Logger log = LoggerFactory.getLogger(HttpServer.class);
    private final int port;
    private final UsersService usersService;
    private final DashboardService dashboardService;
    private final DeviceInfoService deviceService;
    private final VirtualPinService pinService;
    private EventLoopGroup bossGroup;
    private EventLoopGroup workerGroup;
    private Channel serverChannel;

    public HttpServer(int port, UsersService usersService, DashboardService dashboardService,
                      DeviceInfoService deviceService, VirtualPinService pinService) {
        this.port = port;
        this.usersService = usersService;
        this.dashboardService = dashboardService;
        this.deviceService = deviceService;
        this.pinService = pinService;
    }

    public void start() throws InterruptedException {
        bossGroup = new NioEventLoopGroup(1);
        workerGroup = new NioEventLoopGroup();
        ServerBootstrap b = new ServerBootstrap();
        b.group(bossGroup, workerGroup)
         .channel(NioServerSocketChannel.class)
         .childHandler(new ChannelInitializer<SocketChannel>() {
             @Override
             protected void initChannel(SocketChannel ch) throws Exception {
                 ch.pipeline().addLast(new HttpServerCodec());
                 ch.pipeline().addLast(new HttpObjectAggregator(10 * 1024 * 1024));
                 ch.pipeline().addLast(new HttpRequestHandler(usersService, dashboardService, deviceService, pinService));
             }
         })
         .childOption(ChannelOption.SO_KEEPALIVE, true);
        serverChannel = b.bind(port).sync().channel();
        serverChannel.closeFuture().addListener(f -> {
            if (f.cause() != null) {
                log.error("HTTP server channel closed due to error", f.cause());
            } else {
                log.warn("HTTP server channel closed");
            }
        });
        log.info("HTTP server started on port {}", port);
    }

    public void stop() {
        if (serverChannel != null) {
            serverChannel.close();
        }
        if (bossGroup != null) bossGroup.shutdownGracefully();
        if (workerGroup != null) workerGroup.shutdownGracefully();
        log.info("HTTP server stopped");
    }
}
