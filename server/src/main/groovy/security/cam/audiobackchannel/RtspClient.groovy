package security.cam.audiobackchannel

import grails.core.GrailsApplication
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
import security.cam.LogService
import security.cam.interfaceobjects.MessageCallback
import security.cam.interfaceobjects.OnReady

class RtspClient {
    private final String username
    private final String password
    private final String host
    private final int port

    private final String url
    GrailsApplication grailsApplication
    Channel ch
    EventLoopGroup workerGroup
    LogService logService

    Process audioOutProc
    BackchannelClientHandler handler
    RtspClient(String host, int port, String username, String password, LogService logService) {
        this.username = username
        this.password = password
        this.host = host
        this.port = port
        url = "rtsp://" + host + ":" + port.toString()
        this.logService = logService
    }

   void sendReq(GrailsApplication grailApplication) {
        this.grailsApplication = grailApplication
        start(host, port)
    }

    void start(String ip, int port) {
        try {
            Bootstrap rtspClient = new Bootstrap()
            handler = new BackchannelClientHandler(url)
            handler.setMessageCallback(new MessageCallback() {
                @Override
                void callback(String message, Throwable ex) {
                    System.out.println(message)
                    if (ex != null) {
                        System.out.println(ex.getClass().getName() + ": " + ex.getMessage())
                    }
                }
            })
            workerGroup = new NioEventLoopGroup(1) // todo: avoid possible nonceCount race
            rtspClient.group(workerGroup).channel(NioSocketChannel.class)
                    .handler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel ch1) {
                            ChannelPipeline p = ch1.pipeline()
                            p.addLast("encoder", new RtspEncoder())
                            p.addLast("decoder", new RtspDecoder())
                            p.addLast("aggregator", new HttpObjectAggregator(4 * 1024))
                            p.addLast(new NettyHttpAuthenticator(username, password))
                            p.addLast(handler)
                        }
                    })
            rtspClient.remoteAddress(ip, port)
            rtspClient.validate()
            try {
                ch = rtspClient.connect().sync().channel()
                handler.setOnReady(new OnReady() {
                    @Override
                    void ready(BackchannelClientHandler h) {
                        String localAddr
                        // Get preferred local address
                        try(final DatagramSocket socket = new DatagramSocket()){
                            socket.connect(InetAddress.getByName("8.8.8.8"), 10002);
                            localAddr = socket.getLocalAddress().getHostAddress();
                        }
                        final
                        String fifoPath = grailsApplication.config.twoWayAudio.fifo
                        def audioEncoding = h.getRTPAudioEncoding() == "PCMU" ? "pcm_mulaw" :
                                h.getRTPAudioEncoding() == "PCMA" ? "pcm_alaw" : "copy"
                        // Start ffmpeg to transcode and transfer the audio data
                        List<String> command = new ArrayList()
                        command.add("ffmpeg")
                        command.add("-i")
                        command.add(fifoPath)
                        command.add("-c:a")
                        command.add(audioEncoding)
                        command.add("-ar")
                        command.add("${h.getRTPAudioClockRate()}".toString())
                        command.add("-f")
                        command.add("rtp")
                        command.add("rtp://${h.getOriginAddress()}:${h.getServerPort()}?localaddr=${localAddr}&localport=${h.vacantPorts.port1}&streamtype=unicast".toString())
                        audioOutProc = new ProcessBuilder(command).inheritIO().start()

                        System.out.println("Ready! ${h.getRTPAudioEncoding()}")
                    }
                })
                handler.start(ch)
            } catch (Exception e) {
                System.out.println(e.getClass().getName()+": in start, "+e.getMessage() +"\n"+e.getStackTrace())
            }
        }
        catch (Exception ex) {
            System.out.println("${ex.getClass().getName()}: ${ex.getMessage()}")
        }
     }

    void stop() {
        handler.endSession()
        // Stop the ffmpeg process
        if (audioOutProc) {
            def proc = new ProcessBuilder("kill", "-INT", audioOutProc.pid().toString()).start()
            proc.waitFor()
        }
        ch.close().sync()
        workerGroup.shutdownGracefully()
    }
}
