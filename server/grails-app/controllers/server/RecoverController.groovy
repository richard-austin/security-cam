package server

import grails.converters.JSON
import grails.validation.ValidationErrors
import security.cam.LogService
import security.cam.UserAdminService
import security.cam.ValidationErrorService
import security.cam.commands.SendResetPasswordLinkCommand
import security.cam.enums.PassFail
import security.cam.interfaceobjects.ObjectCommandResponse

class RecoverController {
    LogService logService
    UserAdminService userAdminService
    ValidationErrorService validationErrorService

    def forgotPassword() {
        render view: 'forgotPassword.gsp'
    }

    def sendResetPasswordLink(SendResetPasswordLinkCommand cmd) {
        ObjectCommandResponse result
        if (cmd.hasErrors()) {
            def errorsMap = validationErrorService.commandErrors(cmd.errors as ValidationErrors, 'sendResetPasswordLink')
            logService.cam.error "sendResetPasswordLink: Validation error: " + errorsMap.toString()
            render(status: 400, text: errorsMap as JSON)
        } else {
            result = userAdminService.sendResetPasswordLink(cmd)
            if (result.status != PassFail.PASS) {
                logService.cam.error "sendResetPasswordLink: error: ${result.error}"
                render(status: 500, text: result.error)
            } else {
                logService.cam.info("sendResetPasswordLink: success")
                render ""
            }
        }
    }

}
