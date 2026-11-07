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
        @Autowired
        Sc_processesService sc_processesService
        @Autowired
        OnvifService onvifService

    @Autowired
    LogService logService

    @Autowired
    MediaServer mediaServer

    /**
     * This event is executed as late as conceivably possible to indicate that
     * the application is ready to service requests.
     */
    @Override
    void onApplicationEvent(final ApplicationReadyEvent event) {
        sc_processesService.setOnvifService(onvifService)
        sc_processesService.startProcesses()

        mediaServer.runStreamingSession("rtsp://admin:R%40nc1dTapsB0ttom@192.168.1.106:554/Preview_01_main")
     //   mediaServer.runStreamingSession("rtsp://192.168.1.30:554/11")
     //   mediaServer.runStreamingSession("rtsp://192.168.1.30:554/11")
        logService.cam.info("Started NVR Services!!!!!!!!!!!!!!")
    }
}
