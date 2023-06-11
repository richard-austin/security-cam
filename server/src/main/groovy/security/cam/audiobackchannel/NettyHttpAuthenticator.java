/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2019 Vladimir Zhilin
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included
 * in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NON-INFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package security.cam.audiobackchannel;

import common.Authentication;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import io.netty.handler.codec.http.*;
import org.apache.http.protocol.BasicHttpContext;

@ChannelHandler.Sharable
public class NettyHttpAuthenticator extends ChannelDuplexHandler {
    private String challenge;

    public NettyHttpAuthenticator() {
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (msg instanceof HttpResponse) {
            HttpResponse httpResponse = (HttpResponse) msg;
            HttpResponseStatus status = httpResponse.status();

            String authenticateHeader = httpResponse.headers().get(HttpHeaderNames.WWW_AUTHENTICATE);
            if (authenticateHeader != null) {
                if (status.code() == 401) {
                    this.challenge = authenticateHeader;
                }
            }
        }
        super.channelRead(ctx, msg);
    }

    @Override
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
        if (msg instanceof FullHttpRequest) {
            HttpRequest req = (HttpRequest) msg;

            String method = req.method().name();
            String uri = req.uri();

            Authentication auth = new Authentication(null);
            String header = auth.getAuthResponse("admin", "R@nc1dTapsB0ttom", method, uri, challenge, new BasicHttpContext()).getValue();
            if (header != null) {
                if (!header.contains("\"MD5\"") && header.contains("MD5"))
                    header = header.replace("MD5", "\"MD5\"");
                req.headers().set(HttpHeaderNames.AUTHORIZATION, header);
            }
        }
        super.write(ctx, msg, promise);
    }
}
