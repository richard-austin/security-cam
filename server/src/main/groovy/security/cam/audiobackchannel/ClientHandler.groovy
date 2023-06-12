package security.cam.audiobackchannel

import gov.nist.javax.sdp.MediaDescriptionImpl
import gov.nist.javax.sdp.fields.AttributeField
import io.netty.buffer.ByteBuf
import io.netty.channel.Channel
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.SimpleChannelInboundHandler
import io.netty.handler.codec.http.*
import io.netty.handler.codec.rtsp.RtspHeaderNames
import io.netty.handler.codec.rtsp.RtspMethods
import io.netty.handler.codec.rtsp.RtspVersions

import javax.sdp.Origin
import javax.sdp.SdpFactory
import javax.sdp.SdpParseException
import javax.sdp.SessionDescription
import java.nio.charset.Charset
import java.text.SimpleDateFormat
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.atomic.AtomicReference

class ClientHandler extends SimpleChannelInboundHandler<HttpObject> {
    final Queue<HttpRequest> requestQueue
    final String url
    AtomicReference<Vector<AttributeField>> backChannel = new AtomicReference<>()
    int server_port
    ClientHandler(final String url) {
        this.url = url
        requestQueue = new ConcurrentLinkedQueue<>()
        HttpRequest options = new DefaultFullHttpRequest(RtspVersions.RTSP_1_0, RtspMethods.OPTIONS, url)
        requestQueue.add(options)
    }

    private void handleOptions(HttpResponse msg) {
        HttpHeaders hdrs = msg.headers()
        String pub = hdrs.get("public")

        if (!pub.contains("DESCRIBE"))
            throw new RtspException("No DESCRIBE option")
        else if (!pub.contains("SETUP"))
            throw new RtspException("No SETUP option")
        else if (!pub.contains("PLAY"))
            throw new RtspException("No PLAY option")
        else if (!pub.contains("TEARDOWN"))
            throw new RtspException("No TEARDOWN option")
        else if (!pub.contains("SET_PARAMETER"))
            throw new RtspException("No SET_PARAMETER option")
        else {
            final String userAgent = "JAVA_Client"
            HttpRequest describe = new DefaultFullHttpRequest(RtspVersions.RTSP_1_0,
                    RtspMethods.DESCRIBE, url)
            // The ZXTech camera won't accept the all lower case header names in RtspHeaderNames
            describe.headers().add("User-Agent"/*"RtspHeaderNames.USER_AGENT"*/, userAgent)
            describe.headers().add("Accept", "application/sdp")
            describe.headers().add("Require", "www.onvif.org/ver20/backchannel")
            describe.headers().add(RtspHeaderNames.ACCEPT, "application/sdp")
            requestQueue.add(describe)
        }
    }

    private void handleDescribe(FullHttpResponse msg) {
        if (msg.status() == HttpResponseStatus.OK) {
            ByteBuf content = msg.content()
            String sdp = content.toString(Charset.defaultCharset())
            parseDescribeSdp(sdp)
            AttributeField controlAttr = backChannel.get().find(x -> {
                x.name == "control"
            })
            if(controlAttr == null)
                throw new RtspException("No control attribute found for backchannel")
            HttpRequest setup = new DefaultFullHttpRequest(RtspVersions.RTSP_1_0,
                    RtspMethods.SETUP, url + "/${controlAttr.value}")
            setup.headers().add("Transport", "RTP/AVP;unicast;client_port=45178-45179")
            setup.headers().add("Require", "www.onvif.org/ver20/backchannel")
            requestQueue.add(setup)
        } else {
            throw new RtspException("DESCRIBE returned error code ${msg.status().code()}")
        }
    }

    private void setupTransport(FullHttpResponse msg){
        if(!msg.headers().contains("transport"))
            throw new RtspException("No transport header in SETUP response")
        String transport = msg.headers().get("transport")
        if(!transport.contains("RTP"))
            throw new RtspException("RTP transport not specified")

        final String server_portEq = "server_port="
        final int idxSp = transport.indexOf(server_portEq)
        if(idxSp == -1)
            throw new RtspException("Server port not specified")
        final int idxSpEnd = transport.indexOf("-", idxSp)
        if(idxSpEnd == -1 || (idxSpEnd-idxSp <= server_portEq.length()))
            throw new RtspException("Server port numbers cannot be found")

        server_port = Integer.valueOf(transport.substring(idxSp+server_portEq.length(), idxSpEnd))
        int q = server_port
    }

