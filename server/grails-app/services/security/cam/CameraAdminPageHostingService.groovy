package security.cam

import security.cam.interfaceobjects.CamWebadminHostProxy
import grails.converters.JSON
import grails.gorm.transactions.Transactional
import org.apache.http.config.SocketConfig
import security.cam.commands.CloseClientsCommand
import security.cam.commands.GetAccessTokenCommand
import security.cam.commands.ResetTimerCommand
import security.cam.enums.PassFail
import security.cam.interfaceobjects.ObjectCommandResponse

import javax.annotation.PostConstruct


@Transactional
class CameraAdminPageHostingService {
    LogService logService
    CamService camService
    SocketConfig config
    int port = 8446
    CamWebadminHostProxy proxy

    @PostConstruct
    def initialize() {
        proxy = new CamWebadminHostProxy(logService, camService)
        proxy.runServer(port)
    }

    def getAccessToken(GetAccessTokenCommand cmd) {
        final ObjectCommandResponse response = new ObjectCommandResponse()

        try {
            UUID uuid = UUID.randomUUID()
            proxy.addAccessToken(cmd, uuid.toString())
            Map<String, String> map = new HashMap<>()
            map.put('accessToken', uuid.toString())
            response.responseObject = map as JSON
        }
        catch(Exception ex)
        {
            response.status= PassFail.FAIL
            response.error = ex.getClass().getName()+" in getAccessToken: "+ex.getMessage()
            logService.cam.error(response.error)
        }
        return response
    }

    def resetTimer(ResetTimerCommand cmd) {
        final ObjectCommandResponse response = new ObjectCommandResponse()

        try {
            if(!proxy.resetTimer(cmd))
                throw new Exception("No such accessToken "+cmd.accessToken)
        }
        catch(Exception ex)
        {
            response.status= PassFail.FAIL
            response.error = ex.getClass().getName()+" in resetTimer: "+ex.getMessage()
            logService.cam.error(response.error)
        }
        return response
    }

    def closeClients(CloseClientsCommand cmd) {
        final ObjectCommandResponse response = new ObjectCommandResponse()

        try {
            if(!proxy.closeClientConnections(cmd.accessToken))
                throw new Exception("No such accessToken "+cmd.accessToken)
        }
        catch(Exception ex)
        {
            response.status= PassFail.FAIL
            response.error = ex.getClass().getName()+" in closeClients: "+ex.getMessage()
            logService.cam.error(response.error)
        }
        return response
    }
}

