package com.securitycam.audiobackchannel

import com.securitycam.interfaceobjects.ObjectCommandResponse
import com.securitycam.interfaceobjects.OnReady
import gov.nist.javax.sdp.MediaDescriptionImpl
import gov.nist.javax.sdp.fields.AttributeField
import gov.nist.javax.sdp.fields.MediaField
import io.netty.buffer.ByteBuf
import io.netty.channel.Channel
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.SimpleChannelInboundHandler
import io.netty.handler.codec.http.*
import io.netty.handler.codec.rtsp.RtspHeaderNames
import io.netty.handler.codec.rtsp.RtspMethods
import io.netty.handler.codec.rtsp.RtspVersions
import com.securitycam.interfaceobjects.MessageCallback

import javax.sdp.Origin
import javax.sdp.SdpFactory
import javax.sdp.SessionDescription
import java.nio.charset.Charset
import java.text.SimpleDateFormat
import java.util.concurrent.BlockingQueue
import java.util.concurrent.atomic.AtomicReference

class BackchannelClientHandler extends SimpleChannelInboundHandler<HttpObject> {
    private Timer keepaliveTimer
    protected HttpRequest request
    private final String url
    boolean discoveryMode
    protected  AtomicReference<Vector<AttributeField>> backChannel = new AtomicReference<>()
    private MediaField mediaField = null
    private  String address
    private  String netType
    private  String addrType
    boolean backchannelAudioSupported = false

    private int server_port
    private int server_port2
    protected AttributeField rtpmap

    final String userAgent = "JAVA_Client"
    HttpMessage lastMessage
    ChannelHandlerContext context
    int unauthorisedCount
    MessageCallback messageCallback
    private OnReady onReady
    private String timeout
    public final VacantPorts vacantPorts
    final BlockingQueue awaitLock
    ObjectCommandResponse response

    BackchannelClientHandler(final String url, final BlockingQueue awaitLock, final ObjectCommandResponse response) {
        this.url = url
        this.awaitLock = awaitLock
        this.response = response
        HttpRequest options = new DefaultFullHttpRequest(RtspVersions.RTSP_1_0, RtspMethods.OPTIONS, url)
        request = options
        keepaliveTimer = new Timer()
        vacantPorts = findClientPorts()
        unauthorisedCount = 0
        this.onReady = null
        this.discoveryMode = false
    }

    MediaField getMediaField() {
        return mediaField
    }

    String getOriginAddress() {
        return address
    }

    String getOriginAddrType() {
        return addrType
    }

    String getOriginNetType() {
        return netType
    }

    int getServerPort() {
        return server_port
    }

    int getServerPort2() {
        return server_port2
    }

    int getRTPPayloadType() {
        if(rtpmap != null) {
            final int spaceIdx = rtpmap.value.indexOf(' ')
            if(spaceIdx == -1)
                throw new RtspException("Unexpected rtpmap format")
            return Integer.valueOf(rtpmap.value.substring(0, spaceIdx))
        }
        else
            throw new RtspException("No rtpmap found")
    }

    String getRTPAudioEncoding() {
        if(rtpmap != null) {
            final int spaceIdx = rtpmap.value.indexOf(' ')
            final int fwdSlashIdx = rtpmap.value.indexOf("/")
            if(spaceIdx == -1 || fwdSlashIdx == -1)
                throw new RtspException("Unexpected rtpmap format")
            return rtpmap.value.substring(spaceIdx+1, fwdSlashIdx)
        }
        else
            throw new RtspException("No rtpmap found")
    }

    String getRTPAudioClockRate() {
        if(rtpmap != null) {
            final int fwdSlashIdx = rtpmap.value.indexOf("/")
            if(fwdSlashIdx == -1)
                throw new RtspException("Unexpected rtpmap format")
            return rtpmap.value.substring(fwdSlashIdx+1)
        }
        else
            throw new RtspException("No rtpmap found")
    }

    String getTimeout() {
        return timeout
    }

    void setMessageCallback(MessageCallback callback) {
        messageCallback = callback
    }

