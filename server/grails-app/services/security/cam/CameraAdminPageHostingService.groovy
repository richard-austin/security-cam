package security.cam

import grails.gorm.transactions.Transactional
import org.apache.http.config.SocketConfig
import org.apache.http.impl.bootstrap.HttpServer
import org.apache.http.impl.bootstrap.ServerBootstrap
import security.cam.interfaceobjects.CamAdminRequestHandler
import security.cam.interfaceobjects.StdErrorExceptionLogger


@Transactional
class CameraAdminPageHostingService {
    LogService logService
    SocketConfig config
    int port
    final HttpServer server;

    CameraAdminPageHostingService() {
        config = SocketConfig.custom()
                .setSoTimeout(3000)
                .setTcpNoDelay(true)
                .build()

        port = 9900

        final server = ServerBootstrap.bootstrap()
                .setListenerPort(port)
                .setServerInfo("Test/1.1")
                .setSocketConfig(config)
        //     .setSslContext(sslcontext)
                .setExceptionLogger(new StdErrorExceptionLogger(logService))
                .registerHandler("*", new CamAdminRequestHandler())
                .create();

        server.start();
    }

}

