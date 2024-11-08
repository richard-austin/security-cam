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
import jakarta.servlet.http.HttpServletRequest
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.servlet.mvc.support.RedirectAttributes
import org.springframework.web.servlet.support.RequestContextUtils


@Controller
@RequestMapping("/recover")
class RecoverController {
    @Autowired
    LogService logService
    @Autowired
    UserAdminService userAdminService

    @RequestMapping("/forgotPassword")
    def forgotPassword(Model model, HttpServletRequest request) {
        def inputFlashMap = RequestContextUtils.getInputFlashMap(request)
        if(inputFlashMap != null) {
            model.addAttribute("error", inputFlashMap.get("error"))
            model.addAttribute("message", inputFlashMap.get("message"))
        }
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

    @RequestMapping( "/resetPasswordForm")
    def resetPasswordForm(Model model, HttpServletRequest request) {
        def flashMap = RequestContextUtils.getInputFlashMap(request)
        String queryString = request.getQueryString()

        if(queryString != null)
            model.addAttribute("key", queryString.substring("key=".length()))
        else
            model.addAttribute("key", "")

        if(flashMap) {
            model.addAttribute("params", flashMap.get("params"))
            model.addAttribute("error", flashMap.get("error"))
            model.addAttribute("message", flashMap.get("message"))
        }
        else {
            model.addAttribute("params", null)
            model.addAttribute("error", null)
            model.addAttribute("message", null)
        }

        return "/resetPasswordForm"
    }

    @RequestMapping(value = "/resetPassword", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    def resetPassword(RedirectAttributes redirectAttributes,  ResetPasswordFromLinkCommand cmd) {
        ObjectCommandResponse result
        def gv = new GeneralValidator(cmd, new ResetPasswordFromLinkCommandValidator())
        def results = gv.validate()

        if (results.hasErrors()) {
            def retVal = new BadRequestResult(results)
            String error = "Validation Error: "

            boolean doneOne = false
            retVal.forEach {k, v ->
                if(doneOne) {
                    error += ", "
                }
                doneOne = true
                error += (k + ":" + v)
            }
            logService.cam.error "resetPassword:  ${error}"
            redirectAttributes.addFlashAttribute("error", error)
            def params = [key: cmd.resetKey]
            redirectAttributes.addFlashAttribute("params", params)
            return "redirect:/recover/resetPasswordForm"
        } else {
            result = userAdminService.resetPasswordFromLink(cmd)
            if(result.status == PassFail.PASS) {
                redirectAttributes.addFlashAttribute("message", "Password reset successfully")
                def params = [passwordSet: true]
                redirectAttributes.addFlashAttribute("params", params)
                return "redirect:/recover/resetPasswordForm"
            }
            else {
                redirectAttributes.addFlashAttribute("error", result.error)
                return "redirect:/recover/resetPasswordForm"
            }
        }
    }

    @RequestMapping("/notFound")
    def notFound() {
        return "notFound"
    }
}