    void setOnReady(OnReady callback) {
        onReady = callback
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
            HttpRequest describe = new DefaultFullHttpRequest(RtspVersions.RTSP_1_0,
                    RtspMethods.DESCRIBE, url)
            // The ZXTech camera won't accept the all lower case header names in RtspHeaderNames
            describe.headers().add("User-Agent", userAgent)
            describe.headers().add("Accept", "application/sdp")
            describe.headers().add("Require", "www.onvif.org/ver20/backchannel")
            describe.headers().add(RtspHeaderNames.ACCEPT, "application/sdp")
            request = describe
        }
    }


    protected void handleDescribe(ChannelHandlerContext ctx, FullHttpResponse msg) {
        if (msg.status() == HttpResponseStatus.OK) {
            ByteBuf content = msg.content()
            String sdp = content.toString(Charset.defaultCharset())
            parseDescribeSdp(sdp)

            AttributeField controlAttr = backChannel.get().find(x -> {
                x.name == "control"
            })
            if (controlAttr == null)
                throw new RtspException("No control attribute found for backchannel")
            rtpmap = backChannel.get().find(x -> {
                x.name == "rtpmap"
            })
            if(discoveryMode) {
                request = null
                backchannelAudioSupported = rtpmap != null
                onReady.ready(this)
                endSession()
            }
            else {
                if(rtpmap == null)
                    throw new RtspException("No rtpmap attribute found for backchannel")

                if(vacantPorts.port1 == 0)
                    throw new RtspException("No vacant client ports were found")

                HttpRequest setup = new DefaultFullHttpRequest(RtspVersions.RTSP_1_0,
                        RtspMethods.SETUP, url + "/${controlAttr.value}")
                setup.headers().add("Transport", "RTP/AVP;unicast;client_port=${vacantPorts.port1}-${vacantPorts.port2}")
                setup.headers().add("User-Agent", userAgent)
                setup.headers().add("Require", "www.onvif.org/ver20/backchannel")
                request = setup
            }
        } else {
            throw new RtspException("DESCRIBE returned error code ${msg.status().code()}")
        }
    }

    private void setupTransport(HttpResponse msg) {
        if (!msg.headers().contains("transport"))
            throw new RtspException("No transport header in SETUP response")
        String transport = msg.headers().get("transport")
        if (!transport.contains("RTP"))
            throw new RtspException("RTP transport not specified")

        final String server_portEq = "server_port="
        final int idxSp = transport.indexOf(server_portEq)
        if (idxSp == -1)
            throw new RtspException("Server port not specified")
        final int idxSpEnd = transport.indexOf("-", idxSp)
        if (idxSpEnd == -1 || (idxSpEnd - idxSp <= server_portEq.length()))
            throw new RtspException("Server ports cannot be found")
        int idxSp2End = transport.indexOf(";", idxSpEnd)
        if(idxSp2End == -1)
            idxSp2End = transport.length()
        server_port = Integer.valueOf(transport.substring(idxSp + server_portEq.length(), idxSpEnd))
        server_port2 = Integer.valueOf(transport.substring(idxSpEnd+1, idxSp2End))
    }

    private void handleSetup(HttpResponse msg) {
        if (msg.status() == HttpResponseStatus.OK) {
            SimpleDateFormat dateFormat = new SimpleDateFormat("EEE, MMM dd yyyy HH:mm:ss 'GMT'")
            TimeZone tz = TimeZone.getTimeZone("GMT")
            dateFormat.setTimeZone(tz)
            String date = dateFormat.format(new Date())
            setupTransport(msg)

            HttpRequest play = new DefaultFullHttpRequest(RtspVersions.RTSP_1_0, RtspMethods.PLAY, url)
            play.headers().add("Require", "www.onvif.org/ver20/backchannel")
            play.headers().add("User-Agent", userAgent)
            play.headers().add("Date", date)
            play.headers().add("Range", "npt=0.000-")
            request = play
        } else {
            throw new RtspException("SETUP returned error code ${msg.status().code()}")
        }
    }

    private sendKeepAlive(ChannelHandlerContext ctx, HttpResponse msg) {
        HttpRequest get_parameter = new DefaultFullHttpRequest(RtspVersions.RTSP_1_0, RtspMethods.GET_PARAMETER, url)
        get_parameter.headers().add("User-Agent", userAgent)
        request = get_parameter
        addGeneralHeaders(msg)
        ctx.writeAndFlush(request)
    }

    private void handlePlay(final ChannelHandlerContext ctx, HttpResponse msg) {
        if (msg.status() == HttpResponseStatus.OK) {
            int intTimeout = 10
            // Set the keep alive period to 10s less than the device timeout
            if(timeout != "") {
                intTimeout = Integer.valueOf(timeout)
                if(intTimeout > 20)
                    intTimeout -= 10
                else
                    intTimeout -= 3
            }
            intTimeout *= 1000

            keepaliveTimer.scheduleAtFixedRate(() -> sendKeepAlive(ctx, msg), intTimeout, intTimeout)
            request = null
            if(onReady != null)
                onReady.ready(this)
        } else {
            throw new RtspException("PLAY returned error code ${msg.status().code()}")
        }
    }

    void handleTeardown(HttpResponse ignore) {
        request = null
    }

    private void handleGetParameter(HttpResponse msg) {
        if (msg.status() == HttpResponseStatus.OK) {
            request = null
        } else {
            throw new RtspException("GET_PARAMETER returned error code ${msg.status().code()}")
        }
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, HttpObject obj) throws Exception {
        HttpResponse msg = (HttpResponse) obj
        lastMessage = msg
        context = ctx

        if (messageCallback != null) {
            messageCallback.callback("Response -" + msg.toString(), null)
            messageCallback.callback("CTX -" + ctx.toString(), null)
        }

        if (msg.status() != HttpResponseStatus.UNAUTHORIZED) {
            if (obj instanceof HttpResponse && request != null) {
                switch (request.method()) {
                    case RtspMethods.OPTIONS:
                        handleOptions(obj)
                        break

                    case RtspMethods.DESCRIBE:
                        if(obj instanceof FullHttpResponse)
                            handleDescribe(ctx, obj)
                        else
                            throw new RtspException("Response to DESCRIBE was not FullHttpResponse")
                        break

                    case RtspMethods.SETUP:
                        handleSetup(obj)
                        break

                    case RtspMethods.PLAY:
                        handlePlay(ctx, obj)
                        break

                    case RtspMethods.GET_PARAMETER:
                        handleGetParameter(obj)
                        break

                    case RtspMethods.TEARDOWN:
                        handleTeardown(obj)
                        break

                    default:
                        return
                }

                if (request != null) {
                    addGeneralHeaders(msg)
                    ctx.writeAndFlush(request)
                }
            }
        } else {  // Unauthorised, run the request again
            if (++unauthorisedCount < 2 && request != null) {
                request.headers().remove("CSeq")
                request.headers().add("CSeq", getSeqNumber())
                ctx.writeAndFlush(request)
            } else {
                request = null
                unauthorisedCount = 0
            }
        }
    }

    void addGeneralHeaders(HttpMessage msg) {
        if (msg != null && msg.headers().contains("Session")) {
            String sessionId = msg.headers().get("Session")
            int idxOfSemiColon = sessionId.indexOf(";")
            if (idxOfSemiColon != -1) {
                String sessionHeader = sessionId
                sessionId = sessionHeader.substring(0, idxOfSemiColon)
                int idxOfEq = sessionHeader.indexOf("=", idxOfSemiColon)
                if(idxOfEq != -1) {
                    idxOfSemiColon = sessionHeader.indexOf(";", idxOfEq)
                    if(idxOfSemiColon == -1)
                        timeout = sessionHeader.substring(idxOfEq + 1)
                    else
                        timeout = sessionHeader.substring(idxOfEq+1, idxOfSemiColon)
                }
            }
            if(request != null)
                request.headers().add("Session", sessionId)
        }
        if(request != null)
            request.headers().add("CSeq", getSeqNumber())
    }

    void parseDescribeSdp(String sdpContent) {
        SdpFactory sdpFactory = SdpFactory.getInstance()
        SessionDescription sessionDescription = sdpFactory.createSessionDescription(sdpContent)
        Origin origin = sessionDescription.getOrigin()
        address = origin.getAddress()
        netType = origin.getNetworkType()
        addrType = origin.getAddressType()

        final Vector<MediaDescriptionImpl> md = sessionDescription.getMediaDescriptions(true)
        md.forEach((mediaDescription -> {
            final Vector<AttributeField> attributeFields = mediaDescription.getAttributeFields()
            attributeFields.forEach(attributeField -> {
                // Save the backchannel details
                if (Objects.equals(attributeField.getName(), "sendonly")) {
                    backChannel.set(attributeFields)
                    mediaField = mediaDescription.getMediaField()
                }
           })
        }))

        if (backChannel.get() == null)
            throw new RtspException("No backchannel information found on this device")
    }

    void endSession() {
        keepaliveTimer.cancel()  // Stop the keep alive (GET_PARAMETER) calls
        HttpRequest tearDown = new DefaultFullHttpRequest(RtspVersions.RTSP_1_0, RtspMethods.TEARDOWN, url)
        tearDown.headers().add("Require", "www.onvif.org/ver20/backchannel")
        request = tearDown
        addGeneralHeaders(lastMessage)
        if(context != null) {
            context.writeAndFlush(request)
            context.close()
        }
    }

    static int seq = 0

    static synchronized int getSeqNumber() {
        return ++seq
    }

    void start(Channel ch) {
        if (request != null) {
            request.headers().add("CSeq", getSeqNumber())
        }
        def fut = ch.writeAndFlush(request)
        fut.sync()
    }

    @Override
    void exceptionCaught(ChannelHandlerContext ctx, Throwable cause)
            throws Exception {
        ctx.close()
        if (messageCallback != null)
            messageCallback.callback(cause.getMessage(), cause)

        response.status = PassFail.FAIL
        response.error = "${cause.getClass().getName()}  in BackChannelClientHandler: ${cause.getMessage()}"
        awaitLock.put(response)
//        ctx.close()
    }
    class VacantPorts {
        final int port1, port2
        VacantPorts(int port1, int port2) {
            this.port1 = port1
            this.port2 = port2
        }
    }

    /**
     * findClientPorts: Find two successive vacant ports with the first having an even number
     * @return VacantPorts object, if both are 0, none was found
     */
    private VacantPorts findClientPorts() {
        int port1=0, port2=0
        for(int i = 4000; i < 5000; i+=2) {
            try {
                ServerSocket skt1 = new ServerSocket(i)
                ServerSocket skt2 = new ServerSocket(i+1)
                skt1.close()
                skt2.close()
                port1 = i
                port2 = i+1
                break
            }
            catch (Exception ignore){}
        }

        return new VacantPorts(port1, port2)
    }
}
