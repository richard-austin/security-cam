package server

import grails.converters.JSON
import grails.plugin.springsecurity.annotation.Secured
import security.cam.GetMotionEventsCommand
import security.cam.MotionService
import security.cam.ValidationErrorService
import security.cam.enums.PassFail
import security.cam.interfaceobjects.ObjectCommandResponse

class MotionController {
    MotionService motionService
    ValidationErrorService validationErrorService

    @Secured(['ROLE_CLIENT'])
    def getMotionEvents(GetMotionEventsCommand cmd) {
        if (cmd.hasErrors()) {
            def errorsMap = validationErrorService.commandErrors(cmd.errors, 'getMotionEvents')
            render(status: 400, text: errorsMap as JSON)
        } else {
            ObjectCommandResponse motionEvents = motionService.getMotionEvents(cmd)

        if (motionEvents.status != PassFail.PASS)
            render(status: 500, text: motionEvents.error)
        else
            render motionEvents.responseObject as String
        }
    }
}
