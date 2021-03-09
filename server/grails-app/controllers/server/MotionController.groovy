package server

import grails.converters.JSON
import grails.plugin.springsecurity.annotation.Secured
import security.cam.LogService
import security.cam.commands.GetMotionEventsCommand
import security.cam.MotionService
import security.cam.ValidationErrorService
import security.cam.commands.GetOffsetForEpochCommand
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
    String masterManifest
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

    @Secured(['ROLE_CLIENT'])
    def getMotionEvents(GetMotionEventsCommand cmd) {
        response.contentType = "application/json"

        if (cmd.hasErrors()) {
            def errorsMap = validationErrorService.commandErrors(cmd.errors, 'getMotionEvents')
            logService.cam.error "getMotionEvents Validation error: "+cmd.errors.toString()
            render(status: 400, text: errorsMap as JSON)
        } else {
            ObjectCommandResponse motionEvents = motionService.getMotionEvents(cmd)

            if (motionEvents.status != PassFail.PASS) {
                logService.cam.error("getMotionEvents "+motionEvents.error)
                render(status: 500, text: motionEvents.error)
            }
            else {
                logService.cam.info("getMotionEvents success")
                render new MotionEvents(motionEvents.responseObject as String[]) as JSON
            }
        }
    }

    @Secured(['ROLE_CLIENT'])
    def getTimeOffsetForEpoch(GetOffsetForEpochCommand cmd)
    {
        if(cmd.hasErrors())
        {
            def errorsMap = validationErrorService.commandErrors(cmd.errors, 'getTimeOffsetForEpoch')
            render(status: 400, text: errorsMap as JSON)
        }
        else {
            ObjectCommandResponse result = motionService.getOffsetForEpoch(cmd.epoch, cmd.motionName)

            if(result.status == PassFail.PASS)
            {

                Double timeOffset = result.responseObject as Double
                render new TimeOffset(timeOffset) as JSON
            }
            else
                render (status: 500, text: result.error)
        }
    }
}
