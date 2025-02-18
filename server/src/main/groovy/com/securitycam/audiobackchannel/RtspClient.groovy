package com.securitycam.audiobackchannel

import com.securitycam.controllers.Camera
import com.securitycam.enums.PassFail
import com.securitycam.interfaceobjects.MessageCallback
import com.securitycam.interfaceobjects.ObjectCommandResponse
import com.securitycam.interfaceobjects.OnReady
import com.securitycam.services.LogService
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

import java.util.concurrent.BlockingQueue
import java.util.concurrent.LinkedTransferQueue
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
    final BlockingQueue awaitLock = new LinkedTransferQueue<ObjectCommandResponse>()
    final ObjectCommandResponse response = new ObjectCommandResponse()

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
            handler = new BackchannelClientHandler(url, awaitLock, response)
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
                            p.addLast(new NettyHttpAuthenticator(username, password, logService))
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
                            command.add("/usr/bin/ffmpeg")
                            command.add("-loglevel")
                            command.add("info")
                            command.add("-i")
                            command.add("tcp://localhost:8881")
                            // silenceremove prevents ffmpeg starting output until the input sound exceeds the given level. I'm
                            //  not sure this is helpful. Will need to reassess when I next have an audio backchannel device handy.
//                            command.add("-af")
//                            command.add("silenceremove=1:0:-50dB")
                            command.add("-c:a")
                            command.add(audioEncoding)
                            command.add("-ar")
                            command.add("${h.getRTPAudioClockRate()}".toString())
                            command.add("-ac")
                            command.add("1")
                            command.add("-f")
                            command.add("rtp")
                            command.add("rtp://${host}:${h.getServerPort()}?localaddr=${localAddr}&localport=${h.vacantPorts.port1}&streamtype=unicast".toString())

                            audioOutProc = new ProcessBuilder(command).start()
                            BufferedReader reader = audioOutProc.errorReader()
                            String line
                            new Thread().start(() -> {
                                while ((line = reader.readLine()) != null) {
                                    logService.getCam().info(line)
                                }
                            })

                            logService.getCam().info("RTSPClient ready, backchannel stream set up with ${h.getRTPAudioEncoding()}")
                            awaitLock.put(response)
                        }
                    })
                } else {
                    handler.setOnReady(new OnReady() {
                        @Override
                        void ready(BackchannelClientHandler handler) {
                            if (cam != null) {
                                cam.backchannelAudioSupported = handler.backchannelAudioSupported
                                // Was there ever a good reason to do this?
                             //   cam.rtspTransport = handler.backchannelAudioSupported ? "udp" : "tcp"
                            }
                            awaitLock.put(response)
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

    // Wait till complete then return the response
    ObjectCommandResponse await() {
        def response = awaitLock.poll(10000, TimeUnit.MILLISECONDS)
        if(response == null) {
            response = new ObjectCommandResponse()
            response.status = PassFail.FAIL
            response.error = "RTSP backchannel client has timed out"
        }
        response
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
        ch?.close()?.sync()
        workerGroup.shutdownGracefully()
    }
}
