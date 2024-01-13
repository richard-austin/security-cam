package security.cam

import com.proxy.CloudAMQProxy
import org.springframework.messaging.simp.SimpMessagingTemplate
import security.cam.interfaceobjects.CloudProxyRestartTask
import grails.core.GrailsApplication
import grails.gorm.transactions.Transactional
import security.cam.enums.PassFail
import security.cam.interfaceobjects.ObjectCommandResponse

@Transactional
class CloudProxyService {
    LogService logService
    GrailsApplication grailsApplication
    SimpMessagingTemplate brokerMessagingTemplate;

    CloudAMQProxy cloudProxy = null

    ObjectCommandResponse start() {
        if (cloudProxy == null) {
            cloudProxy = new CloudAMQProxy(
                    (String) (grailsApplication.config.cloudProxy.webServerForCloudProxyHost),
                    (Integer) (grailsApplication.config.cloudProxy.webServerForCloudProxyPort),
                    brokerMessagingTemplate)
        }

        ObjectCommandResponse response = new ObjectCommandResponse()
        try {
            cloudProxy.userStart()
        }
        catch (Exception ex) {
            response.status = PassFail.FAIL
            response.error = "Exception in CloudAMQProxy.start: " + ex.getClass().getName() + ": " + ex.getMessage()
            logService.cam.error(response.error)
        }
        return response
    }

    ObjectCommandResponse stop() {
        ObjectCommandResponse response = new ObjectCommandResponse()
        try {
            cloudProxy.userStop()
        }
        catch (Exception ex) {
            response.status = PassFail.FAIL
            response.error = "Exception in CloudAMQProxy.stop: " + ex.getClass().getName() + ": " + ex.getMessage()
            logService.cam.error(response.error)
        }
        return response
    }

    /**
     * restart: Set up an asynchronous restart of the CloudAMQProxy. This is used when switching between Wi-Fi and Ethernet
     *          when setting up or changing Wi-Fi configuration.
     * @return
     */
    ObjectCommandResponse restart() {
        ObjectCommandResponse response = new ObjectCommandResponse()
        try {
            Timer timer = new Timer("CloudProxyRestartTimer")
            timer.schedule(new CloudProxyRestartTask(this), 2000)
            response.responseObject = [message: "Timer set up for restart"]
        }
        catch (Exception ex) {
            response.status = PassFail.FAIL
            response.error = "Exception in CloudAMQProxy.restart: " + ex.getClass().getName() + ": " + ex.getMessage()
            logService.cam.error(response.error)
        }

        return response
    }

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
