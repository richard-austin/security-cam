package security.cam.audiobackchannel
import gov.nist.javax.sdp.MediaDescriptionImpl;
import gov.nist.javax.sdp.fields.AttributeField;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.*;
import io.netty.handler.codec.rtsp.RtspMethods;
import javax.sdp.Origin;
import javax.sdp.SdpFactory;
import javax.sdp.SdpParseException;
import javax.sdp.SessionDescription;
import java.nio.charset.Charset;
import java.util.concurrent.atomic.AtomicReference;

class ClientHandler extends SimpleChannelInboundHandler<HttpObject> {
    final Queue<HttpRequest> requestQueue;

    ClientHandler(Queue<HttpRequest> requestQueue) {
        this.requestQueue = requestQueue;
    }
//    @Override
//    public void channelReadComplete(ChannelHandlerContext ctx, HttpObject msg) {
//        ctx.flush();
//    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, HttpObject obj) throws Exception {
        HttpResponse msg = (HttpResponse) obj;

        if (msg.status().code() == 200) {
            if(obj instanceof FullHttpMessage) {
                HttpRequest request = requestQueue.peek();
                if(request.method() == RtspMethods.DESCRIBE) {
                    FullHttpMessage fm = (FullHttpMessage) obj;
                    ByteBuf content = fm.content();
                    String sdp = content.toString(Charset.defaultCharset());
                    this.parseSdp(sdp);
                }
            }

            requestQueue.remove();
            HttpRequest req = requestQueue.peek();
            if(req.method() == RtspMethods.TEARDOWN)
                Thread.sleep(64000);

            if (msg.headers().contains("Session")) {
                String sessionId = msg.headers().get("Session");
                int idxOfSemiColon = sessionId.indexOf(";");
                if(idxOfSemiColon != -1)
                    sessionId = sessionId.substring(0, idxOfSemiColon);

                req.headers().add("Session", sessionId);
            }

            if (req != null) {
                req.headers().add("CSeq", getSeqNumber());
            }
            ctx.channel().writeAndFlush(requestQueue.peek());

            Set<String> names = msg.headers().names();
            System.out.println("Response -" + msg.toString());
            System.out.println("CTX -" + ctx.toString());
        } else if (msg.status().code() == 401) {
            HttpRequest req = requestQueue.peek();
            if (req != null) {
                req.headers().remove("CSeq");
                req.headers().add("CSeq", getSeqNumber());
            }

            ctx.channel().writeAndFlush(req);
        }
    }

    void parseSdp(String sdpContent)
    {
        try {
            SdpFactory sdpFactory = SdpFactory.getInstance();
            SessionDescription sessionDescription = sdpFactory.createSessionDescription(sdpContent);
            Origin origin = sessionDescription.getOrigin();
            String address =  origin.getAddress();
            String userName = origin.getUsername();
            String netType = origin.getNetworkType();
            String addrType = origin.getAddressType();


            final Vector<MediaDescriptionImpl> md = sessionDescription.getMediaDescriptions(true);
            AtomicReference<Vector<AttributeField>> backChannel = new AtomicReference<>();

            md.forEach((mediaDescription -> {
                final Vector<AttributeField> attributeFields = mediaDescription.getAttributeFields();
                attributeFields.forEach(attributeField -> {
                    try {
                        String name = attributeField.getName();
                        if (Objects.equals(attributeField.getName(), "sendonly")) {
                            backChannel.set(attributeFields);
                        }
                    }
                    catch(SdpParseException sdppe) {
                        System.out.println(sdppe.getClass().getName()+":"+sdppe.getMessage());
                    }
                });
            }));

            System.out.println(sessionDescription.getOrigin());

        }
        catch(Exception ex){
            System.out.println(ex.getClass().getName()+":"+ex.getMessage());
        }

    }
    static int seq = 0;

    static synchronized int getSeqNumber() {
        return ++seq;
    }


    void start(Channel ch) {
        if (requestQueue.peek() != null) {
            requestQueue.peek().headers().add("CSeq", getSeqNumber());
        }
        ch.writeAndFlush(requestQueue.peek());
    }
}
