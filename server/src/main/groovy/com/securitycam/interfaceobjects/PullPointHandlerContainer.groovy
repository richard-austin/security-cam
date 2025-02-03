package com.securitycam.interfaceobjects

import com.securitycam.controllers.Camera
import com.securitycam.services.LogService
import de.onvif.soap.OnvifDevice
import de.onvif.soap.ProcessedPullMessagesResponse
import de.onvif.soap.PullMessagesCallbacks
import de.onvif.soap.PullPointSubscriptionHandler
import org.onvif.ver10.events.wsdl.PullMessagesResponse

class PullPointHandlerContainer implements PullMessagesCallbacks {
    final private OnvifDevice device
    final private Camera cam
    final private LogService logService

    PullPointHandlerContainer(final OnvifDevice device, final Camera cam, final LogService logService) {
        this.device = device
        this.cam = cam
        this.logService = logService
    }

    @Override
    void onPullMessagesReceived(PullMessagesResponse pullMessages) {
        ProcessedPullMessagesResponse ppmr = new ProcessedPullMessagesResponse(pullMessages)
        ppmr.responseData.forEach((x) ->
                x.Data.forEach((data) -> {
                    cam.pullPointEvents.add(x.topic)
                    logService.cam.info(x.created.toString() + " " + x.topic + " " + data.Name + " " + data.Value)
                }))
    }

    void getEvents() {
        try {
            PullPointSubscriptionHandler ppsh = new PullPointSubscriptionHandler(device, this)
            ppsh.createPullPointSubscription("")
            ppsh.getSupportedEvents()
        } catch (Exception e) {
            logService.cam.error(e.getClass().getName() + " " + e.getMessage())
        }
    }
}
