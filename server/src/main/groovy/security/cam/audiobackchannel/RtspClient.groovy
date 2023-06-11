package security.cam.audiobackchannel

import io.netty.bootstrap.Bootstrap
import io.netty.channel.Channel
import io.netty.channel.ChannelInitializer
import io.netty.channel.ChannelPipeline
import io.netty.channel.EventLoopGroup
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.channel.socket.SocketChannel
import io.netty.channel.socket.nio.NioSocketChannel
import io.netty.handler.codec.http.DefaultFullHttpRequest
import io.netty.handler.codec.http.HttpObjectAggregator
import io.netty.handler.codec.http.HttpRequest
import io.netty.handler.codec.rtsp.RtspDecoder
import io.netty.handler.codec.rtsp.RtspEncoder
import io.netty.handler.codec.rtsp.RtspHeaderNames
import io.netty.handler.codec.rtsp.RtspMethods
import io.netty.handler.codec.rtsp.RtspVersions
import org.apache.commons.codec.binary.Base64
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import java.text.SimpleDateFormat
import java.util.concurrent.ConcurrentLinkedQueue

class RtspClient {

    static final String userAgent = "JAVA_Client"
    private static final String USERNAME = "user"
    private static final String PASSWORD = "pass"
    static String authStringEnc
    private final static Logger LOGGER = LoggerFactory.getLogger(RtspClient.class)
    private final static String url = "rtsp://192.168.1.43:554"

    static void setBasicAuth() {
        String authString = USERNAME + ":" + PASSWORD
        System.out.println("auth string: " + authString)
        byte[] authEncBytes = Base64.encodeBase64(authString.getBytes())
        authStringEnc = new String(authEncBytes)
        System.out.println("Base64 encoded auth string: " + authStringEnc)
    }

    static void main(String[] args) throws InterruptedException, UnsupportedEncodingException {
        //setBasicAuth()
        SimpleDateFormat dateFormat = new SimpleDateFormat("EEE, MMM dd yyyy HH:mm:ss 'GMT'")
        TimeZone tz = TimeZone.getTimeZone("GMT")
        dateFormat.setTimeZone(tz)
        String date = dateFormat.format(new Date())
        System.out.print(date)
        ClientHandler x = new ClientHandler(new ConcurrentLinkedQueue<>())
        if(true) {
            Queue<HttpRequest> requestQueue = new ConcurrentLinkedQueue<>()
            HttpRequest options = new DefaultFullHttpRequest(RtspVersions.RTSP_1_0, RtspMethods.OPTIONS, url)
            //options.headers().add("CSeq", 1)
            requestQueue.add(options)

            HttpRequest describe = new DefaultFullHttpRequest(RtspVersions.RTSP_1_0,
                    RtspMethods.DESCRIBE, url)
            // The ZXTech camera won't accept the all lower case header names in RtspHeaderNames
            describe.headers().add("User-Agent"/*"RtspHeaderNames.USER_AGENT"*/, userAgent)
            describe.headers().add("Accept", "application/sdp")
            describe.headers().add("Require", "www.onvif.org/ver20/backchannel")
            // describe.headers().add("Require", "www.onvif.org/ver20/HalfDuplex/Client")
            describe.headers().add(RtspHeaderNames.ACCEPT, "application/sdp")
            requestQueue.add(describe)
            HttpRequest setup = new DefaultFullHttpRequest(RtspVersions.RTSP_1_0,
                    RtspMethods.SETUP, url + "/track3")
            setup.headers().add("Transport", "RTP/AVP;unicast;client_port=45178-45179")
            setup.headers().add("Require", "www.onvif.org/ver20/backchannel")
            // setup.headers().add("Require", "www.onvif.org/ver20/HalfDuplex/Client")
            requestQueue.add(setup)

            HttpRequest setup2 = new DefaultFullHttpRequest(RtspVersions.RTSP_1_0,
                    RtspMethods.SETUP, url + "/track2")
            setup2.headers().add("Transport", "RTP/AVP/;unicast;client_port=6296-6297")
            // requestQueue.add(setup2)

            HttpRequest setup3 = new DefaultFullHttpRequest(RtspVersions.RTSP_1_0,
                    RtspMethods.SETUP, url + "/track1")
            //  digestAuthenticator.setMethodAndUrl(RtspMethods.SETUP, url)
            //   setup.headers().add("Session", sessionId)
            setup3.headers().add("Transport", "RTP/AVP/;unicast;client_port=6298-6299")
            //  requestQueue.add(setup3)

            HttpRequest play = new DefaultFullHttpRequest(RtspVersions.RTSP_1_0, RtspMethods.PLAY, url)
            play.headers().add("Require", "www.onvif.org/ver20/backchannel")
            play.headers().add("Date", date)
            play.headers().add("Range", "npt=0.000-")
            // play.headers().add("Require", "www.onvif.org/ver20/HalfDuplex/Client")
            requestQueue.add(play)

            HttpRequest tearDown = new DefaultFullHttpRequest(RtspVersions.RTSP_1_0, RtspMethods.TEARDOWN, url)
            tearDown.headers().add("Require", "www.onvif.org/ver20/backchannel")
            requestQueue.add(tearDown)

            sendReq(requestQueue)
        }
    }

    static void sendReq(Queue<HttpRequest> requestQueue) throws InterruptedException {
        final String pipeName = "/home/richard/opfifo"
        final int BUFFER_SIZE = 2048

        Channel ch = start("192.168.1.43", 554, requestQueue)

//        byte[] x = {1, 2, 3, 4, 5, 6, 7, 8, 9}
//        try (InputStream is = Files.newInputStream(Paths.get(pipeName))){
//            int bytesRead = -1
//            byte[] buffer = new byte[BUFFER_SIZE]
//            while((bytesRead = is.read(buffer)) != -1) {
//                byte[] opBuf = new byte[bytesRead]
//                opBuf = Arrays.copyOf(buffer, bytesRead)
//                ch.writeAndFlush(opBuf)
//            }
//        }
//        catch(Exception ex) {
//            System.out.println(ex.getClass().getName()+" in sebdReq: "+ex.getMessage())
//        }

        //   channel.writeAndFlush(x)
    }


    static Channel start(String ip, int port, Queue<HttpRequest> requestQueue) {
        Bootstrap rtspClient = new Bootstrap()
        EventLoopGroup workerGroup = new NioEventLoopGroup(1); // todo: avoid possible nonceCount race
        final ClientHandler handler = new ClientHandler(requestQueue)
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

}
