package com.securitycam.controllers

import com.securitycam.commands.ChangeEmailCommand
import com.securitycam.commands.ResetPasswordCommand
import com.securitycam.enums.PassFail
import com.securitycam.error.NVRRestMethodException
import com.securitycam.interfaceobjects.ObjectCommandResponse
import com.securitycam.security.TwoFactorAuthenticationProvider
import com.securitycam.services.LogService
import com.securitycam.services.UserAdminService
import com.securitycam.validators.BadRequestResult
import com.securitycam.validators.ChangeEmailCommandValidator
import com.securitycam.validators.GeneralValidator
import com.securitycam.validators.ResetPasswordCommandValidator
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.annotation.Secured
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/user")
class UserController {
    @Autowired
    UserAdminService userAdminService
    @Autowired
    LogService logService
    @Autowired
    TwoFactorAuthenticationProvider authenticationManager

    @Secured(['ROLE_CLIENT', 'ROLE_CLOUD'])
    @PostMapping("/changePassword")
    def changePassword(@RequestBody ResetPasswordCommand cmd) {
        ObjectCommandResponse resp
        def gv = new GeneralValidator(cmd, new ResetPasswordCommandValidator(authenticationManager))
        def result = gv.validate()

        if (result.hasErrors()) {
            def retVal = new BadRequestResult(result)
            logService.cam.error "changePassword: Validation error: "
            return new ResponseEntity<BadRequestResult>(retVal, HttpStatus.BAD_REQUEST)
        } else {
            resp = userAdminService.resetPassword(cmd)
            if (resp.status != PassFail.PASS) {
                throw new NVRRestMethodException(resp.error, "user/changePassword", "See logs")
            } else {
                logService.cam.info("changePassword: success")
                return ""
            }
        }
    }

    @Secured(['ROLE_CLIENT', 'ROLE_CLOUD', 'ROLE_GUEST'])
    @PostMapping("/getEmail")
    def getEmail() {
        ObjectCommandResponse result = userAdminService.getEmail()
        if (result.status != PassFail.PASS) {
            throw new NVRRestMethodException(result.error, "user/getEmail", "See logs")
        } else {
            logService.cam.info("getEmail: success")
            return result.responseObject
        }
    }

    @Secured(['ROLE_CLIENT', 'ROLE_CLOUD'])
    @PostMapping("/changeEmail")
    def changeEmail(@RequestBody ChangeEmailCommand cmd) {
        ObjectCommandResponse resp

        def gv = new GeneralValidator(cmd, new ChangeEmailCommandValidator(authenticationManager))
        def result = gv.validate()

        if (result.hasErrors()) {
            def retVal = new BadRequestResult(result)
            logService.cam.error "changeEmail: Validation error: "
            return new ResponseEntity<BadRequestResult>(retVal, HttpStatus.BAD_REQUEST)
        } else {
            resp = userAdminService.changeEgetEmail and ChangeEmailmail(cmd)
            if (resp.status != PassFail.PASS) {
                throw new NVRRestMethodException(resp.error, "user/changeEmail", "See logs")
            } else {
                logService.cam.info("changeEmail: success")
                return ""
            }
        }
    }

    @Secured(['ROLE_CLIENT', 'ROLE_CLOUD', 'ROLE_GUEST'])
    @RequestMapping("isGuest")
    def isGuest() {
        ObjectCommandResponse result = userAdminService.isGuest()
        if (result.status != PassFail.PASS)
            throw new NVRRestMethodException(result.error, "user/isGuest", "See logs")
        else
           return ResponseEntity.ok(result.responseObject)
    }

    @Secured(['ROLE_CLIENT'])
    @RequestMapping("guestAccountEnabled")
    def guestAccountEnabled() {
        ObjectCommandResponse result = userAdminService.guestAccountEnabled()
        if (result.status != PassFail.PASS)
            throw new NVRRestMethodException(result.error, "user/guestAccountEnabled", "See logs")
        else
            return  ResponseEntity.ok(result.responseObject)
    }
}
