package security.cam

import com.proxy.CamWebadminHostProxy
import grails.gorm.transactions.Transactional
import org.apache.http.config.SocketConfig

import javax.annotation.PostConstruct

@Transactional
class CameraAdminPageHostingService {
    LogService logService
    SocketConfig config
    int port = 9900

    @PostConstruct
    def initialize() {
        CamWebadminHostProxy proxy = new CamWebadminHostProxy(logService)
        proxy.runServer("192.168.1.30", 80, port)
    }

    String getAccessToken() {
        UUID uuid = UUID.randomUUID()
        return uuid.toString()
    }
}

