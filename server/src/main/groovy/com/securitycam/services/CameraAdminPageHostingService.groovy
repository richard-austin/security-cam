package com.securitycam.services


import com.securitycam.commands.GetHostingAccessCommand
import com.securitycam.commands.SetUseCachingCommand
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
    final int port = 8446
    CamWebadminHostProxy proxy

    @PostConstruct
    def initialize() {
        proxy = new CamWebadminHostProxy(logService, camService)
        proxy.runServer(port)
    }

    def getHostingAccess(GetHostingAccessCommand cmd) {
        ObjectCommandResponse response = new ObjectCommandResponse()

        try {
            if (!proxy.enableAccess(cmd))
                throw new Exception("Web admin hosting is already in use")
            // Get the NVR LAN address to connect to for admin hosting (through the VPN if accessing remotely)
            String addresses = UtilsService.executeLinuxCommand("hostname", "-I")
            String[] addressArray = addresses.split(" ")
            for (a in addressArray)
                if (a.contains(".")) {// Ignore ipv6 addresses
                    response.responseObject = ["nvrIPAddress": a]
                    break
                }
            if (response.responseObject == null)
                throw new Exception("Could not find a valid IP V4 address for the NVR")
        }
        catch (Exception ex) {
            response.status = PassFail.FAIL
            response.error = ex.getClass().getName() + " in getHostingAccess: " + ex.getMessage()
            logService.cam.error(response.error)
        }
        return response
    }

    def resetTimer() {
        final ObjectCommandResponse response = new ObjectCommandResponse()

        try {
            if (!proxy.resetTimer())
                throw new Exception("Error in ResetTimer")
        }
        catch (Exception ex) {
            response.status = PassFail.FAIL
            response.error = ex.getClass().getName() + " in resetTimer: " + ex.getMessage()
            logService.cam.error(response.error)
        }
        return response
    }

    def closeClient() {
        final ObjectCommandResponse response = new ObjectCommandResponse()

        try {
            proxy.closeClientConnection()
        }
        catch (Exception ex) {
            response.status = PassFail.FAIL
            response.error = ex.getClass().getName() + " in closeClient: " + ex.getMessage()
            logService.cam.error(response.error)
        }
        return response
    }

    def setUseCaching(SetUseCachingCommand cmd) {
        ObjectCommandResponse response = new ObjectCommandResponse()
        proxy.setUseCaching(cmd.useCaching)
        response.responseObject = cmd.useCaching  // Just return the value sent
        return response
    }
}

