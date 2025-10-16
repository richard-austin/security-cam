package com.securitycam.services


import com.securitycam.commands.GetHostingAccessCommand

import com.securitycam.enums.PassFail
import com.securitycam.interfaceobjects.ObjectCommandResponse
import com.securitycam.proxies.CamWebadminHostProxy
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

import jakarta.annotation.PostConstruct

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

    def getHostingAccess(GetHostingAccessCommand cmd) {
        final ObjectCommandResponse response = new ObjectCommandResponse()

        try {
            proxy.enableAccess(cmd)
         }
        catch(Exception ex)
        {
            response.status= PassFail.FAIL
            response.error = ex.getClass().getName()+" in getHostingAccess: "+ex.getMessage()
            logService.cam.error(response.error)
        }
        return response
    }

    def resetTimer() {
        final ObjectCommandResponse response = new ObjectCommandResponse()

        try {
            if(!proxy.resetTimer())
                throw new Exception("Error in ResetTimer")
        }
        catch(Exception ex)
        {
            response.status= PassFail.FAIL
            response.error = ex.getClass().getName()+" in resetTimer: "+ex.getMessage()
            logService.cam.error(response.error)
        }
        return response
    }

    def closeClient() {
        final ObjectCommandResponse response = new ObjectCommandResponse()

        try {
            if(!proxy.closeClientConnection())
                throw new Exception("Error closing client connection")
        }
        catch(Exception ex)
        {
            response.status= PassFail.FAIL
            response.error = ex.getClass().getName()+" in closeClient: "+ex.getMessage()
            logService.cam.error(response.error)
        }
        return response
    }
}

