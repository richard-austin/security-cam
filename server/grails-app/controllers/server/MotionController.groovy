package server

import grails.plugin.springsecurity.annotation.Secured
import security.cam.GetMotionEventsCommand
import security.cam.MotionService
import security.cam.enums.PassFail
import security.cam.interfaceobjects.ObjectCommandResponse

class MotionController {
    MotionService motionService

    @Secured(['ROLE_CLIENT'])
    def getMotionEvents(GetMotionEventsCommand cmd) {
        if (cmd.hasErrors()) {
            render(status: 400, text: cmd.getErrors())
        } else {
            ObjectCommandResponse motionEvents = motionService.getMotionEvents()

        if (motionEvents.status != PassFail.PASS)
            render(status: 500, text: motionEvents.error)
        else
            render motionEvents.responseObject as String
        }
    }
}
