package com.securitycam.controllers

import com.fasterxml.jackson.databind.ObjectMapper
import com.securitycam.commands.GetMotionEventsCommand
import com.securitycam.enums.PassFail
import com.securitycam.error.NVRRestMethodException
import com.securitycam.interfaceobjects.Asymmetric
import com.securitycam.interfaceobjects.ObjectCommandResponse
import com.securitycam.services.LogService
import com.securitycam.services.MotionService
import jakarta.validation.Valid
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.security.access.annotation.Secured
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

class MotionEvents {
    MotionEvents(String[] events) {
        this.events = events
    }

    String[] events
}

class Recording {
    boolean enabled=false
    String recording_src_url=''
    String uri=''
    String location=''
}

class Motion {
    boolean enabled=false    // If true, motion detection is enabled for the stream
    String mask_file=''  // Mask file which defines area used in motion sensing
    String trigger_recording_on=''  // The name of the camera stream on which recordings will be triggered following
    // Motion events on this camera stream (usually another stream on the same physical camera).
    Integer threshold = 1500// Threshold for declaring motion.
}

class Stream {
    String descr=''
    boolean defaultOnMultiDisplay=false
    String netcam_uri=''
    String uri=''
    boolean audio = false
    String audio_bitrate="0"
    String audio_encoding = null
    Integer audio_sample_rate = 0
    String media_server_input_uri=''
    Motion motion=new Motion()
    Integer preambleFrames = 100
    Integer video_width=0
    Integer video_height=0
    Recording recording=new Recording()
}


enum cameraType {none, sv3c, zxtechMCW5B10X}

class CameraParamSpecs {
    int camType
    String params
    String uri
    String name
}

class CameraAdminCredentials
{
    String userName = ""
    String password = ""
}


class Camera {
    String name=''
    String address=''
    CameraParamSpecs cameraParamSpecs = null
    String ftp = "none"
    String snapshotUri=''
    boolean ptzControls = false
    Map<String, Stream> streams = new HashMap<String, Stream>()
    String onvifHost=''
    boolean backchannelAudioSupported = false
    String rtspTransport = "tcp"
    boolean useRtspAuth = false
    int retriggerWindow = 30
    String cred = ""
    CameraAdminCredentials credentials() {
        Asymmetric crypto = new Asymmetric()
        String jsonCreds = crypto.decrypt(cred)
        ObjectMapper mapper = new ObjectMapper()
        if (jsonCreds.length() > 0)
            return mapper.readValue(jsonCreds, CameraAdminCredentials.class)
        else
            return new CameraAdminCredentials()
    }
}

@RestController
@RequestMapping("/motion")
class MotionController
{
    @Autowired
    LogService logService

    @Autowired
    MotionService motionService

    /**
     * getMotionEvents: Get the motion events for the given camera.
     * @param cmd : camera: Camera to get motion events for
     * @return
     */
    @Secured(['ROLE_CLIENT', 'ROLE_CLOUD', 'ROLE_GUEST'])
    @RequestMapping("/getMotionEvents")
    def getMotionEvents(@Valid @RequestBody GetMotionEventsCommand cmd) {
        ObjectCommandResponse motionEvents = motionService.getMotionEvents(cmd)

        if (motionEvents.status != PassFail.PASS) {
            return new NVRRestMethodException(motionEvents.error, "motion/getMotionEvents")
        } else {
            logService.cam.info("getMotionEvents: success")
            return new MotionEvents(motionEvents.responseObject as String[])
        }
    }
}
