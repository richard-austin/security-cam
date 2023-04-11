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
import security.cam.commands.SetupGuestAccountCommand
import security.cam.enums.PassFail
import security.cam.interfaceobjects.ObjectCommandResponse

class UserController {
    UserAdminService userAdminService
    ValidationErrorService validationErrorService
    LogService logService

    @Secured(['ROLE_CLIENT', 'ROLE_CLOUD'])
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

    @Secured(['ROLE_CLIENT', 'ROLE_CLOUD', 'ROLE_GUEST'])
    def getEmail() {
        ObjectCommandResponse result = userAdminService.getEmail()
        if (result.status != PassFail.PASS) {
            render(status: 500, text: result.error)
        } else {
            logService.cam.info("getEmail: success")
            render (text: result.responseObject as JSON)
        }
    }

    @Secured(['ROLE_CLIENT', 'ROLE_CLOUD'])
    def changeEmail(ChangeEmailCommand cmd) {
        ObjectCommandResponse result

        if (cmd.hasErrors()) {
            def errorsMap = validationErrorService.commandErrors(cmd.errors as ValidationErrors, 'changeEmail')
            logService.cam.error "changeEmail: Validation error: " + errorsMap.toString()
            render(status: 400, text: errorsMap as JSON)
        } else {
            result = userAdminService.changeEmail(cmd)
            if (result.status != PassFail.PASS) {
                render(status: 500, text: result.error)
            } else {
                logService.cam.info("changeEmail: success")
                render ""
            }
        }
    }

    @Secured(['ROLE_CLOUD'])
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

    /**
     * createAccountLocally: Unsecured to enable account creation without being logged in.
     *                       This URL is blocked by nginx to prevent external access, and is
     *                       accesses on tomcats port 8080.
     *
     * @param cmd
     * @return
     */
    def createAccountLocally(CreateAccountCommand cmd){
        createAccount(cmd)
    }

    @Secured(['ROLE_CLOUD'])
    /**
     * removeAccount: Remove the local web account used for direct access to to NVR
     */
    def removeAccount()
    {
        ObjectCommandResponse result = userAdminService.removeAccount()
        if (result.status != PassFail.PASS) {
            render(status: 500, text: result.error)
        } else {
            logService.cam.info("removeAccount: success")
            render (text: result.responseObject as JSON)
        }
    }

    @Secured(['ROLE_CLOUD'])
    def hasLocalAccount() {
        ObjectCommandResponse result = userAdminService.hasLocalAccount()

        if (result.status != PassFail.PASS) {
            render(status: 500, text: result.error)
        } else {
            logService.cam.info("hasLocalAccount: (= ${result.responseObject}) success")
            render(text: result.responseObject) as JSON
        }
    }

    @Secured(['ROLE_CLIENT'])
    def setupGuestAccount(SetupGuestAccountCommand cmd)
    {
        ObjectCommandResponse result

        if(cmd.hasErrors()) {
            def errorsMap = validationErrorService.commandErrors(cmd.errors as ValidationErrors, 'setupGuestAccount')
            logService.cam.error "setupGuestAccount: Validation error: " + errorsMap.toString()
            render(status: 400, text: errorsMap as JSON)
        }
        else {
            result = userAdminService.setupGuestAccount(cmd)

            if(result.status != PassFail.PASS)
                render (ststus: 500, text: result.error)
            else {
                logService.cam.info("setupGuestAccount: (= ${result.responseObject}) success")
                render(text: result.responseObject as JSON )
            }
        }
    }

    @Secured(['ROLE_CLIENT', 'ROLE_CLOUD', 'ROLE_GUEST'])
    def isGuest()
    {
        ObjectCommandResponse result = userAdminService.isGuest()

        if(result.status != PassFail.PASS)
            render(status: 500, text: result.error)
        else
            render (status: 200, text: result.responseObject as JSON)
    }

    @Secured(['ROLE_CLIENT'])
    def guestAccountEnabled()
    {
        ObjectCommandResponse result = userAdminService.guestAccountEnabled()
        if(result.status != PassFail.PASS)
            render(status: 500, text: result.error)
        else
            render (status: 200, text: result.responseObject as JSON)
    }
}

