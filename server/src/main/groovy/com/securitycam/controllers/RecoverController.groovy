package com.securitycam.controllers

import com.securitycam.commands.ResetPasswordFromLinkCommand
import com.securitycam.commands.SendResetPasswordLinkCommand
import com.securitycam.enums.PassFail
import com.securitycam.interfaceobjects.ObjectCommandResponse
import com.securitycam.services.LogService
import com.securitycam.services.UserAdminService
import com.securitycam.validators.BadRequestResult
import com.securitycam.validators.GeneralValidator
import com.securitycam.validators.ResetPasswordFromLinkCommandValidator
import com.securitycam.validators.SendResetPasswordLinkCommandValidator
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.servlet.view.RedirectView


@Controller
@RequestMapping("/recover")
class RecoverController {
    @Autowired
    LogService logService
    @Autowired
    UserAdminService userAdminService

    @RequestMapping("/forgotPassword")
    def forgotPassword() {
        return "forgotPassword"
    }

    @RequestMapping("/sendResetPasswordLink")
    def sendResetPasswordLink(SendResetPasswordLinkCommand cmd) {
        ObjectCommandResponse result

        def gv = new GeneralValidator(cmd, new SendResetPasswordLinkCommandValidator())
        def results = gv.validate()
        if (results.hasErrors()) {
            def retVal = new BadRequestResult(results)

            logService.cam.error "sendResetPasswordLink: Validation error: " + retVal.toString()
            flash.error = "Validation error: "
            boolean doneOne = false
            retVal.forEach {k, v ->
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
    @RequestMapping("/resetPassword")
    def resetPassword(ResetPasswordFromLinkCommand cmd) {
        ObjectCommandResponse result

        def gv = new GeneralValidator(cmd, new ResetPasswordFromLinkCommandValidator())
        def results = gv.validate()

        if (results.hasErrors()) {
            def retVal = new BadRequestResult(results)
            logService.cam.error "resetPassword: Validation error: " + errorsMap.toString()
            flash.error = "Validation error: "
            boolean doneOne = false
            retVal.forEach {k, v ->
                if(doneOne) {
                    flash.error += ", "
                }
                doneOne = true
                flash.error += (k + ":" + v)
            }
            redirect(action: "resetPasswordForm", params: [key: cmd.resetKey])
        } else {
            result = userAdminService.resetPasswordFromLink(cmd)
            if(result.status == PassFail.PASS) {
                flash.message = "Password reset successfully"
                redirect(action: "resetPasswordForm", params: [passwordSet: true])
            }
            else {
                flash.error = result.error
                redirect(action: "resetPasswordForm")
            }
        }
    }

    @RequestMapping("/notFound")
    def notFound() {
        return "notFound"
    }
}
