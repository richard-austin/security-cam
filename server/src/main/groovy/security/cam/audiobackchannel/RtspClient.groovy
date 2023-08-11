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
import security.cam.LogService
import security.cam.interfaceobjects.MessageCallback
import security.cam.interfaceobjects.OnReady
import server.Camera

import java.util.concurrent.TimeUnit

class RtspClient {
    private final String username
    private final String password
    private final String host
    private final int port
    private final Camera cam
    private final String url
    private final boolean discoveryMode
    Channel ch
    EventLoopGroup workerGroup
    LogService logService

    Process audioOutProc
    BackchannelClientHandler handler = null

    RtspClient(String host, int port, String username, String password, LogService logService, Camera cam = null) {
        this.username = username
        this.password = password
        this.host = host
        this.port = port
        url = "rtsp://" + host + ":" + port.toString()
        this.logService = logService
        this.cam = cam
        this.discoveryMode = cam != null
    }

    void start() {
        try {
            Bootstrap rtspClient = new Bootstrap()
            handler = new BackchannelClientHandler(url)
            handler.discoveryMode = discoveryMode
            handler.setMessageCallback(new MessageCallback() {
                @Override
                void callback(String message, Throwable ex) {
                  //  System.out.println(message)
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
            rtspClient.remoteAddress(host, port)
            rtspClient.validate()
            try {
                ch = rtspClient.connect().sync().channel()
                if (!discoveryMode) {
                    handler.setOnReady(new OnReady() {
                        @Override
                        void ready(BackchannelClientHandler h) {
                            String localAddr
                            // Get preferred local address
                            try (final DatagramSocket socket = new DatagramSocket()) {
                                socket.connect(InetAddress.getByName("8.8.8.8"), 10002)
                                localAddr = socket.getLocalAddress().getHostAddress()
                            }
                            final

                            def audioEncoding = h.getRTPAudioEncoding() == "PCMU" ? "pcm_mulaw" :
                                    h.getRTPAudioEncoding() == "PCMA" ? "pcm_alaw" : "copy"
                            // Start ffmpeg to transcode and transfer the audio data
                            List<String> command = new ArrayList()
                            command.add("/usr/local/bin/ffmpeg")
                            command.add("-i")
                            command.add("tcp://localhost:8881")
                            command.add("-af")
                            command.add("silenceremove=1:0:-50dB")
                            command.add("-c:a")
                            command.add(audioEncoding)
                            command.add("-ar")
                            command.add("${h.getRTPAudioClockRate()}".toString())
                            command.add("-ac")
                            command.add("1")
                            command.add("-f")
                            command.add("rtp")
                            command.add("rtp://${h.getOriginAddress()}:${h.getServerPort()}?localaddr=${localAddr}&localport=${h.vacantPorts.port1}&streamtype=unicast".toString())
                            // Using host rather than h.getOriginAddress() as the latter often came wrong from the device

                            audioOutProc = new ProcessBuilder(command).start()
                            System.out.println("Ready! ${h.getRTPAudioEncoding()}")
                        }
                    })
                } else {
                    handler.setOnReady(new OnReady() {
                        @Override
                        void ready(BackchannelClientHandler handler) {
                            if (cam != null) {
                                cam.backchannelAudioSupported = handler.backchannelAudioSupported
                                cam.rtspTransport = handler.backchannelAudioSupported ? "udp" : "tcp"
                            }
                        }
                    })
                }
                handler.start(ch)
            } catch (Exception e) {
                System.out.println(e.getClass().getName() + ": in start, " + e.getMessage() + "\n" + e.getStackTrace())
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
            if (!proc.waitFor(2, TimeUnit.SECONDS)) {
                // OK so it won't die so get out the heavy mob
                new ProcessBuilder("kill", "-KILL", audioOutProc.pid().toString()).start()
                proc.waitFor()
            }
        }
        ch.close().sync()
        workerGroup.shutdownGracefully()
    }
}
