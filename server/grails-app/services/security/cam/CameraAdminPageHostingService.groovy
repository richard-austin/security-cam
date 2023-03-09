package security.cam

import grails.gorm.transactions.Transactional
import org.apache.http.HttpException
import org.apache.http.HttpRequest
import org.apache.http.HttpResponse
import org.apache.http.config.SocketConfig
import org.apache.http.entity.BasicHttpEntity
import org.apache.http.impl.bootstrap.HttpServer
import org.apache.http.impl.bootstrap.ServerBootstrap
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

        final server = ServerBootstrap.bootstrap()
                .setListenerPort(port)
                .setLocalAddress(Inet4Address.getByName("localhost"))
                .setServerInfo("Test/1.1")
                .setSocketConfig(config)
        //     .setSslContext(sslcontext)
                .setExceptionLogger(new StdErrorExceptionLogger(logService))
                .registerHandler("*", new CamAdminRequestHandler())
                .create()

        server.start()
        final server2
        server2 = ServerBootstrap.bootstrap()
                .setListenerPort(port + 1)
                .setServerInfo("Test/1.1")
                .setSocketConfig(config)
        //     .setSslContext(sslcontext)
                .setExceptionLogger(new StdErrorExceptionLogger(logService))
                .registerHandler("*", new CamAdminRequestHandler())
                .create()

        server2.start()

        def x = config
    }

    class CamAdminRequestHandler implements HttpRequestHandler {
        @Override
        void handle(HttpRequest request, HttpResponse response, HttpContext context) throws HttpException, IOException {
            def x = request.getProperties()
            BasicHttpEntity entity = x['entity'] as BasicHttpEntity
            byte[] b = new byte[1000]
            entity.content.read(b)
            String z = new String(b, StandardCharsets.UTF_8)
            def t = z
        }
    }
}

