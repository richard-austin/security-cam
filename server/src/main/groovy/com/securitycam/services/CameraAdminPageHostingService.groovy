package com.securitycam.services

import com.securitycam.commands.CloseClientsCommand
import com.securitycam.commands.GetAccessTokenCommand
import com.securitycam.commands.ResetTimerCommand
import com.securitycam.interfaceobjects.ObjectCommandResponse
import com.securitycam.proxies.CamWebadminHostProxy
//import org.apache.http.config.SocketConfig
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

import javax.annotation.PostConstruct

@Transactional
@Service
class CameraAdminPageHostingService {
    @Autowired
    LogService logService
    @Autowired
    CamService camService
  //  SocketConfig config
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
            response.responseObject = map
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

