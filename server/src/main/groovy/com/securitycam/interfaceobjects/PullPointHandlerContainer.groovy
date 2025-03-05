package com.securitycam.interfaceobjects

import com.securitycam.controllers.Camera
import com.securitycam.services.LogService
import com.securitycam.services.RestfulInterfaceService
import de.onvif.soap.OnvifDevice
import de.onvif.soap.ProcessedPullMessagesResponse
import de.onvif.soap.PullMessagesCallbacks
import de.onvif.soap.PullPointSubscriptionHandler
import org.onvif.ver10.events.wsdl.PullMessagesResponse

class PullPointHandlerContainer implements PullMessagesCallbacks {
    final private OnvifDevice device
    final private String camKey
    final private Camera cam
    final private LogService logService
    final int recordingSvcTimout = 60
    private boolean gettingEvents = true
    final private RestfulInterfaceService restfulInterfaceService
    private Timer timer = null
    final private recordingOverrun = 60000

    PullPointHandlerContainer(final OnvifDevice device, final String camKey, final Camera cam, RestfulInterfaceService restfulInterfaceService) {
        this.device = device
        this.camKey = camKey
        this.cam = cam
        this.restfulInterfaceService = restfulInterfaceService
        this.logService = restfulInterfaceService.logService
    }

    @Override
    void onPullMessagesReceived(PullMessagesResponse pullMessages) {
        if(gettingEvents) {
            ProcessedPullMessagesResponse ppmr = new ProcessedPullMessagesResponse(pullMessages)
            ppmr.responseData.forEach((x) ->
                    x.Data.forEach((data) -> {
                        cam.pullPointEvents.add(x.topic)
                        logService.cam.info(x.created.toString() + " " + x.topic + " " + data.Name + " " + data.Value)
                    }))
        }
        else // Subscribing
        {
            ProcessedPullMessagesResponse ppmr = new ProcessedPullMessagesResponse(pullMessages)
            ppmr.responseData.forEach((x) ->
                    x.Data.forEach((data) -> {
                        if(x.topic == cam.pullPointTopic) {
                            logService.cam.info(x.created.toString() + " " + x.topic + " " + data.Name + " " + data.Value)
                            if(data.Value == 'true') {
                                startTimer()
                            }
                         }
                    }))

        }
    }

    void getEvents() {
        gettingEvents = true
        try {
            PullPointSubscriptionHandler pullPointSub = new PullPointSubscriptionHandler(device, this)
            pullPointSub.createPullPointSubscription("")  // Get all available events
            pullPointSub.getSupportedEvents()
        } catch (Exception e) {
            logService.cam.error(e.getClass().getName() + " " + e.getMessage())
        }
    }

    PullPointSubscriptionHandler _pullPointSub

    void subscribe(String topic) {
        gettingEvents = false
        try {
            _pullPointSub = new PullPointSubscriptionHandler(device, this)
            _pullPointSub.createPullPointSubscription(topic)
            _pullPointSub.subcribe()
        }
        catch(Exception e) {
            logService.cam.error(e.getClass().getName() + " " + e.getMessage())
        }
    }

    void terminate() {
        _pullPointSub.setTerminate()
    }

    def startTimer() {
        if(timer == null) { // Start
            timer = new Timer()
            timer.schedule(new PullPointEventTimerTask(), recordingOverrun)
            restfulInterfaceService.sendRequest("localhost:8086", "/", "{\"command\": \"start_recording\", \"camera_name\": \"$camKey\"}", true, recordingSvcTimout)
        }
        else {  // Reset
            timer.cancel()
            timer.purge()
            timer = new Timer()
            timer.schedule(new PullPointEventTimerTask(), recordingOverrun)
        }
    }

    private class PullPointEventTimerTask extends TimerTask {
        // The Timer run method
        @Override
        void run() {
            timer.cancel()
            timer.purge()
            timer = null
            restfulInterfaceService.sendRequest("localhost:8086", "/", "{\"command\": \"end_recording\", \"camera_name\": \"$camKey\"}\r\n", true, recordingSvcTimout)
            logService.cam.info("")
        }
    }
}
