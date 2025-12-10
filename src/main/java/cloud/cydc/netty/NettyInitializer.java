package cloud.cydc.netty;

import cloud.cydc.service.DeviceService;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.LineBasedFrameDecoder;
import io.netty.handler.codec.string.StringDecoder;
import io.netty.handler.codec.string.StringEncoder;
import java.nio.charset.StandardCharsets;

public class NettyInitializer extends ChannelInitializer<SocketChannel> {
    private final DeviceService deviceService;

    public NettyInitializer(DeviceService deviceService) {
        this.deviceService = deviceService;
    }

    @Override
    protected void initChannel(SocketChannel ch) throws Exception {
        ChannelPipeline p = ch.pipeline();
        p.addLast(new LineBasedFrameDecoder(8192));
        p.addLast(new StringDecoder(StandardCharsets.UTF_8));
        p.addLast(new StringEncoder(StandardCharsets.UTF_8));
        p.addLast(new DeviceSessionHandler(deviceService));
    }
}
