package server

import grails.validation.ValidationErrors
import security.cam.LogService
import security.cam.UserAdminService
import security.cam.ValidationErrorService
import security.cam.commands.ResetPasswordFromLinkCommand
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
            flash.error = "Validation error: "
            boolean doneOne = false
            errorsMap.forEach {k, v ->
                if(doneOne) {
                    flash.error += ", "
                }
                doneOne = true
                flash.error += (k + ":" + v)
            }
            redirect(action: "forgotPassword")
        } else {
            result = userAdminService.sendResetPasswordLink(cmd)
            if (result.status != PassFail.PASS) {
                logService.cam.error "sendResetPasswordLink: error: ${result.error}"
                flash.error = result.error
                redirect(action: "forgotPassword")
            } else {
                logService.cam.info("sendResetPasswordLink: success")
                flash.message = "Please check your email for the reset password link"
                redirect(action: "forgotPassword")
            }
        }
    }

    def resetPasswordForm() {
     //   redirect(action: 'resetPasswordForm')
    }

    def resetPassword(ResetPasswordFromLinkCommand cmd) {
        ObjectCommandResponse result
        if (cmd.hasErrors()) {
            def errorsMap = validationErrorService.commandErrors(cmd.errors as ValidationErrors, 'sendResetPasswordLink')
            logService.cam.error "resetPassword: Validation error: " + errorsMap.toString()
            flash.error = "Validation error: "
            boolean doneOne = false
            errorsMap.forEach {k, v ->
                if(doneOne) {
                    flash.error += ", "
                }
                doneOne = true
                flash.error += (k + ":" + v)
            }
            redirect(action: "resetPasswordForm")
        } else {
            userAdminService.resetPasswordFromLink(cmd)
            flash.message = "Password reset successfully"
            redirect(action: "resetPasswordForm")
        }
    }
}
