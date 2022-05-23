package security.cam

import com.proxy.CloudProxy
import security.cam.interfaceobjects.CloudProxyRestartTask
import grails.core.GrailsApplication
import grails.gorm.transactions.Transactional
import security.cam.enums.PassFail
import security.cam.interfaceobjects.ObjectCommandResponse

@Transactional
class CloudProxyService {
    LogService logService
    GrailsApplication grailsApplication

    CloudProxy cloudProxy = null

    ObjectCommandResponse start() {
        if(cloudProxy == null)
        {
            cloudProxy = new CloudProxy(
                    (String)(grailsApplication.config.cloudProxy.webServerHost),
                    (Integer)(grailsApplication.config.cloudProxy.webServerPort),
                    (String)(grailsApplication.config.cloudProxy.cloudHost),
                    (Integer)(grailsApplication.config.cloudProxy.cloudPort))
        }

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

    /**
     * restart: Set up an asynchronous restart of the CloudProxy. This is used when switching between Wi-Fi and Ethernet
     *          when setting up or changing Wi-Fi configuration.
     * @return
     */
    ObjectCommandResponse restart()
    {
        ObjectCommandResponse response = new ObjectCommandResponse()
        try {
            Timer timer = new Timer("CloudProxyRestartTimer")
            timer.schedule(new CloudProxyRestartTask(this), 2000)
        }
        catch(Exception ex)
        {
            response.status= PassFail.FAIL
            response.error = "Exception in CloudProxy.restart: "+ex.getClass().getName()+": "+ex.getMessage()
            logService.cam.error(response.error)
        }

        return response
    }

    ObjectCommandResponse status() {
        ObjectCommandResponse response = new ObjectCommandResponse()
        try {
            response.responseObject = cloudProxy == null ? false : cloudProxy.isRunning()
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
