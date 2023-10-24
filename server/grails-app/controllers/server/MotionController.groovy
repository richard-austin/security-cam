package server

import com.google.gson.internal.LinkedHashTreeMap
import grails.converters.JSON
import grails.plugin.springsecurity.annotation.Secured
import grails.validation.ValidationErrors
import security.cam.LogService
import security.cam.commands.DeleteRecordingCommand
import security.cam.commands.DownloadRecordingCommand
import security.cam.commands.GetMotionEventsCommand
import security.cam.MotionService
import security.cam.ValidationErrorService
import security.cam.enums.PassFail
import security.cam.interfaceobjects.ObjectCommandResponse

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

class Camera {
    String name=''
    String address=''
    CameraParamSpecs cameraParamSpecs = null
    boolean ftp = false
    String snapshotUri=''
    boolean ptzControls = false
    Map<String, Stream> streams = new LinkedHashTreeMap<String, Stream>()
    String onvifHost=''
    boolean backchannelAudioSupported = false
    String rtspTransport = "tcp"
    int retriggerWindow = 30
}

class MotionController {
    static responseFormats = ['json', 'xml']
    LogService logService
    MotionService motionService
    ValidationErrorService validationErrorService

    /**
     * getMotionEvents: Get the motion events for the given camera.
     * @param cmd : camera: Camera to get motion events for
     * @return
     */
    @Secured(['ROLE_CLIENT', 'ROLE_CLOUD', 'ROLE_GUEST'])
    def getMotionEvents(GetMotionEventsCommand cmd) {
        response.contentType = "application/json"

        if (cmd.hasErrors()) {
            def errorsMap = validationErrorService.commandErrors(cmd.errors as ValidationErrors, 'getMotionEvents')
            logService.cam.error "getMotionEvents: Validation error: " + errorsMap.toString()
            render(status: 400, text: errorsMap as JSON)
        } else {
            ObjectCommandResponse motionEvents = motionService.getMotionEvents(cmd)

            if (motionEvents.status != PassFail.PASS) {
                render(status: 500, text: motionEvents.error)
            } else {
                logService.cam.info("getMotionEvents: success")
                render new MotionEvents(motionEvents.responseObject as String[]) as JSON
            }
        }
    }

    /**
     * downloadRecording: API call for downloading recordings
     * @param cmd: manifest: The manifest file name.
     *             camera: The camera the recording is from
     * @return: Binary stream for the .mp4 file
     */
    @Secured(['ROLE_CLIENT', 'ROLE_CLOUD', 'ROLE_GUEST'])
    def downloadRecording(DownloadRecordingCommand cmd) {
        ObjectCommandResponse result

        if (cmd.hasErrors()) {
            def errorsMap = validationErrorService.commandErrors(cmd.errors as ValidationErrors, 'downloadRecording')
            logService.cam.error "downloadRecording: Validation error: " + errorsMap.toString()
            render(status: 400, text: errorsMap as JSON)
        } else {
            try {
                result = motionService.downloadRecording(cmd)
                if (result.status == PassFail.FAIL) {
                    def jsonObj = [
                            text       : "Error in downloadRecording",
                            description: result.error
                    ]
                    logService.cam.error "MotionController.downloadNamedPatternsFile() ${result.error}"
                    render(status: 400, text: jsonObj as JSON)
                } else {
                    File recordingFile = result.responseObject as File

                    String fileName = recordingFile.getName()
                    def fis = new FileInputStream(recordingFile)
                    Files.delete(recordingFile.toPath())
                    response.contentType = 'application/octet-stream'
                    response.setHeader "Content-disposition", "attachment;filename=${fileName}"
                    render(file: fis, fileName: fileName, contentType: '*/*')
                    fis.close()
                }
            }
            catch(Exception ex)
            {
                logService.cam.error "MotionController.downloadNamedPatternsFile() "+ex.getMessage()
                render(status: 500, contentType: "application/json") { text: ex.getMessage() }
            }
        }
    }

    /**
     * deleteRecording: Delete al the files comprising a motion event recording
     * @param cmd : fileName The name of any one of the files in the recording to be deleted
     *                      All the files will be deleted.
     */
    @Secured(['ROLE_CLIENT', 'ROLE_CLOUD'])
    def deleteRecording(DeleteRecordingCommand cmd) {
        ObjectCommandResponse result

        if (cmd.hasErrors()) {
            def errorsMap = validationErrorService.commandErrors(cmd.errors as ValidationErrors, 'deleteRecording')
            logService.cam.error "deleteRecording: Validation error: " + errorsMap.toString()
            render(status: 400, text: errorsMap as JSON)
        } else {
            result = motionService.deleteRecording(cmd)
            if (result.status != PassFail.PASS) {
                render(status: 500, text: result.error)
            } else {
                logService.cam.info("deleteRecording: success")
                render ""
            }
        }
    }
}
