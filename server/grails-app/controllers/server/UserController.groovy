package server

import grails.converters.JSON
import grails.plugin.springsecurity.annotation.Secured
import org.springframework.validation.Errors
import security.cam.LogService
import security.cam.UserAdminService
import security.cam.ValidationErrorService
import security.cam.commands.ResetPasswordCommand
import security.cam.enums.PassFail
import security.cam.interfaceobjects.ObjectCommandResponse

class UserController {
    UserAdminService userAdminService
    ValidationErrorService validationErrorService
    LogService logService

    @Secured(['ROLE_CLIENT'])
    def changePassword(ResetPasswordCommand cmd) {
        ObjectCommandResponse result

        if(cmd.hasErrors())
        {
            def errorsMap = validationErrorService.commandErrors(cmd.errors, 'changePassword')
            logService.cam.error "changePassword: Validation error: "+errorsMap.toString()
            render(status: 400, text: errorsMap as JSON)
        }
        else
        {
            result = userAdminService.resetPassword(cmd)
            if (result.status != PassFail.PASS) {
                render(status: 500, text: result.error)
            }
            else {
                logService.cam.info("changePassword: success")
                render ""
            }
        }
     }
}

