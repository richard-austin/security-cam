package security.cam

import grails.gorm.transactions.Transactional
import org.apache.http.HttpException
import org.apache.http.HttpRequest
import org.apache.http.HttpResponse
import org.apache.http.config.SocketConfig
import org.apache.http.entity.BasicHttpEntity
import org.apache.http.impl.bootstrap.HttpServer
import org.apache.http.impl.bootstrap.ServerBootstrap
import org.apache.http.message.BasicRequestLine
import org.apache.http.protocol.HttpContext
import org.apache.http.protocol.HttpRequestHandler
import security.cam.interfaceobjects.StdErrorExceptionLogger

import java.nio.charset.StandardCharsets


@Transactional
class CameraAdminPageHostingService {
    LogService logService
    SocketConfig config
    int port

    def setUpHttpServers() {
        config = SocketConfig.custom()
                .setSoTimeout(3000)
                .setTcpNoDelay(true)
                .build()

        port = 9900
        CamAdminRequestHandler requestHandler = new CamAdminRequestHandler()

        final server = ServerBootstrap.bootstrap()
                .setListenerPort(port)
                .setLocalAddress(Inet4Address.getByName("localhost"))
                .setServerInfo("Test/1.1")
                .setSocketConfig(config)
        //     .setSslContext(sslcontext)
                .setExceptionLogger(new StdErrorExceptionLogger(logService))
                .registerHandler("*", requestHandler)
                .create()

        server.start()
        final server2
        server2 = ServerBootstrap.bootstrap()
                .setListenerPort(port + 1)
                .setServerInfo("Test/1.1")
                .setSocketConfig(config)
        //     .setSslContext(sslcontext)
                .setExceptionLogger(new StdErrorExceptionLogger(logService))
                .registerHandler("/getAccessToken",requestHandler)
                .create()

        server2.start()

    }

    class CamAdminRequestHandler implements HttpRequestHandler {
        Set<String> accessTokens = new HashSet<>()
        @Override
        void handle(HttpRequest request, HttpResponse response, HttpContext context) throws HttpException, IOException {
            BasicRequestLine requestLine
            BasicHttpEntity entity

            def properties = request.getProperties()
            if(properties.containsKey('requestLine'))
                requestLine = properties.get('requestLine') as BasicRequestLine
            else
                throw new HttpException("No request line found")
            if(properties.containsKey('entity'))
                entity = properties.get('entity') as BasicHttpEntity
            else
                throw new HttpException("No entity found")

            switch (requestLine.uri) {
                case '/getAccessToken':
                    def is1 = entity.getContent()
                    byte[] b = new byte [entity.getContentLength()]
                    is1.read(b)
                    def clientRequest = new String(b, StandardCharsets.UTF_8)
                    if(clientRequest == 'get-access-token') {
                        String token = getAccessToken()
                        accessTokens.add(token)

                        BasicHttpEntity ent = new BasicHttpEntity()
                        InputStream is = new ByteArrayInputStream(token.getBytes())
                        ent.setContent(is)
                        ent.setContentLength(token.length())
                        response.setEntity(ent)
                    }
                    else
                    {
                        response.setReasonPhrase("Incorrectly formed request")
                        response.statusCode= 400
                    }
                    break
            }
        }

        String getAccessToken() {
            UUID uuid = UUID.randomUUID()
            return uuid.toString()
        }
    }
}

