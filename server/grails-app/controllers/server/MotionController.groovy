package server

import grails.converters.JSON
import grails.plugin.springsecurity.annotation.Secured
import security.cam.commands.GetMotionEventsCommand
import security.cam.MotionService
import security.cam.ValidationErrorService
import security.cam.commands.GetOffsetForEpochCommand
import security.cam.enums.PassFail
import security.cam.interfaceobjects.ObjectCommandResponse

class MotionEvents
{
    MotionEvents(String[] events)
    {
        this.events = events
    }

    String[] events
}

class MotionController {
    static responseFormats = ['json', 'xml']

    MotionService motionService
    ValidationErrorService validationErrorService

    @Secured(['ROLE_CLIENT'])
    def getMotionEvents(GetMotionEventsCommand cmd) {
        response.contentType = "application/json"

        if (cmd.hasErrors()) {
            def errorsMap = validationErrorService.commandErrors(cmd.errors, 'getMotionEvents')
            render(status: 400, text: errorsMap as JSON)
        } else {
            ObjectCommandResponse motionEvents = motionService.getMotionEvents(cmd)

        if (motionEvents.status != PassFail.PASS)
            render(status: 500, text: motionEvents.error)
        else
            render new MotionEvents(motionEvents.responseObject as String[]) as JSON
        }
    }

    @Secured(['ROLE_CENT'])
    def getTimeOffsetForEpoch(GetOffsetForEpochCommand cmd)
    {
        if(cmd.hasErrors())
        {
            def errorsMap = validationErrorService.commandErrors(cmd.errors, 'getTimeOffsetForEpoch')
            render(status: 400, text: errorsMap as JSON)
        }
        else {
            Double timeOffset = motionService.getOffsetForEpoch(cmd.epoch, cmd.motionName)
            render(offset: timeOffset.toString())
        }
    }
}
