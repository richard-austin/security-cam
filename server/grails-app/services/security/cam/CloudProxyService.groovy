package security.cam

import com.proxy.CloudProxy
import grails.gorm.transactions.Transactional
import security.cam.enums.PassFail
import security.cam.interfaceobjects.ObjectCommandResponse

@Transactional
class CloudProxyService {
    LogService logService
    CloudProxy cloudProxy = new CloudProxy("192.168.0.31", 8088, "localhost", 8081)

    ObjectCommandResponse start() {
        ObjectCommandResponse response = new ObjectCommandResponse()
        try {
            cloudProxy.start()
        }
        catch(Exception ex)
        {
            response.status= PassFail.FAIL
            response.error = "Exception in CloudProxy.start: "+ex.getClass().getName()+": "+ex.getMessage()
            logService.cam.error(response.error)
        }
        return response
    }

    ObjectCommandResponse stop() {
        ObjectCommandResponse response = new ObjectCommandResponse()
        try {
            cloudProxy.stop()
        }
        catch(Exception ex)
        {
            response.status= PassFail.FAIL
            response.error = "Exception in CloudProxy.stop: "+ex.getClass().getName()+": "+ex.getMessage()
            logService.cam.error(response.error)
        }
        return response
    }

    ObjectCommandResponse status() {
        ObjectCommandResponse response = new ObjectCommandResponse()
        try {
            response.responseObject = cloudProxy.isRunning()
        }
        catch(Exception ex)
        {
            response.status= PassFail.FAIL
            response.error = "Exception in CloudProxy.status: "+ex.getClass().getName()+": "+ex.getMessage()
            logService.cam.error(response.error)
        }
        return response
    }
}
