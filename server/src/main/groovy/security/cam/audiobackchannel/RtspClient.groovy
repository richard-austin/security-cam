package security.cam.audiobackchannel

import io.netty.bootstrap.Bootstrap
import io.netty.channel.Channel
import io.netty.channel.ChannelInitializer
import io.netty.channel.ChannelPipeline
import io.netty.channel.EventLoopGroup
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.channel.socket.SocketChannel
import io.netty.channel.socket.nio.NioSocketChannel
import io.netty.handler.codec.http.HttpObjectAggregator
import io.netty.handler.codec.rtsp.RtspDecoder
import io.netty.handler.codec.rtsp.RtspEncoder
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class RtspClient {
    private static final String USERNAME = "user"
    private static final String PASSWORD = "pass"
    private final static Logger LOGGER = LoggerFactory.getLogger(RtspClient.class)
    private final static String url = "rtsp://192.168.1.43:554"

    static void main(String[] args) throws InterruptedException, UnsupportedEncodingException {
        sendReq()
    }

    static void sendReq() throws InterruptedException {
        final String pipeName = "/home/richard/opfifo"
        final int BUFFER_SIZE = 2048

        Channel ch = start("192.168.1.43", 554)
    }

    static Channel start(String ip, int port) {
        try {
            Bootstrap rtspClient = new Bootstrap()
            EventLoopGroup workerGroup = new NioEventLoopGroup(1); // todo: avoid possible nonceCount race
            final ClientHandler handler = new ClientHandler(url)
            Channel ch = null

            rtspClient.group(workerGroup).channel(NioSocketChannel.class)
                    .handler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel ch1) {
                            ChannelPipeline p = ch1.pipeline()
                            p.addLast("decoder", new RtspDecoder())
                            p.addLast("encoder", new RtspEncoder())
                            p.addLast("aggregator", new HttpObjectAggregator(512 * 1024))
                            p.addLast(new NettyHttpAuthenticator())
                            p.addLast(handler)
                        }
                    })
            rtspClient.remoteAddress(ip, port)
            try {
                ch = rtspClient.connect(ip, port).sync().channel()
                handler.start(ch)
            } catch (Exception e) {
                System.out.println("Error " + e)
            }
            return ch
        }
        catch(Exception ex) {
            System.out.println("${ex.getClass().getName()}: ${ex.getMessage()}")
        }
    }

}
