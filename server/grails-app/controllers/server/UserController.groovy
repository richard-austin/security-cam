package server

import grails.converters.JSON
import grails.plugin.springsecurity.annotation.Secured
import grails.validation.ValidationErrors
import security.cam.LogService
import security.cam.UserAdminService
import security.cam.ValidationErrorService
import security.cam.commands.ChangeEmailCommand
import security.cam.commands.CreateAccountCommand
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

        if (cmd.hasErrors()) {
            def errorsMap = validationErrorService.commandErrors(cmd.errors as ValidationErrors, 'changePassword')
            logService.cam.error "changePassword: Validation error: " + errorsMap.toString()
            render(status: 400, text: errorsMap as JSON)
        } else {
            result = userAdminService.resetPassword(cmd)
            if (result.status != PassFail.PASS) {
                render(status: 500, text: result.error)
            } else {
                logService.cam.info("changePassword: success")
                render ""
            }
        }
    }

    @Secured(['ROLE_CLIENT'])
    def changeEmail(ChangeEmailCommand cmd) {
        ObjectCommandResponse result

        if (cmd.hasErrors()) {
            def errorsMap = validationErrorService.commandErrors(cmd.errors as ValidationErrors, 'changeEmail')
            logService.cam.error "changeEmail: Validation error: " + errorsMap.toString()
            render(status: 400, text: errorsMap as JSON)
        }
        else {
            result = userAdminService.changeEmail(cmd)
            if (result.status != PassFail.PASS) {
                render(status: 500, text: result.error)
            } else {
                logService.cam.info("changeEmail: success")
                render ""
            }
        }
    }

    @Secured(['ROLE_CLIENT'])
    def createAccount(CreateAccountCommand cmd) {
        ObjectCommandResponse result

        if (cmd.hasErrors()) {
            def errorsMap = validationErrorService.commandErrors(cmd.errors as ValidationErrors, 'createAccount')
            logService.cam.error "createAccount: Validation error: " + errorsMap.toString()
            render(status: 400, text: errorsMap as JSON)
        } else {
            result = userAdminService.createAccount(cmd)
            if (result.status != PassFail.PASS) {
                render(status: 500, text: result.error)
            } else {
                logService.cam.info("createAccount: success")
                render ""
            }
        }
    }

    @Secured(['ROLE_CLIENT'])
    def hasLocalAccount() {
        ObjectCommandResponse result = userAdminService.hasLocalAccount()

        if (result.status != PassFail.PASS) {
            render(status: 500, text: result.error)
        } else {
            logService.cam.info("hasLocalAccount: (= ${result.responseObject}) success")
            render(text: result.responseObject) as JSON
        }
    }
}

