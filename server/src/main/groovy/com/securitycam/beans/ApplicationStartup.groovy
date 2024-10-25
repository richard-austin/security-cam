package com.securitycam.beans

import com.securitycam.services.LogService
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
    /**
     * This event is executed as late as conceivably possible to indicate that
     * the application is ready to service requests.
     */
    @Override
    void onApplicationEvent(final ApplicationReadyEvent evente) {
        sc_processesService.setOnvifService(onvifService)
        sc_processesService.startProcesses()

        logService.cam.info("Started NVR Services!!!!!!!!!!!!!!")
    }
}
