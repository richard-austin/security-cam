package com.securitycam.services

import com.securitycam.enums.PassFail
import com.securitycam.interfaceobjects.ObjectCommandResponse
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class CloudProxyService {
    @Autowired
    LogService logService
    def /*CloudAMQProxy*/ cloudProxy = null

    ObjectCommandResponse status() {
        ObjectCommandResponse response = new ObjectCommandResponse()
        try {
            response.responseObject = cloudProxy == null ? false : cloudProxy.isRunning()
        }
        catch (Exception ex) {
            response.status = PassFail.FAIL
            response.error = "Exception in CloudAMQProxy.status: " + ex.getClass().getName() + ": " + ex.getMessage()
            logService.cam.error(response.error)
        }
        return response
    }

    ObjectCommandResponse isTransportActive() {
        ObjectCommandResponse response = new ObjectCommandResponse()
        try {
            response.responseObject = cloudProxy == null ? false : cloudProxy.isTransportActive()
        }
        catch (Exception ex) {
            response.status = PassFail.FAIL
            response.error = "Exception in CloudAMQProxy.isTransportActive: " + ex.getClass().getName() + ": " + ex.getMessage()
            logService.cam.error(response.error)
        }
        return response
    }
}
