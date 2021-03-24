package server

import grails.converters.JSON
import grails.plugin.springsecurity.annotation.Secured
import grails.validation.ValidationErrors
import security.cam.LogService
import security.cam.commands.DeleteRecordingCommand
import security.cam.commands.GetMotionEventsCommand
import security.cam.MotionService
import security.cam.ValidationErrorService
import security.cam.enums.PassFail
import security.cam.interfaceobjects.ObjectCommandResponse

import java.text.DecimalFormat

class MotionEvents
{
    MotionEvents(String[] events)
    {
        this.events = events
    }

    String[] events
}

class TimeOffset
{
    TimeOffset(Double timeOffset)
    {
        String strTOffset
        DecimalFormat f = new DecimalFormat("##")
        setOffset(f.format(timeOffset).toString())

    }
    String offset
}

class Recording
{
    String uri
    String location
}

class Camera
{
    String name
    String motionName
    String descr;
    boolean defaultOnMultiDisplay;
    String uri
    Recording recording
}

class MotionController {
    static responseFormats = ['json', 'xml']
    LogService logService
    MotionService motionService
    ValidationErrorService validationErrorService

    /**
     * getMotionEvents: Get the motion events for the given camera.
     * @param cmd: camera: Camera to get motion events for
     * @return
     */
    @Secured(['ROLE_CLIENT'])
    def getMotionEvents(GetMotionEventsCommand cmd) {
        response.contentType = "application/json"

        if (cmd.hasErrors()) {
            def errorsMap = validationErrorService.commandErrors(cmd.errors, 'getMotionEvents')
            logService.cam.error "getMotionEvents: Validation error: "+errorsMap.toString()
            render(status: 400, text: errorsMap as JSON)
        } else {
            ObjectCommandResponse motionEvents = motionService.getMotionEvents(cmd)

            if (motionEvents.status != PassFail.PASS) {
                render(status: 500, text: motionEvents.error)
            }
            else {
                logService.cam.info("getMotionEvents: success")
                render new MotionEvents(motionEvents.responseObject as String[]) as JSON
            }
        }
    }

    /**
     * deleteRecording: Delete al the files comprising a motion event recording
     * @param cmd: fileName The name of any one of the files in the recording to be deleted
     *                      All the files will be deleted.
     */
    @Secured(['ROLE_CLIENT'])
    def deleteRecording(DeleteRecordingCommand cmd)
    {
        ObjectCommandResponse result

        if(cmd.hasErrors())
        {
            def errorsMap = validationErrorService.commandErrors(cmd.errors, 'deleteRecording')
            logService.cam.error "deleteRecording: Validation error: "+errorsMap.toString()
            render(status: 400, text: errorsMap as JSON)
        }
        else
        {
            result = motionService.deleteRecording(cmd)
            if (result.status != PassFail.PASS) {
                render(status: 500, text: result.error)
            }
            else {
                logService.cam.info("deleteRecording: success")
                render ""
            }
        }
    }
}
