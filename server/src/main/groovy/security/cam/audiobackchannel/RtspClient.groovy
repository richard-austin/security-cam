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
import security.cam.interfaceobjects.MessageCallback
import security.cam.interfaceobjects.OnReady

class RtspClient {
    private final String username
    private final String password
    private final static Logger LOGGER = LoggerFactory.getLogger(RtspClient.class)
    private final String host
    private final int port

    private final String url

    RtspClient(String host, int port, String username, String password) {
        this.username = username
        this.password = password
        this.host = host
        this.port = port
        url = "rtsp://" + host + ":" + port.toString()
    }

    static void main(String[] args) throws InterruptedException, UnsupportedEncodingException {
        RtspClient client = new RtspClient("192.168.1.43", 554, "admin", "R@nc1dTapsB0ttom")
        client.sendReq()
    }

    void sendReq() {
        final String pipeName = "/home/richard/opfifo"
        start(host, port)
    }

    void start(String ip, int port) {
        try {
            Bootstrap rtspClient = new Bootstrap()
            EventLoopGroup workerGroup = new NioEventLoopGroup(1) // todo: avoid possible nonceCount race
            final BackchannelClientHandler handler = new BackchannelClientHandler(url)
            Channel ch
            handler.setMessageCallback(new MessageCallback() {
                @Override
                void callback(String message, Throwable ex) {
                    System.out.println(message)
                    if (ex != null) {
                        System.out.println(ex.getClass().getName() + ": " + ex.getMessage())
                    }
                }
            })

            rtspClient.group(workerGroup).channel(NioSocketChannel.class)
                    .handler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel ch1) {
                            ChannelPipeline p = ch1.pipeline()
                            p.addLast("decoder", new RtspDecoder())
                            p.addLast("encoder", new RtspEncoder())
                            p.addLast("aggregator", new HttpObjectAggregator(4 * 1024))
                            p.addLast(new NettyHttpAuthenticator(username, password))
                            p.addLast(handler)
                        }
                    })
            rtspClient.remoteAddress(ip, port)
            try {
                ch = rtspClient.connect(ip, port).sync().channel()
                handler.setOnReady(new OnReady() {
                    @Override
                    void ready(BackchannelClientHandler h) {
                        System.out.println("Ready! ${h.getRTPAudioEncoding()}")
                        def x = h.getMediaField()
                    }
                })
                handler.start(ch)
            } catch (Exception e) {
                System.out.println("Error " + e)
            }
        }
        catch (Exception ex) {
            System.out.println("${ex.getClass().getName()}: ${ex.getMessage()}")
        }
    }
}
