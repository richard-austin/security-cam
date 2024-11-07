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
import org.springframework.http.MediaType
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestMethod
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.servlet.mvc.support.RedirectAttributes


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

    @PostMapping(value="/sendResetPasswordLink", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    def sendResetPasswordLink(RedirectAttributes redirectAttributes, SendResetPasswordLinkCommand cmd) {
        ObjectCommandResponse result

        def gv = new GeneralValidator(cmd, new SendResetPasswordLinkCommandValidator())
        def results = gv.validate()
        if (results.hasErrors()) {
            def retVal = new BadRequestResult(results)

            logService.cam.error "sendResetPasswordLink: Validation error: " + retVal.toString()
            String error = "Validation error: "
            boolean doneOne = false
            retVal.forEach {k, v ->
                if(doneOne) {
                    error += ", "
                }
                doneOne = true
                error += (k + ":" + v)
                redirectAttributes.addFlashAttribute("error", error)
            }
            return "redirect:/recover/forgotPassword"
        } else {
            result = userAdminService.sendResetPasswordLink(cmd)
            if (result.status != PassFail.PASS) {
                logService.cam.error "sendResetPasswordLink: error: ${result.error}"
                redirectAttributes.addFlashAttribute("error", result.error)
                return "redirect:/recover/forgotPassword"
            } else {
                logService.cam.info("sendResetPasswordLink: success")
                redirectAttributes.addFlashAttribute("message", "Please check your email for the reset password link")
                return "redirect:/recover/forgotPassword"
            }
        }
    }

    @RequestMapping("/resetPasswordForm")
    def resetPasswordForm() {
     return "redirect:/resetPasswordForm"
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
