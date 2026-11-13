package com.securitycam.beans

import com.securitycam.services.LogService
import com.securitycam.services.MediaServer
import com.securitycam.services.OnvifService
import com.securitycam.services.Sc_processesService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.context.ApplicationListener
import org.springframework.stereotype.Component

@Component
class ApplicationStartup
        implements ApplicationListener<ApplicationReadyEvent> {

    Sc_processesService sc_processesService
    OnvifService onvifService
    LogService logService
    MediaServer mediaServer

    @Autowired  // Use constructor injection
    ApplicationStartup(Sc_processesService sc_processesService, OnvifService onvifService, LogService logService, MediaServer mediaServer) {
        this.sc_processesService = sc_processesService
        this.onvifService = onvifService
        this.logService = logService
        this.mediaServer = mediaServer
    }

    /**
     * This event is executed as late as conceivably possible to indicate that
     * the application is ready to service requests.
     */
    @Override
    void onApplicationEvent(final ApplicationReadyEvent event) {
        sc_processesService.setOnvifService(onvifService)
        sc_processesService.startProcesses()
        mediaServer.start()
        logService.cam.info("Started NVR Services!!!!!!!!!!!!!!")
    }
}