    private void handleSetup(FullHttpResponse msg) {
        if (msg.status() == HttpResponseStatus.OK) {
            SimpleDateFormat dateFormat = new SimpleDateFormat("EEE, MMM dd yyyy HH:mm:ss 'GMT'")
            TimeZone tz = TimeZone.getTimeZone("GMT")
            dateFormat.setTimeZone(tz)
            String date = dateFormat.format(new Date())
            setupTransport(msg)
            HttpRequest play = new DefaultFullHttpRequest(RtspVersions.RTSP_1_0, RtspMethods.PLAY, url)
            play.headers().add("Require", "www.onvif.org/ver20/backchannel")
            play.headers().add("Date", date)
            play.headers().add("Range", "npt=0.000-")
            requestQueue.add(play)
        } else {
            throw new RtspException("SETUP returned error code ${msg.status().code()}")
        }
    }

    private void handlePlay(FullHttpResponse msg) {
        if (msg.status() == HttpResponseStatus.OK) {
            Thread.sleep(10000)
            HttpRequest tearDown = new DefaultFullHttpRequest(RtspVersions.RTSP_1_0, RtspMethods.TEARDOWN, url)
            tearDown.headers().add("Require", "www.onvif.org/ver20/backchannel")
            requestQueue.add(tearDown)
        } else {
            throw new RtspException("SETUP returned error code ${msg.status().code()}")
        }
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, HttpObject obj) throws Exception {
        HttpResponse msg = (HttpResponse) obj

        if (msg.status() != HttpResponseStatus.UNAUTHORIZED) {
            if (obj instanceof FullHttpResponse) {
                HttpRequest request = requestQueue.peek();
                switch (request.method()) {
                    case RtspMethods.OPTIONS:
                        handleOptions(obj)
                        break

                    case RtspMethods.DESCRIBE:
                        handleDescribe(obj)
                        break

                    case RtspMethods.SETUP:
                        handleSetup(obj)
                        break

                    case RtspMethods.PLAY:
                        handlePlay(obj)
                        break
                }

            }

            requestQueue.remove()
            HttpRequest req = requestQueue.peek()

            if (msg.headers().contains("Session")) {
                String sessionId = msg.headers().get("Session")
                int idxOfSemiColon = sessionId.indexOf(";")
                if (idxOfSemiColon != -1)
                    sessionId = sessionId.substring(0, idxOfSemiColon)

                req.headers().add("Session", sessionId)
            }

            if (req != null) {
                req.headers().add("CSeq", getSeqNumber())
            }
            ctx.channel().writeAndFlush(requestQueue.peek())

            Set<String> names = msg.headers().names()
            System.out.println("Response -" + msg.toString())
            System.out.println("CTX -" + ctx.toString())
        } else {
            HttpRequest req = requestQueue.peek()
            if (req != null) {
                req.headers().remove("CSeq")
                req.headers().add("CSeq", getSeqNumber())
            }
            ctx.channel().writeAndFlush(req)
        }
    }

    void parseDescribeSdp(String sdpContent) {
        try {
            SdpFactory sdpFactory = SdpFactory.getInstance()
            SessionDescription sessionDescription = sdpFactory.createSessionDescription(sdpContent)
            Origin origin = sessionDescription.getOrigin()
            String address = origin.getAddress()
            String userName = origin.getUsername()
            String netType = origin.getNetworkType()
            String addrType = origin.getAddressType()


            final Vector<MediaDescriptionImpl> md = sessionDescription.getMediaDescriptions(true)

            md.forEach((mediaDescription -> {
                final Vector<AttributeField> attributeFields = mediaDescription.getAttributeFields()
                attributeFields.forEach(attributeField -> {
                    try {
                        // Save the backchannel details
                        if (Objects.equals(attributeField.getName(), "sendonly")) {
                            backChannel.set(attributeFields)
                        }
                    }
                    catch (SdpParseException sdppe) {
                        System.out.println(sdppe.getClass().getName() + ":" + sdppe.getMessage())
                    }
                })
            }))

            if (backChannel.get() == null)
                throw new RtspException("No backchannel information found on this device")
        }
        catch (Exception ex) {
            System.out.println(ex.getClass().getName() + ":" + ex.getMessage())
        }
    }


    static int seq = 0

    static synchronized int getSeqNumber() {
        return ++seq
    }


    void start(Channel ch) {
        if (requestQueue.peek() != null) {
            requestQueue.peek().headers().add("CSeq", getSeqNumber())
        }
        ch.writeAndFlush(requestQueue.peek())
    }

    @Override
    void exceptionCaught(ChannelHandlerContext ctx, Throwable cause)
            throws Exception {

        System.out.println("${cause.getClass().getName()}: ${cause.getMessage()}")        //do more exception handling
        ctx.close();
    }
}
