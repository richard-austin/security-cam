package com.securitycam.controllers

import com.fasterxml.jackson.databind.ObjectMapper
import com.securitycam.commands.DeleteRecordingCommand
import com.securitycam.commands.DownloadRecordingCommand
import com.securitycam.commands.GetMotionEventsCommand
import com.securitycam.configuration.Config
import com.securitycam.controlleradvice.ErrorResponse
import com.securitycam.enums.PassFail
import com.securitycam.error.NVRRestMethodException
import com.securitycam.interfaceobjects.Asymmetric
import com.securitycam.interfaceobjects.ObjectCommandResponse
import com.securitycam.services.LogService
import com.securitycam.services.MotionService
import com.securitycam.validators.BadRequestResult
import com.securitycam.validators.DeleteRecordingCommandValidator
import com.securitycam.validators.DownloadRecordingCommandValidator
import com.securitycam.validators.GeneralValidator
import jakarta.validation.Valid
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.security.access.annotation.Secured
import org.springframework.util.MultiValueMap
import org.springframework.validation.BindingResult
import org.springframework.validation.DataBinder
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

import java.net.http.HttpHeaders
import java.nio.file.Files

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

    @Autowired
    Config config

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
            throw new NVRRestMethodException(motionEvents.error, "motion/getMotionEvents")
        } else {
            logService.cam.info("getMotionEvents: success")
            return new MotionEvents(motionEvents.responseObject as String[])
        }
    }

    /**
     * downloadRecording: API call for downloading recordings
     * @param cmd : manifest: The manifest file name.
     *             camera: The camera the recording is from
     * @return: Binary stream for the .mp4 file
     */
    @Secured(['ROLE_CLIENT', 'ROLE_CLOUD', 'ROLE_GUEST'])
    @PostMapping("/downloadRecording")
    def downloadRecording(@RequestBody DownloadRecordingCommand cmd) {
        // This more convoluted validation is used as setting up the folder property in cmd is done during validation
        GeneralValidator gv = new GeneralValidator(cmd,  new DownloadRecordingCommandValidator(config))
        BindingResult results = gv.validate()
        if (results.hasErrors()) {
            def retVal = new BadRequestResult(results)
            return new ResponseEntity<BadRequestResult>(retVal, HttpStatus.BAD_REQUEST)
         } else {
            ObjectCommandResponse result

            try {
                result = motionService.downloadRecording(cmd)
                if (result.status == PassFail.FAIL) {
                    logService.cam.error "MotionController.downloadRecording() ${result.error}"
                    ErrorResponse retVal = new ErrorResponse(new Exception("Error in downloadRecording"), "motion/downloadRecording", result.error, "")
                    return new ResponseEntity<Object>(retVal, HttpStatus.BAD_REQUEST)
                } else {
                    File recordingFile = result.responseObject as File

                    String fileName = recordingFile.getName()
                    try (def fis = new FileInputStream(recordingFile)) {
                        Files.delete(recordingFile.toPath())

                        org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders()
                        headers.add("Content-disposition", "attachment;filename=${fileName}")
                        def body = fis.getBytes()
                        ResponseEntity ent = ResponseEntity
                                .ok()
                                .headers(headers)
                                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                                .contentLength(body.length)
                                .headers(headers)
                                .body(body)
                        return ent
                    }
                }
            }
            catch (Exception ex) {
                logService.cam.error "MotionController.downloadRecording() " + ex.getMessage()
                throw new NVRRestMethodException(ex.getMessage(), "motion/downloadRecording", ex.getCause().getMessage())
            }
        }
    }

    /**
     * deleteRecording: Delete al the files comprising a motion event recording
     * @param cmd : fileName The name of any one of the files in the recording to be deleted
     *                      All the files will be deleted.
     */
    @Secured(['ROLE_CLIENT', 'ROLE_CLOUD'])
    @PostMapping("/deleteRecording")
    def deleteRecording(@RequestBody DeleteRecordingCommand cmd) {
        // This more convoluted validation is used as setting up the folder property in cmd is done during validation
        GeneralValidator gv = new GeneralValidator(cmd,  new DeleteRecordingCommandValidator(config))
        BindingResult results = gv.validate()
        if (results.hasErrors()) {
            def retVal = new BadRequestResult(results)
            return new ResponseEntity<BadRequestResult>(retVal, HttpStatus.BAD_REQUEST)
        } else {
            ObjectCommandResponse result
            result = motionService.deleteRecording(cmd)
            if (result.status != PassFail.PASS) {
                throw new NVRRestMethodException(result.error, "motion/deleteRecording")
            } else {
                logService.cam.info("Recording ${cmd.fileName} has been deleted")
                return ResponseEntity.ok().body("")
            }
        }
    }
}
